# Building IR Voider

## Requirements

- Android Studio (latest stable) **or** JDK 17+ with the Android SDK.
- Android SDK Platform 36 and Build-Tools 36.0.0.
- Android Gradle Plugin 8.10.1 and Gradle 8.11.1 (wrapper included).
- Python 3.10+ only if you want to regenerate the IR database.

The `toolchain/` and `data/` folders used during development are **not** part of
the repository.

## Build the APK

### Android Studio

1. Open this folder as a project.
2. Let Gradle sync and install SDK 34 if prompted.
3. Build → Build Bundle(s) / APK(s) → Build APK(s).

### Command line

Create `local.properties` with your SDK path:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

The bundled `dist/IRVoider-1.4.apk` is signed with a debug key for sideloading.

## Google Play bundle

1. Run `make-upload-key.bat` once and back up `keys/` and the passwords.
2. Build the signed bundle:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Never commit `keys/` or `keystore.properties`. See [PLAY_STORE.md](PLAY_STORE.md)
for the listing, testing requirements and submission steps.

## Regenerate the IR database

1. Download [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB).
2. Extract the archive and place the top-level device folders in `data/irdb_all/`
   (so `data/irdb_all/TVs`, `data/irdb_all/ACs`, etc. exist).
3. Run:

```bash
python tools/build_db.py
```

This regenerates `app/src/main/assets/remotes.json`. The script converts parsed
Flipper protocols to raw timings and copies raw captures.

## Versioning

- `versionName` is the public release (`1.4`).
- `versionCode` is internal and must increase for every update.
- The Play build uses `versionCode 12`.

## Source map

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/kylem/remote/MainActivity.java` | Main remote UI, scanning, categories |
| `app/src/main/java/com/kylem/remote/ConfigsActivity.java` | Saved devices and settings |
| `app/src/main/java/com/kylem/remote/LearnActivity.java` | Raw `.ir` import |
| `app/src/main/java/com/kylem/remote/DeviceStore.java` | Device/settings persistence |
| `app/src/main/java/com/kylem/remote/CustomStore.java` | Imported remotes |
| `tools/build_db.py` | Flipper-IRDB → raw pattern generator |
| `app/src/main/assets/remotes.json` | Generated IR database |
