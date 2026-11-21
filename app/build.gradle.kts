import com.android.build.api.variant.BuildConfigField
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

// Release signing (upload key). Read from keystore.properties (local, git-ignored) or, in CI, from the
// STEPSKY_* environment variables. There is deliberately no fallback to the debug key: without a complete
// config the release build is left unsigned, and bundleRelease fails (see verifyReleaseSigning below).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(property: String, env: String): String? =
    keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "STEPSKY_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "STEPSKY_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "STEPSKY_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "STEPSKY_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null } &&
    rootProject.file(releaseStoreFile!!).exists()

// AdMob ids per variant. Debug (and every non-release variant) always uses Google's published test ids,
// hard-coded here and never read from properties. Only the `release` variant (Prod Release) uses live ids, from
// the Gradle properties or environment variables ADMOB_APP_ID_PROD / ADMOB_REWARDED_UNIT_ID_PROD
// (~/.gradle/gradle.properties or CI secrets, never committed). Without valid live ids, assembleRelease still
// builds for local smoke tests with ads disabled at runtime, and bundleRelease fails (verifyReleaseAdMob).
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobSamplePublisher = "ca-app-pub-3940256099942544"

fun adProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull?.trim()?.takeIf { it.isNotBlank() }

val admobLiveAppId = adProperty("ADMOB_APP_ID_PROD")
val admobLiveRewardedUnitId = adProperty("ADMOB_REWARDED_UNIT_ID_PROD")
val admobLiveIdsProblem: String? = when {
    admobLiveAppId == null || admobLiveRewardedUnitId == null -> "ADMOB_APP_ID_PROD and ADMOB_REWARDED_UNIT_ID_PROD are not both set"
    admobLiveAppId.startsWith(admobSamplePublisher) || admobLiveRewardedUnitId.startsWith(admobSamplePublisher) ->
        "the live AdMob ids are Google's sample ids"
    !Regex("""ca-app-pub-\d{16}~\d{10}""").matches(admobLiveAppId) -> "ADMOB_APP_ID_PROD is not an app id (ca-app-pub-…~…)"
    !Regex("""ca-app-pub-\d{16}/\d{10}""").matches(admobLiveRewardedUnitId) ->
        "ADMOB_REWARDED_UNIT_ID_PROD is not an ad unit id (ca-app-pub-…/…)"
    else -> null
}
val admobLiveConfigured = admobLiveIdsProblem == null

android {
    namespace = "ai.idraak.stepskyscraper"

    defaultConfig {
        applicationId = "ai.idraak.stepskyscraper"
        versionCode = 1
        versionName = "1.0.0"
        // Default for components without variant placeholders (unit-test manifests); every app variant overrides
        // it in androidComponents.onVariants below.
        manifestPlaceholders["admobAppId"] = admobTestAppId
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${providers.gradleProperty("PRIVACY_POLICY_URL").getOrElse("")}\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"${providers.gradleProperty("SUPPORT_EMAIL").getOrElse("")}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            // null (unsigned) when no upload key is configured; never the debug key.
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val prodRelease = variant.name == "release"
        val live = prodRelease && admobLiveConfigured
        // The manifest always needs a well-formed APPLICATION_ID; without live ids the release build keeps the
        // test app id but the runtime never initialises the SDK or requests an ad (AdSetup.NotConfigured).
        variant.manifestPlaceholders.put("admobAppId", if (live) admobLiveAppId!! else admobTestAppId)
        val fields = variant.buildConfigFields ?: return@onVariants
        fun str(value: String) = BuildConfigField("String", "\"$value\"", null)
        fields.put("ADS_PROD_RELEASE", BuildConfigField("boolean", prodRelease.toString(), "Live-ad variant"))
        fields.put("ADMOB_LIVE_APP_ID", str(if (live) admobLiveAppId!! else ""))
        fields.put("ADMOB_LIVE_REWARDED_UNIT_ID", str(if (live) admobLiveRewardedUnitId!! else ""))
        // Debug-only helpers for consent testing (ignored by the release variant at runtime).
        fields.put("ADMOB_TEST_DEVICE_ID", str(if (prodRelease) "" else adProperty("ADMOB_TEST_DEVICE_ID").orEmpty()))
        fields.put(
            "ADMOB_UMP_DEBUG_EEA",
            BuildConfigField("boolean", (!prodRelease && adProperty("ADMOB_UMP_DEBUG_EEA") == "true").toString(), null),
        )
    }
}

// A Play upload must carry real AdMob ids: fail instead of shipping sample ids or a build with ads disabled.
val verifyReleaseAdMob = tasks.register("verifyReleaseAdMob") {
    group = "verification"
    description = "Fails unless live AdMob ids are configured for the release variant."
    val problem = admobLiveIdsProblem
    doLast {
        if (problem != null) {
            throw GradleException(
                "AdMob live ids are not configured: $problem. Set ADMOB_APP_ID_PROD and ADMOB_REWARDED_UNIT_ID_PROD " +
                    "in ~/.gradle/gradle.properties or as environment variables (CI secrets). " +
                    "See docs/TECHNICAL.md §6.",
            )
        }
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach { dependsOn(verifyReleaseAdMob) }
// assembleRelease stays usable for smoke tests; say loudly that its ads are off.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val problem = admobLiveIdsProblem
    doFirst {
        if (problem != null) logger.warn("w: AdMob: $problem; this release build has ads disabled (no ad requests).")
    }
}

// Health Connect apps (and apps with ads) need a hosted privacy policy, linked in-app from Help & privacy:
// fail the Play bundle instead of shipping a build whose policy link is hidden.
val verifyReleasePrivacyPolicy = tasks.register("verifyReleasePrivacyPolicy") {
    group = "verification"
    description = "Fails unless PRIVACY_POLICY_URL is an https URL."
    val url = providers.gradleProperty("PRIVACY_POLICY_URL").getOrElse("").trim()
    doLast {
        if (!url.startsWith("https://")) {
            throw GradleException(
                "PRIVACY_POLICY_URL is not set to an https URL. Host docs/privacy-policy.md and set PRIVACY_POLICY_URL " +
                    "in gradle.properties (or pass -PPRIVACY_POLICY_URL=…). See docs/play-console-checklist.md.",
            )
        }
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach { dependsOn(verifyReleasePrivacyPolicy) }

// A Play upload must be signed with the upload key: fail early with instructions instead of producing an
// unsigned bundle. assembleRelease (part of `build`) still works and yields app-release-unsigned.apk.
val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Fails unless the release upload key is configured."
    // Captured as a local so the configuration cache can serialise the action.
    val releaseSigningConfigured = hasReleaseSigning
    doLast {
        if (!releaseSigningConfigured) {
            throw GradleException(
                "Release signing is not configured. Create keystore.properties in the project root " +
                    "(see keystore.properties.example) or set STEPSKY_KEYSTORE_FILE, STEPSKY_KEYSTORE_PASSWORD, " +
                    "STEPSKY_KEY_ALIAS and STEPSKY_KEY_PASSWORD. See docs/TECHNICAL.md §6.",
            )
        }
    }
}
tasks.matching { it.name == "bundleRelease" }.configureEach { dependsOn(verifyReleaseSigning) }

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
