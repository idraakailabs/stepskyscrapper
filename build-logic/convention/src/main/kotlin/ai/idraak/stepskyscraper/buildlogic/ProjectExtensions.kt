package ai.idraak.stepskyscraper.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

/** Kotlin compiler options shared by every module (Android and JVM). */
fun Project.configureKotlinCompiler() {
    // Guard against a hung test (e.g. an endless ticker under virtual time) blocking the build forever.
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        timeout.set(java.time.Duration.ofMinutes(15))
        maxHeapSize = "2g"
        // Modules whose test sources only hold robolectric.properties have nothing to discover.
        failOnNoDiscoveredTests.set(false)
    }
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            if (this is KotlinJvmCompilerOptions) {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            allWarningsAsErrors.set(false)
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xconsistent-data-class-copy-visibility",
            )
        }
    }
}
