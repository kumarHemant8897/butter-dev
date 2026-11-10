package com.yourcompany.musicsocial

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yourcompany.musicsocial.core.common.logging.CrashReporter
import com.yourcompany.musicsocial.core.common.logging.CrashReportingTree
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MusicSocialApplication : Application(), SingletonImageLoader.Factory {

    /** Lazy so the HTTP stack is only built when the first image or request needs it. */
    @Inject lateinit var okHttpClient: Lazy<OkHttpClient>

    @Inject lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree(crashReporter))
    }

    /** Coil shares the app's OkHttp client (connection pool, timeouts) and caches to memory and disk. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .crossfade(true)
            .build()
}
