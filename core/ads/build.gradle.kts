plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
}

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "REWARDED_UNIT_ID", "\"${providers.gradleProperty("ADMOB_REWARDED_UNIT_ID").get()}\"")
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.kotlinx.coroutines.android)
}
