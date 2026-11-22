# Play Store listing and Play Console state

Source of truth for the Google Play listing text and what is configured in Play Console (IdraakAI developer account,
app "Step Skyscraper: Step Counter", package `ai.idraak.stepskyscraper`). Graphics live outside the repo in
`D:\Work\Idraak\Apps Data\StepSkyScraper\PlayStore Graphics (Store Listing)\store-listing` (from the Claude Design
project); uploaded copies were flattened to RGB.

## Play Console state (2026-11-22)

| Area | State |
|---|---|
| Closed testing – Alpha | Release `1 (1.0.0)` (versionCode 1, `app-release.aab` from `develop` at `c3a9954`, minSdk 26, target 37), en-GB release notes; **sent for review 2026-11-22** with 16 changes |
| Testers | Email list "LaterBox" (66); feedback idraakai.labs@gmail.com; opt-in link appears once the release is live |
| Countries | All 178 countries / regions |
| Store listing (en-GB) | Title, short and full description below; 512 px Night Tower icon, 1024×500 feature graphic, 6 phone screenshots; AI-assets label chosen ("created or edited using AI"), per-asset labels still to set |
| Store settings | App, Health & Fitness, contact idraakai.labs@gmail.com; no tags yet (suggested: Activity tracker) |
| App content | Privacy policy URL; Ads: yes; Sign in details: no restriction; Content rating: All Other App Types (ESRB Everyone, PEGI 3, ClassInd 14+, in-app purchases); Target audience 13–15, 16–17, 18+; Data safety (below); Advertising ID: yes (advertising, fraud prevention); Government apps: no; Financial features: none; Health apps: Activity and fitness, with READ_STEPS, ACTIVITY_RECOGNITION and READ_HEALTH_DATA_IN_BACKGROUND justifications; trader status: completed by the user |
| Warnings | Bundle has native code without debug symbols (not blocking) |

**Data safety as submitted:** encrypted in transit; no account creation; deletion requests yes, via
`https://idraakai.org/step-skyscraper/privacy/#9-keeping-and-deleting-data`.

| Data type | Collected | Shared | Required | Purposes (collected / shared) |
|---|---|---|---|---|
| Approximate location | Yes | Yes | Optional | Advertising, fraud prevention / same (AdMob) |
| Purchase history | Yes | No | Optional | App functionality (RevenueCat) |
| App interactions | Yes | Yes | Required | Analytics, advertising / advertising |
| Crash logs | Yes | No | Required | App functionality, analytics (Crashlytics) |
| Diagnostics | Yes | Yes | Required | App functionality, analytics / advertising, fraud prevention |
| Device or other IDs | Yes | Yes | Required | App functionality, analytics, advertising, fraud prevention / advertising, fraud prevention |

Firebase Analytics doesn't collect the advertising ID (`google_analytics_adid_collection_enabled=false`).

Written from the shipped code (version 1.0.0). Every feature below exists in the app.

## App name (30 max)
```
Step Skyscraper: Step Counter
```

## Short description (80 max)
```
Step counter & walking game: every 1,000 steps builds a floor of your skyscraper
```

## Full description (4,000 max)
```
Turn your daily steps into a skyline. Step Skyscraper is a step counter and walking game where every 1,000 steps adds one floor to your skyscraper. Walk as you normally do, watch your tower rise, and build a whole city over the weeks and months ahead.

★ EVERY STEP BUILDS SOMETHING
Your daily steps sync automatically from Health Connect, or from your phone's own step sensor. There's no workout to start and nothing to log. Open the app and see your tower, the floor under construction and exactly how many steps until the next one.

★ FINISH A TOWER, START A TALLER ONE
Each tower ends with a topping-out ceremony: the crown goes on and the city lights come up. Then you break ground on the next, taller tower.

★ GROW A SKYLINE THAT'S ALL YOURS
Finished towers stay in your skyline for good. Nothing is ever taken away. Every eight towers complete a district, and your skyline becomes a visual record of everywhere you've walked.

★ DESIGN EVERY TOWER YOUR WAY
Five free facade styles: Art Deco, Glass, Brick, Brutalist and Eco. Earn Blueprints by walking and use them to unlock colour palettes, crowns, window lighting and ground styles.

★ GOALS THAT FIT YOUR LIFE
Set a daily step target and see your weekly goal in floors. The app can suggest a new target based on your recent walking. Need a break for rest, travel or illness? Pause your goals. Your floors still count.

★ NO GUILT, NO LOST STREAKS
Missed a day? Your tower simply waits. Step Skyscraper rewards walking and never punishes rest.

★ YOUR TOWER ON YOUR HOME SCREEN
Home screen widgets show your tower and your steps to the next floor at a glance.

★ MORE TO DISCOVER
• 24 achievements and architect levels
• Week and month charts of the floors you've built
• Share cards of your tower or skyline for your story or feed
• Optional notifications when a tower tops out, plus a weekly summary
• A sky that follows your time of day, with light and dark themes

★ PRIVATE BY DESIGN
No account and no sign-up. Your steps never leave your phone. Step Skyscraper reads only your daily step count and never reads location, heart rate or sleep.

★ ARCHITECT PASS (OPTIONAL)
Building is free and always will be. The optional Architect Pass adds premium facades, crowns and palettes, lighting and ground sets, and 2× Blueprints on every tower, with no ads. Free players can watch one optional ad per finished tower to double its Blueprints.

Step Skyscraper is a walking motivation app, not a medical device. Step counts come from Health Connect or your phone's step sensor.

Every step builds something. Start your first tower today.
```

## Keyword and positioning strategy
- Primary intent: "step counter" (title + short description + body), "pedometer" style search is served by "step counter" and "steps"; secondary: "walking game", "walking app", "daily steps", "step goal", "steps tracker", "widget", "Health Connect".
- Differentiators competitors don't have: a growing 3D isometric city (gamified walking with a visible, permanent result), no streak loss and pause goals (anti-guilt), privacy (no account, steps stay on the phone).
- Positioning: "walking motivation that builds something you can see", for casual walkers and habit builders rather than athletes. No weight-loss or health-outcome claims.
- Category: Health & Fitness. Tags (Play Console): Pedometer / Step counter, Walking, Habit tracker if offered.
