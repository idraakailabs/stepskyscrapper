# Play Console release checklist

> Practical steps for the first upload of `ai.idraak.stepskyscraper` 1.0.0 (versionCode 1). Overall status and
> sign-off: [RELEASE-READINESS.md](RELEASE-READINESS.md). The answers below are drafts based on the code as of
> 2026-11-21; check them against Play Console's current wording when you fill in the forms. Nothing here
> guarantees Play acceptance.

## 1. Before the first build (needs you)
- [x] **Firebase** (done 2026-11-21): project `skyscrapper-a675f` with Android apps `ai.idraak.stepskyscraper` and
  `ai.idraak.stepskyscraper.debug`; `google-services.json` in `app/` (git-ignored; copy it to any other build machine
  or CI secret). Remote Config `tower_ladder` (String, `10,25,50,100,160,250`) and `ads_enabled` (Boolean, **false**)
  published. Still to do: set the Analytics data-retention period you name in the privacy policy.
- [ ] **RevenueCat**: the project, Play app, entitlement `architect_pass`, products and `default` offering are set up
  (2026-11-21, [TECH-014](ROADMAP.md#tech-014)); `REVENUECAT_API_KEY=goog_…` is in `~/.gradle/gradle.properties` on the
  dev machine (add it to CI as a secret). Still to do: in Google Cloud create a service account, grant it in Play
  Console (Users and permissions: view financial data, manage orders and subscriptions), upload its JSON key in
  RevenueCat › App › Service account credentials, and connect Google developer notifications.
- [ ] **AdMob**: create the app and one *Rewarded* unit. Put the live ids in `~/.gradle/gradle.properties` (outside the
  repo) or CI secrets: `ADMOB_APP_ID_PROD=ca-app-pub-…~…` and `ADMOB_REWARDED_UNIT_ID_PROD=ca-app-pub-…/…`.
  In AdMob › Privacy & messaging, create and **publish** a GDPR message and a US-state regulations message. Register
  your own phones as test devices (Settings › Test devices). After the listing is live, link the app to Play and host
  `app-ads.txt` on the developer website named in the listing.
- [ ] **Privacy policy**: fill in the placeholders in [privacy-policy.md](privacy-policy.md), host it at a public https
  URL, and set `PRIVACY_POLICY_URL=https://…` in `gradle.properties` (this value is public, so committing it is fine).
  Done 2026-11-21: `PRIVACY_POLICY_URL` and `TERMS_URL` point at idraakai.org in the repo's `gradle.properties`.
  Optionally set `SUPPORT_EMAIL` for the diagnostic email.
- [x] **Upload key** (done 2026-11-21): alias `stepskyscrapper` in the shared Idraak keystore; the `STEPSKY_*` values are
  in `local.properties` on the dev machine. Keep the keystore, its backup and the password outside Git. For a new key
  elsewhere: create it yourself (you choose the passwords; back up the file and passwords outside Git):
  `keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000`.
  Copy `keystore.properties.example` to `keystore.properties` and fill it in. CI alternative: `STEPSKY_KEYSTORE_FILE`,
  `STEPSKY_KEYSTORE_PASSWORD`, `STEPSKY_KEY_ALIAS`, `STEPSKY_KEY_PASSWORD`.
- [ ] Optional: with a device attached, `./gradlew :app:generateBaselineProfile` and commit the profile
  ([TECH-006](ROADMAP.md#tech-006)).

## 2. Build the signed AAB
1. Check the configuration (each fails with the reason if something is missing):

   ```powershell
   ./gradlew :app:verifyReleaseSigning :app:verifyReleaseAdMob :app:verifyReleasePrivacyPolicy :app:verifyReleaseRevenueCat
   ```

2. Build:

   ```powershell
   ./gradlew clean :app:bundleRelease
   ```

   Output: `app/build/outputs/bundle/release/app-release.aab`. With `google-services.json` present, the Crashlytics
   plugin uploads the R8 mapping file during this build (needs network).
3. Sanity checks:

   ```powershell
   jarsigner -verify app/build/outputs/bundle/release/app-release.aab
   ```

   The build log must not show `w: AdMob: … ads disabled`. Optionally install it on a phone with `bundletool` and
   open every tab ([TECH-007](ROADMAP.md#tech-007)).

## 3. Create the app and upload to internal testing
1. Play Console › Create app: name "Step Skyscraper", default language, **App**, **Free** (with in-app purchases),
   accept the declarations.
2. Setup › App signing: use **Play App Signing** (Google-generated app signing key); your key above is the upload key.
3. Testing › Internal testing › Create new release › upload `app-release.aab`. Release name `1.0.0 (1)`. Add release
   notes. Add testers (email list) and save. Roll out to internal testing.
4. Fix anything the pre-launch report flags. Every later upload needs a higher `versionCode` (`app/build.gradle.kts`).
5. Monetize › Products: create the billing products (below), then add license testers (Setup › License testing).

## 4. Billing products
- Subscription `architect_pass` with base plans `monthly` (P1M) and `annual` (P1Y); on `annual`, an offer with a
  7-day free-trial phase for new customers.
- One-time product `architect_pass_lifetime`.
- Products can only be created after an APK/AAB with the billing permission is uploaded (step 3).
- RevenueCat already lists these ids; once they exist in Play, its product pages should show the store status as OK.

## 5. App content declarations
| Declaration | Draft answer |
|---|---|
| Privacy policy | The hosted URL (same as `PRIVACY_POLICY_URL`) |
| App access | **All functionality is available without special access** (no login, no account) |
| Ads | **Yes, my app contains ads** (one opt-in rewarded ad on Skyline; none on Build, onboarding or the ceremony) |
| Content rating | Complete the IARC questionnaire honestly: reference app/utility-type, no violence, no user-generated content shared in-app, digital purchases yes, ads yes. The resulting rating is set by IARC, not by us |
| Target audience and content | **13+ only** (for example 13–15, 16–17, 18+, or 18+ alone). Don't select any under-13 group, or the Families policy and child-directed ad settings apply. Not designed for children; no appeal to children in the listing |
| News app | No |
| COVID-19 tracing / status | No |
| Data safety | Section 7 below |
| Government app | No |
| Financial features | None |
| Health | Health apps declaration, section 6 below |
| Advertising ID | **Yes**, the app uses the advertising ID, for **Advertising or marketing** (Google Mobile Ads SDK merges `com.google.android.gms.permission.AD_ID`). Firebase Analytics ad-id collection is off |

## 6. Health apps declaration (App content › Health apps)
- [ ] Category: *Fitness / Activity tracking*. Declare Health Connect use.
- [ ] Permissions: `READ_STEPS` (core feature: steps build floors); `READ_HEALTH_DATA_IN_BACKGROUND` (widget and
  periodic sync keep the tower current; requested only when Health Connect supports the feature). No write
  permissions, no other data types.
- [ ] Justification: steps are read only to compute floors on the device; never transferred off the device; never used
  for advertising or analytics.
- [ ] `ACTIVITY_RECOGNITION` is used only for the step-counter fallback when Health Connect is unavailable.
- [ ] Privacy rationale: Health Connect's privacy link (`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`, Android
  13 and lower) and permission-usage screen (`VIEW_PERMISSION_USAGE` alias, Android 14+) open the in-app Help &
  privacy screen, which links the hosted policy. Verify on a device after setting `PRIVACY_POLICY_URL`
  ([TECH-011](ROADMAP.md#tech-011) criterion 2).

## 7. Data safety form (draft)
"Collected" includes data sent off the device by SDKs in the app. Step data is processed on the device only and
isn't declared as collected.

| Data type | Collected / shared | Source | Purposes | Optional? |
|---|---|---|---|---|
| Health and fitness | **Not collected** | Steps stay on the device | — | — |
| Location › Approximate location | Collected and shared | AdMob (IP-derived), only when an ad is requested | Advertising or marketing, Analytics, Fraud prevention | Optional (opt-in ad) |
| App activity › App interactions | Collected (Analytics); collected and shared (AdMob ad interactions) | Firebase Analytics events (no step values); AdMob | Analytics; Advertising or marketing | Analytics: required. AdMob: optional |
| App info and performance › Crash logs, Diagnostics | Collected (Crashlytics); collected and shared (AdMob diagnostics) | Firebase Crashlytics; AdMob | App functionality, Analytics; Advertising, Fraud prevention | Crashlytics: required |
| Device or other IDs | Collected (Firebase installation / app-instance id; RevenueCat anonymous app user id); collected and shared (advertising ID via AdMob, after consent where required) | Firebase; RevenueCat; AdMob | Analytics, App functionality; Advertising or marketing, Fraud prevention | Firebase: required. AdMob: optional |
| Financial info / Purchase history | Collected (RevenueCat validates Play purchases as our service provider; payment details stay with Google Play). Recheck Play's current guidance for purchase-management SDKs | RevenueCat | App functionality | Optional (only if the user buys) |
| Personal info, messages, photos, contacts, calendar, files | Not collected (share images and the diagnostic email are sent only by the user through Android's share sheet) | — | — | — |

| Question | Draft answer |
|---|---|
| Is data encrypted in transit? | Yes (Firebase, RevenueCat, AdMob and Play over TLS) |
| Can users request deletion? | Choose based on your support process. On-device data is deleted by clearing data or uninstalling; there's no account. Firebase data isn't linked to identity; if you answer Yes, offer deletion requests through the contact email in the policy |
| Account creation | None |
| Independent security review | No |
| Families policy commitment | Not applicable (13+ audience) |

Keep this table consistent with [privacy-policy.md](privacy-policy.md). If Firebase isn't configured for a build, its
rows don't apply to that build, but declare what the published build does.

## 8. Store listing
- [ ] App name (≤ 30 characters), short description (≤ 80), full description (≤ 4,000) that explains Health Connect
  use plainly ("reads your daily step count only; steps never leave your phone") and mentions the optional rewarded
  ad and Architect Pass.
- [ ] App icon 512 × 512 PNG (32-bit, ≤ 1 MB): the Night Tower `play-512.png` from the Claude Design project
  (`icons/concept-1-night-tower/play-512.png`, also `store-listing/graphics/app-icon-512.png`).
- [ ] Feature graphic 1024 × 500 JPEG or 24-bit PNG.
- [ ] Phone screenshots: at least 2 (aim for 4–8), PNG or JPEG, each side 320–3,840 px, and the long side at most
  twice the short side. **Captures from a 20:9 phone (1080 × 2400, Pixel 6a) and the Roborazzi baselines
  (720 × 1600) are too tall**; use a 16:9 emulator (1080 × 1920) or crop. Show real app screens with demo data
  (seeded debug database or the Roborazzi fixtures), never your own step history. Good set: tower growing on Build,
  topping-out ceremony, Skyline, Customise, widget on a home screen. The Story share card (1080 × 1920) works as a
  promotional image.
- [ ] Category: Health & Fitness. Contact email (required) and, optionally, website.
- [ ] Don't claim medical benefits; avoid implying an affiliation with Google or Health Connect.

## 9. Testing track requirements
- [ ] **Closed testing** (personal developer accounts created after 13 Nov 2023): at least **12 testers opted in for
  14 consecutive days** before you can apply for production access. Check the current number in Play Console.
- [ ] License testers for billing to exercise purchase, trial and restore ([TECH-004](ROADMAP.md#tech-004)).
- [ ] Test on Android 8 or 9 (no Health Connect → sensor fallback; Save image permission), Android 13 (Health Connect
  app install path), Android 14+ (framework Health Connect), and a Samsung device with Samsung Health.
