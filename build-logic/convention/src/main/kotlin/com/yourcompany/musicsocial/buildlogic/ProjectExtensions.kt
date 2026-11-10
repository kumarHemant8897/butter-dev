package com.yourcompany.musicsocial.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** Single place that defines the base package for every module namespace. */
internal const val BASE_PACKAGE = "com.yourcompany.musicsocial"

/** Java/Kotlin bytecode level shared by all modules. */
internal val JAVA_VERSION = JavaVersion.VERSION_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("Unknown library alias '$alias' in libs.versions.toml") }

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).orElseThrow { IllegalArgumentException("Unknown version alias '$alias'") }
        .requiredVersion.toInt()

/** ":core:designsystem" -> "com.yourcompany.musicsocial.core.designsystem" */
internal fun Project.defaultNamespace(): String =
    BASE_PACKAGE + "." + path.removePrefix(":").replace(':', '.').replace('-', '_')

internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
        }
    }
}

/** Unit-test dependencies every module gets, so test conventions are uniform. */
internal fun Project.addUnitTestDependencies() {
    dependencies.apply {
        add("testImplementation", libs.library("junit4"))
        add("testImplementation", libs.library("mockk"))
        add("testImplementation", libs.library("turbine"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
    }
}
