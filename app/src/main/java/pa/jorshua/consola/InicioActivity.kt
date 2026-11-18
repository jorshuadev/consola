package pa.jorshua.consola

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.hardware.input.InputManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.view.View

class InicioActivity : Pantalla() {
    private lateinit var vista: InicioView
    override val lienzo: View get() = vista
    private val principal = Handler(Looper.getMainLooper())
    private var bateriaRx: BroadcastReceiver? = null
    private var redCb: ConnectivityManager.NetworkCallback? = null

    private val paquetes = object : LauncherApps.Callback() {
        override fun onPackageAdded(p: String, u: UserHandle) = cambio(p)
        override fun onPackageRemoved(p: String, u: UserHandle) = cambio(p)
        override fun onPackageChanged(p: String, u: UserHandle) = cambio(p)
        override fun onPackagesAvailable(ps: Array<out String>, u: UserHandle, r: Boolean) = cambio(null)
        override fun onPackagesUnavailable(ps: Array<out String>, u: UserHandle, r: Boolean) = cambio(null)
    }

    private val mandos = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(id: Int) { vista.mando = Mando.hayMando() }
        override fun onInputDeviceRemoved(id: Int) { vista.mando = Mando.hayMando() }
        override fun onInputDeviceChanged(id: Int) { vista.mando = Mando.hayMando() }
    }

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        vista = InicioView(this)
        setContentView(vista)
        vista.setOnApplyWindowInsetsListener { _, ins ->
            val c = ins.displayCutout
            vista.margen = maxOf(c?.safeInsetLeft ?: 0, c?.safeInsetRight ?: 0).toFloat()
            ins
        }
        getSystemService(LauncherApps::class.java).registerCallback(paquetes, principal)
        getSystemService(InputManager::class.java).registerInputDeviceListener(mandos, principal)
    }

    override fun onResume() {
        super.onResume()
        prepararVentana(datos)
        vista.tema = Tema(datos.oscuro)
        vista.nombre = datos.nombre
        vista.colorAvatar = datos.colorAvatar
        vista.mando = Mando.hayMando()
        vista.circulos = circulos()
        recargar()
        escuchar()
        vista.activo = true
        ocultarBarras()
    }

    override fun onPause() {
        super.onPause()
        vista.activo = false
        dejarDeEscuchar()
    }

    override fun onDestroy() {
        getSystemService(LauncherApps::class.java).unregisterCallback(paquetes)
        getSystemService(InputManager::class.java).unregisterInputDeviceListener(mandos)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vista.alInicio()
    }

    @Deprecated("El inicio no se cierra con Atrás")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        vista.accion(Accion.B)
    }

    override fun accion(a: Accion) = vista.accion(a)

    override fun recargar() {
        vista.juegos = Catalogo.cargar(this, datos)
            .filter { it.clase == Clase.JUEGO }
            .sortedByDescending { it.reciente }
            .take(12)
    }

    private fun cambio(pkg: String?) {
        Arte.olvidar(pkg)
        recargar()
    }

    private fun circulos(): List<Circulo> {
        val lista = ArrayList<Circulo>()
        if (datos.botonApps) lista += Circulo("Apps", BOTON, { c, x, y, s, col, _ ->
            Iconos.rejilla(c, x, y, s * 0.8f, col)
        }) { abrirApps() }
        lista += Circulo("Tienda", BOTON, { c, x, y, s, col, f ->
            Iconos.bolsa(c, x, y, s * 0.95f, col, f)
        }) { abrirTienda() }
        lista += Circulo("Capturas", BOTON, { c, x, y, s, col, _ ->
            Iconos.album(c, x, y, s * 1.05f, col)
        }) { abrirAlbum() }
        lista += Circulo("Controles", BOTON, { c, x, y, s, col, f ->
            Iconos.mando(c, x, y, s * 1.1f, col, f)
        }) {
            sonido.tocar("abrir")
            abrirIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        lista += Circulo("Ajustes", BOTON, { c, x, y, s, col, f ->
            Iconos.engranaje(c, x, y, s * 1.0f, col, f)
        }) { abrirAjustes(0) }
        return lista
    }

    fun abrirTodo() {
        sonido.tocar("abrir")
        startActivity(Intent(this, RejillaActivity::class.java).putExtra("modo", "juegos"))
    }

    private fun abrirApps() {
        sonido.tocar("abrir")
        conPin(datos, datos.pinApps) {
            startActivity(Intent(this, RejillaActivity::class.java).putExtra("modo", "apps"))
        }
    }

    private fun abrirTienda() {
        sonido.tocar("abrir")
        conPin(datos, datos.pinApps) {
            val web = Uri.parse("https://play.google.com/store/games")
            if (!abrirIntent(Intent(Intent.ACTION_VIEW, web).setPackage("com.android.vending"))) {
                abrirIntent(Intent(Intent.ACTION_VIEW, web))
            }
        }
    }

    private fun abrirAlbum() {
        sonido.tocar("abrir")
        val galeria = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)
        if (!abrirIntent(galeria)) abrirIntent(Intent(Intent.ACTION_VIEW).setType("image/*"))
    }

    fun abrirAjustes(cat: Int) {
        sonido.tocar("abrir")
        conPin(datos, datos.pinAjustes) {
            startActivity(Intent(this, AjustesActivity::class.java).putExtra("cat", cat))
        }
    }

    // ---------- Batería y red para la barra de arriba ----------

    private fun escuchar() {
        val rx = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = leerBateria(i)
        }
        registerReceiver(rx, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { leerBateria(it) }
        bateriaRx = rx

        val cm = getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(net: Network, caps: NetworkCapabilities) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    val rssi = caps.signalStrength
                    vista.wifi = when {
                        rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 3
                        rssi > -60 -> 3
                        rssi > -72 -> 2
                        rssi > -84 -> 1
                        else -> 0
                    }
                    vista.movil = false
                } else {
                    vista.wifi = -1
                    vista.movil = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            }

            override fun onLost(net: Network) {
                vista.wifi = -1
                vista.movil = false
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb, principal) }
        redCb = cb
    }

    private fun dejarDeEscuchar() {
        bateriaRx?.let { runCatching { unregisterReceiver(it) } }
        bateriaRx = null
        redCb?.let { cb -> runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(cb) } }
        redCb = null
    }

    private fun leerBateria(i: Intent) {
        val nivel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val estado = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        vista.bateria = if (nivel >= 0 && escala > 0) nivel * 100 / escala else -1
        vista.cargando = estado == BatteryManager.BATTERY_STATUS_CHARGING || estado == BatteryManager.BATTERY_STATUS_FULL
    }
}

/** Todos los botones redondos comparten el color de acento de la marca. */
private const val BOTON = 0xFFFFB13B.toInt()
