# Namaz Times – iPhone (SwiftUI)

Native iOS port of the Android app, sharing the same data (`namaz_timings.csv`, Hubli),
the same design language (emerald/gold, Outfit typeface, floating pill nav, sky hero card,
per-prayer cards) and the same features:

| Feature | iOS implementation |
|---|---|
| Today's timings + next-prayer countdown ring | `UI/TodayView.swift` (1 Hz tick in `AppModel`) |
| Per-prayer notifications, on/off per prayer, optional 5–30 min pre-reminder | `Notifications/NotificationScheduler.swift` – `UNCalendarNotificationTrigger`, rolling 7-day window (iOS caps pending at 64), re-armed every time the app becomes active |
| Survives reboot | Local notifications are stored by iOS itself – nothing to re-register |
| Monthly timetable with Hijri dates | `UI/MonthlyView.swift` |
| Hijri date (Umm al-Qura) with ±2-day adjustment | `Domain/HijriDate.swift` via `Calendar(identifier: .islamicUmmAlQura)` |
| Hanafi / Shafi'i Asr, 12/24-h clock, dark (default) / light / system theme | `Data/UserSettings.swift` (`UserDefaults`, JSON) |
| Odd printed dates verbatim; an unprinted date uses the previous printed day (no interpolation) | `Data/Timetable.swift` – identical rule and identical unit-test numbers to the Android version |

## Two ways to get it on an iPhone

| | You need | You get |
|---|---|---|
| **A. GitHub Actions (no Mac)** | a GitHub repo | `.ipa` built on GitHub's free macOS runners – unsigned (sideload with AltStore/Sideloadly) or, with a paid Apple developer account, signed and pushed to **TestFlight** |
| **B. Local Xcode** | a Mac with Xcode 15+ | run directly on your iPhone with ⌘R |

`NamazTimes.xcodeproj` is committed (generated from `project.yml` with XcodeGen 2.43), so
both paths work straight away – nothing to generate.

### A. Build on GitHub – no Mac required

The workflow [`.github/workflows/ios.yml`](../.github/workflows/ios.yml) runs on `macos-15`
with Xcode 16.2:

1. Push this repository to GitHub (the whole `NamazApp` folder, so `.github/` is at the root).
2. **Actions → iOS → Run workflow.** It runs the 12 unit tests on an iPhone 16 simulator and
   builds a release binary for a real device.
3. Open the finished run → **Artifacts → `NamazTimes-iOS-unsigned-ipa`** → download.

**Installing an *unsigned* IPA** (free Apple ID, no developer account):

