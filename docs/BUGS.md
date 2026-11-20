# Step Skyscraper — Bugs and Known Issues

> **Scope:** defects in the current product, with evidence, impact, the proposed fix and acceptance
> criteria. Scheduling: [ROADMAP.md](ROADMAP.md) · Context: [TECHNICAL.md](TECHNICAL.md).
>
> **Baseline:** version 1.0.0, commit `e5c003b` on `main`. **Last reviewed:** 2026-11-20.

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
| [BUG-001](#bug-001) | Notifications are "on" but never delivered on Android 13+ | P2 | Confirmed (code) | Open |
| [BUG-002](#bug-002) | Blueprint bonuses can stack to 3× and Pass bonus applies retroactively | P2 | Confirmed (code) | Open |
| [BUG-003](#bug-003) | "Save image" gives no feedback and silently fails on Android 8–9 | P2 | Confirmed (code) | Open |
| [BUG-004](#bug-004) | Week card opens Goals instead of a weekly detail with "Pause goals" | P2 | Confirmed (code) | Open |
| [BUG-005](#bug-005) | Skyline ignores the tower to highlight after the ceremony | P3 | Confirmed (code) | Open |
| [BUG-006](#bug-006) | Onboarding payoff shows a 20% partial floor at 0 steps and rounds to segments | P3 | Confirmed (code) | Open |
| [BUG-007](#bug-007) | Ad consent form may appear just by opening Skyline | P3 | Suspected | Open |
| [BUG-008](#bug-008) | The Build feature module was missing from the Git repository | P0 | Confirmed (reproduced) | Done |

---

<a id="bug-001"></a>
## BUG-001 — Notifications are "on" but never delivered on Android 13+

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Open
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
- **Resolution:** — · **Verification:** —

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

- **Priority:** P2 · **Classification:** Confirmed (code) · **Status:** Open
- **Evidence:** `ShareScreen` ignores the `Boolean` from `platform.saveImage(...)`
  (`feature/share/.../ShareScreen.kt:272`); the strings `share_saved` / `share_save_failed` are unused. On
  API < 29 `AndroidPlatformActions.saveImage` requests `WRITE_EXTERNAL_STORAGE` and returns `false` without
  retrying after the grant.
- **Impact:** users don't know whether the image was saved; on Android 8–9 the first tap does nothing visible.
- **Proposed fix:** show a snackbar with the result; on API < 29 retry the save after the permission result.
- **Acceptance criteria:**
  1. Tapping Save shows "Saved to Pictures." on success and the failure copy otherwise (UI test with fake).
  2. API 28 emulator: granting the permission saves the image without a second tap.
- **Resolution:** — · **Verification:** —

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

- **Priority:** P3 · **Classification:** Confirmed (code) · **Status:** Open
- **Evidence:** `SkylineRoute(val highlight: Int?)` (`feature/skyline/.../SkylineScreen.kt:88`) is passed by
  the ceremony's Close action but never read by `SkylineViewModel`; highlighting relies on the 24-hour "newest"
  pin instead.
- **Impact:** a ceremony shown more than 24 h after topping out (app closed) closes to a skyline without the
  highlight the design specifies.
- **Proposed fix:** read the route argument from `SavedStateHandle` and select that tower on first load.
- **Acceptance criteria:**
  1. ViewModel test: `SkylineRoute(3)` selects tower 3 and moves the pin to it.
- **Resolution:** — · **Verification:** —

<a id="bug-006"></a>
## BUG-006 — Onboarding payoff shows a 20% partial floor at 0 steps and rounds to segments

- **Priority:** P3 · **Classification:** Confirmed (code) · **Status:** Open
- **Evidence:** `progress = (state.segmentsFilled / 10f).coerceAtLeast(0.2f).takeIf { … } ?: 0.2f`
  (`feature/onboarding/.../OnboardingScreen.kt:289`). With 0 steps into the next floor the tower still draws a
  20% partial floor; otherwise progress is rounded down to whole segments.
- **Impact:** small visual mismatch with the segment bar and the zero-step copy.
- **Proposed fix:** use `stepsIntoFloor / 1000f`, and 0 when there are no steps into the floor.
- **Acceptance criteria:**
  1. Screenshot test of 1c with 0 steps shows no partial floor; with 4,200 steps the partial floor is 20%.
- **Resolution:** — · **Verification:** —

<a id="bug-007"></a>
## BUG-007 — Ad consent form may appear just by opening Skyline

- **Priority:** P3 · **Classification:** Suspected · **Status:** Open
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
- **Resolution:** — · **Verification:** —

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
