import com.android.build.api.dsl.ApplicationExtension
import com.yourcompany.musicsocial.buildlogic.JAVA_VERSION
import com.yourcompany.musicsocial.buildlogic.addUnitTestDependencies
import com.yourcompany.musicsocial.buildlogic.configureKotlinJvmTarget
import com.yourcompany.musicsocial.buildlogic.intVersion
import com.yourcompany.musicsocial.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Base configuration for the single `:app` module.
 * AGP 9 compiles Kotlin natively ("built-in Kotlin"), so the Kotlin Android plugin is not applied.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.intVersion("compileSdk")
            defaultConfig {
                minSdk = libs.intVersion("minSdk")
                targetSdk = libs.intVersion("targetSdk")
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
