package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.data.model.Song
import com.example.data.util.AlbumArtUtil
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.VibrantYellow
import com.example.ui.theme.VibrantYellowDark

private val PlaceholderGradient = Brush.linearGradient(
    colors = listOf(VibrantYellow, VibrantYellowDark)
)

@Composable
fun AlbumArtContainer(
    modifier: Modifier = Modifier,
    albumArtUri: Uri? = null,
    song: Song? = null,
    size: Dp? = null,
    cornerRadius: Dp = 12.dp,
    contentDescription: String = "Album Artwork"
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // High-resolution artwork bytes from embedded ID3/FLAC master tags or folder art
    var highResArtwork by remember(song?.id, song?.path, song?.customArtworkUri, albumArtUri) {
        mutableStateOf<Any?>(null)
    }

    LaunchedEffect(song?.id, song?.path, song?.uri, song?.customArtworkUri, albumArtUri) {
        if (!song?.customArtworkUri.isNullOrEmpty()) {
            highResArtwork = Uri.parse(song!!.customArtworkUri)
            return@LaunchedEffect
        }
        if (song != null) {
            val bytes = AlbumArtUtil.getArtworkBytes(
                context = context,
                songPath = song.path,
                songUri = song.uri,
                albumId = song.albumId
            )
            if (bytes != null) {
                highResArtwork = bytes
                return@LaunchedEffect
            }
        }
        if (albumArtUri != null) {
            val uriStr = albumArtUri.toString()
            if (uriStr.contains("albumart/")) {
                val albumId = albumArtUri.lastPathSegment?.toLongOrNull() ?: 0L
                if (albumId > 0) {
                    val bytes = AlbumArtUtil.getArtworkBytes(
                        context = context,
                        songPath = "",
                        songUri = "",
                        albumId = albumId
                    )
                    if (bytes != null) {
                        highResArtwork = bytes
                        return@LaunchedEffect
                    }
                }
            }
        }
    }

    // Prioritize high-resolution source bytes over low-res legacy MediaStore thumbnail
    val effectiveModel = highResArtwork
        ?: (if (!song?.customArtworkUri.isNullOrEmpty()) Uri.parse(song!!.customArtworkUri) else null)
        ?: albumArtUri
        ?: song?.albumArtUri

    val boxModifier = modifier
        .then(if (size != null) Modifier.size(size) else Modifier)
        .clip(shape)
        .background(PlaceholderGradient)
        .testTag("album_art_container")

    val imageRequest = remember(effectiveModel, size) {
        if (effectiveModel != null) {
            ImageRequest.Builder(context)
                .data(effectiveModel)
                .crossfade(true)
                .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                .allowRgb565(false)
                .allowHardware(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .apply {
                    if (size != null) {
                        val px = with(density) { size.roundToPx() }
                        if (px > 0) {
                            size(px, px)
                            precision(Precision.EXACT)
                            scale(Scale.FILL)
                        }
                    } else {
                        // High-res hero art for Now Playing: render at full master quality
                        precision(Precision.AUTOMATIC)
                        scale(Scale.FIT)
                    }
                }
                .build()
        } else null
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        // Stylized vinyl record / music note icon baseline adhering to the yellow placeholder requirement
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = contentDescription,
            tint = DeepPurple.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxSize(0.52f)
        )

        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
