package ai.idraak.stepskyscraper.core.ui.platform

import ai.idraak.stepskyscraper.core.domain.model.UiHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

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

    /** True when the app may post notifications (POST_NOTIFICATIONS on Android 13+ and not blocked in system settings). */
    fun notificationsAllowed(): Boolean

    fun shareImage(image: ImageBitmap, caption: String)

    /**
     * Saves to Pictures/Step Skyscraper and reports the result on the main thread. On Android 8–9 it asks for
     * WRITE_EXTERNAL_STORAGE first and saves straight after the grant (no second tap).
     */
    fun saveImage(image: ImageBitmap, onResult: (saved: Boolean) -> Unit)

    fun openPlaySubscriptions()

    /** False when no hosted privacy-policy URL is configured for this build. */
    val hasPrivacyPolicy: Boolean

    fun openPrivacyPolicy()

    /** False when no hosted terms-of-service URL is configured for this build. */
    val hasTermsOfService: Boolean

    fun openTermsOfService()

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

    override fun notificationsAllowed(): Boolean = true

    override fun shareImage(image: ImageBitmap, caption: String) = Unit

    override fun saveImage(image: ImageBitmap, onResult: (Boolean) -> Unit) = onResult(true)

    override fun openPlaySubscriptions() = Unit

    override val hasPrivacyPolicy: Boolean = true

    override fun openPrivacyPolicy() = Unit

    override val hasTermsOfService: Boolean = true

    override fun openTermsOfService() = Unit

    override fun sendDiagnostic(report: String) = Unit

    override fun playCeremonySound() = Unit
}

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> { PreviewPlatformActions }

/**
 * Whether notifications can actually be delivered, re-checked every time the screen resumes (the user may change
 * the permission in system settings while the app is in the background). Screens use it so a notification
 * preference never reads "on" while the OS blocks delivery (BUG-001).
 */
@Composable
fun rememberNotificationsAllowed(): Boolean {
    val platform = LocalPlatformActions.current
    var allowed by remember(platform) { mutableStateOf(platform.notificationsAllowed()) }
    LifecycleResumeEffect(platform) {
        allowed = platform.notificationsAllowed()
        onPauseOrDispose { }
    }
    return allowed
}

/** Suspending form of [PlatformActions.requestNotificationPermission]. */
suspend fun PlatformActions.awaitNotificationPermission(): Boolean = suspendCancellableCoroutine { cont ->
    requestNotificationPermission { granted -> if (cont.isActive) cont.resume(granted) }
}

/** The Activity, as the opaque [UiHost] purchase / ad flows need. */
val LocalUiHost = staticCompositionLocalOf<UiHost> { object : UiHost {} }
