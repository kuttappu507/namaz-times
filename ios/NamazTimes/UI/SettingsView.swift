import SwiftUI
import UIKit

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var store: SettingsStore
    @Environment(\.theme) private var theme

    var body: some View {
        let s = store.settings
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Settings").font(AppFont.headlineMedium).foregroundStyle(theme.onSurface)
                    Text("Hubli · timetable as printed").font(AppFont.body).foregroundStyle(theme.onSurfaceVariant)
                }
                .padding(.leading, 4)
                .staggeredEntrance(0)

                // ---- Notifications ----
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader("Notifications")
                    GlassCard {
                        VStack(spacing: 0) {
                            ToggleRow(symbol: "bell.badge.fill", title: "Prayer-time alerts",
                                      subtitle: "A notification at each prayer time",
                                      isOn: Binding(get: { s.notificationsEnabled }, set: { store.setNotificationsEnabled($0) }))
                            if s.notificationsEnabled && !model.notificationsAuthorized {
                                PermissionRow(text: "Notifications are turned off for this app") {
                                    model.requestNotificationPermission()
                                    openSystemSettings()
                                }
                            }
                            Divider().overlay(theme.cardBorder).padding(.horizontal, 16)
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Notify me for").font(AppFont.labelLarge).foregroundStyle(theme.onSurfaceVariant)
                                    .padding(.horizontal, 16).padding(.top, 12)
                                ForEach(Prayer.allCases) { prayer in
                                    PrayerToggleRow(prayer: prayer, enabled: s.notificationsEnabled,
                                                    isOn: Binding(get: { s.notifyFor[prayer] ?? false }, set: { store.setNotify(prayer, $0) }))
                                }
                            }
                            .padding(.bottom, 8)
                            Divider().overlay(theme.cardBorder).padding(.horizontal, 16)
                            ReminderRow(minutes: s.reminderMinutes, enabled: s.notificationsEnabled) { store.setReminderMinutes($0) }
                            ToggleRow(symbol: "iphone.radiowaves.left.and.right", title: "Haptic on tap", subtitle: "Light haptic feedback in the app",
                                      isOn: Binding(get: { s.vibrate }, set: { store.setVibrate($0) }))
                        }
                    }
                }
                .staggeredEntrance(1)

                // ---- Prayer calculation ----
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader("Asr calculation")
                    GlassCard(padding: 14) {
                        VStack(spacing: 8) {
                            ForEach(AsrMadhab.allCases) { m in
                                ChoiceRow(title: m.label, subtitle: m.description, selected: s.madhab == m) { store.setMadhab(m) }
                            }
                        }
                    }
                }
                .staggeredEntrance(2)

                // ---- Display ----
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader("Display")
                    GlassCard {
                        VStack(spacing: 0) {
                            ToggleRow(symbol: "clock", title: "24-hour clock", subtitle: "Show 17:05 instead of 5:05 PM",
                                      isOn: Binding(get: { s.use24Hour }, set: { store.setUse24Hour($0) }))
                            Divider().overlay(theme.cardBorder).padding(.horizontal, 16)
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Theme").font(AppFont.labelLarge).foregroundStyle(theme.onSurfaceVariant)
                                Segmented(options: ThemeMode.allCases.map { ($0.label, $0) }, selection: s.themeMode) { store.setThemeMode($0) }
                            }
                            .padding(16)
                        }
                    }
                }
                .staggeredEntrance(3)

                // ---- Hijri ----
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader("Hijri date")
                    GlassCard(padding: 16) {
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Adjust by days to match local moon sighting").font(AppFont.bodySmall).foregroundStyle(theme.onSurfaceVariant)
                            Segmented(options: [(-2, -2), (-1, -1), (0, 0), (1, 1), (2, 2)].map { ($0.0 > 0 ? "+\($0.0)" : "\($0.0)", $0.1) },
                                      selection: s.hijriAdjustment) { store.setHijriAdjustment($0) }
                            Text("Today: \(model.today.hijri.formatted)").font(AppFont.labelLarge).foregroundStyle(theme.primary)
                        }
                    }
                }
                .staggeredEntrance(4)

                Text("Timings: Hubli timetable – odd dates as printed; an unprinted date uses the previous printed day.\nNamaz Times · v1.3.1 (iOS)")
                    .font(AppFont.bodySmall)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .padding(.horizontal, 6)
                    .staggeredEntrance(5)
            }
            .padding(.horizontal, 18)
            .padding(.top, 10)
            .padding(.bottom, 110)
        }
        .scrollIndicators(.hidden)
    }

    private func openSystemSettings() {
        Task { @MainActor in
            if await NotificationScheduler.shared.authorizationStatus() == .denied,
               let url = URL(string: UIApplication.openSettingsURLString) {
                _ = await UIApplication.shared.open(url)
            }
        }
    }
}

