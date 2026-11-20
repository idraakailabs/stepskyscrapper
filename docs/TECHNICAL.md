# Step Skyscraper — Technical Reference

> **Scope:** how the system is built today (the Phase 1 baseline), important technical decisions, and the
> index of technical concerns. Defects: [BUGS.md](BUGS.md) · Planned work: [ROADMAP.md](ROADMAP.md) ·
> Product: [PRODUCT.md](PRODUCT.md) · Design deviations: [design-deviations.md](design-deviations.md) ·
> Release checklist: [play-console-checklist.md](play-console-checklist.md).
>
> **Baseline:** version 1.0.0, commit `e5c003b` on `main`. **Last reviewed:** 2026-11-20.

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
| External services | Play Billing 9.1.0, Google Mobile Ads 25.5.0 + UMP 4.0.0, Firebase BOM 35.2.0 (Analytics, Crashlytics, Remote Config) |
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

Rewarded ad
  Skyline reward card visible → RewardedAdGateway.prepare (UMP consent, then load)  // see BUG-007
  tap → ClaimRewardedBonusUseCase → show → wallet "tower_ad:N"                       // never grants floors
```

## 4. Storage map

| Store | Location | Contents | Survives restart? |
|---|---|---|---|
| Room `step_skyscraper.db` | `core/database` | `day_steps` (totals), `floor_events`, `towers` (name, look, frozen target, ceremony/ad flags), `wallet` (keyed Blueprint ledger), `unlocks`, `achievements`, `counters` | Yes; Android backup |
| DataStore `user_prefs` | `core/datastore/.../PreferenceStores.kt` | Onboarding, start date, goals, pause, appearance, notifications, sync timestamps, sensor baseline, entitlement cache | Yes; Android backup |
| In memory | `PreferencesSyncStatusRepository.running` | "Syncing…" flag | No (by design) |
| Cache dir `share/` | `AndroidPlatformActions.shareImage` | Last exported share PNG | Until cleared |

Backup rules: `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`. Schema export:
`core/database/schemas/`.

## 5. Configuration and flags

| Key / variable | Default | Used by |
|---|---|---|
| `ADMOB_APP_ID` (gradle property) | Google sample id | Manifest `com.google.android.gms.ads.APPLICATION_ID` |
| `ADMOB_REWARDED_UNIT_ID` | Google sample rewarded unit | `core:ads` BuildConfig |
| `PRIVACY_POLICY_URL` | empty (link hidden) | `AndroidPlatformActions.openPrivacyPolicy` |
| `SUPPORT_EMAIL` | empty (generic share sheet) | Diagnostic draft |
| `app/google-services.json` | absent | Applies Google Services + Crashlytics plugins only if present |
| Remote Config `tower_ladder` | `10,25,50,100,160,250` (bundled) | `RemoteConfigLadderRepository`; malformed values fall back |
| `keystore.properties` or `STEPSKY_KEYSTORE_FILE` / `STEPSKY_KEYSTORE_PASSWORD` / `STEPSKY_KEY_ALIAS` / `STEPSKY_KEY_PASSWORD` | absent → release **unsigned**, `bundleRelease` fails | `app/build.gradle.kts` (template `keystore.properties.example`) |

Live values (Firebase, AdMob, Play Console) weren't available; they are unverified.

## 6. Build, signing and release

- Application id / namespace: `ai.idraak.stepskyscraper` (renamed from `com.idraak.stepskyscraper` on
  2026-11-20, before any store release); library namespaces follow `ai.idraak.stepskyscraper.<module path>`.
- Variants: `debug` (`.debug` suffix) and `release` (R8 minify + resource shrinking, `app/proguard-rules.pro`).
- Signing: upload key from `keystore.properties` (git-ignored, `storeFile` relative to the root, e.g. under the
  ignored `keystore/` folder) or the `STEPSKY_*` environment variables in CI. Never the debug key: without a key
  the release APK is unsigned and `bundleRelease` stops at `verifyReleaseSigning`.
- Baseline profile: hand-seeded `app/src/main/baseline-prof.txt`; generator in `:baselineprofile` needs a
  device ([TECH-006](ROADMAP.md#tech-006)).
- Git: `main`, remote `origin` = `github.com/idraakailabs/stepskyscrapper`; baseline commit `e5c003b`. `.gitattributes` pins LF line endings. No CI configuration yet.

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
emulator, through onboarding with the real Health Connect sheet, to Build and Me.

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
tests for Onboarding and Build, and Roborazzi screenshots (tower renderer, key screens, gallery). Gaps: no tests
for `core:steps`, `core:billing`, `core:ads`, `widget`; screenshots are recorded but not verified in the build
([TECH-008](ROADMAP.md#tech-008), [TECH-009](ROADMAP.md#tech-009)).

## 7. Technical concerns

Index only; details live in the linked card.

| ID | Area | Concern (verified in code unless marked) |
|---|---|---|
| [TECH-001](ROADMAP.md#tech-001) | Process | No version control; docs can't cite commits |
| [TECH-002](ROADMAP.md#tech-002) | Observability | Firebase not configured: no analytics, crash reports or Remote Config |
| [TECH-003](ROADMAP.md#tech-003) | Ads | Google sample AdMob ids by default |
| [TECH-004](ROADMAP.md#tech-004) | Billing | Play products not created; purchase flows unverified |
| [TECH-005](ROADMAP.md#tech-005) | Release | Upload key not created yet; build side fixed (no debug fallback) |
| [TECH-006](ROADMAP.md#tech-006) | Performance | Baseline profile hand-seeded, startup not measured |
| [TECH-007](ROADMAP.md#tech-007) | Release QA | Several screens not exercised in an R8 build on a device |
| [TECH-008](ROADMAP.md#tech-008) | Tests | Screenshot tests not verified; no CI |
| [TECH-009](ROADMAP.md#tech-009) | Tests | Steps, billing, ads, widget untested |
| [TECH-010](ROADMAP.md#tech-010) | Observability | `CrashReporter` unused; non-fatal errors not recorded |
| [TECH-011](ROADMAP.md#tech-011) | Compliance | Privacy policy URL, Health apps declaration, Data safety |

## 8. Implementation guidelines

- **Progress is derived, never accumulated.** Store day totals; never add a counter that could double-count.
  A tower's target is frozen in `towers.target_floors` when founded.
- **Wallet entries are keyed** (`tower:N`, `tower_premium:N`, `tower_ad:N`, `ach:id`, `spend:item`); new
  rewards need a new key prefix to stay idempotent.
- **Room changes** need a version bump and a migration; schemas are exported to `core/database/schemas/`.
- **No step values** in `AnalyticsEvent` params, ad requests or logs.
- **No ads** on Build, onboarding or the ceremony; `RewardedAdGateway.prepare` is only called from Skyline.
- **Strings** in each module's `res/values/strings.xml`; bold spans use escaped `&lt;b>` and `boldText()`.
- **Design tokens only** (`SsTheme.colors/type`, `SsSpacing`, `SsRadius`, `SsMotion`); no raw colours or dp
  in features.
- **Release-only risks:** kotlinx-serialization routes and Glance receivers need the keep rules in
  `app/proguard-rules.pro`.
