# Design deviations and decisions

Where the design is ambiguous, or a web pattern has no direct native equivalent, the closest Material 3 / Compose
behaviour was chosen while keeping the look. Every such decision is listed here.

## Platform and rendering

| # | Design | Implementation | Why |
|---|---|---|---|
| D1 | Status bar mock (`SSStatusBar`, 28px, "9:41") on every frame | Real system status bar, edge-to-edge; screen offsets are measured from the bottom of the real inset (design offset − 28dp) | The mock only stands in for the OS bar |
| D2 | Phone frame 360×800 with 28px corners and drop shadow | Not drawn; full-screen layouts. Previews/screenshot tests use 360×800dp | Presentation-only frame |
| D3 | `SSNav` includes a 24px gesture-handle strip | `SsBottomBar` is 80dp + the real navigation-bar inset (gesture handle is drawn by the OS) | Avoid a fake second handle |
| D4 | SVG tower with `preserveAspectRatio="xMidYMax meet"` | `TowerCanvas` ports the viewBox fit exactly (pad, minh, minw, bottom alignment); `tower.js` points are rounded to 0.1 like `p()` | — |
| D5 | `ssDrop`/`ssLift` CSS keyframes, `cubic-bezier(.3,1.3,.5,1)` (design note says "spring(0.6, 400)") | Ported the CSS keyframes and bezier exactly (overshoot 1.6 / 1.4 units) | The prototype's actual motion; a Compose spring would differ visually |
| D6 | `prefers-reduced-motion` disables animations | "Remove animations" (animator scale 0) shows the final state with a 150ms crossfade, per the motion board | Design board rule |
| D7 | Material Symbols Rounded variable font with optical size = font size | Static instances at opsz 24, wght 400, FILL 0 / FILL 1, subset to the ~95 glyphs used | Variable-axis animation is not needed; subsetting keeps the APK small |
| D8 | DM Sans uses automatic optical sizing | Static opsz 14 instances for text and an opsz 40 instance for the large step numbers (40–64sp) | Matches the browser's auto-opsz for these sizes without variable-font rendering cost |
| D9 | Font sizes in px | Converted 1:1 to sp (scale with the user's font size, tested to 200%); share cards use a fixed font scale | Accessibility requirement; share images must be stable |
| D10 | Long-press "rotate 90° to see the other façade" | Mirrors the tower horizontally (the renderer only has the two visible faces, as in tower.js) | tower.js draws two faces; mirroring shows the "other side" without inventing geometry |
| D11 | Tower content description "Marlow Spire, 14 of 24 floors, next floor in 340 steps." | Implemented verbatim, plus a hint for the tap/long-press actions | — |
| D12 | Sky crossfades 20 minutes around boundaries | Gradients crossfade linearly (`SkyGradient.blend`); the tower tint/lit windows switch at the dominant sky (blend ≥ 0.5) | tower.js has discrete times only |
| D13 | Widgets: DM Sans / Big Shoulders, 6px segment bar | Glance cannot load custom fonts: system font with the same sizes/weights; the medium widget's week bar is quantised to tenths (Glance has no fractional widths) | Glance limitations |
| D14 | Widget sky panel is a CSS gradient | Two-stop gradient drawables (day / night) | Glance backgrounds |
| D15 | Bottom sheets "300ms emphasized-decelerate; drag down to dismiss" | Material 3 `ModalBottomSheet` (its own spring) for modal sheets; Customise opens as a full-height route sliding up 300ms emphasized-decelerate | Closest native behaviour |
| D16 | Customise sheet collapses the preview into a sticky 64dp thumbnail when dragged to full height | Customise is a fixed 73% sheet with the live preview always visible above it; content scrolls inside | Simpler, always shows the live preview |
| D17 | Sheet scrim `#24211D` at 32% | Light theme exactly; dark theme uses black at 48% for contrast on dark surfaces | Dark-mode legibility |
| D18 | Share composer preview at 25%/28%, with shorter preview copy ("24,310" / "steps · 11 Build Days") | Preview at the same on-screen size (198×352 / 280×280dp) and shows the exact export (8b/8c copy "24,310 steps" / "in 11 Build Days", date in the overline); export renders the same composable at exactly 1080×1920 / 1080×1080 px | The preview is what gets shared |
| D19 | Customise has no close control (drag the sheet down) | A ✕ next to the Blueprints balance | Customise is a full-height route (D15), so it needs an explicit, accessible dismiss |
| D20 | 5b shows Colour palette, Window lighting and Ground details in one scroll under the Palette tab | Each section is its own tab (Palette / Lights / Ground) with the designed swatches, radio list and chips | Tabs already exist for each section; one long list would duplicate them |
| D21 | Body text without an explicit CSS line height uses `line-height: normal` (≈1.30em) | Tokens keep their ≈1.4em line heights (label 14/19.6, bodyS 14/21…); blocks drift ≤3dp. Card text where it added up (option/plan, achievement and help-step cards, 1f mock widget) uses the `card*` tokens at 1.30em | Changing every token would ripple through all screens for a few dp |
| D22 | Line heights below the font's height (numbers 60/63, tower names, headlines) | Android keeps the font's full ascent/descent on the outer lines; `cssLineBox` trims the box to the CSS line height on every such text | Matches the browser layout (was up to +15dp per number) |
| D24 | tower.js draws the dashed outline up to the target | The outline reaches at most 10 floors above the built ones and is left open at the top when capped (BUG-010) | On the 100–250-floor ladder the full outline shrank the tower to a sliver; every design frame shows ≤ 10 floors ahead, so they render unchanged |
| D23 | Frames are 360×800 | Layouts stretch to the phone (Pixel 6a: 411×914dp): hero towers scale with the hero box and bottom-anchored blocks move down | Responsive layout; checked on device |

## Product and data decisions

| # | Design | Implementation | Why |
|---|---|---|---|
| P1 | Sample towers of 12 and 24 floors | Heights follow the brief's blueprint ladder 10, 25, 50, 100, 160, 250 (Remote Config `tower_ladder`); a tower's target is frozen when it is founded | The brief is authoritative; freezing keeps history stable if the ladder changes |
| P2 | "Every 8 towers founds a new district" | Implemented; district names are generated ("Juniper Quarter", "Linden Yards"…) | — |
| P3 | Architect levels ("Level 7 Site Architect, 100 of 120 to Level 8") | 15-level table (`CalculateArchitectLevelUseCase`); level 8 starts at 120 floors | Design shows one data point only |
| P4 | "24 achievements" with a few examples | 24 achievements defined (none depends on consecutive days); rewards are Blueprints or the cosmetics named in the design | Design shows 9 of 24 |
| P5 | "12 premium facades, crowns and palettes" | 4 premium facades, 2 premium crowns, 6 premium palettes; Slow twinkle lights and Market stalls ground are the premium "lighting and ground sets" | Exact list not in the design |
| P6 | Extra grounds (Canal, Tram stop, Market, Name plaque) and crowns (Gilded, Beacon, Hammock terrace) | Drawn with small additions to the ported renderer (boxes/lines in the same isometric system and palette) | tower.js only has plaza/park and the 5 kit crowns |
| P7 | Night Shift ("after 9 pm") / Early Riser need time of day | Floor completions are timestamped when a sync first sees them (`floor_events`); sync runs every 15–30 min, so the time is approximate | Only daily totals are read (privacy, idempotence) |
| P8 | Step-counter fallback | Sampled on each sync (no foreground service); steps since the previous sample land on the day of the sample | No service/notification allowed; the sensor has no per-step timestamps |
| P9 | Share card sky "taken from when the tower was finished" | Only the finish date is stored, so cards use the design's examples: tower story dusk, skyline story night, tower square day, skyline square dawn | No finish time-of-day available |
| P10 | Notifications: "Topping out and weekly summary only" | Both are local notifications posted after syncs (weekly summary on Sunday from 18:00) | No backend |
| P11 | Paywall prices `$2.99 / $14.99 / $29.99` | Prices come from Google Play (`ProductDetails`); the design prices are only previews/test values | Localised, real prices |
| P12 | "Still stuck? Send a diagnostic" | Opens an email/share draft with sync timestamps and source status only (never step data); address from `SUPPORT_EMAIL` | No backend |
| P13 | Privacy policy link | Shown only when `PRIVACY_POLICY_URL` is configured; the full policy summary is in-app (Help & privacy) | URL must be provided by the publisher |
| P14 | Firebase (Analytics, Crashlytics, Remote Config) | Applied only when `app/google-services.json` exists; otherwise the app runs with the bundled ladder and drops analytics locally | No Firebase project is available in the repo |
| P15 | AdMob ids | Google's public sample app/unit ids by default (`gradle.properties`); real ids must be set before release | Real ids belong to the publisher |
| P16 | Cosmetic item names | English names from the domain catalogue (not yet in `strings.xml`) | Catalogue is shared with domain logic |
