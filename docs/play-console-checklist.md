# Play Console release checklist

## Before the first upload
- [ ] **Firebase**: create the project, add `app/google-services.json` (git-ignored). The Google Services and Crashlytics plugins apply automatically when the file exists. In Remote Config, create `tower_ladder` = `10,25,50,100,160,250`.
- [ ] **AdMob**: create the app and one *Rewarded* unit; set `ADMOB_APP_ID` and `ADMOB_REWARDED_UNIT_ID` in `~/.gradle/gradle.properties` or CI (defaults are Google's public test ids). Configure the UMP consent message (GDPR + US states) in AdMob › Privacy & messaging.
- [ ] **Billing products** (Monetize › Products):
  - Subscription `architect_pass` with base plans `monthly` (P1M) and `annual` (P1Y); on `annual`, add an offer with a 7-day free-trial phase for new customers.
  - One-time product `architect_pass_lifetime`.
- [ ] **Signing**: create the upload key yourself (you choose the passwords; keep the file and passwords backed up
  outside Git), copy `keystore.properties.example` to `keystore.properties` and fill it in, enrol in Play App
  Signing, then `./gradlew :app:bundleRelease`. Key creation (JDK keytool):
  `keytool -genkeypair -v -keystore keystore/upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000`
  CI: set `STEPSKY_KEYSTORE_FILE`, `STEPSKY_KEYSTORE_PASSWORD`, `STEPSKY_KEY_ALIAS`, `STEPSKY_KEY_PASSWORD`.
- [ ] **Privacy policy**: host it (contents: see Help & privacy screen and the Data Safety answers below) and set `PRIVACY_POLICY_URL`; optionally `SUPPORT_EMAIL` for diagnostics.
- [ ] **Baseline profile**: with a device or emulator attached, run `./gradlew :app:generateBaselineProfile` and commit the generated profile.

## Health apps declaration (App content › Health apps)
- [ ] Category: *Fitness / Activity tracking*. Declare Health Connect use.
- [ ] Permissions requested: `READ_STEPS` (core feature: steps build floors); `READ_HEALTH_DATA_IN_BACKGROUND` (widget and background sync keep the tower current; requested only when Health Connect reports the feature). No write permissions, no other data types.
- [ ] Justification: steps are read only to compute floors on the device; no transfer off-device; no advertising or analytics use.
- [ ] `ACTIVITY_RECOGNITION` is used only for the step-counter fallback when Health Connect is unavailable.
- [ ] Health Connect rationale screen: the app handles `ACTION_SHOW_PERMISSIONS_RATIONALE` and the Android 14 `VIEW_PERMISSION_USAGE` alias.

## Data safety form
| Question | Answer |
|---|---|
| Data collected or shared | **Yes**, collected (not shared): App activity (app interactions), App info and performance (crash logs, diagnostics), Device or other IDs (Firebase installation id; AdMob advertising id only after consent) |
| Health and fitness data | **Not collected**: steps are processed on the device only and never transmitted |
| Shared with third parties | Advertising: AdMob receives device/ad identifiers for the opt-in rewarded ad only; no step or health data |
| Encrypted in transit | Yes (Firebase, AdMob over TLS) |
| Users can request deletion | Data is on-device; uninstalling or clearing data deletes it. Analytics can be reset via Firebase installation deletion |
| Account required | No |
| Purpose | Analytics (app improvement), Advertising (rewarded ad only), App functionality |

## Ads and families
- [ ] Ads declaration: **Contains ads** (one opt-in rewarded ad on the Skyline tab; no ads on Build, onboarding or the topping-out ceremony).
- [ ] Target audience: 13+ (not designed for children); set COPPA/child-directed tags accordingly.
- [ ] Content rating questionnaire (IARC).

## Testing track requirements
- [ ] **Closed testing**: new personal developer accounts must run a closed test with **at least 12 testers opted in for 14 consecutive days** before applying for production.
- [ ] License testers for billing (Setup › License testing) to exercise purchase, trial and restore.
- [ ] Test on: Android 8 (no Health Connect → sensor fallback), Android 13 (Health Connect app install path), Android 14+ (framework Health Connect), Samsung device with Samsung Health.

## Store listing
- [ ] Screenshots (use the Roborazzi outputs in `feature/*/src/test/screenshots` as a starting point), feature graphic, short and full description.
- [ ] Health Connect data-use disclosure in the listing description.
