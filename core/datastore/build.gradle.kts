// Small key-value settings stored with Preferences DataStore (theme, onboarding flags).
// Anything relational or large belongs in :core:database instead.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.dataStore.preferences)
}
