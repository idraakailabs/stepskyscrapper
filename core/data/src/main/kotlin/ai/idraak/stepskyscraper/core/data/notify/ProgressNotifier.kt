package ai.idraak.stepskyscraper.core.data.notify

import ai.idraak.stepskyscraper.core.data.R
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.repository.AchievementRepository
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.domain.repository.SyncListener
import ai.idraak.stepskyscraper.core.domain.usecase.CalculateWeekProgressUseCase
import ai.idraak.stepskyscraper.core.domain.usecase.GetTowerProgressUseCase
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Local notifications only: "topping out" and a Sunday-evening weekly summary (Me › Notifications).
 * Posted from the device; nothing is sent anywhere. Runs after each sync.
 */
@Singleton
class ProgressNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progress: GetTowerProgressUseCase,
    private val settings: SettingsRepository,
    private val counters: AchievementRepository,
    private val clock: AppClock,
) : SyncListener {
    override suspend fun onSynced() {
        val prefs = settings.settings.first()
        if (!prefs.onboardingComplete || !canNotify()) return
        val snapshot = progress().first()
        val counts = counters.counters.first()
        if (prefs.notifyToppingOut) {
            val notified = counts[COUNTER_TOPPED] ?: 0
            val finished = snapshot.city.finishedTowers
            if (finished.size > notified) {
                val tower = finished.last()
                post(
                    ID_TOPPED,
                    CHANNEL_PROGRESS,
                    context.getString(R.string.notify_topped_title, tower.name),
                    context.getString(R.string.notify_topped_body, tower.target),
                )
                repeat(finished.size - notified) { counters.increment(COUNTER_TOPPED) }
            }
        }
        val now = clock.localNow()
        if (prefs.notifyWeeklySummary && now.dayOfWeek == DayOfWeek.SUNDAY && now.toLocalTime() >= SUMMARY_FROM) {
            val key = "weekly_summary_" + CalculateWeekProgressUseCase.weekStartOf(now.toLocalDate())
            if ((counts[key] ?: 0) == 0) {
                val week = snapshot.week
                post(
                    ID_WEEKLY,
                    CHANNEL_SUMMARY,
                    context.getString(R.string.notify_weekly_title),
                    context.getString(R.string.notify_weekly_body, week.floors, snapshot.city.activeTower.name),
                )
                counters.increment(key)
            }
        }
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // POST_NOTIFICATIONS is checked by canNotify() (guarded at the top of this function and in onSynced).
    @android.annotation.SuppressLint("MissingPermission")
    private fun post(id: Int, channel: String, title: String, body: String) {
        ensureChannels()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                id,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_tower)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (!canNotify()) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("ProgressNotifier", "Notification permission revoked", e)
        }
    }

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.channel_topping_out), NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, context.getString(R.string.channel_weekly), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private companion object {
        const val CHANNEL_PROGRESS = "topping_out"
        const val CHANNEL_SUMMARY = "weekly_summary"
        const val ID_TOPPED = 1001
        const val ID_WEEKLY = 1002
        const val COUNTER_TOPPED = "notified_towers"
        val SUMMARY_FROM: LocalTime = LocalTime.of(18, 0)
    }
}
