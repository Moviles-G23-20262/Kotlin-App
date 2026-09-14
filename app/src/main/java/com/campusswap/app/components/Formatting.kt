package com.campusswap.app.components

import java.text.NumberFormat
import java.util.Locale

private val priceFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))

fun formatPrice(amount: Double): String = "$${priceFormatter.format(amount)} COP"
