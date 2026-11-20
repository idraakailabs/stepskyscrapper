import java.util.Properties

plugins {
    alias(libs.plugins.stepsky.android.application)
    alias(libs.plugins.stepsky.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
}

// Firebase is configured per environment: the google-services / Crashlytics plugins are applied only when
// app/google-services.json is present. Without it the app runs with the no-op analytics/remote-config
// fallbacks in :core:analytics and :core:data (see docs/design-deviations.md).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "com.idraak.stepskyscraper"

    defaultConfig {
        applicationId = "com.idraak.stepskyscraper"
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["admobAppId"] = providers.gradleProperty("ADMOB_APP_ID").get()
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${providers.gradleProperty("PRIVACY_POLICY_URL").getOrElse("")}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${providers.gradleProperty("SUPPORT_EMAIL").getOrElse("")}\"")
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
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
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.steps)
    implementation(projects.core.billing)
    implementation(projects.core.ads)
    implementation(projects.core.analytics)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.build)
    implementation(projects.feature.skyline)
    implementation(projects.feature.customise)
    implementation(projects.feature.me)
    implementation(projects.feature.share)
    implementation(projects.feature.paywall)
    implementation(projects.widget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.health.connect)
    "baselineProfile"(projects.baselineprofile)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
