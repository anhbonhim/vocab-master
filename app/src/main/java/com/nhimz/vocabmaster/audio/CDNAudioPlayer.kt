package com.nhimz.vocabmaster.audio

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CDNAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    private val cacheSize: Long = 90 * 1024 * 1024 // 90MB cache for OGG files
    private var simpleCache: SimpleCache? = null
    private var exoPlayer: ExoPlayer? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            val cacheDir = File(context.cacheDir, "audio_cdn_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache!!)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheDataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            super.onPlayerError(error)
                            // Silent fallback: Log error but do not crash or show toast
                            LocalLogger.e("CDNAudioPlayer", "Audio playback failed. Silent fallback triggered. Cause: ${error.message}", error)
                        }
                    })
                }
        } catch (e: Exception) {
            LocalLogger.e("CDNAudioPlayer", "Failed to initialize ExoPlayer/Cache", e)
        }
    }

    fun isAudioCached(url: String): Boolean {
        val cache = simpleCache ?: return false
        val uri = android.net.Uri.parse(url)
        val cacheKey = androidx.media3.datasource.cache.CacheKeyFactory.DEFAULT.buildCacheKey(androidx.media3.datasource.DataSpec(uri))
        return cache.getCachedSpans(cacheKey).isNotEmpty()
    }

    fun playAudio(url: String?) {
        if (url.isNullOrBlank()) {
            LocalLogger.d("CDNAudioPlayer", "URL is null or blank. Silent fallback.")
            return
        }

        try {
            val isCached = isAudioCached(url)
            LocalLogger.i("CDNAudioPlayer", "Requesting play for URL. isCached=$isCached -> $url")
            
            exoPlayer?.let { player ->
                val mediaItem = MediaItem.fromUri(url)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        } catch (e: Exception) {
            LocalLogger.e("CDNAudioPlayer", "Failed to play audio from URL: $url. Silent fallback.", e)
        }
    }

    fun stop() {
        exoPlayer?.stop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        shutdown()
    }

    fun shutdown() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        try {
            simpleCache?.release()
        } catch (e: Exception) {
            LocalLogger.e("CDNAudioPlayer", "Error releasing SimpleCache", e)
        }
        simpleCache = null
        LocalLogger.d("CDNAudioPlayer", "CDNAudioPlayer shut down and released")
    }
}
