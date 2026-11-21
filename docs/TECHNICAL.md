# Step Skyscraper — Technical Reference

> **Scope:** how the system is built today (the Phase 1 baseline), important technical decisions, and the
> index of technical concerns. Defects: [BUGS.md](BUGS.md) · Planned work: [ROADMAP.md](ROADMAP.md) ·
> Product: [PRODUCT.md](PRODUCT.md) · Design deviations: [design-deviations.md](design-deviations.md) ·
> Release checklist: [play-console-checklist.md](play-console-checklist.md) · Release status:
> [RELEASE-READINESS.md](RELEASE-READINESS.md).
>
> **Baseline:** version 1.0.0, commit `e5c003b` on `main`. **Last reviewed:** 2026-11-21 (release-readiness review).

---

## 1. Stack

| Concern | Choice (version, and where it's declared) |
|---|---|
| Language / framework | Kotlin 2.4.21 (AGP 9.2.1 built-in Kotlin; pinned to the newest version Android Studio supports), Jetpack Compose BOM 2026.11.01, Material 3 with a custom theme — `gradle/libs.versions.toml` |
| Platform targets | minSdk 26, compileSdk 37.2, targetSdk 37 |
| DI / composition | Hilt 2.60.1 (KSP 2.3.14) |
| Navigation | Navigation Compose 2.10.2, type-safe `@Serializable` routes, wired in `app/.../navigation/AppNavigation.kt` |
| Persistence | Room 2.8.5 (`core:database`), DataStore Preferences 1.3.0 (`core:datastore`) |
| Step data | Health Connect 1.1.0 aggregate API; `TYPE_STEP_COUNTER` fallback (`core:steps`) |
| External services | RevenueCat Purchases 11.2.0 (on Play Billing 9.1.0), Google Mobile Ads 25.5.0 + UMP 4.0.0, Firebase BOM 35.2.0 (Analytics, Crashlytics, Remote Config) |
| Background work | WorkManager 2.12.0 with Hilt workers |
| Widgets | Glance 1.2.0 (`:widget`) |
| Testing | JUnit 4, Truth, Turbine, Robolectric 4.18.1, Roborazzi 1.79.0, Compose UI test |
| Quality | ktlint-gradle 14.2.0, detekt 1.23.8 (`config/detekt/detekt.yml`, `.editorconfig`, max line 180) |
| Declared but unused | `CrashReporter` (`core/analytics`), Glance `glance-material3` dependency, `GroupAnim.LiftOff` |

Modules (20): `:app`, `build-logic`, `:baselineprofile`, `:core:{designsystem, ui, domain, data, database,
datastore, steps, billing, ads, analytics, testing}`, `:feature:{onboarding, build, skyline, customise, me,
share, paywall}`, `:widget`. About 15.9k lines of Kotlin (largest: `core:designsystem` 3.5k, `core:domain` 2.1k).

## 2. Architecture

```
app            MainActivity (UiHost), navigation, AndroidPlatformActions, component gallery
feature/*      Route (stateful) + Screen (stateless) + MVI ViewModel per screen; depend on domain + ui only
core:ui        MviViewModel, Lce, UiText, formatters, PlatformActions interface, domain → renderer mapping
core:designsystem  tokens, fonts, icons, components, tower renderer (port of design/source/tower.js)
core:domain    pure Kotlin: models, repository interfaces, use cases (no Android imports)
core:data      repository implementations, Remote Config ladder, WorkManager sync, notifications, Hilt bindings
core:database / datastore / steps / billing / ads / analytics   data sources behind domain interfaces
widget         Glance widgets + SyncListener that refreshes them
```

- **Pattern:** MVI. Each ViewModel exposes `StateFlow<UiState>`, a single `onIntent()`, and one-off effects
  over a `Channel` (`core/ui/.../mvi/MviViewModel.kt`). Decision logic lives in pure reducer objects
  (`BuildReducer`, `OnboardingReducer`, …) that are unit-tested.
- **Errors:** typed `Outcome<T>` / `DomainError` (`core/domain/.../model/Outcome.kt`), mapped to calm copy by
  `DomainError.toUiText()`. No red/error colour token.
- **Dependency rule:** features never depend on each other or on data modules; navigation between features
  happens in `:app`. System integrations go through `PlatformActions` (`core/ui/.../platform/Platform.kt`).
- **Gating:** `MainActivity` keeps the splash until settings load; start destination is Onboarding until
  `onboardingComplete`.

**Follow this pattern for new code.**

## 3. Key data flows

```
Sync (app open, pull-to-refresh, WorkManager every 15–30 min)
  SyncStepsUseCase → StepSource.status()                       // typed error if no source/permission
    → readDailyTotals(30 days first time, else 7)              // HC aggregateGroupByPeriod, 1-day slices
    → never lower a stored day; StepsRepository.upsertDays     // days older than 7 days frozen (StepsDao.applySync)
    → record floor events for today's new floors               // used by time-of-day achievements
    → expire goal pause, EnsureTowerRecords, CompleteTower (Blueprints, keyed), AwardAchievements
    → SyncListeners: ProgressNotifier, WidgetSyncListener

City (every screen)
  GetTowerProgressUseCase = combine(days, floor events, tower records, settings, ladder, achievements, wallet, entitlement)
    → CityCalculator: floors = steps / 1000 per day, flowing into towers in order (overflow to next tower)
    → CitySnapshot (today, week, stats, level, achievements, Blueprints)

Rewarded ad (core:ads, every SDK call behind AdsGate; see §5.1)
  Skyline reward card visible → RewardedAdGateway.prepare        // consent status refresh only, no UI; preload if consent allows
  tap → ClaimRewardedBonusUseCase → show                          // consent form if required → MobileAds.initialize → load → show
      → wallet "tower_ad:N" only when onUserEarnedReward fired     // never grants floors
  ads disabled (AdAvailability.Disabled) → no reward card, no consent request, no SDK init, no ad request
  Help & privacy → AdPrivacyChoices.isRequired → "Ad privacy choices" row → UMP privacy options form
```

## 4. Storage map

| Store | Location | Contents | Survives restart? |
|---|---|---|---|
| Room `step_skyscraper.db` | `core/database` | `day_steps` (totals), `floor_events`, `towers` (name, look, frozen target, ceremony/ad flags), `wallet` (keyed Blueprint ledger), `unlocks`, `achievements`, `counters` | Yes; Android backup |
| DataStore `user_prefs` | `core/datastore/.../PreferenceStores.kt` | Onboarding, start date, goals, pause, appearance, notifications (+ `notify_prompt_handled`, the one-time permission prompt), sync timestamps, sensor baseline, entitlement cache | Yes; Android backup |
| In memory | `PreferencesSyncStatusRepository.running` | "Syncing…" flag | No (by design) |
| Cache dir `share/` | `AndroidPlatformActions.shareImage` | Last exported share PNG | Until cleared |

Backup rules: `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`. Schema export:
`core/database/schemas/`.

## 5. Configuration and flags

| Key / variable | Default | Used by |
|---|---|---|
| `ADMOB_APP_ID_PROD` / `ADMOB_REWARDED_UNIT_ID_PROD` (Gradle property or env var, never committed) | absent → `release` has ads disabled; `bundleRelease` fails (`verifyReleaseAdMob`) | `release` variant only: manifest `APPLICATION_ID`, `BuildConfig.ADMOB_LIVE_*` (§5.1) |
| `ADMOB_TEST_DEVICE_ID`, `ADMOB_UMP_DEBUG_EEA` | empty / false | Debug only: UMP debug geography and test device for consent testing; release always empty |
| Remote Config `ads_enabled` | `false` | Prod Release kill switch for the rewarded ad (§5.1); debug ignores it |
| `PRIVACY_POLICY_URL` (`gradle.properties`, public) | `https://idraakai.org/step-skyscraper/privacy/` (blank hides the link and fails `bundleRelease` via `verifyReleasePrivacyPolicy`, needs `https://`) | `AndroidPlatformActions.openPrivacyPolicy` (Help & privacy row and paywall link); draft text in [privacy-policy.md](privacy-policy.md) |
| `TERMS_URL` (`gradle.properties`, public) | `https://idraakai.org/step-skyscraper/terms/`; blank hides the links | `AndroidPlatformActions.openTermsOfService`: Help & privacy row and paywall link |
| `SUPPORT_EMAIL` | empty (generic share sheet) | Diagnostic draft |
| `app/google-services.json` | absent | Applies Google Services + Crashlytics plugins only if present. Firebase project `skyscrapper-a675f`, apps `ai.idraak.stepskyscraper` and `.debug` (both clients in one file) |
| `REVENUECAT_API_KEY` (Gradle property or env var, outside Git; public `goog_…` key) | empty → purchases off (no offers, Restore reports the store unavailable); `bundleRelease` fails (`verifyReleaseRevenueCat`) | `BuildConfig.REVENUECAT_API_KEY` → `BillingEnvironment` → `RevenueCatRepository` (§5.2) |
| Remote Config `tower_ladder` | `10,25,50,100,160,250` (bundled) | `RemoteConfigLadderRepository`; malformed values fall back |
| `keystore.properties` or `STEPSKY_KEYSTORE_FILE` / `STEPSKY_KEYSTORE_PASSWORD` / `STEPSKY_KEY_ALIAS` / `STEPSKY_KEY_PASSWORD` | absent → release **unsigned**, `bundleRelease` fails | `app/build.gradle.kts` (template `keystore.properties.example`) |

Firebase and RevenueCat were configured on 2026-11-21 ([TECH-002](ROADMAP.md#tech-002), [TECH-014](ROADMAP.md#tech-014)); AdMob and Play Console values are still unverified. The old `ADMOB_APP_ID` /
`ADMOB_REWARDED_UNIT_ID` properties were removed and are no longer read.

### 5.1 AdMob and the `ads_enabled` switch

One opt-in rewarded ad (Skyline reward card, doubles a finished tower's Blueprints) with UMP consent. Classes in
`core:ads`: `AdsEnvironment` (from per-variant `BuildConfig`, provided by `app/.../ads/AdsEnvironmentModule.kt`),
`AdsConfigResolver` → `AdSetup.Test` / `Live(unit)` / `NotConfigured`, `AdsGate`, `AdSdk` (`GoogleAdSdk` is the only
class that calls Google Mobile Ads / UMP), `AdMobRewardedGateway`, `AdMobPrivacyChoices`. Logcat tag `StepSkyAds`
(no step or floor values); gate line: `gate mode=<Test|Live|NotConfigured> enabled=<bool> ids=<test|live|none> unit=<id or ->`.

| Variant | Manifest app id | Rewarded unit | `ads_enabled` | Ads |
|---|---|---|---|---|
| `debug` (`.debug`), `benchmarkRelease`, `nonMinifiedRelease` | Google test `ca-app-pub-3940256099942544~3347511713` | Google test `…/5224354917` | Never read | Always on, test ads only |
| `release` with valid live ids (Play build) | `ADMOB_APP_ID_PROD` | `ADMOB_REWARDED_UNIT_ID_PROD` | `true` → on; `false` / unset / never fetched / no Firebase → off | Live ads only while the switch is on |
| `release` without valid live ids (local `assembleRelease`) | Google test (SDK start-up check only) | none | Irrelevant | Off: no consent request, init or ad request; build log `w: AdMob: … ads disabled` |
| `bundleRelease` without valid live ids (missing, malformed or Google sample) | — | — | — | **Build fails** (`verifyReleaseAdMob`) |

Test ids are hard-coded in `app/build.gradle.kts` and `core:ads` `AdMobIds`, never read from properties or Remote Config.

**Remote Config `ads_enabled`:** Boolean, in-app default `false`. Fetched with `tower_ladder` by the single
`RemoteConfigSource` client (`core:data`) at app start (`MainActivity` → `LadderRepository.refresh()`) and in the sync
worker, with a 12-hour minimum fetch interval. The last **activated** value is used, so a `true` from an earlier session
stays in effect offline, and turning ads off in the console reaches a device on its next successful fetch (up to ~12 h
plus the next launch; real-time updates aren't implemented). When the switch turns off, a loaded ad is discarded and
nothing more is requested. Safe default: anything other than a fetched `true` means no ads.

**Policy-relevant behaviour:** consent is gathered only on the user's tap (BUG-007); `MobileAds.initialize` runs on
`Dispatchers.IO` once, after consent; `DELAY_APP_MEASUREMENT_INIT=true`; requests carry no custom targeting; the
reward needs `onUserEarnedReward`; once per finished tower (wallet key `tower_ad:N`), hidden for Architect Pass
members; no preload after a successful claim; a loaded ad expires after 55 minutes; no ads on Build, onboarding, the
ceremony, the paywall or widgets. Help & privacy shows **Ad privacy choices** (UMP `showPrivacyOptionsForm`) while
ads are enabled and UMP reports `PrivacyOptionsRequirementStatus.REQUIRED`.

**Testing ads safely**
- Never tap a live ad or create live impressions on your own devices. Use the debug build: it can only request
  Google's test unit, whatever the properties or Remote Config say (the ad shows a "Test Ad" label).
- To exercise a release build with live ids, first register the phone in AdMob › Settings › Test devices, or keep
  `ads_enabled` false for it (Remote Config condition).
- Consent form in debug: run once, tap the ad button, copy the hashed id from
  `adb logcat | findstr /i "addTestDeviceHashedId"`, then set `ADMOB_TEST_DEVICE_ID=<hash>` and
  `ADMOB_UMP_DEBUG_EEA=true` in `~/.gradle/gradle.properties`, reinstall, and reset consent with
  `adb shell pm clear ai.idraak.stepskyscraper.debug` (also clears app data). The form appears only once a GDPR message
  is published for the AdMob app.
- Watch `adb logcat -s StepSkyAds`. The reward card shows only for a tower topped out in the last 24 h, for non-Pass
  users who haven't claimed it.
- Checks: `./gradlew :app:verifyReleaseAdMob` fails without live ids and with Google's sample ids;
  `./gradlew --dry-run :app:bundleRelease` lists `verifyReleaseAdMob`, `verifyReleasePrivacyPolicy` and `verifyReleaseSigning`.

### 5.2 Purchases (RevenueCat)

Architect Pass goes through RevenueCat (`core:billing` `RevenueCatRepository`, SDK 11.2.0), which runs Google Play
Billing, acknowledges and validates purchases server-side. The domain `EntitlementRepository` is unchanged; the
entitlement is still cached offline in DataStore (`EntitlementCacheStore`) and refreshed at start-up (`MainActivity`)
and on the paywall.

| RevenueCat object | Value |
|---|---|
| Project / app | "SkyScrapper" / Play Store app `ai.idraak.stepskyscraper` |
| Entitlement | `architect_pass` (`PassProducts.ENTITLEMENT`) |
| Products | `architect_pass:monthly`, `architect_pass:annual` (Play subscription `architect_pass`, base plans), `architect_pass_lifetime` (one-time) |
| Offering | `default`: `$rc_monthly`, `$rc_annual`, `$rc_lifetime` → `PassPlan.Monthly/Annual/Lifetime` |
| Test Store | Products `pass_monthly` ($2.99), `pass_yearly` ($14.99, 1-week free trial for new customers), `pass_lifetime` ($29.99, non-consumable), all on entitlement `architect_pass` and in the `default` packages; key `test_…`. Older `monthly` / `yearly` / `lifetime` test products ($9.99 / $79.99 / $99.99, on a stray `create_a_project_called_skyscrapper_pro` entitlement) are no longer offered. Test Store prices can't be edited after creation: to change one, create a new product and swap it into the package |

**Keys per variant (`app/build.gradle.kts` `onVariants`):** `release` gets only `REVENUECAT_API_KEY` (Google Play,
`goog_…`; `verifyReleaseRevenueCat` enforces the prefix). Debug and every other non-release variant get
`REVENUECAT_TEST_API_KEY` (RevenueCat Test Store, `test_…`) when it is set, else the Play key. Both live in
`~/.gradle/gradle.properties` or CI env, never in Git. With the Test Store key the paywall shows the Test Store prices
and purchases are simulated by RevenueCat (sandbox, no Google Play, no revenue); RevenueCat logs "Using a Test Store
API key". The SDK crashes if a Test Store key reaches a production build, which is why `release` never reads it.
`PassProducts.planFor` maps both Play ids and Test Store ids to `PassPlan` (`PassProductsTest`).

The plan comes from the active entitlement's product (`architect_pass_lifetime` → Lifetime, base plan `monthly` →
Monthly, else Annual); the trial length comes from the package's default subscription option. A failed offerings fetch
(e.g. products not yet in Play Console) is logged as `StepSkyBilling: Offerings unavailable` and does not block the
entitlement refresh; offerings and customer info are fetched in parallel, and the last plans are cached in DataStore
(`pass_offers_cache`) so the paywall shows them at once on the next launch ([BUG-021](BUGS.md#bug-021)). Only
RevenueCat's anonymous app user id and purchase data leave the device. Real Google Play purchases
need the release package on a testing track with license testers; the debug build uses the Test Store instead.

## 6. Build, signing and release

- Application id / namespace: `ai.idraak.stepskyscraper` (renamed from `com.idraak.stepskyscraper` on
  2026-11-20, before any store release); library namespaces follow `ai.idraak.stepskyscraper.<module path>`.
- Version: `versionCode = 1`, `versionName = "1.0.0"` (`app/build.gradle.kts` `defaultConfig`); raise `versionCode`
  for every upload. minSdk 26, targetSdk 37 (Play requires API 36+ for new apps and updates since 31 Aug 2026).
- Variants: `debug` (`.debug` suffix) and `release` (R8 minify + resource shrinking, `app/proguard-rules.pro`: keep
  rules for `@Serializable` navigation routes and Glance receivers; `SourceFile,LineNumberTable` kept for Crashlytics).
  With `google-services.json` present the Crashlytics Gradle plugin (3.0.8) uploads the R8 mapping file for minified
  builds by default. Without it, Firebase is skipped (no analytics, crash reports or Remote Config).
- Release guards wired into `bundleRelease`: `verifyReleaseSigning` (upload key), `verifyReleaseAdMob` (live ids,
  §5.1), `verifyReleasePrivacyPolicy` (`PRIVACY_POLICY_URL` starts with `https://`), `verifyReleaseRevenueCat`
  (`REVENUECAT_API_KEY` starts with `goog_`). `assembleRelease` (part of
  `build`) still produces `app-release-unsigned.apk` for smoke tests.
- Building the upload: put `keystore.properties`, `ADMOB_*_PROD`, `REVENUECAT_API_KEY`, `PRIVACY_POLICY_URL` and `app/google-services.json` in
  place, then `./gradlew clean :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`. Step-by-step:
  [play-console-checklist.md §2](play-console-checklist.md#2-build-the-signed-aab).
- Permissions in the merged release manifest (checked with `aapt2 dump permissions`, 2026-11-21):

  | Permission | Source | Why |
  |---|---|---|
  | `health.READ_STEPS`, `health.READ_HEALTH_DATA_IN_BACKGROUND` | app | Daily step totals; background read for widget and periodic sync |
  | `ACTIVITY_RECOGNITION` | app | `TYPE_STEP_COUNTER` fallback without Health Connect |
  | `POST_NOTIFICATIONS` | app | Topping-out and weekly-summary notifications (requested after the first topping-out or from Settings) |
  | `INTERNET`, `ACCESS_NETWORK_STATE` | app | Firebase, Remote Config, ads, billing |
  | `WRITE_EXTERNAL_STORAGE` (maxSdk 28) | app | Save share image on Android 8–9 |
  | `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID` / `_ATTRIBUTION` / `_TOPICS` | Google Mobile Ads | Rewarded ad (declare "uses advertising ID" in Play Console) |
  | `com.android.vending.BILLING` | Play Billing | Architect Pass |
  | `BIND_GET_INSTALL_REFERRER_SERVICE` | Firebase Analytics / Ads | Install attribution |
  | `WAKE_LOCK` | WorkManager, Firebase | Background sync and measurement |
  | `<package>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX core | Signature-level, internal receivers |

  `FOREGROUND_SERVICE` (merged from WorkManager) is removed with `tools:node="remove"`: the app never runs foreground
  work. No exact-alarm, location or foreground-service-type permissions are declared.
- Health Connect privacy rationale: `ACTION_SHOW_PERMISSIONS_RATIONALE` (intent filter on `MainActivity`) and the
  Android 14 `ViewPermissionUsageActivity` alias (`VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS`, guarded by
  `START_VIEW_PERMISSION_USAGE`) open Help & privacy as a standalone screen (`MainActivity` checks `intent.action`).
- Backup: `allowBackup=true`; `backup_rules.xml` (Android 11 and lower) and `data_extraction_rules.xml` (12+: cloud
  backup and device transfer) include only `step_skyscraper.db` and `datastore/user_prefs.preferences_pb`.
- Signing: upload key from `keystore.properties` (git-ignored, `storeFile` relative to the root, e.g. under the
  ignored `keystore/` folder), else the `STEPSKY_*` keys in `local.properties` (git-ignored; how the dev machine is set
  up: alias `stepskyscrapper` in the shared Idraak keystore), else the `STEPSKY_*` environment variables in CI. Never the debug key: without a key
  the release APK is unsigned and `bundleRelease` stops at `verifyReleaseSigning`.
- Baseline profile: hand-seeded `app/src/main/baseline-prof.txt`; generator in `:baselineprofile` needs a
  device ([TECH-006](ROADMAP.md#tech-006)).
- Git: `develop` is the integration branch and `main` is release-only (merges are the user's call); remote
  `origin` = `github.com/idraakailabs/stepskyscrapper`; baseline commit `e5c003b`. `.gitattributes` pins LF line
  endings. No CI configuration yet.

### Debug-only device tools

- **Clock shift** (debug builds only): `SystemAppClock` adds an optional `DebugClockShift`, bound only in
  `app/src/debug` (`@BindsOptionalOf` in `DataModule`; release binds nothing). Set a time of day to check the
  dawn / dusk / night skies without changing the phone's clock:
  `adb shell am broadcast -n ai.idraak.stepskyscraper.debug/ai.idraak.stepskyscraper.debug.DebugClockReceiver --es time 18:30`
  (`--es time 2026-11-22T10:00` for another day, `--es time off` to clear).
- **Seeding data:** pull `databases/step_skyscraper.db` with `adb exec-out run-as ai.idraak.stepskyscraper.debug cat …`,
  edit it with SQLite, push it back with `run-as` while the app is stopped. A sync never lowers a stored day
  total, so seeded totals survive the next Health Connect read.

### Verification commands

Run on 2026-11-20 with JDK 21: `build test lint` passed; the release APK (6.3 MB) launched on an Android 16
emulator, through onboarding with the real Health Connect sheet, to Build and Me. 2026-11-21 (integration, Pixel 6a,
Android 16): the R8 release APK (throwaway signing key, ads `NotConfigured`) opened onboarding, Build, Skyline,
Customise, Me and Share with no crash, and made no consent, SDK-init or ad request. 2026-11-21 (release-readiness
review): `ktlintFormat build test lint` green after the review fixes.

```powershell
./gradlew build test lint
```

```powershell
./gradlew ktlintFormat
```

```powershell
./gradlew recordRoborazziDebug
```

### Tests

84 tests: domain logic (floor maths, ladder, city derivation, weekly goal, adaptive target, levels, sky clock,
achievements, cosmetics, step-counter reboot handling), Room DAOs, sync use case, ViewModel reducers, Compose UI
tests for Onboarding and Build, and Roborazzi screenshots (tower renderer, key screens, gallery). Since then:
ad gating (`AdsGatingTest`, `AdMobPrivacyChoicesTest` in `core:ads`), `RemoteConfigAdsSwitchTest`,
`SkylineRewardGatingTest`, `CeremonyViewModelTest`, `ShareSaveFeedbackTest`, and more `MeTest` / `SkylineTest` cases.
Gaps: no tests for `core:steps`, `core:billing`, `widget`; screenshots are recorded but not verified in the build
([TECH-008](ROADMAP.md#tech-008), [TECH-009](ROADMAP.md#tech-009)).

## 7. Technical concerns

Index only; details live in the linked card.

| ID | Area | Concern (verified in code unless marked) |
|---|---|---|
| [TECH-001](ROADMAP.md#tech-001) | Process | No version control; docs can't cite commits |
| [TECH-002](ROADMAP.md#tech-002) | Observability | Firebase not configured: no analytics, crash reports or Remote Config |
| [TECH-003](ROADMAP.md#tech-003) | Ads | Code side done (per-variant ids, `verifyReleaseAdMob`, `ads_enabled`, privacy options); live ids and UMP messages pending |
| [TECH-004](ROADMAP.md#tech-004) | Billing | Play products not created; purchase flows unverified |
| [TECH-005](ROADMAP.md#tech-005) | Release | Upload key not created yet; build side fixed (no debug fallback) |
| [TECH-006](ROADMAP.md#tech-006) | Performance | Baseline profile hand-seeded, startup not measured |
| [TECH-007](ROADMAP.md#tech-007) | Release QA | Several screens not exercised in an R8 build on a device |
| [TECH-008](ROADMAP.md#tech-008) | Tests | Screenshot tests not verified; no CI |
| [TECH-009](ROADMAP.md#tech-009) | Tests | Steps, billing, ads, widget untested |
| [TECH-010](ROADMAP.md#tech-010) | Observability | `CrashReporter` unused; non-fatal errors not recorded |
| [TECH-011](ROADMAP.md#tech-011) | Compliance | Privacy policy drafted, not hosted; Health apps declaration, Data safety |
| [TECH-013](ROADMAP.md#tech-013) | Release | Play readiness review: findings and fixes |

## 8. Implementation guidelines

- **Progress is derived, never accumulated.** Store day totals; never add a counter that could double-count.
  A tower's target is frozen in `towers.target_floors` when founded.
- **Wallet entries are keyed** (`tower:N`, `tower_premium:N`, `tower_ad:N`, `ach:id`, `spend:item`); new
  rewards need a new key prefix to stay idempotent.
  `tower_premium:N` is written only when `tower:N` is first recorded (Pass at topping-out time), never alongside
  `tower_ad:N`; a tower earns at most 2× its base (BUG-002).
- **Room changes** need a version bump and a migration; schemas are exported to `core/database/schemas/`.
- **No step values** in `AnalyticsEvent` params, ad requests or logs.
- **No ads** on Build, onboarding or the ceremony; `RewardedAdGateway` is only injected by `SkylineViewModel`.
  Every Google Mobile Ads / UMP call goes through `AdSdk` behind `AdsGate`; never show a consent form without a tap.
- **Strings** in each module's `res/values/strings.xml`; bold spans use escaped `&lt;b>` and `boldText()`.
- **Design tokens only** (`SsTheme.colors/type`, `SsSpacing`, `SsRadius`, `SsMotion`); no raw colours or dp
  in features.
- **Text with a line height below the font's height** (big numbers, tower names, headlines: anything under
  ≈1.3em for DM Sans) gets `Modifier.cssLineBox(style)`; Android otherwise keeps the font's full ascent/descent
  and the text sits up to 15dp taller than the design. Rows that pair a number with a unit use
  `alignByBaseline()`; `RollingNumber` reports its own baseline.
- **Permission callbacks:** `AndroidPlatformActions` uses one `permissionLauncher` for ACTIVITY_RECOGNITION,
  POST_NOTIFICATIONS and WRITE_EXTERNAL_STORAGE and runs every pending callback on any result. Safe while only one
  request is pending at a time; split it before adding concurrent requests.
- **Release-only risks:** kotlinx-serialization routes and Glance receivers need the keep rules in
  `app/proguard-rules.pro`.
