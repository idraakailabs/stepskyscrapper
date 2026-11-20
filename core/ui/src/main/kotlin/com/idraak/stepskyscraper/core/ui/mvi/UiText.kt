package com.idraak.stepskyscraper.core.ui.mvi

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.idraak.stepskyscraper.core.domain.model.AdFailure
import com.idraak.stepskyscraper.core.domain.model.BillingFailure
import com.idraak.stepskyscraper.core.domain.model.DomainError
import com.idraak.stepskyscraper.core.ui.R

/** Text resolved at render time, so ViewModels never touch Context. */
@Immutable
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    data class Raw(val value: String) : UiText

    @Composable
    fun resolve(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Plural -> pluralStringResource(id, count, *args.toTypedArray())
        is Raw -> value
    }
}

/** Calm, reassuring copy for every domain error (no red states, no blame). */
fun DomainError.toUiText(): UiText = when (this) {
    DomainError.HealthConnectNotInstalled -> UiText.Res(R.string.error_hc_missing)
    DomainError.HealthConnectNeedsUpdate -> UiText.Res(R.string.error_hc_update)
    DomainError.StepPermissionDenied -> UiText.Res(R.string.error_permission)
    DomainError.NoStepSource -> UiText.Res(R.string.error_no_source)
    is DomainError.StepSourceFailed -> UiText.Res(R.string.error_sync_failed)
    is DomainError.Storage -> UiText.Res(R.string.error_storage)
    is DomainError.InsufficientBlueprints -> UiText.Res(R.string.error_blueprints, listOf(needed - balance))
    is DomainError.ItemLocked -> UiText.Res(R.string.error_locked)
    is DomainError.Billing -> when (reason) {
        BillingFailure.UserCancelled -> UiText.Res(R.string.error_billing_cancelled)
        BillingFailure.Pending -> UiText.Res(R.string.error_billing_pending)
        BillingFailure.AlreadyOwned -> UiText.Res(R.string.error_billing_owned)
        else -> UiText.Res(R.string.error_billing)
    }
    is DomainError.AdUnavailable -> when (reason) {
        AdFailure.Dismissed -> UiText.Res(R.string.error_ad_dismissed)
        AdFailure.NoConsent -> UiText.Res(R.string.error_ad_consent)
        else -> UiText.Res(R.string.error_ad)
    }
    DomainError.AlreadyClaimed -> UiText.Res(R.string.error_claimed)
}
