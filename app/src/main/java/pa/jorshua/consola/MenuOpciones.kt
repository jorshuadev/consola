package pa.jorshua.consola

import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/** Panel que entra por la derecha con las opciones de un programa. */
class MenuOpciones(private val titulo: String, private val items: List<Pair<String, () -> Unit>>) {
    var sel = 0
    private val zonas = Array(items.size) { RectF() }
    private val panel = RectF()

    fun mover(d: Int): Boolean {
        val nuevo = (sel + d).coerceIn(0, items.size - 1)
        val cambio = nuevo != sel
        sel = nuevo
        return cambio
    }

    fun elegida(): () -> Unit = items[sel].second

    fun dibujar(c: Canvas, w: Float, alto: Float, tema: Tema, dp: Float) {
        Pinta.relleno.color = 0x99000000.toInt()
        c.drawRect(0f, 0f, w, alto, Pinta.relleno)
        val ancho = min(max(w * 0.34f, 300 * dp), w * 0.92f)
        panel.set(w - ancho, 0f, w, alto)
        Pinta.relleno.color = tema.superficie
        c.drawRect(panel, Pinta.relleno)

        val m = 24 * dp
        Pinta.fuente(21 * dp, tema.texto, 1)
        Pinta.escribir(c, Pinta.recortar(titulo, ancho - 2 * m), panel.left + m, m + 14 * dp)
        Pinta.trazo.strokeWidth = dp
        Pinta.trazo.color = tema.linea
        c.drawLine(panel.left + m, m + 40 * dp, panel.right - m, m + 40 * dp, Pinta.trazo)

        var y = m + 54 * dp
        val alto1 = 54 * dp
        for ((i, it) in items.withIndex()) {
            val z = zonas[i]
            z.set(panel.left + m * 0.6f, y, panel.right - m * 0.6f, y + alto1)
            Pinta.fuente(17 * dp, tema.texto, 0)
            Pinta.escribir(c, it.first, z.left + m * 0.6f, z.centerY())
            if (i == sel) Pinta.borde(c, z, 4 * dp, 3 * dp)
            y += alto1 + 6 * dp
        }
    }

    /** -1 fuera del panel, -2 en el panel sin tocar opción, o el índice de la opción. */
    fun tocar(x: Float, y: Float): Int {
        if (!panel.contains(x, y)) return -1
        val i = zonas.indexOfFirst { it.contains(x, y) }
        return if (i >= 0) i else -2
    }
}
