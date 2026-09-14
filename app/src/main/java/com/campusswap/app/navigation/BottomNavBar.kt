package com.campusswap.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.campusswap.app.ui.theme.AccentBlue

@Composable
fun CampusSwapBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            if (item.isCentral) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .clickable { onNavigate(item.route) },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(AccentBlue, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.filledIcon,
                                contentDescription = item.label,
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                        )
                    }
                }
            } else {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        indicatorColor = AccentBlue.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}
