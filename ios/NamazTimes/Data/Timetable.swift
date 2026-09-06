import Foundation

/// The in-memory timetable for the city, indexed by month/day so it can be applied to any year.
///
/// The printed source only lists odd dates. A date that is not printed simply **uses the most
/// recent printed date's times** (the 2nd shows the 1st's times, the 30th shows the 29th's) –
/// exactly the way the paper sheet is read. No interpolation or rounding is performed, so every
/// time displayed or scheduled is a value that appears verbatim in the CSV.
///
/// Internally every month is modelled with 31 "slots" (matching the printed sheet, which lists
/// dates 1–31 for every month). Real calendar dates always map onto a valid slot; the phantom
/// rows (e.g. "February 31") are never displayed because such dates never exist.
final class Timetable: @unchecked Sendable {

    private static let slots = 12 * 31

    private let printed: [Int: TimetableParser.RawEntry]
    private var cache: [Int: Slot] = [:]
    private let lock = NSLock()
    let calendar: Calendar

    /// Times for one slot, independent of the year.
    private struct Slot {
        let fajr, sunrise, zuhr, asrShafi, asrHanafi, maghrib, isha: TimeOfDay
    }

    init(printed: [Int: TimetableParser.RawEntry], calendar: Calendar = .current) {
        self.printed = printed
        self.calendar = calendar
    }

    static func fromCSV(_ csv: String, calendar: Calendar = .current) throws -> Timetable {
        let entries = try TimetableParser.parse(csv)
        precondition(!entries.isEmpty, "Timetable CSV contained no rows")
        var map: [Int: TimetableParser.RawEntry] = [:]
        map.reserveCapacity(entries.count * 2)
        for e in entries where map[slot(month: e.month, day: e.day)] == nil {
            map[slot(month: e.month, day: e.day)] = e
        }
        return Timetable(printed: map, calendar: calendar)
    }

    /// Loads `namaz_timings.csv` from the app bundle (or whichever bundle contains this class).
    static func loadBundled(calendar: Calendar = .current) -> Timetable {
        let bundles = [Bundle.main, Bundle(for: Timetable.self)]
        guard let url = bundles.lazy.compactMap({ $0.url(forResource: "namaz_timings", withExtension: "csv") }).first,
              let csv = try? String(contentsOf: url, encoding: .utf8)
        else { fatalError("namaz_timings.csv is missing from the app bundle") }
        do { return try fromCSV(csv, calendar: calendar) } catch { fatalError("Bad timetable: \(error)") }
    }

    /// Prayer times for `date`. Never fails for a valid calendar date.
    func day(for date: Date) -> PrayerDay {
        let start = calendar.startOfDay(for: date)
        let comps = calendar.dateComponents([.month, .day], from: start)
        let slot = Timetable.slot(month: comps.month ?? 1, day: comps.day ?? 1)
        let s = slotTimes(slot)
        return PrayerDay(
            date: start,
            fajr: s.fajr, sunrise: s.sunrise, zuhr: s.zuhr,
            asrShafi: s.asrShafi, asrHanafi: s.asrHanafi,
            maghrib: s.maghrib, isha: s.isha
        )
    }

    /// `true` if this exact date is printed in the sheet (as opposed to carried forward).
    func isPrinted(_ date: Date) -> Bool {
        let comps = calendar.dateComponents([.month, .day], from: date)
        return printed[Timetable.slot(month: comps.month ?? 1, day: comps.day ?? 1)] != nil
    }

    /// Every real calendar day of the month containing `date`, in order.
    func month(containing date: Date) -> [PrayerDay] {
        let comps = calendar.dateComponents([.year, .month], from: date)
        guard let first = calendar.date(from: comps),
              let range = calendar.range(of: .day, in: .month, for: first)
        else { return [] }
        return range.compactMap { d -> PrayerDay? in
            guard let dd = calendar.date(byAdding: .day, value: d - 1, to: first) else { return nil }
            return day(for: dd)
        }
    }

    // MARK: - Internals

    private static func slot(month: Int, day: Int) -> Int { (month - 1) * 31 + (day - 1) }

    private func slotTimes(_ slot: Int) -> Slot {
        lock.lock(); defer { lock.unlock() }
        if let cached = cache[slot] { return cached }
        let built = build(slot)
        cache[slot] = built
        return built
    }

    private func build(_ slot: Int) -> Slot {
        // Walk backwards (wrapping from January 1st to December 31st) to the nearest printed row.
        var s = slot
        var steps = 0
        while printed[s] == nil && steps < Timetable.slots {
            s = (s - 1 + Timetable.slots) % Timetable.slots
            steps += 1
        }
        guard let p = printed[s] else { preconditionFailure("Timetable has no printed rows") }
        return Slot(fajr: p.fajr, sunrise: p.sunrise, zuhr: p.zuhr, asrShafi: p.asrShafi,
                    asrHanafi: p.asrHanafi, maghrib: p.maghrib, isha: p.isha)
    }
}
