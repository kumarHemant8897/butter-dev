// Profiles and the follow graph: Supabase data source and the UserRepository implementation.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
