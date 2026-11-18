package pa.jorshua.consola

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import kotlin.math.max

class Programa(val info: LauncherActivityInfo, val auto: Clase, var clase: Clase, val reciente: Long) {
    val comp: ComponentName = info.componentName
    val pkg: String = comp.packageName
    val nombre: String = info.label.toString()
}

/** Lee las apps instaladas y decide cuáles son juegos. */
object Catalogo {
    /** Emuladores: no se declaran juegos, pero en una consola es donde van. */
    private val EMULADORES = setOf(
        "com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32",
        "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold",
        "org.dolphinemu.dolphinemu", "com.github.stenzek.duckstation",
        "xyz.aethersx2.android",
        "io.github.lime3ds.android", "io.github.azahar_emu.azahar",
        "com.dsemu.drastic", "me.magnum.melonds", "com.swordfish.lemuroid",
        "org.mupen64plusae.v3.fzurita", "org.mupen64plusae.v3.alpha",
        "com.fastemulator.gba", "com.fastemulator.gbc",
        "it.dbtecno.pizzaboypro", "it.dbtecno.pizzaboygbapro",
        "com.explusalpha.Snes9xPlus", "com.explusalpha.GbaEmu",
        "com.explusalpha.MdEmu", "com.explusalpha.NesEmu",
        "info.cemu.cemu", "org.vita3k.emulator", "net.rpcsx",
    )

    fun cargar(ctx: Context, datos: Datos): List<Programa> {
        val la = ctx.getSystemService(LauncherApps::class.java)
        return la.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != ctx.packageName }
            .map { i ->
                val ai = i.applicationInfo
                @Suppress("DEPRECATION")
                val juego = ai.category == ApplicationInfo.CATEGORY_GAME ||
                    ai.flags and ApplicationInfo.FLAG_IS_GAME != 0 ||
                    ai.packageName in EMULADORES
                val auto = if (juego) Clase.JUEGO else Clase.APP
                Programa(
                    i, auto, datos.clase(ai.packageName) ?: auto,
                    max(datos.ultimaVez(ai.packageName), i.firstInstallTime),
                )
            }
    }
}
