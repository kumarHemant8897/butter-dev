// Authentication: Supabase Auth data source and the AuthRepository implementation.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.hilt)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)

    testImplementation(projects.core.testing)
}
