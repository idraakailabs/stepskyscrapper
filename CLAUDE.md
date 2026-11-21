# CLAUDE.md — Step Skyscraper

Android app (Kotlin, Jetpack Compose, Material 3) that turns walking into building: every 1,000 steps adds a
floor to an isometric skyscraper. Offline-first, no accounts, steps read from Health Connect (sensor fallback)
and never leave the device. Version 1.0.0, built but not yet published. Multi-module clean architecture with
MVI, Hilt, Room, DataStore, WorkManager, Glance, RevenueCat (on Play Billing), AdMob and Firebase.

## Project documentation — read before changing anything

| Document | Use it for |
|---|---|
| [docs/ROADMAP.md](docs/ROADMAP.md) | **Start here.** Current phase, next task, every task ID with priority, status, dependencies and acceptance criteria |
| [docs/BUGS.md](docs/BUGS.md) | Every known defect: evidence, impact, fix, acceptance criteria, resolution history |
| [docs/TECHNICAL.md](docs/TECHNICAL.md) | Architecture, data flows, storage, config, build and release, implementation guidelines |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Vision, audience, current feature baseline, product analysis |
| [docs/design-inventory.md](docs/design-inventory.md) | Checklist of every design token, component, screen and state, with implementation status |
| [docs/design-deviations.md](docs/design-deviations.md) | Every intentional difference from the design and why |
| [docs/RELEASE-READINESS.md](docs/RELEASE-READINESS.md) | Release Readiness Sheet: every pre-upload check with its status and what the user still has to do |
| [docs/play-console-checklist.md](docs/play-console-checklist.md) | Release steps, signed AAB, Health apps declaration, Data safety answers |
| [docs/privacy-policy.md](docs/privacy-policy.md) | Privacy policy draft to host (keep in step with the Data safety answers) |
| [docs/terms-of-service.md](docs/terms-of-service.md) | Terms of Service draft to host (keep in step with the paywall and Pass plans) |

The "Current position" table in `docs/ROADMAP.md` is the source of truth for the active phase and next task.
The design source files are in `design/source/` (`Step Skyscraper All.dc.html`, `tower.js`).

## Working rules

1. **Follow the roadmap.** Work within the active phase, in priority order, respecting dependencies. Work
   that isn't on the roadmap gets a new task entry (next free ID, correct phase).
2. **Preserve existing behaviour.** Don't change user-visible behaviour, remove features or change stored
   data formats unless a roadmap task or explicit user approval covers it.
3. **Protect user data.** Progress is derived from stored daily totals, never accumulated. Room changes need a
   version bump and a migration (schemas in `core/database/schemas/`). Never rename cosmetic or achievement ids
   or wallet key prefixes; they are stored.
4. **No secrets in Git.** `app/google-services.json`, `keystore.properties`, the `keystore/` folder, `local.properties` and real AdMob
   ids stay out of the repo (gradle properties or CI secrets).
5. **Privacy is a product rule.** No step or floor values in analytics, ads, logs or crash reports. No ads on
   Build, onboarding or the topping-out ceremony.
