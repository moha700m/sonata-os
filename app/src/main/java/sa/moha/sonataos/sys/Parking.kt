package sa.moha.sonataos.sys

import android.content.Context

data class ParkingSpot(val lat: Double, val lon: Double, val time: Long)

class ParkingRepo(context: Context) {
    private val p = context.getSharedPreferences("parking", Context.MODE_PRIVATE)
    fun save(lat: Double, lon: Double) = p.edit().putString("lat", lat.toString()).putString("lon", lon.toString()).putLong("t", System.currentTimeMillis()).apply()
    fun get(): ParkingSpot? {
        val la = p.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lo = p.getString("lon", null)?.toDoubleOrNull() ?: return null
        return ParkingSpot(la, lo, p.getLong("t", 0))
    }
}
