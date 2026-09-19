package com.campusswap.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusswap.app.components.plainClickable
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.OutfitFamily


@Composable
fun CampusSwapBottomBar(
    currentRoute: String?,
    cartCount: Int,
    onNavigate: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind { drawLine(c.border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomNavItems.forEach { item ->
            if (item.isCentral) {
                val shape = RoundedCornerShape(12.dp)
                Row(
                    modifier = Modifier
                        .offset(y = (-7).dp)
                        .shadow(8.dp, shape, ambientColor = c.shadowAccent, spotColor = c.shadowAccent)
                        .clip(shape)
                        .background(c.accent)
                        .border(1.dp, c.accentLo, shape)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(item.icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
                    Text(
                        item.label,
                        style = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.accentText),
                    )
                }
            } else {
                val selected = currentRoute == item.route || (currentRoute != null && currentRoute in item.alsoActiveOn)
                val color = if (selected) c.accentHi else c.textMuted
                Box {
                    Column(
                        modifier = Modifier
                            .plainClickable { onNavigate(item.route) }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = color, modifier = Modifier.size(22.dp))
                        Text(item.label, style = TextStyle(fontFamily = OutfitFamily, fontSize = 10.sp, color = color))
                    }
                    if (item.route == Routes.CART && cartCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(c.accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                cartCount.coerceAtMost(99).toString(),
                                style = TextStyle(fontFamily = OutfitFamily, fontSize = 10.sp, color = c.accentText),
                            )
                        }
                    }
                }
            }
        }
    }
}
