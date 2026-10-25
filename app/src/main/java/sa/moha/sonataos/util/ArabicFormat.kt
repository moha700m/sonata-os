package sa.moha.sonataos.util

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

object ArabicFormat {
    private const val EASTERN = "٠١٢٣٤٥٦٧٨٩"

    /** Converts digits both ways so output is consistent regardless of device locale. */
    fun digits(s: String, eastern: Boolean): String = buildString {
        s.forEach { c ->
            val e = EASTERN.indexOf(c)
            append(
                when {
                    eastern && c in '0'..'9' -> EASTERN[c - '0']
                    !eastern && e >= 0 -> '0' + e
                    else -> c
                }
            )
        }
    }

    val gregorianMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    val hijriMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val weekdays = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

    fun gregorian(d: LocalDate, eastern: Boolean) =
        digits("${weekdays[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${gregorianMonths[d.monthValue - 1]} ${d.year}", eastern)

    /** Hijri date via java.time HijrahDate (Umm al-Qura based tables in the JDK/Android). */
    fun hijri(d: LocalDate, eastern: Boolean): String {
        val h = HijrahDate.from(d)
        val day = h.get(ChronoField.DAY_OF_MONTH)
        val month = h.get(ChronoField.MONTH_OF_YEAR)
        val year = h.get(ChronoField.YEAR)
        return digits("$day ${hijriMonths[month - 1]} $year هـ", eastern)
    }

    fun clock(hour: Int, minute: Int, h24: Boolean, eastern: Boolean): String {
        val hh = if (h24) hour else ((hour + 11) % 12 + 1)
        return digits("%d:%02d".format(hh, minute), eastern)
    }

    fun amPm(hour: Int) = if (hour < 12) "ص" else "م"

    fun compassAr(deg: Float): String {
        val names = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")
        val i = (((deg % 360 + 360) % 360 + 22.5f) / 45f).toInt() % 8
        return names[i]
    }
}
