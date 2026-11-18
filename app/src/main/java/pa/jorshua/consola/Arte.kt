package pa.jorshua.consola

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.LruCache
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Convierte el ícono de cada app en un mosaico cuadrado a sangre, como la portada de un juego:
 * los íconos adaptativos ocupan todo el cuadro; los viejos van sobre un fondo de su color.
 */
object Arte {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 5).toInt()) {
        override fun sizeOf(k: String, v: Bitmap) = v.byteCount
    }
    private val pendientes = HashSet<String>()
    private val fallidos = HashSet<String>()
    private val hilos = Executors.newFixedThreadPool(2)
    private val principal = Handler(Looper.getMainLooper())

    /** Devuelve el mosaico si ya está; si no, lo prepara aparte y llama a listo(). */
    fun obtener(ctx: Context, p: Programa, lado: Int, listo: () -> Unit): Bitmap? {
        if (lado <= 0) return null
        val clave = "${p.comp.flattenToShortString()}@$lado"
        cache.get(clave)?.let { return it }
        if (clave !in fallidos && pendientes.add(clave)) {
            val dens = max(ctx.resources.displayMetrics.densityDpi, DisplayMetrics.DENSITY_XXXHIGH)
            hilos.execute {
                val b = try {
                    quitarBandeja(pintar(original(ctx, p, dens) ?: p.info.getIcon(dens), lado))
                } catch (e: Exception) { null }
                principal.post {
                    pendientes.remove(clave)
                    if (b != null) cache.put(clave, b) else fallidos.add(clave)
                    listo()
                }
            }
        }
        return null
    }

    fun olvidar(pkg: String?) {
        if (pkg == null) {
            cache.evictAll()
            fallidos.clear()
            return
        }
        cache.snapshot().keys.filter { it.startsWith("$pkg/") }.forEach { cache.remove(it) }
        fallidos.removeAll { it.startsWith("$pkg/") }
    }

    /**
     * El ícono tal como lo trae la app, leído de sus recursos. Así no pasa por la «bandeja»
     * con que Samsung (y otras capas) envuelve y encoge los íconos.
     */
    private fun original(ctx: Context, p: Programa, dens: Int): Drawable? = try {
        val pm = ctx.packageManager
        val ai = p.info.applicationInfo
        val id = pm.getActivityInfo(p.comp, 0).iconResource.takeIf { it != 0 } ?: ai.icon
        if (id == 0) null else pm.getResourcesForApplication(ai).getDrawableForDensity(id, dens, null)
    } catch (e: Exception) {
        null
    }

    /**
     * Red de seguridad por si el ícono llega envuelto igual: si alrededor del dibujo hay un
     * relleno de un solo color (la bandeja blanca) y adentro un bloque de arte, se recorta a
     * ese bloque y se agranda hasta llenar el cuadro.
     */
    private fun quitarBandeja(b: Bitmap): Bitmap {
        val n = b.width
        val px = IntArray(n * n).also { b.getPixels(it, 0, n, 0, 0, n, n) }
        fun en(x: Int, y: Int) = px[y.coerceIn(0, n - 1) * n + x.coerceIn(0, n - 1)]

        // Color del relleno: el medio de cada lado, un poco hacia adentro
        val m = (n * 0.05f).toInt()
        val muestras = ArrayList<Int>()
        for (k in 0 until 9) {
            val t = (n * (0.3f + 0.4f * k / 8f)).toInt()
            muestras += en(t, m); muestras += en(t, n - 1 - m); muestras += en(m, t); muestras += en(n - 1 - m, t)
        }
        if (muestras.any { it ushr 24 < 230 }) return b
        val relleno = Color.rgb(
            muestras.sumOf { Color.red(it) } / muestras.size,
            muestras.sumOf { Color.green(it) } / muestras.size,
            muestras.sumOf { Color.blue(it) } / muestras.size,
        )
        if (muestras.any { distancia(it, relleno) > 36 }) return b
        // Las bandejas son blancas o grises; un fondo de color es parte del arte
        val r = Color.red(relleno); val g = Color.green(relleno); val a = Color.blue(relleno)
        if (maxOf(r, g, a) - minOf(r, g, a) > 20) return b

        // Caja del arte que se distingue del relleno. Solo se mira por dentro de la bandeja:
        // en sus curvas asoma el fondo del mosaico y engañaría la medida
        val esq = (n * 0.1f).toInt()
        var x0 = n; var y0 = n; var x1 = -1; var y1 = -1
        for (y in esq until n - esq) for (x in esq until n - esq) {
            val p = px[y * n + x]
            if (p ushr 24 > 200 && distancia(p, relleno) > 70) {
                if (x < x0) x0 = x
                if (x > x1) x1 = x
                if (y < y0) y0 = y
                if (y > y1) y1 = y
            }
        }
        if (x1 < 0) return b
        val caja = Rect(x0, y0, x1 + 1, y1 + 1)
        val prop = caja.width().toFloat() / caja.height()
        if (caja.width() > n * 0.78f || prop !in 0.85f..1.18f) return b

        // El bloque de adentro debe ser macizo (no un logo suelto sobre fondo liso)
        var total = 0
        var distintos = 0
        for (k in 0 until 16) {
            val f = 0.25f + 0.5f * k / 15f
            val x = (caja.left + caja.width() * f).toInt()
            val y = (caja.top + caja.height() * f).toInt()
            val mi = maxOf(2, caja.width() / 25)
            for (p in intArrayOf(en(x, caja.top + mi), en(x, caja.bottom - 1 - mi), en(caja.left + mi, y), en(caja.right - 1 - mi, y))) {
                total++
                if (distancia(p, relleno) > 50) distintos++
            }
        }
        if (distintos < total * 0.92f) return b

        // Si el bloque tiene esquinas redondeadas, se acerca un poco para que no asomen
        val redondo = distancia(en(caja.left + 1, caja.top + 1), relleno) < 50
        if (redondo) caja.inset((caja.width() * 0.08f).toInt(), (caja.height() * 0.08f).toInt())
        val salida = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        Canvas(salida).drawBitmap(b, caja, Rect(0, 0, n, n), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return salida
    }

    private fun distancia(a: Int, b: Int) =
        kotlin.math.abs(Color.red(a) - Color.red(b)) + kotlin.math.abs(Color.green(a) - Color.green(b)) +
            kotlin.math.abs(Color.blue(a) - Color.blue(b))

    /**
     * Pinta el mosaico a sangre. Se mide dónde está el dibujo de verdad (sin márgenes
     * transparentes) para que llene el cuadro aunque la app traiga el ícono pequeño o
     * el sistema lo haya envuelto con relleno (pasa en Samsung y Xiaomi).
     */
    private fun pintar(d: Drawable, lado: Int): Bitmap {
        val b = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val destino = RectF(0f, 0f, lado.toFloat(), lado.toFloat())
        val pincel = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        if (d is AdaptiveIconDrawable) {
            // Capa a capa en un lienzo de 108 dp donde los 72 dp visibles miden «lado»
            val n = (lado * 108f / 72f).roundToInt()
            val fondo = capa(d.background, n)
            val frente = capa(d.foreground, n) ?: Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
            val px = IntArray(n * n).also { frente.getPixels(it, 0, n, 0, 0, n, n) }
            val caja = caja(px, n)
            // Solo si cabe dentro de lo visible: un arte a sangre de 108 dp se trata normal
            if (caja != null && caja.width() < n * 0.66f && bordeLleno(px, n, caja)) {
                // Ícono viejo envuelto: el cuadro de adentro es el arte completo
                if (fondo != null) c.drawBitmap(fondo, null, destino, pincel)
                c.drawBitmap(frente, Rect(caja), destino, pincel)
                return b
            }
            val visible = n * 72f / 108f
            val lad = if (caja == null) visible else minOf(visible, maxOf(caja.width(), caja.height()) / 0.8f)
            val cx = (caja?.exactCenterX() ?: (n / 2f)).coerceIn(lad / 2, n - lad / 2)
            val cy = (caja?.exactCenterY() ?: (n / 2f)).coerceIn(lad / 2, n - lad / 2)
            val origen = Rect(
                (cx - lad / 2).roundToInt(), (cy - lad / 2).roundToInt(),
                (cx + lad / 2).roundToInt(), (cy + lad / 2).roundToInt(),
            )
            if (fondo != null) c.drawBitmap(fondo, origen, destino, pincel)
            else c.drawColor(Pinta.mezcla(promedio(px), Color.WHITE, 0.6f))
            c.drawBitmap(frente, origen, destino, pincel)
            return b
        }

        val img = capa(d, lado, 0) ?: return b
        val px = IntArray(lado * lado).also { img.getPixels(it, 0, lado, 0, 0, lado, lado) }
        val caja = caja(px, lado) ?: return b
        val base = promedio(px)
        if (bordeLleno(px, lado, caja)) {
            // Ícono cuadrado o redondeado: se recortan los márgenes y llena todo
            c.drawColor(base)
            c.drawBitmap(img, caja, destino, pincel)
            return b
        }
        // Forma suelta (círculo, logo): sobre un fondo de su color, ocupando el 78 %
        c.drawColor(Pinta.mezcla(base, Color.WHITE, 0.6f))
        val esc = lado * 0.78f / maxOf(caja.width(), caja.height())
        val w = caja.width() * esc
        val h = caja.height() * esc
        c.drawBitmap(img, caja, RectF((lado - w) / 2, (lado - h) / 2, (lado + w) / 2, (lado + h) / 2), pincel)
        return b
    }

    /** Dibuja una capa en un cuadro de n×n. «margen» extiende los bordes (las capas adaptativas miden 108). */
    private fun capa(d: Drawable?, n: Int, margen: Int = 0): Bitmap? {
        d ?: return null
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        d.setBounds(-margen, -margen, n + margen, n + margen)
        d.draw(Canvas(b))
        return b
    }

    /** El rectángulo que ocupa el dibujo (pixeles no transparentes), o null si está vacío. */
    private fun caja(px: IntArray, n: Int): Rect? {
        var x0 = n; var y0 = n; var x1 = -1; var y1 = -1
        for (y in 0 until n) {
            val fila = y * n
            for (x in 0 until n) {
                if (px[fila + x] ushr 24 > 24) {
                    if (x < x0) x0 = x
                    if (x > x1) x1 = x
                    if (y < y0) y0 = y
                    if (y > y1) y1 = y
                }
            }
        }
        return if (x1 < 0) null else Rect(x0, y0, x1 + 1, y1 + 1)
    }

    /**
     * ¿El dibujo es un bloque lleno hasta sus bordes? Se mira el tramo central de cada lado
     * (las esquinas redondeadas no cuentan).
     */
    private fun bordeLleno(px: IntArray, n: Int, r: Rect): Boolean {
        val m = maxOf(1, r.width() / 30)
        val ini = 0.2f
        val fin = 0.8f
        var total = 0
        var llenos = 0
        fun ver(x: Int, y: Int) {
            total++
            if (px[y.coerceIn(0, n - 1) * n + x.coerceIn(0, n - 1)] ushr 24 > 220) llenos++
        }
        for (k in 0 until 24) {
            val f = ini + (fin - ini) * k / 23f
            val x = (r.left + r.width() * f).toInt()
            val y = (r.top + r.height() * f).toInt()
            ver(x, r.top + m); ver(x, r.bottom - 1 - m)
            ver(r.left + m, y); ver(r.right - 1 - m, y)
        }
        // Además debe ser casi cuadrado: un logo alargado no se estira
        val prop = r.width().toFloat() / r.height()
        return llenos >= total * 0.95f && prop in 0.85f..1.18f
    }

    private fun promedio(px: IntArray): Int {
        var rs = 0L; var gs = 0L; var bs = 0L; var k = 0
        var i = 0
        while (i < px.size) {
            val p = px[i]
            if (p ushr 24 > 200) {
                rs += Color.red(p); gs += Color.green(p); bs += Color.blue(p); k++
            }
            i += 7
        }
        return if (k > 0) Color.rgb((rs / k).toInt(), (gs / k).toInt(), (bs / k).toInt()) else Color.GRAY
    }
}