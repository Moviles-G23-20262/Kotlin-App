package com.campusswap.app.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Line icons used by the Figma prototype (Feather-style, 24x24 viewBox, round caps).
 * They are drawn as strokes in black; `Icon(tint = ...)` recolors them.
 */
object CampusIcons {

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},${cy} a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) = "M$x1,$y1 L$x2,$y2"

    private fun poly(points: String, close: Boolean = false): String {
        val nums = points.trim().split(Regex("[\\s,]+")).map { it.toFloat() }
        val sb = StringBuilder()
        for (i in nums.indices step 2) {
            sb.append(if (i == 0) "M" else " L").append(nums[i]).append(',').append(nums[i + 1])
        }
        if (close) sb.append(" Z")
        return sb.toString()
    }

    private fun icon(
        name: String,
        vararg paths: String,
        strokeWidth: Float = 2f,
        filled: Boolean = false,
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    val CampusSwap by lazy {
        icon(
            "CampusSwap",
            "M4 9 C4 5 20 5 20 9",
            poly("17 6 20 9 17 12"),
            "M20 15 C20 19 4 19 4 15",
            poly("7 12 4 15 7 18"),
        )
    }
    val Home by lazy { icon("Home", "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", poly("9 22 9 12 15 12 15 22")) }
    val Search by lazy { icon("Search", circle(11f, 11f, 8f), line(21f, 21f, 16.65f, 16.65f)) }
    val SearchLight by lazy { icon("SearchLight", circle(11f, 11f, 8f), line(21f, 21f, 16.65f, 16.65f), strokeWidth = 1.5f) }
    val Cart by lazy {
        icon("Cart", circle(9f, 21f, 1f), circle(20f, 21f, 1f), "M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6")
    }
    val CartLight by lazy {
        icon("CartLight", circle(9f, 21f, 1f), circle(20f, 21f, 1f), "M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6", strokeWidth = 1.5f)
    }
    val User by lazy { icon("User", "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2", circle(12f, 7f, 4f)) }
    private const val HEART = "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
    val Heart by lazy { icon("Heart", HEART) }
    val HeartFilled by lazy { icon("HeartFilled", HEART, filled = true) }
    private const val STAR = "12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
    val Star by lazy { icon("Star", poly(STAR, close = true), filled = true) }
    val StarOutline by lazy { icon("StarOutline", poly(STAR, close = true)) }
    val Plus by lazy { icon("Plus", line(12f, 5f, 12f, 19f), line(5f, 12f, 19f, 12f), strokeWidth = 2.5f) }
    val Minus by lazy { icon("Minus", line(5f, 12f, 19f, 12f), strokeWidth = 2.5f) }
    val Back by lazy { icon("Back", line(19f, 12f, 5f, 12f), poly("12 19 5 12 12 5"), strokeWidth = 2.5f) }
    val Check by lazy { icon("Check", poly("20 6 9 17 4 12"), strokeWidth = 2.5f) }
    val Bell by lazy { icon("Bell", "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9", "M13.73 21a2 2 0 0 1-3.46 0") }
    val Filter by lazy { icon("Filter", poly("22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3", close = true)) }
    val Location by lazy { icon("Location", "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z", circle(12f, 10f, 3f)) }
    val Eye by lazy { icon("Eye", "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z", circle(12f, 12f, 3f)) }
    val Message by lazy { icon("Message", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z") }
    val Flame by lazy {
        icon("Flame", "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z")
    }
    val BookOpen by lazy { icon("BookOpen", "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z", "M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z") }
    val Store by lazy {
        icon(
            "Store",
            "M3 9l1-5h16l1 5",
            "M3 9a2 2 0 0 0 2 2 2 2 0 0 0 2-2 2 2 0 0 0 2 2 2 2 0 0 0 2-2 2 2 0 0 0 2 2 2 2 0 0 0 2-2",
            "M5 21V11",
            "M19 21V11",
            "M10 14h4a1 1 0 0 1 1 1v6h-6v-6a1 1 0 0 1 1-1z",
        )
    }
    val Wallet by lazy { icon("Wallet", "M21 12V7H5a2 2 0 0 1 0-4h14v4", "M3 5v14a2 2 0 0 0 2 2h16v-5", "M18 12a2 2 0 0 0 0 4h4v-4z") }
    val Package by lazy {
        icon(
            "Package",
            line(16.5f, 9.4f, 7.5f, 4.21f),
            "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z",
            poly("3.27 6.96 12 12.01 20.73 6.96"),
            line(12f, 22.08f, 12f, 12f),
        )
    }
    val Tag by lazy { icon("Tag", "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z", line(7f, 7f, 7.01f, 7f)) }
    val Sparkle by lazy { icon("Sparkle", "M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17l-6.2 4.3 2.4-7.4L2 9.4h7.6z") }
    val Clock by lazy { icon("Clock", circle(12f, 12f, 10f), poly("12 6 12 12 16 14")) }
    val Inbox by lazy {
        icon("Inbox", poly("22 12 16 12 14 15 10 15 8 12 2 12"), "M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z")
    }
    val TrendingUp by lazy { icon("TrendingUp", poly("23 6 13.5 15.5 8.5 10.5 1 18"), poly("17 6 23 6 23 12")) }
    val ShoppingBag by lazy { icon("ShoppingBag", "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z", line(3f, 6f, 21f, 6f), "M16 10a4 4 0 0 1-8 0") }
    val Send by lazy { icon("Send", line(22f, 2f, 11f, 13f), poly("22 2 15 22 11 13 2 9 22 2", close = true)) }
    val ArrowRight by lazy { icon("ArrowRight", line(5f, 12f, 19f, 12f), poly("12 5 19 12 12 19"), strokeWidth = 2.5f) }
    val CheckCircle by lazy { icon("CheckCircle", "M22 11.08V12a10 10 0 1 1-5.93-9.14", poly("22 4 12 14.01 9 11.01")) }
    val MapPin by lazy { icon("MapPin", "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z", circle(12f, 10f, 3f)) }
    val Pencil by lazy { icon("Pencil", "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z") }
    val Close by lazy { icon("Close", line(18f, 6f, 6f, 18f), line(6f, 6f, 18f, 18f), strokeWidth = 2.5f) }
    val Sun by lazy {
        icon(
            "Sun",
            circle(12f, 12f, 5f),
            line(12f, 1f, 12f, 3f), line(12f, 21f, 12f, 23f),
            line(4.22f, 4.22f, 5.64f, 5.64f), line(18.36f, 18.36f, 19.78f, 19.78f),
            line(1f, 12f, 3f, 12f), line(21f, 12f, 23f, 12f),
            line(4.22f, 19.78f, 5.64f, 18.36f), line(18.36f, 5.64f, 19.78f, 4.22f),
        )
    }
    val Moon by lazy { icon("Moon", "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z") }
    val LogOut by lazy { icon("LogOut", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", poly("16 17 21 12 16 7"), line(21f, 12f, 9f, 12f)) }
}
