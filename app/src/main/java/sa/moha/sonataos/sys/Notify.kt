package sa.moha.sonataos.sys

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

object Notify {
    const val CH_PRAYER = "prayer"; const val CH_MAINT = "maintenance"
    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_PRAYER, "تنبيه الأذان", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        })
        nm.createNotificationChannel(NotificationChannel(CH_MAINT, "تذكير الصيانة", NotificationManager.IMPORTANCE_DEFAULT))
    }
    fun show(ctx: Context, ch: String, id: Int, title: String, body: String) = runCatching {
        val n = NotificationCompat.Builder(ctx, ch).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}
