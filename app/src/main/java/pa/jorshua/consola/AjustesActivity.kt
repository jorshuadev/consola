package pa.jorshua.consola

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Ajustes al estilo de la consola: categorías a la izquierda, opciones a la derecha. */
class AjustesActivity : Activity() {
    private lateinit var datos: Datos
    private lateinit var tema: Tema
    private lateinit var sonido: Sonido
    private lateinit var contenido: LinearLayout
    private lateinit var scroll: ScrollView
    private val cats = ArrayList<TextView>()
    private var actual = -1
    private var filtro = 0
    private val nombres = arrayOf(
        "Perfil", "Tema", "Juegos y apps", "Control parental", "Controles y sonido", "Pantalla", "Sistema",
    )
    private val dp get() = resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).roundToInt()

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        datos = Datos(this)
        tema = Tema(datos.oscuro)
        sonido = Sonido(datos)
        prepararVentana(datos)
        setContentView(construir())
        val cat = estado?.getInt("cat") ?: intent.getIntExtra("cat", 0)
        mostrar(cat)
        cats[cat].requestFocus()
    }

    override fun onSaveInstanceState(s: Bundle) {
        super.onSaveInstanceState(s)
        s.putInt("cat", actual)
    }

    override fun onWindowFocusChanged(foco: Boolean) {
        super.onWindowFocusChanged(foco)
        if (foco) ocultarBarras()
    }

    override fun onDestroy() {
        super.onDestroy()
        sonido.soltar()
    }

    /** A pulsa lo enfocado y B vuelve, respetando el intercambio de A/B. */
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BUTTON_A || e.keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            if (e.action == KeyEvent.ACTION_UP) {
                if (Mando.traducir(e.keyCode, datos.abIntercambiados) == Accion.A) currentFocus?.performClick()
                else volver()
            }
            return true
        }
        if (e.keyCode == KeyEvent.KEYCODE_BACK || e.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (e.action == KeyEvent.ACTION_UP) volver()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun volver() {
        sonido.tocar("atras")
        finish()
    }

    // ---------- Armazón ----------

    private fun construir(): View {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(tema.fondo)
        }
        raiz.setOnApplyWindowInsetsListener { v, ins ->
            val c = ins.displayCutout
            v.setPadding(c?.safeInsetLeft ?: 0, 0, c?.safeInsetRight ?: 0, 0)
            ins
        }

        val izq = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(28), px(22), px(12), px(14))
        }
        izq.addView(TextView(this).apply {
            text = "Ajustes"
            textSize = 26f
            setTextColor(tema.texto)
            typeface = Pinta.medio
            setPadding(px(14), 0, 0, px(18))
        })
        nombres.forEachIndexed { i, n ->
            val t = TextView(this).apply {
                text = n
                textSize = 17f
                setTextColor(tema.texto)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(16), 0, px(12), 0)
                isFocusable = true
                isClickable = true
                background = fondoFoco()
                setOnFocusChangeListener { _, f ->
                    if (f && actual != i) {
                        sonido.tocar("mover")
                        mostrar(i)
                    }
                }
                setOnClickListener {
                    if (actual != i) mostrar(i)
                    filas().firstOrNull()?.requestFocus()
                }
            }
            izq.addView(t, LinearLayout.LayoutParams(-1, px(50)).apply { bottomMargin = px(4) })
            cats += t
        }
        izq.addView(Space(this), LinearLayout.LayoutParams(0, 0, 1f))
        izq.addView(TextView(this).apply {
            text = "‹  Volver"
            textSize = 16f
            setTextColor(tema.suave)
            setPadding(px(16), px(10), px(16), px(10))
            setOnClickListener { volver() }
        })
        raiz.addView(izq, LinearLayout.LayoutParams(0, -1, 0.34f))

        raiz.addView(View(this).apply { setBackgroundColor(tema.linea) },
            LinearLayout.LayoutParams(px(1), -1).apply { topMargin = px(24); bottomMargin = px(24) })

        scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(28), px(24), px(36), px(36))
        }
        scroll.addView(contenido)
        raiz.addView(scroll, LinearLayout.LayoutParams(0, -1, 0.66f))
        return raiz
    }

    private fun fondoFoco() = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply {
            setColor(tema.superficie)
            setStroke(px(3), tema.acento)
            cornerRadius = px(4).toFloat()
        })
        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
            setColor(tema.superficie)
            cornerRadius = px(4).toFloat()
        })
        addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    }

    private fun filas(): List<View> =
        (0 until contenido.childCount).map { contenido.getChildAt(it) }.filter { it.isFocusable }

    private fun mostrar(i: Int) {
        actual = i
        cats.forEachIndexed { k, t ->
            t.setTextColor(if (k == i) tema.acento else tema.texto)
            t.typeface = if (k == i) Pinta.medio else Pinta.normal
        }
        contenido.removeAllViews()
        scroll.scrollTo(0, 0)
        contenido.addView(TextView(this).apply {
            text = nombres[i]
            textSize = 22f
            setTextColor(tema.texto)
            typeface = Pinta.medio
            setPadding(px(14), 0, 0, px(12))
        })
        when (i) {
            0 -> perfil()
            1 -> apariencia()
            2 -> juegosYApps()
            3 -> parental()
            4 -> controles()
            5 -> pantalla()
            else -> sistema()
        }
    }

    /** Una fila enfocable: título a la izquierda, valor en azul a la derecha. Devuelve el valor. */
    private fun fila(
        titulo: String,
        valor: String = "",
        icono: ((ImageView) -> Unit)? = null,
        alPulsar: (TextView) -> Unit,
    ): TextView {
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            background = fondoFoco()
            setPadding(px(14), 0, px(16), 0)
        }
        if (icono != null) {
            val iv = ImageView(this)
            f.addView(iv, LinearLayout.LayoutParams(px(38), px(38)).apply { marginEnd = px(14) })
            icono(iv)
        }
        f.addView(TextView(this).apply {
            text = titulo
            textSize = 17f
            setTextColor(tema.texto)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val v = TextView(this).apply {
            text = valor
            textSize = 16f
            setTextColor(tema.acento)
            typeface = Pinta.medio
            setPadding(px(12), 0, 0, 0)
        }
        f.addView(v)
        f.setOnClickListener {
            sonido.tocar("abrir")
            alPulsar(v)
        }
        f.setOnKeyListener { _, code, e ->
            if (code == KeyEvent.KEYCODE_DPAD_LEFT && e.action == KeyEvent.ACTION_DOWN) {
                cats[actual].requestFocus()
                true
            } else false
        }
        contenido.addView(f, LinearLayout.LayoutParams(-1, px(60)))
        contenido.addView(View(this).apply { setBackgroundColor(tema.linea) }, LinearLayout.LayoutParams(-1, 1))
        return v
    }

    private fun nota(s: String) {
        contenido.addView(TextView(this).apply {
            text = s
            textSize = 14f
            setTextColor(tema.suave)
            setPadding(px(14), px(12), px(14), px(12))
            setLineSpacing(0f, 1.15f)
        })
    }

    private fun siNo(b: Boolean) = if (b) "Sí" else "No"
    private fun aviso(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ---------- Secciones ----------

    private fun perfil() {
        fila("Nombre", datos.nombre) { v ->
            pedirTexto(datos, "Tu nombre", datos.nombre, false) { s ->
                if (s.isNotBlank()) {
                    datos.nombre = s.trim()
                    v.text = datos.nombre
                }
            }
        }
        val color = fila("Color del ícono", "⬤") { v ->
            datos.colorAvatar = (datos.colorAvatar + 1) % Datos.COLORES.size
            v.setTextColor(Datos.COLORES[datos.colorAvatar])
        }
        color.setTextColor(Datos.COLORES[datos.colorAvatar.mod(Datos.COLORES.size)])
        color.textSize = 22f
    }

    private fun apariencia() {
        fila("Tema", if (datos.oscuro) "Oscuro" else "Claro") {
            datos.oscuro = !datos.oscuro
            intent.putExtra("cat", actual)
            recreate()
        }
    }

    private fun juegosYApps() {
        nota("Juego: sale en el inicio.   App: queda guardada en el cajón de Apps.   Oculta: no sale en ningún lado.\nPulsa una app para cambiarla. Lo que dice «auto» lo detectó Consola sola.")
        val filtros = arrayOf("Todas", "Solo juegos", "Solo apps", "Solo ocultas")
        fila("Mostrar", filtros[filtro]) {
            filtro = (filtro + 1) % filtros.size
            mostrar(actual)
            filas().firstOrNull()?.requestFocus()
        }
        fila("Volver todo a lo automático", "") {
            AlertDialog.Builder(this, estiloDialogo(datos))
                .setTitle("¿Volver a lo automático?")
                .setMessage("Se olvidan los cambios: los juegos vuelven al inicio y nada queda oculto.")
                .setPositiveButton("Sí") { _, _ ->
                    datos.restablecerClases()
                    mostrar(actual)
                }
                .setNegativeButton("No", null)
                .show()
        }
        val lado = px(38)
        Catalogo.cargar(this, datos)
            .distinctBy { it.pkg }
            .filter {
                when (filtro) {
                    1 -> it.clase == Clase.JUEGO
                    2 -> it.clase == Clase.APP
                    3 -> it.clase == Clase.OCULTA
                    else -> true
                }
            }
            .sortedBy { it.nombre.lowercase() }
            .forEach { p ->
                val v = fila(p.nombre, etiqueta(p), icono = { iv ->
                    Arte.obtener(this, p, lado) { Arte.obtener(this, p, lado) {}?.let { iv.setImageBitmap(it) } }
                        ?.let { iv.setImageBitmap(it) }
                }) { v ->
                    val sig = when (p.clase) {
                        Clase.JUEGO -> Clase.APP
                        Clase.APP -> Clase.OCULTA
                        Clase.OCULTA -> Clase.JUEGO
                    }
                    datos.fijarClase(p.pkg, if (sig == p.auto) null else sig)
                    p.clase = sig
                    v.text = etiqueta(p)
                    v.setTextColor(if (sig == Clase.OCULTA) tema.suave else tema.acento)
                }
                if (p.clase == Clase.OCULTA) v.setTextColor(tema.suave)
            }
    }

    private fun etiqueta(p: Programa): String {
        val base = when (p.clase) {
            Clase.JUEGO -> "Juego"
            Clase.APP -> "App"
            Clase.OCULTA -> "Oculta"
        }
        return if (p.clase == p.auto) "$base · auto" else base
    }

    private fun parental() {
        fila("PIN", if (datos.tienePin) "Activado" else "Sin PIN") { v ->
            if (!datos.tienePin) {
                nuevoPin { v.text = "Activado" }
            } else {
                AlertDialog.Builder(this, estiloDialogo(datos))
                    .setTitle("PIN")
                    .setItems(arrayOf("Cambiar el PIN", "Quitar el PIN")) { _, w ->
                        if (w == 0) nuevoPin {} else {
                            datos.fijarPin(null)
                            v.text = "Sin PIN"
                            aviso("PIN quitado")
                        }
                    }
                    .show()
            }
        }
        fila("Pedir PIN para Apps y Tienda", siNo(datos.pinApps)) { v ->
            datos.pinApps = !datos.pinApps
            v.text = siNo(datos.pinApps)
        }
        fila("Pedir PIN para entrar a Ajustes", siNo(datos.pinAjustes)) { v ->
            datos.pinAjustes = !datos.pinAjustes
            v.text = siNo(datos.pinAjustes)
        }
        fila("Botón Apps en el inicio", if (datos.botonApps) "Visible" else "Escondido") { v ->
            datos.botonApps = !datos.botonApps
            v.text = if (datos.botonApps) "Visible" else "Escondido"
        }
        fila("Abrir el cajón de Apps", "›") {
            startActivity(Intent(this, RejillaActivity::class.java).putExtra("modo", "apps"))
        }
        nota("Con el botón Apps escondido, el cajón solo se abre desde aquí. " +
            "Consola no puede bloquear la barra de notificaciones ni las apps recientes de Android: " +
            "para un bloqueo total, combínalo con «Fijar aplicación» o con Family Link.")
    }

    private fun nuevoPin(ok: () -> Unit) {
        pedirTexto(datos, "PIN nuevo (4 a 8 números)", "", true) { s ->
            if (s.length in 4..8 && s.all { it.isDigit() }) {
                datos.fijarPin(s)
                aviso("PIN guardado")
                ok()
            } else aviso("El PIN debe tener de 4 a 8 números")
        }
    }

    private fun controles() {
        fila("Botón para aceptar", if (datos.abIntercambiados) "B" else "A") { v ->
            datos.abIntercambiados = !datos.abIntercambiados
            v.text = if (datos.abIntercambiados) "B" else "A"
        }
        fila("Sonidos", siNo(datos.sonido)) { v ->
            datos.sonido = !datos.sonido
            v.text = siNo(datos.sonido)
        }
        fila("Vibración", siNo(datos.vibrar)) { v ->
            datos.vibrar = !datos.vibrar
            v.text = siNo(datos.vibrar)
        }
        fila("Emparejar un control", "›") { abrirIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        nota("Sirve cualquier control Bluetooth o USB, y también el teclado (flechas, Enter = A, Esc = B). " +
            "Si tu control tiene la A y la B al revés, cambia «Botón para aceptar».\n" +
            "En la pantalla: toca para abrir y mantén pulsado para ver las opciones.")
    }

    private fun pantalla() {
        val ops = arrayOf("Horizontal", "Girar sola", "Vertical")
        fila("Orientación", ops[datos.orientacion.coerceIn(0, 2)]) { v ->
            datos.orientacion = (datos.orientacion + 1) % ops.size
            v.text = ops[datos.orientacion]
            prepararVentana(datos)
        }
    }

    private fun sistema() {
        fila("Usar Consola como pantalla de inicio", "›") {
            if (!abrirIntent(Intent(Settings.ACTION_HOME_SETTINGS))) abrirIntent(Intent(Settings.ACTION_SETTINGS))
        }
        fila("Ajustes de Android", "›") { abrirIntent(Intent(Settings.ACTION_SETTINGS)) }
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "?"
        fila("Versión", version) {}
    }
}
