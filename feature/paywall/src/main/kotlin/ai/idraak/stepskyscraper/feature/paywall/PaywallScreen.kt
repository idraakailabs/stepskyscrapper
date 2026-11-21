package ai.idraak.stepskyscraper.feature.paywall

import ai.idraak.stepskyscraper.core.designsystem.component.SsBadge
import ai.idraak.stepskyscraper.core.designsystem.component.SsIconButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsMessageState
import ai.idraak.stepskyscraper.core.designsystem.component.SsPrimaryButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsRadioCard
import ai.idraak.stepskyscraper.core.designsystem.component.SsTextButton
import ai.idraak.stepskyscraper.core.designsystem.component.SsTonalButton
import ai.idraak.stepskyscraper.core.designsystem.component.cssLineBox
import ai.idraak.stepskyscraper.core.designsystem.icon.IconSize
import ai.idraak.stepskyscraper.core.designsystem.icon.SsIcon
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SkyTokens
import ai.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import ai.idraak.stepskyscraper.core.designsystem.theme.SsSpacing
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.designsystem.tower.Facade
import ai.idraak.stepskyscraper.core.designsystem.tower.GroundStyle
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerAnimation
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerCanvas
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerSpec
import ai.idraak.stepskyscraper.core.designsystem.tower.TowerTime
import ai.idraak.stepskyscraper.core.domain.AppClock
import ai.idraak.stepskyscraper.core.domain.model.AnalyticsEvent
import ai.idraak.stepskyscraper.core.domain.model.BillingFailure
import ai.idraak.stepskyscraper.core.domain.model.DomainError
import ai.idraak.stepskyscraper.core.domain.model.Outcome
import ai.idraak.stepskyscraper.core.domain.model.PassPlan
import ai.idraak.stepskyscraper.core.domain.model.PlanOffer
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.repository.AnalyticsTracker
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import ai.idraak.stepskyscraper.core.ui.component.SystemBarsEffect
import ai.idraak.stepskyscraper.core.ui.format.Formats
import ai.idraak.stepskyscraper.core.ui.mvi.MviViewModel
import ai.idraak.stepskyscraper.core.ui.mvi.UiText
import ai.idraak.stepskyscraper.core.ui.mvi.toUiText
import ai.idraak.stepskyscraper.core.ui.platform.LocalUiHost
import ai.idraak.stepskyscraper.core.ui.preview.PreviewSurface
import ai.idraak.stepskyscraper.core.ui.preview.ScreenPreviews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class PaywallRoute(val source: String = "me")

@Immutable
data class PaywallUiState(
    val offers: List<PlanOffer> = emptyList(),
    val loading: Boolean = true,
    val selected: PassPlan = PassPlan.Annual,
    val premium: Boolean = false,
    val busy: Boolean = false,
    val message: UiText? = null,
    val today: LocalDate = LocalDate.now(),
) {
    fun offer(plan: PassPlan) = offers.firstOrNull { it.plan == plan }
}

sealed interface PaywallIntent {
    data class Select(val plan: PassPlan) : PaywallIntent

    data class Purchase(val host: UiHost) : PaywallIntent

    data object Restore : PaywallIntent

    data object Retry : PaywallIntent
}

object PaywallReducer {
    /** "Save 58%": annual price versus twelve monthly payments. */
    fun savingPercent(monthly: PlanOffer?, annual: PlanOffer?): Int? {
        if (monthly == null || annual == null || monthly.priceMicros == 0L) return null
        val saving = 1.0 - annual.priceMicros.toDouble() / (monthly.priceMicros * 12)
        return (saving * 100).roundToInt().takeIf { it > 0 }
    }

    fun perMonth(annual: PlanOffer): String = runCatching {
        NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(annual.currencyCode) }
            .format(annual.priceMicros / 1_000_000.0 / 12)
    }.getOrDefault("")
}

/** Architect Pass paywall: once after the first topping-out, otherwise only from Me, Premium tiles or Restore. */
@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val entitlements: EntitlementRepository,
    private val analytics: AnalyticsTracker,
    clock: AppClock,
) : MviViewModel<PaywallUiState, PaywallIntent, Unit>(PaywallUiState(today = clock.today())) {
    init {
        analytics.track(AnalyticsEvent.PaywallShown("paywall"))
        viewModelScope.launch {
            combine(entitlements.offers, entitlements.entitlement) { o, e -> o to e }.collect { (offers, e) ->
                reduce { it.copy(offers = offers, premium = e.isPremium, loading = it.loading && offers.isEmpty()) }
            }
        }
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        reduce { it.copy(loading = true, message = null) }
        val r = entitlements.refresh()
        reduce { it.copy(loading = false, message = (r as? Outcome.Failure)?.error?.toUiText()) }
    }

    override fun onIntent(intent: PaywallIntent) {
        when (intent) {
            is PaywallIntent.Select -> reduce { it.copy(selected = intent.plan) }
            PaywallIntent.Retry -> refresh()
            PaywallIntent.Restore -> viewModelScope.launch {
                reduce { it.copy(busy = true) }
                val r = entitlements.restore()
                reduce { it.copy(busy = false, message = (r as? Outcome.Failure)?.error?.toUiText()) }
            }
            is PaywallIntent.Purchase -> viewModelScope.launch {
                val plan = currentState.selected
                reduce { it.copy(busy = true, message = null) }
                analytics.track(AnalyticsEvent.PurchaseStarted(plan))
                val r = entitlements.purchase(plan, intent.host)
                if (r is Outcome.Success) {
                    analytics.track(AnalyticsEvent.PurchaseCompleted(plan))
                    emit(Unit)
                }
                val cancelled = (r as? Outcome.Failure)?.error.let { it is DomainError.Billing && it.reason == BillingFailure.UserCancelled }
                reduce { it.copy(busy = false, message = if (r is Outcome.Failure && !cancelled) r.error.toUiText() else null) }
            }
        }
    }
}

