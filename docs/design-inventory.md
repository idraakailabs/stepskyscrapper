# Step Skyscraper: design inventory

Source: Claude Design project `28cbb1ce-1aa3-42a7-a36b-460f7043966d`, file `Step Skyscraper All.dc.html`
(plus `tower.js`, `support.js`, `SSNav.dc.html`, `SSStatusBar.dc.html`). Local copies live in `design/source/`.
`support.js` is the generic Design Components runtime (template engine, React loader); it contains no design tokens.

Status legend: `[x]` implemented and checked against the design. Code locations are listed per item.

**Device check (2026-11-21, Pixel 6a, debug build, [TECH-012](ROADMAP.md#tech-012)):** every frame was compared
with the rendered design at the same dp scale. *(device ✓)* = checked on the phone; *(device —)* gives the reason
when it couldn't be.

## 1. Design tokens

### 1.1 Colour, light (`core/designsystem/.../theme/Color.kt`)
| Token | Value | Note |
|---|---|---|
| [x] background | `#F4F1EC` | |
| [x] surface | `#FBFAF7` | |
| [x] surfaceVariant | `#ECE7E0` | tiles, empty segments, rings |
| [x] secondaryContainer | `#E6DFD5` | nav pill, tonal buttons |
| [x] onSurface | `#24211D` | 14.6:1 |
| [x] onSurfaceVariant | `#6A635A` | 5.4:1 |
| [x] outline | `#D9D2C8` | |
| [x] info | `#4E6A86` | sync only |
| [x] primary (amber) | `#E8A23A` | progress + primary action only |
| [x] onPrimary | `#2A1D08` | 8.9:1 |
| [x] primaryContainer | `#F8E6C8` | "Target met" chip |
| [x] primaryText | `#8F5608` | text buttons, accent labels, 5.6:1 |
| [x] Extra: bodySecondary `#4A443D`, outlineStrong `#CFC7BC`, outlineCard `#E2DBD1`, divider `#ECE7E0`, segmentEmptyOnboarding `#E3DDD4`, disabledContainer `#E6E1DA`, disabledContent `#8E877D`, infoContainer `#E5ECF3`, onInfoContainer `#3B546D`, onPrimaryContainer `#6E420A`, snackbar `#2E2A26`, inverseInk `#24211D`, navLine `#E7E1D8`, scrim `#24211D @32%` | |

### 1.2 Colour, dark
| Token | Value |
|---|---|
| [x] background | `#171513` |
| [x] surface | `#201D1A` |
| [x] surfaceVariant | `#2B2724` |
| [x] secondaryContainer | `#3A3530` |
| [x] onSurface | `#F1ECE5` (15.1:1) |
| [x] onSurfaceVariant | `#ADA59B` (7.4:1) |
| [x] outline | `#3D3833` |
| [x] info | `#9DB6D0` |
| [x] primary | `#F0B455` |
| [x] onPrimary | `#2A1D08` |
| [x] primaryContainer | `#3A2C17` |
| [x] primaryText | `#F0B455` (9.8:1) |
| [x] navLine `#2F2B27`, empty week bar on night `#4A4F66`, sky-header ink on night `#C3CADB` |

### 1.3 Sky gradients (vertical, `SkyTokens`)
| [x] | Time | Window | Stops |
|---|---|---|---|
| [x] | Dawn | 05:00–08:00 | `#F0B8A0` 0% → `#F8D9C2` 55% → `#FCEBDD` 100% |
| [x] | Day | 08:00–17:30 | `#9CC4E4` 0% → `#C9DFEE` 55% → `#EAF1F4` 100% |
| [x] | Dusk | 17:30–20:00 | `#5D4C8C` 0% → `#A27A9E` 55% → `#E8AA8C` 100% |
| [x] | Night | 20:00–05:00 | `#0C1530` 0% → `#1A2647` 60% → `#2C3858` 100% |
| [~] | Crossfade 20 min around each boundary — *gap: `SkyGradient.blend` exists, but screens switch to the dominant sky once per minute instead of blending* |
| [x] | Header ink per sky: day `#3E4A55`, dawn `#6B4A3A`, dusk `#EDE3F2` + light title, night `#C3CADB` + light title |
| [x] | Header icon button fill: day/dawn `rgba(251,250,247,.7)`, dusk `rgba(36,33,29,.35)`, night `rgba(255,255,255,.1)` |
| [x] | Onboarding variants: 1a dawn→background at 78%, 1c day→background at 72% |

### 1.4 Typography (`Type.kt`, sp, scales to 200%)
| [x] | Style | Spec |
|---|---|---|
| [x] | numberXL | DM Sans 64/64 600, −1.5 tracking, tabular |
| [x] | numberHero | DM Sans 60/63 600, −1.5, tabular (Build card) |
| [x] | numberL | 40/44 600, −0.8, tabular |
| [x] | towerName | Big Shoulders Display 36/36 800 caps, +0.6 |
| [x] | towerNameHeader | 34/35.7 800 caps +0.5, autosize 34→26 then ellipsis, max 18 chars |
| [x] | headline | 28/34 700 −0.3 (onboarding 30/34.5, 26/31.2 variants) |
| [x] | title | 20/26 600 |
| [x] | body | 16/24 400 |
| [x] | label | 14/20 600 |
| [x] | caption | 12/16 500 |
| [x] | overline | 12 600 +0.4 caps (sky header) |
| [x] | Icons: Material Symbols Rounded wght 400, outline; filled when selected |

### 1.5 Spacing, radii, elevation, motion
- [x] Spacing scale 4, 8, 12, 16, 24, 32, 48. Screen margin 20, card padding 20, section gap 24.
- [x] Radii: sm 12 (tiles), md 16 (cards), card-lg 20, lg 24 (sheets), full (buttons), phone 28 (preview frame only).
- [x] Elevation 0 = outline only `#E2DBD1`; 1 = floating card (`0 1 2 rgba(60,45,25,.06)`, `0 4 12 rgba(60,45,25,.05)`); 2 = sheet/snackbar (`0 −2 24 rgba(60,45,25,.10)`).
- [~] Motion tokens — *gap: no light haptic on each Build floor drop and no soft haptic when Target met pops in (haptics on onboarding floor ticks and the ceremony only)*: floor drop 420ms spring(0.6, 400) falls 24dp, overshoot 1.5dp, stagger 140ms; crown lift 760ms ease-out + settle from 44dp, delay 200ms; windows on 900ms steps, random delay 0–1.4s; number roll 250ms per digit; sky shift 20 min linear; segment fill 160ms; snackbar slide 220ms, 4s; sheet 300ms emphasized-decelerate; button press scale .97 + 8% darker 90ms; chip pop .9→1 180ms; badge flip 300ms; re-skin bottom→top 400ms; preview crossfade 150ms; reduce-motion → 150ms crossfade.

## 2. Tower renderer (port of `tower.js`)
- [x] Isometric projection `X=(x−y)·0.866`, `Y=(x+y)·0.5−z`; W=36, lobby 13, floor 8. (`designsystem/tower/IsoScene.kt`)
- [x] Sky tint per time (`dawn #F2B9A0 .12`, `dusk #4E3F7A .3`, `night #121A33 .58`); `mono` silhouette tint for small widget.
- [x] Lit-window ratio dusk .38 / night .62, deterministic hash, lit colour `#FFD98A`; flicker animation `lights`.
- [x] 5 facade kits deco / glass / brick / brut / eco with exact colours and setbacks; window patterns per kit; lobby pattern.
- [x] 5 crowns (ziggurat + spire, plant room + mast, water tank, cantilever block, roof garden) with exact geometry.
- [x] Under-construction state (outline capped 10 floors ahead, D24): translucent (28%) next floor shell, solid partial floor at `progress`, amber dashed outline; dashed ghost outline up to target per setback band; top cap.
- [x] Crane (mast, lattice, jib, counter-jib, cab, cable, hook, hook load) only while under construction.
- [x] Ground plates plaza / park + trees; added canal and market ground (see deviations).
- [x] `SSSkyline`: hex model base, 3 edge faces, street strip, towers placed diagonally (S=W+16), trees, amber selection pin.
- [x] Auto-fit viewBox with pad / minh / minw; `xMidYMax meet` alignment (`TowerFit`).
- [x] Animations: `drop` (lobby then floors staggered 140ms, −26px → +1.6 → 0, 420ms cubic(.3,1.3,.5,1)), `lift` crown (−44 → +1.4 → 0, 760ms cubic(.25,1,.4,1), 200ms delay), `lights` flicker (900ms steps, delay hash·1400ms); disabled with reduce-motion.
- [x] Palette, lights and extra-ground cosmetics layered on top of the faithful port.

## 3. Components (`core/designsystem/component`, gallery in `app/.../gallery`)
- [x] PrimaryButton 52dp amber, icon option; pressed scale .97 + 8% darker, 90ms
- [x] TonalButton 48dp `secondaryContainer`; OutlinedButton 48dp `#CFC7BC`; TextButton `primaryText`; IconCircleButton 48dp; disabled state `#E6E1DA`/`#8E877D`
- [x] FilterChip 36dp: selected = ink fill + check icon; unselected 1dp `#CFC7BC`
- [x] Status chips 32dp: lock (`surfaceVariant`), Architect Pass (ink + amber filled badge), sync info (`#E5ECF3`/`#3B546D`, rotating while syncing), Target met (`primaryContainer` + filled check_circle)
- [x] Snackbar (ink `#2E2A26`, amber icon + action, 12dp above nav, 220ms, 4s)
- [x] WeeklyRing 96dp/56dp with centre number; SegmentBar 10 × 100 steps (filled, partial gradient + 1dp outline on active, empty); mini segment bar (pill & widget)
- [x] StatCard (Build Days), TowerListCard (thumb + name + meta + chevron)
- [x] Modal bottom sheet (24dp top, handle 32×4 `#CFC7BC`, scrim 32%)
- [x] Bottom navigation (3 destinations, 64×32 neutral pill, filled icon + bold label, 80dp + system gesture inset)
- [x] NextFloorPill (number + "to next floor" + 10×5dp mini bar)
- [x] Page dots (active 20×6 ink, inactive 6×6 `#CFC7BC`)
- [x] Radio option card (1dp outline / 2dp ink ring), Suggested badge
- [x] Segmented toggle (Week / Month, Tower / Skyline, Story / Square)
- [x] Switch (52×32; off = outline ring + grey knob; on = ink + white knob with check)
- [x] Settings row 60dp (icon, title, subtitle, chevron or switch), dividers `#ECE7E0`
- [x] Achievement badge 64dp (earned ink + amber filled; in progress neutral), achievement list card with progress + reward chip
- [x] Numbered help step card (28dp ink circle) with inline text action
- [x] Number roll (per-digit vertical roll, tabular, 250ms)
- [x] Sky header (overline, tower name, subtitle, two header icon buttons)

## 4. Screens and states
### Onboarding (`feature/onboarding`)
- [~] 1a Promise *(device ✓)*: dawn gradient, tower deco 7/12 progress .5 with crane and `drop`, headline, body, dots 1/4, "Start building" — *gap: the 6° crane swing is not animated*
- [x] 1b Permission rationale *(device ✓, real Health Connect sheet incl. background access)*: walk icon tile, title, lead, 3-row list, dots 2/4, "Allow step access" → Health Connect sheet (READ_STEPS [+ background]), "Not now" → demo tower
- [x] 1b' *(device — Health Connect is installed on the test phone)* Health Connect missing / needs update → Play listing deep link (reviewer note)
- [x] 1c Payoff *(device ✓; number/unit baseline fixed)*: "You've already walked", 0 → steps roll 1.2s, floors drop with haptic tick, dashed next floor, segment bar, "Floor N in X steps…", dots 3/4, "Choose its style"; zero-step copy variant
- [~] 1d Style & name *(device ✓)*: preview card, 5 style tiles (selected = ring + check badge), name field (display face, 2dp ring) + dice, "You can rename it any time.", dots 4/4, "Lay the foundation"; re-skin drop 60ms stagger — *gap: the 80ms lift-off of the old floors is not played*
- [x] 1e Daily target *(device ✓; card line heights fixed)*: back, title, 7-day average copy, Easy-going / Steady (Suggested) / Ambitious / Set my own (steps of 500), pause hint, CTA "Set N floors a week"
- [x] 1f Add widget *(device ✓ after BUG-009; mock widget type fixed)*: home-screen mock with medium widget, title, body, "Add widget" (requestPinAppWidget), "Maybe later"; both → Build with first floor drop replayed
### Build (`feature/build`)
- [x] 2a In progress (day) *(device ✓; number line box fixed; outline capped 10 floors ahead, BUG-010 / D24)*: sky header, tower with crane + progress, Next floor card (hero number, 10 segments, steps/floors today), week card (ring %, "N of M floors this week", "X to go · D days left", chevron)
- [x] 2b Morning (dawn) *(device ✓)*: greeting overline before 11:00 ("GOOD MORNING · ART DECO"), nearest-milestone copy ("4 floors to topping out")
- [x] 2c Target met (dusk) *(device ✓)*: light header ink, "Target met" chip pop, "8,420 of 7,000 today", "Anything more today is a bonus", lights flicker
- [x] 2d Zero steps *(device ✓; 48h link fixed, BUG-012)*: calm copy "A fresh day. Your tower is right where you left it.", outlined first segment, "Resting? You can pause goals"; after 48h zero → link to 10a
- [x] 2e Syncing / stale *(device ✓ chip and dimming; snackbar — needs new floors from Health Connect)*: dimmed number, "Syncing…" chip with rotating icon, "Updated 12 min ago", pull-to-refresh, snackbar "Synced. N floors added." + View, floors drop in sequence, number never goes backwards
- [x] 2f Dark (night) *(device ✓)*: night sky, sheet blends into dark background, dark nav
- [x] Goals paused *(device ✓)*: week card "Goals paused. Floors still count." in neutral grey
- [x] Steps not connected (permission denied) *(device — permission not revoked on the user's phone; screenshot test)*: demo tower + "Connect steps to start building." card
- [x] *(device ✓)* Tap tower → zoom 1.4× to active floor with its step count; long-press → rotate 90° (other façade)
- [x] Font scale > 150% *(device — needs the system font size changed by the user)*: hero shrinks to 200dp, card area scrolls
- [x] Loading and error states
### Topping out (`feature/build` ceremony route)
- [x] 3a light (dusk) / 3b dark (night) *(device ✓; reward sentence fixed)*: beams, close, tower with crown `lift` + lights, sheet "TOPPED OUT", name, stats, +Blueprints (roll 0→N), Share + Start next tower
- [x] Sequence 0.0 crown lift + haptic, 0.9 windows + beams, 1.6 sheet rises 300ms; tap to skip; optional clack sound respecting silent mode
- [x] *(device ✓; Close → Skyline → Build fixed, BUG-011)* Start next tower → style + name picker → Build; Close → Skyline with new tower highlighted; paywall once after first ceremony is dismissed
### Skyline (`feature/skyline`)
- [x] 4a New tower joined *(device ✓)*: header DISTRICT N, district name, "N towers · F floors · since d MMM", next-floor pill, share; skyline with amber pin (24h); reward card + "Watch a short ad to double Blueprints" + disclaimer; tower chips row
- [x] 4b Tower stats sheet *(device ✓ after BUG-013)*: thumb, "TOWER N · STYLE", name, 4 stat tiles, rows (Steps, Best day, Crown · palette), Share tower / Rename; swipe between towers; selected tower lifts 4dp + pin
- [x] 4c Dark default *(device ✓)*: next-district card (8 segments, "5 of 8 towers", copy), next lot row
- [x] Pinch zoom 1–3×, drag pan; empty skyline state (first tower under construction)
- [x] Rewarded ad hidden for Architect Pass members (2× by default)
### Customise (`feature/customise`)
- [x] 5a Facade tab *(device ✓; achievement locks show the condition)*: live preview above sheet, Blueprints balance chip, tabs Facade / Palette / Crown / Lights / Ground, 3-column tiles with three lock types (progress, Blueprint price, Premium)
- [~] 5b *(device ✓, D19/D20)* Palette swatches, Window lighting radio list, Ground details chips, sticky "Done" — *gap: no 64dp sticky thumbnail on full expansion (deviation D16)*
- [x] Crown tab
- [x] *(device ✓)* Locked tile tap → 3s "Preview" on the tower, then unlock-explainer sheet; Premium → paywall; buy with Blueprints
- [x] Re-skin bottom-to-top 400ms
### Me (`feature/me`)
- [x] 6a Me *(device ✓; BUG-014, BUG-015 fixed)*: title + settings, next-floor pill, Architect level card, stats (Build Days / Floors / Towers), Floors built (Week/Month toggle, bars, today amber, future outlined), Last 4 weeks (paused week icon), Achievements grid + "See all 24", Architect Pass card, settings list (Goals, Pause goals switch, Step source, Notifications, Appearance, Help & privacy)
- [x] Pause goals sheet *(device ✓, no design frame)*: 3 days / 1 week / until I turn it back on
- [x] Weekly detail "This week" *(device ✓, no design frame, D25; BUG-004)*: opened from the Build week card; floors of goal + bar, status line, Me week bars, Pause goals switch (pause sheet / resume), Goals row
- [x] Goals screen (daily target options, weekly goal, adaptive suggestion)
- [x] Notifications, Appearance (Follow system / Light / Dark), Help & privacy, Settings screens *(2026-11-21: notification rows read "Off" while the OS blocks delivery (BUG-001); Help & privacy adds "Ad privacy choices" only when UMP requires it (no design frame, see P13) and opens on its own from Health Connect's privacy link)* *(Help & privacy: "Terms of service" link, D26)*
- [~] 7a Achievements list *(device ✓; card line heights fixed)*: In progress (progress + reward chip), Earned (date + reward); badge flip — *gap: the "Equip" chip is shown for 7 days but is a label, not a shortcut into Customise*
### Share (`feature/share`)
- [x] 8a Composer *(device ✓, D18)*: preview, Tower/Skyline toggle, Story 9:16 / Square 1:1, "Show step stats" switch, Download, "Share image" (share sheet + caption); 150ms crossfade; save result snackbar (BUG-003)
- [x] *(device ✓, exported PNGs)* 8b Story cards tower / skyline (1080×1920, safe zones top 14% / bottom 10%)
- [x] *(device ✓, exported PNGs)* 8c Square cards tower / skyline no stats (1080×1080); brand mark
### Paywall (`feature/paywall`)
- [x] 9a *(device: error state ✓; plan cards need Play products, TECH-004 — screenshot test only)*: close + Restore, dusk hero with 3 towers, title, body, 3 benefits, Yearly (trial, Save 58%) / Monthly / Lifetime, CTA follows plan, legal copy with trial end date; loading/error/purchased states — *2026-11-21: terms and privacy links under the purchase terms (D26)*
### Help (`feature/me` step-source route)
- [x] 10a *(device ✓; step-card heights fixed)*: "Steps not arriving?", last-data card + status, 3 numbered steps (reordered for Samsung), deep links, "Check again" inline result, "Still stuck? Send a diagnostic"
- [x] HC not installed / needs update / permission revoked / sensor fallback states
### Widgets (`widget`)
- [x] *(device — not placed on the user's home screen)* 11a Small 2×2 (silhouette mono tower, FLOOR N, number, bar), light + dark
- [x] Medium 4×2 (sky panel tower with crane, name, number, floors today, week x/y, bar), light + dark
- [x] Tap → Build; update after each sync

## 5. Copy (verbatim, `strings.xml`)
All UI strings from the design are in `values/strings.xml` under the screen prefixes (`onb_`, `build_`, `ceremony_`, `skyline_`, `customise_`, `me_`, `ach_`, `share_`, `paywall_`, `help_`, `widget_`). Copy audit: no "streak", "missed", "lost", "behind", "only" in user-facing strings.

## 6. Accessibility
- [x] Tower content description "Marlow Spire, 14 of 24 floors, next floor in 340 steps."; segment bar "Floor 15: 660 of 1,000 steps"
- [x] 48dp touch targets, selection never colour-only, AA contrast pairs, font scale to 200%
