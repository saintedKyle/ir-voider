# Privacy Policy — IR Voider

**Effective: 2026-10-30**

Hosted version: https://saintedkyle.github.io/ir-voider/privacy.html

IR Voider is an offline Android IR remote. It does not collect, transmit or sell
personal data. Every remote feature works with the radio switched off. The single
exception is the optional update check described below, which you can turn off in
one tap.

## What the app accesses

- **IR emitter** — `android.permission.TRANSMIT_IR`, used only to send infrared
  patterns to devices you point at.
- **Files you choose** — when you import a `.ir` capture, Android's file picker
  gives the app temporary read access to that single file. The file is parsed
  locally and is not uploaded.
- **Network** — `INTERNET` and `ACCESS_NETWORK_STATE`, used only by the optional
  update check. See the next section.

## The optional update check

- It requests one public URL: `https://api.github.com/repos/saintedKyle/ir-voider/releases/latest`.
- It runs at most once per app open, only when the phone reports an internet
  connection, and only to read a version number.
- Nothing from the app is sent with the request — no device id, no account, no
  saved remotes, no settings. GitHub sees the request like any web visitor would
  (its server logs your IP address; that is GitHub's logging, not the app's).
- It never downloads or installs anything. If a newer version exists the app shows
  a dialog and you choose Download, Skip this version or Not now.
- Turn it off under **≡ More → Updates → Auto update check**. With it off the app
  makes no network request of any kind, and the `INTERNET` permission goes unused.

## What the app stores

On your device only:

- your selected category, brand, model and matched remote;
- saved device names, types and settings (IR boost, scan speed);
- imported `.ir` remotes.

This data stays in the app's private storage. Android backup is disabled, so it
is not copied to Google Drive or another cloud service by the app.

## What the app does not do

- No network use beyond the optional update check above.
- No accounts, ads, analytics, crash reporting or third-party SDKs.
- No location, contacts, camera, microphone or storage-wide access.
- No tracking of which buttons you press.

## Children

The app is a general-purpose remote control and collects no data from anyone,
including children.

## Changes

If this policy changes, the new version will be published in this repository and
the effective date updated.

## Contact

Open a private security advisory on this repository for security/privacy issues
(see [SECURITY.md](SECURITY.md)).