@Composable
fun PaywallRoute(onClose: () -> Unit, viewModel: PaywallViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { onClose() } }
    PaywallScreen(state, onClose, viewModel::onIntent)
}

@Composable
fun PaywallScreen(state: PaywallUiState, onClose: () -> Unit, onIntent: (PaywallIntent) -> Unit) {
    val c = SsTheme.colors
    val host = LocalUiHost.current
    SystemBarsEffect(lightIcons = c.isDark)
    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = SsSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            SsIconButton(Symbol.Close, stringResource(R.string.pw_close), onClose)
            Box(Modifier.weight(1f))
            SsTextButton(stringResource(R.string.pw_restore), { onIntent(PaywallIntent.Restore) }, enabled = !state.busy)
        }
        if (state.premium) {
            SsMessageState(
                Symbol.WorkspacePremium,
                stringResource(R.string.pw_owned_title),
                stringResource(R.string.pw_owned_body),
                action = { SsTonalButton(stringResource(R.string.pw_done), onClose) },
            )
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Hero()
            Column(
                Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.pw_title).uppercase(),
                    style = SsTheme.type.displayTitle,
                    color = c.onSurface,
                    modifier = Modifier.cssLineBox(SsTheme.type.displayTitle),
                )
                Text(stringResource(R.string.pw_body), style = SsTheme.type.bodyM, color = c.bodySecondary)
            }
            Column(
                Modifier.padding(start = SsSpacing.onboardingMargin, end = SsSpacing.onboardingMargin, top = SsSpacing.m),
                verticalArrangement = Arrangement.spacedBy(SsSpacing.s),
            ) {
                listOf(R.string.pw_benefit_1, R.string.pw_benefit_2, R.string.pw_benefit_3).forEach {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SsIcon(Symbol.Check, size = IconSize.m, tint = c.onSurface)
                        Text(stringResource(it), style = SsTheme.type.bodyS, color = c.onSurface)
                    }
                }
            }
            Column(
                Modifier.padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, top = SsSpacing.l),
                verticalArrangement = Arrangement.spacedBy(SsSpacing.s),
            ) {
                when {
                    state.offers.isEmpty() && state.loading -> Text(
                        stringResource(R.string.pw_loading),
                        style = SsTheme.type.bodyS,
                        color = c.onSurfaceVariant,
                    )
                    state.offers.isEmpty() -> {
                        Text(stringResource(R.string.pw_unavailable), style = SsTheme.type.bodyS, color = c.onSurfaceVariant)
                        SsTonalButton(stringResource(R.string.pw_retry), { onIntent(PaywallIntent.Retry) })
                    }
                    else -> Plans(state, onIntent)
                }
                state.message?.let { Text(it.resolve(), style = SsTheme.type.bodyXS, color = c.onSurfaceVariant) }
            }
        }
        val offer = state.offer(state.selected)
        Column(
            Modifier.windowInsetsPadding(
                WindowInsets.navigationBars,
            ).padding(start = SsSpacing.screenMargin, end = SsSpacing.screenMargin, bottom = 28.dp, top = SsSpacing.s),
            verticalArrangement = Arrangement.spacedBy(SsSpacing.s),
        ) {
            val cta = when {
                offer == null -> stringResource(R.string.pw_title)
                offer.plan == PassPlan.Annual && offer.freeTrialDays > 0 -> stringResource(R.string.pw_cta_trial, offer.freeTrialDays)
                offer.plan == PassPlan.Annual -> stringResource(R.string.pw_cta_yearly, offer.formattedPrice)
                offer.plan == PassPlan.Monthly -> stringResource(R.string.pw_cta_monthly, offer.formattedPrice)
                else -> stringResource(R.string.pw_cta_lifetime, offer.formattedPrice)
            }
            SsPrimaryButton(
                cta,
                { onIntent(PaywallIntent.Purchase(host)) },
                Modifier.fillMaxWidth(),
                enabled = offer != null && !state.busy,
            )
            if (offer != null) {
                val legal = when {
                    offer.plan == PassPlan.Annual && offer.freeTrialDays > 0 ->
                        stringResource(
                            R.string.pw_legal_trial,
                            Formats.trialEnd(state.today.plusDays(offer.freeTrialDays.toLong())),
                            offer.formattedPrice,
                        )
                    offer.plan == PassPlan.Annual -> stringResource(R.string.pw_legal_yearly, offer.formattedPrice)
                    offer.plan == PassPlan.Monthly -> stringResource(R.string.pw_legal_monthly, offer.formattedPrice)
                    else -> stringResource(R.string.pw_legal_lifetime, offer.formattedPrice)
                }
                Text(
                    legal,
                    style = SsTheme.type.legal,
                    color = c.bodySecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Hero() {
    Box(
        Modifier
            .padding(horizontal = SsSpacing.screenMargin)
            .fillMaxWidth()
            .height(150.dp)
            .clip(SsRadius.shapeLg)
            .background(SkyTokens.PaywallDusk.brush()),
    ) {
        val desc = stringResource(R.string.pw_hero)
        TowerCanvas(
            TowerSpec(Facade.Glass, 10, 10, time = TowerTime.Dusk, ground = GroundStyle.None),
            desc,
            Modifier.offset(28.dp, 14.dp).size(80.dp, 132.dp),
            animation = TowerAnimation.Lights,
        )
        TowerCanvas(
            TowerSpec(Facade.Deco, 14, 14, time = TowerTime.Dusk, ground = GroundStyle.None),
            desc,
            Modifier.offset(120.dp, 6.dp).size(90.dp, 140.dp),
        )
        TowerCanvas(
            TowerSpec(Facade.Eco, 7, 7, time = TowerTime.Dusk, ground = GroundStyle.None),
            desc,
            Modifier.offset(220.dp, 30.dp).size(80.dp, 116.dp),
        )
    }
}

@Composable
private fun Plans(state: PaywallUiState, onIntent: (PaywallIntent) -> Unit) {
    val annual = state.offer(PassPlan.Annual)
    val monthly = state.offer(PassPlan.Monthly)
    val lifetime = state.offer(PassPlan.Lifetime)
    val pad = PaddingValues(horizontal = SsSpacing.l, vertical = SsSpacing.m)
    val title = SsTheme.type.cardTitleS
    val sub = SsTheme.type.cardCaption
    annual?.let { a ->
        val save = PaywallReducer.savingPercent(monthly, a)
        SsRadioCard(
            stringResource(R.string.pw_yearly, a.formattedPrice),
            if (a.freeTrialDays >
                0
            ) {
                stringResource(R.string.pw_yearly_sub_trial, a.freeTrialDays, PaywallReducer.perMonth(a))
            } else {
                stringResource(R.string.pw_yearly_sub, PaywallReducer.perMonth(a))
            },
            state.selected == PassPlan.Annual, { onIntent(PaywallIntent.Select(PassPlan.Annual)) },
            radius = SsRadius.option, padding = pad, titleStyle = title, subtitleStyle = sub, textGap = SsSpacing.none, subtitleColor = SsTheme.colors.bodySecondary,
            trailing = save?.let { { SsBadge(stringResource(R.string.pw_save, it)) } },
        )
    }
    monthly?.let { m ->
        SsRadioCard(
            stringResource(R.string.pw_monthly, m.formattedPrice), stringResource(R.string.pw_monthly_sub),
            state.selected == PassPlan.Monthly, { onIntent(PaywallIntent.Select(PassPlan.Monthly)) },
            radius = SsRadius.option, padding = pad, titleStyle = title, subtitleStyle = sub, textGap = SsSpacing.none, subtitleColor = SsTheme.colors.bodySecondary,
        )
    }
    lifetime?.let { l ->
        SsRadioCard(
            stringResource(R.string.pw_lifetime, l.formattedPrice), stringResource(R.string.pw_lifetime_sub),
            state.selected == PassPlan.Lifetime, { onIntent(PaywallIntent.Select(PassPlan.Lifetime)) },
            radius = SsRadius.option, padding = pad, titleStyle = title, subtitleStyle = sub, textGap = SsSpacing.none, subtitleColor = SsTheme.colors.bodySecondary,
        )
    }
}

@ScreenPreviews
@Composable
private fun PaywallPreview() = PreviewSurface {
    PaywallScreen(
        PaywallUiState(
            offers = listOf(
                PlanOffer(PassPlan.Annual, "$14.99", 14_990_000, "USD", 7),
                PlanOffer(PassPlan.Monthly, "$2.99", 2_990_000, "USD", 0),
                PlanOffer(PassPlan.Lifetime, "$29.99", 29_990_000, "USD", 0),
            ),
            loading = false,
            today = LocalDate.of(2026, 11, 20),
        ),
        {},
        {},
    )
}
