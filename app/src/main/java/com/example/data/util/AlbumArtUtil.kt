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
                var initialized = false
                if (songPath.isNotEmpty()) {
                    try {
                        retriever.setDataSource(songPath)
                        initialized = true
                    } catch (_: Throwable) {}
                }
                if (!initialized && songUri.isNotEmpty()) {
                    try {
                        retriever.setDataSource(context, Uri.parse(songUri))
                        initialized = true
                    } catch (_: Throwable) {}
                }
                if (initialized) {
                    val pic = retriever.embeddedPicture
                    if (pic != null && pic.isNotEmpty()) {
                        memoryCache.put(cacheKey, pic)
                        return@withContext pic
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever?.release()
                } catch (_: Throwable) {}
            }

            // 2. High-res folder master artwork (cover.jpg, folder.jpg, front.jpg, etc.) in the song's album directory
            if (songPath.isNotEmpty()) {
                try {
                    val songFile = File(songPath)
                    val parent = songFile.parentFile
                    if (parent != null && parent.isDirectory && parent.canRead()) {
                        val candidates = listOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "front.jpg", "front.png", "album.jpg", "album.png", "artwork.jpg")
                        for (candidate in candidates) {
                            val f = File(parent, candidate)
                            if (f.exists() && f.length() > 2048) {
                                val bytes = f.readBytes()
                                if (bytes.isNotEmpty()) {
                                    memoryCache.put(cacheKey, bytes)
                                    return@withContext bytes
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }

            // 3. On Android 10+ (API 29+), use ContentResolver.loadThumbnail for high-res 1440x1440 art
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && songUri.isNotEmpty()) {
                try {
                    val bmp = context.contentResolver.loadThumbnail(
                        Uri.parse(songUri),
                        android.util.Size(1440, 1440),
                        null
                    )
                    val bos = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 98, bos)
                    val bytes = bos.toByteArray()
                    if (bytes.isNotEmpty()) {
                        memoryCache.put(cacheKey, bytes)
                        return@withContext bytes
                    }
                } catch (_: Throwable) {}
            }

            // 4. Try MediaStore album art via Android 10+ Albums API or legacy albumart Uri
            if (albumId > 0) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        val albumUri = ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            albumId
                        )
                        val bmp = context.contentResolver.loadThumbnail(
                            albumUri,
                            android.util.Size(1440, 1440),
                            null
                        )
                        val bos = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 98, bos)
                        val bytes = bos.toByteArray()
                        if (bytes.isNotEmpty()) {
                            memoryCache.put(cacheKey, bytes)
                            return@withContext bytes
                        }
                    } catch (_: Throwable) {}
                }

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
                } catch (_: Exception) {}
            }

            null
        }

    suspend fun getArtworkBitmap(context: Context, songPath: String, songUri: String, albumId: Long): Bitmap? =
        withContext(Dispatchers.IO) {
            val bytes = getArtworkBytes(context, songPath, songUri, albumId)
            if (bytes != null) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inDither = true
                    }
                    return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed decoding artwork bitmap: ${e.message}")
                }
            }
            null
        }
}
