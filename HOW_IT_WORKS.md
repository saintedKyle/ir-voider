# How IR Voider works

## The short version

The phone's IR LED blinks invisible light in precise patterns. The TV, AC, fan or
projector has an IR receiver that decodes those patterns into button presses.
IR Voider stores patterns and sends them with Android's
`ConsumerIrManager.transmit(frequency, pattern)`. There is no Wi-Fi, Bluetooth,
account or server involved.

## IR packets

A command is a list of on/off durations in microseconds:

```
9000, 4500, 560, 560, 560, 1690, ...
```

Different protocols (NEC, NECext, Samsung32, RC5, RC6, SIRC, Kaseikyo, RCA,
Pioneer) use different headers, bit timings and bit orders. The app stores each
button as a finished timing array, so sending does not require decoding at
runtime.

## Where the codes come from

The built-in database is generated from
[Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB), which is public domain
(CC0). Parsed Flipper protocols are converted to raw timings by
`tools/build_db.py`; Flipper raw captures are used directly.

Database at release 1.0:

- 33 categories
- 1,454 remotes
- 17,278 button patterns

The app loads the database once at startup. No network request is made.

## Finding a code that works

1. **Brand/model** — try the known profile first.
2. **Scan** — send the power codes one by one, deduplicated by pattern. The
   screen shows the brand and model being tested.
3. **It worked / Previous** — confirm the last code, or replay the previous one
   if you reacted late.
4. **Save** — the selected profile is stored locally and reused next launch.

Power is a toggle on most devices, so the same code turns the device on and off.

## Swipeable control pages

Controls are grouped into pages: **Basics**, **Navigation**, **Media**,
**Numbers**, **Colors** and **Extras**. Swipe left/right or use the ‹ › buttons.
POWER stays pinned above the pages. Only buttons present in that remote are shown.

## IR boost

Some buttons (volume, channel, temperature, brightness) are sent two or three
times with a short gap. **Boost all** repeats every button except POWER; turn it
on in IR & scan settings if a device is unreliable. It can double-trigger
toggles or OK on sensitive devices. Power, OK and navigation are sent once by
default so they cannot double-fire.

**Software cannot amplify the IR LED.** Android exposes frequency and pattern,
not current or power. Real range improvements require an external IR blaster.

## No learning on this phone

A phone with only an IR emitter cannot receive IR. The Redmi Note 10 5G has no
IR receiver, and Android has no API to read IR codes.

To copy an original remote you need external hardware, for example a TSOP38238
receiver connected to an ESP32. Capture the code there, save it in a raw `.ir`
file, and import it through **More → Fetch original remote**. Imported remotes
appear under the **Learned** brand in the current category.

## Limits

- Line of sight: point the top edge at the device's IR window.
- Range depends on the phone's LED and the device's receiver (often 3–5 m).
- Sunlight, fluorescent/plasma light and some cases reduce range.
- Built-in air-conditioner profiles include the captured buttons (power, temp,
  mode, fan where available). Raw AC frames can set a fixed state rather than
  step; unsupported AC protocols are not in the database.
- Some ACs need the same frame repeated. Enable **Repeat aircon POWER** in
  IR & scan settings to send it using the boost count.
- The phone cannot control RF, Bluetooth-only, or Wi-Fi-only devices.

## Permissions and privacy

IR Voider requests only `android.permission.TRANSMIT_IR`. It has no `INTERNET`
permission and no networking code. Settings, saved devices and imported codes are
stored locally; Android backup is disabled.
