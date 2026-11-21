package ai.idraak.stepskyscraper.core.domain.model

import java.time.LocalDate

enum class Appearance { FollowSystem, Light, Dark }

/** User-level settings persisted in DataStore. */
data class UserSettings(
    val onboardingComplete: Boolean,
    /** First day whose steps build floors (install / onboarding day). Earlier days are history only. */
    val buildStartDate: LocalDate?,
    val goals: Goals,
    val appearance: Appearance,
    val notifyToppingOut: Boolean,
    val notifyWeeklySummary: Boolean,
    val historyImported: Boolean,
    val paywallShownAfterFirstTower: Boolean,
    val widgetPromptHandled: Boolean,
    val stepsConnectSkipped: Boolean,
    val shareShowStats: Boolean,
    val ceremonySound: Boolean,
    /** The one-time POST_NOTIFICATIONS prompt after the first topping-out has been shown (or wasn't needed). */
    val notificationPromptHandled: Boolean = false,
) {
    companion object {
        val Default = UserSettings(
            onboardingComplete = false,
            buildStartDate = null,
            goals = Goals(TargetPreset.Steady.dailySteps, TargetPreset.Steady, GoalPause.None),
            appearance = Appearance.FollowSystem,
            notifyToppingOut = true,
            notifyWeeklySummary = true,
            historyImported = false,
            paywallShownAfterFirstTower = false,
            widgetPromptHandled = false,
            stepsConnectSkipped = false,
            shareShowStats = true,
            ceremonySound = true,
        )
    }
}
