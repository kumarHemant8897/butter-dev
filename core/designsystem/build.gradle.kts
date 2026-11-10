// Visual language of the app: tokens (colour, type, shape, spacing), theme, icons and
// stateless, reusable components. It knows nothing about domain models or features.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.android.compose)
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.coil.compose)
}
