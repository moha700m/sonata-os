package sa.moha.sonataos.ui

import sa.moha.sonataos.data.AppSettings
import sa.moha.sonataos.data.MaintenanceItem
import sa.moha.sonataos.media.NowPlaying
import sa.moha.sonataos.obd.ObdState
import sa.moha.sonataos.prayer.PrayerSlot
import sa.moha.sonataos.sys.AppEntry
import sa.moha.sonataos.sys.GeoState
import sa.moha.sonataos.sys.ParkingSpot
import sa.moha.sonataos.sys.Weather
import sa.moha.sonataos.trip.TripRecord

enum class Tab(val titleAr: String) { HOME("الرئيسية"), APPS("التطبيقات"), OBD("السيارة"), TRIPS("المشاوير"), VOICE("المساعد"), CAMERA("الكاميرا"), SETTINGS("الإعدادات") }

data class LiveTrip(val km: Double = 0.0, val seconds: Long = 0, val maxKmh: Double = 0.0, val fuelL: Double = 0.0, val active: Boolean = false)

data class UiState(
    val nowMillis: Long = System.currentTimeMillis(),
    val settings: AppSettings = AppSettings(),
    val night: Boolean = true,
    val geo: GeoState = GeoState(),
    val weather: Weather? = null,
    val prayers: List<PrayerSlot> = emptyList(),
    val nextPrayer: PrayerSlot? = null,
    val city: String = "",
    val media: NowPlaying = NowPlaying(),
    val mediaAccess: Boolean = false,
    val obd: ObdState = ObdState(),
    val trip: LiveTrip = LiveTrip(),
    val trips: List<TripRecord> = emptyList(),
    val maintenance: List<MaintenanceItem> = emptyList(),
    val apps: List<AppEntry> = emptyList(),
    val parking: ParkingSpot? = null,
    val voiceText: String = "",
    val voiceReply: String = "",
    val listening: Boolean = false,
    val toast: String? = null,
) {
    /** Prefer OBD speed when connected, else GPS. */
    val speedKmh: Double get() = obd.values[sa.moha.sonataos.obd.Pid.SPEED] ?: geo.speedKmh
    val moving: Boolean get() = speedKmh > 8
}

interface Actions {
    fun tab(t: Tab) {}
    fun launch(pkg: String) {}
    fun toggleFavorite(pkg: String) {}
    fun navigateHome() {}
    fun navigateWork() {}
    fun openMaps() {}
    fun openMusic() {}
    fun openCarApp() {}
    fun mediaPlayPause() {}
    fun mediaNext() {}
    fun mediaPrev() {}
    fun grantMedia() {}
    fun obdConnect() {}
    fun obdDisconnect() {}
    fun obdReadDtc() {}
    fun obdClearDtc() {}
    fun obdRecord() {}
    fun saveParking() {}
    fun showParking() {}
    fun tripStartStop() {}
    fun listen() {}
    fun runText(text: String) {}
    fun wifi() {}
    fun bluetooth() {}
    fun volume(up: Boolean) {}
    fun brightness(v: Float) {}
    fun updateSettings(f: (AppSettings) -> AppSettings) {}
    fun pickObdDevice() {}
    fun backup() {}
    fun restore() {}
    fun markOilDone() {}
    fun markTiresDone() {}
    fun markServiceDone() {}
    fun setDefaultHome() {}
    fun dismissScreensaver() {}
}

object NoActions : Actions
