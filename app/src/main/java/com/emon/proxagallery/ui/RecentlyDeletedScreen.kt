package com.emon.proxagallery.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.emon.proxagallery.data.TrashItem
import java.io.File

@Composable
fun RecentlyDeletedScreen(
    onBackClick: () -> Unit,
    onItemRestored: () -> Unit = {},
    viewModel: TrashViewModel = viewModel(
        factory = TrashViewModelFactory(LocalContext.current)
    )
) {
    val items by viewModel.trashItems.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var selectedItemIndex by remember { mutableIntStateOf(-1) }

    val deleteAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.confirmDeleteAllAfterPermission()
        } else {
            viewModel.cancelDeleteAll()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TrashEffect.LaunchSystemDeleteDialog -> {
                    deleteAllLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
                }
                is TrashEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is TrashEffect.RestoreSuccess -> {
                    selectedItemIndex = -1
                    snackbarHostState.showSnackbar("Item restored")
                }
                is TrashEffect.RestoreAllSuccess -> {
                    snackbarHostState.showSnackbar("All items restored")
                }
            }
        }
    }

    // If the list shrank (e.g. from a delete-forever in the viewer) and the selected
    // index is now out of range, clear it so we don't pass a bad index.
    if (selectedItemIndex >= 0 && selectedItemIndex >= items.size) {
        selectedItemIndex = -1
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                RecentlyDeletedTopBar(
                    onBackClick = onBackClick,
                    showActions = items.isNotEmpty(),
                    onRestoreAllClick = { showRestoreAllDialog = true },
                    onDeleteAllClick = { showDeleteAllDialog = true }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (items.isEmpty()) {
                    RecentlyDeletedEmptyState()
                } else {
                    RecentlyDeletedGrid(
                        items = items,
                        onItemClick = { index -> selectedItemIndex = index }
                    )
                }
            }
        }

        if (selectedItemIndex >= 0) {
            PhotoViewerScreen(
                itemIds = items.map { it.id },
                initialItemId = items[selectedItemIndex].id,
                resolveViewerItem = { id -> items.find { it.id == id }?.toViewerItem() },
                onBackClick = { selectedItemIndex = -1 },
                mode = ViewerMode.TRASH,
                onRestore = { viewerItem ->
                    items.find { it.id == viewerItem.id }?.let { trashItem ->
                        viewModel.restoreItem(trashItem, onRestored = onItemRestored)
                    }
                    selectedItemIndex = -1
                },
                onDeleteForever = { viewerItem ->
                    viewModel.deleteForever(viewerItem.id)
                    selectedItemIndex = -1
                }
            )
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all?") },
            text = {
                Text(
                    "All items in Recently Deleted will be permanently removed.\n" +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAll()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            title = { Text("Restore all?") },
            text = {
                Text(
                    "All items in Recently Deleted will be moved back to the gallery."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreAllDialog = false
                        viewModel.restoreAll(onRestored = onItemRestored)
                    }
                ) {
                    Text("Restore All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatRelativeDeletedDate(deletedAtMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - deletedAtMs
    val hours = diff / (1000L * 60L * 60L)
    val days = hours / 24L
    return when {
        days < 1L -> "Deleted today"
        days < 2L -> "Deleted yesterday"
        days < 30L -> "Deleted $days days ago"
        else -> {
            val instant = java.time.Instant.ofEpochMilli(deletedAtMs)
            val localDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault())
            "Deleted " + formatter.format(localDate)
        }
    }
}

@Composable
private fun RecentlyDeletedTopBar(
    onBackClick: () -> Unit,
    showActions: Boolean,
    onRestoreAllClick: () -> Unit,
    onDeleteAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Recently Deleted",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.weight(1f))

        if (showActions) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRestoreAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Restore All",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onDeleteAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Delete All",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(Modifier.width(96.dp))
        }
    }
}

@Composable
private fun RecentlyDeletedEmptyState() {
    GeminiEmptyState(
        icon = Icons.Rounded.Delete,
        title = "No recently deleted items",
        subtitle = "Deleted photos and videos will appear here."
    )
}

@Composable
private fun RecentlyDeletedGrid(
    items: List<TrashItem>,
    onItemClick: (Int) -> Unit
) {
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val handleItemClick = remember { { index: Int -> currentOnItemClick(index) } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            RecentlyDeletedCard(
                item = item,
                index = index,
                modifier = Modifier.animateItem(),
                onClick = handleItemClick
            )
        }
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun RecentlyDeletedCard(
    item: TrashItem,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    val imageSource = remember(item) {
        item.localFilePath?.let { File(it) }
            ?: item.localThumbnailPath?.let { File(it) }
            ?: item.uri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }
    val context = LocalContext.current
    val imageRequest = remember(context, imageSource) {
        imageSource?.let { source ->
            ImageRequest.Builder(context)
                .data(source)
                .size(256)
                .crossfade(true)
                .build()
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick(index) }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imageRequest != null) {
                    // AsyncImage is lighter than SubcomposeAsyncImage:
                    // it doesn't launch a sub-composition for loading/error states.
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ThumbnailPlaceholder()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            )
                        )
                )

                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .height(24.dp)
                            .widthIn(min = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }

            // Cache formatted date string so it isn't recomputed on every recomposition.
            val deletedDateLabel = remember(item.deletedAt) {
                formatRelativeDeletedDate(item.deletedAt)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = deletedDateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}