// MARK: - Rows

private struct ToggleRow: View {
    @Environment(\.theme) private var theme
    let symbol: String
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(theme.onPrimaryContainer)
                .frame(width: 36, height: 36)
                .background(theme.primaryContainer, in: Circle())
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(AppFont.titleMedium).foregroundStyle(theme.onSurface)
                Text(subtitle).font(AppFont.bodySmall).foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer()
            Toggle("", isOn: $isOn).labelsHidden().tint(theme.primary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct PrayerToggleRow: View {
    @Environment(\.theme) private var theme
    let prayer: Prayer
    let enabled: Bool
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 12) {
            PrayerIconBadge(prayer: prayer, size: 32, active: isOn && enabled)
            VStack(alignment: .leading, spacing: 0) {
                Text(prayer.displayName).font(AppFont.labelLarge).foregroundStyle(theme.onSurface)
                Text(prayer.isSalah ? prayer.arabicName : "Not a prayer · end of Fajr")
                    .font(AppFont.bodySmall).foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer()
            Toggle("", isOn: $isOn).labelsHidden().tint(theme.primary).disabled(!enabled)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .opacity(enabled ? 1 : 0.5)
    }
}

private struct PermissionRow: View {
    @Environment(\.theme) private var theme
    let text: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: "exclamationmark.triangle.fill")
                Text(text).font(AppFont.bodySmall)
                Spacer()
                Text("Fix").font(AppFont.labelMedium)
            }
            .foregroundStyle(theme.onErrorContainer)
            .padding(.horizontal, 14).padding(.vertical, 10)
            .background(theme.errorContainer, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .pressScale()
        .padding(.horizontal, 16)
        .padding(.bottom, 10)
    }
}

/// Segmented "Off / 5m / 10m / 15m / 30m" reminder picker.
private struct ReminderRow: View {
    @Environment(\.theme) private var theme
    let minutes: Int
    let enabled: Bool
    let onChange: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Reminder before prayer").font(AppFont.titleMedium).foregroundStyle(theme.onSurface)
                Spacer()
                Text(minutes == 0 ? "Off" : "\(minutes) min").font(AppFont.labelLarge).foregroundStyle(theme.primary)
            }
            Segmented(options: [("Off", 0), ("5m", 5), ("10m", 10), ("15m", 15), ("30m", 30)], selection: minutes, onSelect: onChange)
        }
        .padding(16)
        .opacity(enabled ? 1 : 0.5)
        .disabled(!enabled)
    }
}

private struct ChoiceRow: View {
    @Environment(\.theme) private var theme
    let title: String
    let subtitle: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 20))
                    .foregroundStyle(selected ? theme.primary : theme.outline)
                    .contentTransition(.symbolEffect(.replace))
                VStack(alignment: .leading, spacing: 1) {
                    Text(title).font(AppFont.titleMedium).foregroundStyle(theme.onSurface)
                    Text(subtitle).font(AppFont.bodySmall).foregroundStyle(theme.onSurfaceVariant)
                }
                Spacer()
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(selected ? theme.primary.opacity(theme.isDark ? 0.16 : 0.10) : .clear)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(selected ? theme.primary.opacity(0.5) : theme.cardBorder, lineWidth: 1)
            )
        }
        .pressScale(0.98)
        .animation(.easeInOut(duration: 0.2), value: selected)
    }
}

/// Generic sliding-indicator segmented control (same look as the Android ReminderRow).
struct Segmented<Value: Hashable>: View {
    @Environment(\.theme) private var theme
    let options: [(String, Value)]
    let selection: Value
    let onSelect: (Value) -> Void
    @Namespace private var ns

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options.indices, id: \.self) { i in
                let label = options[i].0
                let value = options[i].1
                let isSel = value == selection
                Button { onSelect(value) } label: {
                    Text(label)
                        .font(AppFont.labelMedium)
                        .foregroundStyle(isSel ? theme.onPrimary : theme.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background {
                            if isSel {
                                Capsule().fill(theme.primary).matchedGeometryEffect(id: "seg", in: ns)
                            }
                        }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(3)
        .background(theme.surfaceHigh.opacity(theme.isDark ? 0.6 : 1), in: Capsule())
        .animation(.spring(response: 0.35, dampingFraction: 0.82), value: selection)
    }
}
