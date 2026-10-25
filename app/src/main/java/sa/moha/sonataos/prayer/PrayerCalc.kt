package sa.moha.sonataos.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.util.Date
import java.util.TimeZone

data class PrayerSlot(val key: String, val nameAr: String, val time: Date)

data class PrayerDay(val slots: List<PrayerSlot>) {
    /** Next prayer after [now] (excludes sunrise), or null if all passed today. */
    fun next(now: Date): PrayerSlot? = slots.filter { it.key != "sunrise" }.firstOrNull { it.time.after(now) }
}

/** Offline prayer calc using batoulapps/adhan, Umm al-Qura (Saudi) by default. */
object PrayerCalc {
    fun day(lat: Double, lon: Double, date: Date, method: CalculationMethod = CalculationMethod.UMM_AL_QURA): PrayerDay {
        val params = method.parameters.apply { madhab = Madhab.SHAFI }
        val pt = PrayerTimes(Coordinates(lat, lon), DateComponents.from(date), params)
        return PrayerDay(
            listOf(
                PrayerSlot("fajr", "الفجر", pt.fajr),
                PrayerSlot("sunrise", "الشروق", pt.sunrise),
                PrayerSlot("dhuhr", "الظهر", pt.dhuhr),
                PrayerSlot("asr", "العصر", pt.asr),
                PrayerSlot("maghrib", "المغرب", pt.maghrib),
                PrayerSlot("isha", "العشاء", pt.isha)
            )
        )
    }

    /** Next prayer, rolling over to tomorrow's Fajr after Isha. */
    fun nextPrayer(lat: Double, lon: Double, now: Date): PrayerSlot {
        day(lat, lon, now).next(now)?.let { return it }
        val tomorrow = Date(now.time + 24L * 3600_000L)
        return day(lat, lon, tomorrow).slots.first()
    }

    fun countdown(now: Date, target: Date): String {
        val s = ((target.time - now.time) / 1000).coerceAtLeast(0)
        return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }

    @Suppress("unused") val defaultTz: TimeZone = TimeZone.getTimeZone("Asia/Riyadh")
}
