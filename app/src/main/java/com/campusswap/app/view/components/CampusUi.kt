package com.campusswap.app.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType
import com.campusswap.app.ui.theme.frauncesFor
import com.campusswap.app.ui.theme.JetBrainsMonoFamily
import com.campusswap.app.ui.theme.OutfitFamily

/* ------------------------------------------------------------------------------------------
 * Building blocks that reproduce the CSS classes of the Figma prototype:
 * .btn-primary, .btn-secondary, .pill, .badge, .input-field, .sticky-note, .hero-card,
 * .product-card, the `card` / `iconBtn` / `header` inline styles and the text helpers.
 * ------------------------------------------------------------------------------------------ */

/** Clickable without the Material ripple (the prototype uses subtle press states instead). */
fun Modifier.plainClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
)

/** `card` style: surface background, 1px border, 16px radius, soft shadow. */
@Composable
fun Modifier.campusCard(radius: Dp = 16.dp, borderColor: Color? = null): Modifier {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(elevation = 4.dp, shape = shape, ambientColor = c.shadowCard, spotColor = c.shadowCard)
        .background(c.surface, shape)
        .border(1.dp, borderColor ?: c.border, shape)
        .clip(shape)
}

// ---------- Text helpers ----------

@Composable
fun HeadingText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = CampusType.sizeMd,
    color: Color = CampusSwapTheme.colors.text,
    weight: FontWeight = FontWeight.SemiBold,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = frauncesFor(size.value),
            fontWeight = weight,
            fontSize = size,
            color = color,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = CampusType.sizeXs,
    color: Color = CampusSwapTheme.colors.text2,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
    minLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        minLines = minLines,
        style = TextStyle(
            fontFamily = OutfitFamily,
            fontWeight = weight,
            fontSize = size,
            color = color,
            lineHeight = lineHeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = CampusType.size2xs,
    color: Color = CampusSwapTheme.colors.textMuted,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(fontFamily = JetBrainsMonoFamily, fontWeight = weight, fontSize = size, color = color),
    )
}

/** Price in Fraunces + accent-hi, as in every product card of the prototype. */
@Composable
fun PriceText(amount: Double, modifier: Modifier = Modifier, size: TextUnit = CampusType.sizeSm, weight: FontWeight = FontWeight.SemiBold) {
    HeadingText(
        text = formatPrice(amount),
        modifier = modifier,
        size = size,
        color = CampusSwapTheme.colors.accentHi,
        weight = weight,
        maxLines = 1,
    )
}

/** `.section-title` with an optional leading icon in accent-hi. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color? = null) {
    val c = CampusSwapTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint ?: c.accentHi, modifier = Modifier.size(16.dp))
        }
        Text(text = title, style = CampusType.sectionTitle.copy(color = c.text))
    }
}

/** "See all →" link used next to section titles. */
@Composable
fun SeeAllLink(onClick: () -> Unit) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BodyText("See all", color = c.textMuted)
        Icon(CampusIcons.ArrowRight, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(15.dp))
    }
}

// ---------- Buttons ----------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
    fontSize: TextUnit = CampusType.sizeSm,
) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = c.shadowAccent, spotColor = c.shadowAccent)
            .clip(shape)
            .background(if (enabled) c.accent else c.accent.copy(alpha = 0.6f))
            .border(1.dp, c.accentLo, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonContent(text, leadingIcon, trailingIcon, c.accentText, fontSize)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
    fontSize: TextUnit = CampusType.sizeSm,
) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .shadow(3.dp, shape, ambientColor = c.shadowCard, spotColor = c.shadowCard)
            .clip(shape)
            .background(c.elevated)
            .border(1.dp, c.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonContent(text, leadingIcon, trailingIcon, c.text2, fontSize)
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    color: Color,
    fontSize: TextUnit,
) {
    val iconSize = if (fontSize.value < 14f) 14.dp else 16.dp
    if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    Text(
        text = text,
        style = TextStyle(
            fontFamily = OutfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = 0.01.em,
            color = color,
        ),
        maxLines = 1,
    )
    if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
}

/** 36x36 `iconBtn`: elevated square with subtle border. */
@Composable
fun CampusIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconTint: Color? = null,
    iconSize: Dp = 20.dp,
) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(shape)
            .background(if (active) c.accent else c.elevated)
            .border(1.dp, if (active) c.accentLo else c.borderSubtle, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = iconTint ?: if (active) c.accentText else c.text2,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ---------- Pills & badges ----------

/** `.pill` (single-select chip). [small] matches the compact pills in the filter panel. */
@Composable
fun Pill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val c = CampusSwapTheme.colors
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) c.accent else c.elevated)
            .border(1.dp, if (selected) c.accentLo else c.borderSubtle, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 3.dp else 4.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Medium,
                fontSize = if (small) CampusType.size2xs else CampusType.sizeXs,
                color = if (selected) c.accentText else c.text2,
            ),
            maxLines = 1,
        )
    }
}

/** `.badge` / `.badge.purple` — small uppercase tag. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (highlighted) c.tagHiBg else c.tagBg)
            .border(1.dp, if (highlighted) c.accentLo else c.borderSubtle, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = OutfitFamily,
                fontWeight = FontWeight.Medium,
                fontSize = CampusType.size2xs,
                letterSpacing = 0.03.em,
                color = if (highlighted) c.tagHiText else c.tagText,
            ),
            maxLines = 1,
        )
    }
}

/** Star + mono rating label, e.g. "★ 4.7 (12)". */
@Composable
fun RatingLabel(text: String, modifier: Modifier = Modifier, size: TextUnit = CampusType.size2xs) {
    val c = CampusSwapTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(CampusIcons.Star, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(13.dp))
        MonoText(text, size = size)
    }
}

