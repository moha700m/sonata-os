package sa.moha.sonataos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.moha.sonataos.util.ArabicFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

private val tabIcons = mapOf(
    Tab.HOME to Icons.Rounded.Home, Tab.APPS to Icons.Rounded.Apps, Tab.OBD to Icons.Rounded.DirectionsCar,
    Tab.TRIPS to Icons.Rounded.Route, Tab.VOICE to Icons.Rounded.Mic, Tab.CAMERA to Icons.Rounded.Videocam, Tab.SETTINGS to Icons.Rounded.Settings,
)

@Composable
fun Root(
    s: UiState, tab: Tab, a: Actions, screensaver: Boolean = false,
    camera: @Composable () -> Unit = { CameraScreen(s, false) {} },
) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(p.surfaceHi.copy(alpha = if (p.dark) 0.6f else 1f), p.bg), radius = 1400f))) {
        Row(Modifier.fillMaxSize().padding(12.dp)) {
            NavRail(tab, a)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopBar(s, tab, a)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                        when (t) {
                            Tab.HOME -> HomeScreen(s, a)
                            Tab.APPS -> AppsScreen(s, a)
                            Tab.OBD -> ObdScreen(s, a)
                            Tab.TRIPS -> TripsScreen(s, a)
                            Tab.VOICE -> VoiceScreen(s, a)
                            Tab.CAMERA -> camera()
                            Tab.SETTINGS -> SettingsScreen(s, a)
                        }
                    }
                }
            }
        }
        if (s.night && s.settings.nightDim) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = s.settings.nightDimLevel * 0.6f)))
        if (screensaver) Screensaver(s, a)
        s.toast?.let {
            Text(it, color = Color.White, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xE6101826)).padding(horizontal = 22.dp, vertical = 14.dp))
        }
    }
}

@Composable
fun NavRail(tab: Tab, a: Actions) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val showLabels = maxHeight > 560.dp
        Column(
            Modifier.fillMaxHeight().width(92.dp).clip(RoundedCornerShape(26.dp)).background(p.surface).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.entries.forEach { t ->
                val sel = t == tab
                Column(
                    Modifier.width(80.dp).heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) p.accent.copy(alpha = 0.18f) else Color.Transparent).clickable { a.tab(t) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(tabIcons[t]!!, t.titleAr, tint = if (sel) p.accent else p.textDim, modifier = Modifier.size(30.dp))
                    if (showLabels) Text(t.titleAr, fontSize = 13.sp, lineHeight = 18.sp, color = if (sel) p.accent else p.textDim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun TopBar(s: UiState, tab: Tab, a: Actions) {
    val p = LocalPalette.current
    val cal = Calendar.getInstance().apply { timeInMillis = s.nowMillis }
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tab.titleAr, style = MaterialTheme.typography.headlineMedium, color = p.text, maxLines = 1)
        Spacer(Modifier.width(14.dp))
        if (tab != Tab.HOME) Text(ArabicFormat.clock(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), s.settings.clock24h, LocalEastern.current),
            style = MaterialTheme.typography.titleLarge, color = p.textDim, maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (s.obd.status == sa.moha.sonataos.obd.ObdStatus.CONNECTED) { Chip(if (s.obd.demo) "OBD تجريبي" else "OBD متصل", p.ok); Spacer(Modifier.width(8.dp)) }
        QuickBtn(Icons.Rounded.Wifi, "واي فاي", a::wifi)
        QuickBtn(Icons.Rounded.Bluetooth, "بلوتوث", a::bluetooth)
        QuickBtn(Icons.Rounded.VolumeDown, "خفض الصوت") { a.volume(false) }
        QuickBtn(Icons.Rounded.VolumeUp, "رفع الصوت") { a.volume(true) }
        QuickBtn(if (s.night) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, "السطوع") { a.brightness(-1f) }
    }
}

@Composable
private fun QuickBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(64.dp, 56.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = p.text, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun Screensaver(s: UiState, a: Actions) {
    val cal = Calendar.getInstance().apply { timeInMillis = s.nowMillis }
    val ld = Instant.ofEpochMilli(s.nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val e = LocalEastern.current
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = a::dismissScreensaver), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val fs = (maxHeight.value * 0.38f).coerceAtMost(260f).sp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ArabicFormat.clock(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), s.settings.clock24h, e),
                    fontSize = fs, lineHeight = fs * 1.05f, fontWeight = FontWeight.Light, color = Color(0xFFCFD8E6), maxLines = 1)
                Text(ArabicFormat.gregorian(ld, e) + "  •  " + ArabicFormat.hijri(ld, e), style = MaterialTheme.typography.titleLarge, color = Color(0xFF7D8BA1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                s.nextPrayer?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("${it.nameAr} بعد " + num(sa.moha.sonataos.prayer.PrayerCalc.countdown(java.util.Date(s.nowMillis), it.time)), style = MaterialTheme.typography.titleLarge, color = p.accent)
                }
            }
        }
    }
}
