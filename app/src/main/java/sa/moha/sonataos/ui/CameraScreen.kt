package sa.moha.sonataos.ui

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Reverse/dash camera viewer. Works with any camera the OS exposes through Camera2 (incl. USB UVC
 * cameras on devices whose HAL supports EXTERNAL cameras). Raw UVC over USB without OS support is NOT implemented.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraScreen(s: UiState, hasPermission: Boolean, requestPermission: () -> Unit) {
    val p = LocalPalette.current
    var error by remember { mutableStateOf<String?>(null) }
    Panel(Modifier.fillMaxSize(), pad = 8.dp) {
        if (LocalInspectionMode.current || !hasPermission) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Videocam, null, tint = p.accent, modifier = Modifier.size(64.dp))
                    Text("كاميرا الخلفية / الطريق", style = MaterialTheme.typography.titleLarge, color = p.text)
                    Text("تشتغل إذا الجهاز يتعرف على كاميرا USB أو داخلية", style = MaterialTheme.typography.bodyMedium, color = p.textDim)
                    Spacer(Modifier.height(12.dp))
                    if (!hasPermission) BigButton("اسمح بالكاميرا", Icons.Rounded.Videocam, filled = true, onClick = requestPermission)
                }
            }
            return@Panel
        }
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { c ->
                PreviewView(c).also { pv ->
                    val fut = ProcessCameraProvider.getInstance(c)
                    fut.addListener({
                        runCatching {
                            val prov = fut.get()
                            val sel = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
                                .firstOrNull { prov.hasCamera(it) } ?: prov.availableCameraInfos.firstOrNull()?.cameraSelector
                            if (sel == null) { error = "ما لقيت أي كاميرا على الجهاز"; return@runCatching }
                            val prev = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                            prov.unbindAll(); prov.bindToLifecycle(owner, sel, prev)
                        }.onFailure { error = "تعذر تشغيل الكاميرا: ${it.message}" }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }, modifier = Modifier.fillMaxSize())
            error?.let { Text(it, color = p.danger, modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium) }
            Chip(if (s.moving) "انتبه للطريق" else "عرض مباشر", if (s.moving) p.warn else p.ok)
        }
    }
}
