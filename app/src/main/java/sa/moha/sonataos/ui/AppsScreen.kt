package sa.moha.sonataos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(s: UiState, a: Actions) {
    val p = LocalPalette.current
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Android Auto / CarPlay", Icons.Rounded.DirectionsCar, filled = true, onClick = a::openCarApp)
            BigButton("الخرائط", Icons.Rounded.Map, onClick = a::openMaps)
            BigButton("الموسيقى", Icons.Rounded.MusicNote, onClick = a::openMusic)
            OutlinedTextField(q, { q = it }, placeholder = { Text("دوّر تطبيق…") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                shape = RoundedCornerShape(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("اضغط مطوّل على أي تطبيق عشان تضيفه/تشيله من المفضلة", style = MaterialTheme.typography.labelMedium, color = p.textDim)
        Spacer(Modifier.height(8.dp))
        val list = s.apps.filter { q.isBlank() || it.label.contains(q, true) }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ما لقيت تطبيقات", color = p.textDim, style = MaterialTheme.typography.titleMedium) }
        }
        LazyVerticalGrid(GridCells.Adaptive(132.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.pkg + it.activity }) { app ->
                val fav = app.pkg in s.settings.favorites
                Column(
                    Modifier.clip(RoundedCornerShape(22.dp)).background(p.surface)
                        .combinedClickable(onClick = { a.launch(app.pkg) }, onLongClick = { a.toggleFavorite(app.pkg) })
                        .padding(12.dp).heightIn(min = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        app.icon?.let { Image(it, null, Modifier.size(64.dp)) } ?: Icon(Icons.Rounded.Apps, null, Modifier.size(64.dp), tint = p.text)
                        if (fav) Icon(Icons.Rounded.Star, "مفضل", tint = p.warn, modifier = Modifier.size(20.dp).align(Alignment.TopEnd))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(app.label, style = MaterialTheme.typography.labelLarge, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
