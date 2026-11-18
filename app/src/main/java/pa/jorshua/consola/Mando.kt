package pa.jorshua.consola

import android.view.InputDevice
import android.view.KeyEvent

enum class Accion { ARRIBA, ABAJO, IZQ, DER, A, B, X, Y, MAS, MENOS, L, R }

/**
 * Traduce teclas a acciones. El stick y la cruceta llegan como flechas: Android los convierte
 * solo (con repetición) mientras las vistas no consuman el movimiento del joystick.
 */
object Mando {
    val DIRECCIONES = setOf(Accion.ARRIBA, Accion.ABAJO, Accion.IZQ, Accion.DER, Accion.L, Accion.R)

    fun traducir(code: Int, invertir: Boolean): Accion? = when (code) {
        KeyEvent.KEYCODE_DPAD_UP -> Accion.ARRIBA
        KeyEvent.KEYCODE_DPAD_DOWN -> Accion.ABAJO
        KeyEvent.KEYCODE_DPAD_LEFT -> Accion.IZQ
        KeyEvent.KEYCODE_DPAD_RIGHT -> Accion.DER
        KeyEvent.KEYCODE_BUTTON_A -> if (invertir) Accion.B else Accion.A
        KeyEvent.KEYCODE_BUTTON_B -> if (invertir) Accion.A else Accion.B
        KeyEvent.KEYCODE_BUTTON_X -> if (invertir) Accion.Y else Accion.X
        KeyEvent.KEYCODE_BUTTON_Y -> if (invertir) Accion.X else Accion.Y
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> Accion.A
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_DEL -> Accion.B
        KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> Accion.MAS
        KeyEvent.KEYCODE_BUTTON_SELECT -> Accion.MENOS
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_PAGE_UP -> Accion.L
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_PAGE_DOWN -> Accion.R
        else -> null
    }

    fun hayMando(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }
}
