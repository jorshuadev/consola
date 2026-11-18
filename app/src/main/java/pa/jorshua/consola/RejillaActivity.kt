package pa.jorshua.consola

import android.os.Bundle
import android.view.View

/** «Todos los juegos» (modo juegos) o el cajón escondido de Apps (modo apps). */
class RejillaActivity : Pantalla() {
    private lateinit var vista: RejillaView
    override val lienzo: View get() = vista
    private val apps get() = intent.getStringExtra("modo") == "apps"

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        vista = RejillaView(this)
        vista.apps = apps
        vista.titulo = if (apps) "Apps" else "Todos los juegos"
        setContentView(vista)
        vista.setOnApplyWindowInsetsListener { _, ins ->
            val c = ins.displayCutout
            vista.margen = maxOf(c?.safeInsetLeft ?: 0, c?.safeInsetRight ?: 0).toFloat()
            ins
        }
    }

    override fun onResume() {
        super.onResume()
        prepararVentana(datos)
        vista.tema = Tema(datos.oscuro)
        recargar()
        vista.activo = true
        ocultarBarras()
    }

    override fun onPause() {
        super.onPause()
        vista.activo = false
    }

    override fun recargar() {
        val todos = Catalogo.cargar(this, datos)
        vista.lista = if (apps) {
            todos.filter { it.clase == Clase.APP }.sortedBy { it.nombre.lowercase() }
        } else {
            todos.filter { it.clase == Clase.JUEGO }.sortedByDescending { it.reciente }
        }
    }

    override fun accion(a: Accion) {
        if (!vista.accion(a) && a == Accion.B) volver()
    }

    fun volver() {
        sonido.tocar("atras")
        finish()
    }
}
