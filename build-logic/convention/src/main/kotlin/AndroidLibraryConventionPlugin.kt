import com.android.build.api.dsl.LibraryExtension
import com.yourcompany.musicsocial.buildlogic.JAVA_VERSION
import com.yourcompany.musicsocial.buildlogic.addUnitTestDependencies
import com.yourcompany.musicsocial.buildlogic.configureKotlinJvmTarget
import com.yourcompany.musicsocial.buildlogic.defaultNamespace
import com.yourcompany.musicsocial.buildlogic.intVersion
import com.yourcompany.musicsocial.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Base configuration for every Android library module (core, data and feature modules). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            namespace = defaultNamespace()
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            testOptions {
                unitTests.isReturnDefaultValues = true
            }
        }
        configureKotlinJvmTarget()
        addUnitTestDependencies()
    }
}
