# IR Voider

<p align="center">
  <img src="docs/images/banner.png" alt="IR Voider — offline IR remote for Android phones" width="100%">
</p>

IR Voider turns an **Android phone with an IR blaster** — built-in, or two LEDs you
soldered onto the 3.5 mm jack — into a clean,
offline universal remote. No ads, no account, no tracking — just the phone's
IR LED.

**[Download the latest APK](https://github.com/saintedKyle/ir-voider/releases/latest)** ·
[Phone compatibility](PHONE_COMPATIBILITY.md) · [Privacy](PRIVACY.md)

## Install on your phone

1. Download the latest APK from [Releases](https://github.com/saintedKyle/ir-voider/releases/latest).
2. Open it on the phone. Android asks to allow installs from your browser or file
   manager — allow it.
3. Install **IR Voider** and open it.

Requires **Android 6.0+** and an IR emitter: either the phone's **built-in blaster**
or a **DIY two-LED blaster on the 3.5 mm headset jack** (see the jack-blaster bullet
below). It does not work on iPhones, iPads, tablets, TVs, PCs, or phones that have
neither. On MIUI/HyperOS:
**Settings → Apps → Special access → Install unknown apps**.

The APK is sideloaded, so Android may show a warning. Only install builds from
this repository's Releases.

> Upgrading from an earlier v1.4 APK? 1.5 installs straight over it and keeps your
> saved devices. Only the original debug-signed build has to be uninstalled first —
> Android blocks in-place updates when the signing key changes.

## Use

1. Tap **≡ More** (top-left) and pick a category: TV, air conditioner, projector,
   fan, LED light, soundbar, and more.
2. Pick your brand and model — or tap **Search** next to either row and type, e.g.
   `HWE08`, to jump straight to that profile. Then press **POWER**.
3. No reaction? Tap **Find code**. It sends one power code at a time; when the
   device reacts, tap **It worked**. **Previous** replays the last code.
4. Save and name the remote with **⚙** (top-right).

## What it does

- **40 categories · 2,050 brands · 5,666 remotes · 90,054 IR codes** from Flipper-IRDB,
  bundled offline.
- TVs, ACs, projectors, fans, LED lights, soundbars, audio receivers, streaming
  and cable boxes, DVD/Blu-ray players, cameras, air purifiers, and more.
- Device-specific buttons on swipeable pages: Basics, Navigation, Media, Numbers,
  Colors, a Keyboard page when the profile has letter keys, and Extras. Basics
  are ordered per device type, pairs such as
  volume and channel stay side by side, the D-pad keeps its cross layout, numbers
  form a 3×4 pad, colours sit in one row, and two-button rows fill the width.
  Power stays pinned above the pages.
- **One tap sends exactly one code.** Volume, channel, temperature and everything
  else fire once, the way a press on the original remote does.
- **Hold any button to keep sending it** — release stops. Power is the exception:
  holding a toggle would strobe the device on and off, so it stays one press.
- **Type to the device.** A **Type to TV** button opens the phone keyboard and sends
  each character as one IR key press — digits, dash and dot on a normal media
  remote, plus letters on any profile that carries letter keys. TVs type letters
  through their on-screen keyboard; IR has no universal text protocol, so the panel
  says which characters this profile can actually send and skips the rest.
- **Type to search brands and models** on the *Find a working code* card. One field
  filters every profile in the category, so `HWE08` or `AS18` finds the aircon
  without scrolling a spinner.
- **Explicit aircon On and Off.** A profile that captured both directions shows
  **POWER ON** and **POWER OFF** instead of one guessing toggle, and **POWER ×2**
  re-sends with a real gap for units that ignore a single frame. Every other device
  type now shows **POWER OFF** too — 1,044 remotes carried an Off code the old
  interface never displayed.
- Searchable brands; scanner with **Last 5** history, Previous/Pause/Stop, and a
  confirmation before an all-brand scan.
- **Scan speed down to 0.2 s** per code — the **Speed** row on the *Find a working
  code* card, 10× faster than the old 2 s default and 2.5× faster than 1.6.3. The
  scanner now starts the next code when the previous frame has actually finished,
  not on a blind timer, so fast scanning never truncates a code. A **Range** row
  limits an all-brand scan to brand initials, e.g. **F–R**, so you can skip the
  brands you have already ruled out.
- **Honest provenance.** Codes that come from community dumps rather than this app's
  own bench runs are labelled **untested source** in the scanner and in the About
  panel. Nothing is claimed to work that has not been seen to work.
- Saved devices with custom names, opt-in **IR boost** / **Force mode**, diagnostics.
- **Ten themes** (orange, gold, green, teal, blue, violet, rose, silver, black, light)
  — pick one under **≡ More → Appearance**.
- Air-conditioner temperature, mode, fan and swing where the profile supports it.
- Import raw `.ir` captures as learned remotes.
- **IR override** for phones that report NO IR despite having an emitter.
- **DIY jack blaster** — send IR out of the 3.5mm port. The app plays a 19 kHz tone
  and a pair of IR LEDs wired back-to-back turns it into 38 kHz of light, because a
  48 kHz audio output cannot produce 38 kHz itself. Set up under **≡ More → Jack
  blaster**. Needs two LEDs and a 47–100 Ω resistor; range is about 1–2 m.
- **Scan clock** — elapsed time and an estimate of what is left, paused correctly
  when you pause, with the screen held awake for the whole run.
- **Optional update check** — the only network code in the app. It asks GitHub for
  the newest release once per app open and is one tap away in **≡ More → Updates**.
  Everything else, including every remote and scan, works in airplane mode.

Air conditioners include the buttons captured in the database. Raw AC frames can
represent a fixed state rather than an incremental step, and a captured power code
may be one-directional — that is why profiles with both an On and an Off capture
show two separate buttons. If your model is missing, its protocol is not in the
database; a raw `.ir` capture can add your own buttons.

## Look

Nothing in the app is a flat fill. Buttons, cards, chips, the scanner track and
the bottom sheet are shaded plates — lit top rim, body gradient, contact shadow —
drawn in code, and they sink under a press.

Ten themes re-colour all of it, and each brings its own neutrals, so a blue accent
sits on blue-black surfaces instead of a warm card with a cold button: **Ember
Signal** (orange), **Void Amber** (gold), **Terrace Green**, **Teal Current**,
**Cobalt Pulse**, **Ultraviolet**, **Rose Signal**, **Smoke Signal** (silver),
**OLED Void** (true black for AMOLED) and **Bone Frost** — the light one, for
daylight. The layout, type and control positions never change.

**≡ More → Appearance** switches themes instantly. Saved devices, IR settings and
scanner history are untouched, and the choice is remembered on the phone.

<p align="center">
  <img src="docs/images/themes.png" alt="The ten themes: Ember Signal, Void Amber, Terrace Green, Teal Current, Cobalt Pulse, Ultraviolet, Rose Signal, Smoke Signal, OLED Void and Bone Frost" width="100%">
</p>

## How IR works — and the limits

The phone's IR LED blinks invisible light in a precise pattern; a receiver in the
device reads the pulses. IR is **one-way and line-of-sight**:

- the phone cannot detect, identify, or automatically control nearby devices;
- IR does not pass through walls;
- RF and Bluetooth devices are not supported;
- some air conditioners need the exact model protocol.

Only scan devices you own or have permission to control.

## Phones

See **[PHONE_COMPATIBILITY.md](PHONE_COMPATIBILITY.md)** for the **535-listing IR
phone list**, the confirmed model, historical IR phones that cannot run the app,
and a 10-second check. It is a public-data snapshot, not a guarantee — same-name
phones can ship with or without IR by region. IR blasters are not part of
Android's requirements, so the app asks the hardware and shows **IR READY** (an
emitter the OS reports), **JACK BLASTER** (jack sending is on and the port reports a
plug) or **NO IR**. No label can prove light left an LED, because a phone has no IR
receiver to look for it — the jack screen reports where the sound was *measured* going
instead, which is the closest thing to a real check.

## Privacy

No account, analytics, ads, or network access. The only permission is
`TRANSMIT_IR`. See [PRIVACY.md](PRIVACY.md) or the hosted policy:
https://saintedkyle.github.io/ir-voider/privacy.html

## Build from source

Requirements: JDK 17+, Android SDK Platform 36, Build-Tools 36.0.0.

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

The IR database can be regenerated with `python tools/build_db.py` after
downloading [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) into
`data/irdb_all/`. It reads both the top-level captures and the `_Converted_/`
dumps, drops anything Android cannot transmit, and writes `assets/remotes.json`
(the index) plus one `assets/db/<category>.json` shard per category, which the
app loads on demand. The README artwork comes from `python
tools/make_readme_images.py` and `python tools/make_theme_sheet.py`, which redraw
the same shading maths in Pillow.

## License, forks and contributions

**License:** [GPL-3.0-or-later](LICENSE). Copyright and credits are in
[NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

**Forking:** the GPL permits forks, and GitHub does not allow disabling forks on
a public repository — a public open-source repo is forkable by design. The
maintainer does not support or endorse modified builds. The **IR Voider** name
and logo are not licensed for forks or redistributed builds.

**Contributions:** this is a personal project; external contributions and pull
requests are not accepted.

## Preview

<p align="center">
  <img src="docs/images/preview.png" alt="IR Voider UI preview: remote, scanner and air-conditioner screens" width="100%">
</p>

<p align="center"><em>Illustrative v1.5 UI preview — shaded plates, not flat fills. Buttons depend on the selected remote profile; pages are swipeable.</em></p>

## Docs

- [PHONE_COMPATIBILITY.md](PHONE_COMPATIBILITY.md) — which phones work
- [PRIVACY.md](PRIVACY.md) — no data collection
- [TERMS.md](TERMS.md) — terms of use
- [SECURITY.md](SECURITY.md) — reporting a vulnerability
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) — code and data credits

See [Releases](https://github.com/saintedKyle/ir-voider/releases) for version history.

---

Made by **poging kyle**.
