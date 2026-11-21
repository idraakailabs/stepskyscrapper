# Step Skyscraper — Bugs and Known Issues

> **Scope:** defects in the current product, with evidence, impact, the proposed fix and acceptance
> criteria. Scheduling: [ROADMAP.md](ROADMAP.md) · Context: [TECHNICAL.md](TECHNICAL.md).
>
> **Baseline:** version 1.0.0, commit `e5c003b` on `main`. **Last reviewed:** 2026-11-21 (release-readiness review).

## How to read this file

| Field | Meaning |
|---|---|
| **Classification** | **Confirmed (code):** follows directly from the cited code; not necessarily reproduced. **Confirmed (reproduced):** observed at runtime. **Suspected:** plausible, but impact depends on something not visible here, or needs measuring. |
| **Status** | `Open`, `In Progress`, `Blocked`, `Fixed – unverified`, `Done` (fixed **and** acceptance criteria verified), `Won't fix` |
| **Priority** | P0 critical · P1 high · P2 medium · P3 low |

**Rules:** never delete entries. When fixing one, update the status and fill in **Resolution** (what changed,
commit) and **Verification** (how the acceptance criteria were checked). An entry becomes `Done` only after
verification.

Two defects found during the build session were fixed before this review and aren't listed: the adaptive
target suggesting 2,000 steps after a week of zero steps, and dark status-bar icons on the night sky (both
reproduced on the emulator and fixed in `SuggestDailyTargetUseCase` and `SystemBarsEffect`).

## Summary

