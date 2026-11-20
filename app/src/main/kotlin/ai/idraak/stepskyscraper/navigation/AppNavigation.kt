package ai.idraak.stepskyscraper.navigation

import ai.idraak.stepskyscraper.BuildConfig
import ai.idraak.stepskyscraper.core.designsystem.component.NavItem
import ai.idraak.stepskyscraper.core.designsystem.component.SsBottomBar
import ai.idraak.stepskyscraper.core.designsystem.icon.Symbol
import ai.idraak.stepskyscraper.core.designsystem.theme.SsMotion
import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.ui.R as UiR
import ai.idraak.stepskyscraper.feature.build.BuildRoute
import ai.idraak.stepskyscraper.feature.build.ceremony.CeremonyRoute
import ai.idraak.stepskyscraper.feature.build.next.NextTowerRoute
import ai.idraak.stepskyscraper.feature.customise.CustomiseRoute
import ai.idraak.stepskyscraper.feature.me.AchievementsRoute
import ai.idraak.stepskyscraper.feature.me.GoalsRoute
import ai.idraak.stepskyscraper.feature.me.MeNavigation
import ai.idraak.stepskyscraper.feature.me.MeRoute
import ai.idraak.stepskyscraper.feature.me.PrivacyRoute
import ai.idraak.stepskyscraper.feature.me.PrivacyScreen
import ai.idraak.stepskyscraper.feature.me.SettingsRoute
import ai.idraak.stepskyscraper.feature.me.StepHelpRoute
import ai.idraak.stepskyscraper.feature.onboarding.OnboardingRoute
import ai.idraak.stepskyscraper.feature.paywall.PaywallRoute
import ai.idraak.stepskyscraper.feature.share.ShareRoute
import ai.idraak.stepskyscraper.feature.skyline.SkylineRoute
import ai.idraak.stepskyscraper.gallery.ComponentGallery
import ai.idraak.stepskyscraper.gallery.GalleryRoute
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * The only place that knows every feature: features expose routes + stateless callbacks and never depend
 * on each other. Tabs keep their state across switches (save/restore state).
 */
@Composable
fun StepSkyscraperApp(onboardingComplete: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tabs = listOf(
        NavItem("build", stringResource(UiR.string.nav_build), Symbol.Apartment),
        NavItem("skyline", stringResource(UiR.string.nav_skyline), Symbol.LocationCity),
        NavItem("me", stringResource(UiR.string.nav_me), Symbol.Person),
    )
    val selected = when {
        destination?.hasRoute(BuildRoute::class) == true -> "build"
        destination?.hasRoute(SkylineRoute::class) == true -> "skyline"
        destination?.hasRoute(MeRoute::class) == true -> "me"
        else -> null
    }
    Scaffold(
        containerColor = SsTheme.colors.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
            if (selected != null) {
                SsBottomBar(tabs, selected, { key ->
                    val route: Any = when (key) {
                        "skyline" -> SkylineRoute()
                        "me" -> MeRoute
                        else -> BuildRoute
                    }
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            AppNavHost(nav, onboardingComplete)
        }
    }
}

private fun NavHostController.goToTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = false }
    launchSingleTop = true
}

@Composable
private fun AppNavHost(nav: NavHostController, onboardingComplete: Boolean) {
    val reduce = SsTheme.reduceMotion
    val fade = tween<Float>(if (reduce) SsMotion.CROSSFADE_MS else 220)
    NavHost(
        navController = nav,
        startDestination = if (onboardingComplete) BuildRoute else OnboardingRoute,
        enterTransition = { fadeIn(fade) },
        exitTransition = { fadeOut(fade) },
    ) {
        composable<OnboardingRoute> {
            OnboardingRoute(onFinished = {
                nav.navigate(BuildRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            })
        }
        composable<BuildRoute> {
            BuildRoute(
                onOpenCustomise = { nav.navigate(CustomiseRoute(it)) },
                onOpenShare = { nav.navigate(ShareRoute(it)) },
                onOpenWeek = { nav.navigate(GoalsRoute) },
                onOpenStepHelp = { nav.navigate(StepHelpRoute) },
                onOpenCeremony = { nav.navigate(CeremonyRoute(it)) },
            )
        }
        composable<CeremonyRoute>(
            enterTransition = { if (reduce) fadeIn(fade) else fadeIn(tween(400)) },
            exitTransition = { fadeOut(fade) },
        ) {
            CeremonyRoute(
                onShare = { nav.navigate(ShareRoute(it)) },
                onStartNext = { paywall ->
                    nav.navigate(NextTowerRoute) { popUpTo<CeremonyRoute> { inclusive = true } }
                    if (paywall) nav.navigate(PaywallRoute("first_tower"))
                },
                onClose = { index, paywall ->
                    nav.goToTab(SkylineRoute(index))
                    if (paywall) nav.navigate(PaywallRoute("first_tower"))
                },
            )
        }
        composable<NextTowerRoute> { NextTowerRoute(onBack = { nav.popBackStack() }, onDone = { nav.goToTab(BuildRoute) }) }
        composable<SkylineRoute> {
            SkylineRoute(onShare = { nav.navigate(ShareRoute(it)) }, onOpenBuild = { nav.goToTab(BuildRoute) })
        }
        composable<MeRoute> {
            MeRoute(
                MeNavigation(
                    goals = { nav.navigate(GoalsRoute) },
                    achievements = { nav.navigate(AchievementsRoute) },
                    stepSource = { nav.navigate(StepHelpRoute) },
                    settings = { nav.navigate(SettingsRoute) },
                    help = { nav.navigate(PrivacyRoute) },
                    paywall = { nav.navigate(PaywallRoute("me")) },
                ),
            )
        }
        composable<CustomiseRoute>(
            enterTransition = {
                if (reduce) fadeIn(fade) else slideInVertically(tween(SsMotion.SHEET_MS, easing = SsMotion.EmphasizedDecelerate)) { it }
            },
            exitTransition = { ExitTransition.None },
            popExitTransition = { if (reduce) fadeOut(fade) else slideOutVertically(tween(SsMotion.SHEET_MS)) { it } },
            popEnterTransition = { EnterTransition.None },
        ) {
            CustomiseRoute(onClose = { nav.popBackStack() }, onOpenPaywall = { nav.navigate(PaywallRoute("premium_tile")) })
        }
        composable<ShareRoute> { ShareRoute(onClose = { nav.popBackStack() }) }
        composable<PaywallRoute> { PaywallRoute(onClose = { nav.popBackStack() }) }
        composable<GoalsRoute> { GoalsRoute(onBack = { nav.popBackStack() }) }
        composable<AchievementsRoute> { AchievementsRoute(onBack = { nav.popBackStack() }) }
        composable<StepHelpRoute> { StepHelpRoute(onBack = { nav.popBackStack() }) }
        composable<SettingsRoute> {
            SettingsRoute(
                onBack = { nav.popBackStack() },
                versionName = BuildConfig.VERSION_NAME,
                onOpenGallery = if (BuildConfig.DEBUG) ({ nav.navigate(GalleryRoute) }) else null,
            )
        }
        composable<PrivacyRoute> { PrivacyScreen(onBack = { nav.popBackStack() }, onStepHelp = { nav.navigate(StepHelpRoute) }) }
        composable<GalleryRoute> { ComponentGallery(onBack = { nav.popBackStack() }) }
    }
}
