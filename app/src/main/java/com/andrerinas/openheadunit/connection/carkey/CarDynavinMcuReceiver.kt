package com.andrerinas.openheadunit.connection.carkey

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SystemClock
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.input.DynavinPanelKey
import com.andrerinas.openheadunit.utils.AppLog

/**
 * Dynavin knob joystick. DynavinService only broadcasts a key on release, and the MCU never sends the release for
 * joystick down/left/right, so those never reach [CarKeyBroadcastReceiver]. The press does arrive as an
 * `E_ProtOS_Key` frame on the dynavin_mcu service (no permission check), so listen there instead.
 */
class CarDynavinMcuReceiver : CarKeyReceiver {

    private var context: Context? = null
    private var mcu: IBinder? = null

    private val listener = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != 1) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(LISTENER)
            if (data.readInt() != 0) {
                val cmd = data.readInt() and 0x1FFFFFFF
                data.readInt()
                val bytes = data.createByteArray()
                if (cmd == E_PROTOS_KEY && bytes != null && bytes.size > 4) onKey(bytes[4].toInt() and 0xFF)
            }
            if (flags and FLAG_ONEWAY == 0) reply?.writeNoException()
            return true
        }
    }.apply { attachInterface(null, LISTENER) }

    override val isSupported: Boolean get() = service() != null
    override val isSUNeeded = false

    override fun register(context: Context) {
        val binder = service() ?: return
        this.context = context
        mcu = binder
        call(binder, 1)
        DynavinPanelKey.mcuKeysActive = true
        AppLog.i("CarKeyReceiver: Listening for Dynavin MCU keys")
    }

    override fun unregister() {
        DynavinPanelKey.mcuKeysActive = false
        mcu?.let { runCatching { call(it, 2) } }
        mcu = null
        context = null
    }

    private fun onKey(mcuKey: Int) {
        val code = DynavinPanelKey.MCU_KEYS[mcuKey] ?: return
        val ctx = context ?: return
        val key = DynavinPanelKey.keyFor(App.provide(ctx).settings.keyCodes, code)
        handleClick(ctx, key, SystemClock.uptimeMillis(), DynavinPanelKey.toKeyCode(code))
    }

    /** IDynavinMcuService txn 1 = RPC_RequestMcuInfoChangedListener, 2 = RPC_RemoveMcuInfoChangedListener. */
    private fun call(binder: IBinder, txn: Int) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("android.dynavin.mcu.IDynavinMcuService")
            data.writeStrongBinder(listener)
            data.writeString(context?.packageName ?: "")
            binder.transact(txn, data, reply, 0)
            reply.readException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun service(): IBinder? = try {
        Class.forName("android.os.ServiceManager").getMethod("checkService", String::class.java)
            .invoke(null, "dynavin_mcu") as IBinder?
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val LISTENER = "android.dynavin.mcu.IMcuListener"
        const val E_PROTOS_KEY = 0x10500
    }
}
