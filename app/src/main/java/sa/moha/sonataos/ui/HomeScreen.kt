package sa.moha.sonataos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.moha.sonataos.prayer.PrayerCalc
import sa.moha.sonataos.util.ArabicFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

@Composable
fun HomeScreen(s: UiState, a: Actions) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 1000.dp
        val short = maxHeight < 420.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ClockPanel(s, Modifier.weight(1f).fillMaxWidth())
                    PrayerPanel(s, Modifier.fillMaxWidth(), compact = short)
                }
                SpeedPanel(s, Modifier.weight(1f).fillMaxHeight())
                if (!compact) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MediaPanel(s, a, Modifier.fillMaxWidth().weight(1f))
                        WeatherTripPanel(s, Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Dock(s, a, compact)
        }
    }
}

@Composable
fun ClockPanel(s: UiState, modifier: Modifier) {
    val p = LocalPalette.current
    val cal = Calendar.getInstance().apply { timeInMillis = s.nowMillis }
    val ld = Instant.ofEpochMilli(s.nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val e = LocalEastern.current
    Panel(modifier, pad = 20.dp) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight.value
            val big = (h * 0.42f).coerceIn(40f, 104f).sp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(ArabicFormat.clock(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), s.settings.clock24h, e),
                        fontSize = big, lineHeight = big * 1.05f, fontWeight = FontWeight.Light, color = p.text, maxLines = 1)
                    if (!s.settings.clock24h) {
                        Spacer(Modifier.width(8.dp))
                        Text(ArabicFormat.amPm(cal.get(Calendar.HOUR_OF_DAY)), style = MaterialTheme.typography.headlineMedium, color = p.accent, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                Text(ArabicFormat.gregorian(ld, e), style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ArabicFormat.hijri(ld, e), style = MaterialTheme.typography.bodyMedium, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PrayerPanel(s: UiState, modifier: Modifier, compact: Boolean = false) {
    val p = LocalPalette.current
    val e = LocalEastern.current
    Panel(modifier, glow = p.accent, pad = if (compact) 12.dp else 18.dp) {
        val next = s.nextPrayer
        if (next == null) {
            PanelTitle("أوقات الصلاة", Icons.Rounded.NightsStay)
            Text("فعّل الموقع عشان نحسب أوقات الصلاة", style = MaterialTheme.typography.bodyMedium, color = p.textDim)
            return@Panel
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NightsStay, null, tint = p.accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("باقي على ${next.nameAr}", style = MaterialTheme.typography.labelLarge, color = p.textDim, maxLines = 1)
                Text(num(PrayerCalc.countdown(Date(s.nowMillis), next.time)), style = MaterialTheme.typography.headlineMedium, color = p.text, maxLines = 1)
            }
            if (s.city.isNotBlank()) Chip(s.city)
        }
        if (!compact) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                s.prayers.filter { it.key != "sunrise" }.forEach { sl ->
                    val c = Calendar.getInstance().apply { time = sl.time }
                    val isNext = sl.key == next.key
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sl.nameAr, style = MaterialTheme.typography.labelMedium, color = if (isNext) p.accent else p.textDim, maxLines = 1)
                        Text(ArabicFormat.clock(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), s.settings.clock24h, e),
                            style = MaterialTheme.typography.titleMedium, color = if (isNext) p.accent else p.text, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedPanel(s: UiState, modifier: Modifier) {
    val p = LocalPalette.current
    val speed = s.speedKmh
    val over = s.settings.speedWarn && speed > s.settings.speedLimit
    Panel(modifier, glow = if (over) p.danger else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speed, null, tint = p.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (s.obd.status == sa.moha.sonataos.obd.ObdStatus.CONNECTED) "السرعة (OBD)" else if (s.geo.hasFix) "السرعة (GPS)" else "السرعة — بانتظار GPS",
                style = MaterialTheme.typography.labelLarge, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            SpeedLimitSign(s.settings.speedLimit, over)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArcGauge(speed, 220.0, if (over) "خفف السرعة!" else "كم/ساعة", "", Modifier.fillMaxHeight(), warnAt = if (s.settings.speedWarn) s.settings.speedLimit.toDouble() else null, big = true)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val hd = s.geo.bearing ?: s.geo.heading
            Icon(Icons.Rounded.Navigation, null, tint = p.accent, modifier = Modifier.size(24.dp).rotate(hd ?: 0f))
            Spacer(Modifier.width(6.dp))
            Text(if (hd != null) "${ArabicFormat.compassAr(hd)} ${num("%.0f".format(hd))}°" else "البوصلة غير متوفرة",
                style = MaterialTheme.typography.labelLarge, color = p.text, maxLines = 1, modifier = Modifier.weight(1f))
            s.obd.values[sa.moha.sonataos.obd.Pid.RPM]?.let { Text(num("%.0f".format(it)) + " rpm", style = MaterialTheme.typography.labelLarge, color = p.textDim) }
        }
    }
}

@Composable
fun SpeedLimitSign(limit: Int, over: Boolean) {
    val p = LocalPalette.current
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(Color.White).padding(4.dp)
        .clip(RoundedCornerShape(50)).background(if (over) p.danger else Color(0xFFD0202F)).padding(4.dp)
        .clip(RoundedCornerShape(50)).background(Color.White), contentAlignment = Alignment.Center) {
        Text(num("$limit"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun MediaPanel(s: UiState, a: Actions, modifier: Modifier) {
    val p = LocalPalette.current
    val m = s.media
    Panel(modifier) {
        PanelTitle("يشتغل الحين", Icons.Rounded.MusicNote)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(p.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                val art = m.art
                if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize())
                else Icon(Icons.Rounded.Album, null, tint = p.accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                when {
                    !s.mediaAccess -> {
                        Text("عطني صلاحية الإشعارات عشان أعرض الأغاني", style = MaterialTheme.typography.bodyMedium, color = p.textDim, maxLines = 2)
                        TextButton(onClick = a::grantMedia, modifier = Modifier.heightIn(min = 48.dp)) { Text("فعّلها", color = p.accent) }
                    }
                    !m.active -> Text("ما فيه شي يشتغل", style = MaterialTheme.typography.titleMedium, color = p.textDim)
                    else -> {
                        Text(m.title.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(m.artist, style = MaterialTheme.typography.bodyMedium, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // Media transport keeps the universal LTR order (prev | play | next) like Arabic Android players.
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundIcon(Icons.Rounded.SkipPrevious, "السابق", onClick = a::mediaPrev)
                RoundIcon(if (m.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "تشغيل/إيقاف", active = true, onClick = a::mediaPlayPause)
                RoundIcon(Icons.Rounded.SkipNext, "التالي", onClick = a::mediaNext)
            }
        }
    }
}

@Composable
fun WeatherTripPanel(s: UiState, modifier: Modifier) {
    val p = LocalPalette.current
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val w = s.weather
            Icon(if (w == null || w.code <= 1) Icons.Rounded.WbSunny else Icons.Rounded.Cloud, null, tint = p.warn, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (w == null) Text("الطقس: بانتظار الموقع/النت", style = MaterialTheme.typography.bodyMedium, color = p.textDim, maxLines = 1)
                else {
                    Text(num("%.0f°".format(w.tempC)) + "  " + w.descAr, style = MaterialTheme.typography.titleLarge, color = p.text, maxLines = 1)
                    Text(num("العظمى %.0f° • الصغرى %.0f°".format(w.maxC, w.minC)) + if (w.cached) " • محفوظ" else "", style = MaterialTheme.typography.labelMedium, color = p.textDim, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("المشوار", num("%.1f".format(s.trip.km)) + " كم")
            MiniStat("المدة", num("%d:%02d".format(s.trip.seconds / 3600, (s.trip.seconds % 3600) / 60)))
            MiniStat("أعلى سرعة", num("%.0f".format(s.trip.maxKmh)))
        }
    }
}

@Composable
fun MiniStat(label: String, value: String) {
    val p = LocalPalette.current
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.textDim, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = p.text, maxLines = 1)
    }
}

@Composable
fun Dock(s: UiState, a: Actions, compact: Boolean) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.surface).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BigButton("البيت", Icons.Rounded.Home, filled = true, onClick = a::navigateHome)
        BigButton("الدوام", Icons.Rounded.Work, onClick = a::navigateWork)
        if (!compact) BigButton("الخرائط", Icons.Rounded.Map, onClick = a::openMaps)
        BigButton("الموقف", Icons.Rounded.LocalParking, onClick = a::saveParking)
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val favs = s.settings.favorites.mapNotNull { f -> s.apps.firstOrNull { it.pkg == f } }
            items(favs, key = { it.pkg }) { app ->
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(p.surfaceHi).clickable { a.launch(app.pkg) }, contentAlignment = Alignment.Center) {
                    app.icon?.let { Image(it, app.label, Modifier.size(44.dp)) } ?: Icon(Icons.Rounded.Apps, app.label, tint = p.text)
                }
            }
        }
        RoundIcon(Icons.Rounded.Mic, "المساعد", active = true, onClick = a::listen)
    }
}
