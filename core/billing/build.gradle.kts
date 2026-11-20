plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.datastore)
    implementation(libs.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(projects.core.testing)
}
