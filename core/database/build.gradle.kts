// Room database: local cache and (later) offline outbox. Entities and DAOs live here;
// mapping to domain models happens in the :data:* modules that own each table.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.android.room)
    alias(libs.plugins.musicsocial.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
