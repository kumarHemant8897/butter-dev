// Conversations and messages. Phase 1 defines the remote contract, DTOs and mapping;
// the Supabase (PostgREST + Realtime) implementation and ChatRepository binding arrive in Phase 4.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.serialization.json)
}