6. **Match existing patterns.** MVI per screen (Route + stateless Screen + reducer), features depend only on
   `core:domain`, `core:ui`, `core:designsystem`; design tokens only (no raw colours, dp or text styles in
   features). See [docs/TECHNICAL.md](docs/TECHNICAL.md#8-implementation-guidelines).
7. **Release safety.** Check R8 keep rules for serialised routes and Glance receivers; exercise changed screens
   in a release build.

## Required workflow for every task

**Before implementation**
1. Read this file and the relevant sections of the docs above.
2. Identify the task ID and its acceptance criteria, or create a new entry.
3. Set its status to `In Progress`.

**After implementation**
1. Run the relevant checks: `./gradlew build test lint` (run `./gradlew ktlintFormat` first). Add or update
   tests for changed logic; re-record screenshots with `./gradlew recordRoborazziDebug` when UI changes.
2. Verify each acceptance criterion and note how.
3. Update the documentation (rule below).
4. Report the code changes, verification results, documentation updates and remaining work.

## Mandatory documentation maintenance rule

This is a permanent project rule for every session.

- **Every change that implements, modifies, fixes, removes or refactors behaviour must update the affected
  documentation in the same task.** Update only what the change affects:
  - `docs/ROADMAP.md`: statuses, dependencies, new tasks, the "Current position" table.
  - `docs/BUGS.md`: status, **Resolution** and **Verification**. Never delete entries.
  - `docs/TECHNICAL.md`: architecture, dependencies, data flow, storage, config, build or important
    decisions.
  - `docs/PRODUCT.md`: the current-feature baseline when user-visible behaviour changes. Never list planned
    work as existing.
  - `docs/design-inventory.md` / `docs/design-deviations.md`: when design coverage or a deviation changes.
- **Definition of done:** not complete until the docs are updated, or until you've stated why no
  documentation change is needed.
- **Status honesty:** `Done` only after the acceptance criteria are verified. Otherwise use
  `Fixed – unverified` or `In Progress`, and say what's outstanding.
- **Facts vs plans:** keep implemented behaviour, plans, assumptions and unverified claims separate. Never
  invent metrics, test results or completed work.
- **Small, accurate edits:** don't rewrite whole documents; keep IDs stable.
- **Enforcement:** never knowingly leave docs outdated after a code change. If you can't update them,
  report why and what's outstanding.

## Useful facts

- AGP is pinned to **9.2.1**, the newest version Android Studio accepts; don't upgrade it past what Studio
  supports. Build needs JDK 17+ (21 used) and Android SDK platform **37.2** (Compose 1.13 requires compileSdk ≥ 37.1).
- Without `app/google-services.json`, Firebase is skipped: analytics are dropped and the bundled ladder is used
  (Firebase project `skyscrapper-a675f`; the file is git-ignored). Purchases need `REVENUECAT_API_KEY` (public
  `goog_…` key, kept in `~/.gradle/gradle.properties`); without it purchases are off and `bundleRelease` fails.
- AdMob: debug (and every non-`release` variant) always uses Google's test ids, hard-coded. Only `release` uses live
  ids from `ADMOB_APP_ID_PROD` / `ADMOB_REWARDED_UNIT_ID_PROD` (`~/.gradle/gradle.properties` or CI env, never in Git);
  the old `ADMOB_APP_ID` / `ADMOB_REWARDED_UNIT_ID` properties are gone. Remote Config `ads_enabled` (default false)
  switches live ads on. See [docs/TECHNICAL.md §5.1](docs/TECHNICAL.md#51-admob-and-the-ads_enabled-switch).
- RevenueCat: `release` uses only `REVENUECAT_API_KEY` (`goog_…`); debug uses the Test Store key
  `REVENUECAT_TEST_API_KEY` (`test_…`, simulated purchases, no Play) when set. Both in `~/.gradle/gradle.properties`.
  Never let a `test_` key reach `release` (the SDK crashes). See [docs/TECHNICAL.md §5.2](docs/TECHNICAL.md#52-purchases-revenuecat).
- `bundleRelease` fails by design unless the upload key (`keystore.properties`, `STEPSKY_*` in `local.properties` or env vars), valid live
  AdMob ids and an https `PRIVACY_POLICY_URL` are configured (`verifyReleaseSigning`, `verifyReleaseAdMob`,
  `verifyReleasePrivacyPolicy`); `assembleRelease` still builds an unsigned, ads-off APK for smoke tests.
- Git: `develop` is the integration branch and `main` is release-only (the user decides merges); remote `origin` = `https://github.com/idraakailabs/stepskyscrapper.git`. Line endings are LF
  (`.gitattributes`). Commit email for this repo: zabiirana27@gmail.com (local git config).
- Component gallery: Settings › "Component gallery (debug)" in debug builds.
