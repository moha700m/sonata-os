package sa.moha.sonataos.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VoiceScreen(s: UiState, a: Actions) {
    val p = LocalPalette.current
    val pulse = rememberInfiniteTransition(label = "p").animateFloat(1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).scale(if (s.listening) pulse.value else 1f).clip(CircleShape)
                    .background(if (s.listening) p.danger else p.accent).clickable(onClick = a::listen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, "تكلم", tint = Color.White, modifier = Modifier.size(72.dp))
                }
            }
            Text(if (s.listening) "أسمعك… تكلم" else "اضغط وقول مثلاً: «وديني البيت»", style = MaterialTheme.typography.titleLarge, color = p.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            if (s.voiceText.isNotBlank()) Text("«${s.voiceText}»", style = MaterialTheme.typography.bodyLarge, color = p.textDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            if (s.voiceReply.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(s.voiceReply, style = MaterialTheme.typography.titleMedium, color = p.accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            PanelTitle("أوامر تقدر تقولها")
            Spacer(Modifier.height(8.dp))
            val cmds = listOf("وديني البيت", "خذني الدوام", "وديني مطار الملك خالد", "اتصل على أبوي", "شغل الأغاني", "الأغنية الجاية", "وقف الأغنية",
                "افتح الحساسات", "احفظ الموقف", "وين وقفت", "متى يأذن", "كيف الجو", "افتح الكاميرا")
            FlowRowSimple(cmds) { c ->
                Text(c, style = MaterialTheme.typography.labelLarge, color = p.text,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(p.surfaceHi).clickable { a.runText(c) }.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(if (s.settings.llmKey.isBlank()) "الأوامر تشتغل محلياً بدون نت مدفوع. التعرف على الصوت يعتمد على خدمة Google بالجهاز." else "أي سؤال ما يفهمه التطبيق يروح لمزود الذكاء اللي حطيته بالإعدادات.",
                style = MaterialTheme.typography.labelMedium, color = p.textDim)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FlowRowSimple(items: List<T>, item: @Composable (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items.forEach { item(it) } }
}
