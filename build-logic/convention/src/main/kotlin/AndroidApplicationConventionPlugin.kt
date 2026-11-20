import com.android.build.api.dsl.ApplicationExtension
import ai.idraak.stepskyscraper.buildlogic.JAVA_VERSION
import ai.idraak.stepskyscraper.buildlogic.configureCompose
import ai.idraak.stepskyscraper.buildlogic.configureKotlinCompiler
import ai.idraak.stepskyscraper.buildlogic.libs
import ai.idraak.stepskyscraper.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.configure<ApplicationExtension> {
            compileSdk {
                version = release(libs.version("compileSdk").toInt()) { minorApiLevel = libs.version("compileSdkMinor").toInt() }
            }
            defaultConfig {
                minSdk = libs.version("minSdk").toInt()
                targetSdk = libs.version("targetSdk").toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            buildFeatures {
                compose = true
                buildConfig = true
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            packaging {
                resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
            }
        }
        configureKotlinCompiler()
        configureCompose()
    }
}
