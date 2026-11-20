package com.idraak.stepskyscraper.core.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.idraak.stepskyscraper.core.domain.model.UiHost

/**
 * System integrations a screen can trigger (Health Connect permission sheet, deep links, widget pinning,
 * share sheet). Implemented once in :app; features talk to this interface only.
 */
interface PlatformActions {
    /** Opens the Health Connect permission sheet (Steps read only), or ACTIVITY_RECOGNITION on the sensor fallback. */
    fun requestStepAccess(onResult: (granted: Boolean) -> Unit)

    fun openHealthConnect()

    /** Play listing for Health Connect (missing on Android 13 and lower, or needs an update). */
    fun installHealthConnect()

    fun isSamsungHealthInstalled(): Boolean

    fun openSamsungHealth()

    fun openBatterySettings()

    /** requestPinAppWidget(); returns false when the launcher does not support pinning. */
    fun requestPinWidget(): Boolean

    fun requestNotificationPermission(onResult: (granted: Boolean) -> Unit)

    fun shareImage(image: ImageBitmap, caption: String)

    /** Saves to Pictures/Step Skyscraper; returns false if saving failed. */
    fun saveImage(image: ImageBitmap): Boolean

    fun openPlaySubscriptions()

    /** False when no hosted privacy-policy URL is configured for this build. */
    val hasPrivacyPolicy: Boolean

    fun openPrivacyPolicy()

    /** Opens an email draft containing sync times only (never step data). */
    fun sendDiagnostic(report: String)

    fun playCeremonySound()
}

/** No-op implementation for previews, screenshot tests and the component gallery. */
object PreviewPlatformActions : PlatformActions {
    override fun requestStepAccess(onResult: (Boolean) -> Unit) = onResult(true)

    override fun openHealthConnect() = Unit

    override fun installHealthConnect() = Unit

    override fun isSamsungHealthInstalled(): Boolean = false

    override fun openSamsungHealth() = Unit

    override fun openBatterySettings() = Unit

    override fun requestPinWidget(): Boolean = true

    override fun requestNotificationPermission(onResult: (Boolean) -> Unit) = onResult(true)

    override fun shareImage(image: ImageBitmap, caption: String) = Unit

    override fun saveImage(image: ImageBitmap): Boolean = true

    override fun openPlaySubscriptions() = Unit

    override val hasPrivacyPolicy: Boolean = true

    override fun openPrivacyPolicy() = Unit

    override fun sendDiagnostic(report: String) = Unit

    override fun playCeremonySound() = Unit
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { PreviewPlatformActions }

/** The Activity, as the opaque [UiHost] purchase / ad flows need. */
val LocalUiHost = staticCompositionLocalOf<UiHost> { object : UiHost {} }
