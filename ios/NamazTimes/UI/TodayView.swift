import SwiftUI
import UIKit

/// Home screen. The greeting/date header and the hero card are pinned; only the
/// "Today's timings" list below them scrolls.
struct TodayView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var settingsStore: SettingsStore
    @Environment(\.theme) private var theme

    var body: some View {
        let state = model.today
        let settings = settingsStore.settings
        VStack(spacing: 0) {
            VStack(spacing: 0) {
                Header(state: state)
                    .staggeredEntrance(0)
                    .padding(.top, 10)
                HeroCard(state: state, use24: settings.use24Hour)
                    .staggeredEntrance(1)
                    .padding(.top, 14)
                if settings.notificationsEnabled && !model.notificationsAuthorized {
                    WarningCard(text: "Notifications are off · tap to allow") {
                        model.requestNotificationPermission()
                        openSettingsIfDenied()
                    }
                    .staggeredEntrance(2)
                    .padding(.top, 12)
                }
                SectionHeader("Today's timings") {
                    Pill(text: settings.madhab.label.uppercased(), container: theme.goldContainer, content: theme.onGoldContainer)
                }
                .staggeredEntrance(2)
                .padding(.top, 18)
                .padding(.bottom, 6)
            }
            .padding(.horizontal, 18)

            // The list opens on the *ongoing* prayer so "NOW" and "NEXT" are visible without
            // scrolling; earlier prayers sit above (scroll up to see them).
            let currentIndex = model.calendar.isDate(state.current.at, inSameDayAs: state.today.date)
                ? Prayer.allCases.firstIndex(of: state.current.prayer) : nil
            let nextIndex = model.calendar.isDate(state.next.at, inSameDayAs: state.today.date)
                ? Prayer.allCases.firstIndex(of: state.next.prayer) : nil
            let anchor = currentIndex ?? nextIndex ?? 0
            let nextIsTomorrow = !model.calendar.isDate(state.next.at, inSameDayAs: state.today.date)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(Array(Prayer.allCases.enumerated()), id: \.element) { index, prayer in
                            let time = state.today.time(of: prayer, madhab: settings.madhab)
                            let at = time.on(state.today.date, calendar: model.calendar)
                            PrayerCard(
                                prayer: prayer,
                                name: prayer.name(on: state.today.date, calendar: model.calendar),
                                time: TimeFormat.timeNumber(time, use24Hour: settings.use24Hour),
                                amPm: TimeFormat.amPm(time, use24Hour: settings.use24Hour),
                                isCurrent: index == currentIndex,
                                isNext: index == nextIndex,
                                isPast: state.now >= at,
                                notifyOn: settings.isNotifyOn(prayer)
                            )
                            .id(prayer)
                            .staggeredEntrance(3 + max(0, index - anchor), stagger: 0.055)
                        }
                        if nextIsTomorrow {
                            // After Isha the upcoming prayer is tomorrow's Fajr – keep "next" in the list.
                            let comps = model.calendar.dateComponents([.hour, .minute], from: state.next.at)
                            let tod = TimeOfDay(hour: comps.hour ?? 0, minute: comps.minute ?? 0)
                            PrayerCard(
                                prayer: state.next.prayer,
                                name: state.next.prayer.name(on: state.next.at, calendar: model.calendar),
                                note: "Tomorrow · \(state.next.prayer.arabicName)",
                                time: TimeFormat.timeNumber(tod, use24Hour: settings.use24Hour),
                                amPm: TimeFormat.amPm(tod, use24Hour: settings.use24Hour),
                                isCurrent: false,
                                isNext: true,
                                isPast: false,
                                notifyOn: settings.isNotifyOn(state.next.prayer)
                            )
                            .id("tomorrow")
                            .staggeredEntrance(4, stagger: 0.055)
                        }
                        Text("Asr per \(settings.madhab.label) school · change in Settings")
                            .font(AppFont.bodySmall)
                            .foregroundStyle(theme.onSurfaceVariant)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 4)
                            .staggeredEntrance(10, stagger: 0.055)
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 8)
                    .padding(.bottom, 110)
                }
                .scrollIndicators(.hidden)
                .onAppear { proxy.scrollTo(Prayer.allCases[anchor], anchor: .top) }
                .onChange(of: anchor) { _, new in
                    withAnimation(.easeInOut(duration: 0.45)) { proxy.scrollTo(Prayer.allCases[new], anchor: .top) }
                }
            }
        }
    }

    private func openSettingsIfDenied() {
        Task { @MainActor in
            if await NotificationScheduler.shared.authorizationStatus() == .denied,
               let url = URL(string: UIApplication.openSettingsURLString) {
                _ = await UIApplication.shared.open(url)
            }
        }
    }
}

// MARK: - Header

