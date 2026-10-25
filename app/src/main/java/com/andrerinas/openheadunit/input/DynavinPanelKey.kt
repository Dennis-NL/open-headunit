package com.andrerinas.openheadunit.input

import android.view.KeyEvent

/**
 * Keys of Dynavin head units (PX30, D9). DynavinService turns MCU keys into the broadcast [ACTION]
 * with an int [EXTRA_KEYCODE]; the stock menu app only forwards them to ZLink. Names are from an
 * Audi MMI 3G panel. The knob joystick reports the same codes as the four corner keys.
 */
object DynavinPanelKey {

    const val ACTION = "dynavin.intent.action.KeyEvent"
    const val EXTRA_KEYCODE = "keycode"

    /** Clear of NWD 1000+, Eryanet 2001+, BZ 3001+ and BYD 4000+. */
    const val VIRTUAL_BASE = 5000
    private val VIRTUAL_RANGE = VIRTUAL_BASE until (VIRTUAL_BASE + 1000)

    private val NAMES = mapOf(
        305 to "NAV",
        316 to "RADIO",
        323 to "MEDIA",
        326 to "BACK",
        329 to "MENU",
        340 to "Knob press",
        342 to "INFO",
        376 to "Top-right / joystick up",
        377 to "Bottom-right / joystick down",
        378 to "Top-left / joystick left",
        379 to "Bottom-left / joystick right",
        380 to "Knob right",
        381 to "Knob left",
    )

    /** Sent while the virtual key has no keymap entry; SOFT_LEFT/RIGHT become rotary scroll. */
    private val DEFAULTS = mapOf(
        340 to KeyEvent.KEYCODE_DPAD_CENTER,
        376 to KeyEvent.KEYCODE_DPAD_UP,
        377 to KeyEvent.KEYCODE_DPAD_DOWN,
        378 to KeyEvent.KEYCODE_DPAD_LEFT,
        379 to KeyEvent.KEYCODE_DPAD_RIGHT,
        380 to KeyEvent.KEYCODE_SOFT_RIGHT,
        381 to KeyEvent.KEYCODE_SOFT_LEFT,
    )

    fun toKeyCode(dynavinCode: Int): Int = VIRTUAL_BASE + dynavinCode

    fun defaultKeyCode(dynavinCode: Int): Int? = DEFAULTS[dynavinCode]

    fun label(keyCode: Int): String? {
        if (keyCode !in VIRTUAL_RANGE) return null
        val code = keyCode - VIRTUAL_BASE
        return "Dynavin ${NAMES[code] ?: "key"} ($code)"
    }
}
