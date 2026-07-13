package com.emon.proxagallery.ui

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Describes a single action row inside the "More" bottom sheet.
 *
 * Adding new viewer actions (Rename, Move, Hide, Set as Wallpaper, etc.) in the future
 * requires no changes to [MoreBottomSheet] — just append another [ViewerMoreAction]
 * to the list passed by the caller.
 */
data class ViewerMoreAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit
)
