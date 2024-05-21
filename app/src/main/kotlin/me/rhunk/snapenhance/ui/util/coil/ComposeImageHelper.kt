package me.rhunk.snapenhance.ui.util.coil

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.RemoteSideContext
import me.rhunk.snapenhance.common.data.download.MediaEncryptionKeyPair

@Composable
fun BitmojiImage(context: RemoteSideContext, modifier: Modifier = Modifier, size: Int = 48, url: String?) {
    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequestHelper.newBitmojiImageRequest(
                context.androidContext,
                url
            ),
            imageLoader = context.imageLoader
        ),
        contentDescription = null,
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .requiredWidthIn(min = 0.dp, max = size.dp)
            .height(size.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(modifier)
    )
}

fun ImageRequest.Builder.cacheKey(key: String?) = apply {
    memoryCacheKey(key)
    diskCacheKey(key)
}

object ImageRequestHelper {
    fun newBitmojiImageRequest(context: Context, url: String?) = ImageRequest.Builder(context)
        .data(url)
        .fallback(R.drawable.bitmoji_blank)
        .precision(Precision.INEXACT)
        .crossfade(true)
        .cacheKey(url)
        .build()

    fun newPreviewImageRequest(context: Context, url: String, mediaEncryptionKeyPair: MediaEncryptionKeyPair? = null) = ImageRequest.Builder(context)
        .cacheKey(url)
        .precision(Precision.INEXACT)
        .crossfade(true)
        .placeholder(ColorDrawable(0x1EFFFFFF))
        .crossfade(200)
        .data(url)
        .decoderFactory { result, _, _ ->
            CoilPreviewDecoder(
                context.resources,
                result,
                mediaEncryptionKeyPair,
                mergeOverlay = true
            )
        }
        .build()
}