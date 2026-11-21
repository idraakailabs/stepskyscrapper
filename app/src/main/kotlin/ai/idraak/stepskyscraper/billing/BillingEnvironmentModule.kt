package ai.idraak.stepskyscraper.billing

import ai.idraak.stepskyscraper.BuildConfig
import ai.idraak.stepskyscraper.core.billing.BillingEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** RevenueCat key from app/build.gradle.kts (`REVENUECAT_API_KEY` Gradle property or environment variable). */
@Module
@InstallIn(SingletonComponent::class)
object BillingEnvironmentModule {
    @Provides
    @Singleton
    fun billingEnvironment(): BillingEnvironment = BillingEnvironment(revenueCatApiKey = BuildConfig.REVENUECAT_API_KEY)
}
