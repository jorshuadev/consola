package pa.jorshua.consola

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.PI
import kotlin.math.cos

/** Colores de los dos temas: oscuro y claro. */
class Tema(val oscuro: Boolean) {
    val fondo = if (oscuro) 0xFF141821.toInt() else 0xFFF4F1EA.toInt()
    val superficie = if (oscuro) 0xFF1E2430.toInt() else 0xFFFFFFFF.toInt()
    val texto = if (oscuro) 0xFFF2EFE8.toInt() else 0xFF1C2230.toInt()
    val suave = if (oscuro) 0xFF9AA3B2.toInt() else 0xFF6B7280.toInt()
    val linea = if (oscuro) 0xFF2C3442.toInt() else 0xFFD9D3C7.toInt()
    val circulo = if (oscuro) 0xFF252C3A.toInt() else Color.WHITE
    val vacio = if (oscuro) 0xFF222937.toInt() else 0xFFE6E0D4.toInt()
    val acento = if (oscuro) 0xFFFFB13B.toInt() else 0xFFD9822B.toInt()
}

/** Un botón de la barra de abajo («Ⓐ Iniciar»). También se puede tocar. */
class Pista(val letra: String, val texto: String, val accion: () -> Unit) {
    val zona = RectF()
}

object Pinta {
    val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
    val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val texto = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val imagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val normal: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val medio: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val negrita = Typeface.create("sans-serif", Typeface.BOLD)
    private val ruta = Path()
    private val tmp = RectF()

    fun mezcla(a: Int, b: Int, f: Float): Int = Color.argb(
        (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * f).toInt(),
        (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt(),
    )

    /** Última vez que el usuario hizo algo: tras 30 s quieto el marco deja de latir (ahorra batería). */
    private var ultimoToque = 0L
    fun despierto() = SystemClock.uptimeMillis() - ultimoToque < 30_000
    fun tocado() {
        ultimoToque = SystemClock.uptimeMillis()
    }

    /** Color del marco de selección: late despacio entre dos tonos ámbar. */
    fun pulso(): Int {
        if (!despierto()) return 0xFFFFB13B.toInt()
        val t = (SystemClock.uptimeMillis() % 2400L) / 2400f
        return mezcla(0xFFF29A1F.toInt(), 0xFFFFD27A.toInt(), ((1 - cos(t * 2 * PI)) / 2).toFloat())
    }

    fun tile(c: Canvas, r: RectF, bmp: Bitmap?, tema: Tema, radio: Float) {
        if (bmp == null) {
            relleno.color = tema.vacio
            c.drawRoundRect(r, radio, radio, relleno)
            return
        }
        c.save()
        ruta.reset()
        ruta.addRoundRect(r, radio, radio, Path.Direction.CW)
        c.clipPath(ruta)
        c.drawBitmap(bmp, null, r, imagen)
        c.restore()
    }

    fun borde(c: Canvas, r: RectF, radio: Float, grosor: Float) {
        tmp.set(r)
        tmp.inset(-grosor * 1.2f, -grosor * 1.2f)
        trazo.strokeWidth = grosor
        trazo.color = pulso()
        c.drawRoundRect(tmp, radio + grosor, radio + grosor, trazo)
    }

    fun bordeCirculo(c: Canvas, cx: Float, cy: Float, r: Float, grosor: Float) {
        trazo.strokeWidth = grosor
        trazo.color = pulso()
        c.drawCircle(cx, cy, r + grosor * 1.2f, trazo)
    }

    /** peso: 0 normal, 1 medio, 2 negrita. */
    fun fuente(tamano: Float, color: Int, peso: Int = 1, alin: Paint.Align = Paint.Align.LEFT): TextPaint {
        texto.textSize = tamano
        texto.color = color
        texto.textAlign = alin
        texto.typeface = when (peso) { 0 -> normal; 2 -> negrita; else -> medio }
        return texto
    }

    fun recortar(s: String, ancho: Float): String =
        TextUtils.ellipsize(s, texto, ancho, TextUtils.TruncateAt.END).toString()

    /** Escribe con el centro vertical del texto en cy. */
    fun escribir(c: Canvas, s: String, x: Float, cy: Float) {
        c.drawText(s, x, cy - (texto.descent() + texto.ascent()) / 2, texto)
    }

    fun letra(c: Canvas, l: String, cx: Float, cy: Float, r: Float, tema: Tema) {
        relleno.color = tema.texto
        c.drawCircle(cx, cy, r, relleno)
        fuente(r * 1.2f, tema.fondo, 2, Paint.Align.CENTER)
        escribir(c, l, cx, cy)
    }

    /** Dibuja las pistas de derecha a izquierda y deja en cada una su zona táctil. */
    fun pistas(c: Canvas, lista: List<Pista>, derecha: Float, cy: Float, alto: Float, tema: Tema) {
        var x = derecha
        val r = alto * 0.36f
        for (p in lista.asReversed()) {
            fuente(alto * 0.5f, tema.texto, 1)
            val fin = x
            x -= texto.measureText(p.texto)
            escribir(c, p.texto, x, cy)
            x -= r * 0.55f + r
            letra(c, p.letra, x, cy, r, tema)
            x -= r
            p.zona.set(x - r * 0.4f, cy - alto, fin + r * 0.4f, cy + alto)
            x -= alto * 0.9f
        }
    }
}
