package ai.idraak.stepskyscraper

import ai.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import ai.idraak.stepskyscraper.core.domain.model.Appearance
import ai.idraak.stepskyscraper.core.domain.model.UiHost
import ai.idraak.stepskyscraper.core.domain.model.UserSettings
import ai.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import ai.idraak.stepskyscraper.core.domain.repository.LadderRepository
import ai.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import ai.idraak.stepskyscraper.core.steps.HealthConnectStepSource
import ai.idraak.stepskyscraper.core.steps.SensorStepSource
import ai.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import ai.idraak.stepskyscraper.core.ui.platform.LocalUiHost
import ai.idraak.stepskyscraper.feature.me.PrivacyRoute
import ai.idraak.stepskyscraper.navigation.StepSkyscraperApp
import ai.idraak.stepskyscraper.platform.AndroidPlatformActions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    entitlements: EntitlementRepository,
    ladder: LadderRepository,
) : ViewModel() {
    /** null until DataStore has been read (the splash screen stays up meanwhile). */
    val settings: StateFlow<UserSettings?> = settings.settings.map<UserSettings, UserSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { entitlements.refresh() }
        viewModelScope.launch { ladder.refresh() }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), UiHost {
    @Inject lateinit var healthConnect: HealthConnectStepSource

    @Inject lateinit var sensor: SensorStepSource

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }
        val platform = AndroidPlatformActions(this, healthConnect, sensor)
        // Health Connect's "privacy policy" link (Android 13 and lower) and permission-usage screen (Android 14+)
        // open Help & privacy on its own: the in-app privacy summary plus the hosted policy link.
        val privacyRationale = intent?.action in PRIVACY_RATIONALE_ACTIONS
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val dark = when (current.appearance) {
                Appearance.FollowSystem -> isSystemInDarkTheme()
                Appearance.Light -> false
                Appearance.Dark -> true
            }
            SsTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalPlatformActions provides platform, LocalUiHost provides this) {
                    if (privacyRationale) {
                        PrivacyRoute(onBack = ::finish, onStepHelp = null)
                    } else {
                        StepSkyscraperApp(onboardingComplete = current.onboardingComplete)
                    }
                }
            }
        }
    }

    private companion object {
        val PRIVACY_RATIONALE_ACTIONS = setOf(
            "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE",
            "android.intent.action.VIEW_PERMISSION_USAGE",
        )
    }
}
