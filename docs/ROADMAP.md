# Step Skyscraper — Roadmap

> **Scope:** the phased development plan. Phase 1 is the current product. Every future item has a stable
> ID, priority, status, dependencies and acceptance criteria. Bug details (`BUG-xxx`) live in
> [BUGS.md](BUGS.md); this file only schedules them. Rationale: [PRODUCT.md](PRODUCT.md) · Context:
> [TECHNICAL.md](TECHNICAL.md).
>
> **Last updated:** 2026-11-21 (release-readiness review)

## Current position

| | |
|---|---|
| **Live baseline** | Phase 1, version 1.0.0 (versionCode 1), commit `a8d9d51` on `main` (not published). Release-prep work is merged into `develop` (`aa95425`), not yet into `main` |
| **Active phase** | Phase 2, Release blockers: every code-side item is done; what remains are your console and key actions (see [RELEASE-READINESS.md](RELEASE-READINESS.md)) |
| **In progress** | [TECH-002](#tech-002) (Firebase configured; device checks), [TECH-014](#tech-014) (RevenueCat; Play service account), [TECH-005](#tech-005) (upload key), [TECH-003](#tech-003) (live AdMob ids, UMP messages), [TECH-011](#tech-011) (host the policy, declarations), [TECH-012](#tech-012) (on-device checks that need your phone settings) |
| **Recommended next task** | You: create the upload key and AdMob app, connect the Play service account to RevenueCat, host [privacy-policy.md](privacy-policy.md), then build the AAB and upload it to internal testing ([play-console-checklist.md](play-console-checklist.md)). Then [TECH-004](#tech-004) billing products and [TECH-007](#tech-007) release smoke test. |

Update this table whenever a task starts, finishes or the active phase changes.

## Conventions

- **ID prefixes:** `BUG` defect ([BUGS.md](BUGS.md)) · `TECH` technical, security or release work ·
  `PROD` product or UX · `GROW` growth or monetisation.
- **Type:** Confirmed bug · Suspected issue / investigation · Technical task · Product improvement · Product
  decision · Product idea (optional; needs approval) · Growth task.
- **Priority:** P0 critical · P1 high · P2 medium · P3 low. **Effort:** XS < 1 h, S ≤ 1 day, M ≤ 1 week, L > 1 week.
- **Status:** `Planned` · `In Progress` · `Blocked` · `Done` (acceptance criteria verified) · `Dropped`
  (with a reason).
- IDs are permanent. Never reuse or renumber them.
- Phases are ordered by dependency and risk. Later-phase work may start early only if its dependencies are
  `Done`.

---

## Phase 1 — Current baseline (1.0.0) · Status: Built, not published

A record, not a task list. Features: [PRODUCT.md §4](PRODUCT.md#4-phase-1-baseline--what-the-product-does-today).
Architecture: [TECHNICAL.md](TECHNICAL.md). Known defects: [BUGS.md](BUGS.md). Design coverage:
[design-inventory.md](design-inventory.md).

- Every designed screen and state, both widgets, share cards, paywall, rewarded ad and step-source help.
- Health Connect + sensor fallback; deterministic, repeat-safe sync; all data on the device.
- Clean modular architecture (MVI, Hilt, Room, DataStore, WorkManager); 84 passing tests; green
  `build test lint` and an R8 release APK.
- Gaps: Firebase/AdMob/Play not configured, debug-signed release, no CI, six small design gaps (inventory items marked `[~]`).
  (Release prep since then: signing guard, AdMob hardening, bug fixes and the Play readiness review; see Phase 2.)

---

<a id="phase-2--release-blockers"></a>
## Phase 2 — Release blockers

Goal: a release build that can be uploaded and observed. **Exit criteria:** a signed AAB on the internal
testing track with crash reports, analytics, real ads and working purchases.

| ID | Title | Type | Priority | Status | Depends on |
|---|---|---|---|---|---|
| [TECH-001](#tech-001) | Put the project under version control | Technical task | P1 | Done | — |
| [BUG-008](BUGS.md#bug-008) | Build feature module missing from Git | Confirmed bug | P0 | Done | — |
| [TECH-002](#tech-002) | Configure Firebase (Analytics, Crashlytics, Remote Config) | Technical task | P0 | In Progress (configured; device checks pending) | — |
| [TECH-003](#tech-003) | Real AdMob ids and UMP consent message | Technical task | P0 | In Progress (code done; ids and console pending you) | AdMob console |
| [TECH-004](#tech-004) | Create Play billing products and test purchases | Technical task | P0 | Planned | Play Console |
| [TECH-005](#tech-005) | Release signing | Technical task | P0 | In Progress | Upload key |
| [TECH-011](#tech-011) | Privacy policy, Health apps declaration, Data safety | Technical task | P0 | In Progress (drafts done; hosting and console pending you) | Hosted policy, Play Console |
| [TECH-013](#tech-013) | Play release readiness review | Technical task | P0 | Done | — |
| [TECH-014](#tech-014) | Architect Pass purchases through RevenueCat | Technical task | P0 | In Progress | Play service account, TECH-004 |
| [BUG-019](BUGS.md#bug-019) | Paywall promises a trial reminder the app never sends | Confirmed bug | P1 | Done | — |
| [BUG-018](BUGS.md#bug-018) | Health Connect privacy link opens Build | Confirmed bug | P1 | Done | — |

## Phase 3 — Reliability and release quality

Goal: trust the build on real devices. **Exit criteria:** every screen exercised in a release build, startup
measured, regressions caught automatically.

| ID | Title | Type | Priority | Status | Depends on |
|---|---|---|---|---|---|
| [TECH-007](#tech-007) | Release-build smoke test of every screen on devices | Technical task | P1 | Planned | TECH-002…005 |
| [TECH-012](#tech-012) | Design parity pass on a physical phone | Technical task | P1 | In Progress | — |
| [BUG-009](BUGS.md#bug-009) | Onboarding skips "Add widget" (1f) | Confirmed bug | P2 | Done | — |
| [BUG-010](BUGS.md#bug-010) | Under-construction towers render as a sliver | Confirmed bug | P1 | Done | — |
| [BUG-011](BUGS.md#bug-011) | Build tab dead after closing a ceremony | Confirmed bug | P1 | Done | — |
| [BUG-012](BUGS.md#bug-012) | 48h step-help link never appears | Confirmed bug | P2 | Done | — |
| [BUG-013](BUGS.md#bug-013) | Tower stats sheet cut off | Confirmed bug | P2 | Done | — |
| [BUG-014](BUGS.md#bug-014) | Me "Today" label clipped | Confirmed bug | P3 | Done | — |
| [BUG-015](BUGS.md#bug-015) | Me scrolls under the status bar | Confirmed bug | P3 | Done | — |
| [TECH-006](#tech-006) | Generate the baseline profile and measure startup | Technical task | P1 | Planned | Device |
| [BUG-001](BUGS.md#bug-001) | Notifications "on" but never delivered on Android 13+ | Confirmed bug | P2 | Fixed – unverified | — |
| [BUG-003](BUGS.md#bug-003) | "Save image" gives no feedback | Confirmed bug | P2 | Fixed – unverified | — |
| [BUG-016](BUGS.md#bug-016) | "Syncing…" icon wobbles while rotating | Confirmed bug | P3 | Done | — |
| [BUG-017](BUGS.md#bug-017) | Rectangular ripple on capsule tabs | Confirmed bug | P3 | Done | — |
| [TECH-008](#tech-008) | CI with screenshot verification | Technical task | P2 | Planned | TECH-001 |
| [TECH-009](#tech-009) | Tests for steps, billing, ads and widget | Technical task | P2 | In Progress (ads done) | — |
| [BUG-007](BUGS.md#bug-007) | Ad consent form may appear on Skyline open | Suspected issue | P3 | Fixed – unverified | TECH-003 |
| [TECH-010](#tech-010) | Record non-fatal errors in Crashlytics | Technical task | P3 | Planned | TECH-002 |

## Phase 4 — Core product value

Goal: close the remaining gaps in the main promise. **Exit criteria:** the week card leads to pausing, all
districts are viewable, Blueprint rules are consistent.

| ID | Title | Type | Priority | Status | Depends on |
|---|---|---|---|---|---|
| [PROD-006](#prod-006) | Decide first-tower height and premium-bonus rules | Product decision | P2 | In Progress (bonus rules decided; ladder open) | — |
| [BUG-002](BUGS.md#bug-002) | Blueprint bonuses can stack / apply retroactively | Confirmed bug | P2 | Done | PROD-006 |
| [BUG-004](BUGS.md#bug-004) | Week card opens Goals, not weekly detail | Confirmed bug | P2 | Done | — |
| [PROD-001](#prod-001) | Browse every district in Skyline | Product improvement | P2 | Planned | — |
| [PROD-002](#prod-002) | Re-skin finished towers from the stats sheet | Product improvement | P3 | Planned | — |

## Phase 5 — Growth and monetisation

Goal: learn from real users. **Exit criteria:** funnel dashboards live and the store listing published.

| ID | Title | Type | Priority | Status | Depends on |
|---|---|---|---|---|---|
| [GROW-001](#grow-001) | Activation and retention dashboards | Growth task | P2 | Planned | TECH-002 |
| [GROW-002](#grow-002) | Store listing from share cards | Growth task | P2 | Planned | TECH-007 |
| [GROW-003](#grow-003) | Paywall price and trial experiments | Product idea (optional) | P3 | Planned | TECH-004, GROW-001 |
| [GROW-004](#grow-004) | Policy-safe interstitial ad (post-launch, optional) | Product idea (optional) | P3 | Planned | TECH-003, GROW-001 |

## Phase 6 — Polish and future

Goal: finish the remaining design details. Each item is optional.

| ID | Title | Type | Priority | Status | Depends on |
|---|---|---|---|---|---|
| [PROD-003](#prod-003) | Remaining motion details from the design | Product improvement | P3 | Planned | — |
| [PROD-004](#prod-004) | "Equip" shortcut and Customise sticky thumbnail | Product improvement | P3 | Planned | — |
| [PROD-005](#prod-005) | Localise cosmetic and achievement names | Technical task | P3 | Planned | — |
| [BUG-005](BUGS.md#bug-005) | Skyline ignores the highlight argument | Confirmed bug | P3 | Fixed – unverified | — |
| [BUG-006](BUGS.md#bug-006) | Payoff partial floor at 0 steps | Confirmed bug | P3 | Fixed – unverified | — |

## Not planned

See [PRODUCT.md §11](PRODUCT.md#11-out-of-scope-dont-build): accounts and cloud sync, streaks or decay, paying
for floors, location features.

---

# Task cards

Bug cards are in [BUGS.md](BUGS.md).

<a id="tech-001"></a>
### TECH-001 — Put the project under version control
- **Type:** Technical task · **Priority:** P1 · **Status:** Done · **Effort:** XS
- **Problem (verified):** `git rev-parse` failed in the project root; there is no history, no way to cite a
  baseline commit, and the doc update workflow can't diff changes.
- **Solution:** `git init`, review `.gitignore` (it already excludes `google-services.json`, keystores and
  `local.properties`), commit the current tree, push to a private remote.
- **Benefit:** history, code review, CI, and docs that reference commits.
- **Dependencies / risks:** choose a remote host.
- **Acceptance criteria:**
  1. `git log` shows an initial commit; `git status` is clean.
  2. `google-services.json`, `keystore.properties` and `local.properties` aren't tracked (`git ls-files`).
  3. The doc baselines are updated to that commit hash.
  4. `main` is pushed to `origin` and tracks `origin/main`.
- **Resolution (2026-11-20):** Git initialised; initial commit `e5c003b` (author zabiirana27@gmail.com);
  `.gitattributes` pins LF line endings; remote `origin` = `github.com/idraakailabs/stepskyscrapper`.
- **Verification:** 1. `git log` shows the initial commit and `git status` is clean. 2. `git ls-files` contains
  no `google-services.json`, keystore, `local.properties` or build output. 3. Docs cite `e5c003b`. 4. `git push
  -u origin main` succeeded after signing the GitHub CLI in as `MuhammadZahab` (push permission confirmed via
  the API); `git status -sb` shows `main...origin/main`.

<a id="tech-002"></a>
### TECH-002 — Configure Firebase (Analytics, Crashlytics, Remote Config)
- **Type:** Technical task · **Priority:** P0 · **Status:** In Progress · **Effort:** S
- **Problem (verified):** the Google Services and Crashlytics plugins apply only when `app/google-services.json`
  exists (`app/build.gradle.kts`); without it analytics events are dropped, crashes aren't reported and the
  ladder is always the bundled default.
- **Solution:** create the Firebase project, add `google-services.json` (git-ignored, provided to CI as a
  secret), create Remote Config `tower_ladder`, confirm Crashlytics mapping upload in release.
- **Benefit:** crash visibility and product metrics for a live app.
- **Dependencies / risks:** Firebase console access.
- **Acceptance criteria:**
  1. A forced test crash in a release build appears in Crashlytics with a de-obfuscated stack trace.
  2. `onboarding_completed` appears in DebugView; no event parameter contains a number of steps or floors.
  3. Changing `tower_ladder` in the console changes the next tower's target on the next launch (device).
- **Progress (2026-11-21):** Firebase project `skyscrapper-a675f` has Android apps `ai.idraak.stepskyscraper` and
  `ai.idraak.stepskyscraper.debug`; `app/google-services.json` (both clients, git-ignored) is in place on the dev
  machine; Remote Config version 1 published with `tower_ladder` = `10,25,50,100,160,250` (String) and `ads_enabled` =
  `false` (Boolean). Pixel 6a debug: `FirebaseApp initialization successful`, Crashlytics initialised. Criteria 1–3
  not yet checked (test crash, DebugView, ladder change). CI still needs the file as a secret.

<a id="tech-003"></a>
### TECH-003 — Real AdMob ids and UMP consent message
- **Type:** Technical task · **Priority:** P0 · **Status:** In Progress (code side done; live ids and console steps pending the user) · **Effort:** S
- **Problem (verified):** `gradle.properties` defaulted `ADMOB_APP_ID` / `ADMOB_REWARDED_UNIT_ID` to Google's
  public sample ids; shipping them earns nothing and breaks ad policy.
- **Solution:** create the app and a rewarded unit; set the ids in `~/.gradle/gradle.properties` or CI; create
  GDPR and US-state consent messages; fail the release build when sample ids are used.
- **Benefit:** a working, policy-compliant rewarded ad.
- **Dependencies / risks:** AdMob console; Play app listing for app-ads.txt.
- **Acceptance criteria:**
  1. `./gradlew :app:assembleRelease` fails if the sample app id is configured.
  2. On a device in EEA debug geography, consent is collected before the first ad request (logcat).
  3. Watching the ad doubles the newest tower's Blueprints once; a second claim shows "already doubled".
- **Progress (2026-11-21, commits `280e1b3`, `fce3977` and the release-readiness review):** per-variant ids (debug and
  baseline-profile variants always use Google's test ids; only `release` reads `ADMOB_APP_ID_PROD` /
  `ADMOB_REWARDED_UNIT_ID_PROD`); `verifyReleaseAdMob` fails `bundleRelease` when the live ids are missing, malformed or
  Google's sample ids (criterion 1 is met for `bundleRelease`, the upload path; `assembleRelease` deliberately still
  builds with ads disabled for smoke tests); Remote Config `ads_enabled` (default false) gates every SDK call; consent
  only on tap (BUG-007); UMP privacy options row in Help & privacy (`AdMobPrivacyChoices`). Details:
  [TECHNICAL.md §5.1](TECHNICAL.md#51-admob-and-the-ads_enabled-switch). Verified: `AdsGatingTest` (20 tests incl.
  privacy choices), `RemoteConfigAdsSwitchTest`, `SkylineRewardGatingTest`; Pixel 6a debug logs only the test unit;
  R8 release without ids logs `gate mode=NotConfigured` and makes no consent, init or ad request.
- **Outstanding (user):** create the AdMob app and rewarded unit, set the `_PROD` ids, publish GDPR and US-state
  messages, register test devices, `app-ads.txt`, `ads_enabled` in Remote Config; then criteria 2–3 on a device
  (a tower topped out within 24 h is needed for the reward card).

<a id="tech-004"></a>
### TECH-004 — Create Play billing products and test purchases
- **Type:** Technical task · **Priority:** P0 · **Status:** Planned · **Effort:** S
- **Problem (verified):** product ids `architect_pass` (base plans `monthly`, `annual` + trial offer) and
  `architect_pass_lifetime` are hard-coded in `core/billing/.../RevenueCatRepository.kt` (`PassProducts`; purchases go
  through RevenueCat since [TECH-014](#tech-014)); none exist in a Play
  Console, so purchase, trial, acknowledgement and restore are untested (unverified area).
- **Solution:** create the products in Play Console, add license testers, run each flow on a device. The RevenueCat
  side (products, entitlement, offering) is already set up ([TECH-014](#tech-014)).
- **Benefit:** revenue path works on day one.
- **Dependencies / risks:** an app uploaded to a testing track ([TECH-005](#tech-005)).
- **Acceptance criteria:**
  1. Yearly with trial, monthly and lifetime purchases each unlock premium items immediately (device).
  2. Purchases are acknowledged (no auto-refund after 3 days on the test track).
  3. Reinstall + Restore restores premium; a cancelled test subscription removes it after expiry.
- **Progress (2026-11-21):** Play Console checked: the app "SkyScrapper" (`ai.idraak.stepskyscraper`) exists as a
  Draft in the IdraakAI developer account; Monetize › Subscriptions shows only "Upload a new APK", so no product can be
  created until a signed AAB with the billing permission is on a testing track. That upload needs
  [TECH-005](#tech-005) (upload key) and, because of the `bundleRelease` guards, live AdMob ids
  ([TECH-003](#tech-003)) and an https `PRIVACY_POLICY_URL` ([TECH-011](#tech-011)).

<a id="tech-005"></a>
### TECH-005 — Release signing
- **Type:** Technical task · **Priority:** P0 · **Status:** In Progress (waiting for the upload key) · **Effort:** XS
- **Problem (verified):** `signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")`
  (`app/build.gradle.kts:55`): without `keystore.properties` the release build is signed with the debug key.
- **Solution:** create an upload key, enrol in Play App Signing, add `keystore.properties` locally and as CI
  secrets; make the release build fail instead of falling back to debug signing.
- **Benefit:** an uploadable build and no accidental debug-signed releases.
- **Dependencies / risks:** keep the upload key out of Git.
- **Acceptance criteria:**
  1. `./gradlew :app:bundleRelease` without `keystore.properties` fails with a clear message.
  2. With it, the AAB is accepted by the Play internal testing track.
- **Progress (2026-11-20):** build side done in `app/build.gradle.kts`: the upload key comes from
  `keystore.properties` or the `STEPSKY_*` environment variables (template: `keystore.properties.example`);
  there is no debug-key fallback (release is left unsigned); `verifyReleaseSigning` makes `bundleRelease` fail
  with instructions. Criterion 1 verified: `bundleRelease` without a key fails with "Release signing is not
  configured…"; `assembleRelease` still produces `app-release-unsigned.apk`; with a throwaway key passed via the
  environment variables, `bundleRelease` produced an AAB that `jarsigner -verify` accepted. Outstanding: create
  the real upload key (you choose the passwords; see [play-console-checklist.md](play-console-checklist.md)),
  enrol in Play App Signing and upload to internal testing (criterion 2).

<a id="tech-006"></a>
### TECH-006 — Generate the baseline profile and measure startup
- **Type:** Technical task · **Priority:** P1 · **Status:** Planned · **Effort:** S
- **Problem (verified):** `app/src/main/baseline-prof.txt` is hand-seeded; startup was observed once (`am start -W`
  TotalTime ≈ 1.5 s after a reinstall, on an emulator) but not benchmarked.
- **Solution:** run `:app:generateBaselineProfile` on a physical device, commit the profile, run
  `StartupBenchmark` with and without it.
- **Benefit:** faster first launch, which matters for the 30-second "aha" target.
- **Dependencies / risks:** a physical device.
- **Acceptance criteria:**
  1. A generated profile is committed under `app/src/release/generated/`.
  2. Cold-startup median with the profile is reported and at least 10% better than `CompilationMode.None`.

<a id="tech-012"></a>
### TECH-012 — Design parity pass on a physical phone
- **Type:** Technical task · **Priority:** P1 · **Status:** In Progress · **Effort:** M
- **Problem (verified):** parity with the Claude Design had only been checked with Robolectric screenshots and an
  emulator.
- **Solution:** walk every item in [design-inventory.md §4](design-inventory.md) on a Pixel 6a (debug build),
  compare each with the rendered design frame at the same dp scale, fix differences or record deviations. States
  that need data are seeded by editing the debug app's Room database over `adb run-as` (no app code); times of
  day use the debug-only clock shift (`app/src/debug/.../DebugClock.kt`).
- **Progress (2026-11-21):** every screen in the inventory compared on the phone (see the *(device …)* notes in
  [design-inventory.md](design-inventory.md)). Fixed: CSS line boxes for numbers / names / headlines
  (`cssLineBox`), number + unit baseline, card line heights, ceremony reward sentence, step-help card heights,
  achievement lock copy, BUG-006, BUG-009, BUG-011…015. Deviations D18–D23 recorded.
- **Outstanding:** ([BUG-010](BUGS.md#bug-010) fixed: outline capped 10 floors ahead.) On-device checks not done because they need
  changes to the user's phone: widgets 11a on the home screen, font scale 200%, TalkBack, Health Connect missing /
  permission revoked; 9a plan cards (Play products, TECH-004); the "Synced. N floors added." snackbar (needs real
  new steps); BUG-006 at exactly 0 steps.
- **Acceptance criteria:**
  1. Every inventory item has a design/device screenshot pair from the phone.
  2. Differences fixed or listed in [design-deviations.md](design-deviations.md).
  3. `build test lint` green; Roborazzi baselines re-recorded where UI changed.

<a id="tech-007"></a>
### TECH-007 — Release-build smoke test of every screen on devices
- **Type:** Technical task · **Priority:** P1 · **Status:** Planned · **Effort:** S
- **Problem (verified):** the R8 release build was exercised only through onboarding, Build and Me on one
  emulator; Skyline, Customise, Share (export and share sheet), Paywall, widgets, ceremony and Step help weren't
  (unverified area).
- **Solution:** a scripted checklist on two devices (Android 9 and Android 14+, one Samsung), with seeded steps
  to reach a topping-out.
- **Benefit:** catches R8 keep-rule and OEM problems before testers do.
- **Dependencies / risks:** TECH-002…005 for real services.
- **Acceptance criteria:**
  1. Every screen in [design-inventory.md §4](design-inventory.md) opened in the release build with no crash
     (Crashlytics empty).
  2. A share PNG at 1080×1920 and 1080×1080 is produced and opens in another app.
  3. Both widgets render and update after a sync.

<a id="tech-008"></a>
### TECH-008 — CI with screenshot verification
- **Type:** Technical task · **Priority:** P2 · **Status:** Planned · **Effort:** S
- **Problem (verified):** Roborazzi screenshots are recorded into `*/src/test/screenshots/` but `build` doesn't
  run `verifyRoborazziDebug`; there is no CI configuration.
- **Solution:** a CI workflow running `./gradlew build test lint verifyRoborazziDebug` on each push, with
  JDK 21 and SDK 37.2.
- **Benefit:** visual and logic regressions caught automatically.
- **Dependencies / risks:** [TECH-001](#tech-001); font rendering must match between CI and local machines.
- **Acceptance criteria:**
  1. CI fails when a screenshot changes and passes after re-recording.
  2. CI is green on the main branch.

<a id="tech-009"></a>
### TECH-009 — Tests for steps, billing, ads and widget
- **Type:** Technical task · **Priority:** P2 · **Status:** In Progress · **Effort:** M
- **Progress (2026-11-21):** `core:ads` now has `AdsGatingTest` / `AdMobPrivacyChoicesTest` behind the `AdSdk` seam.
  `core:steps`, `core:billing` and `:widget` still have none.
- **Problem (verified):** `core:steps`, `core:billing`, `core:ads` and `:widget` had no tests (their test
  folders only hold `robolectric.properties`).
- **Solution:** tests for Health Connect result mapping and source selection, entitlement mapping from purchase
  lists (lifetime, subscription, pending), ad gateway state transitions with fakes, widget model loading.
- **Benefit:** confidence in the paths that touch money and data sources.
- **Dependencies / risks:** wrap SDK classes behind small seams where needed (no behaviour change).
- **Acceptance criteria:**
  1. Each of the four modules has at least one meaningful test class; `./gradlew test` passes.
  2. Entitlement tests cover lifetime, active subscription, pending and none.

<a id="tech-010"></a>
### TECH-010 — Record non-fatal errors in Crashlytics
- **Type:** Technical task · **Priority:** P3 · **Status:** Planned · **Effort:** XS
- **Problem (verified):** `CrashReporter` in `core/analytics` is never injected; sync and billing failures are
  only stored or shown, not reported.
- **Solution:** report unexpected `DomainError`s (not permission or user-cancel cases) through `CrashReporter`,
  without step data.
- **Benefit:** visibility of silent failures in the field.
- **Dependencies / risks:** [TECH-002](#tech-002).
- **Acceptance criteria:**
  1. A forced `StepSourceFailed` appears as a non-fatal in Crashlytics with no health values.

<a id="tech-011"></a>
### TECH-011 — Privacy policy, Health apps declaration, Data safety
- **Type:** Technical task · **Priority:** P0 · **Status:** In Progress (drafts done; hosting and console steps pending the user) · **Effort:** S
- **Problem (verified):** `PRIVACY_POLICY_URL` is empty, so the policy link is hidden; Health Connect apps
  need a hosted policy and the Health apps declaration before release. Answers are drafted in
  [play-console-checklist.md](play-console-checklist.md).
- **Solution:** host the policy, set `PRIVACY_POLICY_URL`, complete the Health apps declaration (READ_STEPS,
  background read) and the Data safety form.
- **Benefit:** passes Play review; users can read the policy in-app and from Health Connect.
- **Dependencies / risks:** a website or hosting; Play Console.
- **Acceptance criteria:**
  1. Help & privacy shows "Read the full privacy policy" and it opens the hosted page (device).
  2. The Health Connect permission screen's privacy link opens the in-app rationale (device).
  3. Play Console shows no outstanding policy declarations.
- **Progress (2026-11-21):** policy text drafted in [privacy-policy.md](privacy-policy.md) from the final code;
  Data safety, ads, advertising-ID, target-audience and Health apps answers updated in
  [play-console-checklist.md](play-console-checklist.md); `verifyReleasePrivacyPolicy` makes `bundleRelease` fail
  until `PRIVACY_POLICY_URL` is an https URL; the Health Connect rationale intents now open Help & privacy
  ([BUG-018](BUGS.md#bug-018)). Outstanding: fill the placeholders, host the policy, set the URL, complete the forms,
  then criteria 1–3 on a device and in Play Console.

<a id="tech-013"></a>
### TECH-013 — Play release readiness review
- **Type:** Technical task · **Priority:** P0 · **Status:** Done · **Effort:** S
- **Problem:** before the first upload, the integrated release-prep code had not been checked against current Google
  Play requirements as a whole.
- **Solution:** review target API, version, R8 rules, signing and AdMob guards, the merged-manifest permissions, Health
  Connect rationale, privacy policy, declarations, listing and backup; fix what is detectable in code; record the
  rest for the user in [RELEASE-READINESS.md](RELEASE-READINESS.md).
- **Acceptance criteria:**
  1. Every area above has a status and verification steps in RELEASE-READINESS.md.
  2. Detectable code blockers fixed; `ktlintFormat build test lint` green.
- **Resolution (2026-11-21):** target API 37 meets Play's API 36 requirement (Play Console Help, checked 2026-11-21);
  fixed: Health Connect rationale intents ignored ([BUG-018](BUGS.md#bug-018)); missing UMP privacy options entry
  point (Help & privacy › Ad privacy choices, `AdPrivacyChoices`); unused `FOREGROUND_SERVICE` removed from the merged
  manifest; `verifyReleasePrivacyPolicy` added to `bundleRelease`; `verifyReleaseAdMob` message pointed at the docs.
  Drafted [privacy-policy.md](privacy-policy.md) and updated the console answers.
- **Verification:** `aapt2 dump permissions` / `badging` on the release APK (targetSdk 37, versionCode 1, permission list
  in [TECHNICAL.md §6](TECHNICAL.md#6-build-signing-and-release)); new unit tests `AdMobPrivacyChoicesTest` (2) and
  `MeTest.adPrivacyChoicesFollowTheUmpRequirement`; `ktlintFormat build test lint` green. Device checks of the two
  UI changes are listed in RELEASE-READINESS.md.

<a id="tech-014"></a>
### TECH-014 — Architect Pass purchases through RevenueCat
- **Type:** Technical task · **Priority:** P0 · **Status:** In Progress (code and RevenueCat setup done; Play side pending) · **Effort:** S
- **Problem:** the user chose RevenueCat for subscriptions (receipt validation, analytics, paywall experiments
  later); the app called Google Play Billing directly (`PlayBillingRepository`).
- **Solution:** `core:billing` `RevenueCatRepository` (RevenueCat Android SDK 11.2.0) behind the unchanged
  `EntitlementRepository`; same Play product ids; RevenueCat entitlement `architect_pass` and the `default` offering
  (`$rc_monthly`, `$rc_annual`, `$rc_lifetime`). Key from `REVENUECAT_API_KEY` (Gradle property or env var, outside
  Git); `verifyReleaseRevenueCat` fails `bundleRelease` without a `goog_` key.
- **Benefit:** server-side validation and acknowledgement, cross-device restore, revenue charts.
- **Dependencies / risks:** RevenueCat needs a Play service-account key to validate purchases; products exist only after
  the first internal-testing upload ([TECH-005](#tech-005), [TECH-004](#tech-004)). Adds RevenueCat as a data processor
  (privacy policy, Data safety).
- **Acceptance criteria:**
  1. Paywall, Me and premium unlocks behave as before (no UI change); `EntitlementRepository` unchanged.
  2. Without the key the build works and purchases are off; `bundleRelease` fails with instructions.
  3. With the key, a debug build configures RevenueCat and fetches the `default` offering (logcat).
  4. On the internal-testing track, monthly, annual (trial) and lifetime purchases and Restore set the entitlement
     ([TECH-004](#tech-004)).
- **Progress (2026-11-21):** RevenueCat project "SkyScrapper": Play app `ai.idraak.stepskyscraper`, entitlement
  `architect_pass` with `architect_pass:monthly`, `architect_pass:annual` (not backwards compatible) and
  `architect_pass_lifetime`, all three on the `default` offering. Code as above; a failed offerings fetch no longer
  blocks the entitlement refresh. Verified: `ktlintCheck`, paywall/data/billing tests, lint, debug and R8 release
  builds; Pixel 6a debug: RevenueCat configured and fetched `default`, Play answered `PRODUCT_NOT_FOUND` (expected until
  the products exist), entitlement refresh still ran. AC 1–3 met; AC 4 pending.
- **Outstanding (user):** create the Play service account, grant it in Play Console and upload its JSON in RevenueCat
  (App › Service account credentials); connect Google developer notifications; then TECH-004.

<a id="prod-001"></a>
### PROD-001 — Browse every district in Skyline
- **Type:** Product improvement · **Priority:** P2 · **Status:** Planned · **Effort:** M
- **Problem (verified):** `SkylineReducer.districtTowers` returns only the current (or last) district
  (`feature/skyline/.../SkylineViewModel.kt:106-117`); once a ninth tower is started, District 1 is no longer
  visible. The design says districts "sit together as the city grows".
- **Solution:** a district switcher (chips or horizontal paging) in the Skyline header, keeping the current
  district as default.
- **Benefit:** the long-term collection stays visible, which is the main return driver after month one.
- **Dependencies / risks:** performance with many towers (measure).
- **Acceptance criteria:**
  1. With 10 finished towers, both districts can be opened and show 8 and 2 towers (UI test).
  2. Frame time stays under 16 ms while panning a full district (macrobenchmark or profiler).

<a id="prod-002"></a>
### PROD-002 — Re-skin finished towers from the stats sheet
- **Type:** Product improvement · **Priority:** P3 · **Status:** Planned · **Effort:** S
- **Problem (verified):** Customise is reachable only from Build (active tower); the design says finished towers
  "can be renamed and re-skinned", but the stats sheet offers Share and Rename only.
- **Solution:** a "Customise" action in the stats sheet that opens `CustomiseRoute(towerIndex)`.
- **Benefit:** more use for Blueprints and premium styles.
- **Dependencies / risks:** none; `CustomiseTowerUseCase` already accepts any index.
- **Acceptance criteria:**
  1. Changing a finished tower's facade from Skyline updates its skyline rendering (UI test).

<a id="prod-003"></a>
### PROD-003 — Remaining motion details from the design
- **Type:** Product improvement · **Priority:** P3 · **Status:** Planned · **Effort:** S
- **Problem (verified):** inventory items marked `[~]`: no crane swing on 1a, no lift-off before re-styling on 1d,
  discrete sky switch instead of the 20-minute blend, no haptics on Build floor drops or "Target met".
- **Solution:** implement each with Compose animation APIs; respect "Remove animations".
- **Benefit:** closer match to the design's feel.
- **Dependencies / risks:** none.
- **Acceptance criteria:**
  1. Each item is visible in a screen recording; with animator scale 0 only 150 ms crossfades remain.
  2. The four inventory items are changed to `[x]`.

<a id="prod-004"></a>
### PROD-004 — "Equip" shortcut and Customise sticky thumbnail
- **Type:** Product improvement · **Priority:** P3 · **Status:** Planned · **Effort:** S
- **Problem (verified):** the Achievements "Equip" chip is a label only; Customise has no 64 dp sticky
  thumbnail when expanded (deviation D16).
- **Solution:** make Equip open Customise on the reward's tab with it pre-selected; add the collapsing preview.
- **Benefit:** rewards get used, which reinforces achievements.
- **Dependencies / risks:** none.
- **Acceptance criteria:**
  1. Tapping Equip opens Customise with the reward selected (UI test).

<a id="prod-005"></a>
### PROD-005 — Localise cosmetic and achievement names
- **Type:** Technical task · **Priority:** P3 · **Status:** Planned · **Effort:** S
- **Problem (verified):** names live as English literals in `CosmeticCatalog` and `AchievementCatalog`
  (domain module), not in string resources (deviation P16).
- **Solution:** keep stable ids in the domain; map ids to string resources in `core:ui`.
- **Benefit:** the app becomes translatable.
- **Dependencies / risks:** don't change the ids (they are stored in Room and the wallet keys).
- **Acceptance criteria:**
  1. No user-visible cosmetic or achievement text comes from domain literals (grep + lint).

<a id="prod-006"></a>
### PROD-006 — Decide first-tower height and premium-bonus rules
- **Type:** Product decision · **Priority:** P2 · **Status:** In Progress · **Effort:** XS
- **Problem:** the design review left open whether the first tower should be short for a quick first
  topping-out (the ladder starts at 10); Blueprint bonus rules for the Pass and the rewarded ad are ambiguous
  ([BUG-002](BUGS.md#bug-002)).
- **Solution:** decide the ladder (Remote Config can test it) and whether the Pass bonus is retroactive and
  stackable with the ad.
- **Benefit:** a consistent economy and an informed first-session experience.
- **Dependencies / risks:** analytics ([TECH-002](#tech-002)) for a ladder test.
- **Acceptance criteria:**
  1. The decision is written in this card and [PRODUCT.md §9](PRODUCT.md#9-monetisation-strategy).
- **Decided (2026-11-21, BUG-002's recommended fix):** the Pass bonus applies only to towers topped out while a
  member (not retroactive), and the Pass and rewarded-ad bonuses exclude each other (max 2× per tower). Implemented
  in [BUG-002](BUGS.md#bug-002) and recorded in PRODUCT.md §9. **Still open:** first-tower height (ladder).

<a id="grow-001"></a>
### GROW-001 — Activation and retention dashboards
- **Type:** Growth task · **Priority:** P2 · **Status:** Planned · **Effort:** S
- **Opportunity:** events for onboarding, step access, target, widget, topping-out, paywall, purchases, ads and
  shares already exist (`core/domain/.../model/Analytics.kt`) but nothing reads them.
- **Solution:** Firebase funnels for onboarding → first floor → first topping-out; D1/D7/D30 cohorts; paywall
  funnel; add a `first_floor` event if needed (no step values).
- **Benefit:** decisions based on data instead of guesses.
- **Dependencies / risks:** [TECH-002](#tech-002).
- **Acceptance criteria:**
  1. The dashboards show the metrics in [PRODUCT.md §10](PRODUCT.md#10-product-metrics-to-track) for internal
     testers.

<a id="grow-002"></a>
### GROW-002 — Store listing from share cards
- **Type:** Growth task · **Priority:** P2 · **Status:** Planned · **Effort:** S
- **Opportunity:** the share cards and screenshot tests already produce outcome-focused images.
- **Solution:** listing screenshots (tower growing, ceremony, skyline, widget), feature graphic, description
  that explains Health Connect use plainly.
- **Benefit:** discoverability and conversion from the store page.
- **Dependencies / risks:** [TECH-007](#tech-007).
- **Acceptance criteria:**
  1. The listing is published on the closed-testing track with at least 5 screenshots.

<a id="grow-003"></a>
### GROW-003 — Paywall price and trial experiments
- **Type:** Product idea (optional) · **Priority:** P3 · **Status:** Planned · **Effort:** M
- **Opportunity:** analysis: prices ($2.99 / $14.99 / $29.99) come from the design, not data.
- **Solution:** Play pricing experiments or regional pricing once there is paywall traffic.
- **Benefit:** better revenue per user.
- **Dependencies / risks:** [TECH-004](#tech-004), [GROW-001](#grow-001); needs approval.
- **Acceptance criteria:**
  1. An experiment runs to its planned sample size and the decision is recorded here.

<a id="grow-004"></a>
### GROW-004 — Policy-safe interstitial ad (post-launch, optional)
- **Type:** Product idea (optional) · **Priority:** P3 · **Status:** Planned (do **not** build before launch) ·
  **Effort:** M
- **Opportunity:** a second ad format could add revenue once there is real traffic. Decided 2026-11-21: no
  interstitial in 1.0. A new AdMob account is at its most fragile (limited ad serving, invalid-traffic review), the
  app has almost no natural breaks outside the ad-free ceremony, and a full-screen ad in a short daily check-in hurts
  retention and the Architect Pass.
- **Solution (only if the gate below passes):** one interstitial at a single user-initiated natural break, for example
  after the Share flow is finished and closed, or when leaving the weekly detail screen. Same consent path as the
  rewarded ad (UMP, `AdsGate`), its own Remote Config switch.
- **Gate before starting:** AdMob app approved, GDPR and US-state messages live, at least 4 weeks of clean rewarded-ad
  data (no invalid-traffic or policy warnings in AdMob), and [GROW-001](#grow-001) retention data to compare against.
- **Hard rules (from AdMob interstitial guidance, Play's disruptive-ads policy and this project's rules):** never at
  app launch or exit, on a tab switch, when opening from a notification or widget, or on Build, onboarding, the
  topping-out ceremony, the paywall or Help & privacy; never mid-interaction or right after an unexpected tap; no
  loading screen before it; never for Architect Pass members; no step or floor data in requests or logs.
- **Dependencies / risks:** [TECH-003](#tech-003), [GROW-001](#grow-001). Risk: account suspension if placement or
  frequency is wrong; retention and conversion loss. Needs the user's approval to start.
- **Acceptance criteria:**
  1. Remote Config `interstitial_enabled` (Boolean, default `false`) gates every load and show; off, missing or
     unfetched → no interstitial, and a loaded one is discarded.
  2. Frequency caps: at most 1 per session, at least 5 minutes apart, none in the first session or the first 3 days
     after install, none within 2 minutes of a rewarded ad. Unit-tested.
  3. The ad is preloaded and shown only if already loaded; otherwise the transition continues with no delay.
  4. Shown only at the chosen natural break; tests prove it never shows on the forbidden screens or moments above.
  5. Debug and non-release variants use Google's interstitial test id only; `verifyReleaseAdMob` also checks the live
     interstitial unit id.
  6. Pass members never see it; Data safety, privacy policy and [RELEASE-READINESS.md](RELEASE-READINESS.md) updated.
  7. After a staged rollout (Remote Config percentage), day-7 retention and Pass conversion are not worse than the
     control group by more than an agreed threshold; the decision is recorded here.
