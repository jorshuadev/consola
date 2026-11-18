package pa.jorshua.consola

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast

fun Activity.prepararVentana(datos: Datos) {
    requestedOrientation = when (datos.orientacion) {
        1 -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        2 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
    window.attributes = window.attributes.also {
        it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

/** Pantalla completa, como una consola; las barras de Android salen deslizando desde el borde. */
fun Activity.ocultarBarras() {
    if (Build.VERSION.SDK_INT >= 30) {
        window.insetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

fun Activity.abrirIntent(i: Intent): Boolean = try {
    startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}

fun estiloDialogo(datos: Datos) =
    if (datos.oscuro) android.R.style.Theme_DeviceDefault_Dialog_Alert
    else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

fun Activity.pedirTexto(datos: Datos, titulo: String, inicial: String, numerico: Boolean, ok: (String) -> Unit) {
    val campo = EditText(this).apply {
        setText(inicial)
        setSelection(inicial.length)
        isSingleLine = true
        inputType = if (numerico) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
    val d = resources.displayMetrics.density
    val marco = FrameLayout(this).apply {
        setPadding((24 * d).toInt(), (8 * d).toInt(), (24 * d).toInt(), 0)
        addView(campo)
    }
    val dialogo = AlertDialog.Builder(this, estiloDialogo(datos))
        .setTitle(titulo)
        .setView(marco)
        .setPositiveButton("Aceptar") { _, _ -> ok(campo.text.toString()) }
        .setNegativeButton("Cancelar", null)
        .create()
    dialogo.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    dialogo.show()
    campo.requestFocus()
}

/** Si hay PIN y esta parte lo pide, lo pregunta antes de seguir. */
fun Activity.conPin(datos: Datos, requerido: Boolean, ok: () -> Unit) {
    if (!requerido || !datos.tienePin) {
        ok()
        return
    }
    pedirTexto(datos, "Escribe el PIN", "", true) { p ->
        if (datos.pinCorrecto(p)) ok() else Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
    }
}

/** Base de las pantallas dibujadas (inicio y rejillas): mando, sonidos y opciones de cada programa. */
abstract class Pantalla : Activity() {
    lateinit var datos: Datos
    lateinit var sonido: Sonido
    abstract val lienzo: View
    abstract fun recargar()
    abstract fun accion(a: Accion)

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        datos = Datos(this)
        sonido = Sonido(datos)
        prepararVentana(datos)
    }

    override fun onDestroy() {
        super.onDestroy()
        sonido.soltar()
    }

    override fun onWindowFocusChanged(foco: Boolean) {
        super.onWindowFocusChanged(foco)
        if (foco) ocultarBarras()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val a = Mando.traducir(e.keyCode, datos.abIntercambiados) ?: return super.dispatchKeyEvent(e)
        Pinta.tocado()
        if (e.action == KeyEvent.ACTION_DOWN && (e.repeatCount == 0 || a in Mando.DIRECCIONES)) accion(a)
        return true
    }

    fun tic() {
        sonido.tocar("mover")
        if (datos.vibrar) lienzo.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun abrir(p: Programa, zona: RectF?) {
        sonido.tocar("abrir")
        datos.marcarAbierto(p.pkg)
        val r = zona?.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
        val opciones = r?.let {
            ActivityOptions.makeScaleUpAnimation(lienzo, it.left, it.top, it.width(), it.height()).toBundle()
        }
        try {
            getSystemService(LauncherApps::class.java).startMainActivity(p.comp, p.info.user, r, opciones)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir ${p.nombre}", Toast.LENGTH_SHORT).show()
        }
    }

    fun opciones(p: Programa, zona: RectF?): MenuOpciones {
        sonido.tocar("menu")
        val esJuego = p.clase == Clase.JUEGO
        return MenuOpciones(
            p.nombre, listOf(
                "Abrir" to { abrir(p, zona) },
                (if (esJuego) "Mover a Apps" else "Mover a Juegos") to {
                    val nueva = if (esJuego) Clase.APP else Clase.JUEGO
                    datos.fijarClase(p.pkg, if (nueva == p.auto) null else nueva)
                    recargar()
                },
                "Ocultar" to {
                    datos.fijarClase(p.pkg, Clase.OCULTA)
                    recargar()
                    Toast.makeText(this, "Oculta. Se recupera en Ajustes ▸ Juegos y apps", Toast.LENGTH_LONG).show()
                },
                "Información de la app" to {
                    abrirIntent(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${p.pkg}")))
                },
                "Desinstalar" to { abrirIntent(Intent(Intent.ACTION_DELETE, Uri.parse("package:${p.pkg}"))) },
            )
        )
    }
}
