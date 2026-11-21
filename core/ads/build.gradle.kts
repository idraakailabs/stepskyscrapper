plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
}

// Ad unit ids are chosen at runtime from AdsEnvironment, which :app provides per variant (see
// app/build.gradle.kts): debug variants always get Google's test ids, only the release variant gets live ids.

dependencies {
    implementation(projects.core.domain)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.kotlinx.coroutines.android)
}
