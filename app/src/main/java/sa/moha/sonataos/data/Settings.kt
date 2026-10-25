package sa.moha.sonataos.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

enum class ThemeMode { AUTO, DARK, LIGHT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accent: Long = 0xFF2F7DF6,
    val fontScale: Float = 1f,
    val easternDigits: Boolean = true,
    val clock24h: Boolean = false,
    val nightDim: Boolean = true,
    val nightDimLevel: Float = 0.35f,
    val speedLimit: Int = 120,
    val speedWarn: Boolean = true,
    val adhanReminder: Boolean = true,
    val homeAddress: String = "",
    val workAddress: String = "",
    val fuelPrice: Float = 2.33f, // SAR per liter (91), user-editable
    val obdMac: String = "",
    val obdBle: Boolean = false,
    val obdAutoConnect: Boolean = true,
    val demoMode: Boolean = true,
    val favorites: List<String> = emptyList(),
    val widgets: List<String> = listOf("clock", "speed", "media", "prayer", "weather", "trip"),
    val screensaverMin: Int = 3,
    val llmEndpoint: String = "",
    val llmKey: String = "",
    val llmModel: String = "",
    val oilEveryKm: Int = 10000,
    val oilEveryDays: Int = 180,
    val lastOilKm: Int = 0,
    val lastOilDate: Long = 0L,
    val odometerKm: Int = 0,
    val tiresEveryKm: Int = 40000,
    val lastTiresKm: Int = 0,
    val serviceEveryDays: Int = 365,
    val lastServiceDate: Long = 0L,
)

class SettingsRepo(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sonata", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state

    fun update(f: (AppSettings) -> AppSettings) {
        val n = f(_state.value)
        _state.value = n
        prefs.edit().putString("settings", toJson(n).toString()).apply()
    }

    private fun load(): AppSettings = prefs.getString("settings", null)?.let {
        runCatching { fromJson(JSONObject(it)) }.getOrNull()
    } ?: AppSettings()

    fun exportJson(includeSecrets: Boolean = false): String {
        val j = toJson(_state.value)
        if (!includeSecrets) j.remove("llmKey")
        return j.toString(2)
    }

    fun importJson(s: String) {
        val cur = _state.value
        val n = fromJson(JSONObject(s)).let { if (it.llmKey.isEmpty()) it.copy(llmKey = cur.llmKey) else it }
        update { n }
    }

    companion object {
        fun toJson(s: AppSettings) = JSONObject().apply {
            put("themeMode", s.themeMode.name); put("accent", s.accent); put("fontScale", s.fontScale.toDouble())
            put("easternDigits", s.easternDigits); put("clock24h", s.clock24h); put("nightDim", s.nightDim)
            put("nightDimLevel", s.nightDimLevel.toDouble()); put("speedLimit", s.speedLimit); put("speedWarn", s.speedWarn)
            put("adhanReminder", s.adhanReminder); put("homeAddress", s.homeAddress); put("workAddress", s.workAddress)
            put("fuelPrice", s.fuelPrice.toDouble()); put("obdMac", s.obdMac); put("obdBle", s.obdBle)
            put("obdAutoConnect", s.obdAutoConnect); put("demoMode", s.demoMode)
            put("favorites", s.favorites.joinToString("|")); put("widgets", s.widgets.joinToString("|"))
            put("screensaverMin", s.screensaverMin); put("llmEndpoint", s.llmEndpoint); put("llmKey", s.llmKey)
            put("llmModel", s.llmModel); put("oilEveryKm", s.oilEveryKm); put("oilEveryDays", s.oilEveryDays)
            put("lastOilKm", s.lastOilKm); put("lastOilDate", s.lastOilDate); put("odometerKm", s.odometerKm)
            put("tiresEveryKm", s.tiresEveryKm); put("lastTiresKm", s.lastTiresKm)
            put("serviceEveryDays", s.serviceEveryDays); put("lastServiceDate", s.lastServiceDate)
        }

        fun fromJson(j: JSONObject): AppSettings {
            val d = AppSettings()
            fun list(k: String, def: List<String>) = if (j.has(k)) j.getString(k).split("|").filter { it.isNotBlank() } else def
            return AppSettings(
                themeMode = runCatching { ThemeMode.valueOf(j.optString("themeMode", d.themeMode.name)) }.getOrDefault(d.themeMode),
                accent = j.optLong("accent", d.accent), fontScale = j.optDouble("fontScale", 1.0).toFloat(),
                easternDigits = j.optBoolean("easternDigits", d.easternDigits), clock24h = j.optBoolean("clock24h", d.clock24h),
                nightDim = j.optBoolean("nightDim", d.nightDim), nightDimLevel = j.optDouble("nightDimLevel", 0.35).toFloat(),
                speedLimit = j.optInt("speedLimit", d.speedLimit), speedWarn = j.optBoolean("speedWarn", d.speedWarn),
                adhanReminder = j.optBoolean("adhanReminder", d.adhanReminder),
                homeAddress = j.optString("homeAddress", ""), workAddress = j.optString("workAddress", ""),
                fuelPrice = j.optDouble("fuelPrice", d.fuelPrice.toDouble()).toFloat(),
                obdMac = j.optString("obdMac", ""), obdBle = j.optBoolean("obdBle", false),
                obdAutoConnect = j.optBoolean("obdAutoConnect", true), demoMode = j.optBoolean("demoMode", true),
                favorites = list("favorites", d.favorites), widgets = list("widgets", d.widgets),
                screensaverMin = j.optInt("screensaverMin", d.screensaverMin),
                llmEndpoint = j.optString("llmEndpoint", ""), llmKey = j.optString("llmKey", ""), llmModel = j.optString("llmModel", ""),
                oilEveryKm = j.optInt("oilEveryKm", d.oilEveryKm), oilEveryDays = j.optInt("oilEveryDays", d.oilEveryDays),
                lastOilKm = j.optInt("lastOilKm", 0), lastOilDate = j.optLong("lastOilDate", 0), odometerKm = j.optInt("odometerKm", 0),
                tiresEveryKm = j.optInt("tiresEveryKm", d.tiresEveryKm), lastTiresKm = j.optInt("lastTiresKm", 0),
                serviceEveryDays = j.optInt("serviceEveryDays", d.serviceEveryDays), lastServiceDate = j.optLong("lastServiceDate", 0),
            )
        }
    }
}
