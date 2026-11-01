# Namaz Times · Hubli — Native Android app

The original **native Android app** (Kotlin + Jetpack Compose), kept in this repo
alongside the web PWA on `main`.

- Package `com.namazapp.hubli` · version **1.3.1** · minSdk 26 · targetSdk 35
- Bundled Hubli timetable (`app/src/main/assets/namaz_timings.csv`) — works fully offline
- Pre-prayer reminders via `AlarmManager`, vibrate option, Hijri (Umm al-Qura) dates
- The same deep-emerald + warm-gold Compose UI the web PWA was ported from

## Ready-made APK

A prebuilt, installable release APK ships inside this repo at
[`android/release/NamazTimes-Hubli-v1.3.1.apk`](release/NamazTimes-Hubli-v1.3.1.apk)
(≈ 1.3 MB).

**To install on your phone:** download the APK → open it → allow "Install unknown apps"
for your browser when prompted → **Install**.

Newer signed builds are also published on the repo's
[Releases](https://github.com/kuttappu507/namaz-times/releases) page.

## Build it yourself

```bash
cd android
./gradlew assembleRelease        # APK at app/build/outputs/apk/release/
```

…or open the `android/` folder in **Android Studio** and press **Run**.

## iOS

The SwiftUI iPhone app and the Android/iOS CI workflows live on the
[`native-apps`](https://github.com/kuttappu507/namaz-times/tree/native-apps) branch.
