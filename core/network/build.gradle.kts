// Remote access foundation: backend configuration, the shared OkHttp client, the Supabase
// client and the mapping from transport/SDK exceptions to AppError.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)

    api(platform(libs.supabase.bom))
    api(libs.supabase.auth)
    api(libs.supabase.postgrest)
    api(libs.supabase.realtime)
    api(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)

    api(libs.okhttp)
    implementation(libs.okhttp.logging)
}
