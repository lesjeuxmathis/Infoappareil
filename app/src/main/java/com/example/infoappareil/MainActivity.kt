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
        addSection("📱 Appareil", getDeviceInfo()) { getDeviceDetails() }
        addSection("⚙️ Processeur (CPU)", getCpuInfo()) { getCpuDetails() }
        addSection("💾 Mémoire (RAM)", getRamInfo()) { getRamDetails() }
        addSection("🗄️ Stockage", getStorageInfo()) { getStorageDetails() }
        addSection("🔋 Batterie", getBatteryInfo()) { getBatteryDetails() }
        addSection("🖥️ Écran", getDisplayInfo()) { getDisplayDetails() }
        addSection("🧭 Capteurs", getSensorsInfo()) { getSensorsDetails() }
        addSection("📷 Caméras", getCameraInfo()) { getCameraDetails() }
        addSection("📶 Réseau & Connectivité", getNetworkInfo()) { getNetworkDetails() }
    }

    // ---------------------------------------------------------------------
    // Construction de l'interface
    // ---------------------------------------------------------------------

    private fun addSection(
        title: String,
        items: List<Pair<String, String>>,
        detailsProvider: () -> List<Pair<String, String>>
    ) {
        val colorAccent = ContextCompat.getColor(this, R.color.accent)
        val colorSurface = ContextCompat.getColor(this, R.color.surface_card)
        val colorTextPrimary = ContextCompat.getColor(this, R.color.text_primary)
        val colorTextSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val colorDivider = ContextCompat.getColor(this, R.color.divider)

        // "Focus block" façon One UI : grands coins arrondis + fond qui contraste
        // toujours avec l'arrière-plan, en clair comme en sombre.
        val card = MaterialCardView(this).apply {
            radius = dp(26f)
            cardElevation = dp(0f)
            setCardBackgroundColor(colorSurface)
            strokeWidth = 0
            useCompatPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(16f).toInt() }
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f).toInt(), dp(18f).toInt(), dp(20f).toInt(), dp(18f).toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(this).apply {
            text = title
            setTextColor(colorAccent)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        headerRow.addView(TextView(this).apply {
            text = "ⓘ"
            textSize = 18f
            setTextColor(colorAccent)
            gravity = Gravity.CENTER
            setPadding(dp(10f).toInt(), dp(4f).toInt(), dp(10f).toInt(), dp(4f).toInt())
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            this@MainActivity.theme.resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless, outValue, true
            )
            setBackgroundResource(outValue.resourceId)
            contentDescription = "Plus de détails sur $title"
            setOnClickListener { showDetailsDialog(title, detailsProvider()) }
        })
        inner.addView(headerRow)

        inner.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1f).toInt()
            ).also {
                it.topMargin = dp(8f).toInt()
                it.bottomMargin = dp(8f).toInt()
            }
            setBackgroundColor(colorDivider)
        })

        if (items.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = "Aucune information disponible"
                setTextColor(colorTextSecondary)
                textSize = 14f
            })
        }

        for ((label, value) in items) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6f).toInt(), 0, dp(6f).toInt())
            }
            row.addView(TextView(this).apply {
                text = label
                setTextColor(colorTextSecondary)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this).apply {
                text = value
                setTextColor(colorTextPrimary)
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

    private fun showDetailsDialog(title: String, details: List<Pair<String, String>>) {
        val colorTextPrimary = ContextCompat.getColor(this, R.color.text_primary)
        val colorTextSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val colorDivider = ContextCompat.getColor(this, R.color.divider)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f).toInt(), dp(8f).toInt(), dp(24f).toInt(), dp(8f).toInt())
        }

        val content = if (details.isEmpty()) {
            listOf("Détails" to "Aucun détail supplémentaire disponible.")
        } else {
            details
        }

        for ((label, value) in content) {
            layout.addView(TextView(this).apply {
                text = label
                setTextColor(colorTextSecondary)
                textSize = 12f
                setPadding(0, dp(10f).toInt(), 0, dp(2f).toInt())
            })
            layout.addView(TextView(this).apply {
                text = value
                setTextColor(colorTextPrimary)
                textSize = 15f
            })
            layout.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1f).toInt()
                ).also { it.topMargin = dp(10f).toInt() }
                setBackgroundColor(colorDivider)
            })
        }

        val scrollView = android.widget.ScrollView(this).apply { addView(layout) }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Fermer", null)
            .show()
    }

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

    private fun getDeviceDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        list.add("Nom de code Android" to Build.VERSION.CODENAME)
        list.add("Version incrémentale" to Build.VERSION.INCREMENTAL)
        list.add("Type de build" to Build.TYPE)
        list.add("Tags de build" to Build.TAGS)
        list.add("Bootloader" to Build.BOOTLOADER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add("Fabricant du SoC" to Build.SOC_MANUFACTURER)
            list.add("Modèle du SoC" to Build.SOC_MODEL)
        } else {
            list.add("SoC" to "Nécessite Android 12+")
        }
        try {
            val date = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm", java.util.Locale.FRANCE
            ).format(java.util.Date(Build.TIME))
            list.add("Date de build" to date)
        } catch (e: Exception) {
            list.add("Date de build" to "Inconnue")
        }
        list.add("Hôte de compilation" to Build.HOST)
        list.add("Empreinte complète (fingerprint)" to Build.FINGERPRINT)
        return list
    }

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

    private fun getCpuDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        try {
            val cpuInfo = java.io.File("/proc/cpuinfo").readText()
            Regex("Hardware\\s*:\\s*(.+)").find(cpuInfo)?.groupValues?.get(1)?.trim()?.let {
                list.add("Matériel (Hardware)" to it)
            }
            Regex("(?:model name|Processor)\\s*:\\s*(.+)").find(cpuInfo)?.groupValues?.get(1)?.trim()?.let {
                list.add("Modèle du processeur" to it)
            }
        } catch (e: Exception) {
            // /proc/cpuinfo non lisible sur cet appareil, on ignore.
        }

        val cores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cores) {
            try {
                val minKHz = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                    .readText().trim().toLong()
                val maxKHz = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                    .readText().trim().toLong()
                list.add("Cœur $i" to "${minKHz / 1000} – ${maxKHz / 1000} MHz")
            } catch (e: Exception) {
                list.add("Cœur $i" to "Fréquence non accessible sur cet appareil")
            }
        }

        if (list.isEmpty()) list.add("Détails CPU" to "Non accessibles sur cet appareil")
        return list
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

    private fun getRamDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val meminfo = java.io.File("/proc/meminfo").readText()
            fun grab(key: String): String? {
                val m = Regex("$key:\\s*(\\d+)\\s*kB").find(meminfo) ?: return null
                return formatBytes(m.groupValues[1].toLong() * 1024)
            }
            grab("MemAvailable")?.let { list.add("Mémoire disponible (noyau)" to it) }
            grab("Cached")?.let { list.add("Cache" to it) }
            grab("Buffers")?.let { list.add("Tampons (buffers)" to it) }
            grab("SwapTotal")?.let { list.add("Swap total" to it) }
            grab("SwapFree")?.let { list.add("Swap libre" to it) }
        } catch (e: Exception) {
            list.add("Détails noyau" to "Non accessibles sur cet appareil")
        }

        val rt = Runtime.getRuntime()
        list.add("Tas Java max (cette app)" to formatBytes(rt.maxMemory()))
        list.add("Tas Java alloué (cette app)" to formatBytes(rt.totalMemory()))
        list.add("Tas Java libre (cette app)" to formatBytes(rt.freeMemory()))
        return list
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

    private fun getStorageDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
            for ((index, vol) in sm.storageVolumes.withIndex()) {
                val desc = vol.getDescription(this)
                val type = if (vol.isPrimary) "Principal" else "Secondaire"
                val kind = if (vol.isRemovable) "Amovible" else "Interne"
                list.add("Volume #${index + 1} : $desc" to "$type · $kind · État : ${vol.state}")
            }
        } catch (e: Exception) {
            list.add("Volumes" to "Non accessibles sur cet appareil")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ssm = getSystemService(Context.STORAGE_STATS_SERVICE) as android.app.usage.StorageStatsManager
                val stats = ssm.queryStatsForUid(StorageManager.UUID_DEFAULT, android.os.Process.myUid())
                list.add("Données de cette app" to formatBytes(stats.dataBytes))
                list.add("Cache de cette app" to formatBytes(stats.cacheBytes))
                list.add("APK de cette app" to formatBytes(stats.appBytes))
            }
        } catch (e: Exception) {
            list.add("Stockage de l'app" to "Non accessible")
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

    private fun getBatteryDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (capacity in 0..100) list.add("Capacité (service système)" to "$capacity %")

            val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (chargeCounter > 0) list.add("Charge restante" to "${chargeCounter / 1000} mAh")

            val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            list.add("Courant instantané" to "${currentNow / 1000} mA")

            val currentAvg = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            list.add("Courant moyen" to "${currentAvg / 1000} mA")

            val energy = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (energy > 0) list.add("Énergie restante" to "${energy / 1_000_000} mWh")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                list.add("En charge (service système)" to if (bm.isCharging) "Oui" else "Non")
            }
        } catch (e: Exception) {
            list.add("Détails avancés" to "Non disponibles sur cet appareil")
        }
        if (list.isEmpty()) list.add("Détails avancés" to "Non disponibles sur cet appareil")
        return list
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

    @Suppress("DEPRECATION")
    private fun getDisplayDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay

            for (mode in display.supportedModes) {
                list.add(
                    "Mode ${mode.modeId}" to
                        "${mode.physicalWidth} x ${mode.physicalHeight} @ ${mode.refreshRate.roundToInt()} Hz"
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hdrTypes = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
                val hdrNames = hdrTypes.mapNotNull {
                    when (it) {
                        android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                        else -> null
                    }
                }
                list.add("HDR supporté" to if (hdrNames.isEmpty()) "Aucun" else hdrNames.joinToString(", "))
            }

            val rotation = when (display.rotation) {
                android.view.Surface.ROTATION_0 -> "0°"
                android.view.Surface.ROTATION_90 -> "90°"
                android.view.Surface.ROTATION_180 -> "180°"
                android.view.Surface.ROTATION_270 -> "270°"
                else -> "Inconnue"
            }
            list.add("Rotation actuelle" to rotation)

            val realMetrics = android.util.DisplayMetrics()
            display.getRealMetrics(realMetrics)
            list.add("Résolution physique réelle" to "${realMetrics.widthPixels} x ${realMetrics.heightPixels} px")
        } catch (e: Exception) {
            list.add("Détails avancés" to "Non disponibles")
        }
        return list
    }

    private fun getSensorsInfo(): List<Pair<String, String>> {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        if (sensors.isEmpty()) return listOf("Capteurs" to "Aucun détecté")
        return sensors.map { it.name to it.vendor }
    }

    private fun getSensorsDetails(): List<Pair<String, String>> {
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        if (sensors.isEmpty()) return listOf("Capteurs" to "Aucun détecté")

        return sensors.map { s ->
            val wake = if (s.isWakeUpSensor) "Oui" else "Non"
            val details = "Fournisseur : ${s.vendor}\n" +
                "Puissance : ${s.power} mA\n" +
                "Portée maximale : ${s.maximumRange}\n" +
                "Résolution : ${s.resolution}\n" +
                "Délai minimal : ${s.minDelay} µs\n" +
                "Capteur de réveil (wake-up) : $wake"
            s.name to details
        }
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

    private fun getCameraDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in cm.cameraIdList) {
                val c = cm.getCameraCharacteristics(id)
                val orientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: -1
                val pixelArray = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val level = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limité"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Complet"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Niveau 3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "Externe"
                    else -> "Inconnu"
                }
                val flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val focalLengths = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.joinToString(", ") { "$it mm" } ?: "Inconnues"
                val apertures = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                    ?.joinToString(", ") { "f/$it" } ?: "Inconnues"
                val ois = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = ois?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } == true

                val details = "Résolution capteur : ${pixelArray?.width ?: "?"} x ${pixelArray?.height ?: "?"}\n" +
                    "Orientation capteur : $orientation°\n" +
                    "Niveau matériel : $level\n" +
                    "Flash disponible : ${if (flash) "Oui" else "Non"}\n" +
                    "Distances focales : $focalLengths\n" +
                    "Ouvertures : $apertures\n" +
                    "Stabilisation optique : ${if (hasOis) "Oui" else "Non"}"
                list.add("Caméra $id" to details)
            }
        } catch (e: Exception) {
            list.add("Détails caméras" to "Non disponibles")
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

    private fun getNetworkDetails(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()

        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val info = wm.connectionInfo
            list.add("Vitesse de liaison Wi-Fi" to "${info.linkSpeed} Mbps")
            list.add("Fréquence Wi-Fi" to "${info.frequency} MHz")
            list.add("Puissance du signal (RSSI)" to "${info.rssi} dBm")
        } catch (e: Exception) {
            list.add("Détails Wi-Fi" to "Non disponibles (Wi-Fi désactivé ou non connecté)")
        }

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (caps != null) {
                list.add("Débit descendant estimé" to "${caps.linkDownstreamBandwidthKbps / 1000} Mbps")
                list.add("Débit montant estimé" to "${caps.linkUpstreamBandwidthKbps / 1000} Mbps")
                list.add(
                    "Connexion validée (Internet)" to
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Oui" else "Non"
                )
                list.add(
                    "Connexion mesurée (data limitée)" to
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) "Non" else "Oui"
                )
            }
        } catch (e: Exception) {
            // ignoré
        }

        if (list.isEmpty()) list.add("Détails réseau" to "Non disponibles")
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
