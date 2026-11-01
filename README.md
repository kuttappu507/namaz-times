# Namaz Times · Hubli — Web (PWA)

The **Namaz Times · Hubli** prayer-times app as an installable **React (Next.js) PWA** —
the same deep-emerald + warm-gold design, the same bundled Hubli timetable, the same
offline behaviour, now in the browser for **Android and iPhone**.

**Live: https://hubli.vercel.app**

> 📱 The original native apps (Android: Kotlin + Jetpack Compose, iOS: SwiftUI) live on
> the [`native-apps`](https://github.com/kuttappu507/namaz-times/tree/native-apps) branch
> and in [Releases](https://github.com/kuttappu507/namaz-times/releases).

## Install on your phone

* **Android (Chrome):** open the site → tap **Install** in the prompt (or ⋮ → *Add to Home
  screen*). The app runs full-screen, shows prayer notifications and works offline.
* **iPhone (Safari):** open the site → tap the **Share** button → **Add to Home Screen** →
  **Add**. Full-screen, offline, home-screen icon — like a normal app.

The install prompt appears automatically a couple of seconds after each visit until the
app is installed.

## What's inside

| Screen | What it does |
|---|---|
| **Today** | Gradient hero card (the sky colour follows the time of day — dawn, morning, noon, afternoon, dusk, evening, night) with the next prayer, live `hh:mm:ss` countdown ring with rolling digits, current prayer, Gregorian + Hijri dates, and the full list of the day's six timings. Friday Zuhr is shown as **Jumu'ah**. |
| **Monthly** | Scrollable timetable for any month — today highlighted, Fridays accented, Hijri day under each date, jump-to-today button. |
| **Settings** | Notifications master switch + per-prayer toggles, pre-prayer reminder (5/10/15/30 min), vibrate, Asr school (Hanafi / Shafi'i), 12/24-hour clock, Light/Dark/System theme, Hijri date ±2-day adjustment, install shortcut. |

Plus the animated splash (crescent draws itself, star pops, title cascades), ambient
orbs, shimmer, starfield at night, swipeable tabs with the floating pill navigation and
press-scale feedback — all ported from the native UI.

## How the timetable works

* All timings come from the **printed annual timetable for Hubli, Karnataka**
  (originally `app/src/main/assets/namaz_timings.csv` on the `native-apps` branch), now
  bundled in `src/lib/namaz/timetable-data.ts` — the app works **fully offline**.
* The printed sheet lists **odd dates only** (1, 3, 5 … 31). A date that is not printed
  shows the **previous printed day's times** (2nd = 1st, 30th = 29th), exactly as the
  paper sheet is read. Nothing is interpolated or rounded.
* The table is keyed on month/day, so it applies to **every year**.
* Hijri dates use the browser's built-in **Umm al-Qura** calendar
  (`Intl.DateTimeFormat('…ca-islamic-umalqura')`), with a manual ±2-day adjustment for
  local moon sighting.
* Prayer alerts fire through the **Web Notifications API** while the app is open or
  installed (the PWA counterpart of the native apps' AlarmManager/UNNotificationCenter
  scheduling).

## Tech

Next.js 16 (App Router) · React 19 · TypeScript · Tailwind CSS 4 · Framer Motion ·
Outfit (Google Fonts) · Service worker + Web App Manifest · deployed on **Vercel**.

## Run locally

```bash
npm install
npm run dev
```

## Updating the timetable

Edit `src/lib/namaz/timetable-data.ts` keeping the same row shape
`[month, day, fajr, sunrise, zuhr, asrShafi, asrHanafi, maghrib, isha]`
(times in minutes since midnight; the printed sheet is 12-hour — Fajr/Sunrise AM,
the rest PM). Push to `main` and Vercel redeploys automatically.
