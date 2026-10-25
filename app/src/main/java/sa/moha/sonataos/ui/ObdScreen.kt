package sa.moha.sonataos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sa.moha.sonataos.obd.DtcDescriptions
import sa.moha.sonataos.obd.ObdStatus
import sa.moha.sonataos.obd.Pid

@Composable
fun ObdScreen(s: UiState, a: Actions) {
    val p = LocalPalette.current
    val o = s.obd
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (txt, col) = when (o.status) {
                ObdStatus.CONNECTED -> (if (o.demo) "وضع تجريبي (محاكاة)" else "متصل بالـ ELM327") to p.ok
                ObdStatus.CONNECTING -> "جاري الاتصال…" to p.warn
                ObdStatus.ERROR -> o.message to p.danger
                ObdStatus.DISCONNECTED -> "غير متصل" to p.textDim
            }
            Chip(txt, col)
            Spacer(Modifier.weight(1f))
            if (o.status == ObdStatus.CONNECTED) {
                BigButton(if (o.recording) "وقف التسجيل" else "سجّل", Icons.Rounded.FiberManualRecord, tint = if (o.recording) p.danger else null, onClick = a::obdRecord)
                BigButton("افصل", Icons.Rounded.LinkOff, onClick = a::obdDisconnect)
            } else {
                BigButton("اختر القطعة", Icons.Rounded.Bluetooth, onClick = a::pickObdDevice)
                BigButton(if (s.settings.demoMode) "شغّل التجريبي" else "اتصل", Icons.Rounded.Link, filled = true, onClick = a::obdConnect)
            }
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val compact = maxWidth < 1000.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(if (compact) 1.4f else 1.6f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        ArcGauge(o.values[Pid.RPM] ?: 0.0, 7000.0, "دورات المحرك", "rpm", Modifier.weight(1f), warnAt = 6000.0)
                        ArcGauge(o.values[Pid.SPEED] ?: 0.0, 220.0, "السرعة", "كم/س", Modifier.weight(1f))
                        if (!compact) ArcGauge(o.values[Pid.COOLANT_TEMP] ?: 0.0, 130.0, "حرارة المحرك", "°م", Modifier.weight(1f), warnAt = 105.0)
                    }
                    val bars = listOf(
                        Triple(Pid.FUEL_LEVEL, 100.0, 0), Triple(Pid.ENGINE_LOAD, 100.0, 0), Triple(Pid.THROTTLE, 100.0, 0),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        bars.forEach { (pid, mx, d) ->
                            BarStat(pid.labelAr, o.values[pid], mx, pid.unit, Modifier.weight(1f), d, warn = pid == Pid.FUEL_LEVEL && (o.values[pid] ?: 100.0) < 12)
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Panel(Modifier.fillMaxWidth()) {
                        val v = o.voltage ?: o.values[Pid.MODULE_VOLTAGE]
                        BarStat("فولت البطارية", v, 16.0, "V", decimals = 1, warn = v != null && (v < 12.0 || v > 15.0))
                        Spacer(Modifier.height(10.dp))
                        if (compact) { BarStat("حرارة المحرك", o.values[Pid.COOLANT_TEMP], 130.0, "°م", warn = (o.values[Pid.COOLANT_TEMP] ?: 0.0) > 105); Spacer(Modifier.height(10.dp)) }
                        BarStat("حرارة الهواء الداخل", o.values[Pid.INTAKE_TEMP], 80.0, "°م")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat("تعديل قصير", o.values[Pid.STFT_B1]?.let { num("%+.1f".format(it)) + "%" } ?: "—")
                            MiniStat("تعديل طويل", o.values[Pid.LTFT_B1]?.let { num("%+.1f".format(it)) + "%" } ?: "—")
                            MiniStat("MAF", o.values[Pid.MAF]?.let { num("%.1f".format(it)) + " g/s" } ?: "—")
                        }
                    }
                    Panel(Modifier.fillMaxWidth().weight(1f)) {
                        PanelTitle("أكواد الأعطال (${num(o.dtcs.size.toString())})", Icons.Rounded.Warning) {
                            TextButton(onClick = a::obdReadDtc, modifier = Modifier.heightIn(min = 48.dp)) { Text("اقرأ", color = p.accent) }
                            TextButton(onClick = { confirmClear = true }, enabled = o.dtcs.isNotEmpty() && o.status == ObdStatus.CONNECTED, modifier = Modifier.heightIn(min = 48.dp)) { Text("امسح", color = p.danger) }
                        }
                        if (o.dtcs.isEmpty()) Text(if (o.status == ObdStatus.CONNECTED) "ما فيه أعطال مسجلة ✓" else "اتصل عشان نقرأ الأعطال", color = p.textDim, style = MaterialTheme.typography.bodyMedium)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(o.dtcs) { c ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Chip(c, p.warn); Spacer(Modifier.width(8.dp))
                                    Text(DtcDescriptions.describe(c), style = MaterialTheme.typography.bodyMedium, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("قراءة فقط: التطبيق ما يكتب على كمبيوتر السيارة. PIDs قياسية OBD-II، وأكواد هيونداي الخاصة غير مدعومة/غير مجربة.",
            style = MaterialTheme.typography.labelMedium, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("متأكد تبي تمسح الأعطال؟") },
        text = { Text("المسح يطفي لمبة المكينة ويصفّر فحوصات الجاهزية، لكنه ما يصلح العطل. لا تمسح وأنت تسوق، وسجّل الأكواد قبل.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; a.obdClearDtc() }) { Text("إيه، امسح", color = p.danger) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("لا، خلها") } },
    )
}
