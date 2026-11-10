// Music catalogue: Supabase remote source + Room cache behind MusicRepository.
// This is the reference implementation of the repository pattern for the other data modules.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
