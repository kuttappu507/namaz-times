import Foundation
import Combine

/// Dark / light / follow-system.
enum ThemeMode: String, CaseIterable, Codable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
    var label: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
}

/// Everything the user can configure, as one value type that is persisted as JSON in
/// `UserDefaults` (mirrors the Android DataStore snapshot).
struct UserSettings: Codable, Equatable {
    var madhab: AsrMadhab = .hanafi
    var use24Hour: Bool = false
    var themeMode: ThemeMode = .dark
    /// Master switch for prayer-time notifications.
    var notificationsEnabled: Bool = true
    /// Per-prayer switches (Sunrise is off by default – it is not a prayer).
    var notifyFor: [Prayer: Bool] = Dictionary(uniqueKeysWithValues: Prayer.allCases.map { ($0, $0.isSalah) })
    /// Minutes before each prayer to send a heads-up; 0 disables the pre-reminder.
    var reminderMinutes: Int = 0
    var vibrate: Bool = true
    /// Manual correction (in days, −2...+2) applied to the calculated Hijri date.
    var hijriAdjustment: Int = 0

    func isNotifyOn(_ prayer: Prayer) -> Bool { notificationsEnabled && (notifyFor[prayer] ?? false) }

    /// The subset of settings that affects the notification schedule.
    var scheduleKey: String {
        let toggles = Prayer.allCases.map { "\($0.rawValue)=\(notifyFor[$0] ?? false)" }.joined(separator: ",")
        return "\(notificationsEnabled)|\(toggles)|\(reminderMinutes)|\(madhab.rawValue)|\(vibrate)"
    }
}

/// Observable, persisted settings store.
@MainActor
final class SettingsStore: ObservableObject {

    @Published var settings: UserSettings {
        didSet { persist() }
    }

    private let defaults: UserDefaults
    private let key = "user_settings_v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: key),
           let decoded = try? JSONDecoder().decode(UserSettings.self, from: data) {
            settings = decoded
        } else {
            settings = UserSettings()
        }
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(settings) {
            defaults.set(data, forKey: key)
        }
    }

    // MARK: - Mutators (mirror the Android repository API)

    func setMadhab(_ m: AsrMadhab) { settings.madhab = m }
    func setUse24Hour(_ v: Bool) { settings.use24Hour = v }
    func setThemeMode(_ m: ThemeMode) { settings.themeMode = m }
    func setNotificationsEnabled(_ v: Bool) { settings.notificationsEnabled = v }
    func setNotify(_ p: Prayer, _ v: Bool) { settings.notifyFor[p] = v }
    func setReminderMinutes(_ m: Int) { settings.reminderMinutes = min(max(m, 0), 60) }
    func setVibrate(_ v: Bool) { settings.vibrate = v }
    func setHijriAdjustment(_ d: Int) { settings.hijriAdjustment = min(max(d, -2), 2) }
}
