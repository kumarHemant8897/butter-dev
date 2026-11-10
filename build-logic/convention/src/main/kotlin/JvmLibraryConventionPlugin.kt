import com.yourcompany.musicsocial.buildlogic.JAVA_VERSION
import com.yourcompany.musicsocial.buildlogic.addUnitTestDependencies
import com.yourcompany.musicsocial.buildlogic.configureKotlinJvmTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/**
 * Pure Kotlin/JVM module with no Android dependency (used by `:core:domain`).
 * Keeping the domain layer off the Android SDK makes it fast to test and impossible
 * to accidentally couple business rules to framework classes.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        configureKotlinJvmTarget()
        addUnitTestDependencies()
    }
}
