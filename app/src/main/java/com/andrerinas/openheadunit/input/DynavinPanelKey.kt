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

    /** Raw MCU key in `E_ProtOS_Key` frames → Dynavin code. These come on press; the MCU drops the joystick releases. */
    val MCU_KEYS = mapOf(240 to 376, 241 to 377, 242 to 378, 243 to 379)

    /** Codes taken from MCU frames while CarDynavinMcuReceiver listens, so the broadcast copy is ignored. */
    @Volatile var mcuKeysActive = false

    fun toKeyCode(dynavinCode: Int): Int = VIRTUAL_BASE + dynavinCode

    fun defaultKeyCode(dynavinCode: Int): Int? = DEFAULTS[dynavinCode]

    /** The key to send: the keymap entry for the virtual key if there is one, else the default, else the virtual key. */
    fun keyFor(keyCodes: Map<Int, Int>, dynavinCode: Int): Int {
        val virtual = toKeyCode(dynavinCode)
        return if (keyCodes.containsValue(virtual)) virtual else defaultKeyCode(dynavinCode) ?: virtual
    }

    fun label(keyCode: Int): String? {
        if (keyCode !in VIRTUAL_RANGE) return null
        val code = keyCode - VIRTUAL_BASE
        return "Dynavin ${NAMES[code] ?: "key"} ($code)"
    }
}
