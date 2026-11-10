// Pure Kotlin module: domain models, repository contracts, use cases and the shared
// result/error types. It must never depend on Android, Supabase, Room or any other module.
plugins {
    alias(libs.plugins.musicsocial.jvm.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
}
