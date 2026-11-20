plugins {
    alias(libs.plugins.stepsky.android.library.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
}