private struct Header: View {
    @Environment(\.theme) private var theme
    let state: TodayState

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_IN")
        f.dateFormat = "EEEE, d MMMM"
        return f
    }()

    var body: some View {
        let hour = Calendar.current.component(.hour, from: state.now)
        let greeting: String = {
            switch hour {
            case 4...11: return "Good morning"
            case 12...16: return "Good afternoon"
            case 17...20: return "Good evening"
            default: return "Good night"
            }
        }()
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Assalamu Alaikum · \(greeting)")
                    .font(AppFont.labelLarge)
                    .foregroundStyle(theme.primary)
                Text(Self.dateFormatter.string(from: state.now))
                    .font(AppFont.headlineMedium)
                    .foregroundStyle(theme.onSurface)
                Text("\(state.hijri.formatted)  ·  \(state.hijri.monthNameArabic)")
                    .font(AppFont.body)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer(minLength: 8)
            HStack(spacing: 4) {
                Image(systemName: "location.fill").font(.system(size: 12, weight: .semibold))
                Text("Hubli").font(AppFont.labelLarge)
            }
            .foregroundStyle(theme.onPrimaryContainer)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(theme.primaryContainer, in: Capsule())
        }
        .padding(.horizontal, 2)
    }
}

// MARK: - Hero

/// Sky-gradient card with drifting orbs, shimmer, and the odometer countdown ring.
private struct HeroCard: View {
    let state: TodayState
    let use24: Bool

    var body: some View {
        let hour = Calendar.current.component(.hour, from: state.now)
        let palette = SkyPalette.forHour(hour)
        let isNight = hour >= 19 || hour < 5
        let nextName = state.next.prayer.name(on: state.next.at)
        let nextIsTomorrow = !Calendar.current.isDate(state.next.at, inSameDayAs: state.now)
        let currentLabel: String = {
            if state.current.prayer == .sunrise { return "Fajr has ended" }
            return "Now · \(state.current.prayer.name(on: state.current.at))"
        }()
        let comps = Calendar.current.dateComponents([.hour, .minute], from: state.next.at)
        let nextTod = TimeOfDay(hour: comps.hour ?? 0, minute: comps.minute ?? 0)
        let shape = RoundedRectangle(cornerRadius: 30, style: .continuous)

        ZStack {
            LinearGradient(colors: [palette.top, palette.mid, palette.bottom], startPoint: .topLeading, endPoint: .bottomTrailing)
                .animation(.easeInOut(duration: 1.5), value: palette)
            AmbientOrbs(colors: [palette.glow, palette.accent, .white], alpha: 0.4)
            if isNight { Starfield(count: 28, seed: 3) }
            GeometryReader { geo in
                Circle().fill(.white.opacity(0.07))
                    .frame(width: geo.size.height * 1.8)
                    .position(x: geo.size.width * 1.02, y: -geo.size.height * 0.15)
                Circle().fill(.white.opacity(0.05))
                    .frame(width: geo.size.height * 1.2)
                    .position(x: -geo.size.width * 0.05, y: geo.size.height * 1.1)
            }
            ShimmerOverlay()

            HStack(alignment: .center, spacing: 10) {
                VStack(alignment: .leading, spacing: 0) {
                    Pill(text: nextIsTomorrow ? "NEXT · TOMORROW" : "UP NEXT", container: .white.opacity(0.18), content: .white)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(nextName)
                            .font(AppFont.display)
                            .foregroundStyle(.white)
                            .contentTransition(.opacity)
                        Text(state.next.prayer.arabicName)
                            .font(AppFont.titleMedium)
                            .foregroundStyle(palette.accent)
                    }
                    .padding(.top, 10)
                    .animation(.easeInOut(duration: 0.4), value: nextName)
                    HStack(alignment: .lastTextBaseline, spacing: 5) {
                        Text(TimeFormat.timeNumber(nextTod, use24Hour: use24))
                            .font(AppFont.headlineLarge)
                            .foregroundStyle(.white)
                        if !use24 {
                            Text(TimeFormat.amPm(nextTod, use24Hour: use24))
                                .font(AppFont.labelLarge)
                                .foregroundStyle(.white.opacity(0.85))
                        }
                    }
                    .padding(.top, 10)
                    HStack(spacing: 8) {
                        BreathingDot(color: palette.accent)
                        Text(currentLabel)
                            .font(AppFont.body)
                            .foregroundStyle(.white.opacity(0.85))
                    }
                    .padding(.top, 4)
                }
                Spacer(minLength: 0)
                CountdownRing(progress: state.progress, label: TimeFormat.countdown(state.timeToNext), accent: palette.accent)
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 20)
        }
        .clipShape(shape)
        .shadow(color: palette.mid.opacity(0.5), radius: 24, y: 10)
    }
}

