import Foundation

/// A wall-clock time of day, independent of any date or time zone.
struct TimeOfDay: Hashable, Comparable, Codable {
    let hour: Int
    let minute: Int

    init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }

    /// Minutes since midnight (0 ..< 1440).
    init(minutesOfDay: Int) {
        let m = ((minutesOfDay % 1440) + 1440) % 1440
        hour = m / 60
        minute = m % 60
    }

    var minutesOfDay: Int { hour * 60 + minute }

    static func < (lhs: TimeOfDay, rhs: TimeOfDay) -> Bool { lhs.minutesOfDay < rhs.minutesOfDay }

    /// The concrete instant of this time on `date` (start-of-day based, DST safe).
    func on(_ date: Date, calendar: Calendar = .current) -> Date {
        let start = calendar.startOfDay(for: date)
        return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: start) ?? start
    }
}

/// One row of the timetable: the prayer times for a single calendar date.
struct PrayerDay: Hashable, Identifiable {
    let date: Date          // start of day in the current calendar
    let fajr: TimeOfDay
    let sunrise: TimeOfDay
    let zuhr: TimeOfDay
    let asrShafi: TimeOfDay
    let asrHanafi: TimeOfDay
    let maghrib: TimeOfDay
    let isha: TimeOfDay

    var id: Date { date }

    func time(of prayer: Prayer, madhab: AsrMadhab) -> TimeOfDay {
        switch prayer {
        case .fajr: return fajr
        case .sunrise: return sunrise
        case .zuhr: return zuhr
        case .asr: return madhab == .hanafi ? asrHanafi : asrShafi
        case .maghrib: return maghrib
        case .isha: return isha
        }
    }

    func dateTime(of prayer: Prayer, madhab: AsrMadhab, calendar: Calendar = .current) -> Date {
        time(of: prayer, madhab: madhab).on(date, calendar: calendar)
    }

    /// All six events for this day, in chronological order.
    func events(madhab: AsrMadhab, calendar: Calendar = .current) -> [PrayerEvent] {
        Prayer.allCases.map { PrayerEvent(prayer: $0, at: dateTime(of: $0, madhab: madhab, calendar: calendar)) }
    }
}

/// A concrete prayer at a concrete moment in time.
struct PrayerEvent: Hashable {
    let prayer: Prayer
    let at: Date
}
