package sa.moha.sonataos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import sa.moha.sonataos.data.AppSettings
import sa.moha.sonataos.data.ThemeMode

@Composable
fun SettingsScreen(s: UiState, a: Actions) {
    val st = s.settings
    val up: ((AppSettings) -> AppSettings) -> Unit = a::updateSettings
    LazyVerticalStaggeredGrid(StaggeredGridCells.Adaptive(400.dp), verticalItemSpacing = 14.dp, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Section("المظهر", Icons.Rounded.Palette) {
                Segmented(listOf("تلقائي", "ليلي", "نهاري"), ThemeMode.entries.indexOf(st.themeMode)) { i -> up { it.copy(themeMode = ThemeMode.entries[i]) } }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.all.forEachIndexed { i, c ->
                        val p = LocalPalette.current
                        Box(Modifier.size(52.dp).clip(CircleShape).background(Color(c)).border(3.dp, if (st.accent == c) p.text else Color.Transparent, CircleShape)
                            .clickable { up { it.copy(accent = c) } })
                    }
                }
                SliderRow("حجم الخط", st.fontScale, 0.85f..1.3f) { v -> up { it.copy(fontScale = v) } }
                SwitchRow("أرقام عربية (١٢٣)", st.easternDigits) { v -> up { it.copy(easternDigits = v) } }
                SwitchRow("ساعة ٢٤", st.clock24h) { v -> up { it.copy(clock24h = v) } }
                SwitchRow("تعتيم تلقائي بالليل", st.nightDim) { v -> up { it.copy(nightDim = v) } }
                if (st.nightDim) SliderRow("قوة التعتيم", st.nightDimLevel, 0f..0.7f) { v -> up { it.copy(nightDimLevel = v) } }
                SliderRow("شاشة الساعة بعد (دقايق، ٠ = مقفلة)", st.screensaverMin.toFloat(), 0f..15f, steps = 14) { v -> up { it.copy(screensaverMin = v.toInt()) } }
            }
        }
        item {
            Section("القيادة والتنبيهات", Icons.Rounded.Speed) {
                SwitchRow("تنبيه تجاوز السرعة", st.speedWarn) { v -> up { it.copy(speedWarn = v) } }
                SliderRow("حد السرعة: ${num(st.speedLimit.toString())} كم/س", st.speedLimit.toFloat(), 40f..160f, steps = 11) { v -> up { it.copy(speedLimit = v.toInt()) } }
                SwitchRow("تذكير الأذان (أم القرى)", st.adhanReminder) { v -> up { it.copy(adhanReminder = v) } }
                Field("عنوان البيت", st.homeAddress) { v -> up { it.copy(homeAddress = v) } }
                Field("عنوان الدوام", st.workAddress) { v -> up { it.copy(workAddress = v) } }
            }
        }
        item {
            Section("OBD2 (ELM327)", Icons.Rounded.Bluetooth) {
                SwitchRow("وضع تجريبي (بدون سيارة)", st.demoMode) { v -> up { it.copy(demoMode = v) } }
                SwitchRow("القطعة BLE (مو كلاسيك)", st.obdBle) { v -> up { it.copy(obdBle = v) } }
                SwitchRow("اتصل تلقائياً لما يشتغل التطبيق", st.obdAutoConnect) { v -> up { it.copy(obdAutoConnect = v) } }
                Text("القطعة المختارة: " + st.obdMac.ifBlank { "ما اخترت" }, style = MaterialTheme.typography.bodyMedium, color = LocalPalette.current.textDim)
                Spacer(Modifier.height(8.dp))
                BigButton("اختر قطعة OBD", Icons.Rounded.BluetoothSearching, onClick = a::pickObdDevice)
            }
        }
        item {
            Section("الصيانة والبنزين", Icons.Rounded.Build) {
                Field("العداد الحالي (كم)", st.odometerKm.toString(), number = true) { v -> v.toIntOrNull()?.let { n -> up { it.copy(odometerKm = n) } } }
                Field("آخر غيار زيت عند (كم)", st.lastOilKm.toString(), number = true) { v -> v.toIntOrNull()?.let { n -> up { it.copy(lastOilKm = n) } } }
                Field("الزيت كل (كم)", st.oilEveryKm.toString(), number = true) { v -> v.toIntOrNull()?.let { n -> up { it.copy(oilEveryKm = n) } } }
                Field("سعر لتر البنزين (ريال)", st.fuelPrice.toString(), number = true) { v -> v.toFloatOrNull()?.let { n -> up { it.copy(fuelPrice = n) } } }
            }
        }
        item {
            Section("المساعد الذكي (اختياري)", Icons.Rounded.AutoAwesome) {
                Text("حط رابط ومفتاح مزود متوافق مع OpenAI لو تبي. المفتاح ينحفظ بجهازك بس وما ينرفع لأي مكان غير المزود.", style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.textDim)
                Field("الرابط (…/v1/chat/completions)", st.llmEndpoint) { v -> up { it.copy(llmEndpoint = v) } }
                Field("المفتاح", st.llmKey, secret = true) { v -> up { it.copy(llmKey = v) } }
                Field("الموديل", st.llmModel) { v -> up { it.copy(llmModel = v) } }
            }
        }
        item {
            Section("النظام", Icons.Rounded.Settings) {
                BigButton("خلّه الواجهة الأساسية (Home)", Icons.Rounded.Home, filled = true, onClick = a::setDefaultHome, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("نسخة احتياطية", Icons.Rounded.Backup, onClick = a::backup, modifier = Modifier.weight(1f))
                    BigButton("استرجاع", Icons.Rounded.Restore, onClick = a::restore, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                BigButton("صلاحية عرض الأغاني", Icons.Rounded.MusicNote, onClick = a::grantMedia, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("سوناتا OS ١.٠ — مشروع مفتوح، ما تم تجربته على سيارة حقيقية للحين.", style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.textDim)
            }
        }
    }
}

@Composable
fun Section(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Panel(Modifier.fillMaxWidth()) { PanelTitle(title, icon); Spacer(Modifier.height(8.dp)); content() }
}

@Composable
fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onChange(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.text, modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
    }
}

@Composable
fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.text)
        Slider(value, onChange, valueRange = range, steps = steps, modifier = Modifier.heightIn(min = 48.dp),
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent))
    }
}

@Composable
fun Field(label: String, value: String, number: Boolean = false, secret: Boolean = false, onChange: (String) -> Unit) {
    var t by remember(value) { mutableStateOf(value) }
    OutlinedTextField(t, { t = it; onChange(it) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else if (secret) KeyboardType.Password else KeyboardType.Text),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surfaceHi).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            Box(Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(14.dp)).background(if (i == selected) p.accent else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center) { Text(o, style = MaterialTheme.typography.titleMedium, color = if (i == selected) Color.White else p.text) }
        }
    }
}
