// Shared test utilities and fakes. Only ever added as a test/androidTest dependency.
plugins {
    alias(libs.plugins.musicsocial.android.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
}
