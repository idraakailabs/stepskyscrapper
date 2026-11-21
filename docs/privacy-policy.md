# Step Skyscraper — Privacy Policy (draft for hosting)

> **For the publisher, not part of the hosted text.** This is a draft based on what version 1.0.0 actually
> does (code review, 2026-11-21). Before hosting it:
> 1. Replace every `[placeholder]`: developer or company name, contact email, postal address if your
>    jurisdiction requires one, and the effective date.
> 2. Have it reviewed for your jurisdiction. This draft is not legal advice.
> 3. Host it at a public **https** URL that needs no login and isn't a PDF, then set `PRIVACY_POLICY_URL` (the
>    Play bundle won't build without it, see [play-console-checklist.md](play-console-checklist.md)) and enter
>    the same URL in Play Console › App content › Privacy policy.
> 4. Keep it in step with the Data safety answers in [play-console-checklist.md](play-console-checklist.md). If
>    the app's data use changes, update both.
>
>
> **Hosted (2026-11-21):** https://idraakai.org/step-skyscraper/privacy/ and /step-skyscraper/terms/, in the
> Idraak-Web repo under `src/content/step-skyscraper-legal/` with the placeholders filled (IdraakAI,
> idraakai.labs@gmail.com). Change both copies together.
> Everything below the line is the text to host.

---

**Effective date:** [date]

Step Skyscraper ("the app") is published by [developer or company name] ("we", "us"). This policy explains
what information the app uses, where it goes and the choices you have.

## The short version

- Your steps never leave your phone. We have no account system and no server that stores your progress.
- We use Google Firebase for anonymous usage statistics and crash reports, which never include step counts,
  floors or any health data.
- If optional ads are switched on, one opt-in rewarded ad is served by Google AdMob, only after you make a
  privacy choice. Ads never receive step data.
- Purchases are handled by Google Play. We never see your payment details.

## 1. Step data (Health Connect and the step counter)

- **What we read:** your daily step totals only. On Android devices with Health Connect, the app asks for the
  "Steps" read permission and, where your device supports it, permission to read in the background so the
  widget and tower stay current. Without Health Connect, the app can use your phone's built-in step counter
  (the "Physical activity" permission).
- **What we don't read:** location, heart rate, sleep, workouts or any other health data. The app never writes
  data to Health Connect.
- **How it's used:** only to work out floors and towers on your phone. Steps, floors, towers, achievements and
  settings are stored in the app's private storage on your device.
- **Not shared:** step data is never sent to us, to Firebase, to AdMob or to anyone else, and it is never used
  for advertising, analytics or profiling.
- **Health Connect:** the use of information received from Health Connect adheres to the Health Connect
  Permissions policy, including its Limited Use requirements.
- **Backup:** like most apps, the app's data is included in your Android backup to your Google account if you
  have backup turned on in your phone's settings. That backup is managed by Google under your account and
  can be turned off in Android settings.

## 2. Usage statistics and crash reports (Google Firebase)

When the published app includes Firebase, it uses:

- **Firebase Analytics** to count how the app is used, for example "onboarding completed", "screen viewed",
  "tower topped out", "paywall shown" or "share card created", with simple labels such as the chosen style or
  plan. These events **never contain step counts, floors or health data**. Firebase also collects standard
  information such as an app-instance identifier, device model, OS version, app version and approximate
  region derived from the IP address. Advertising-ID collection and ad-personalisation signals are switched
  off in Analytics.
- **Firebase Crashlytics** to receive crash reports: the technical stack trace, device model, OS version, app
  version and a random installation identifier. Crash reports never include step or health data.
- **Firebase Remote Config** to receive app settings from us (for example tower heights and whether optional
  ads are switched on). It uses a Firebase installation identifier.

