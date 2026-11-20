# Step Skyscraper — Product

> **Scope:** product vision, audience, the Phase 1 feature baseline, and product-level analysis.
> Technical detail: [TECHNICAL.md](TECHNICAL.md) · Defects: [BUGS.md](BUGS.md) · Planned work:
> [ROADMAP.md](ROADMAP.md). Design source of truth: [design-inventory.md](design-inventory.md) and
> [design-deviations.md](design-deviations.md).
>
> **Baseline:** version 1.0.0 (versionCode 1), working tree of 2026-11-20. The project is **not under version
> control yet** (no commit to cite, see [TECH-001](ROADMAP.md#tech-001)). Not published.
> **Last reviewed:** 2026-11-20.

---

## 1. Vision

Walking becomes building: every 1,000 steps adds a floor to an isometric skyscraper, finished towers join a
growing skyline, and nothing built is ever taken away. Onboarding promise (`feature/onboarding` strings):
"Every 1,000 steps builds a floor … nothing you build is ever taken away."

## 2. Target audience

Segments are **inferred** from the product design; no research or analytics data exists yet.

| Segment | Why the product fits | Evidence in the app |
|---|---|---|
| Casual walkers who dislike streak pressure | No streaks, no decay, calm zero-step copy, goal pause | `BuildReducer.weekMessage`, pause sheet, design review "No guilt anywhere" |
| People who like collecting and building | Towers, 5 facade kits, 24 achievements, cosmetics | `CosmeticCatalog`, `AchievementCatalog` |
| Privacy-sensitive users | No account, steps stay on the device | Help & privacy screen, `core:analytics` events carry no step values |
| Android users with Health Connect / Samsung Health | Reads deduplicated daily totals | `HealthConnectStepSource` |

## 3. Value proposition and alternatives

Analysis, not research:

| Alternative | Its strength | Where this product can win |
|---|---|---|
| Google Fit / Samsung Health step counters | Free, preinstalled, accurate | A visible thing that grows; reward that never resets |
| Streak-based habit apps | Strong daily pull | No guilt or broken streaks; weekly goal; pause |
| Walking games (location-based) | Social, exploration | Works from any walking, no location, no account |

**Honest position today:** the core loop (steps → floors → towers → skyline), privacy stance and calm tone are
implemented. Social and multi-user features don't exist. Premium value relies on cosmetics only.

## 4. Phase 1 baseline — what the product does today

Implemented in 1.0.0. Known defects: [BUGS.md](BUGS.md). Planned work is in [ROADMAP.md](ROADMAP.md) and is not
included here. Screen-by-screen design coverage: [design-inventory.md](design-inventory.md).

| Area | Current behaviour |
|---|---|
| Onboarding (1a–1f) | Promise → step-access rationale → Health Connect permission sheet (Steps, plus background read when available) → payoff with today's real steps → style + name → daily target with a 7-day suggestion → widget pin prompt. "Not now" skips to style with a "Connect steps" card on Build. |
| Build tab | Sky header (sky follows the clock), tower with crane and partial floor, "Next floor in N steps" with 10 segments, week ring card, pull-to-refresh, "Synced. N floors added." snackbar, tap to zoom, long-press to mirror. Week card opens **Goals**, not a weekly detail ([BUG-004](BUGS.md#bug-004)). |
| Topping-out ceremony | Full screen once per tower: crown lift, lit windows, beams, Blueprints count-up, Share / Start next tower / Close; paywall once after the first tower. |
| Skyline tab | Finished towers of the current district on a model base, pinch/pan, stats sheet with rename and swipe, opt-in rewarded ad card for the newest tower, next-district progress. Earlier districts can't be browsed ([PROD-001](ROADMAP.md#prod-001)). |
| Customise | Facade, palette, crown, lights and ground tabs; three lock types (progress, Blueprints, Architect Pass); 3 s locked preview then unlock sheet; buying with Blueprints. Opened from Build only. |
| Me tab | Next-floor pill, architect level, Build Days / floors / towers, week and month bars, last 4 weeks, achievements, Architect Pass card, Goals, Pause goals, Step source, Settings, Help & privacy. |
| Achievements | 24 cumulative achievements with progress and rewards; "Equip" chip is a label only. |
| Share | Tower or skyline card, Story 9:16 / Square 1:1, stats toggle, share sheet with caption, save to Pictures (no feedback, [BUG-003](BUGS.md#bug-003)). |
| Paywall | Architect Pass: yearly with 7-day trial, monthly, lifetime, prices from Play; restore. |
| Step-source help | Last data time, source status, three steps (reordered for Samsung), deep links, "Check again", diagnostic draft without step data. |
| Widgets | Small 2×2 and medium 4×2 Glance widgets, light and dark, refreshed after each sync. |
| Notifications | Local "topping out" and Sunday weekly summary; on by default but the Android 13+ permission is never requested by default ([BUG-001](BUGS.md#bug-001)). |
| Persistence | All progress on the device (Room + DataStore), included in Android backup. |
| Debug only | Component gallery from Settings. |

## 5. Strengths

- The core loop is complete and deterministic: progress is recalculated from stored daily totals, so syncs
  can repeat safely and progress can't be lost (`CityCalculator`, `SyncStepsUseCase`).
- Strong privacy story that is enforced in code: no account, no step values in analytics or ads.
- Visual identity: a faithful port of the design's tower renderer, plus five styles and cosmetics.
- Kind tone: no streaks, pause goals, reassuring error copy, no red colour token.
- Solid engineering base: modular clean architecture, 84 automated tests, R8 release build.

## 6. Weaknesses and limitations

Facts (verified in code):

1. No social or multi-user element; growth relies on share cards only.
2. Premium is cosmetic only (by design); conversion depends on how much users care about looks.
3. Earlier districts aren't viewable once a new district starts ([PROD-001](ROADMAP.md#prod-001)).
4. Notifications are effectively off on Android 13+ until the user visits Settings ([BUG-001](BUGS.md#bug-001)).
5. Firebase, AdMob and billing products aren't configured, so analytics, crash reports and payments don't work
   yet ([ROADMAP Phase 2](ROADMAP.md#phase-2--release-blockers)).

Assumptions (not verifiable from the repo): whether the ladder heights (10, 25, 50…) feel right; how often
users return without notifications; real Health Connect behaviour across OEMs.

## 7. Why users may leave, and what would bring them back

| Churn risk | Return driver |
|---|---|
| Steps don't arrive (permissions, battery) | Step-source help, "Updated X min ago", widgets |
| Long towers feel slow after the first 10 floors | Ceremony and Blueprints; tune ladder via Remote Config ([PROD-006](ROADMAP.md#prod-006)) |
| Nothing pulls the user back | Topping-out and weekly-summary notifications once permission is requested ([BUG-001](BUGS.md#bug-001)) |
| Novelty of cosmetics wears off | Districts, achievements, premium styles |

## 8. Opportunities

Task cards are in [ROADMAP.md](ROADMAP.md).

| ID | Opportunity | Type |
|---|---|---|
| [PROD-001](ROADMAP.md#prod-001) | Browse every district in Skyline | Product improvement |
| [PROD-002](ROADMAP.md#prod-002) | Re-skin finished towers from the stats sheet | Product improvement |
| [PROD-006](ROADMAP.md#prod-006) | Decide first-tower height and premium-bonus rules | Product decision |
| [GROW-001](ROADMAP.md#grow-001) | Activation and retention funnel dashboards | Growth task |
| [GROW-002](ROADMAP.md#grow-002) | Store listing built from the share cards | Growth task |

## 9. Monetisation strategy

Current model (enforced on the device via Play Billing; entitlement cached offline): Architect Pass unlocks
12 premium facades/crowns/palettes, lighting and ground sets, and 2× Blueprints; one opt-in rewarded ad doubles
a finished tower's Blueprints. Blueprints buy cosmetics only and never floors.

| Feature | Tier | Rationale |
|---|---|---|
| Building floors and towers, widgets, share cards | Free | Core habit loop must stay free |
| 5 launch styles and progress-unlocked cosmetics | Free | Reward for walking |
| Premium styles, lighting/ground sets, 2× Blueprints | Architect Pass | Depth and expression, not progress |
| Rewarded ad | Free, opt-in, once per tower | Hidden for Pass members |

## 10. Product metrics to track

| Metric | Definition |
|---|---|
| Activation | `onboarding_completed` plus first floor built within 24 h |
| North star | Weekly active builders (≥ 1 floor in the week) |
| Retention | Day 1 / 7 / 30 return, by week of install |
| Quality | Share of syncs failing; days with "Steps not arriving?" opened |
| Monetisation | `paywall_shown` → `purchase_started` → `purchase_completed`; rewarded-ad completion rate |
| Growth | `share_card` events per active user |

Events are defined in `core/domain/.../model/Analytics.kt` but **instrumentation is inert** until Firebase is
configured ([TECH-002](ROADMAP.md#tech-002)); dashboards are [GROW-001](ROADMAP.md#grow-001).

## 11. Out of scope ("Don't build")

- Accounts, cloud sync or leaderboards — they would break the "nothing leaves your phone" promise.
- Streaks or decay — contradict the product's calm tone.
- Buying floors with money or ads — would make progress meaningless and is excluded by design.
- Location-based features — unnecessary data collection.
