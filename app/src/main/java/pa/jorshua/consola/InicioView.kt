package pa.jorshua.consola

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Un botón redondo de la fila de abajo. */
class Circulo(
    val nombre: String,
    val color: Int,
    val icono: (c: Canvas, cx: Float, cy: Float, s: Float, color: Int, fondo: Int) -> Unit,
    val accion: () -> Unit,
)

/**
 * El inicio: arriba el perfil, la hora y la batería; en medio la fila de juegos (el más reciente
 * primero, y al final «Todos los juegos»); debajo los botones redondos; abajo del todo las pistas.
 */
@SuppressLint("ViewConstructor")
class InicioView(private val act: InicioActivity) : View(act) {
    var tema = Tema(true)
    var juegos: List<Programa> = emptyList()
        set(v) {
            field = v
            selFila = selFila.coerceIn(0, v.size)
            limitar()
            invalidate()
        }
    var circulos: List<Circulo> = emptyList()
        set(v) {
            field = v
            selCirc = selCirc.coerceIn(0, max(0, v.size - 1))
            invalidate()
        }
    var nombre = ""
    var colorAvatar = 0
    var bateria = -1
    var cargando = false
    var wifi = -1
    var movil = false
    var mando = false
    var margen = 0f
        set(v) {
            field = v
            medir()
        }
    var activo = false
        set(v) {
            field = v
            if (v) Pinta.tocado()
            if (v) invalidate()
        }
    var menu: MenuOpciones? = null
        set(v) {
            field = v
            invalidate()
        }

    private enum class Zona { PERFIL, FILA, CIRCULOS }

    private var zona = Zona.FILA
    private var selFila = 0
    private var selCirc = 0
    private val scroller = OverScroller(act)
    private var desp = 0f
    private var arrastre = false
    private val pistas = ArrayList<Pista>()
    private val r = RectF()
    private val dp = resources.displayMetrics.density

    // Medidas (se recalculan con el tamaño de la pantalla)
    private var lado = 0f
    private var gap = 0f
    private var x0 = 0f
    private var filaY = 0f
    private var supAlto = 0f
    private var infY = 0f
    private var circR = 0f
    private var circY = 0f
    private var circSep = 0f
    private var circX = FloatArray(0)
    private var avR = 0f
    private var avX = 0f
    private var avY = 0f

    private val n get() = juegos.size + 1
    private val maxDesp get() = max(0f, n * (lado + gap) - gap - (width - 2 * x0))

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = medir()

