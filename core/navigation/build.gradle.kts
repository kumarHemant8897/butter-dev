// Type-safe route contracts shared across features. A feature navigates to another feature's
// screen through these @Serializable types, so features never depend on each other.
plugins {
    alias(libs.plugins.musicsocial.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
