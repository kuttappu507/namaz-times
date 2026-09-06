import Foundation

/// Single source of prayer-time data for the whole app (UI and notification scheduling).
/// Loads the bundled CSV once and answers every query from memory.
final class PrayerRepository: @unchecked Sendable {

    static let shared = PrayerRepository(timetable: Timetable.loadBundled())

    let timetable: Timetable
    var calendar: Calendar { timetable.calendar }

    init(timetable: Timetable) {
        self.timetable = timetable
    }

    func day(_ date: Date) -> PrayerDay { timetable.day(for: date) }

    func month(containing date: Date) -> [PrayerDay] { timetable.month(containing: date) }

    /// The first prayer event strictly after `now`, looking across midnight into the following
    /// day (so after Isha, "next" is tomorrow's Fajr). Sunrise is skipped unless `includeSunrise`.
    func nextEvent(after now: Date, madhab: AsrMadhab, includeSunrise: Bool = false) -> PrayerEvent {
        var date = now
        for _ in 0..<3 {
            let hit = day(date).events(madhab: madhab, calendar: calendar)
                .filter { includeSunrise || $0.prayer.isSalah }
                .first { $0.at > now }
            if let hit { return hit }
            date = calendar.date(byAdding: .day, value: 1, to: date)!
        }
        fatalError("No upcoming prayer found") // unreachable: every day has 5 prayers
    }

    /// The most recent prayer event at or before `now`.
    func currentEvent(at now: Date, madhab: AsrMadhab, includeSunrise: Bool = false) -> PrayerEvent {
        var date = now
        for _ in 0..<3 {
            let hit = day(date).events(madhab: madhab, calendar: calendar)
                .filter { includeSunrise || $0.prayer.isSalah }
                .last { $0.at <= now }
            if let hit { return hit }
            date = calendar.date(byAdding: .day, value: -1, to: date)!
        }
        fatalError("No previous prayer found")
    }
}
