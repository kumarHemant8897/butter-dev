import com.yourcompany.musicsocial.buildlogic.library
import com.yourcompany.musicsocial.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention for `:feature:*` modules.
 *
 * A feature module contains presentation code only (screens, components, ViewModels, UI state).
 * It may depend on core modules but never on another feature or on a `:data:*` module, which keeps
 * features independently buildable and stops UI code from reaching Supabase or Room directly.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("musicsocial.android.library")
        pluginManager.apply("musicsocial.android.compose")
        pluginManager.apply("musicsocial.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:navigation"))
            add("implementation", project(":core:ui"))

            add("implementation", libs.library("androidx-compose-material3"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("kotlinx-collections-immutable"))
            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
