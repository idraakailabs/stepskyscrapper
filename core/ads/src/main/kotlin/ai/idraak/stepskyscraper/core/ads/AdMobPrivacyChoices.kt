package ai.idraak.stepskyscraper.core.ads

import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.AdPrivacyChoices
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UMP privacy options entry point (required by Google's GDPR messages so users can change consent later).
 * Behind the same [AdsGate] as the rewarded ad: while ads are off there is no consent request and no entry point.
 */
@Singleton
class AdMobPrivacyChoices @Inject constructor(
    private val gate: AdsGate,
    private val sdk: AdSdk,
) : AdPrivacyChoices {
    override suspend fun isRequired(host: UiHost): Boolean {
        if (gate.currentUnitId() == null) return false
        sdk.updateConsentInfo(host)
        return sdk.privacyOptionsRequired()
    }

    override suspend fun show(host: UiHost) {
        if (gate.currentUnitId() == null) return
        sdk.showPrivacyOptionsForm(host)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdPrivacyModule {
    @Binds
    abstract fun privacyChoices(impl: AdMobPrivacyChoices): AdPrivacyChoices
}