private struct BreathingDot: View {
    let color: Color
    @State private var big = false
    var body: some View {
        Circle()
            .fill(color)
            .frame(width: 7, height: 7)
            .scaleEffect(big ? 1.25 : 0.8)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true)) { big = true }
            }
    }
}

private struct CountdownRing: View {
    let progress: Double
    let label: String
    let accent: Color

    var body: some View {
        ZStack {
            Circle().fill(RadialGradient(colors: [.white.opacity(0.14), .clear], center: .center, startRadius: 0, endRadius: 66))
            Circle()
                .stroke(.white.opacity(0.2), style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .padding(7)
            Circle()
                .trim(from: 0, to: max(0.001, min(1, progress)))
                .stroke(
                    AngularGradient(colors: [accent, .white, accent], center: .center, startAngle: .degrees(0), endAngle: .degrees(360)),
                    style: StrokeStyle(lineWidth: 10, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .padding(7)
                .animation(.easeOut(duration: 0.9), value: progress)
            VStack(spacing: 0) {
                RollingText(text: label, font: AppFont.outfit(20, weight: .bold), color: .white)
                Text("remaining")
                    .font(AppFont.labelSmall)
                    .foregroundStyle(.white.opacity(0.75))
            }
        }
        .frame(width: 132, height: 132)
    }
}

// MARK: - Prayer card

/// One prayer as its own card. The NEXT card gets a gradient tint, a pulsing coloured border;
/// past prayers dim.
private struct PrayerCard: View {
    @Environment(\.theme) private var theme
    let prayer: Prayer
    let name: String
    var note: String? = nil
    let time: String
    let amPm: String
    let isCurrent: Bool
    let isNext: Bool
    let isPast: Bool
    let notifyOn: Bool

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 22, style: .continuous)
        let accent = theme.primary
        let tint: Color = {
            if isNext { return accent.opacity(theme.isDark ? 0.22 : 0.14) }
            if isCurrent && prayer.isSalah { return theme.gold.opacity(theme.isDark ? 0.16 : 0.12) }
            return .clear
        }()
        let border: Color = {
            if isNext { return accent.opacity(0.55) }
            if isCurrent && prayer.isSalah { return theme.gold.opacity(0.5) }
            return theme.cardBorder
        }()
        let textColor = prayer.isSalah ? theme.onSurface : theme.onSurfaceVariant

        HStack(spacing: 14) {
            PrayerIconBadge(prayer: prayer, active: isNext || isCurrent)
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 8) {
                    Text(name).font(AppFont.titleMedium).foregroundStyle(textColor)
                    if isCurrent && prayer.isSalah {
                        Pill(text: "NOW", container: theme.goldContainer, content: theme.onGoldContainer)
                    } else if isNext {
                        Pill(text: "NEXT", container: accent, content: theme.onPrimary)
                    }
                }
                Text(note ?? prayer.arabicName).font(AppFont.bodySmall).foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer(minLength: 4)
            Image(systemName: notifyOn ? "bell.fill" : "bell.slash")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(notifyOn ? accent : theme.outline)
            HStack(alignment: .lastTextBaseline, spacing: 3) {
                Text(time).font(AppFont.outfit(22, weight: .bold)).foregroundStyle(textColor)
                if !amPm.isEmpty {
                    Text(amPm).font(AppFont.labelSmall).foregroundStyle(theme.onSurfaceVariant)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            ZStack {
                LinearGradient(colors: [theme.cardStart, theme.cardEnd], startPoint: .top, endPoint: .bottom)
                LinearGradient(colors: [tint, tint.opacity(0.25)], startPoint: .leading, endPoint: .trailing)
            }
        )
        .clipShape(shape)
        .overlay(shape.strokeBorder(border, lineWidth: 1))
        .overlay { if isNext { PulsingBorder(shape: shape, color: accent) } }
        .shadow(
            color: isNext ? accent.opacity(0.35) : .black.opacity(theme.isDark ? 0.4 : 0.1),
            radius: isNext ? 14 : (theme.isDark ? 6 : 10), y: 5
        )
        .opacity(isPast && !isCurrent ? 0.62 : 1)
        .animation(.easeInOut(duration: 0.4), value: isNext)
        .animation(.easeInOut(duration: 0.4), value: isPast)
    }
}

private struct PulsingBorder: View {
    let shape: RoundedRectangle
    let color: Color
    @State private var on = false
    var body: some View {
        shape.strokeBorder(color.opacity(0.85), lineWidth: 1.5)
            .opacity(on ? 1 : 0.15)
            .onAppear { withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { on = true } }
    }
}

private struct WarningCard: View {
    @Environment(\.theme) private var theme
    let text: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "exclamationmark.triangle.fill").font(.system(size: 16, weight: .semibold))
                Text(text).font(AppFont.body)
                Spacer(minLength: 0)
            }
            .foregroundStyle(theme.onErrorContainer)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(theme.errorContainer, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .pressScale()
    }
}
