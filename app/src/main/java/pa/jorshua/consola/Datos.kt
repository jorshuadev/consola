package pa.jorshua.consola

import android.content.Context
import java.security.MessageDigest

/** Dónde vive cada programa: en el inicio (juego), en el cajón de Apps, o en ningún lado. */
enum class Clase { JUEGO, APP, OCULTA }

/** Todo lo que el usuario elige, guardado en las preferencias de la app. */
class Datos(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("consola", Context.MODE_PRIVATE)

    fun clase(pkg: String): Clase? =
        sp.getString("c_$pkg", null)?.let { n -> Clase.entries.firstOrNull { it.name == n } }

    /** null = volver a lo que se detecta solo. */
    fun fijarClase(pkg: String, c: Clase?) {
        sp.edit().apply { if (c == null) remove("c_$pkg") else putString("c_$pkg", c.name) }.apply()
    }

    fun restablecerClases() {
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("c_") }.forEach { e.remove(it) }
        e.apply()
    }

    fun ultimaVez(pkg: String) = sp.getLong("u_$pkg", 0L)
    fun marcarAbierto(pkg: String) = sp.edit().putLong("u_$pkg", System.currentTimeMillis()).apply()

    private fun b(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun pb(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()

    var nombre: String
        get() = sp.getString("nombre", null) ?: "Jugador"
        set(v) = sp.edit().putString("nombre", v).apply()
    var colorAvatar: Int
        get() = sp.getInt("avatar", 0)
        set(v) = sp.edit().putInt("avatar", v).apply()
    /** 0 horizontal, 1 girar sola, 2 vertical. */
    var orientacion: Int
        get() = sp.getInt("orientacion", 0)
        set(v) = sp.edit().putInt("orientacion", v).apply()
    var oscuro: Boolean get() = b("oscuro", true); set(v) = pb("oscuro", v)
    var pinApps: Boolean get() = b("pin_apps", true); set(v) = pb("pin_apps", v)
    var pinAjustes: Boolean get() = b("pin_ajustes", true); set(v) = pb("pin_ajustes", v)
    var botonApps: Boolean get() = b("boton_apps", true); set(v) = pb("boton_apps", v)
    var abIntercambiados: Boolean get() = b("ab", false); set(v) = pb("ab", v)
    var sonido: Boolean get() = b("sonido", true); set(v) = pb("sonido", v)
    var vibrar: Boolean get() = b("vibrar", true); set(v) = pb("vibrar", v)

    val tienePin get() = sp.getString("pin", null) != null
    fun pinCorrecto(p: String) = sp.getString("pin", null) == resumen(p)
    fun fijarPin(p: String?) {
        sp.edit().apply { if (p == null) remove("pin") else putString("pin", resumen(p)) }.apply()
    }

    private fun resumen(p: String) = MessageDigest.getInstance("SHA-256")
        .digest("consola:$p".toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        val COLORES = intArrayOf(
            0xFFE8505B.toInt(), 0xFF2D9CDB.toInt(), 0xFF27AE60.toInt(), 0xFFF2994A.toInt(),
            0xFF9B51E0.toInt(), 0xFF00B8A9.toInt(), 0xFFF06292.toInt(), 0xFF5C6BC0.toInt(),
        )
    }
}
