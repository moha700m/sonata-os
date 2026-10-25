package sa.moha.sonataos

import android.app.Application
import android.location.Geocoder
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import sa.moha.sonataos.data.AppSettings
import sa.moha.sonataos.data.Maintenance
import sa.moha.sonataos.media.MediaHub
import sa.moha.sonataos.obd.Pid
import sa.moha.sonataos.prayer.AdhanScheduler
import sa.moha.sonataos.prayer.PrayerCalc
import sa.moha.sonataos.sys.*
import sa.moha.sonataos.trip.TripMath
import sa.moha.sonataos.trip.TripRecord
import sa.moha.sonataos.trip.TripStore
import sa.moha.sonataos.ui.LiveTrip
import sa.moha.sonataos.ui.UiState
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val sonata = app as SonataApp
    val settings = sonata.settings
    val obd = sonata.obd
    val sensors = SensorsHub(app)
    private val weatherRepo = WeatherRepo(app)
    private val tripStore = TripStore(app)
    val parkingRepo = ParkingRepo(app)

    private val _ui = MutableStateFlow(UiState(settings = settings.state.value, weather = weatherRepo.cached(), trips = tripStore.all(), parking = parkingRepo.get()))
    val ui: StateFlow<UiState> = _ui

    private var acc: TripMath.Accumulator? = null
    private var tripStart = 0L
    private var stillSince = 0L
    private var lastWeather = 0L
    private var lastPrayerKey = ""
    private var overSpeed = false
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull()

    init {
        viewModelScope.launch { settings.state.collect { st -> _ui.update { it.copy(settings = st, maintenance = Maintenance.items(st, System.currentTimeMillis())) } } }
        viewModelScope.launch { obd.state.collect { o -> _ui.update { it.copy(obd = o) } } }
        viewModelScope.launch { MediaHub.state.collect { m -> _ui.update { it.copy(media = m, mediaAccess = MediaHub.hasAccess(app)) } } }
        viewModelScope.launch { sensors.state.collect { g -> _ui.update { it.copy(geo = g) } } }
        viewModelScope.launch { while (isActive) { tick(); delay(1000) } }
        val st = settings.state.value
        if (st.obdAutoConnect && (st.obdMac.isNotBlank() || st.demoMode)) obd.connect(st.obdMac, st.obdBle, st.demoMode)
    }

    fun refreshApps() = viewModelScope.launch(Dispatchers.IO) {
        val list = Apps.load(getApplication())
        _ui.update { it.copy(apps = list, mediaAccess = MediaHub.hasAccess(getApplication())) }
    }

    fun startSensors() { sensors.start(); MediaHub.attach(getApplication()) }
    fun stopSensors() = sensors.stop()

    private fun tick() {
        val now = System.currentTimeMillis()
        val s = _ui.value
        val g = s.geo
        // Prayer & night
        var prayers = s.prayers; var next = s.nextPrayer; var night = s.night
        if (g.lat != null && g.lon != null) {
            val cal = Calendar.getInstance()
            val key = "${cal.get(Calendar.DAY_OF_YEAR)}:${"%.2f".format(Locale.US, g.lat)}:${"%.2f".format(Locale.US, g.lon)}"
            if (key != lastPrayerKey) {
                lastPrayerKey = key
                prayers = PrayerCalc.day(g.lat, g.lon, Date(now)).slots
                runCatching { AdhanScheduler.schedule(getApplication(), g.lat, g.lon) }
                resolveCity(g.lat, g.lon)
            }
            next = PrayerCalc.nextPrayer(g.lat, g.lon, Date(now))
            val sunrise = prayers.firstOrNull { it.key == "sunrise" }?.time?.time
            val maghrib = prayers.firstOrNull { it.key == "maghrib" }?.time?.time
            if (sunrise != null && maghrib != null) night = now < sunrise || now > maghrib
            if (now - lastWeather > 30 * 60_000) { lastWeather = now; viewModelScope.launch { weatherRepo.fetch(g.lat, g.lon)?.let { w -> _ui.update { it.copy(weather = w) } } } }
        } else {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); night = h < 6 || h >= 18
        }
        night = when (s.settings.themeMode) { sa.moha.sonataos.data.ThemeMode.DARK -> true; sa.moha.sonataos.data.ThemeMode.LIGHT -> false; else -> night }

        // Trip
        val speed = s.speedKmh
        var a = acc
        if (a == null && speed > 15) { a = TripMath.Accumulator(); acc = a; tripStart = now; stillSince = 0 }
        if (a != null) {
            a.addSpeedSample(speed, 1.0, s.obd.values[Pid.MAF])
            if (speed < 3) { if (stillSince == 0L) stillSince = now; if (now - stillSince > 5 * 60_000) endTrip() } else stillSince = 0
        }
        val live = acc?.let { LiveTrip(it.km, (now - tripStart) / 1000, it.maxSpeed, it.fuelL, true) } ?: LiveTrip()

        // Speed warning beep (once per exceedance)
        val over = s.settings.speedWarn && speed > s.settings.speedLimit
        if (over && !overSpeed) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        overSpeed = over

        _ui.update { it.copy(nowMillis = now, prayers = prayers, nextPrayer = next, night = night, trip = live) }
    }

    private fun resolveCity(lat: Double, lon: Double) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val r = Geocoder(getApplication(), Locale("ar")).getFromLocation(lat, lon, 1)?.firstOrNull()
            val c = r?.locality ?: r?.subAdminArea ?: r?.adminArea
            if (c != null) _ui.update { it.copy(city = c) }
        }
    }

    fun toggleTrip() {
        if (acc != null) endTrip() else { acc = TripMath.Accumulator(); tripStart = System.currentTimeMillis(); stillSince = 0 }
    }

    private fun endTrip() {
        val a = acc ?: return
        acc = null
        val end = System.currentTimeMillis()
        if (a.km >= 0.2) {
            val rec = TripRecord(tripStart, end, a.km, a.maxSpeed, TripMath.averageSpeedKmh(a.km, (end - tripStart) / 1000), a.fuelL.takeIf { it > 0 }, a.movingSeconds)
            tripStore.add(rec)
            settings.update { it.copy(odometerKm = it.odometerKm + a.km.toInt()) }
            _ui.update { it.copy(trips = tripStore.all()) }
        }
    }

    fun toast(msg: String) = viewModelScope.launch { _ui.update { it.copy(toast = msg) }; delay(3000); _ui.update { if (it.toast == msg) it.copy(toast = null) else it } }
    fun setVoice(text: String? = null, reply: String? = null, listening: Boolean? = null) =
        _ui.update { it.copy(voiceText = text ?: it.voiceText, voiceReply = reply ?: it.voiceReply, listening = listening ?: it.listening) }
    fun updateSettings(f: (AppSettings) -> AppSettings) = settings.update(f)
    fun refreshParking() = _ui.update { it.copy(parking = parkingRepo.get()) }

    override fun onCleared() { sensors.stop(); tone?.release() }
}
