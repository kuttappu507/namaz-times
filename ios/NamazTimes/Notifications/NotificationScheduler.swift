import Foundation
import UserNotifications

/// Schedules local notifications for upcoming prayers.
///
/// iOS has no exact-alarm API for third-party apps; instead a local notification with a
/// calendar trigger fires at the exact minute, even when the app is closed. iOS caps pending
/// requests at 64 per app, so we schedule a rolling window (about the next 7 days for five
/// prayers, plus optional pre-reminders) and top it up whenever the app becomes active.
final class NotificationScheduler: @unchecked Sendable {

    static let shared = NotificationScheduler()

    static let prayerCategory = "PRAYER_TIME"
    static let reminderCategory = "PRAYER_REMINDER"
    private static let maxPending = 60           // stay under the 64 system limit

    private let center = UNUserNotificationCenter.current()

    // MARK: - Permission

    /// Requests permission the first time; later calls just report the current status.
    func requestAuthorization() async -> Bool {
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .denied:
            return false
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        @unknown default:
            return false
        }
    }

    func authorizationStatus() async -> UNAuthorizationStatus {
        await center.notificationSettings().authorizationStatus
    }

    func registerCategories() {
        let prayer = UNNotificationCategory(identifier: Self.prayerCategory, actions: [], intentIdentifiers: [])
        let reminder = UNNotificationCategory(identifier: Self.reminderCategory, actions: [], intentIdentifiers: [])
        center.setNotificationCategories([prayer, reminder])
    }

    // MARK: - Scheduling

    /// Rebuilds the whole pending schedule from `settings`. Safe to call often.
    func scheduleAll(repository: PrayerRepository, settings: UserSettings, now: Date = Date()) async {
        center.removeAllPendingNotificationRequests()
        guard settings.notificationsEnabled else { return }
        let enabled = Prayer.allCases.filter { settings.isNotifyOn($0) }
        guard !enabled.isEmpty else { return }

        let calendar = repository.calendar
        var requests: [UNNotificationRequest] = []
        var dayOffset = 0
        outer: while dayOffset < 14 {
            let date = calendar.date(byAdding: .day, value: dayOffset, to: now)!
            let day = repository.day(date)
            for prayer in Prayer.allCases where enabled.contains(prayer) {
                let at = day.dateTime(of: prayer, madhab: settings.madhab, calendar: calendar)
                guard at > now else { continue }

                if settings.reminderMinutes > 0, prayer.isSalah {
                    let remindAt = at.addingTimeInterval(TimeInterval(-settings.reminderMinutes * 60))
                    if remindAt > now {
                        requests.append(makeRequest(
                            id: "reminder-\(prayer.rawValue)-\(Int(at.timeIntervalSince1970))",
                            title: "\(prayer.name(on: at, calendar: calendar)) in \(settings.reminderMinutes) min",
                            body: "\(prayer.displayName) is at \(TimeFormat.time(day.time(of: prayer, madhab: settings.madhab), use24Hour: settings.use24Hour)) · Hubli",
                            at: remindAt, category: Self.reminderCategory, calendar: calendar, sound: false
                        ))
                    }
                }
                let title = prayer.isSalah
                    ? "Time for \(prayer.name(on: at, calendar: calendar))"
                    : "Sunrise · Fajr has ended"
                let body = prayer.isSalah
                    ? "\(prayer.arabicName) · \(TimeFormat.time(day.time(of: prayer, madhab: settings.madhab), use24Hour: settings.use24Hour)) · Hubli"
                    : "\(TimeFormat.time(day.sunrise, use24Hour: settings.use24Hour)) · Hubli"
                requests.append(makeRequest(
                    id: "prayer-\(prayer.rawValue)-\(Int(at.timeIntervalSince1970))",
                    title: title, body: body, at: at,
                    category: Self.prayerCategory, calendar: calendar, sound: prayer.isSalah
                ))
                if requests.count >= Self.maxPending { break outer }
            }
            dayOffset += 1
        }

        for request in requests {
            try? await center.add(request)
        }
    }

    private func makeRequest(
        id: String, title: String, body: String, at: Date,
        category: String, calendar: Calendar, sound: Bool
    ) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.categoryIdentifier = category
        content.sound = sound ? .default : nil
        content.threadIdentifier = "namaz-times"
        content.interruptionLevel = .timeSensitive
        var comps = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: at)
        comps.second = 0
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        return UNNotificationRequest(identifier: id, content: content, trigger: trigger)
    }
}
