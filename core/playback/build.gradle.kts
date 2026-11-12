// The app's single player: ExoPlayer behind the PlaybackController interface from :core:domain,
// plus a MediaSessionService so playback continues in the background with lock-screen and
// notification controls.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
}
