package pa.jorshua.consola

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Clics cortos de la interfaz, sintetizados aquí mismo (no hay archivos de audio). */
class Sonido(private val datos: Datos) {
    private val tasa = 44100
    private val pistas = HashMap<String, AudioTrack>()

    init {
        runCatching {
            pistas["mover"] = crear(listOf(1900.0 to 16), 0.16)
            pistas["abrir"] = crear(listOf(1000.0 to 40, 1500.0 to 70), 0.2)
            pistas["atras"] = crear(listOf(1400.0 to 35, 900.0 to 60), 0.18)
            pistas["menu"] = crear(listOf(1250.0 to 28, 1700.0 to 40), 0.16)
            pistas["tope"] = crear(listOf(230.0 to 55), 0.3)
        }
    }

    private fun crear(notas: List<Pair<Double, Int>>, vol: Double): AudioTrack {
        val total = notas.sumOf { it.second } * tasa / 1000
        val muestras = ShortArray(total)
        var i = 0
        for ((f, ms) in notas) {
            val n = ms * tasa / 1000
            for (k in 0 until n) {
                val t = k.toDouble() / tasa
                val env = exp(-t * 1000.0 / ms * 4.0) * min(1.0, k / (tasa * 0.002))
                val v = (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t)) * env * vol / 1.3
                muestras[i++] = (v * Short.MAX_VALUE).toInt().toShort()
            }
        }
        val pista = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(tasa).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(total * 2)
            .build()
        pista.write(muestras, 0, total)
        return pista
    }

    fun tocar(nombre: String) {
        if (!datos.sonido) return
        val p = pistas[nombre] ?: return
        runCatching {
            if (p.playState == AudioTrack.PLAYSTATE_PLAYING) p.stop()
            p.reloadStaticData()
            p.play()
        }
    }

    fun soltar() = pistas.values.forEach { it.release() }
}
