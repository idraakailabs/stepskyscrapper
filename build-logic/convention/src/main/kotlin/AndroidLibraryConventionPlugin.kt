import com.android.build.api.dsl.LibraryExtension
import ai.idraak.stepskyscraper.buildlogic.JAVA_VERSION
import ai.idraak.stepskyscraper.buildlogic.configureKotlinCompiler
import ai.idraak.stepskyscraper.buildlogic.lib
import ai.idraak.stepskyscraper.buildlogic.libs
import ai.idraak.stepskyscraper.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            compileSdk {
                version = release(libs.version("compileSdk").toInt()) { minorApiLevel = libs.version("compileSdkMinor").toInt() }
            }
            defaultConfig {
                minSdk = libs.version("minSdk").toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            compileOptions {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
                targetSdk = libs.version("targetSdk").toInt()
            }
            lint {
                targetSdk = libs.version("targetSdk").toInt()
            }
            // Namespace derived from the Gradle path: :core:data -> ai.idraak.stepskyscraper.core.data
            namespace = "ai.idraak.stepskyscraper." + path.removePrefix(":").replace(':', '.').replace('-', '_')
        }
        configureKotlinCompiler()
        dependencies {
            add("testImplementation", libs.lib("junit4"))
            add("testImplementation", libs.lib("truth"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("turbine"))
        }
    }
}
