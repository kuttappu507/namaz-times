# Namaz Times · Hubli

[![Android](https://github.com/kuttappu507/namaz-times/actions/workflows/android.yml/badge.svg)](https://github.com/kuttappu507/namaz-times/actions/workflows/android.yml)
[![iOS](https://github.com/kuttappu507/namaz-times/actions/workflows/ios.yml/badge.svg)](https://github.com/kuttappu507/namaz-times/actions/workflows/ios.yml)
[![Latest release](https://img.shields.io/github/v/release/kuttappu507/namaz-times?label=download)](https://github.com/kuttappu507/namaz-times/releases/latest)

A modern prayer-times app for **Android (Kotlin + Jetpack Compose, Material 3)** with a
native **iPhone port (SwiftUI)** in [`ios/`](ios/README.md).
All timings come from the bundled annual timetable for Hubli, Karnataka
(`app/src/main/assets/namaz_timings.csv`) – the app works fully offline.

**Ready-to-install builds:** [GitHub Releases → v1.3.1](https://github.com/kuttappu507/namaz-times/releases/tag/v1.3.1)
(also mirrored in [`release/`](release/))
* `NamazTimes-Hubli-v1.3.1.apk` – **Android, recommended**: release build, R8-optimised (1.3 MB),
  signed with the debug key so it side-loads on any phone.
* `NamazTimes-iOS-v1.3.1-unsigned.ipa` – **iPhone (iOS 17+)**, built by the `iOS` workflow with
  signing disabled; sideload with Sideloadly / AltStore and a free Apple ID (see [`ios/README.md`](ios/README.md)).
* `NamazTimes-Hubli-v1.3.1-debug.apk` – debuggable Android build (17 MB, noticeably slower UI).

Every push to `main` rebuilds both apps on GitHub Actions (`Android` on Ubuntu, `iOS` on a macOS
runner with Xcode 16.4); the fresh APK/IPA are attached to each run as artifacts.

## What's new in 1.3.1

* **No more interpolation.** A date that is not printed on the sheet now shows the previous
  printed day's times (2nd = 1st, 4th = 3rd …), matching how the timetable is meant to be read.
  Every displayed/scheduled time is now a value that appears verbatim in the CSV.
* **Ongoing + upcoming always visible.** The Today list opens on the current prayer, so the
  NOW and NEXT cards are on screen at any time of day without scrolling (earlier prayers are
  above – scroll up). After Isha a "Tomorrow · Fajr" card keeps *next* in view. The list glides
  to the new prayer when it changes.

## What's new in 1.3 – smooth navigation

Tab switching was re-engineered so the whole UI stays at the display's native frame rate:

* **Kept-alive pager instead of NavHost** – the three screens live in a `HorizontalPager`
  (`beyondViewportPageCount = 2`), so switching tabs only *translates* already-laid-out content.
  Nothing is created, destroyed, re-measured, faded or re-composited on the way. Swiping between
  tabs works too, and a parked page pauses its ambient effects (`LocalPageVisible`).
* **Nav indicator bound to the scroll offset** – the highlight capsule's `translationX` is read
  from `pagerState` inside `graphicsLayer`, so it tracks the page pixel-for-pixel with zero
  recomposition (previously a `Dp` state drove `offset()`, re-laying-out the bar every frame).
* **No animation state read in composition** – the NEXT-card pulse, shimmer, orbs and stars now
  update layer properties / draw lambdas only; the list cards themselves are skipped entirely
  between the once-a-second clock ticks.
* **No off-screen compositing** – the alpha-mask list fade (`CompositingStrategy.Offscreen`
  over the entire list, nested inside the NavHost fade) was replaced by a cached one-rect
  scroll scrim. Brushes are built once in `drawWithCache` instead of per frame.
* **Release build** – R8 + resource shrinking, signed with the debug keystore
  (`signingConfigs.sideload`) so a genuinely optimised APK can be installed without extra setup.

## Design & motion

* **Animated splash** – system splash icon hands over to a Compose intro: the crescent draws
  itself along its outline, fills, a gold star pops in with overshoot, the title letters cascade
  up, then the whole screen zooms and cross-fades into the app (~2 s, once per cold start).
* **Typography** – [Outfit](https://fonts.google.com/specimen/Outfit) variable font
  (bundled, SIL OFL), six weights instantiated from a single 110 KB TTF.
* **Colour** – deep-emerald + warm-gold system with light and dark schemes, plus
  `ExtendedColors` for gold accents, card gradients and the frosted nav bar. The hero card's sky
  gradient has seven time-of-day palettes (dawn, morning, noon, afternoon, dusk, evening, night)
  and animates between them.
* **Motion** – all built with Compose animation APIs, no extra libraries:
  * staggered fade/slide-up entrance for every screen (`Modifier.staggeredEntrance`)
  * drifting ambient light orbs behind the hero card and the whole app (`AmbientOrbs`)
  * shimmer sweep across the hero card, twinkling starfield at night
  * odometer-style rolling digits in the countdown (`RollingText`)
  * countdown ring with sweep gradient, glowing head and a slow "breathe"
  * per-prayer gradient icon badges that bounce when NEXT moves to them
  * month-to-month slide transitions with direction awareness, rows cascade in
  * floating pill navigation bar with fixed-width tabs and a sliding highlight that follows
    the pager; swipe or tap
  * pinned header + hero card on Today, per-prayer cards scroll underneath (NEXT pulses, NOW is
    gold, past prayers dim); segmented reminder picker in Settings
  * press-scale feedback on tappable cards, animated expand/collapse in Settings

## Features

| Screen | What it does |
|---|---|
| **Today** | Gradient hero card (colour follows the time of day) with the next prayer, live `hh:mm:ss` countdown ring, current prayer, Gregorian + Hijri date, and a full list of today's six timings (Fajr, Sunrise, Zuhr, Asr, Maghrib, Isha). Friday Zuhr is shown as *Jumu'ah*. |
| **Monthly** | Scrollable timetable for any month, today highlighted, Fridays accented, Hijri day shown under each date, jump-to-today button. |
| **Settings** | Notifications master switch + per-prayer toggles, optional pre-prayer reminder (5/10/15/30 min), vibrate, Asr school (Hanafi / Shafi'i), 12/24-hour clock, Light/Dark/System theme, Hijri date ±2-day adjustment. |
| **Notifications** | Exact alarms via `AlarmManager.setExactAndAllowWhileIdle`, re-armed after every alarm and after reboot / app update / time-zone change. Notification-permission and *Alarms & reminders* prompts are handled for Android 13+/12+. |

## How the CSV is used

* The printed sheet lists **odd dates only** (1, 3, 5 … 31) for each month, with times in
  12-hour format without AM/PM.
* `TimetableParser` reads the rows and infers AM/PM per column
  (Fajr/Sunrise → AM, Zuhr onward → PM).
* `Timetable` shows an **unprinted date with the previous printed day's times** – the 2nd uses
  the 1st, the 30th uses the 29th – exactly as the paper sheet is read. Nothing is interpolated
  or rounded, so every time shown or scheduled appears verbatim in the CSV. Phantom rows such
  as "February 31" are never displayed because those dates never exist.
* The table is keyed on month/day, so it is applied to **every year** (solar prayer times shift
  by ≤ 1 min year-to-year for the same date).

To use a different city or an updated sheet, replace `namaz_timings.csv` keeping the same
columns: `Month,Date,Fajr,Sunrise,Zuhr,Asr (Shafi),Asr (Hanafi),Maghrib,Isha`.
Then change `city_name` / `city_region` in `res/values/strings.xml`.

## Project layout

```
app/src/main/java/com/namazapp/hubli/
├── NamazApplication.kt          # service locator + notification channels
├── MainActivity.kt              # edge-to-edge scaffold, kept-alive pager + floating nav, permissions
├── data/
│   ├── TimetableParser.kt       # CSV → RawEntry
│   ├── Timetable.kt             # per-date lookup (unprinted date → previous printed day)
│   ├── PrayerRepository.kt      # next / current prayer queries
│   └── SettingsRepository.kt    # DataStore-backed user preferences
├── domain/
│   ├── Prayer.kt                # Prayer enum, AsrMadhab
│   ├── PrayerDay.kt             # one day's timings
│   ├── HijriDate.kt             # Umm al-Qura conversion (java.time.chrono)
│   └── TimeFormat.kt            # 12h/24h + countdown formatting
├── notifications/
│   ├── PrayerAlarmScheduler.kt  # exact alarms, one per enabled prayer (+ reminder)
│   ├── PrayerAlarmReceiver.kt   # shows notification, re-arms schedule
│   ├── RescheduleReceiver.kt    # BOOT_COMPLETED / TIME_SET / package replaced
│   └── NotificationHelper.kt    # channels + notification builder
└── ui/
    ├── AppViewModel.kt          # 1 Hz ticker, Today / Monthly state, settings actions
    ├── splash/SplashScreen.kt   # animated intro
    ├── theme/                   # colour system, Outfit typography, shapes
    ├── components/
    │   ├── Common.kt            # GlassCard, PrayerIconBadge, Pill, SectionHeader
    │   └── Effects.kt           # draw-phase-only effects: staggeredEntrance, ShimmerOverlay, breathe,
    │                            #   rememberPulse, pressScale, bounceOnChange, AmbientOrbs, Starfield,
    │                            #   BackgroundWash, scrollScrim, RollingText, LocalPageVisible
    ├── today/TodayScreen.kt
    ├── monthly/MonthlyScreen.kt
    └── settings/SettingsScreen.kt
```

## Build & run

**Requirements:** Android Studio Ladybug (2024.2) or newer, JDK 17, Android SDK 35.
(`tools/setup_android.sh` bootstraps a headless JDK + SDK + Gradle toolchain on Linux if you
want to build without Android Studio.)

```bash
# Optimised, side-loadable release APK (recommended for testing smoothness)
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Unit tests (timetable parsing / carry-forward lookup / formatting)
./gradlew :app:testDebugUnitTest

# Install on a connected device
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or simply open the `NamazApp` folder in Android Studio and press **Run**.

* `minSdk 26` (Android 8.0) · `targetSdk 35` (Android 15)
* Release builds have R8 + resource shrinking enabled and are signed with the standard Android
  debug keystore (`~/.android/debug.keystore`, created automatically by the SDK) so they can be
  side-loaded. Replace `signingConfigs.sideload` with your own upload key before publishing.
* Building on a machine with < 4 GB RAM? R8 needs ~1.5 GB; `tools/setup_android.sh` writes a
  low-memory `gradle.properties` and a swap file helps.

## Tech stack

Kotlin 2.0 · Jetpack Compose BOM 2024.12 · Material 3 · Foundation Pager · DataStore ·
Core-SplashScreen · Coroutines/Flow · AlarmManager · `java.time` (no third-party date libs).

## iPhone version

`ios/` contains a full native SwiftUI port (iOS 17+): same timetable & carry-forward rule, Hijri via
`Calendar(.islamicUmmAlQura)`, per-prayer local notifications with pre-reminders, monthly view,
settings, splash, floating pill navigation and the same visual design.

No Mac? Push the repo to GitHub and run the **iOS** workflow (`.github/workflows/ios.yml`):
GitHub's free macOS runners test it and produce an `.ipa` you can sideload with
AltStore/Sideloadly, or – with a paid Apple developer account and a few secrets – sign it and
upload straight to TestFlight. Details in [`ios/README.md`](ios/README.md).
An **Android** workflow builds the release APK the same way.
