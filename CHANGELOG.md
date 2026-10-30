# Changelog

## 1.4 — 2026-10-29

- Scanner shows the **last 5 codes tried**, newest first.
- Added **IR override** for phones that have an emitter but report NO IR.
- Clearer AC Power On/Off guidance.
- Force mode remains repeat-based; IR still cannot pass solid walls or be amplified.

## 1.3 — 2026-10-29

- Searchable brand picker for the expanded model list.
- Scanner Pause/Resume.
- Confirmation before an all-brand scan to avoid controlling nearby devices.
- Scanner keeps trying AC Power and Power Off codes.
- Model database expanded to 39 categories, 1,502 remotes, 17,789 codes.

## 1.2 — 2026-10-29

- Expanded to **39 categories · 1,502 remotes · 17,789 codes**.
- Kept every model profile instead of collapsing duplicate code sets, so more
  exact model names are available.
- Added Clock, Digital Sign, Dust Collector, Head Unit, Universal Remote and
  Video Conferencing categories.
- All available aircon buttons remain included.

## 1.1.1 — 2026-10-29

- Scanner now tries AC **Power Off** codes as well as Power, for units with
  separate On/Off commands.
- Added a dedicated **POWER OFF** button on aircon controls.
- Fixed ACs that appeared to "never be found" while already powered on.

## 1.1 — 2026-10-29

- OLED-black redesign with rounded outlined buttons and cleaner layout.
- Full-screen scanner: big model display, progress, It worked / Previous /
  Skip 5 / Stop, 1 s turbo option.
- Swipeable control pages and expanded buttons (digits, colors, media, info).
- Force mode: 3x repeats with 25 ms gaps (repeat only, not hardware amplification).
- Diagnostics screen with IR status, phone, database and tips.
- All available aircon buttons (temp/mode/fan/swing/timer) where captured.
- AC POWER x2 and opt-in AC POWER repeat.

## Unreleased

- Target Android 16 (API 36) for Google Play submission.
- Edge-to-edge window insets on all screens.
- Release AAB signing setup and Play Store release guide.
- GitHub Pages privacy/terms pages.
- Swipeable control pages (Basics, Navigation, Media, Numbers, Colors, Extras).
- Expanded button set: digits, colors, info/guide, media transport, subtitle,
  audio, aspect, apps and more.
- "Boost all" repeat option for every button except POWER.
- Opt-in AC POWER repeat for air conditioners that ignore a single frame.


## 1.0 — 2026-10-29

First public release.

- Android IR remote for phones with a built-in IR blaster.
- 39 device categories, 1,502 remotes, 17,789 IR patterns from Flipper-IRDB.
- TV, air conditioner, projector, fan, LED lighting, soundbar, audio receiver,
  speaker, streaming box, cable/sat box, disc players, cameras, purifiers,
  heaters, monitors and more.
- Dynamic per-device button layouts.
- Brand/model picker, auto-find with deduplication, "It worked" confirmation and
  "Previous" replay.
- Saved devices with custom names and types.
- IR boost for volume/channel/temperature/brightness repeats.
- Scan speed control (1.5 s / 2 s / 3 s).
- Import raw `.ir` captures as "Learned" remotes.
- IR Voider UI: gradient theme, bottom-sheet More menu, category grid.
- No internet permission; no ads, analytics or accounts.
- Phone-only; Android 6.0+; built-in IR emitter required.
