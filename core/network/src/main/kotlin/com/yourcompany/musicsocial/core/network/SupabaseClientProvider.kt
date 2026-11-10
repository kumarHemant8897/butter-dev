package com.yourcompany.musicsocial.core.network

import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import com.yourcompany.musicsocial.core.domain.system.BackendAvailability
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the single [SupabaseClient] instance.
 *
 * The client is created lazily on first use, so app start-up does not pay for it, and it is
 * never created when credentials are missing. Data sources call [clientOrNull] and turn `null`
 * into AppError.BackendNotConfigured instead of pretending to be connected.
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    private val config: BackendConfig,
    private val okHttpClient: OkHttpClient,
    private val logger: AppLogger,
) : BackendAvailability {

    override val isConfigured: Boolean get() = config.isConfigured

    private val client: SupabaseClient? by lazy {
        if (!config.isConfigured) {
            logger.warn(LogCategory.Network, "Supabase is not configured; remote features are disabled. $config")
            return@lazy null
        }
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey,
        ) {
            httpEngine = OkHttp.create { preconfigured = supabaseHttpClient() }
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    fun clientOrNull(): SupabaseClient? = client

    /**
     * Logs request lines and status codes only (Level.BASIC): never headers or bodies, which carry
     * the API key, access tokens and user content. Messages still pass through LogRedactor.
     */
    private fun supabaseHttpClient(): OkHttpClient {
        if (!config.enableHttpLogging) return okHttpClient
        val logging = HttpLoggingInterceptor { message -> logger.debug(LogCategory.Network, message) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }
        return okHttpClient.newBuilder().addInterceptor(logging).build()
    }
}
