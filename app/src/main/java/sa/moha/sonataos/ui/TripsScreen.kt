package sa.moha.sonataos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sa.moha.sonataos.trip.TripMath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripsScreen(s: UiState, a: Actions) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.fillMaxWidth()) {
                PanelTitle("المشوار الحالي", Icons.Rounded.Route) { Chip(if (s.trip.active) "شغّال" else "موقف", if (s.trip.active) p.ok else p.textDim) }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("المسافة", num("%.1f".format(s.trip.km)) + " كم")
                    MiniStat("المدة", num("%d:%02d".format(s.trip.seconds / 3600, (s.trip.seconds % 3600) / 60)))
                    MiniStat("المتوسط", num("%.0f".format(TripMath.averageSpeedKmh(s.trip.km, s.trip.seconds))) + " كم/س")
                    MiniStat("البنزين", if (s.trip.fuelL > 0) num("%.2f".format(s.trip.fuelL)) + " لتر" else "يحتاج OBD")
                }
                Spacer(Modifier.height(12.dp))
                val kml = TripMath.kmPerLiter(s.trip.fuelL, s.trip.km)
                Text(if (kml != null) num("الاستهلاك التقديري: %.1f كم/لتر • التكلفة ≈ %.1f ريال".format(kml, TripMath.fuelCostSar(s.trip.fuelL, s.settings.fuelPrice.toDouble()))) else "الاستهلاك يُحسب من حساس MAF لما يكون OBD متصل (تقدير مو دقيق 100%)",
                    style = MaterialTheme.typography.bodyMedium, color = p.textDim)
                Spacer(Modifier.height(12.dp))
                BigButton(if (s.trip.active) "خلّص المشوار واحفظه" else "ابدأ مشوار", if (s.trip.active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, filled = !s.trip.active, onClick = a::tripStartStop, modifier = Modifier.fillMaxWidth())
            }
            Panel(Modifier.fillMaxWidth().weight(1f)) {
                PanelTitle("الصيانة", Icons.Rounded.Build)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.maintenance) { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.titleAr, style = MaterialTheme.typography.titleMedium, color = if (m.due) p.danger else p.text)
                                Text(listOfNotNull(m.remainingKm?.let { num("باقي $it كم") }, m.remainingDays?.let { num("باقي $it يوم") }).joinToString(" • ").ifBlank { "سجّل آخر مرة" },
                                    style = MaterialTheme.typography.labelMedium, color = if (m.soon || m.due) p.warn else p.textDim)
                            }
                            TextButton(onClick = { when (m.key) { "oil" -> a.markOilDone(); "tires" -> a.markTiresDone(); else -> a.markServiceDone() } }, modifier = Modifier.heightIn(min = 56.dp)) {
                                Text("سويتها اليوم", color = p.accent)
                            }
                        }
                    }
                }
            }
        }
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            PanelTitle("سجل المشاوير", Icons.Rounded.History)
            Spacer(Modifier.height(8.dp))
            if (s.trips.isEmpty()) Text("ما فيه مشاوير محفوظة للحين", color = p.textDim, style = MaterialTheme.typography.bodyMedium)
            val df = SimpleDateFormat("d/M HH:mm", Locale.US)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.trips) { t ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surfaceHi).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(num(df.format(Date(t.start))), style = MaterialTheme.typography.labelLarge, color = p.textDim, modifier = Modifier.width(110.dp))
                        Text(num("%.1f كم".format(t.km)), style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.weight(1f))
                        Text(num("متوسط %.0f • أعلى %.0f".format(t.avgKmh, t.maxKmh)), style = MaterialTheme.typography.labelMedium, color = p.textDim)
                    }
                }
            }
        }
    }
}
