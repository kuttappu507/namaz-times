import Foundation

/// Parses the printed timetable CSV that ships in the app bundle as `namaz_timings.csv`.
///
/// The file looks like:
/// ```
/// Complete Timings -hubli - All 12 Months (odd dates as printed),,,,,,,,
/// ,,,,,,,,
/// Month,Date,Fajr,Sunrise,Zuhr,Asr (Shafi),Asr (Hanafi),Maghrib,Isha
/// January,1,5:38,6:55,12:37,3:51,4:39,6:15,7:31
/// ```
///
/// Quirks handled here:
///  * Only odd dates are printed (1, 3, 5 … 31). Unprinted dates carry forward the previous
///    printed day's times – see `Timetable`.
///  * Every month is printed with 16 rows (dates 1..31), including days that do not exist
///    (e.g. "February 29/31"). Those rows are kept as raw entries; `Timetable` only
///    materialises real calendar dates.
///  * Times are printed in 12-hour form without AM/PM. Fajr/Sunrise are AM; Zuhr is around
///    noon (12:xx is already PM); Asr/Maghrib/Isha are PM.
enum TimetableParser {

    /// A raw printed row; month/day are as printed, not validated against the calendar.
    struct RawEntry: Hashable {
        let month: Int   // 1...12
        let day: Int     // 1...31
        let fajr: TimeOfDay
        let sunrise: TimeOfDay
        let zuhr: TimeOfDay
        let asrShafi: TimeOfDay
        let asrHanafi: TimeOfDay
        let maghrib: TimeOfDay
        let isha: TimeOfDay
    }

    enum ParseError: Error, CustomStringConvertible {
        case badTime(String)
        var description: String {
            switch self { case .badTime(let t): return "Bad time '\(t)'" }
        }
    }

    private static let monthsByName: [String: Int] = [
        "january": 1, "february": 2, "march": 3, "april": 4, "may": 5, "june": 6,
        "july": 7, "august": 8, "september": 9, "october": 10, "november": 11, "december": 12,
    ]

    static func parse(_ csv: String) throws -> [RawEntry] {
        var entries: [RawEntry] = []
        entries.reserveCapacity(200)
        for rawLine in csv.split(whereSeparator: { $0 == "\n" || $0 == "\r\n" || $0 == "\r" }) {
            let line = rawLine.trimmingCharacters(in: .whitespacesAndNewlines)
            if line.isEmpty { continue }
            let cols = line.split(separator: ",", omittingEmptySubsequences: false)
                .map { $0.trimmingCharacters(in: .whitespaces) }
            guard cols.count >= 9 else { continue }
            guard let month = monthsByName[cols[0].lowercased()] else { continue } // skips header rows
            guard let day = Int(cols[1]), (1...31).contains(day) else { continue }
            entries.append(RawEntry(
                month: month,
                day: day,
                fajr: try parseTime(cols[2], pm: false),
                sunrise: try parseTime(cols[3], pm: false),
                zuhr: try parseTime(cols[4], pm: true),
                asrShafi: try parseTime(cols[5], pm: true),
                asrHanafi: try parseTime(cols[6], pm: true),
                maghrib: try parseTime(cols[7], pm: true),
                isha: try parseTime(cols[8], pm: true)
            ))
        }
        return entries
    }

    /// Converts a printed "h:mm" (12-hour, no AM/PM marker) into a `TimeOfDay`.
    /// Hour 12 is treated as noon for PM columns and midnight for AM columns.
    static func parseTime(_ text: String, pm: Bool) throws -> TimeOfDay {
        let parts = text.split(separator: ":")
        guard parts.count == 2,
              var hour = Int(parts[0].trimmingCharacters(in: .whitespaces)),
              let minute = Int(parts[1].trimmingCharacters(in: .whitespaces)),
              (0...12).contains(hour), (0...59).contains(minute)
        else { throw ParseError.badTime(text) }
        if pm {
            if hour != 12 { hour += 12 }
        } else if hour == 12 {
            hour = 0
        }
        return TimeOfDay(hour: hour, minute: minute)
    }
}
