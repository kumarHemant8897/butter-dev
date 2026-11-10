package com.yourcompany.musicsocial.core.network

/**
 * Backend settings injected at build time from local.properties or CI environment variables
 * (see local.properties.example). The app module provides this from BuildConfig.
 *
 * [supabaseAnonKey] is the publishable (anon) key. It is safe in a client app only because every
 * table is protected by Row Level Security. A service_role key must never be used here.
 */
class BackendConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val enableHttpLogging: Boolean,
) {
    val isConfigured: Boolean
        get() = supabaseAnonKey.isNotBlank() &&
            (supabaseUrl.startsWith("https://") || supabaseUrl.startsWith("http://"))

    /** Never print the key, even partially. */
    override fun toString(): String =
        "BackendConfig(supabaseUrl=$supabaseUrl, supabaseAnonKey=${if (supabaseAnonKey.isBlank()) "<missing>" else "<set>"})"
}
