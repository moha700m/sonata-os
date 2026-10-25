package sa.moha.sonataos

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sa.moha.sonataos.data.AppSettings
import sa.moha.sonataos.media.MediaHub
import sa.moha.sonataos.obd.ObdStatus
import sa.moha.sonataos.prayer.PrayerCalc
import sa.moha.sonataos.sys.Apps
import sa.moha.sonataos.ui.*
import sa.moha.sonataos.voice.LlmHook
import sa.moha.sonataos.voice.VoiceCommand
import sa.moha.sonataos.voice.VoiceCommandParser
import sa.moha.sonataos.voice.VoiceInput
import java.util.Calendar
import java.util.Date

class MainActivity : ComponentActivity(), Actions {
    private val vm: MainViewModel by viewModels()
    private lateinit var voice: VoiceInput
    private var tab by mutableStateOf(Tab.HOME)
    private var lastTouch by mutableLongStateOf(System.currentTimeMillis())
    private var screensaverForced by mutableStateOf(false)
    private var showPicker by mutableStateOf(false)
    private val found = mutableStateListOf<Pair<String, String>>()
    private var cameraGranted by mutableStateOf(false)
    private var brightness = -1f

    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.ACCESS_FINE_LOCATION] == true || r[Manifest.permission.ACCESS_COARSE_LOCATION] == true) vm.startSensors()
        cameraGranted = has(Manifest.permission.CAMERA)
    }
    private val exportDoc = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.use { it.write(vm.settings.exportJson().toByteArray()) } }
            .onSuccess { vm.toast("انحفظت النسخة الاحتياطية (بدون مفتاح الذكاء)") }.onFailure { vm.toast("ما قدرت أحفظ") }
    }
    private val importDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.openInputStream(uri)?.use { vm.settings.importJson(it.bufferedReader().readText()) } }
            .onSuccess { vm.toast("رجعت الإعدادات") }.onFailure { vm.toast("الملف غير صالح") }
    }
    private val homeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voice = VoiceInput(this)
        cameraGranted = has(Manifest.permission.CAMERA)
        val want = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) want += listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        if (Build.VERSION.SDK_INT >= 33) want += Manifest.permission.POST_NOTIFICATIONS
        if (want.any { !has(it) }) perms.launch(want.toTypedArray()) else vm.startSensors()
        vm.refreshApps()

        setContent {
            val s by vm.ui.collectAsStateWithLifecycle()
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
            val idle = s.settings.screensaverMin > 0 && now - lastTouch > s.settings.screensaverMin * 60_000L && !s.moving && tab == Tab.HOME
            LaunchedEffect(s.night, s.settings.nightDim) { applyBrightness(if (s.night && s.settings.nightDim) 0.25f else brightness) }
            SonataTheme(dark = s.night, accent = s.settings.accent, fontScale = s.settings.fontScale, eastern = s.settings.easternDigits) {
                Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); lastTouch = System.currentTimeMillis(); screensaverForced = false } }
                }) {
                    Root(s, tab, this@MainActivity, screensaver = idle || screensaverForced,
                        camera = { CameraScreen(s, cameraGranted) { perms.launch(arrayOf(Manifest.permission.CAMERA)) } })
                    if (showPicker) DevicePicker()
                }
            }
        }
    }

    override fun onResume() { super.onResume(); vm.refreshApps(); MediaHub.attach(this) }
    override fun onDestroy() { voice.release(); super.onDestroy() }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() { tab = Tab.HOME } // launcher: back never leaves home

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (intent.hasCategory(Intent.CATEGORY_HOME)) tab = Tab.HOME }

    // ---------- Actions ----------
    override fun tab(t: Tab) { tab = t }
    override fun launch(pkg: String) { if (!Apps.launch(this, pkg)) vm.toast("ما قدرت أفتح التطبيق") }
    override fun toggleFavorite(pkg: String) = vm.updateSettings { s ->
        s.copy(favorites = if (pkg in s.favorites) s.favorites - pkg else (s.favorites + pkg).take(10))
    }.also { vm.toast(if (pkg in vm.settings.state.value.favorites) "انضاف للمفضلة" else "انشال من المفضلة") }

    private fun go(address: String, label: String) {
        if (address.isBlank()) { vm.toast("حط عنوان $label من الإعدادات"); tab = Tab.SETTINGS } else Apps.navigate(this, address)
    }
    override fun navigateHome() = go(vm.settings.state.value.homeAddress, "البيت")
    override fun navigateWork() = go(vm.settings.state.value.workAddress, "الدوام")
    override fun openMaps() { Apps.firstInstalled(this, Apps.navPkgs)?.let { Apps.launch(this, it) } ?: vm.toast("ما فيه تطبيق خرائط") }
    override fun openMusic() { Apps.firstInstalled(this, Apps.musicPkgs)?.let { Apps.launch(this, it) } ?: vm.toast("ما فيه تطبيق موسيقى معروف") }
    override fun openCarApp() { Apps.firstInstalled(this, Apps.carPkgs)?.let { Apps.launch(this, it) } ?: vm.toast("ما لقيت Android Auto أو تطبيق CarPlay") }
    override fun mediaPlayPause() { MediaHub.playPause() }
    override fun mediaNext() { MediaHub.next() }
    override fun mediaPrev() { MediaHub.prev() }
    override fun grantMedia() { runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }

    override fun obdConnect() { val s = vm.settings.state.value; vm.obd.connect(s.obdMac, s.obdBle, s.demoMode || s.obdMac.isBlank()); if (s.obdMac.isBlank() && !s.demoMode) vm.toast("ما اخترت قطعة، شغلت التجريبي") }
    override fun obdDisconnect() = vm.obd.disconnect()
    override fun obdReadDtc() { lifecycleScope.launch { runCatching { vm.obd.refreshDtcs() } } }
    override fun obdClearDtc() { if (vm.ui.value.moving) vm.toast("وقف السيارة أول") else vm.obd.clearDtcsConfirmed() }
    override fun obdRecord() = vm.obd.toggleRecording()

    override fun saveParking() {
        val g = vm.ui.value.geo
        if (g.lat == null || g.lon == null) { vm.toast("ما فيه موقع GPS للحين"); return }
        vm.parkingRepo.save(g.lat, g.lon); vm.refreshParking(); vm.toast("حفظت مكان الموقف 📍")
    }
    override fun showParking() { vm.ui.value.parking?.let { Apps.showLatLon(this, it.lat, it.lon, "موقف السيارة") } ?: vm.toast("ما فيه موقف محفوظ") }
    override fun tripStartStop() = vm.toggleTrip()

    override fun listen() {
        tab = Tab.VOICE
        if (!has(Manifest.permission.RECORD_AUDIO)) { perms.launch(arrayOf(Manifest.permission.RECORD_AUDIO)); return }
        vm.setVoice(text = "", reply = "", listening = true)
        voice.listen(onPartial = { vm.setVoice(text = it) }, onResult = { vm.setVoice(text = it, listening = false); runText(it) },
            onError = { vm.setVoice(reply = it, listening = false) })
    }

    override fun runText(text: String) {
        vm.setVoice(text = text)
        val reply: String? = when (val c = VoiceCommandParser.parse(text)) {
            VoiceCommand.NavigateHome -> { navigateHome(); "أبشر، رايحين البيت" }
            VoiceCommand.NavigateWork -> { navigateWork(); "أبشر، على الدوام" }
            is VoiceCommand.Navigate -> { Apps.navigate(this, c.query); "أبشر، أوديك ${c.query}" }
            is VoiceCommand.Call -> { Apps.dial(this, c.who); "فتحت الاتصال، اختر ${c.who}" }
            VoiceCommand.PlayMusic -> { if (MediaHub.state.value.active) MediaHub.play() else openMusic(); "شغلت لك" }
            VoiceCommand.PauseMusic -> { MediaHub.pause(); "وقفتها" }
            VoiceCommand.NextTrack -> { MediaHub.next(); "اللي بعدها" }
            VoiceCommand.PrevTrack -> { MediaHub.prev(); "رجعت" }
            VoiceCommand.ShowObd -> { tab = Tab.OBD; "هذي حساسات السيارة" }
            VoiceCommand.ShowTrips -> { tab = Tab.TRIPS; "هذي مشاويرك" }
            VoiceCommand.SaveParking -> { saveParking(); "حفظت الموقف" }
            VoiceCommand.FindParking -> { showParking(); "هذا مكان سيارتك" }
            VoiceCommand.NextPrayer -> vm.ui.value.nextPrayer?.let { "باقي على ${it.nameAr} ${PrayerCalc.countdown(Date(), it.time)}" } ?: "فعّل الموقع عشان أحسب الصلاة"
            VoiceCommand.Weather -> vm.ui.value.weather?.let { "الحرارة ${it.tempC.toInt()} درجة، ${it.descAr}" } ?: "ما عندي بيانات طقس للحين"
            VoiceCommand.Time -> Calendar.getInstance().let { "الساعة ${it.get(Calendar.HOUR)}:${"%02d".format(it.get(Calendar.MINUTE))}" }
            VoiceCommand.OpenCamera -> { tab = Tab.CAMERA; "هذي الكاميرا" }
            is VoiceCommand.OpenApp -> vm.ui.value.apps.firstOrNull { it.label.contains(c.name, true) }?.let { launch(it.pkg); "فتحت ${it.label}" } ?: "ما لقيت تطبيق اسمه ${c.name}"
            is VoiceCommand.Unknown -> null
        }
        if (reply != null) { vm.setVoice(reply = reply); voice.speak(reply); return }
        val s = vm.settings.state.value
        if (s.llmKey.isBlank()) { vm.setVoice(reply = "ما فهمت الأمر. جرب: «وديني البيت»"); return }
        vm.setVoice(reply = "لحظة…")
        lifecycleScope.launch {
            val r = LlmHook.ask(s.llmEndpoint, s.llmKey, s.llmModel, text) ?: "ما قدرت أوصل للمزود"
            vm.setVoice(reply = r); voice.speak(r)
        }
    }

    override fun wifi() { Apps.settingsPanel(this, if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS) }
    override fun bluetooth() { Apps.settingsPanel(this, Settings.ACTION_BLUETOOTH_SETTINGS) }
    override fun volume(up: Boolean) {
        (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }
    override fun brightness(v: Float) {
        brightness = when (brightness) { -1f -> 0.3f; 0.3f -> 0.6f; 0.6f -> 1f; else -> -1f }
        applyBrightness(brightness)
        vm.toast(if (brightness < 0) "السطوع: تلقائي" else "السطوع: ${(brightness * 100).toInt()}٪")
    }
    private fun applyBrightness(v: Float) { window.attributes = window.attributes.apply { screenBrightness = v } }

    override fun updateSettings(f: (AppSettings) -> AppSettings) = vm.updateSettings(f)
    override fun backup() = exportDoc.launch("sonata-os-backup.json")
    override fun restore() = importDoc.launch(arrayOf("application/json", "*/*"))
    override fun markOilDone() = vm.updateSettings { it.copy(lastOilKm = it.odometerKm, lastOilDate = System.currentTimeMillis()) }
    override fun markTiresDone() = vm.updateSettings { it.copy(lastTiresKm = it.odometerKm) }
    override fun markServiceDone() = vm.updateSettings { it.copy(lastServiceDate = System.currentTimeMillis()) }
    override fun dismissScreensaver() { lastTouch = System.currentTimeMillis(); screensaverForced = false }
    override fun setDefaultHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) { homeRole.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)); return }
        }
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }.onFailure { vm.toast("اضغط زر الهوم واختر سوناتا OS ثم «دائماً»") }
    }

    // ---------- OBD device picker ----------
    @SuppressLint("MissingPermission")
    override fun pickObdDevice() {
        if (Build.VERSION.SDK_INT >= 31 && !has(Manifest.permission.BLUETOOTH_CONNECT)) { perms.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)); return }
        val ad = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: run { vm.toast("ما فيه بلوتوث"); return }
        found.clear()
        runCatching { ad.bondedDevices.forEach { found += (it.name ?: "?") to it.address } }
        showPicker = true
        runCatching {
            val sc = ad.bluetoothLeScanner ?: return@runCatching
            val cb = object : ScanCallback() {
                override fun onScanResult(type: Int, r: ScanResult) {
                    val n = r.device.name ?: return
                    if (found.none { it.second == r.device.address }) found += "$n (BLE)" to r.device.address
                }
            }
            sc.startScan(cb)
            lifecycleScope.launch { delay(8000); runCatching { sc.stopScan(cb) } }
        }
    }

    @Composable
    private fun DevicePicker() {
        AlertDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton({ showPicker = false }) { Text("إغلاق") } },
            title = { Text("اختر قطعة OBD (ELM327)") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (found.isEmpty()) item { Text("ما فيه أجهزة. اقرن القطعة من إعدادات البلوتوث (الرمز غالباً 1234 أو 0000)") }
                    items(found) { (n, mac) ->
                        Text("$n\n$mac", modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable {
                            vm.updateSettings { it.copy(obdMac = mac, obdBle = n.endsWith("(BLE)"), demoMode = false) }
                            showPicker = false
                            vm.obd.connect(mac, n.endsWith("(BLE)"), false)
                        }.padding(8.dp))
                    }
                }
            })
    }
}
