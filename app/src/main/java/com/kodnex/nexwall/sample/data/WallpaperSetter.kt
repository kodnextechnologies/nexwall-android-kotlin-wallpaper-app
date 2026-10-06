package com.kodnex.nexwall.sample.data

import android.app.WallpaperManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

enum class WallpaperTarget(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/**
 * Downloads the full-size image and hands the stream straight to
 * [WallpaperManager], so the bitmap is never decoded in app memory.
 * `image_url` is loaded directly, like the thumbnails shown by Coil.
 */
class WallpaperSetter(context: Context) {
    private val wallpaperManager = WallpaperManager.getInstance(context.applicationContext)
    private val client = OkHttpClient()

    suspend fun set(imageUrl: String, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
            throw IOException("Setting the wallpaper is not allowed on this device")
        }
        client.newCall(Request.Builder().url(imageUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
            response.body!!.byteStream().use { stream ->
                wallpaperManager.setStream(stream, null, true, target.flags)
            }
        }
    }
}
