package com.campusswap.app.view.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campusswap.app.model.Product

/** Foto real del material (imageUrls del backend), si no tiene o no carga */
@Composable
fun ProductPhoto(
    product: Product,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    index: Int = 0,
) {
    val url = product.imageUrls.getOrNull(index)
    if (url == null) {
        ProductPlaceholderImage(product.category, product.imageSeed + index, modifier, cornerRadius)
    } else {
        RemoteImage(
            url = url,
            contentDescription = product.title,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp)),
            fallback = { ProductPlaceholderImage(product.category, product.imageSeed + index, Modifier.fillMaxSize(), cornerRadius = 0) },
        )
    }
}