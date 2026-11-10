// UI building blocks that understand domain types (AppError -> message, screen scaffolds,
// formatting). Kept apart from :core:designsystem so the design system stays domain-agnostic.
plugins {
    alias(libs.plugins.musicsocial.android.library)
    alias(libs.plugins.musicsocial.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.domain)
}
