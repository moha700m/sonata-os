package sa.moha.sonataos.data

data class MaintenanceItem(val key: String, val titleAr: String, val remainingKm: Int?, val remainingDays: Int?) {
    val due: Boolean get() = (remainingKm != null && remainingKm <= 0) || (remainingDays != null && remainingDays <= 0)
    val soon: Boolean get() = !due && ((remainingKm != null && remainingKm < 500) || (remainingDays != null && remainingDays < 7))
}

object Maintenance {
    private const val DAY = 86_400_000L
    fun items(s: AppSettings, now: Long): List<MaintenanceItem> = listOf(
        MaintenanceItem(
            "oil", "تغيير الزيت",
            s.lastOilKm + s.oilEveryKm - s.odometerKm,
            if (s.lastOilDate == 0L) null else (((s.lastOilDate + s.oilEveryDays * DAY) - now) / DAY).toInt()
        ),
        MaintenanceItem("tires", "تبديل/ترصيص الكفرات", s.lastTiresKm + s.tiresEveryKm - s.odometerKm, null),
        MaintenanceItem(
            "service", "الصيانة الدورية", null,
            if (s.lastServiceDate == 0L) null else (((s.lastServiceDate + s.serviceEveryDays * DAY) - now) / DAY).toInt()
        ),
    )
}
