package sa.moha.sonataos.sys

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppEntry(val label: String, val pkg: String, val activity: String, val icon: ImageBitmap?)

object Apps {
    val navPkgs = listOf("com.google.android.apps.maps", "com.waze", "ru.yandex.yandexnavi", "net.osmand", "com.here.app.maps")
    val musicPkgs = listOf("com.spotify.music", "com.anghami", "com.google.android.apps.youtube.music", "com.apple.android.music", "deezer.android.app", "com.soundcloud.android", "com.audiomack")
    val carPkgs = listOf("com.google.android.projection.gearhead", "com.zjinnova.zlink", "com.autokit.carplay", "com.suding.speedplay", "com.carlinkit.autokit", "com.ottocast.app")

    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                val d = runCatching { it.loadIcon(pm) }.getOrNull()
                val bmp = d?.let { dr ->
                    val b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    dr.setBounds(0, 0, 96, 96); dr.draw(Canvas(b)); b.asImageBitmap()
                }
                AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, bmp)
            }.sortedBy { it.label.lowercase() }
    }

    fun launch(ctx: Context, pkg: String): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true
    }

    fun firstInstalled(ctx: Context, pkgs: List<String>) = pkgs.firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }

    fun navigate(ctx: Context, query: String) {
        val q = Uri.encode(query)
        val tries = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q")).setPackage("com.google.android.apps.maps"),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes")).setPackage("com.waze"),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")),
        )
        for (t in tries) try { ctx.startActivity(t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: ActivityNotFoundException) {}
    }

    fun showLatLon(ctx: Context, lat: Double, lon: Double, label: String) = runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun dial(ctx: Context, who: String) = runCatching {
        val digits = who.filter { it.isDigit() || it == '+' }
        val uri = if (digits.length >= 3) Uri.parse("tel:$digits") else Uri.parse("tel:")
        ctx.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun settingsPanel(ctx: Context, action: String) = runCatching { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
