// SAMPLE DATA FOR TESTING THE UI. Not real users, songs or messages.
// Features read from here only until the matching repository is implemented; then the
// ViewModel switches to the repository and this module is deleted (see docs/IMPLEMENTATION_STATUS.md).
plugins {
    alias(libs.plugins.musicsocial.jvm.library)
}

dependencies {
    api(projects.core.domain)
}
