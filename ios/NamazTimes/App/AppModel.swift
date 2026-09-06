import Foundation
import Combine
import SwiftUI
import UserNotifications

/// Everything the Today screen needs, recomputed once per second.
struct TodayState: Equatable {
    let now: Date
    let today: PrayerDay
    let hijri: HijriDate
    let current: PrayerEvent
    let next: PrayerEvent
    let timeToNext: TimeInterval
    /// 0...1 – how far we are through the current prayer window.
    let progress: Double
}

struct MonthlyState: Equatable {
    let monthStart: Date
    let days: [PrayerDay]
    let today: Date
}

/// Application-wide view model (mirrors the Android `AppViewModel`).
@MainActor
final class AppModel: ObservableObject {

    let repository: PrayerRepository
    let settingsStore: SettingsStore
    private let scheduler = NotificationScheduler.shared

    @Published private(set) var today: TodayState
    @Published private(set) var monthly: MonthlyState
    @Published private(set) var notificationsAuthorized: Bool = true
    @Published private(set) var selectedMonth: Date

    var settings: UserSettings { settingsStore.settings }
    var calendar: Calendar { repository.calendar }

    private var ticker: AnyCancellable?
    private var settingsSub: AnyCancellable?
    private var storeForward: AnyCancellable?
    private var lastScheduleKey = ""

    init(repository: PrayerRepository = .shared, settingsStore: SettingsStore? = nil) {
        self.repository = repository
        let settingsStore = settingsStore ?? SettingsStore()
        self.settingsStore = settingsStore
        let cal = repository.calendar
        let now = Date()
        let monthStart = cal.date(from: cal.dateComponents([.year, .month], from: now))!
        selectedMonth = monthStart
        today = AppModel.computeToday(now: now, repository: repository, settings: settingsStore.settings)
        monthly = MonthlyState(monthStart: monthStart, days: repository.month(containing: monthStart), today: cal.startOfDay(for: now))

        // 1 Hz clock, aligned to wall-clock seconds.
        ticker = Timer.publish(every: 1, tolerance: 0.05, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in self?.tick() }

        // Recompute + reschedule whenever settings change.
        settingsSub = settingsStore.$settings
            .dropFirst()
            .receive(on: RunLoop.main)
            .sink { [weak self] s in
                guard let self else { return }
                self.tick()
                self.refreshMonthly()
                if s.scheduleKey != self.lastScheduleKey { self.rescheduleNotifications() }
            }
        lastScheduleKey = settingsStore.settings.scheduleKey

        // Any settings change also re-renders views that only observe the model.
        storeForward = settingsStore.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }
    }

    // MARK: - Lifecycle

    /// Called when the scene becomes active: asks for permission on first launch, refreshes the
    /// permission state afterwards, and tops up the rolling notification schedule.
    func onBecameActive() {
        tick()
        refreshMonthly(keepingSelection: true)
        requestNotificationPermission()
    }

    func requestNotificationPermission() {
        Task {
            notificationsAuthorized = await scheduler.requestAuthorization()
            await scheduler.scheduleAll(repository: repository, settings: settings)
        }
    }

    func rescheduleNotifications() {
        lastScheduleKey = settings.scheduleKey
        let settings = self.settings
        Task { await scheduler.scheduleAll(repository: repository, settings: settings) }
    }

    // MARK: - Today

    private func tick() {
        let s = AppModel.computeToday(now: Date(), repository: repository, settings: settings)
        if s != today { today = s }
        // Roll the month view's "today" marker past midnight.
        let startOfDay = calendar.startOfDay(for: s.now)
        if startOfDay != monthly.today { refreshMonthly(keepingSelection: true) }
    }

    private static func computeToday(now: Date, repository: PrayerRepository, settings: UserSettings) -> TodayState {
        let day = repository.day(now)
        // `current` includes Sunrise so the UI can say "Fajr has ended"; the progress ring
        // measures the span between the two surrounding *prayers* so it never jumps at sunrise.
        let current = repository.currentEvent(at: now, madhab: settings.madhab, includeSunrise: true)
        let currentSalah = repository.currentEvent(at: now, madhab: settings.madhab, includeSunrise: false)
        let next = repository.nextEvent(after: now, madhab: settings.madhab)
        let window = max(1, next.at.timeIntervalSince(currentSalah.at))
        let elapsed = min(max(0, now.timeIntervalSince(currentSalah.at)), window)
        return TodayState(
            now: now,
            today: day,
            hijri: HijriDate.from(now, adjustmentDays: settings.hijriAdjustment, calendar: repository.calendar),
            current: current,
            next: next,
            timeToNext: next.at.timeIntervalSince(now),
            progress: elapsed / window
        )
    }

    // MARK: - Monthly

    private func refreshMonthly(keepingSelection: Bool = true) {
        let start = keepingSelection ? selectedMonth : calendar.date(from: calendar.dateComponents([.year, .month], from: Date()))!
        monthly = MonthlyState(monthStart: start, days: repository.month(containing: start), today: calendar.startOfDay(for: Date()))
    }

    func previousMonth() { shiftMonth(by: -1) }
    func nextMonth() { shiftMonth(by: 1) }
    func jumpToCurrentMonth() {
        selectedMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: Date()))!
        refreshMonthly()
    }

    private func shiftMonth(by months: Int) {
        selectedMonth = calendar.date(byAdding: .month, value: months, to: selectedMonth)!
        refreshMonthly()
    }

    var isCurrentMonthSelected: Bool {
        calendar.isDate(selectedMonth, equalTo: Date(), toGranularity: .month)
    }
}
