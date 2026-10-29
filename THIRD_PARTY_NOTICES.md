# Third-party notices

IR Voider is built on open protocol data and reference implementations. The app
is distributed under **GPL-3.0-or-later** to remain compatible with the GPL code
it derives from.

| Project | License | Use in IR Voider |
| --- | --- | --- |
| [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB) | CC0-1.0 | IR code data used to generate `assets/remotes.json` |
| [Flipper Zero firmware](https://github.com/flipperdevices/flipperzero-firmware) | GPL-3.0 | IR encoder logic ported to Python and Java |
| [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266) | LGPL-2.1 | Protocol timing references and encoder behavior |
| Android SDK / platform APIs | Apache-2.0 | Build and runtime platform, not redistributed |

## Notes

- Flipper-IRDB data is dedicated to the public domain under CC0 1.0. Attribution
  is not required, but is given here.
- The Flipper Zero firmware is GPL-3.0. Portions of the protocol encoders in
  `tools/build_db.py` and the Java IR pattern generation are derived from that
  implementation.
- IRremoteESP8266 is LGPL-2.1; GPL-3.0-or-later is compatible with it.
- If you redistribute IR Voider or a modified version, keep the GPL license, the
  copyright notice, and these notices.
