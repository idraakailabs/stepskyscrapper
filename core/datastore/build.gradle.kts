plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