* **Windows / Mac / Linux:** [Sideloadly](https://sideloadly.io) or
  [AltStore](https://altstore.io) – plug the iPhone in, drag the `.ipa` on, sign in with any
  Apple ID. The tool signs it with a free personal certificate. Free-ID installs expire after
  **7 days**; AltStore/Sideloadly re-sign automatically while the computer is on the same
  Wi-Fi, or you re-install manually.
* After installing: *Settings → General → VPN & Device Management* → trust your Apple ID.

Apple does not allow installing an IPA that nobody has signed, so "unsigned" always means
"signed on the way in by a sideloading tool"; this is the same mechanism Xcode uses for free
accounts.

**Signed IPA + TestFlight** (paid Apple Developer Program, US$99/yr) – installs from the
TestFlight app, no cable, no 7-day limit, up to 10 000 testers:

1. In [developer.apple.com](https://developer.apple.com/account) create an **App ID**
   `com.namazapp.hubli`, an **Apple Distribution** certificate (export as `.p12` with a
   password) and an **App Store** provisioning profile for that App ID.
2. In [App Store Connect](https://appstoreconnect.apple.com) create the app record and an
   **API key** (Users and Access → Integrations → App Store Connect API; role *App Manager*).
   Download the `.p8` once.
3. Add these repository secrets (*Settings → Secrets and variables → Actions*):

   | Secret | Value |
   |---|---|
   | `IOS_CERT_P12_BASE64` | `base64 -i Distribution.p12 \| pbcopy` (Mac) / `certutil -encode` (Win) |
   | `IOS_CERT_PASSWORD` | the `.p12` password |
   | `IOS_PROFILE_BASE64` | `base64 -i NamazTimes_AppStore.mobileprovision` |
   | `APPLE_TEAM_ID` | 10-character Team ID |
   | `ASC_KEY_ID`, `ASC_ISSUER_ID` | from the API key page |
   | `ASC_KEY_P8_BASE64` | `base64 -i AuthKey_XXXXXXXX.p8` |

4. **Actions → iOS → Run workflow → tick "Sign and upload to TestFlight"**, or push a tag
   `ios-v1.3.0`. Ten minutes later the build appears in TestFlight; add yourself as an
   internal tester and install from the TestFlight app. The signed `.ipa` is also attached to
   the run as an artifact.

Free tier: GitHub gives private repos 2 000 Actions-minutes/month; macOS minutes count 10×,
and one iOS run takes ~8–12 macOS minutes (≈ 100 billed). Public repos are unlimited.

### B. Build locally with Xcode

```bash
open ios/NamazTimes.xcodeproj
```

Select the **NamazTimes** scheme → *Signing & Capabilities* → pick your Team → choose your
iPhone (or a simulator) → **⌘R**. `⌘U` runs the 12 unit tests in `NamazTimesTests/` – they
assert the same numbers as the Android tests (Jan 1 = 5:38 / 6:55 / 12:37 / 3:51 / 4:39 /
6:15 / 7:31, Feb 28 Fajr 5:36, 1 Ramadan 1446 = 1 March 2025, and so on).

If you change `project.yml`, regenerate with `brew install xcodegen && (cd ios && xcodegen)`.

### Without the committed project (manual Xcode project)

1. Xcode → *File ▸ New ▸ Project ▸ iOS App*, name **NamazTimes**, interface SwiftUI,
   language Swift, bundle id `com.namazapp.hubli`. Delete the generated `ContentView.swift`
   and `NamazTimesApp.swift`.
2. Drag the folders `NamazTimes/App`, `Domain`, `Data`, `Notifications`, `UI` and
   `Resources` into the project (tick *Copy items if needed*, *Create groups*, target
   NamazTimes).
3. Target ▸ *Build Settings* ▸ **Info.plist File** → `NamazTimes/Resources/Info.plist`
   and set **Generate Info.plist File** → No.
4. Target ▸ *Signing & Capabilities* ▸ **+ Capability** → *Time Sensitive Notifications*
   (optional – lets prayer alerts break through Focus modes).
5. Add a *Unit Testing Bundle* target, drag `NamazTimesTests/TimetableTests.swift` into it and
   also add `Resources/namaz_timings.csv` to the test target's *Copy Bundle Resources*.

## Project layout

```
ios/
├── NamazTimes.xcodeproj            ready to open / used by CI (generated from project.yml)
├── project.yml                     XcodeGen spec (targets, scheme, settings)
├── NamazTimes/
│   ├── App/        NamazTimesApp.swift (entry, splash→tabs, floating nav), AppModel.swift
│   ├── Domain/     Prayer, PrayerDay/TimeOfDay, HijriDate, TimeFormat
│   ├── Data/       TimetableParser, Timetable (carry-forward lookup), PrayerRepository, UserSettings
│   ├── Notifications/ NotificationScheduler (UNUserNotificationCenter)
│   ├── UI/         Theme (palette/fonts), Components (cards, pills, effects), TodayView,
│   │               MonthlyView, SettingsView, SplashView
│   ├── Resources/  namaz_timings.csv, Outfit-{Regular,Medium,SemiBold,Bold}.ttf,
│   │               Assets.xcassets (AppIcon, AccentColor, LaunchBackground), Info.plist
│   └── NamazTimes.entitlements     time-sensitive notifications
└── NamazTimesTests/TimetableTests.swift  (12 tests)
```

## Notes on the port

* **Navigation** – a paging `TabView` keeps all three screens alive; the floating pill nav's
  indicator is a spring-animated capsule. Swiping between pages works too.
* **Ambient effects** (orbs, shimmer, stars) are `TimelineView` + `Canvas`, which SwiftUI
  renders off the main thread's layout pass, so they don't affect scrolling or tab switches.
* **Notifications on iOS** cannot be "exact alarms" the way Android allows, but calendar
  triggers fire at the scheduled minute reliably, including when the app is terminated.
  Sunrise is a silent notification; the five prayers use the default sound. The optional
  pre-reminder is scheduled as a separate, silent notification.
* Everything except the SwiftUI views (`Domain/`, `Data/`, `Notifications/`, `AppModel`) was
  compiled with the open-source Swift 6.0.3 toolchain on Linux; the domain/data layer's
  12 unit tests pass there. The view layer is compiled by the GitHub workflow / Xcode.
