import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint + detekt for every module, both wired into `check`. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        extensions.configure<KtlintExtension> {
            android.set(true)
            outputToConsole.set(true)
            filter {
                exclude { it.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") }
            }
        }
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            parallel = true
        }
        // detekt 1.23.x embeds the Kotlin 2.0 compiler; keep its private classpath on that version.
        configurations.matching { it.name == "detekt" }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") useVersion("2.0.21")
            }
        }
    }
}
