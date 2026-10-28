package com.campusswap.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campusswap.app.CampusSwapApplication

@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: (@Composable () -> Unit)? = null,
) {
    val imageCache = (LocalContext.current.applicationContext as CampusSwapApplication).container.imageCache
    var image by remember(url) { mutableStateOf(imageCache.cached(url)) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (image == null) {
            image = imageCache.load(url)
            failed = image == null
        }
    }

    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
        val loaded = image
        when {
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            failed && fallback != null -> fallback()
            else -> CircularProgressIndicator(strokeWidth = 2.dp)
        }
    }
}
