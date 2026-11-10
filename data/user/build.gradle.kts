// Profiles and the follow graph. Phase 1 defines the remote contract and DTOs;
// the Supabase implementation and UserRepository binding arrive with Phase 2 (profiles).
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.serialization.json)
}
