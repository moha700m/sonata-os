package sa.moha.sonataos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test
import sa.moha.sonataos.data.AppSettings
import sa.moha.sonataos.data.Maintenance
import sa.moha.sonataos.media.NowPlaying
import sa.moha.sonataos.obd.ObdState
import sa.moha.sonataos.obd.ObdStatus
import sa.moha.sonataos.obd.Pid
import sa.moha.sonataos.prayer.PrayerCalc
import sa.moha.sonataos.sys.GeoState
import sa.moha.sonataos.sys.Weather
import sa.moha.sonataos.trip.TripRecord
import sa.moha.sonataos.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

object Samples {
    init { TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh")) }
    val now: Long = SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-10-25 16:42")!!.time
    fun state(night: Boolean = true): UiState {
        val lat = 24.7136; val lon = 46.6753
        val art = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also { b ->
            Canvas(b).apply { drawColor(AColor.rgb(30, 60, 140)); drawCircle(32f, 32f, 18f, Paint().apply { color = AColor.rgb(120, 180, 255) }) }
        }
        val st = AppSettings(homeAddress = "حي النرجس", odometerKm = 84250, lastOilKm = 76000, lastOilDate = now - 150L * 86_400_000L, lastTiresKm = 60000, lastServiceDate = now - 300L * 86_400_000L)
        return UiState(
            nowMillis = now, settings = st, night = night,
            geo = GeoState(lat, lon, 96.0, 35f, 5f, 35f, true),
            weather = Weather(33.0, 1, 36.0, 22.0, 12.0, now),
            prayers = PrayerCalc.day(lat, lon, Date(now)).slots,
            nextPrayer = PrayerCalc.nextPrayer(lat, lon, Date(now)),
            city = "الرياض",
            media = NowPlaying("فوق هام السحب", "محمد عبده", "com.anghami", true, art, true), mediaAccess = true,
            obd = ObdState(ObdStatus.CONNECTED, demo = true, values = mapOf(
                Pid.RPM to 2150.0, Pid.SPEED to 96.0, Pid.COOLANT_TEMP to 89.0, Pid.ENGINE_LOAD to 34.0, Pid.THROTTLE to 22.0,
                Pid.FUEL_LEVEL to 58.0, Pid.INTAKE_TEMP to 41.0, Pid.STFT_B1 to 2.3, Pid.LTFT_B1 to 3.9, Pid.MAF to 18.4), voltage = 14.1,
                dtcs = listOf("P0171", "P0420")),
            trip = LiveTrip(23.4, 1260, 118.0, 1.9, true),
            trips = listOf(
                TripRecord(now - 86_400_000L, now - 86_000_000L, 18.2, 112.0, 54.0, 1.6, 1100),
                TripRecord(now - 2 * 86_400_000L, now - 2 * 86_000_000L, 41.7, 131.0, 77.0, 3.4, 1900),
                TripRecord(now - 3 * 86_400_000L, now - 3 * 86_000_000L, 6.3, 64.0, 31.0, null, 700),
            ),
            maintenance = Maintenance.items(st, now),
        )
    }
}

abstract class ShotsBase(w: Int, h: Int, density: Density) {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = w, screenHeight = h, xdpi = density.dpiValue, ydpi = density.dpiValue, density = density,
            orientation = ScreenOrientation.LANDSCAPE, softButtons = false, locale = "ar",
        ),
        showSystemUi = false, maxPercentDifference = 0.1,
    )

    private fun shot(name: String, tab: Tab, night: Boolean = true, saver: Boolean = false) {
        val s = Samples.state(night)
        paparazzi.snapshot(name) {
            SonataTheme(dark = night, accent = s.settings.accent, eastern = s.settings.easternDigits) { Root(s, tab, NoActions, screensaver = saver) }
        }
    }

    @Test fun home() = shot("home", Tab.HOME)
    @Test fun homeDay() = shot("home_day", Tab.HOME, night = false)
    @Test fun obd() = shot("obd", Tab.OBD)
    @Test fun trips() = shot("trips", Tab.TRIPS)
    @Test fun voice() = shot("voice", Tab.VOICE)
    @Test fun settings() = shot("settings", Tab.SETTINGS)
    @Test fun apps() = shot("apps", Tab.APPS)
    @Test fun camera() = shot("camera", Tab.CAMERA)
    @Test fun screensaver() = shot("screensaver", Tab.HOME, saver = true)
}

class Shots1280x720 : ShotsBase(1280, 720, Density.MEDIUM)
class Shots1920x720 : ShotsBase(1920, 720, Density.MEDIUM)
class Shots1920x720hdpi : ShotsBase(1920, 720, Density.HIGH)
class Shots800x480 : ShotsBase(800, 480, Density.MEDIUM)
