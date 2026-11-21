# Step Skyscraper — Release Readiness Sheet

> **Scope:** go / no-go status for the first Google Play upload of `ai.idraak.stepskyscraper` 1.0.0 (versionCode 1).
> How-to steps: [play-console-checklist.md](play-console-checklist.md) · Bugs: [BUGS.md](BUGS.md) · Tasks:
> [ROADMAP.md](ROADMAP.md) · Config and build: [TECHNICAL.md](TECHNICAL.md).
>
> **Reviewed:** 2026-11-21 on branch `claude/release-readiness-048453` (release integration + review fixes; not yet
> merged into `develop`). This sheet records what was checked. It does **not** guarantee that Google Play will accept
> the app; Play review decides that.

**Status key:** **Done** (checked, evidence given) · **Pending – user** (needs your keys, accounts or console) ·
**Manual verification** (needs a device or situation not available here) · **Blocked** (waiting on another item).

**Bottom line:** no known code blocker remains. The upload is blocked only on items that need you: upload key,
Firebase, AdMob ids and messages, the hosted privacy policy, and the Play Console forms.

## 1. Bug fixes and verification
- [x] **Done** — BUG-016 sync icon wobble, BUG-017 capsule ripple: fixed and checked on the Pixel 6a (frame
  measurements, long-press captures).
- [x] **Done** — BUG-008…015 (earlier parity pass), see [BUGS.md](BUGS.md).
- [ ] **Manual verification** — BUG-001 post-ceremony notification prompt (unit-tested; needs a fresh topping-out on a
  device). Steps: after the next topping-out, close the ceremony → the system dialog appears once, before any paywall.
- [ ] **Manual verification** — BUG-005 late-ceremony highlight (unit-tested). Steps: top out with the app closed,
  open it a day later, close the ceremony → Skyline pins that tower.
- [ ] **Manual verification** — BUG-003 Android 8–9 save (Android 16 verified). Steps: Android 9 emulator, Share ›
  Save → grant storage → "Saved to Pictures." without a second tap.
- [ ] **Manual verification** — BUG-007 consent only on tap (unit-tested; non-EEA path and tap → test ad verified on
  the Pixel 6a 2026-11-21). Remaining: the EEA form, once the AdMob app's GDPR message is published. Steps:
  TECHNICAL.md §5.1 "Testing ads safely" with `ADMOB_UMP_DEBUG_EEA=true`; open Skyline with a reward card → no form;
  tap → form.
- [ ] **Manual verification** — BUG-006 payoff at exactly 0 steps (unit-tested).
- [x] BUG-018 Health Connect privacy link → Help & privacy alone, Back → Health Connect (Pixel 6a, 2026-11-21). The
  hosted-policy link inside it appears once `PRIVACY_POLICY_URL` is set (TECH-011).
- [x] BUG-002 (Blueprint stacking) and BUG-004 (week card → weekly detail) fixed and verified 2026-11-21.

## 2. Critical functional and UI checks
- [x] **Done** — R8 release APK on the Pixel 6a: onboarding, Build, Skyline, Customise, Me, Share opened with no
  crash; debug build: sync chip, tabs, Share save, notification state (2026-11-21).
