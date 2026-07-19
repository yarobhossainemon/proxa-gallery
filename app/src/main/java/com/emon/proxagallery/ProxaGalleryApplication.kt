package com.emon.proxagallery

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.video.VideoFrameDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class ProxaGalleryApplication : Application(), SingletonImageLoader.Factory {

    val appContainer: AppContainer by lazy { AppContainer(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        mediaObserverJob = appScope.launch {
            appContainer.mediaChangeMonitor.changes.collect {
                appContainer.aiIndexScheduler.scheduleChanges()
            }
        }
    }

    override fun onTerminate() {
        mediaObserverJob?.cancel()
        super.onTerminate()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    // 20% of RAM. Leaves headroom for decode scratch buffers and
                    // Compose render nodes; 25% was borderline on low-RAM devices.
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // Fixed 256 MB cap — reliable across all device storage sizes.
                    // 2% of disk on a 32 GB device = 640 MB, which is excessive.
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            // Do NOT set crossfade(true) globally — it forces an alpha-composite
            // animation even on instant memory-cache hits. Each site that needs
            // crossfade opts in explicitly (PhotoViewerScreen pager does).
            .build()
    }
}
