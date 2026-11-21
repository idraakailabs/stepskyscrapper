package ai.idraak.stepskyscraper.platform

import ai.idraak.stepskyscraper.BuildConfig
import ai.idraak.stepskyscraper.R
import ai.idraak.stepskyscraper.core.steps.HealthConnectPermissions
import ai.idraak.stepskyscraper.core.steps.HealthConnectStepSource
import ai.idraak.stepskyscraper.core.steps.SensorStepSource
import ai.idraak.stepskyscraper.core.ui.platform.PlatformActions
import ai.idraak.stepskyscraper.widget.MediumTowerWidgetReceiver
import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The single place that talks to the Android system on behalf of the screens. Activity-result launchers are
 * registered in the activity's onCreate (before STARTED) and complete the pending callback.
 */
class AndroidPlatformActions(
    private val activity: ComponentActivity,
    private val healthConnect: HealthConnectStepSource,
    private val sensor: SensorStepSource,
) : PlatformActions {
    private var stepCallback: ((Boolean) -> Unit)? = null
    private var notifyCallback: ((Boolean) -> Unit)? = null
    private var storageCallback: ((Boolean) -> Unit)? = null

    private val hcLauncher: ActivityResultLauncher<Set<String>> =
        activity.registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            stepCallback?.invoke(HealthConnectPermissions.READ_STEPS in granted)
            stepCallback = null
        }

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            stepCallback?.invoke(granted)
            stepCallback = null
            notifyCallback?.invoke(granted)
            notifyCallback = null
            storageCallback?.invoke(granted)
            storageCallback = null
        }

    override fun requestStepAccess(onResult: (granted: Boolean) -> Unit) {
        val status = healthConnect.sdkStatus()
        stepCallback = onResult
        when {
            status == HealthConnectClient.SDK_AVAILABLE -> hcLauncher.launch(healthConnect.permissionsToRequest())
            sensor.hasSensor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            sensor.hasSensor() -> onResult(true).also { stepCallback = null }
            else -> installHealthConnect().also { stepCallback = null }
        }
    }

    override fun openHealthConnect() {
        // The public Health Connect settings entry point. The per-app MANAGE_HEALTH_PERMISSIONS screen needs the
        // system-only GRANT_RUNTIME_PERMISSIONS on Android 14+ and threw a SecurityException (BUG-020); the help copy
        // already guides the user to App permissions › Step Skyscraper from here.
        if (!start(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))) installHealthConnect()
    }

    override fun installHealthConnect() {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${HealthConnectPermissions.PROVIDER_PACKAGE}&url=healthconnect%3A%2F%2Fonboarding"),
        )
            .setPackage("com.android.vending")
        if (!start(
                market,
            )
        ) {
            start(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${HealthConnectPermissions.PROVIDER_PACKAGE}"),
                ),
            )
        }
    }

    override fun isSamsungHealthInstalled(): Boolean =
        activity.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH) != null

    override fun openSamsungHealth() {
        activity.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH)?.let(::start)
            ?: start(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SAMSUNG_HEALTH")))
    }

    override fun openBatterySettings() {
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
    }

    override fun requestPinWidget(): Boolean {
        val manager = activity.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(activity, MediumTowerWidgetReceiver::class.java), null, null)
    }

    override fun requestNotificationPermission(onResult: (granted: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }
        notifyCallback = onResult
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun notificationsAllowed(): Boolean {
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(activity).areNotificationsEnabled()
    }

    override fun shareImage(image: ImageBitmap, caption: String) {
        activity.lifecycleScope.launch {
            val dir = File(activity.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, "step-skyscraper.png")
            try {
                file.outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            } catch (e: IOException) {
                Log.w(TAG, "Could not write share image", e)
                return@launch
            }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.share", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, caption)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            start(Intent.createChooser(send, null))
        }
    }

    override fun saveImage(image: ImageBitmap, onResult: (saved: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            // BUG-003: save as soon as the permission is granted instead of dropping the first tap.
            storageCallback = { granted -> if (granted) writeImage(image, onResult) else onResult(false) }
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        writeImage(image, onResult)
    }

    private fun writeImage(image: ImageBitmap, onResult: (Boolean) -> Unit) {
        activity.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { insertImage(image) }
            onResult(saved)
        }
    }

    private fun insertImage(image: ImageBitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "step-skyscraper-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Step Skyscraper")
        }
        val resolver = activity.contentResolver
        val uri = try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not create image entry", e)
            null
        } ?: return false
        val written = try {
            resolver.openOutputStream(uri)?.use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } ==
                true
        } catch (e: IOException) {
            Log.w(TAG, "Could not save image", e)
            false
        }
        // Don't leave an empty picture behind when the write failed.
        if (!written) runCatching { resolver.delete(uri, null, null) }
        return written
    }

    override fun openPlaySubscriptions() {
        start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${activity.packageName}")))
    }

    override val hasPrivacyPolicy: Boolean get() = BuildConfig.PRIVACY_POLICY_URL.isNotBlank()

    override fun openPrivacyPolicy() {
        val url = BuildConfig.PRIVACY_POLICY_URL
        if (url.isNotBlank()) start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override val hasTermsOfService: Boolean get() = BuildConfig.TERMS_URL.isNotBlank()

    override fun openTermsOfService() {
        val url = BuildConfig.TERMS_URL
        if (url.isNotBlank()) start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun sendDiagnostic(report: String) {
        val email = BuildConfig.SUPPORT_EMAIL
        val intent = if (email.isNotBlank()) {
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        } else {
            Intent(Intent.ACTION_SEND).setType("text/plain")
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.diagnostic_subject)).putExtra(Intent.EXTRA_TEXT, report)
        start(Intent.createChooser(intent, null))
    }

    override fun playCeremonySound() {
        val audio = activity.getSystemService(AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        MediaPlayer.create(
            activity,
            R.raw.ceremony,
            AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
            ).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            audio.generateAudioSessionId(),
        )?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    private fun start(intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No activity for ${intent.action}", e)
        false
    } catch (e: SecurityException) {
        // A protected system screen must never crash the app: report "not opened" so callers fall back.
        Log.w(TAG, "Not allowed to start ${intent.action}", e)
        false
    }

    private companion object {
        const val TAG = "PlatformActions"
        const val SAMSUNG_HEALTH = "com.sec.android.app.shealth"
    }
}
