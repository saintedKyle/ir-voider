# Google Play release guide

Status: **technical build ready; Play Console account and testing required.**

IR Voider is a free, phone-only Android app. This document covers everything needed
to submit it to Google Play.

## 1. Account prerequisites (only the owner can do these)

- **Google Play Console account** — one-time **$25** fee, identity verification.
- **New personal developer accounts** created after 13 Nov 2023 must run a
  **closed test with at least 12 testers opted in for 14 consecutive days** before
  applying for production access. Organization accounts are exempt.
- Accept the Play Developer Distribution Agreement and provide a **contact email**
  and developer address.
- Keep the **upload keystore** and its passwords backed up. Losing them prevents
  updates unless you reset the upload key.

## 2. Build facts

| Item | Value |
| --- | --- |
| Package | `com.kylem.remote` |
| Version name | `1.0` |
| Version code | `7` (Play build; GitHub release APK is code 6) |
| Min SDK | 23 (Android 6.0) |
| Target SDK | **36 (Android 16)** — required for new apps since 31 Aug 2026 |
| Permissions | `android.permission.TRANSMIT_IR` only (normal, no runtime prompt) |
| Internet | None |
| Ads / IAP | None |
| Native libs | None |
| Format | Android App Bundle (`.aab`) |

The app has been updated for edge-to-edge on Android 15/16 with window-inset
padding in all screens.

## 3. Create the upload key

On the build machine:

```bat
make-upload-key.bat
```

This creates `keys/irvoider-upload.jks` and `keystore.properties` (both gitignored).
Back up both files and the passwords somewhere safe.

Then build the signed bundle:

```bat
gradlew.bat bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Verify it is signed:

```bat
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

Do not commit the keystore or `keystore.properties`.

## 4. Store listing copy

**App name (30 chars max):** `IR Voider`  
**Short description (80 chars max):**  
`Offline IR remote for Android phones with a built-in IR blaster.`

**Full description:**

```
IR Voider turns your Android phone into a clean, offline universal IR remote.
No ads, no account, and no internet permission — just your phone's IR LED.

BUILT FOR PHONES WITH IR
IR Voider works only on phones with a built-in infrared emitter. It is not
for iPhone, iPad, tablets, TVs, PCs, or phones without IR hardware.

33 DEVICE CATEGORIES
TV, air conditioner, projector, fan, LED lighting, soundbar, audio receiver,
speaker, streaming box, cable/satellite box, DVD/Blu-ray, VCR, camera, air
purifier, heater, humidifier, vacuum, monitor and more.

1,421 REMOTES · 10,180 IR CODES
Codes come from the public Flipper-IRDB database and are bundled offline.

FIND A WORKING CODE
Pick your brand and model, then press POWER. If nothing happens, use Find code:
it sends one power code at a time and shows the exact model being tested. When
the device reacts, tap "It worked". Missed it? "Previous" replays the last code.

YOUR DEVICES
Name your TV, AC or projector, save it, and switch between devices. Dynamic
controls show only the buttons available for that remote.

NO NETWORK · NO TRACKING
IR Voider requests only the TRANSMIT_IR permission. There is no internet
permission, no analytics, no ads, and no account.

LIMITS
- IR is line of sight; point the top edge at the device.
- Range depends on the phone and the device receiver (often 3–5 m).
- The phone cannot receive IR, so direct learning is not possible.
- Built-in aircon profiles expose Power only; full temperature/mode frames need
  a separate encoder set or an imported raw capture.

Source code: https://github.com/saintedKyle/ir-voider
Phone list: https://github.com/saintedKyle/ir-voider/blob/main/PHONE_COMPATIBILITY.md
Privacy: https://saintedkyle.github.io/ir-voider/privacy.html
```

**Category:** Tools  
**Tags:** remote, IR, TV, universal remote  
**Contact email:** `YOUR_EMAIL_HERE`  
**Privacy policy URL:** `https://saintedkyle.github.io/ir-voider/privacy.html`  
**Website:** `https://saintedkyle.github.io/ir-voider/`

## 5. Graphics

| Asset | Requirement | File |
| --- | --- | --- |
| App icon | 512×512 PNG, 32-bit | `docs/play/icon-512.png` |
| Feature graphic | 1024×500 PNG/JPEG, no alpha | `docs/play/feature-graphic.png` |
| Phone screenshots | 2–8, actual app screenshots, 16:9 or 9:16 | **need real device screenshots** |

Do not use the illustrative README previews as Play screenshots. Take them on
your Redmi Note 10 5G: install the app, open Remote, More and Devices, and capture
2–4 screenshots.

## 6. App content forms

- **Privacy policy:** use the URL above.
- **Ads:** No.
- **App access:** All functionality available without login.
- **Content rating:** Utility/Tools; no violence, sex, profanity, gambling,
  drugs, or user-generated content. Expected rating: Everyone.
- **Target audience:** 13+ (not designed for children).
- **News app:** No.
- **Data safety:** No data collected or shared. No analytics, ads, or third-party
  SDKs. No internet permission.
- **Permissions:** `TRANSMIT_IR` is a normal permission; no sensitive-permission
  declaration is required.
- **Government / financial / health apps:** No.

## 7. Upload flow

1. Play Console → **Create app** → name `IR Voider`, English, App, Free.
2. Complete **App content** (privacy, ads, access, content rating, target
   audience, data safety).
3. **Testing → Closed testing** → create a track → upload
   `app-release.aab` → add at least **12 testers** → roll out.
4. Keep testers opted in for **14 consecutive days**.
5. Apply for **production access**, complete the questionnaire, upload store
   listing and screenshots.
6. Submit for review.

## 8. Notes

- Google Play requires **AAB**, not APK, for new apps.
- Target API 36 is mandatory now; this project has been updated.
- The GitHub APK remains available for direct sideloading.
- Google may take a few days to review the first release.
