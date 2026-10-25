package sa.moha.sonataos.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import sa.moha.sonataos.SonataApp
import sa.moha.sonataos.sys.Notify
import java.util.Date

object AdhanScheduler {
    fun schedule(ctx: Context, lat: Double, lon: Double) {
        val next = PrayerCalc.nextPrayer(lat, lon, Date())
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, AdhanReceiver::class.java).putExtra("name", next.nameAr),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.time.time, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.time.time, pi)
        ctx.getSharedPreferences("adhan", Context.MODE_PRIVATE).edit().putString("lat", "$lat").putString("lon", "$lon").apply()
    }

    fun reschedule(ctx: Context) {
        val p = ctx.getSharedPreferences("adhan", Context.MODE_PRIVATE)
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return
        val lon = p.getString("lon", null)?.toDoubleOrNull() ?: return
        schedule(ctx, lat, lon)
    }
}

class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext as SonataApp
        if (app.settings.state.value.adhanReminder) {
            Notify.show(ctx, Notify.CH_PRAYER, 100, "حان وقت صلاة ${intent.getStringExtra("name") ?: ""}", "الله أكبر — تذكير من سوناتا OS")
        }
        AdhanScheduler.reschedule(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { AdhanScheduler.reschedule(ctx) }
}
