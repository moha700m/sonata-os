package sa.moha.sonataos.sys

import android.content.Context
import androidx.work.*
import sa.moha.sonataos.SonataApp
import sa.moha.sonataos.data.Maintenance
import java.util.concurrent.TimeUnit

class MaintenanceWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val s = (applicationContext as SonataApp).settings.state.value
        Maintenance.items(s, System.currentTimeMillis()).filter { it.due || it.soon }.forEachIndexed { i, it ->
            Notify.show(applicationContext, Notify.CH_MAINT, 200 + i, if (it.due) "${it.titleAr}: حان وقتها" else "${it.titleAr}: قربت",
                listOfNotNull(it.remainingKm?.let { k -> "باقي $k كم" }, it.remainingDays?.let { d -> "باقي $d يوم" }).joinToString(" • "))
        }
        return Result.success()
    }
    companion object {
        fun enqueue(ctx: Context) = WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("maint",
            ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<MaintenanceWorker>(12, TimeUnit.HOURS).build())
    }
}
