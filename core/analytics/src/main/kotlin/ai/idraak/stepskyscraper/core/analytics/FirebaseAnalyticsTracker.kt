package ai.idraak.stepskyscraper.core.analytics

import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anonymous product analytics. [AnalyticsEvent] can only carry enum-like parameters, so no step count, floor
 * count or other health value can ever be sent. No user id or user properties are set. When Firebase is not
 * configured for this build (no google-services.json) events are dropped locally.
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnalyticsTracker {
    private val analytics: FirebaseAnalytics? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAnalytics.getInstance(context)
    }

    override fun track(event: AnalyticsEvent) {
        val instance = analytics
        if (instance == null) {
            Log.d(TAG, "Analytics not configured; dropped ${event.name}")
            return
        }
        val bundle = Bundle().apply { event.params.forEach { (k, v) -> putString(k, v) } }
        instance.logEvent(event.name, bundle)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}

/** Records non-fatal problems in Crashlytics without any health data. */
@Singleton
class CrashReporter @Inject constructor(@ApplicationContext private val context: Context) {
    fun record(throwable: Throwable) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.w("CrashReporter", "Crashlytics not configured", throwable)
            return
        }
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun tracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker
}
