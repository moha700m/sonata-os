package sa.moha.sonataos.voice

/** Local, offline intent parser for Saudi/Gulf Arabic phrases. No network or paid API. */
sealed class VoiceCommand {
    data object NavigateHome : VoiceCommand()
    data object NavigateWork : VoiceCommand()
    data class Navigate(val query: String) : VoiceCommand()
    data class Call(val who: String) : VoiceCommand()
    data object PlayMusic : VoiceCommand()
    data object PauseMusic : VoiceCommand()
    data object NextTrack : VoiceCommand()
    data object PrevTrack : VoiceCommand()
    data object ShowObd : VoiceCommand()
    data object ShowTrips : VoiceCommand()
    data object SaveParking : VoiceCommand()
    data object FindParking : VoiceCommand()
    data object NextPrayer : VoiceCommand()
    data object Weather : VoiceCommand()
    data object Time : VoiceCommand()
    data object OpenCamera : VoiceCommand()
    data class OpenApp(val name: String) : VoiceCommand()
    data class Unknown(val text: String) : VoiceCommand()
}

object VoiceCommandParser {
    private fun norm(s: String) = s.trim()
        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        .replace("ة", "ه").replace("ى", "ي")
        .replace(Regex("[\\u064B-\\u0652]"), "") // tashkeel
        .replace(Regex("\\s+"), " ")
        .lowercase()

    private fun has(t: String, vararg words: String) = words.any { t.contains(norm(it)) }

    fun parse(input: String): VoiceCommand {
        val t = norm(input)
        if (t.isBlank()) return VoiceCommand.Unknown(input)

        val navVerb = has(t, "وديني", "ودني", "خذني", "روح", "اروح", "نروح", "طريق", "ملاح", "navigate", "وصلني", "دلني")
        if (navVerb || has(t, "الخريطه", "خرايط")) {
            if (has(t, "البيت", "بيتي", "المنزل", "home")) return VoiceCommand.NavigateHome
            if (has(t, "الدوام", "الشغل", "العمل", "المكتب", "work")) return VoiceCommand.NavigateWork
            val q = Regex("(?:وديني|ودني|خذني|وصلني|دلني|طريق|اروح|نروح|روح)\\s+(?:الي|لـ|علي|عند)?\\s*(.+)").find(t)?.groupValues?.get(1)?.trim()
            if (!q.isNullOrBlank()) return VoiceCommand.Navigate(q)
        }
        if (has(t, "اتصل", "دق على", "كلم", "call")) {
            val who = t.replace(Regex(".*?(اتصل|دق علي|كلم|call)\\s*(علي|ب)?\\s*"), "").trim()
            return VoiceCommand.Call(who)
        }
        if (has(t, "وقف الاغنيه", "وقف الموسيقى", "وقف الصوت", "اسكت", "ايقاف", "pause")) return VoiceCommand.PauseMusic
        if (has(t, "الاغنيه الجايه", "التالي", "اللي بعدها", "next")) return VoiceCommand.NextTrack
        if (has(t, "السابق", "اللي قبلها", "رجع الاغنيه", "previous")) return VoiceCommand.PrevTrack
        if (has(t, "شغل", "شغلي") && has(t, "اغاني", "اغنيه", "موسيقى", "قران", "سبوتيفاي", "انغامي", "music"))
            return VoiceCommand.PlayMusic
        if (has(t, "حساسات", "obd", "المحرك", "الاعطال", "الكمبيوتر", "عداد")) return VoiceCommand.ShowObd
        if (has(t, "المشاوير", "الرحلات", "استهلاك")) return VoiceCommand.ShowTrips
        if (has(t, "وين وقفت", "وين السياره", "مكان الموقف", "وين موقفي")) return VoiceCommand.FindParking
        if (has(t, "احفظ الموقف", "احفظ مكان", "حفظ الموقف")) return VoiceCommand.SaveParking
        if (has(t, "الصلاه", "الاذان", "صلاة", "متى يأذن", "متى يأذن")) return VoiceCommand.NextPrayer
        if (has(t, "الجو", "الطقس", "الحراره", "weather")) return VoiceCommand.Weather
        if (has(t, "الساعه كم", "كم الساعه", "الوقت")) return VoiceCommand.Time
        if (has(t, "الكاميرا", "كاميرا الخلفيه", "الريفيرس")) return VoiceCommand.OpenCamera
        if (has(t, "افتح", "شغل", "open")) {
            val name = t.replace(Regex(".*?(افتح|شغل|open)\\s*"), "").trim()
            if (name.isNotBlank()) return VoiceCommand.OpenApp(name)
        }
        return VoiceCommand.Unknown(input)
    }
}
