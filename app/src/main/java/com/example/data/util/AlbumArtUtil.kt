package com.example.data.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AlbumArtUtil {
    private const val TAG = "AlbumArtUtil"
    private val memoryCache = LruCache<String, ByteArray>(250)

    suspend fun getArtworkBytes(context: Context, songPath: String, songUri: String, albumId: Long): ByteArray? =
        withContext(Dispatchers.IO) {
            val cacheKey = if (songPath.isNotEmpty()) songPath else songUri
            memoryCache.get(cacheKey)?.let { return@withContext it }

            // 1. Try MediaMetadataRetriever on local file or content Uri for embedded ID3/FLAC artwork
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                if (songPath.isNotEmpty() && File(songPath).exists()) {
                    retriever.setDataSource(songPath)
                } else {
                    retriever.setDataSource(context, Uri.parse(songUri))
                }
                val pic = retriever.embeddedPicture
                if (pic != null && pic.isNotEmpty()) {
                    memoryCache.put(cacheKey, pic)
                    return@withContext pic
                }
            } catch (e: Exception) {
                // Ignore and try MediaStore fallback
            } finally {
                try {
                    retriever?.release()
                } catch (ignored: Throwable) {}
            }

            // 2. On Android 10+ (API 29+), use ContentResolver.loadThumbnail for high-res 1024x1024 art
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && songUri.isNotEmpty()) {
                try {
                    val bmp = context.contentResolver.loadThumbnail(
                        Uri.parse(songUri),
                        android.util.Size(1024, 1024),
                        null
                    )
                    val bos = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.isNotEmpty()) {
                        memoryCache.put(cacheKey, bytes)
                        return@withContext bytes
                    }
                } catch (_: Throwable) {
                    // Fall through to MediaStore fallback
                }
            }

            // 3. Try legacy MediaStore audio album art Uri
            if (albumId > 0) {
                try {
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                    context.contentResolver.openInputStream(artUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        if (bytes.isNotEmpty()) {
                            memoryCache.put(cacheKey, bytes)
                            return@withContext bytes
                        }
                    }
                } catch (e: Exception) {
                    // Not available in MediaStore cache
                }
            }

            null
        }

    suspend fun getArtworkBitmap(context: Context, songPath: String, songUri: String, albumId: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            val bytes = getArtworkBytes(context, songPath, songUri, albumId)
            if (bytes != null) {
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed decoding artwork bitmap: ${e.message}")
                }
            }
            null
        }
}
