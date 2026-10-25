package sa.moha.sonataos.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.moha.sonataos.util.ArabicFormat
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun num(s: String): String = ArabicFormat.digits(s, LocalEastern.current)

@Composable
fun Panel(modifier: Modifier = Modifier, glow: Color? = null, pad: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(p.surfaceHi, p.surface)))
            .border(1.dp, glow?.copy(alpha = 0.55f) ?: p.stroke, shape)
            .padding(pad),
        content = content
    )
}

@Composable
fun PanelTitle(text: String, icon: ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) { Icon(icon, null, tint = p.accent, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Min 64dp touch target, glanceable. */
@Composable
fun BigButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, tint: Color? = null, filled: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = tint ?: p.accent
    Row(
        modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (filled) c else c.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (filled) Color.White else c, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (filled) Color.White else p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RoundIcon(icon: ImageVector, desc: String, modifier: Modifier = Modifier, size: Dp = 64.dp, active: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier.size(size).clip(CircleShape)
            .background(if (active) p.accent else p.surfaceHi)
            .border(1.dp, p.stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (active) Color.White else p.text, modifier = Modifier.size(size * 0.45f)) }
}

/**
 * Animated arc gauge (240° sweep). Drawn LTR on purpose: gauges read the same in any language.
 */
@Composable
fun ArcGauge(
    value: Double, max: Double, label: String, unit: String, modifier: Modifier = Modifier,
    warnAt: Double? = null, decimals: Int = 0, big: Boolean = false,
) {
    val p = LocalPalette.current
    val frac by animateFloatAsState((value / max).coerceIn(0.0, 1.0).toFloat(), animationSpec = spring(stiffness = Spring.StiffnessLow), label = "g")
    val color = if (warnAt != null && value >= warnAt) p.danger else p.accent
    BoxWithConstraints(modifier.aspectRatio(1.1f), contentAlignment = Alignment.Center) {
        val s = minOf(maxWidth, maxHeight)
        Canvas(Modifier.size(s)) {
            val stroke = size.minDimension * 0.075f
            val inset = stroke / 2 + 4
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(p.stroke, 150f, 240f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(color.copy(alpha = 0.35f), color, color), center),
                150f, 240f * frac, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ticks
            val r = arcSize.width / 2
            for (i in 0..12) {
                val a = Math.toRadians((150 + i * 20).toDouble())
                val r1 = r - stroke * 1.2f; val r2 = r - stroke * (if (i % 2 == 0) 2.0f else 1.6f)
                drawLine(p.textDim.copy(alpha = 0.6f), Offset(center.x + (r1 * cos(a)).toFloat(), center.y + (r1 * sin(a)).toFloat()),
                    Offset(center.x + (r2 * cos(a)).toFloat(), center.y + (r2 * sin(a)).toFloat()), strokeWidth = 2f)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val fs = (s.value * if (big) 0.26f else 0.24f).sp
            Text(num(if (decimals == 0) "%.0f".format(value) else "%.${decimals}f".format(value)),
                fontSize = fs, lineHeight = fs * 1.1f, fontWeight = FontWeight.SemiBold, color = if (color == p.danger) p.danger else p.text, maxLines = 1)
            Text(unit, fontSize = (s.value * 0.075f).coerceAtLeast(11f).sp, color = p.textDim, maxLines = 1)
            Text(label, fontSize = (s.value * 0.085f).coerceAtLeast(12f).sp, color = p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** Horizontal bar for secondary values (fuel, load, throttle). */
@Composable
fun BarStat(label: String, value: Double?, max: Double, unit: String, modifier: Modifier = Modifier, decimals: Int = 0, warn: Boolean = false) {
    val p = LocalPalette.current
    val frac by animateFloatAsState(((value ?: 0.0) / max).coerceIn(0.0, 1.0).toFloat(), label = "b")
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = p.textDim, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (value == null) "—" else num("%.${decimals}f".format(value)) + " " + unit, style = MaterialTheme.typography.titleMedium, color = if (warn) p.danger else p.text, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.stroke)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(if (warn) p.danger else p.accent))
        }
    }
}

@Composable
fun Chip(text: String, color: Color? = null) {
    val p = LocalPalette.current
    val c = color ?: p.accent
    Text(text, style = MaterialTheme.typography.labelMedium, color = c, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 4.dp))
}
