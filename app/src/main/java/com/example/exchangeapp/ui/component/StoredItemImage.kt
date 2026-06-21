package com.example.exchangeapp.ui.component

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun StoredItemImage(
    image: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (image.isNullOrBlank()) return

    if (image.isRemoteOrUriImage()) {
        AsyncImage(
            model = image,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }

    val bitmap = remember(image) { image.decodeBase64ImageBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = image,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun String.isRemoteOrUriImage(): Boolean {
    val value = trim()
    return value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("content://", ignoreCase = true) ||
        value.startsWith("file://", ignoreCase = true)
}

private fun String.decodeBase64ImageBitmap(): ImageBitmap? {
    return try {
        val payload = substringAfter("base64,", this).trim()
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