/** Round avatar with initials in Fraunces. */
@Composable
fun InitialsAvatar(name: String, size: Dp, modifier: Modifier = Modifier, background: Color? = null, fontSize: TextUnit = 13.sp) {
    val c = CampusSwapTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: c.elevated)
            .border(1.dp, c.borderSubtle, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        HeadingText(initialsOf(name), size = fontSize, color = c.accentHi, weight = FontWeight.Bold)
    }
}

fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

// ---------- Inputs ----------

/** `.input-field`: 10px radius, subtle border that turns accent with a glow when focused. */
@Composable
fun CampusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    minLines: Int = 1,
) {
    val c = CampusSwapTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (focused) Modifier.border(3.dp, c.shadowAccent, RoundedCornerShape(12.dp)).padding(2.dp) else Modifier.padding(2.dp))
            .onFocusChanged { focused = it.isFocused },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(c.accent),
        textStyle = TextStyle(fontFamily = OutfitFamily, fontSize = CampusType.sizeSm, color = c.text),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(c.surface)
                    .border(1.dp, if (focused) c.accent else c.borderSubtle, shape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
                    Box(Modifier.width(8.dp))
                } else {
                    Box(Modifier.width(4.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = TextStyle(fontFamily = OutfitFamily, fontSize = CampusType.sizeSm, color = c.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
                if (trailingContent != null) trailingContent()
            }
        },
    )
}

/** Read-only search bar that behaves like a button (Home header). */
@Composable
fun FakeSearchField(placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .padding(2.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(CampusIcons.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        Text(placeholder, style = TextStyle(fontFamily = OutfitFamily, fontSize = CampusType.sizeSm, color = c.textMuted), maxLines = 1)
    }
}

/** Form label (`font-delius`: Outfit 500, xs, tx-2). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    BodyText(text, modifier = modifier, color = CampusSwapTheme.colors.text2, weight = FontWeight.Medium)
}

// ---------- Surfaces ----------

/** Screen header: surface background with a bottom border (`header` style). */
@Composable
fun CampusHeader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind {
                drawLine(c.border, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1.dp.toPx())
            }
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
    ) {
        content()
    }
}

/** Header row with back button + title (+ optional trailing text / actions). */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CampusHeader {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampusIconButton(CampusIcons.Back, contentDescription = "Back", onClick = onBack)
            HeadingText(title, size = CampusType.sizeMd, maxLines = 1)
            if (subtitle != null) BodyText(subtitle, color = CampusSwapTheme.colors.textMuted)
            Box(Modifier.weight(1f))
            actions()
        }
    }
}

/** Bottom action bar (surface + top border), e.g. "Proceed to Checkout". */
@Composable
fun BottomActionBar(content: @Composable () -> Unit) {
    val c = CampusSwapTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind { drawLine(c.border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
    ) {
        content()
    }
}

/** `.hero-card`: diagonal gradient from hero-from to hero-to. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(10.dp, shape, ambientColor = c.shadowAccent, spotColor = c.shadowAccent)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.heroFrom, c.heroTo), start = Offset.Zero, end = Offset.Infinite))
            .border(1.dp, c.accentLo, shape)
            .padding(20.dp),
    ) {
        content()
    }
}

/** `.sticky-note` with the little tape on top; [wiggle] reproduces `animate-wiggle`. */
@Composable
fun StickyNote(modifier: Modifier = Modifier, wiggle: Boolean = false, content: @Composable () -> Unit) {
    val c = CampusSwapTheme.colors
    val rotation = if (wiggle) {
        val transition = rememberInfiniteTransition(label = "wiggle")
        val angle by transition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wiggleAngle",
        )
        angle
    } else {
        0f
    }
    val shape = RoundedCornerShape(10.dp)
    Box(modifier = modifier.rotate(rotation).padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, ambientColor = c.shadowCard, spotColor = c.shadowCard)
                .background(c.elevated, shape)
                .border(1.dp, c.borderSubtle, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .size(width = 28.dp, height = 8.dp)
                .background(c.borderSubtle, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
        )
    }
}

/** Radio-like option row used on Checkout. */
@Composable
fun RadioOption(label: String, selected: Boolean, onClick: () -> Unit, sub: String? = null) {
    val c = CampusSwapTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.elevated else Color.Transparent)
            .border(1.dp, if (selected) c.borderSubtle else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
                .border(BorderStroke(if (selected) 4.dp else 1.dp, if (selected) c.accent else c.textMuted), CircleShape),
        )
        Column {
            BodyText(label, color = c.text, weight = FontWeight.SemiBold)
            if (sub != null) BodyText(sub, size = CampusType.size2xs, color = c.textMuted)
        }
    }
}

/** Thin rounded progress-like dot used by the image carousel. */
fun Modifier.pagerDot(active: Boolean, activeColor: Color): Modifier =
    this
        .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
        .drawBehind {
            drawRoundRect(
                color = if (active) activeColor else Color.White.copy(alpha = 0.4f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }

/** Thin range input like the prototype's `<input type="range">` (accent track + round thumb). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val c = CampusSwapTheme.colors
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.height(24.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .shadow(2.dp, CircleShape)
                    .background(c.accent, CircleShape),
            )
        },
        track = {
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(color = c.borderSubtle, cornerRadius = radius)
                drawRoundRect(color = c.accent, size = Size(size.width * fraction, size.height), cornerRadius = radius)
            }
        },
    )
}
