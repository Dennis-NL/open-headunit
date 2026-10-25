package com.andrerinas.openheadunit.connection.carkey

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.connection.carkey.byd.CarBydReceiver
import com.andrerinas.openheadunit.connection.carkey.fyt.CarFYTReceiver
import com.andrerinas.openheadunit.contract.KeyIntent
import com.andrerinas.openheadunit.utils.AppLog

interface CarKeyReceiver {

    companion object {
        @JvmStatic
        fun newDefaultReceivers(): Array<CarKeyReceiver> {
            return arrayOf(
                CarKeyBroadcastReceiver(),
                CarBydReceiver(),
                CarFYTReceiver(),
            )
        }
    }

    val isSupported: Boolean

    val isSUNeeded: Boolean

    /**
     * Whether this receiver requires an active Android Auto projection session to be registered.
     * Receivers that aggressively grab hardware focus (e.g., FYT binding IPC and setting sys.carlink.type=2)
     * MUST be session-scoped so factory apps retain steering wheel control while OpenHU is idle.
     */
    val isSessionScoped: Boolean
        get() = false

    @Throws(Exception::class)
    fun register(context: Context)

    @Throws(Exception::class)
    fun unregister()


    /** Single key press or release — broadcasts for learning and projection handling. */

    private fun handleKey(context: Context, commManager: CommManager, keyCode: Int, isDown: Boolean, downTime: Long? = null, learnCode: Int = keyCode) {
        AppLog.d("CarKeyReceiver: Broadcasting key event: code=$keyCode, isDown=$isDown")
        context.sendBroadcast(
            Intent(KeyIntent.action).apply {
                setPackage(context.packageName)
                putExtra(
                    KeyIntent.extraEvent,
                    KeyEvent(
                        if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, learnCode,
                    ),
                )
            },
        )
        commManager.sendKey(keyCode, isDown, downTime, "carkey")
    }

    fun handleKey(context: Context, keyCode: Int, isDown: Boolean) {
        handleKey(context, App.provide(context).commManager, keyCode, isDown)
    }

    /** Full click (DOWN + UP) — broadcasts both events for learning AND sends to AA. */
    private fun handleClick(context: Context, commManager: CommManager, keyCode: Int, downTime: Long?, learnCode: Int) {
        handleKey(context, commManager, keyCode, true, downTime, learnCode)
        handleKey(context, commManager, keyCode, false, downTime, learnCode)
    }

    /**
     * [downTime] identifies the press, so fast distinct clicks are not debounced as duplicates.
     * [learnCode] is what key learning sees, when that differs from the key sent to AA.
     */
    fun handleClick(context: Context, keyCode: Int, downTime: Long? = null, learnCode: Int = keyCode) {
        handleClick(context, App.provide(context).commManager, keyCode, downTime, learnCode)
    }

    fun toggleVoiceAssistant(context: Context) {
        App.provide(context).commManager.sendToggleVoiceAssistant()
    }
}
