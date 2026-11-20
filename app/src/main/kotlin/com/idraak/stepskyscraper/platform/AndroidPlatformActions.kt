package com.idraak.stepskyscraper.platform

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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.idraak.stepskyscraper.BuildConfig
import com.idraak.stepskyscraper.R
import com.idraak.stepskyscraper.core.steps.HealthConnectPermissions
import com.idraak.stepskyscraper.core.steps.HealthConnectStepSource
import com.idraak.stepskyscraper.core.steps.SensorStepSource
import com.idraak.stepskyscraper.core.ui.platform.PlatformActions
import com.idraak.stepskyscraper.widget.MediumTowerWidgetReceiver
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch

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
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(ACTION_MANAGE_HEALTH_PERMISSIONS).putExtra(Intent.EXTRA_PACKAGE_NAME, activity.packageName)
        } else {
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        }
        if (!start(intent)) installHealthConnect()
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

    override fun saveImage(image: ImageBitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "step-skyscraper-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Step Skyscraper")
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return false
        }
        val resolver = activity.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } ==
                true
        } catch (e: IOException) {
            Log.w(TAG, "Could not save image", e)
            false
        }
    }

    override fun openPlaySubscriptions() {
        start(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${activity.packageName}")))
    }

    override val hasPrivacyPolicy: Boolean get() = BuildConfig.PRIVACY_POLICY_URL.isNotBlank()

    override fun openPrivacyPolicy() {
        val url = BuildConfig.PRIVACY_POLICY_URL
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
    }

    private companion object {
        const val TAG = "PlatformActions"
        const val SAMSUNG_HEALTH = "com.sec.android.app.shealth"
        const val ACTION_MANAGE_HEALTH_PERMISSIONS = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"
    }
}
