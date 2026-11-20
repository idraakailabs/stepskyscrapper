package com.idraak.stepskyscraper.core.domain.model

/**
 * Anonymous analytics events. By construction none of them can carry a step count, floor count or any
 * health value: parameters are limited to enums and screen names.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, String> = emptyMap()) {
    data class ScreenView(val screen: String) : AnalyticsEvent("screen_view", mapOf("screen_name" to screen))

    data object OnboardingStarted : AnalyticsEvent("onboarding_started")

    data class StepAccess(val granted: Boolean) : AnalyticsEvent("step_access", mapOf("granted" to granted.toString()))

    data object OnboardingCompleted : AnalyticsEvent("onboarding_completed")

    data class TargetChosen(val preset: TargetPreset) : AnalyticsEvent("target_chosen", mapOf("preset" to preset.name))

    data object WidgetPinRequested : AnalyticsEvent("widget_pin_requested")

    data object ToppingOutShown : AnalyticsEvent("topping_out_shown")

    data class FacadeChosen(val kit: FacadeKit) : AnalyticsEvent("facade_chosen", mapOf("kit" to kit.name))

    data class CosmeticUnlocked(val category: String) : AnalyticsEvent("cosmetic_unlocked", mapOf("category" to category))

    data class ShareCard(val subject: String, val format: String) :
        AnalyticsEvent("share_card", mapOf("subject" to subject, "format" to format))

    data class PaywallShown(val source: String) : AnalyticsEvent("paywall_shown", mapOf("source" to source))

    data class PurchaseStarted(val plan: PassPlan) : AnalyticsEvent("purchase_started", mapOf("plan" to plan.name))

    data class PurchaseCompleted(val plan: PassPlan) : AnalyticsEvent("purchase_completed", mapOf("plan" to plan.name))

    data object RewardedAdOffered : AnalyticsEvent("rewarded_ad_offered")

    data object RewardedAdCompleted : AnalyticsEvent("rewarded_ad_completed")

    data object GoalsPaused : AnalyticsEvent("goals_paused")

    data object StepHelpOpened : AnalyticsEvent("step_help_opened")
}
