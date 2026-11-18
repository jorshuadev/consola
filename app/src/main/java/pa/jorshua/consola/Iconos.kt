package pa.jorshua.consola

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Íconos planos dibujados a mano, centrados en (cx, cy) y de lado s. */
object Iconos {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ruta = Path()
    private val r = RectF()

    private fun lleno(color: Int) {
        p.style = Paint.Style.FILL
        p.color = color
    }

    private fun linea(color: Int, ancho: Float) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.strokeWidth = ancho
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
    }

    fun rejilla(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, n: Int = 3) {
        lleno(color)
        val celda = s / n
        val lado = celda * 0.74f
        val rr = lado * 0.22f
        for (i in 0 until n) for (j in 0 until n) {
            val x = cx - s / 2 + i * celda + (celda - lado) / 2
            val y = cy - s / 2 + j * celda + (celda - lado) / 2
            r.set(x, y, x + lado, y + lado)
            c.drawRoundRect(r, rr, rr, p)
        }
    }

    /** Bolsa de compras: cuerpo más ancho abajo y el asa marcada dentro, para que no parezca un candado. */
    fun bolsa(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, fondo: Int) {
        lleno(color)
        ruta.reset()
        ruta.moveTo(cx - s * 0.34f, cy - s * 0.3f)
        ruta.lineTo(cx + s * 0.34f, cy - s * 0.3f)
        ruta.lineTo(cx + s * 0.42f, cy + s * 0.4f)
        ruta.lineTo(cx - s * 0.42f, cy + s * 0.4f)
        ruta.close()
        c.drawPath(ruta, p)
        linea(fondo, s * 0.075f)
        r.set(cx - s * 0.17f, cy - s * 0.36f, cx + s * 0.17f, cy - s * 0.02f)
        c.drawArc(r, 0f, 180f, false, p)
        lleno(fondo)
        c.drawCircle(cx - s * 0.17f, cy - s * 0.19f, s * 0.05f, p)
        c.drawCircle(cx + s * 0.17f, cy - s * 0.19f, s * 0.05f, p)
    }

    fun album(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        linea(color, s * 0.085f)
        r.set(cx - s * 0.44f, cy - s * 0.36f, cx + s * 0.44f, cy + s * 0.36f)
        c.drawRoundRect(r, s * 0.08f, s * 0.08f, p)
        lleno(color)
        c.drawCircle(cx + s * 0.17f, cy - s * 0.13f, s * 0.08f, p)
        ruta.reset()
        ruta.moveTo(cx - s * 0.33f, cy + s * 0.26f)
        ruta.lineTo(cx - s * 0.1f, cy - s * 0.04f)
        ruta.lineTo(cx + s * 0.06f, cy + s * 0.15f)
        ruta.lineTo(cx + s * 0.17f, cy + s * 0.06f)
        ruta.lineTo(cx + s * 0.33f, cy + s * 0.26f)
        ruta.close()
        c.drawPath(ruta, p)
    }

    fun mando(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, fondo: Int) {
        lleno(color)
        r.set(cx - s * 0.5f, cy - s * 0.26f, cx + s * 0.5f, cy + s * 0.3f)
        c.drawRoundRect(r, s * 0.24f, s * 0.24f, p)
        lleno(fondo)
        val dx = cx - s * 0.25f
        val dy = cy + s * 0.02f
        val g = s * 0.05f
        val l = s * 0.13f
        c.drawRect(dx - l, dy - g, dx + l, dy + g, p)
        c.drawRect(dx - g, dy - l, dx + g, dy + l, p)
        c.drawCircle(cx + s * 0.2f, cy - s * 0.03f, s * 0.055f, p)
        c.drawCircle(cx + s * 0.31f, cy + s * 0.08f, s * 0.055f, p)
    }

    fun engranaje(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, fondo: Int) {
        lleno(color)
        c.drawCircle(cx, cy, s * 0.32f, p)
        for (k in 0 until 8) {
            c.save()
            c.rotate(k * 45f, cx, cy)
            r.set(cx - s * 0.085f, cy - s * 0.47f, cx + s * 0.085f, cy - s * 0.25f)
            c.drawRoundRect(r, s * 0.03f, s * 0.03f, p)
            c.restore()
        }
        lleno(fondo)
        c.drawCircle(cx, cy, s * 0.13f, p)
    }

    /** nivel: 0..3; -1 = sin conexión. */
    fun wifi(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, apagado: Int, nivel: Int) {
        val base = cy + s * 0.34f
        for (i in 1..3) {
            linea(if (i <= nivel) color else apagado, s * 0.1f)
            val rad = s * 0.22f * i
            r.set(cx - rad, base - rad, cx + rad, base + rad)
            c.drawArc(r, 225f, 90f, false, p)
        }
        lleno(if (nivel >= 0) color else apagado)
        c.drawCircle(cx, base - s * 0.02f, s * 0.07f, p)
    }

    fun senal(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        lleno(color)
        for (i in 0 until 4) {
            val alto = s * (0.25f + i * 0.25f)
            val x = cx - s * 0.5f + i * s * 0.28f
            r.set(x, cy + s * 0.5f - alto, x + s * 0.18f, cy + s * 0.5f)
            c.drawRoundRect(r, s * 0.04f, s * 0.04f, p)
        }
    }

    /** Batería que termina en x = derecha. Devuelve su ancho. */
    fun bateria(c: Canvas, derecha: Float, cy: Float, s: Float, color: Int, nivel: Int, cargando: Boolean): Float {
        val w = s * 1.85f
        val izq = derecha - w
        val cuerpo = derecha - s * 0.18f
        linea(color, s * 0.1f)
        r.set(izq, cy - s / 2, cuerpo, cy + s / 2)
        c.drawRoundRect(r, s * 0.16f, s * 0.16f, p)
        lleno(color)
        r.set(derecha - s * 0.12f, cy - s * 0.2f, derecha, cy + s * 0.2f)
        c.drawRoundRect(r, s * 0.05f, s * 0.05f, p)
        val mg = s * 0.18f
        val ancho = (cuerpo - izq - 2 * mg) * nivel.coerceIn(0, 100) / 100f
        lleno(
            when {
                cargando -> 0xFF3DDC84.toInt()
                nivel <= 15 -> 0xFFFF4D4D.toInt()
                else -> color
            }
        )
        r.set(izq + mg, cy - s / 2 + mg, izq + mg + ancho, cy + s / 2 - mg)
        c.drawRoundRect(r, s * 0.06f, s * 0.06f, p)
        return w
    }
}
