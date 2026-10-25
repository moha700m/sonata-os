package sa.moha.sonataos

import org.junit.Assert.*
import org.junit.Test
import sa.moha.sonataos.prayer.PrayerCalc
import java.text.SimpleDateFormat
import java.util.*

class PrayerTest {
    private val tz = TimeZone.getTimeZone("Asia/Riyadh")
    private fun at(s: String) = SimpleDateFormat("yyyy-MM-dd HH:mm").apply { timeZone = tz }.parse(s)!!
    private fun hm(d: Date) = SimpleDateFormat("HH:mm").apply { timeZone = tz }.format(d)
    private fun minutes(d: Date) = hm(d).split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    // Riyadh 24.7136, 46.6753. Reference (Umm al-Qura, published tables ±3 min) for 2026-10-25:
    // Fajr ~04:42, Dhuhr ~11:41, Asr ~14:59, Maghrib ~17:19, Isha = Maghrib + 90 min.
    @Test fun riyadhUmmAlQura() {
        val d = PrayerCalc.day(24.7136, 46.6753, at("2026-10-25 12:00"))
        val m = d.slots.associate { it.key to minutes(it.time) }
        assertEquals(4 * 60 + 42.0, m["fajr"]!!.toDouble(), 5.0)
        assertEquals(11 * 60 + 41.0, m["dhuhr"]!!.toDouble(), 5.0)
        assertEquals(14 * 60 + 59.0, m["asr"]!!.toDouble(), 6.0)
        assertEquals(17 * 60 + 19.0, m["maghrib"]!!.toDouble(), 5.0)
        assertEquals(90, m["isha"]!! - m["maghrib"]!!)
    }

    @Test fun orderIsChronological() {
        val d = PrayerCalc.day(21.4858, 39.1925, at("2026-03-01 12:00")) // Jeddah
        val times = d.slots.map { it.time.time }
        assertEquals(times.sorted(), times)
    }

    @Test fun nextPrayerAfterIshaIsTomorrowFajr() {
        val n = PrayerCalc.nextPrayer(24.7136, 46.6753, at("2026-10-25 23:30"))
        assertEquals("fajr", n.key)
        assertTrue(n.time.after(at("2026-10-26 03:00")))
    }

    @Test fun nextPrayerMidday() = assertEquals("asr", PrayerCalc.nextPrayer(24.7136, 46.6753, at("2026-10-25 13:00")).key)

    @Test fun countdown() = assertEquals("1:05:09", PrayerCalc.countdown(Date(0), Date((3600 + 309) * 1000L)))
}
