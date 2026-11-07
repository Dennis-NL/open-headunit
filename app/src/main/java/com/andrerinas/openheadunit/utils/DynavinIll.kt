package com.andrerinas.openheadunit.utils

import android.os.IBinder
import android.os.Parcel

/**
 * Headlight (ILL) state on Dynavin head units, for the "car signal" night mode and app theme where
 * Settings.System car_mode does not exist. The state lives in the dynavin_prop service (no permission
 * check); every change is announced by the panel-brightness broadcast [ACTION].
 */
object DynavinIll {

    const val ACTION = "dynavin.intent.action.ScreenBrightness"

    /** True/false, or null when this is not a Dynavin unit. */
    fun read(): Boolean? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "dynavin_prop") as? IBinder ?: return null
            data.writeInterfaceToken("android.dynavin.prop.IDynavinPropService")
            data.writeString("dynavin_mark_mcu_heartbeat_ill")
            binder.transact(4, data, reply, 0) // RPC_ProviderDnvService_keep_getValue
            reply.readException()
            reply.readString()?.toBooleanStrictOrNull()
        } catch (_: Exception) {
            null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
