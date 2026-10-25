package sa.moha.sonataos.trip

import kotlin.math.*

object TripMath {
    private const val EARTH_R = 6_371_000.0
    private const val AFR_GASOLINE = 14.7
    private const val GASOLINE_DENSITY_G_PER_L = 740.0

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_R * asin(min(1.0, sqrt(a)))
    }

    /** Fuel flow in L/h from MAF (g/s), stoichiometric gasoline estimate. */
    fun fuelLitersPerHourFromMaf(mafGs: Double): Double =
        mafGs * 3600.0 / (AFR_GASOLINE * GASOLINE_DENSITY_G_PER_L)

    /** L/100km given fuel liters and km; null when distance too small. */
    fun litersPer100km(liters: Double, km: Double): Double? = if (km < 0.1) null else liters / km * 100.0

    /** km per liter (what Saudi drivers usually quote). */
    fun kmPerLiter(liters: Double, km: Double): Double? = if (liters <= 0.0) null else km / liters

    fun averageSpeedKmh(km: Double, seconds: Long): Double = if (seconds <= 0) 0.0 else km / (seconds / 3600.0)

    fun fuelCostSar(liters: Double, pricePerLiter: Double): Double = liters * pricePerLiter

    /** Accumulator for a running trip. Feed GPS fixes or OBD speed samples. */
    class Accumulator {
        var distanceM = 0.0; private set
        var fuelL = 0.0; private set
        var maxSpeed = 0.0; private set
        var movingSeconds = 0L; private set
        private var lastLat: Double? = null
        private var lastLon: Double? = null

        fun addFix(lat: Double, lon: Double, speedKmh: Double, accuracyM: Float = 5f) {
            val pl = lastLat; val pn = lastLon
            if (pl != null && pn != null && accuracyM < 40f) {
                val d = haversineMeters(pl, pn, lat, lon)
                if (d < 500) distanceM += d // drop GPS jumps
            }
            lastLat = lat; lastLon = lon
            maxSpeed = max(maxSpeed, speedKmh)
        }

        /** Integrate distance from speed over dt (used when no GPS, e.g. OBD/demo). */
        fun addSpeedSample(speedKmh: Double, dtSeconds: Double, mafGs: Double? = null) {
            distanceM += speedKmh / 3.6 * dtSeconds
            if (speedKmh > 2) movingSeconds += dtSeconds.roundToLong()
            maxSpeed = max(maxSpeed, speedKmh)
            if (mafGs != null) fuelL += fuelLitersPerHourFromMaf(mafGs) * dtSeconds / 3600.0
        }

        fun tickMoving(seconds: Long) { movingSeconds += seconds }
        val km get() = distanceM / 1000.0
    }
}
