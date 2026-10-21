package com.flowtv.launcher.ui

import android.media.tv.TvContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.flowtv.launcher.data.*

@Composable
fun HomeScreen(apps: List<AppItem>, feed: TitleFeed, loading: Boolean, allApps: Boolean,
               onApp: (AppItem) -> Unit, onTitle: (TitleCard) -> Unit, onRefresh: () -> Unit,
               onSettings: () -> Unit, onAllApps: () -> Unit, onHome: () -> Unit) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(Modifier.fillMaxSize().background(Navy).safeDrawingPadding().padding(if (wide) 24.dp else 16.dp)) {
        Text("Flow TV", style = MaterialTheme.typography.headlineMedium)
        Text("PROTOTYPE / ${if (allApps) "ALL APPS" else "HOME"}", color = Lavender,
            style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowButton(onClick = onRefresh, modifier = Modifier.focusRequester(first)
                .semantics { contentDescription = "Refresh installed apps and permitted TV cards" }) { Text("Refresh") }
            FlowButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "Prototype settings" }) { Text("Settings") }
            if (allApps) FlowButton(onClick = onHome) { Text("Home") }
        }
        Spacer(Modifier.height(14.dp))
        if (allApps) {
            Text("${apps.size} actual installed apps", color = Muted)
            LazyVerticalGrid(columns = GridCells.Adaptive(148.dp), modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)) {
                gridItems(apps, key = { it.pkg }) { a -> AppTile(a, onApp, Modifier.fillMaxWidth()) }
            }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Slate, RoundedCornerShape(20.dp)).padding(18.dp)) {
                    Text(if (loading) "Refreshing device data…" else "Your apps, ready to open",
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp)); Text(feed.status, color = Muted)
                    Spacer(Modifier.height(8.dp))
                    Text("This separate prototype has app launching and optional TV cards. Use the existing Flow TV app for Dashboard and audio.",
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            val continuing = feed.watchNext.filter { it.watchType == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE }
            val other = feed.watchNext.filter { it.watchType != TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE }
            if (continuing.isNotEmpty()) item { TitleRow("Continue watching · source-published", continuing, onTitle) }
            if (other.isNotEmpty()) item { TitleRow("Shared Watch Next cards", other, onTitle) }
            if (feed.previews.isNotEmpty()) item { TitleRow("Shared TV recommendations", feed.previews, onTitle) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Installed apps", style = MaterialTheme.typography.titleLarge)
                    if (apps.isEmpty()) Text("No launchable apps found. Refresh or open device settings.", color = Muted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
                        items(apps.take(20), key = { it.pkg }) { a -> AppTile(a, onApp, Modifier.width(if (wide) 190.dp else 148.dp)) }
                    }
                    FlowButton(onClick = onAllApps) { Text("All Apps (${apps.size})") }
                }
            }
            item { Text("Actual installed icons/banners · touch and D-pad navigation · no private catalogue access", color = Muted) }
        }
    }
}

@Composable
private fun AppTile(app: AppItem, onApp: (AppItem) -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    Card(onClick = { onApp(app) }, modifier = modifier.touchClick { onApp(app) }.height(140.dp)
        .onFocusChanged { focused = it.hasFocus }.border(if (focused) 2.dp else 1.dp, if (focused) Mint else androidx.compose.ui.graphics.Color(0xFF283552), RoundedCornerShape(18.dp))
        .semantics { contentDescription = "Open ${app.label}" }, shape = CardDefaults.shape(RoundedCornerShape(18.dp))) {
        Column(Modifier.fillMaxSize().background(Slate).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (app.banner != null) Image(app.banner.asImageBitmap(), null,
                Modifier.fillMaxWidth().height(82.dp), contentScale = ContentScale.Fit)
            else Image(app.icon.asImageBitmap(), null, Modifier.size(72.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(6.dp)); Text(app.label, maxLines = 2, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TitleRow(label: String, titles: List<TitleCard>, onTitle: (TitleCard) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(4.dp)) {
            items(titles, key = { it.key }) { t ->
                var focused by remember(t.key) { mutableStateOf(false) }
                Card(onClick = { onTitle(t) }, modifier = Modifier.touchClick { onTitle(t) }.size(160.dp, 245.dp)
                    .onFocusChanged { focused = it.hasFocus }.border(if (focused) 2.dp else 1.dp, if (focused) Mint else androidx.compose.ui.graphics.Color(0xFF283552), RoundedCornerShape(18.dp)),
                    shape = CardDefaults.shape(RoundedCornerShape(18.dp))) {
                    Column(Modifier.fillMaxSize().background(Slate).padding(8.dp)) {
                        var failed by remember(t.posterUri) { mutableStateOf(t.posterUri == null) }
                        Box(Modifier.fillMaxWidth().height(165.dp), contentAlignment = Alignment.Center) {
                            if (!failed) AsyncImage(t.posterUri, "Source artwork", Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop, onError = { failed = true })
                            else Text("Artwork unavailable", color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(t.title, maxLines = 2, style = MaterialTheme.typography.labelLarge)
                        Text(t.source, maxLines = 1, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
