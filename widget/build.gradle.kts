plugins {
    alias(libs.plugins.stepsky.android.library.compose)
    alias(libs.plugins.stepsky.android.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(projects.core.testing)
}
