package sa.moha.sonataos

import org.junit.Assert.*
import org.junit.Test
import sa.moha.sonataos.trip.TripMath
import sa.moha.sonataos.data.*
import sa.moha.sonataos.util.ArabicFormat
import java.time.LocalDate

class TripMathTest {
    @Test fun haversineRiyadhJeddah() {
        val km = TripMath.haversineMeters(24.7136, 46.6753, 21.4858, 39.1925) / 1000
        assertEquals(845.0, km, 10.0)
    }
    @Test fun fuelFromMaf() = assertEquals(1.655, TripMath.fuelLitersPerHourFromMaf(5.0), 0.01)
    @Test fun consumption() {
        assertEquals(8.0, TripMath.litersPer100km(8.0, 100.0)!!, 1e-9)
        assertEquals(12.5, TripMath.kmPerLiter(8.0, 100.0)!!, 1e-9)
        assertNull(TripMath.litersPer100km(1.0, 0.0))
    }
    @Test fun accumulatorIntegratesSpeed() {
        val a = TripMath.Accumulator()
        repeat(3600) { a.addSpeedSample(60.0, 1.0, 10.0) }
        assertEquals(60.0, a.km, 0.01)
        assertEquals(3600L, a.movingSeconds)
        assertEquals(3.31, a.fuelL, 0.02)
    }
    @Test fun gpsJumpIgnored() {
        val a = TripMath.Accumulator()
        a.addFix(24.7, 46.6, 50.0); a.addFix(24.7009, 46.6, 50.0); a.addFix(25.7, 46.6, 50.0)
        assertEquals(0.1, a.km, 0.01)
    }
    @Test fun avgSpeed() = assertEquals(60.0, TripMath.averageSpeedKmh(30.0, 1800), 1e-9)
    @Test fun maintenanceDue() {
        val s = AppSettings(odometerKm = 20500, lastOilKm = 10000, oilEveryKm = 10000)
        val oil = Maintenance.items(s, 0).first { it.key == "oil" }
        assertTrue(oil.due); assertEquals(-500, oil.remainingKm)
    }
    @Test fun easternDigits() {
        assertEquals("١٢:٠٥", ArabicFormat.digits("12:05", true))
        assertEquals("12:05", ArabicFormat.digits("١٢:٠٥", false))
    }
    @Test fun hijri() {
        // 1 Ramadan 1447 ≈ 18/19 Feb 2026 (Umm al-Qura)
        val h = ArabicFormat.hijri(LocalDate.of(2026, 2, 19), false)
        assertTrue(h, h.contains("رمضان") && h.contains("1447"))
    }
    @Test fun compass() { assertEquals("شمال", ArabicFormat.compassAr(5f)); assertEquals("غرب", ArabicFormat.compassAr(270f)) }
    @Test fun settingsRoundTrip() {
        val s = AppSettings(homeAddress = "حي النرجس", favorites = listOf("a.b", "c.d"), llmKey = "x")
        assertEquals(s, SettingsRepo.fromJson(SettingsRepo.toJson(s)))
    }
}
