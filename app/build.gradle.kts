import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.musicsocial.android.application)
    alias(libs.plugins.musicsocial.android.compose)
    alias(libs.plugins.musicsocial.hilt)
}

/**
 * Backend secrets come from local.properties (developers) or environment variables (CI), never
 * from source control. Read through Gradle providers so the configuration cache notices changes.
 */
val localProperties: Properties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}

fun secret(name: String): String =
    (localProperties.getProperty(name) ?: providers.environmentVariable(name).orNull ?: "").trim()

/** Refuses to build with a Supabase secret/service_role key, which bypasses Row Level Security. */
fun requirePublishableKey(name: String, key: String) {
    val isSecretKey = key.startsWith("sb_secret_")
    val isServiceRoleJwt = key.split('.').getOrNull(1)?.let { payload ->
        runCatching { String(Base64.getUrlDecoder().decode(payload)) }.getOrNull()?.contains("\"service_role\"")
    } == true
    if (isSecretKey || isServiceRoleJwt) {
        throw GradleException("$name is a Supabase secret/service_role key. Use the publishable (anon) key in the app.")
    }
}

fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.yourcompany.musicsocial"

    defaultConfig {
        // Placeholder. The applicationId cannot change after the first Play Store release.
        applicationId = "com.yourcompany.musicsocial"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            val url = secret("SUPABASE_URL")
            val key = secret("SUPABASE_ANON_KEY")
            requirePublishableKey("SUPABASE_ANON_KEY", key)
            buildConfigField("String", "SUPABASE_URL", quoted(url))
            buildConfigField("String", "SUPABASE_ANON_KEY", quoted(key))
        }
        create("prod") {
            dimension = "environment"
            val url = secret("PROD_SUPABASE_URL")
            val key = secret("PROD_SUPABASE_ANON_KEY")
            requirePublishableKey("PROD_SUPABASE_ANON_KEY", key)
            buildConfigField("String", "SUPABASE_URL", quoted(url))
            buildConfigField("String", "SUPABASE_ANON_KEY", quoted(key))
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signing is intentionally not configured here. See docs/DEVELOPMENT_GUIDE.md (Release signing).
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/INDEX.LIST", "/META-INF/io.netty.versions.properties")
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.navigation)
    implementation(projects.core.network)
    implementation(projects.core.ui)

    // Data modules are wired here and only here: features see repository interfaces, never these.
    implementation(projects.data.auth)
    implementation(projects.data.chat)
    implementation(projects.data.music)
    implementation(projects.data.user)

    implementation(projects.feature.chat)
    implementation(projects.feature.discover)
    implementation(projects.feature.home)
    implementation(projects.feature.player)
    implementation(projects.feature.profile)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.timber)

    testImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
}
