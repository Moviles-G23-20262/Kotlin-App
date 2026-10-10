package com.campusswap.app.components

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Collections

private val cache = Collections.synchronizedMap(mutableMapOf<String, ImageBitmap>())

@Composable
fun RemoteImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var image by remember(url) { mutableStateOf(cache[url]) }

    LaunchedEffect(url) {
        if (image == null) image = load(url)?.also { cache[url] = it }
    }

    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
        val loaded = image
        if (loaded == null) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private suspend fun load(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        URL(url).openStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()
}
