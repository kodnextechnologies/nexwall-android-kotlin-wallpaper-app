package com.kodnex.nexwall.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kodnex.nexwall.sample.data.Category
import com.kodnex.nexwall.sample.data.Wallpaper
import com.kodnex.nexwall.sample.data.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: WallpaperViewModel) {
    val state by vm.categories.collectAsStateWithLifecycle()
    val remaining by vm.remainingToday.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                actions = {
                    QuotaText(remaining)
                    TextButton(onClick = { vm.openCategory(null) }) { Text("All") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> ErrorBox(state.error!!, onRetry = vm::loadCategories)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.id }) { category ->
                        CategoryTile(category) { vm.openCategory(category) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = category.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
        )
        Text(
            text = "${category.name} (${category.wallpaperCount})",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpapersScreen(vm: WallpaperViewModel, category: Category?) {
    val state by vm.grid.collectAsStateWithLifecycle()
    val remaining by vm.remainingToday.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    var sortMenu by remember { mutableStateOf(false) }

    // Infinite scroll: request the next page when the last 6 items are visible.
    // Emitting the item count (not just a boolean) re-triggers after each page,
    // so a page that doesn't fill the screen still loads the next one.
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last >= info.totalItemsCount - 6) info.totalItemsCount else -1
        }.collect { if (it >= 0) vm.loadMore() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "All wallpapers") },
                navigationIcon = { TextButton(onClick = { vm.back() }) { Text("Back") } },
                actions = {
                    QuotaText(remaining)
                    Box {
                        TextButton(onClick = { sortMenu = true }) { Text(state.sort.replaceFirstChar { it.uppercase() }) }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("newest", "popular", "random", "oldest").forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        sortMenu = false
                                        vm.changeSort(sort)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.items.isEmpty() && state.error != null) {
                ErrorBox(state.error!!, onRetry = vm::retry)
            } else if (state.items.isEmpty() && !state.loading) {
                Text("No wallpapers found.", Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { wallpaper ->
                        AsyncImage(
                            model = wallpaper.previewUrl,
                            contentDescription = wallpaper.category?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { vm.openPreview(wallpaper) },
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            when {
                                state.loading -> CircularProgressIndicator()
                                state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(state.error!!, textAlign = TextAlign.Center)
                                    TextButton(onClick = vm::retry) { Text("Retry") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewScreen(vm: WallpaperViewModel, wallpaper: Wallpaper) {
    val message by vm.messages.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var chooseTarget by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            busy = false
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Thumbnail first (already cached from the grid), full image on top.
        AsyncImage(
            model = wallpaper.previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model = wallpaper.imageUrl,
            contentDescription = "Wallpaper ${wallpaper.id}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { vm.back() }) { Text("Back") }
            Button(onClick = { chooseTarget = true }, enabled = !busy && wallpaper.type == "image") {
                Text(if (busy) "Setting..." else "Set as wallpaper")
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
    }

    if (chooseTarget) {
        AlertDialog(
            onDismissRequest = { chooseTarget = false },
            title = { Text("Set wallpaper on") },
            text = {
                Column {
                    WallpaperTarget.entries.forEach { target ->
                        TextButton(onClick = {
                            chooseTarget = false
                            busy = true
                            vm.setWallpaper(wallpaper, target)
                        }) {
                            Text(
                                when (target) {
                                    WallpaperTarget.HOME -> "Home screen"
                                    WallpaperTarget.LOCK -> "Lock screen"
                                    WallpaperTarget.BOTH -> "Home and lock screen"
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseTarget = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun QuotaText(remaining: Int?) {
    if (remaining != null) {
        Text(
            "$remaining left today",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
    }
}
