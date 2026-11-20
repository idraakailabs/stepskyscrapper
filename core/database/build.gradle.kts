plugins {
    alias(libs.plugins.stepsky.android.library)
    alias(libs.plugins.stepsky.android.hilt)
    alias(libs.plugins.stepsky.android.room)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