- [ ] **Manual verification** — full release smoke test on Android 9 and 14+ incl. paywall, widgets, ceremony, step
  help, Health Connect missing / revoked ([TECH-007](ROADMAP.md#tech-007)). Do it on the internal-testing build.
- [ ] **Manual verification** — widgets on the home screen, 200% font, TalkBack ([TECH-012](ROADMAP.md#tech-012)).

## 3. AdMob implementation and policy
- [x] **Done** — one opt-in rewarded ad on Skyline only; none on Build, onboarding, ceremony, paywall or widgets
  (only `SkylineViewModel` injects the gateway). Reward stated before the ad, granted only on `onUserEarnedReward`,
  Blueprints only, once per tower, hidden for Pass members. No custom targeting or step data in requests or logs.
- [x] **Done** — consent before any request (UMP → `MobileAds.initialize` → load), form only on tap,
  `DELAY_APP_MEASUREMENT_INIT`. Tests: `AdsGatingTest`.
- [x] **Done** — UMP privacy options entry point: Help & privacy › "Ad privacy choices" while ads are on and UMP
  requires it. Tests: `AdMobPrivacyChoicesTest`, `MeTest.adPrivacyChoicesFollowTheUmpRequirement`.
- [ ] **Manual verification** — the "Ad privacy choices" row and form with a published GDPR message in EEA debug
  geography.
- [ ] **Pending – user** — max ad content rating (optional product decision; SDK default today).

## 4. Debug vs prod ad configuration
- [x] **Done** — debug and baseline-profile variants always use Google's test ids (hard-coded); only `release` reads
  `ADMOB_APP_ID_PROD` / `ADMOB_REWARDED_UNIT_ID_PROD`. Device: debug logs `gate mode=Test … unit=…/5224354917`;
  release without ids logs `gate mode=NotConfigured` and makes no consent, init or ad request.
- [x] **Done** — `verifyReleaseAdMob` fails `bundleRelease` when ids are missing, malformed or Google's sample ids.
- [ ] **Pending – user** — create the AdMob app and rewarded unit; put the ids in `~/.gradle/gradle.properties` or CI.
  Verify: `./gradlew :app:verifyReleaseAdMob` passes.

## 5. Remote Config ad control and safe defaults
- [x] **Done** — `ads_enabled` Boolean, default `false`; missing, unfetched, failed fetch or no Firebase → no ads;
  debug ignores it; turning it off discards a loaded ad. Tests: `AdsGatingTest`, `RemoteConfigAdsSwitchTest`.
- [x] **Done** (2026-11-21) — Firebase Remote Config version 1 published: `ads_enabled` = false, `tower_ladder`. Switch
  ads on only after the AdMob app is approved and the UMP messages are published.
- [ ] **Manual verification** — release with live ids and `ads_enabled` true/false on a registered AdMob test device
  (TECHNICAL.md §5.1). Never tap a live ad on an unregistered device.

## 6. Release build variant and configuration
- [x] **Done** — `release`: R8 + resource shrinking; keep rules for `@Serializable` routes and Glance receivers;
  `SourceFile,LineNumberTable` kept. `./gradlew build` (includes `assembleRelease`) green on 2026-11-21.
- [x] **Done** — Crashlytics mapping upload: automatic for minified builds once `google-services.json` exists
  (Crashlytics plugin 3.0.8).
- [x] **Done** (2026-11-21) — `app/google-services.json` for Firebase project `skyscrapper-a675f` (release and debug
  apps) is on the dev machine, git-ignored; Firebase initialises on the Pixel 6a. Copy it to any other build machine.
- [x] **Done** (2026-11-21) — purchases go through RevenueCat ([TECH-014](ROADMAP.md#tech-014)); `REVENUECAT_API_KEY`
  is in `~/.gradle/gradle.properties`; `verifyReleaseRevenueCat` guards `bundleRelease`.
- [ ] **Pending – user** — RevenueCat › App › Service account credentials (Play service-account JSON) and Google
  developer notifications, so RevenueCat can validate purchases.
- [ ] **Manual verification** — forced test crash appears de-obfuscated in Crashlytics ([TECH-002](ROADMAP.md#tech-002)).

## 7. Version name and code
- [x] **Done** — `versionName 1.0.0`, `versionCode 1` (`aapt2 dump badging`). Raise `versionCode` for every later
  upload.

## 8. Signing and AAB generation
- [x] **Done** — no debug-key fallback; `verifyReleaseSigning`, `verifyReleaseAdMob` and
  `verifyReleasePrivacyPolicy` run before `bundleRelease` (`--dry-run` confirms). A throwaway-key AAB passed
  `jarsigner -verify` earlier ([TECH-005](ROADMAP.md#tech-005)).
- [ ] **Pending – user** — create the upload key and `keystore.properties`; enrol in Play App Signing; build with
  `./gradlew clean :app:bundleRelease` ([play-console-checklist.md §2](play-console-checklist.md#2-build-the-signed-aab)).

## 9. Target API and compatibility
- [x] **Done** — targetSdk 37, compileSdk 37.2, minSdk 26. Play requires API 36+ for new apps and updates since
  31 Aug 2026 (Play Console Help, checked 2026-11-21).
- [ ] **Manual verification** — Android 8/9 (sensor fallback), Android 13 (Health Connect app), Samsung device
  (play-console-checklist.md §9).

## 10. Permissions, privacy policy and Data safety
- [x] **Done** — merged release manifest reviewed, each permission justified
  ([TECHNICAL.md §6](TECHNICAL.md#6-build-signing-and-release)); unused `FOREGROUND_SERVICE` removed; no exact-alarm,
  location or foreground-service-type permissions.
- [x] **Done** — Health Connect: `READ_STEPS` + background read only; rationale intent and Android 14 alias now open
  Help & privacy (BUG-018).
- [x] **Done** — privacy policy drafted from the final code: [privacy-policy.md](privacy-policy.md); Data safety
  draft updated ([play-console-checklist.md §7](play-console-checklist.md#7-data-safety-form-draft)).
- [ ] **Pending – user** — fill the placeholders, get it reviewed, host at an https URL, set `PRIVACY_POLICY_URL`
  (`bundleRelease` fails until you do). Verify: Help & privacy shows "Read the full privacy policy" and it opens.
- [ ] **Pending – user** — Data safety form and Health apps declaration in Play Console.

## 11. Ads declaration and consent
- [ ] **Pending – user** — Play Console: "Contains ads" = Yes; advertising ID = Yes (Advertising or marketing).
- [ ] **Pending – user** — AdMob › Privacy & messaging: publish the GDPR and US-state messages; link the app to
  Play; host `app-ads.txt`.

## 12. Crash reporting and stability
- [x] **Done** — no crash in the R8 smoke test; `build test lint` green.
- [x] **Done** — Crashlytics initialises (Firebase configured 2026-11-21); non-fatal reporting is a later task
  ([TECH-010](ROADMAP.md#tech-010)).
- [ ] **Manual verification** — pre-launch report on the internal-testing upload has no crashes.

## 13. Store listing, screenshots and declarations
- [ ] **Pending – user** — name, short and full description (explain Health Connect use plainly), 512 px icon,
  1024×500 feature graphic, contact email, category Health & Fitness.
- [ ] **Pending – user** — phone screenshots: long side ≤ 2× short side, so Pixel 6a captures (1080×2400) and the
  Roborazzi images (720×1600) need cropping, or use a 1080×1920 emulator; demo data only, never your own history.
  Share cards (1080×1920) work as promo images ([GROW-002](ROADMAP.md#grow-002)).
- [ ] **Pending – user** — content rating (IARC questionnaire), target audience 13+ (no under-13 groups, so the
  Families policy doesn't apply), news / COVID / government / financial: No.
- [ ] **Pending – user** — billing products `architect_pass` (`monthly`, `annual` + 7-day trial) and
  `architect_pass_lifetime` in Play Console (already set up in RevenueCat), license testers
  ([TECH-004](ROADMAP.md#tech-004)).
- [x] **Done** — Auto Backup: only the Room database and the settings DataStore, for cloud backup and device transfer.

## 14. App access
- [x] **Done** — no login or account; declare "All functionality is available without special access".

## 15. Final pre-upload verification
- [ ] `./gradlew :app:verifyReleaseSigning :app:verifyReleaseAdMob :app:verifyReleasePrivacyPolicy` passes.
- [ ] `./gradlew clean :app:bundleRelease` succeeds with no `w: AdMob: … ads disabled` line; `jarsigner -verify` OK.
- [ ] `google-services.json` present; Crashlytics mapping upload ran in the build log.
- [ ] Install from internal testing; onboarding with Health Connect, Build sync, Skyline, Share, paywall prices load.
- [ ] `ads_enabled` still false until AdMob approval and UMP messages are live.
- [ ] Play Console › Dashboard shows no outstanding declarations; Data safety matches the hosted policy.
- [ ] Personal developer account: closed test with the required testers and days before applying for production.

## Action required from you
1. Upload key + `keystore.properties`; enrol in Play App Signing.
2. ~~Firebase project, `app/google-services.json`, Remote Config~~ (done 2026-11-21). RevenueCat: Play service-account
   JSON and developer notifications.
3. AdMob app and rewarded unit, `ADMOB_APP_ID_PROD` / `ADMOB_REWARDED_UNIT_ID_PROD`, GDPR + US-state messages, test
   devices, `app-ads.txt`.
4. Host the privacy policy and set `PRIVACY_POLICY_URL`.
5. Play Console: app content declarations (ads, advertising ID, Data safety, Health apps, target audience, content
   rating, app access), billing products, store listing assets.
6. Device checks after the next topping-out: BUG-001 prompt, BUG-005 highlight; BUG-007 EEA consent form (needs the
   published GDPR message), ad privacy choices; Android 8–9 save; BUG-006 on a fresh install with 0 steps.
