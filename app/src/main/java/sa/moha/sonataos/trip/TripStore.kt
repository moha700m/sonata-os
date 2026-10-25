package sa.moha.sonataos.trip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TripRecord(val start: Long, val end: Long, val km: Double, val maxKmh: Double, val avgKmh: Double, val fuelL: Double?, val movingSec: Long)

class TripStore(context: Context) {
    private val file = File(context.filesDir, "trips.json")
    fun all(): List<TripRecord> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { a.getJSONObject(it) }.map {
            TripRecord(it.getLong("s"), it.getLong("e"), it.getDouble("km"), it.getDouble("max"), it.getDouble("avg"),
                if (it.has("fuel")) it.getDouble("fuel") else null, it.optLong("mv"))
        }
    }.getOrDefault(emptyList())

    fun add(t: TripRecord) {
        val list = (listOf(t) + all()).take(200)
        val a = JSONArray()
        list.forEach { r -> a.put(JSONObject().apply { put("s", r.start); put("e", r.end); put("km", r.km); put("max", r.maxKmh); put("avg", r.avgKmh); r.fuelL?.let { put("fuel", it) }; put("mv", r.movingSec) }) }
        file.writeText(a.toString())
    }
}
