plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
