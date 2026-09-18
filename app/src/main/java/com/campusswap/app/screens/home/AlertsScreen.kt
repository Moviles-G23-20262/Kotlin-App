package com.campusswap.app.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BackHeader
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.CampusSlider
import com.campusswap.app.components.CampusTextField
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.MonoText
import com.campusswap.app.components.Pill
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.SectionTitle
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.formatPrice
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.AlertMatch
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.SmartAlert
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

private const val ALERT_PRICE_MAX = 350000f

/**
 * View 11 — Alerts (saved searches and their matches).
 * Saved searches with their trigger thresholds and the listings that already match them,
 * with one-tap reservation.
 */
@Composable
fun AlertsScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    var showNewAlert by remember { mutableStateOf(false) }
    val matches = vm.alertMatches

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "Alerts", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionTitle("New matches", icon = CampusIcons.Sparkle)
            }

            if (matches.isEmpty()) {
                item {
                    BodyText(
                        "No listings match your alerts right now. We'll notify you the moment one is posted.",
                        color = c.textMuted,
                    )
                }
            } else {
                items(matches, key = { "match-${it.product.id}" }) { match ->
                    MatchCard(
                        match = match,
                        onOpen = { onProductClick(match.product.id) },
                        onReserve = { vm.reserveMatch(match.product.id) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("Your alerts", modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = if (showNewAlert) "Cancel" else "New alert",
                        onClick = { showNewAlert = !showNewAlert },
                        leadingIcon = if (showNewAlert) null else CampusIcons.Plus,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = CampusType.sizeXs,
                    )
                }
            }

            item {
                AnimatedVisibility(visible = showNewAlert) {
                    NewAlertForm(
                        onCancel = { showNewAlert = false },
                        onCreate = { alert ->
                            vm.addAlert(alert)
                            showNewAlert = false
                        },
                    )
                }
            }

            items(vm.alerts, key = { "alert-${it.id}" }) { alert ->
                AlertCard(
                    alert = alert,
                    matchCount = matches.count { it.alert.id == alert.id },
                    onToggle = { vm.toggleAlert(alert.id) },
                    onRemove = { vm.removeAlert(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun MatchCard(match: AlertMatch, onOpen: () -> Unit, onReserve: () -> Unit) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard(borderColor = c.accent.copy(alpha = 0.45f))
            .clickable(onClick = onOpen)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductPlaceholderImage(
                category = match.product.category,
                seed = match.product.imageSeed,
                modifier = Modifier.size(68.dp),
                cornerRadius = 10,
            )
            Column(modifier = Modifier.weight(1f)) {
                MonoText(
                    "Posted ${match.postedMinutesAgo} min ago",
                    color = c.accentHi,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                BodyText(
                    match.product.title,
                    color = c.text,
                    weight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(match.product.condition.label)
                    match.product.course?.let { Badge(it.code, highlighted = true) }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PriceText(match.product.price)
                BodyText(
                    if (match.savingPercent > 0) {
                        "${match.savingPercent}% below your ${formatPrice(match.alert.maxPrice)} limit"
                    } else {
                        "Within your limit"
                    },
                    size = CampusType.size2xs,
                    color = c.textMuted,
                )
            }
            if (match.reserved) {
                Badge("Reserved for you", highlighted = true)
            } else {
                PrimaryButton(
                    text = "Reserve",
                    onClick = onReserve,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = CampusType.sizeXs,
                )
            }
        }
    }
}

@Composable
private fun AlertCard(alert: SmartAlert, matchCount: Int, onToggle: () -> Unit, onRemove: () -> Unit) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard()
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BodyText(
                    alert.keyword.ifBlank { alert.course?.name ?: alert.category.label },
                    color = c.text,
                    weight = FontWeight.SemiBold,
                    size = CampusType.sizeSm,
                )
                MonoText(
                    "Alert me under ${formatPrice(alert.maxPrice)}",
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = alert.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.accentText,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.textMuted,
                    uncheckedTrackColor = c.elevated,
                    uncheckedBorderColor = c.borderSubtle,
                ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            alert.course?.let { Badge(it.code, highlighted = true) }
            Badge("${alert.minCondition.label} or better")
            Badge(if (alert.instant) "Instant" else "Daily digest")
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(CampusIcons.Clock, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(13.dp))
            BodyText(
                if (alert.quietDuringClasses) " Muted during your classes" else " Notifies any time",
                size = CampusType.size2xs,
                color = c.textMuted,
                modifier = Modifier.weight(1f),
            )
            BodyText(
                "$matchCount ${if (matchCount == 1) "match" else "matches"}",
                size = CampusType.size2xs,
                color = c.accentHi,
                modifier = Modifier.padding(end = 12.dp),
            )
            BodyText(
                "Delete",
                size = CampusType.size2xs,
                color = c.error,
                modifier = Modifier.plainClickable(onRemove),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewAlertForm(onCancel: () -> Unit, onCreate: (SmartAlert) -> Unit) {
    val c = CampusSwapTheme.colors
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.ALL) }
    var condition by remember { mutableStateOf(Condition.GOOD) }
    var maxPrice by remember { mutableFloatStateOf(120000f) }
    var instant by remember { mutableStateOf(true) }
    var quiet by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard(borderColor = c.accentLo)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeadingText("New alert", size = CampusType.sizeSm)
        CampusTextField(
            value = keyword,
            onValueChange = { keyword = it },
            placeholder = "Item or course code",
            leadingIcon = CampusIcons.Search,
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            BodyText("Category", weight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach {
                    Pill(it.label, selected = category == it, onClick = { category = it }, small = true)
                }
            }
        }
        Column {
            BodyText("Minimum condition", weight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Condition.entries.forEach {
                    Pill(it.label, selected = condition == it, onClick = { condition = it }, small = true)
                }
            }
        }
        Column {
            BodyText(
                "Alert me under ${formatPrice(maxPrice.toDouble())}",
                weight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            CampusSlider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                valueRange = 10000f..ALERT_PRICE_MAX,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column {
            BodyText("How often", weight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Instant", selected = instant, onClick = { instant = true }, small = true)
                Pill("Daily digest", selected = !instant, onClick = { instant = false }, small = true)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BodyText("Mute during classes", color = c.text, weight = FontWeight.Medium)
                BodyText("Uses your timetable so alerts arrive on a break.", size = CampusType.size2xs, color = c.textMuted)
            }
            Switch(
                checked = quiet,
                onCheckedChange = { quiet = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.accentText,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.textMuted,
                    uncheckedTrackColor = c.elevated,
                    uncheckedBorderColor = c.borderSubtle,
                ),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Create alert",
                onClick = {
                    onCreate(
                        SmartAlert(
                            id = "a-${System.currentTimeMillis()}",
                            keyword = keyword.trim(),
                            course = null,
                            category = category,
                            maxPrice = maxPrice.toDouble(),
                            minCondition = condition,
                            instant = instant,
                            quietDuringClasses = quiet,
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 11.dp),
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .border(1.dp, c.borderSubtle, RoundedCornerShape(10.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BodyText("Cancel", color = c.text2)
            }
        }
    }
}
