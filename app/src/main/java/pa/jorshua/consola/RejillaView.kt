package pa.jorshua.consola

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Cuadrícula de mosaicos con su nombre debajo: «Todos los juegos» o el cajón de Apps. */
@SuppressLint("ViewConstructor")
class RejillaView(private val act: RejillaActivity) : View(act) {
    var tema = Tema(true)
    var titulo = ""
    var apps = false
    var lista: List<Programa> = emptyList()
        set(v) {
            field = v
            sel = sel.coerceIn(0, max(0, v.size - 1))
            medir()
            invalidate()
        }
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

    private var sel = 0
    private val scroller = OverScroller(act)
    private var desp = 0f
    private val pistas = ArrayList<Pista>()
    private val r = RectF()
    private val dp = resources.displayMetrics.density

    private var cab = 0f
    private var pie = 0f
    private var m = 0f
    private var cols = 1
    private var lado = 0f
    private var gap = 0f
    private var celdaAlto = 0f
    private var maxDesp = 0f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = medir()

    private fun medir() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f) return
        val paisaje = w > h
        cab = if (paisaje) h * 0.14f else h * 0.09f
        pie = if (paisaje) h * 0.115f else h * 0.07f
        m = max(w * 0.06f, margen + 16 * dp)
        val deseado = if (paisaje) h * 0.24f else w * 0.27f
        gap = deseado * 0.07f
        cols = max(2, ((w - 2 * m + gap) / (deseado + gap)).toInt())
        lado = (w - 2 * m - (cols - 1) * gap) / cols
        celdaAlto = lado * 1.2f + gap
        val filas = ceil(lista.size / cols.toFloat()).toInt()
        maxDesp = max(0f, filas * celdaAlto + gap * 2 - (h - cab - pie))
        desp = desp.coerceIn(0f, maxDesp)
    }

    private fun zonaCelda(i: Int): RectF {
        val izq = m + (i % cols) * (lado + gap)
        val arriba = cab + gap * 1.5f + (i / cols) * celdaAlto - desp
        return RectF(izq, arriba, izq + lado, arriba + lado)
    }

    override fun onDraw(c: Canvas) {
        if (scroller.computeScrollOffset()) {
            desp = scroller.currY.toFloat().coerceIn(0f, maxDesp)
            postInvalidateOnAnimation()
        }
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(tema.fondo)
        val radio = lado * 0.025f

        c.save()
        c.clipRect(0f, cab, w, h - pie)
        for (i in lista.indices) {
            val z = zonaCelda(i)
            if (z.bottom + lado * 0.25f < cab || z.top > h - pie) continue
            Pinta.tile(c, z, Arte.obtener(context, lista[i], lado.toInt()) { invalidate() }, tema, radio)
            Pinta.fuente(lado * 0.095f, if (i == sel) tema.acento else tema.texto, if (i == sel) 1 else 0, Paint.Align.CENTER)
            Pinta.escribir(c, Pinta.recortar(lista[i].nombre, lado), z.centerX(), z.bottom + lado * 0.1f)
        }
        if (lista.isNotEmpty() && menu == null) Pinta.borde(c, zonaCelda(sel), radio, lado * 0.025f)
        c.restore()

        if (lista.isEmpty()) {
            Pinta.fuente(cab * 0.22f, tema.suave, 0, Paint.Align.CENTER)
            Pinta.escribir(c, if (apps) "No hay apps guardadas aquí." else "Todavía no hay juegos.", w / 2, h / 2 - cab * 0.2f)
            Pinta.escribir(c, "Elige qué va en cada lugar en Ajustes ▸ Juegos y apps.", w / 2, h / 2 + cab * 0.2f)
        }

        // Cabecera
        Pinta.relleno.color = tema.fondo
        c.drawRect(0f, 0f, w, cab, Pinta.relleno)
        val s = cab * 0.32f
        if (apps) Iconos.rejilla(c, m + s / 2, cab * 0.55f, s, tema.texto)
        else Iconos.mando(c, m + s / 2, cab * 0.55f, s * 1.2f, tema.texto, tema.fondo)
        Pinta.fuente(cab * 0.27f, tema.texto, 1)
        Pinta.escribir(c, titulo, m + s * 1.6f, cab * 0.55f)
        Pinta.fuente(cab * 0.2f, tema.suave, 0, Paint.Align.RIGHT)
        Pinta.escribir(c, "${lista.size}", w - m, cab * 0.55f)
        Pinta.trazo.strokeWidth = max(1f, dp)
        Pinta.trazo.color = tema.linea
        c.drawLine(m * 0.6f, cab, w - m * 0.6f, cab, Pinta.trazo)

        menu?.dibujar(c, w, h - pie, tema, dp)

        // Pie
        Pinta.relleno.color = tema.fondo
        c.drawRect(0f, h - pie, w, h, Pinta.relleno)
        c.drawLine(m * 0.6f, h - pie, w - m * 0.6f, h - pie, Pinta.trazo)
        pistas.clear()
        if (menu != null) {
            pistas += Pista("B", "Cerrar") { cerrarMenu() }
            pistas += Pista("A", "OK") { elegirMenu() }
        } else {
            pistas += Pista("B", "Atrás") { act.volver() }
            if (lista.isNotEmpty()) {
                pistas += Pista("X", "Opciones") { abrirMenu() }
                pistas += Pista("A", "Abrir") { abrirSel() }
            }
        }
        Pinta.pistas(c, pistas, w - m * 0.8f, h - pie / 2, pie * 0.42f, tema)

        if (activo) postInvalidateDelayed(if (Pinta.despierto()) 40 else 1000)
    }

    /** Devuelve false si la acción no se usó (B sin menú = salir). */
    fun accion(a: Accion): Boolean {
        val mn = menu
        if (mn != null) {
            when (a) {
                Accion.ARRIBA -> if (mn.mover(-1)) act.tic() else act.sonido.tocar("tope")
                Accion.ABAJO -> if (mn.mover(1)) act.tic() else act.sonido.tocar("tope")
                Accion.A -> elegirMenu()
                Accion.B, Accion.X, Accion.MAS -> cerrarMenu()
                else -> {}
            }
            invalidate()
            return true
        }
        if (lista.isEmpty()) return a != Accion.B
        val col = sel % cols
        when (a) {
            Accion.IZQ -> mover(if (col > 0) sel - 1 else sel)
            Accion.DER -> mover(if (col < cols - 1 && sel + 1 < lista.size) sel + 1 else sel)
            Accion.ARRIBA -> mover(if (sel - cols >= 0) sel - cols else sel)
            Accion.ABAJO -> mover(
                when {
                    sel + cols < lista.size -> sel + cols
                    sel / cols < (lista.size - 1) / cols -> lista.size - 1
                    else -> sel
                }
            )
            Accion.L -> mover(max(0, sel - cols * 3))
            Accion.R -> mover(minOf(lista.size - 1, sel + cols * 3))
            Accion.A -> abrirSel()
            Accion.X, Accion.MAS -> abrirMenu()
            Accion.B -> return false
            else -> {}
        }
        invalidate()
        return true
    }

    private fun mover(nuevo: Int) {
        if (nuevo == sel) {
            act.sonido.tocar("tope")
            return
        }
        sel = nuevo
        act.tic()
        asegurar()
    }

    private fun asegurar() {
        val arriba = gap * 1.5f + (sel / cols) * celdaAlto
        val visible = height - cab - pie
        var objetivo = desp
        if (arriba - gap < objetivo) objetivo = arriba - gap
        if (arriba + celdaAlto > objetivo + visible) objetivo = arriba + celdaAlto - visible
        objetivo = objetivo.coerceIn(0f, maxDesp)
        scroller.forceFinished(true)
        scroller.startScroll(0, desp.toInt(), 0, (objetivo - desp).toInt(), 200)
        invalidate()
    }

    private fun abrirSel() {
        lista.getOrNull(sel)?.let { act.abrir(it, zonaCelda(sel)) }
    }

    private fun abrirMenu() {
        lista.getOrNull(sel)?.let { menu = act.opciones(it, zonaCelda(sel)) }
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

    // ---------- Táctil ----------

    private val gestos = GestureDetector(act, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (menu == null) {
                desp = (desp + dy).coerceIn(0f, maxDesp)
                invalidate()
            }
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (menu == null) {
                scroller.fling(0, desp.toInt(), 0, -vy.toInt(), 0, 0, 0, maxDesp.toInt())
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
            val i = celdaEn(e.x, e.y)
            if (i >= 0) {
                sel = i
                abrirMenu()
            }
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        Pinta.tocado()
        return gestos.onTouchEvent(e)
    }

    private fun celdaEn(x: Float, y: Float): Int {
        if (y < cab || y > height - pie) return -1
        val col = floor((x - m) / (lado + gap)).toInt()
        val fila = floor((y - cab - gap * 1.5f + desp) / celdaAlto).toInt()
        if (col !in 0 until cols || fila < 0) return -1
        val i = fila * cols + col
        return if (i in lista.indices) i else -1
    }

    private fun tocar(x: Float, y: Float) {
        pistas.firstOrNull { it.zona.contains(x, y) }?.let {
            it.accion()
            invalidate()
            return
        }
        val mn = menu
        if (mn != null) {
            when (val i = mn.tocar(x, y)) {
                -1 -> cerrarMenu()
                -2 -> {}
                else -> {
                    mn.sel = i
                    elegirMenu()
                }
            }
            return
        }
        val i = celdaEn(x, y)
        if (i >= 0) {
            sel = i
            invalidate()
            abrirSel()
        }
    }
}