    private fun medir() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val paisaje = w > h
        supAlto = if (paisaje) h * 0.13f else h * 0.08f
        infY = if (paisaje) h * 0.885f else h * 0.93f
        lado = if (paisaje) min(h * 0.4f, w * 0.29f) else min(w * 0.6f, h * 0.3f)
        gap = lado * 0.035f
        x0 = max(w * 0.075f, margen + 16 * dp)
        filaY = if (paisaje) h * 0.235f else h * 0.2f
        circR = if (paisaje) h * 0.056f else w * 0.072f
        circSep = circR * 0.95f
        circY = filaY + lado + (infY - filaY - lado) * 0.47f
        avR = supAlto * 0.34f
        avX = x0 + avR
        avY = supAlto * 0.58f
        limitar()
    }

    private fun limitar() {
        desp = desp.coerceIn(0f, maxDesp)
    }

    private fun zonaTile(i: Int): RectF {
        val izq = x0 + i * (lado + gap) - desp
        return RectF(izq, filaY, izq + lado, filaY + lado)
    }

    // ---------- Dibujo ----------

    override fun onDraw(c: Canvas) {
        if (scroller.computeScrollOffset()) {
            desp = scroller.currX.toFloat()
            limitar()
            postInvalidateOnAnimation()
        }
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(tema.fondo)
        superior(c, w)
        fila(c, w)
        botones(c, w)
        menu?.dibujar(c, w, infY, tema, dp)
        inferior(c, w, h)
        if (activo) postInvalidateDelayed(if (Pinta.despierto()) 40 else 1000)
    }

    private fun superior(c: Canvas, w: Float) {
        val p = Pinta.relleno
        p.color = Datos.COLORES[colorAvatar.mod(Datos.COLORES.size)]
        c.drawCircle(avX, avY, avR, p)
        Pinta.fuente(avR * 1.05f, Color.WHITE, 2, Paint.Align.CENTER)
        Pinta.escribir(c, nombre.trim().take(1).uppercase(), avX, avY)
        if (zona == Zona.PERFIL) {
            Pinta.bordeCirculo(c, avX, avY, avR, avR * 0.12f)
            Pinta.fuente(avR * 0.75f, tema.acento, 1)
            Pinta.escribir(c, nombre, avX + avR * 1.7f, avY)
        }

        // Derecha: hora · wifi · porcentaje · batería
        val s = supAlto * 0.17f
        var x = w - x0
        if (bateria >= 0) {
            x -= Iconos.bateria(c, x, avY, s, tema.texto, bateria, cargando)
            Pinta.fuente(s * 1.25f, tema.texto, 1, Paint.Align.RIGHT)
            x -= s * 0.5f
            Pinta.escribir(c, "$bateria%", x, avY)
            x -= Pinta.texto.measureText("$bateria%") + s * 1.1f
        }
        if (wifi >= 0) {
            Iconos.wifi(c, x - s * 0.8f, avY, s * 1.6f, tema.texto, tema.linea, wifi)
            x -= s * 2.6f
        } else if (movil) {
            Iconos.senal(c, x - s * 0.7f, avY, s * 1.3f, tema.texto)
            x -= s * 2.4f
        }
        Pinta.fuente(s * 1.45f, tema.texto, 1, Paint.Align.RIGHT)
        val formato = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        Pinta.escribir(c, DateFormat.format(formato, System.currentTimeMillis()).toString(), x, avY)
    }

    private fun fila(c: Canvas, w: Float) {
        val radio = lado * 0.02f
        for (i in 0 until n) {
            val izq = x0 + i * (lado + gap) - desp
            if (izq > w || izq + lado < 0) continue
            r.set(izq, filaY, izq + lado, filaY + lado)
            if (i < juegos.size) {
                Pinta.tile(c, r, Arte.obtener(context, juegos[i], lado.toInt()) { invalidate() }, tema, radio)
            } else {
                todo(c, r, radio)
            }
        }
        if (juegos.isEmpty()) {
            val tx = x0 + lado + gap * 8 - desp
            Pinta.fuente(lado * 0.065f, tema.suave, 0)
            Pinta.escribir(c, "Aún no hay juegos aquí.", tx, filaY + lado * 0.44f)
            Pinta.escribir(c, "Márcalos en Ajustes ▸ Juegos y apps,", tx, filaY + lado * 0.54f)
            Pinta.escribir(c, "o abre las opciones de una app.", tx, filaY + lado * 0.64f)
        }
        if (zona == Zona.FILA) {
            val z = zonaTile(selFila)
            Pinta.borde(c, z, radio, lado * 0.022f)
            val titulo = if (selFila < juegos.size) juegos[selFila].nombre else "Todos los juegos"
            Pinta.fuente(lado * 0.078f, tema.acento, 1, Paint.Align.CENTER)
            val t = Pinta.recortar(titulo, w - 2 * x0)
            val mitad = Pinta.texto.measureText(t) / 2
            val cx = z.centerX().coerceIn(x0 + mitad, max(x0 + mitad, w - x0 - mitad))
            Pinta.escribir(c, t, cx, filaY - lado * 0.085f)
        }
    }

    private fun todo(c: Canvas, r: RectF, radio: Float) {
        Pinta.relleno.color = tema.circulo
        c.drawRoundRect(r, radio, radio, Pinta.relleno)
        Iconos.rejilla(c, r.centerX(), r.centerY() - lado * 0.07f, lado * 0.32f, tema.texto)
        Pinta.fuente(lado * 0.062f, tema.texto, 1, Paint.Align.CENTER)
        Pinta.escribir(c, "Todos los juegos", r.centerX(), r.bottom - lado * 0.17f)
    }

    private fun botones(c: Canvas, w: Float) {
        val k = circulos.size
        if (circX.size != k) circX = FloatArray(k)
        val total = k * 2 * circR + (k - 1) * circSep
        var cx = w / 2 - total / 2 + circR
        val p = Pinta.relleno
        for ((i, ci) in circulos.withIndex()) {
            circX[i] = cx
            p.color = tema.circulo
            if (!tema.oscuro) p.setShadowLayer(circR * 0.14f, 0f, circR * 0.05f, 0x30000000)
            c.drawCircle(cx, circY, circR, p)
            p.clearShadowLayer()
            ci.icono(c, cx, circY, circR, ci.color, tema.circulo)
            if (zona == Zona.CIRCULOS && i == selCirc) {
                Pinta.bordeCirculo(c, cx, circY, circR, circR * 0.1f)
                Pinta.fuente(circR * 0.42f, tema.acento, 1, Paint.Align.CENTER)
                Pinta.escribir(c, ci.nombre, cx, circY + circR * 1.75f)
            }
            cx += 2 * circR + circSep
        }
    }

    private fun inferior(c: Canvas, w: Float, h: Float) {
        Pinta.relleno.color = tema.fondo
        c.drawRect(0f, infY, w, h, Pinta.relleno)
        Pinta.trazo.strokeWidth = max(1f, dp)
        Pinta.trazo.color = tema.linea
        c.drawLine(x0 * 0.6f, infY, w - x0 * 0.6f, infY, Pinta.trazo)
        val alto = h - infY
        val cy = infY + alto / 2
        if (mando) Iconos.mando(c, x0 + alto * 0.2f, cy, alto * 0.5f, tema.texto, tema.fondo)

        pistas.clear()
        if (menu != null) {
            pistas += Pista("B", "Cerrar") { cerrarMenu() }
            pistas += Pista("A", "OK") { elegirMenu() }
        } else when (zona) {
            Zona.FILA -> {
                if (selFila < juegos.size) pistas += Pista("X", "Opciones") { abrirMenu() }
                pistas += Pista("A", if (selFila < juegos.size) "Iniciar" else "Abrir") { activarFila() }
            }
            Zona.CIRCULOS -> pistas += Pista("A", "OK") { circulos.getOrNull(selCirc)?.accion?.invoke() }
            Zona.PERFIL -> pistas += Pista("A", "Ajustes") { act.abrirAjustes(0) }
        }
        Pinta.pistas(c, pistas, w - x0 * 0.8f, cy, alto * 0.42f, tema)
    }

    // ---------- Mando ----------

    fun accion(a: Accion) {
        val m = menu
        if (m != null) {
            when (a) {
                Accion.ARRIBA -> if (m.mover(-1)) act.tic() else act.sonido.tocar("tope")
                Accion.ABAJO -> if (m.mover(1)) act.tic() else act.sonido.tocar("tope")
                Accion.A -> elegirMenu()
                Accion.B, Accion.X, Accion.MAS -> cerrarMenu()
                else -> {}
            }
            invalidate()
            return
        }
        when (zona) {
            Zona.FILA -> when (a) {
                Accion.IZQ -> moverFila(-1)
                Accion.DER -> moverFila(1)
                Accion.L -> moverFila(-5)
                Accion.R -> moverFila(5)
                Accion.ARRIBA -> cambiar(Zona.PERFIL)
                Accion.ABAJO -> cambiar(Zona.CIRCULOS)
                Accion.A -> activarFila()
                Accion.X, Accion.MAS -> abrirMenu()
                else -> {}
            }
            Zona.CIRCULOS -> when (a) {
                Accion.IZQ -> moverCirc(-1)
                Accion.DER -> moverCirc(1)
                Accion.ARRIBA -> cambiar(Zona.FILA)
                Accion.A -> circulos.getOrNull(selCirc)?.accion?.invoke()
                else -> {}
            }
            Zona.PERFIL -> when (a) {
                Accion.ABAJO, Accion.DER -> cambiar(Zona.FILA)
                Accion.A -> act.abrirAjustes(0)
                else -> {}
            }
        }
        invalidate()
    }

    private fun cambiar(z: Zona) {
        zona = z
        act.tic()
        if (z == Zona.FILA) asegurar(selFila)
    }

    private fun moverFila(d: Int) {
        val nuevo = (selFila + d).coerceIn(0, n - 1)
        if (nuevo == selFila) {
            act.sonido.tocar("tope")
            return
        }
        selFila = nuevo
        act.tic()
        asegurar(selFila)
    }

    private fun moverCirc(d: Int) {
        val nuevo = (selCirc + d).coerceIn(0, max(0, circulos.size - 1))
        if (nuevo == selCirc) act.sonido.tocar("tope") else {
            selCirc = nuevo
            act.tic()
        }
    }

    private fun asegurar(i: Int) {
        val izq = i * (lado + gap)
        val visible = width - 2 * x0
        var objetivo = desp
        if (izq < objetivo) objetivo = izq
        if (izq + lado > objetivo + visible) objetivo = izq + lado - visible
        objetivo = objetivo.coerceIn(0f, maxDesp)
        scroller.forceFinished(true)
        scroller.startScroll(desp.toInt(), 0, (objetivo - desp).toInt(), 0, 200)
        invalidate()
    }

    private fun activarFila() {
        if (selFila < juegos.size) act.abrir(juegos[selFila], zonaTile(selFila)) else act.abrirTodo()
    }

    private fun abrirMenu() {
        if (selFila < juegos.size) menu = act.opciones(juegos[selFila], zonaTile(selFila))
    }

    private fun cerrarMenu() {
        menu = null
        act.sonido.tocar("atras")
    }

    private fun elegirMenu() {
        val f = menu?.elegida() ?: return
        menu = null
        f()
    }

    /** Al pulsar Inicio estando ya en el inicio: volver al primer juego. */
    fun alInicio() {
        menu = null
        zona = Zona.FILA
        selFila = 0
        scroller.forceFinished(true)
        scroller.startScroll(desp.toInt(), 0, -desp.toInt(), 0, 250)
        invalidate()
    }

    // ---------- Táctil ----------

    private val gestos = GestureDetector(act, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            arrastre = menu == null && e.y in filaY..(filaY + lado)
            if (arrastre) scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (arrastre) {
                desp = (desp + dx).coerceIn(0f, maxDesp)
                invalidate()
            }
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (arrastre) {
                scroller.fling(desp.toInt(), 0, -vx.toInt(), 0, 0, maxDesp.toInt(), 0, 0)
                invalidate()
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            tocar(e.x, e.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (menu != null) return
            val i = tileEn(e.x, e.y)
            if (i in juegos.indices) {
                zona = Zona.FILA
                selFila = i
                abrirMenu()
            }
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        Pinta.tocado()
        return gestos.onTouchEvent(e)
    }

    private fun tileEn(x: Float, y: Float): Int {
        if (y !in filaY..(filaY + lado)) return -1
        val pos = (x - x0 + desp) / (lado + gap)
        val i = floor(pos).toInt()
        return if (i in 0 until n && pos - i <= lado / (lado + gap)) i else -1
    }

    private fun tocar(x: Float, y: Float) {
        pistas.firstOrNull { it.zona.contains(x, y) }?.let {
            it.accion()
            invalidate()
            return
        }
        val m = menu
        if (m != null) {
            when (val i = m.tocar(x, y)) {
                -1 -> cerrarMenu()
                -2 -> {}
                else -> {
                    m.sel = i
                    elegirMenu()
                }
            }
            return
        }
        if (hypot(x - avX, y - avY) <= avR * 1.4f) {
            zona = Zona.PERFIL
            invalidate()
            act.abrirAjustes(0)
            return
        }
        val i = tileEn(x, y)
        if (i >= 0) {
            zona = Zona.FILA
            selFila = i
            invalidate()
            activarFila()
            return
        }
        for (k in circX.indices) {
            if (hypot(x - circX[k], y - circY) <= circR * 1.2f) {
                zona = Zona.CIRCULOS
                selCirc = k
                invalidate()
                circulos[k].accion()
                return
            }
        }
    }
}
