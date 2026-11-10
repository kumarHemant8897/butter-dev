// Cross-cutting Android utilities: logging, coroutine dispatchers/scopes, connectivity.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
}

dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
}