Google processes this data on our behalf under the
[Firebase terms and privacy information](https://firebase.google.com/support/privacy). It's used to fix
problems and improve the app, not to identify you. We don't link it to your name, email or any account.

## 3. Optional rewarded ad (Google AdMob)

- The app may offer one optional ad on the Skyline tab: watch a short ad to double the Blueprints (an in-app
  currency) earned for a finished tower. There are no ads while building, during onboarding or during the
  topping-out celebration, and no banner or pop-up ads.
- An ad is only requested after you tap "Watch a short ad". Before the first ad, the app asks for your privacy
  choice using Google's consent tool (User Messaging Platform) where the law requires it, for example in the
  EEA, the UK and Switzerland, and certain US states. You can change that choice later in **Me › Help &
  privacy › Ad privacy choices** when it applies to you.
- When an ad is requested, Google AdMob may collect your device's advertising ID, IP address (used for
  approximate location), device and app information, and how you interact with the ad, to show the ad, measure
  it, prevent fraud and, if you agree, personalise ads. You can reset or delete your advertising ID in your
  phone's settings.
- AdMob never receives step counts, floors or health data. See
  [how Google uses information from apps that use its services](https://policies.google.com/technologies/partner-sites).

## 4. Purchases (Google Play Billing and RevenueCat)

The optional Architect Pass subscription and lifetime purchase are processed by Google Play. Google handles
your payment details; we never receive them. To check that a purchase is valid and restore it on a new phone, the
app uses RevenueCat, our purchase-management provider: it receives your purchase records from Google Play
(product, purchase time, status and a purchase token) together with a random app user id that RevenueCat creates.
It never receives your name, email, payment details or any step data. The app stores only whether a purchase is
active on your device to unlock premium styles. See the
[Google Play terms](https://play.google.com/about/play-terms/), the
[Google Privacy Policy](https://policies.google.com/privacy) and the
[RevenueCat Privacy Policy](https://www.revenuecat.com/privacy).

## 5. Things you choose to share

- **Share cards:** images of your tower are created on your phone. They leave your phone only if you choose to
  share or save them, using Android's share sheet. If you turn on "show stats", the image includes the numbers
  shown on the card.
- **Diagnostic email:** if you tap "Send a diagnostic" on the step-help screen, the app prepares an email or
  message with technical sync information (last sync times and step-source status, never step counts) that you
  can review before sending.

## 6. Notifications

Topping-out and weekly-summary notifications are created on your phone. Nothing is sent to a server to deliver
them. You can turn them off in the app's Settings or in Android settings.

## 7. Permissions the app requests

| Permission | Why |
|---|---|
| Health Connect: read steps; read in the background | Turn your daily steps into floors; keep the tower and widget current |
| Physical activity | Step-counter fallback when Health Connect isn't available |
| Notifications | Topping-out and weekly-summary notifications (optional) |
| Internet / network state | Firebase, Remote Config, ads and Google Play purchases |
| Advertising ID and Android ad services | Used by Google AdMob for the optional rewarded ad |
| Google Play billing | Optional purchases (validated through RevenueCat) |
| Storage (Android 9 and lower only) | Save a share image to your Pictures folder when you ask |

## 8. Children

The app is not directed at children under 13 and isn't designed for them. We don't knowingly collect personal
information from children.

## 9. Keeping and deleting data

- Your progress lives on your phone. Clearing the app's data or uninstalling the app deletes it from the
  phone. Any Android backup is managed in your Google account settings.
- Firebase Analytics data is kept for [retention period you set in Firebase, for example 2 or 14 months];
  Crashlytics keeps crash reports for up to 90 days. Because we don't link this data to you, we usually can't
  find it by person, but you can contact us and we'll help where we can.
- There is no account, so there is no account to delete.

## 10. Security

Data sent to Firebase, AdMob and Google Play is encrypted in transit (HTTPS/TLS). On-device data is protected
by Android's app sandbox.

## 11. Changes to this policy

If we change this policy, we'll update the effective date above and, for significant changes, mention it in
the app's release notes.

## 12. Terms

Use of the app is also governed by our Terms of Service: [terms URL].

## 13. Contact

[developer or company name] · [contact email] · [postal address, if required]
