package com.campusswap.app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/** A text toolbar that never shows, so long-press offers no Copy, Cut or Paste. */
private object DisabledTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() = Unit
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}

/** Text fields inside [content] can't be copied from or pasted into through the selection menu. */
@Composable
fun NoClipboard(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextToolbar provides DisabledTextToolbar, content = content)
}

/**
 * Keyboard clipboard suggestions and hardware Ctrl+V bypass the toolbar and insert the whole
 * clip at once. For fields that are always typed one key at a time (passwords), reject any
 * change that adds more than one character.
 */
fun typedOnly(old: String, new: String): String = if (new.length - old.length > 1) old else new
