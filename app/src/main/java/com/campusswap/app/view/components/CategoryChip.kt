package com.campusswap.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CampusSwapChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Pill(label = label, selected = selected, onClick = onClick, modifier = modifier)
}
