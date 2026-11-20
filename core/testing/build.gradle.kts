plugins {
    alias(libs.plugins.stepsky.android.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.truth)
}