| ID | Title | Priority | Classification | Status |
|---|---|---|---|---|
| [BUG-001](#bug-001) | Notifications are "on" but never delivered on Android 13+ | P2 | Confirmed (code) | Fixed – unverified |
| [BUG-002](#bug-002) | Blueprint bonuses can stack to 3× and Pass bonus applies retroactively | P2 | Confirmed (code) | Open |
| [BUG-003](#bug-003) | "Save image" gives no feedback and silently fails on Android 8–9 | P2 | Confirmed (code) | Fixed – unverified |
| [BUG-004](#bug-004) | Week card opens Goals instead of a weekly detail with "Pause goals" | P2 | Confirmed (code) | Open |
| [BUG-005](#bug-005) | Skyline ignores the tower to highlight after the ceremony | P3 | Confirmed (code) | Fixed – unverified |
| [BUG-006](#bug-006) | Onboarding payoff shows a 20% partial floor at 0 steps and rounds to segments | P3 | Confirmed (code) | Fixed – unverified |
| [BUG-007](#bug-007) | Ad consent form may appear just by opening Skyline | P3 | Suspected | Fixed – unverified |
| [BUG-008](#bug-008) | The Build feature module was missing from the Git repository | P0 | Confirmed (reproduced) | Done |
| [BUG-009](#bug-009) | Onboarding skips "Add widget" (1f) and jumps to Build | P2 | Confirmed (reproduced) | Done |
| [BUG-010](#bug-010) | Under-construction towers on the real ladder render as a sliver | P1 | Confirmed (reproduced) | Done |
| [BUG-011](#bug-011) | Build tab does nothing after closing a ceremony | P1 | Confirmed (reproduced) | Done |
| [BUG-012](#bug-012) | "Steps not arriving?" link never appears while syncs run | P2 | Confirmed (reproduced) | Done |
| [BUG-013](#bug-013) | Tower stats sheet opens half-height with its buttons under the gesture bar | P2 | Confirmed (reproduced) | Done |
| [BUG-014](#bug-014) | Me: "Today" label clipped when today is the tallest bar | P3 | Confirmed (reproduced) | Done |
| [BUG-015](#bug-015) | Me scrolls under the transparent status bar | P3 | Confirmed (reproduced) | Done |
| [BUG-016](#bug-016) | "Syncing…" icon wobbles while it rotates | P3 | Confirmed (reproduced) | Done |
| [BUG-017](#bug-017) | Pressed tab / segment / chip shows a rectangular ripple, not the capsule | P3 | Confirmed (reproduced) | Done |
| [BUG-018](#bug-018) | Health Connect privacy link opens Build instead of the privacy rationale | P1 | Confirmed (code) | Fixed – unverified |

---

<a id="bug-001"></a>
## BUG-001 — Notifications are "on" but never delivered on Android 13+

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Fixed – unverified
- **Evidence:** `UserSettings.Default` sets `notifyToppingOut = true` and `notifyWeeklySummary = true`
  (`core/domain/.../model/Settings.kt:29-30`). `POST_NOTIFICATIONS` is only requested when the user switches a
  toggle on in Settings (`feature/me/.../SubScreens.kt:348`, `:359`). `ProgressNotifier.onSynced` returns early
  when the permission isn't granted (`core/data/.../notify/ProgressNotifier.kt:41`).
- **Files:** `Settings.kt`, `SubScreens.kt`, `ProgressNotifier.kt`, onboarding or ceremony entry points.
- **Impact:** on Android 13+, users see "Topping out and weekly summary only" on Me but get no notifications,
  losing the main return driver.
- **Proposed fix:** ask for the permission at a meaningful moment (after the first topping-out, or on the
  widget step of onboarding), and show the Me row as "Off" when the OS permission is missing.
- **Acceptance criteria:**
  1. Fresh install on API 33+: after the first topping-out, the permission prompt appears once (emulator).
  2. With permission denied, Me › Notifications shows "Off" (Compose UI test).
  3. With permission granted, a topping-out triggers a notification (emulator with seeded steps).
- **Resolution (2026-11-21, commit `a4b1e3d`):** `PlatformActions.notificationsAllowed()` (permission on 33+ and
  `areNotificationsEnabled()`) and `rememberNotificationsAllowed()` in `core:ui`, re-checked on every resume. Me ›
  Notifications reads "Off" and both Settings switches read off while delivery is blocked. One-time prompt after the
  first topping-out: `CeremonyViewModel` asks on the first ceremony dismissal (Close or Start next) while either
  preference is on, and `CeremonyRoute` waits for the system dialog before navigating, so it never stacks on the
  paywall. New DataStore key `notify_prompt_handled` (`UserSettings.notificationPromptHandled`); no existing key changed.
- **Verification:** AC 2: `MeTest.notificationsRowReadsOffWhenTheOsBlocksThem` / `notificationsRowReadsOnWhenAllowed`;
  Pixel 6a (Android 16) with the permission denied: Me "Notifications · Off", both switches off; turning "Topping
  out" on showed the system dialog, Allow → on (re-checked on the integrated build). AC 1: unit-tested
  (`CeremonyViewModelTest`: first dismissal asks once and sets the flag; no prompt when both preferences are off) but
  **not checked on a device** (needs a fresh topping-out). AC 3 not checked. Known follow-ups (not fixed): granting
  from one switch also shows the other as on (both default on); no route to system settings after a permanent denial.

<a id="bug-002"></a>
## BUG-002 — Blueprint bonuses can stack to 3× and Pass bonus applies retroactively

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Open
- **Evidence:** `EnsureTowerRecordsUseCase` calls `CompleteTowerUseCase` for **every** finished tower on every
  sync (`core/domain/.../TowerLifecycleUseCases.kt:32`). `CompleteTowerUseCase` adds a `tower_premium:N` entry
  whenever the user is premium *now* (`:52`), so buying the Pass doubles Blueprints for all past towers. A
  rewarded ad claimed before buying the Pass (`GoalAndRewardUseCases.kt:58` only blocks premium users at claim
  time) plus the later premium entry gives 3×.
- **Impact:** inflated currency (cheapens Blueprint unlocks); ceremony totals inconsistent with rules shown on
  the paywall ("2× Blueprints on every tower").
- **Proposed fix:** product decision first ([PROD-006](ROADMAP.md#prod-006)); then award the premium bonus only
  for towers topped out while premium, and skip it when `tower_ad:N` exists (or vice versa).
- **Acceptance criteria:**
  1. Unit test: buying the Pass after 3 towers doesn't change the balance for those towers.
  2. Unit test: a tower never earns more than 2× its base Blueprints.
- **Resolution:** — · **Verification:** —

<a id="bug-003"></a>
## BUG-003 — "Save image" gives no feedback and silently fails on Android 8–9

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Fixed – unverified
- **Evidence:** `ShareScreen` ignores the `Boolean` from `platform.saveImage(...)`
  (`feature/share/.../ShareScreen.kt:272`); the strings `share_saved` / `share_save_failed` are unused. On
  API < 29 `AndroidPlatformActions.saveImage` requests `WRITE_EXTERNAL_STORAGE` and returns `false` without
  retrying after the grant.
- **Impact:** users don't know whether the image was saved; on Android 8–9 the first tap does nothing visible.
- **Proposed fix:** show a snackbar with the result; on API < 29 retry the save after the permission result.
- **Acceptance criteria:**
  1. Tapping Save shows "Saved to Pictures." on success and the failure copy otherwise (UI test with fake).
  2. API 28 emulator: granting the permission saves the image without a second tap.
- **Resolution (2026-11-21, commit `a4b1e3d`):** `PlatformActions.saveImage(image, onResult)` is callback-based; on
  API < 29 without permission it launches `WRITE_EXTERNAL_STORAGE` and saves straight after the grant. The PNG is
  written on `Dispatchers.IO`; a failed write deletes the empty MediaStore entry. `ShareUiState.saveResult` drives an
  `SsSnackbar` above the sheet ("Saved to Pictures." / "Couldn't save the image. Please try again.").
- **Verification:** AC 1: `ShareSaveFeedbackTest` (success, failure, nothing when idle); Pixel 6a: Save → "Saved to
  Pictures." and a new PNG in `Pictures/Step Skyscraper/`. AC 2 (Android 8–9 grant-then-save) **not verified**: no
  Android 8–9 device used.

> **Device check (2026-11-21, Pixel 6a, Android 16):** "Save image" wrote the PNG to `Pictures/Step Skyscraper/`
> with no on-screen confirmation — the missing feedback is reproduced; the Android 8–9 failure is not checked.

<a id="bug-004"></a>
## BUG-004 — Week card opens Goals instead of a weekly detail with "Pause goals"

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Open
- **Evidence:** `onOpenWeek = { nav.navigate(GoalsRoute) }` (`app/.../navigation/AppNavigation.kt:121`). The
  design (2a note) says "Week card → weekly detail with 'Pause goals'"; the Goals screen has no pause control,
  and the zero-step week card text "Resting? You can pause goals" leads somewhere without that option.
- **Impact:** the gentle "pause goals" path promised on Build is two screens away (Me tab).
- **Proposed fix:** add a weekly detail route (week bars, goal, pause toggle, link to Goals), reusing the Me
  chart components.
- **Acceptance criteria:**
  1. Tapping the week card opens a screen with this week's bars and a "Pause goals" control (UI test).
  2. Pausing there updates the Build week card to "Goals paused. Floors still count." (ViewModel test).
- **Resolution:** — · **Verification:** —

<a id="bug-005"></a>
## BUG-005 — Skyline ignores the tower to highlight after the ceremony

- **Priority:** P3 · **Classification:** Confirmed (code) · **Status:** Fixed – unverified
- **Evidence:** `SkylineRoute(val highlight: Int?)` (`feature/skyline/.../SkylineScreen.kt:88`) is passed by
  the ceremony's Close action but never read by `SkylineViewModel`; highlighting relies on the 24-hour "newest"
  pin instead.
- **Impact:** a ceremony shown more than 24 h after topping out (app closed) closes to a skyline without the
  highlight the design specifies.
- **Proposed fix:** read the route argument from `SavedStateHandle` and select that tower on first load.
- **Acceptance criteria:**
  1. ViewModel test: `SkylineRoute(3)` selects tower 3 and moves the pin to it.
- **Resolution (2026-11-21, commit `a4b1e3d`):** `SkylineViewModel` reads the route with
  `SavedStateHandle.toRoute<SkylineRoute>()`; `SkylineReducer.highlighted()` prefers the requested tower (if it is in
  the shown district) over the 24 h newest. That tower gets the pin, the "New" chip and the reward card. Merged with the
  AdMob gating in `fce3977` (card dropped when ads are `Disabled`).
- **Verification:** AC 1: `SkylineTest.lateCeremonyHighlightsTheRequestedTower` (tower topped out 9 days ago: pinned
  only with the argument) and `highlightIgnoresTowersOutsideTheDistrict`, passing on the merged code. Device check of
  a late ceremony still outstanding.

<a id="bug-006"></a>
## BUG-006 — Onboarding payoff shows a 20% partial floor at 0 steps and rounds to segments

- **Priority:** P3 · **Classification:** Confirmed (code) · **Status:** Fixed – unverified
- **Evidence:** `progress = (state.segmentsFilled / 10f).coerceAtLeast(0.2f).takeIf { … } ?: 0.2f`
  (`feature/onboarding/.../OnboardingScreen.kt:289`). With 0 steps into the next floor the tower still draws a
  20% partial floor; otherwise progress is rounded down to whole segments.
- **Impact:** small visual mismatch with the segment bar and the zero-step copy.
- **Proposed fix:** use `stepsIntoFloor / 1000f`, and 0 when there are no steps into the floor.
- **Acceptance criteria:**
  1. Screenshot test of 1c with 0 steps shows no partial floor; with 4,200 steps the partial floor is 20%.
- **Resolution:** the payoff tower uses `OnboardingUiState.floorProgress` (`stepsIntoFloor / 1000`, 0 when none)
  instead of the rounded, 20%-minimum segment value. · **Verification:** unit test
  `OnboardingTest.payoffPartialFloorFollowsStepsIntoTheFloor` (0 → 0, 4,200 → 0.2, 4,250 → 0.25). Device check of
  1c at 0 steps still outstanding (Pixel disconnected during the 2026-11-21 parity pass).

<a id="bug-007"></a>
## BUG-007 — Ad consent form may appear just by opening Skyline

- **Priority:** P3 · **Classification:** Suspected · **Status:** Fixed – unverified
- **Evidence:** `LaunchedEffect(showReward) { if (showReward) onIntent(SkylineIntent.PrepareAd(host)) }`
  (`feature/skyline/.../SkylineScreen.kt:138`) runs UMP `loadAndShowConsentFormIfRequired` as soon as the reward
  card is visible, not when the user taps the ad button.
- **Unknowns:** whether the form actually shows depends on UMP configuration and the user's region (not
  configured yet, [TECH-003](ROADMAP.md#tech-003)).
- **Investigation:** with a real UMP message and an EEA debug geography, open Skyline after a topping-out and
  observe whether the form appears without a tap.
- **Proposed fix:** gather consent only when the user taps "Watch a short ad…", then load and show.
- **Acceptance criteria:**
  1. EEA debug geography: no consent form appears until the ad button is tapped (device).
- **Resolution (2026-11-21, commit `280e1b3`):** `AdMobRewardedGateway.prepare()` (card visible) only refreshes the
  consent status without UI and preloads only if consent already allows requests. The form is shown only from
  `show()`, i.e. the tap on "Watch a short ad to double Blueprints", followed by `MobileAds.initialize` (background
  thread) → load → show. The button is tappable in every enabled state except `Loading`.
- **Verification:** `AdsGatingTest` "prepare never shows the consent form…" and "tap gathers consent before
  initialising and loading" (pass). Pixel 6a (debug): opening Skyline showed no form and no `consent` / `load request`
  lines, but no reward card was visible, so `prepare()` and the tap path weren't exercised. **Outstanding:** the
  acceptance case needs a tower topped out within 24 h, a published GDPR message and `ADMOB_UMP_DEBUG_EEA=true`
  ([TECHNICAL.md §5.1](TECHNICAL.md#51-admob-and-the-ads_enabled-switch)).

<a id="bug-008"></a>
## BUG-008 — The Build feature module was missing from the Git repository

- **Priority:** P0 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** `.gitignore` had a blanket `build/` rule, which also matched the `:feature:build` module folder
  (`feature/build/`) and the Kotlin package folder `…/feature/build/`. `git check-ignore -v` reported
  `.gitignore:6:build/` for `feature/build/src/main/kotlin/…/BuildScreen.kt`; commits `e5c003b`…`df71b8f` on
  `origin/main` contain no `feature/build` files.
- **Files:** `.gitignore`; all of `feature/build/`.
- **Impact:** a fresh clone of `main` doesn't build (Build tab, ceremony and next-tower screens missing);
  found during the package rename when KSP still compiled the un-renamed, untracked sources.
- **Proposed fix:** replace `build/` with explicit Gradle output folders (`/app/build/`, `/core/*/build/`,
  `/feature/*/build/`, …) and add the module's sources.
- **Acceptance criteria:**
  1. `git ls-tree -r origin/main --name-only` lists `feature/build/src/main/kotlin/…/BuildScreen.kt`.
  2. A fresh clone of `main` passes `./gradlew build`.
  3. No Gradle output (`*/build/intermediates`, `generated`, `outputs`) is tracked.
- **Resolution:** `.gitignore` now lists the Gradle output folders explicitly; `feature/build` sources added
  (commit `55df97b`). · **Verification:** 1. `git ls-tree -r origin/main` lists 13 `feature/build/src` files.
  2. A fresh `git clone` of `main` built `:app:assembleDebug` successfully. 3. `git ls-tree` shows no tracked
  `build/intermediates|generated|outputs|tmp` paths.

<a id="bug-009"></a>
## BUG-009 — Onboarding skips "Add widget" (1f) and jumps to Build

- **Priority:** P2 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** on the Pixel 6a (debug build, 2026-11-21) tapping "Set 49 floors a week" on 1e opened Build
  directly; 1f never appeared. `StartBuildingUseCase` marks onboarding complete before the widget step, the
  settings flow re-emits, and `AppNavHost` computed `startDestination = if (onboardingComplete) BuildRoute else
  OnboardingRoute` on every recomposition, so the nav graph was rebuilt with Build as the start.
- **Files:** `app/.../navigation/AppNavigation.kt`.
- **Impact:** the widget prompt (and its first-floor replay on Build) is never shown; widget adoption suffers.
- **Fix:** the start destination is fixed for the life of the screen (`rememberSaveable { onboardingComplete }`).
- **Acceptance criteria:**
  1. Fresh install on a device: 1e CTA → 1f "Add widget"; both "Add widget" and "Maybe later" → Build.
  2. Relaunching after onboarding opens Build directly.
- **Resolution:** `AppNavHost` remembers the start destination (commit `fb98ad6`). · **Verification:** Pixel 6a,
  fresh install: 1e CTA → 1f shown; "Maybe later" → Build; relaunch opens Build directly.

<a id="bug-010"></a>
## BUG-010 — Under-construction towers on the real ladder render as a sliver

- **Priority:** P1 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a, tower 4 at 19 of 100 floors: the dashed ghost outline runs to floor 100 and the view
  fits that height, so the built floors fill ~10% of the Build hero (also the Customise preview and the
  in-progress share card). `tower.js` behaves the same; the design simply uses 12–24-floor targets, while the
  ladder is 10/25/50/100/160/250 ([P1](design-deviations.md)).
- **Impact:** from tower 4 on, the main screen's tower is tiny for weeks; the core "watch it grow" payoff is lost.
- **Options:** (a) draw the ghost only up to the next setback band or N floors ahead and fit to that;
  (b) fit to the built floors plus a fixed margin and let the ghost run off the top; (c) shorten the ladder.
- **Decision (user, 2026-11-21):** option (a), capped at 10 floors ahead so every design frame is unchanged.
- **Acceptance criteria:** at 19 of 100 floors the built tower fills at least half of the hero height on a
  360×800 and a 411×914 screen; topped-out towers unchanged.
- **Resolution:** the dashed outline reaches at most `TowerConst.GHOST_FLOORS_AHEAD` (10) floors above the built
  ones (`ghostEnd` in `IsoScene.kt`); setbacks still follow the full target; a capped outline has no top, so the
  tower reads as continuing. Applies to Build, Customise preview, share cards and the skyline.
- **Verification:** `GhostOutlineTest` (design frames 14/24, 4/12, 7/12, 4/10 keep the full outline; built floors
  ≥ 50% of the drawing at 19 of 100/160/250). Pixel 6a: Build at 19 of 100 fills the hero like design 2a; the
  Customise preview and in-progress share card show a readable tower. Topped-out towers have no outline (unchanged).

<a id="bug-011"></a>
## BUG-011 — Build tab does nothing after closing a ceremony

- **Priority:** P1 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a: ceremony → Close → Skyline; tapping Build (or Me → Build) kept showing Skyline.
  Ceremony close used `goToTab` (pop with `saveState = false`), the tab bar popped to the start destination
  with `saveState = true` and navigated with `restoreState = true`, so selecting Build restored the stack just
  saved (Skyline).
- **Files:** `app/.../navigation/AppNavigation.kt`.
- **Resolution:** one `goToTab` for all tab changes: pop up to `BuildRoute` (the real root after onboarding)
  saving state; never restore state when selecting Build; ceremony close opens a fresh Skyline (commit `9b94410`).
- **Verification:** Pixel 6a: ceremony → Close → Skyline → Build opens Build; Me → Build opens Build; Build →
  Skyline → Build still works.

<a id="bug-012"></a>
## BUG-012 — "Steps not arriving?" link never appears while syncs run

- **Priority:** P2 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a with the debug clock 3 days ahead and no steps: Build showed "Resting? You can pause
  goals", never the 48h step-help link. Every new day's sync inserts a 0-step row and `markSucceeded` treated
  any written row as new data, resetting `sync_last_change`.
- **Files:** `core/domain/.../SyncStepsUseCase.kt`, `core/domain/.../model/Steps.kt`.
- **Resolution:** new data = a day's total went up (`SyncResult.stepsArrived`) (commit `9b94410`).
- **Verification:** unit test `SyncStepsUseCaseTest.newEmptyDaysAreNotNewData`; Pixel 6a: last data change on
  the 21st, clock on the 24th → "Steps not arriving? Let's check" → opens 10a.

<a id="bug-013"></a>
## BUG-013 — Tower stats sheet opens half-height with its buttons under the gesture bar

- **Priority:** P2 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a (914dp tall): 4b opened partially expanded (~457dp) and "Share tower" / "Rename" sat
  under the navigation bar; the design shows the whole sheet.
- **Files:** `feature/skyline/.../SkylineScreen.kt` (`skipPartiallyExpanded = false`).
- **Resolution:** the sheet opens at its content height like every other sheet (this branch).
- **Verification:** Pixel 6a: 4b opens fully with both buttons above the gesture bar; swipe between towers works.

<a id="bug-014"></a>
## BUG-014 — Me: "Today" label clipped when today is the tallest bar

- **Priority:** P3 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a: the bar column was a fixed 120dp but value + 88dp bar + label needs 124dp, so the bold
  "Today" was cut off when today had the most floors.
- **Resolution:** the row is at least 120dp and grows to fit (this branch). · **Verification:** Pixel 6a, today
  = 6 floors (week max): "Today" fully visible.

<a id="bug-015"></a>
## BUG-015 — Me scrolls under the transparent status bar

- **Priority:** P3 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence:** Pixel 6a: the status-bar padding was inside the scroll container, so cards and titles scrolled
  under the clock and icons.
- **Resolution:** padding applied outside the scroll (this branch). · **Verification:** Pixel 6a: scrolled Me
  content stops below the status bar.

<a id="bug-016"></a>
## BUG-016 — "Syncing…" icon wobbles while it rotates

- **Priority:** P3 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence (user report, 2026-11-21):** the sync chip rotated a Material Symbols font glyph. The glyph sits in a text
  line box built from the font metrics (ascent 1056 / descent 96 on a 960 em, rounded to whole pixels), so it was not
  centred in its 16dp box and the rotation pivot was off the glyph's centre.
- **Files:** `core/designsystem/.../icon/SyncVector.kt` (new), `icon/SsIcon.kt`, `component/Chips.kt`.
- **Acceptance criteria:** the icon turns about its own centre during "Syncing…" (device recording).
- **Resolution (commit `a4b1e3d`):** `SsSyncIcon` draws the same outline as a vector (path from the bundled font,
  viewport = the 960×960 em, outline 160..800 on both axes) in a square 16dp box, rotated in `graphicsLayer` about the
  default centre; the angle is read in the draw phase only. Reduce motion still disables the spin.
- **Verification:** Pixel 6a, `screenrecord` during pull-to-refresh, centroid measured per frame: drift < 0.25 px on
  the bug-fix branch and < 0.1 px on the integrated build.

<a id="bug-017"></a>
## BUG-017 — Pressed tab / segment / chip shows a rectangular ripple, not the capsule

- **Priority:** P3 · **Classification:** Confirmed (reproduced) · **Status:** Done
- **Evidence (user report, 2026-11-21):** the resting 64×32 pill already matched the design, but the touch feedback
  came from `selectable()` on the whole cell, so pressing a bottom-bar tab, a segmented toggle or a Customise chip
  showed a rectangular state layer over the full touch target.
- **Files:** `core/designsystem/.../component/Navigation.kt`, `Surfaces.kt`, `Chips.kt`.
- **Acceptance criteria:** the pressed state is confined to the visible capsule; resting visuals unchanged.
- **Resolution (commit `a4b1e3d`):** bottom bar and filter chips keep the 48dp touch target with `indication = null`
  and draw the ripple with `.indication(interaction, ripple())` on the pill clipped to `SsRadius.shapeFull`; segmented
  toggles clip before `background`/`selectable`; clickable `SsCard` / `SsRadioCard` ripples clipped to their shape.
- **Verification:** Pixel 6a, long-press captures: Me tab state layer only within the 64×32 dp pill; Share segment and
  Customise chip pressed states stay inside the capsule. Roborazzi baselines re-recorded (anti-aliasing differences only).

<a id="bug-018"></a>
## BUG-018 — Health Connect privacy link opens Build instead of the privacy rationale

- **Priority:** P1 · **Classification:** Confirmed (code) · **Status:** Fixed – unverified
- **Evidence (release-readiness review, 2026-11-21):** the manifest routes
  `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13 and lower) and the Android 14 `VIEW_PERMISSION_USAGE`
  alias to `MainActivity`, but nothing read `intent.action`: Health Connect's privacy link opened onboarding or Build.
  Health Connect expects that link to show the app's privacy rationale / policy.
- **Files:** `app/.../MainActivity.kt`, `feature/me/.../SubScreens.kt`.
- **Impact:** Play review risk for the Health apps declaration ([TECH-011](ROADMAP.md#tech-011)).
- **Acceptance criteria:** from Health Connect's app-permission screen (Android 13 and 14+), the privacy link opens
  Help & privacy on its own; back returns to Health Connect.
- **Resolution:** launched with either action, `MainActivity` shows `PrivacyRoute` (Help & privacy: the in-app privacy
  summary, Health Connect access and, once `PRIVACY_POLICY_URL` is set, the hosted policy link) instead of the app;
  back finishes the activity. The step-help button is hidden there (no app navigation).
- **Verification:** `build test lint` green. Pixel 6a (Android 16, debug build):
  `adb shell am start -a androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE -n …debug/ai.idraak.stepskyscraper.MainActivity`
  opened Help & privacy alone (no step-help button, no tabs); back returned to the launcher; a normal launch still
  opens Build / Skyline / Me. The Android 14+ alias can't be started from adb (protected by
  `START_VIEW_PERMISSION_USAGE`, as intended) and runs the same `intent.action` check. **Outstanding:** tap the privacy
  link from Health Connect itself, with `PRIVACY_POLICY_URL` set so the policy link shows.
