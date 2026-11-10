// Player UI. Media3/ExoPlayer and the MediaSessionService are added in Phase 3 together with a
// playback abstraction in :core:domain; until then this module renders the idle player only.
plugins {
    alias(libs.plugins.musicsocial.android.feature)
}

dependencies {
    // TESTING ONLY: sample content until the real repositories exist.
    implementation(projects.core.sample)
}
