package sa.moha.sonataos.sys

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val tempC: Double, val code: Int, val maxC: Double, val minC: Double, val wind: Double, val fetchedAt: Long, val cached: Boolean = false) {
    val descAr: String get() = describe(code)
    companion object {
        fun describe(code: Int) = when (code) {
            0 -> "صحو"; 1, 2 -> "غائم جزئياً"; 3 -> "غائم"; 45, 48 -> "ضباب"
            in 51..57 -> "رذاذ"; in 61..67 -> "مطر"; in 71..77 -> "ثلج"; in 80..82 -> "زخات مطر"
            in 95..99 -> "عواصف رعدية"; else -> "—"
        }
    }
}

/** Open-Meteo (free, no API key). Cached in prefs for offline use. */
class WeatherRepo(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)

    fun cached(): Weather? = prefs.getString("w", null)?.let { runCatching { parse(JSONObject(it), prefs.getLong("t", 0)).copy(cached = true) }.getOrNull() }

    suspend fun fetch(lat: Double, lon: Double): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1".format(java.util.Locale.US, lat, lon))
            val c = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
            val body = c.inputStream.bufferedReader().readText()
            val now = System.currentTimeMillis()
            prefs.edit().putString("w", body).putLong("t", now).apply()
            parse(JSONObject(body), now)
        }.getOrNull() ?: cached()
    }

    private fun parse(j: JSONObject, t: Long): Weather {
        val cur = j.getJSONObject("current"); val d = j.getJSONObject("daily")
        return Weather(cur.getDouble("temperature_2m"), cur.getInt("weather_code"),
            d.getJSONArray("temperature_2m_max").getDouble(0), d.getJSONArray("temperature_2m_min").getDouble(0),
            cur.getDouble("wind_speed_10m"), t)
    }
}
