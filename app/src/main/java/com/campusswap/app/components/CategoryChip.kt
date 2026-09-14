package com.campusswap.app.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.SecondaryBlue

@Composable
fun CampusSwapChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { androidx.compose.material3.Text(label) },
        modifier = modifier,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SecondaryBlue.copy(alpha = 0.5f),
            selectedBorderColor = AccentBlue,
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentBlue,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
        ),
    )
}
