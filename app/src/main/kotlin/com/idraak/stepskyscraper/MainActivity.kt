package com.idraak.stepskyscraper

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
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme
import com.idraak.stepskyscraper.core.domain.model.Appearance
import com.idraak.stepskyscraper.core.domain.model.UiHost
import com.idraak.stepskyscraper.core.domain.model.UserSettings
import com.idraak.stepskyscraper.core.domain.repository.EntitlementRepository
import com.idraak.stepskyscraper.core.domain.repository.LadderRepository
import com.idraak.stepskyscraper.core.domain.repository.SettingsRepository
import com.idraak.stepskyscraper.core.steps.HealthConnectStepSource
import com.idraak.stepskyscraper.core.steps.SensorStepSource
import com.idraak.stepskyscraper.core.ui.platform.LocalPlatformActions
import com.idraak.stepskyscraper.core.ui.platform.LocalUiHost
import com.idraak.stepskyscraper.navigation.StepSkyscraperApp
import com.idraak.stepskyscraper.platform.AndroidPlatformActions
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
                    StepSkyscraperApp(onboardingComplete = current.onboardingComplete)
                }
            }
        }
    }
}
