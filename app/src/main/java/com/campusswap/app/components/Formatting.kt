package com.campusswap.app.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

private val priceFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))

fun formatPrice(amount: Double): String = "$${priceFormatter.format(amount)} COP"

/**
 * Shows a digits-only price as it's typed with the Colombian thousands separator
 * ("1500000" reads "1.500.000") while the field keeps the raw digits.
 */
object PriceVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = buildString {
            digits.forEachIndexed { i, ch ->
                if (i > 0 && (digits.length - i) % 3 == 0) append('.')
                append(ch)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset + (1 until offset).count { (digits.length - it) % 3 == 0 }

            override fun transformedToOriginal(offset: Int): Int =
                grouped.take(offset).count { it != '.' }
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }
}
