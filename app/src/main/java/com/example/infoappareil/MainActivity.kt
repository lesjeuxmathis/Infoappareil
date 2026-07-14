package com.example.infoappareil

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.telephony.TelephonyManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    // Enregistrement du lanceur de permissions (doit être fait avant STARTED)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        buildAllSections()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        container = findViewById(R.id.sectionsContainer)

        val toRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        } else {
            buildAllSections()
        }
    }

    private fun buildAllSections() {
        container.removeAllViews()
        addSection("📱 Appareil", getDeviceInfo())
        addSection("⚙️ Processeur (CPU)", getCpuInfo())
        addSection("💾 Mémoire (RAM)", getRamInfo())
        addSection("🗄️ Stockage", getStorageInfo())
        addSection("🔋 Batterie", getBatteryInfo())
        addSection("🖥️ Écran", getDisplayInfo())
        addSection("🧭 Capteurs", getSensorsInfo())
        addSection("📷 Caméras", getCameraInfo())
        addSection("📶 Réseau & Connectivité", getNetworkInfo())
    }

    // ---------------------------------------------------------------------
    // Construction de l'interface
    // ---------------------------------------------------------------------

    private fun addSection(title: String, items: List<Pair<String, String>>) {
        val card = MaterialCardView(this).apply {
            radius = dp(12f)
            cardElevation = dp(3f)
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(12f).toInt() }
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f).toInt(), dp(14f).toInt(), dp(16f).toInt(), dp(14f).toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        inner.addView(TextView(this).apply {
            text = title
            setTextColor(0xFF3700B3.toInt())
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        inner.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1f).toInt()
            ).also {
                it.topMargin = dp(6f).toInt()
                it.bottomMargin = dp(6f).toInt()
            }
            setBackgroundColor(0xFFE0E0E0.toInt())
        })

        if (items.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = "Aucune information disponible"
                setTextColor(0xFF757575.toInt())
                textSize = 14f
            })
        }

        for ((label, value) in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(4f).toInt(), 0, dp(4f).toInt())
            }
            row.addView(TextView(this).apply {
                text = label
                setTextColor(0xFF757575.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = value
                setTextColor(0xFF212121.toInt())
                textSize = 14f
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            inner.addView(row)
        }

        card.addView(inner)
        container.addView(card)
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    // ---------------------------------------------------------------------
    // Collecte des informations
    // ---------------------------------------------------------------------

    private fun getDeviceInfo(): List<Pair<String, String>> = listOf(
        "Fabricant" to Build.MANUFACTURER,
        "Marque" to Build.BRAND,
        "Modèle" to Build.MODEL,
        "Nom de l'appareil" to Build.DEVICE,
        "Produit" to Build.PRODUCT,
        "Carte mère" to Build.BOARD,
        "Matériel" to Build.HARDWARE,
        "Version Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Build ID" to Build.ID,
        "Patch de sécurité" to (Build.VERSION.SECURITY_PATCH ?: "Inconnu")
    )

    private fun getCpuInfo(): List<Pair<String, String>> {
        val cores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val arch = System.getProperty("os.arch") ?: "Inconnu"
        return listOf(
            "Cœurs logiques" to cores.toString(),
            "Architecture" to arch,
            "ABIs supportées" to abis
        )
    }

    private fun getRamInfo(): List<Pair<String, String>> {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return listOf(
            "RAM totale" to formatBytes(info.totalMem),
            "RAM disponible" to formatBytes(info.availMem),
            "Seuil mémoire faible" to formatBytes(info.threshold),
            "Mémoire faible" to if (info.lowMemory) "Oui" else "Non"
        )
    }

    private fun getStorageInfo(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val internal = StatFs(Environment.getDataDirectory().path)
        val internalTotal = internal.blockCountLong * internal.blockSizeLong
        val internalFree = internal.availableBlocksLong * internal.blockSizeLong
        list.add("Stockage interne total" to formatBytes(internalTotal))
        list.add("Stockage interne libre" to formatBytes(internalFree))

        try {
            val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumes = sm.storageVolumes
            var sdIndex = 1
            for (vol in volumes) {
                if (vol.isRemovable) {
                    list.add("Carte SD #$sdIndex" to (vol.getDescription(this)))
                    sdIndex++
                }
            }
        } catch (e: Exception) {
            // Pas de volume amovible détectable, on ignore.
        }
        return list
    }

    private fun getBatteryInfo(): List<Pair<String, String>> {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, filter)
            ?: return listOf("Batterie" to "Indisponible")

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "En charge"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "En décharge"
            BatteryManager.BATTERY_STATUS_FULL -> "Pleine"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Ne charge pas"
            else -> "Inconnu"
        }

        val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Bonne"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surchauffe"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Morte"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Surtension"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Défaillance"
            BatteryManager.BATTERY_HEALTH_COLD -> "Froide"
            else -> "Inconnue"
        }

        val tech = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Inconnue"
        val temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        return listOf(
            "Niveau" to "$pct %",
            "État" to statusStr,
            "Santé" to healthStr,
            "Technologie" to tech,
            "Température" to if (temp >= 0) "${temp / 10.0} °C" else "Inconnue",
            "Tension" to if (voltage >= 0) "$voltage mV" else "Inconnue"
        )
    }

    @Suppress("DEPRECATION")
    private fun getDisplayInfo(): List<Pair<String, String>> {
        val metrics = resources.displayMetrics
        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        val xInches = widthPx / metrics.xdpi
        val yInches = heightPx / metrics.ydpi
        val diagonal = sqrt((xInches * xInches + yInches * yInches).toDouble())

        val refreshRate = try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.refreshRate
        } catch (e: Exception) {
            0f
        }

        return listOf(
            "Résolution" to "$widthPx x $heightPx px",
            "Densité" to "$densityDpi dpi (x${metrics.density})",
            "Taille estimée" to "${String.format("%.2f", diagonal)} pouces",
            "Taux de rafraîchissement" to "${refreshRate.roundToInt()} Hz"
        )
    }

    private fun getSensorsInfo(): List<Pair<String, String>> {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        if (sensors.isEmpty()) return listOf("Capteurs" to "Aucun détecté")
        return sensors.map { it.name to it.vendor }
    }

    private fun getCameraInfo(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Avant"
                    CameraCharacteristics.LENS_FACING_BACK -> "Arrière"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externe"
                    else -> "Inconnu"
                }
                list.add("Caméra $id" to facing)
            }
        } catch (e: Exception) {
            list.add("Caméras" to "Indisponibles")
        }
        if (list.isEmpty()) list.add("Caméras" to "Aucune détectée")
        return list
    }

    private fun getNetworkInfo(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> "Aucune connexion"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Autre"
        }
        list.add("Connexion active" to type)

        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            list.add("Opérateur" to (tm.networkOperatorName.takeIf { it.isNotEmpty() } ?: "Inconnu"))
            val simState = when (tm.simState) {
                TelephonyManager.SIM_STATE_READY -> "Prête"
                TelephonyManager.SIM_STATE_ABSENT -> "Absente"
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN requis"
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK requis"
                else -> "Inconnu"
            }
            list.add("État SIM" to simState)
            val phoneType = when (tm.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_NONE -> "Aucun"
                else -> "Inconnu"
            }
            list.add("Type de téléphonie" to phoneType)
        } catch (e: Exception) {
            list.add("Infos SIM" to "Indisponibles")
        }

        try {
            val hasBtPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
            val adapter = BluetoothAdapter.getDefaultAdapter()
            when {
                adapter == null -> list.add("Bluetooth" to "Non disponible")
                !hasBtPermission -> list.add("Bluetooth" to "Permission refusée")
                adapter.isEnabled -> list.add("Bluetooth" to "Activé (${adapter.name ?: "?"})")
                else -> list.add("Bluetooth" to "Désactivé")
            }
        } catch (e: Exception) {
            list.add("Bluetooth" to "Indisponible")
        }

        return list
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "Ko", "Mo", "Go", "To")
        var value = bytes.toDouble()
        var i = 0
        while (value >= 1024 && i < units.size - 1) {
            value /= 1024
            i++
        }
        return String.format("%.2f %s", value, units[i])
    }
}
