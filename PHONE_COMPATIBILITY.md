# Phone compatibility

IR Voider is **Android-phone only**. It talks to the phone's built-in IR emitter
through Android's `ConsumerIrManager`. If the phone has no IR emitter, the app
shows **NO IR** and cannot send anything.

## Requirements

- Android **6.0 (API 23)** or newer.
- A built-in **IR blaster** (not an add-on dongle).
- Not supported: iPhone, iPad, tablets, TVs, PCs, watches, or phones without IR.

## Confirmed working

| Phone | Notes |
| --- | --- |
| Xiaomi Redmi Note 10 5G (MIUI 14) | Tested by the maintainer; controls a TCL TV. |

## Commonly reported IR phones

The models below are **examples**, not a guarantee. Manufacturers ship different
hardware by region and model number, so verify before buying or relying on it.

| Family | Examples | Notes |
| --- | --- | --- |
| Xiaomi / Redmi / Poco | Redmi Note 8–14 (select models), Poco X3–X7, Poco F3–F5, Xiaomi 11T/12 series (select) | Many, but not every variant has IR. |
| Huawei / Honor | Mate 20/30, P50 Pro, Honor 8/8 Pro | Older or China-market models; newer flagships often omit IR. |
| vivo / iQOO | X70 Pro+, iQOO 13, T4X/T5 (reports) | Region-dependent. |
| OPPO | K13, Reno 14 (reports) | Region-dependent. |
| Samsung | Galaxy S4, S5, S6, Note 3, Note 4 | Older devices only; S7 and newer have no IR. |
| LG | G3, G4, G5, G6, V20, V30 | Older devices. |
| HTC | One M8, M9, U11 | Older devices. |
| Unihertz | Jelly Max and similar models | Check the specific model. |
| Sony | Select older Xperia models | Verify the model. |

## Not supported

- **iPhone / iPad** — no IR emitter.
- **Google Pixel** — no IR emitter.
- **Most Samsung Galaxy S7 and newer**, most OnePlus, most Motorola and Nokia.
- **Tablets, laptops, watches, TVs** — no phone IR emitter.
- **USB-C / headphone-jack IR dongles** — Android's standard API targets the
  built-in emitter; dongles need vendor-specific apps.

## If the app says NO IR

Android's `hasIrEmitter()` can report false even on a phone that has an IR blaster,
usually because the vendor hides the standard API. Try **⚙ → IR override** (advanced)
and test a real device. If the phone exposes no IR service at all, no app can make
it emit IR. The override only unlocks misreported hardware; it does not add hardware.

## How to check your phone in 10 seconds

1. Install IR Voider. The **Now using** card shows **IR READY** or **NO IR**.
2. Look at the top edge for a small dark IR window (often next to the
   headphone jack).
3. Camera test: point the top edge at a phone camera and press **POWER** in the
   app. Many cameras show a faint purple/white flash from the IR LED.

## Why there is no complete list

IR blasters are not part of Android's compatibility requirements. The same model
name can ship with or without IR depending on region, carrier, and revision.
Publishing a "complete" list would be inaccurate. IR Voider checks the actual
hardware at runtime and reports what it finds.

## Sources

- [91mobiles — phones with IR blaster](https://www.91mobiles.com/list-of-phones/phones-with-ir-blaster)
- [PhoneArena — phones with IR blaster](https://www.phonearena.com/phones/phones-with-ir-blaster)
- [Wikipedia — mobile phones with infrared transmitter](https://en.wikipedia.org/wiki/Category:Mobile_phones_with_infrared_transmitter)

Lists change frequently. Verify the exact model before buying.
