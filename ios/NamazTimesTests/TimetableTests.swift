import XCTest
@testable import NamazTimes

/// Same expectations as the Android `TimetableTest` / `HijriAndFormatTest`, so both ports are
/// verified against identical numbers.
final class TimetableTests: XCTestCase {

    static var ist: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "Asia/Kolkata")!
        c.locale = Locale(identifier: "en_IN")
        return c
    }()

    static let table: Timetable = {
        let url = Bundle(for: TimetableTests.self).url(forResource: "namaz_timings", withExtension: "csv")
            ?? Bundle.main.url(forResource: "namaz_timings", withExtension: "csv")!
        let csv = try! String(contentsOf: url, encoding: .utf8)
        return try! Timetable.fromCSV(csv, calendar: ist)
    }()

    private func date(_ y: Int, _ m: Int, _ d: Int) -> Date {
        Self.ist.date(from: DateComponents(year: y, month: m, day: d))!
    }

    func testParsesAllPrintedRows() throws {
        let url = Bundle(for: TimetableTests.self).url(forResource: "namaz_timings", withExtension: "csv")
            ?? Bundle.main.url(forResource: "namaz_timings", withExtension: "csv")!
        let rows = try TimetableParser.parse(try String(contentsOf: url, encoding: .utf8))
        XCTAssertEqual(rows.count, 12 * 16)
    }

    func testAmPmInference() throws {
        XCTAssertEqual(try TimetableParser.parseTime("5:38", pm: false), TimeOfDay(hour: 5, minute: 38))
        XCTAssertEqual(try TimetableParser.parseTime("12:37", pm: true), TimeOfDay(hour: 12, minute: 37))
        XCTAssertEqual(try TimetableParser.parseTime("3:51", pm: true), TimeOfDay(hour: 15, minute: 51))
        XCTAssertEqual(try TimetableParser.parseTime("7:31", pm: true), TimeOfDay(hour: 19, minute: 31))
        XCTAssertEqual(try TimetableParser.parseTime("12:05", pm: false), TimeOfDay(hour: 0, minute: 5))
    }

    func testPrintedOddDatesVerbatim() {
        let d = Self.table.day(for: date(2025, 1, 1))
        XCTAssertEqual(d.fajr, TimeOfDay(hour: 5, minute: 38))
        XCTAssertEqual(d.sunrise, TimeOfDay(hour: 6, minute: 55))
        XCTAssertEqual(d.zuhr, TimeOfDay(hour: 12, minute: 37))
        XCTAssertEqual(d.asrShafi, TimeOfDay(hour: 15, minute: 51))
        XCTAssertEqual(d.asrHanafi, TimeOfDay(hour: 16, minute: 39))
        XCTAssertEqual(d.maghrib, TimeOfDay(hour: 18, minute: 15))
        XCTAssertEqual(d.isha, TimeOfDay(hour: 19, minute: 31))
    }

    func testUnprintedDatesCarryForwardPreviousPrintedDay() {
        // Jan 2 is not printed -> identical to Jan 1 (no interpolation, no rounding).
        let jan1 = Self.table.day(for: date(2025, 1, 1))
        let jan2 = Self.table.day(for: date(2025, 1, 2))
        for p in Prayer.allCases {
            XCTAssertEqual(jan1.time(of: p, madhab: .hanafi), jan2.time(of: p, madhab: .hanafi), "\(p)")
            XCTAssertEqual(jan1.time(of: p, madhab: .shafi), jan2.time(of: p, madhab: .shafi), "\(p)")
        }
        XCTAssertEqual(jan2.fajr, TimeOfDay(hour: 5, minute: 38))
        XCTAssertEqual(jan2.isha, TimeOfDay(hour: 19, minute: 31))
        // Jan 4 -> Jan 3 (Zuhr 12:38), Jan 10 -> Jan 9 (Maghrib 6:20)
        XCTAssertEqual(Self.table.day(for: date(2025, 1, 4)).zuhr, TimeOfDay(hour: 12, minute: 38))
        XCTAssertEqual(Self.table.day(for: date(2025, 1, 10)).maghrib, TimeOfDay(hour: 18, minute: 20))
        XCTAssertFalse(Self.table.isPrinted(date(2025, 1, 2)))
        XCTAssertTrue(Self.table.isPrinted(date(2025, 1, 3)))
    }

    func testEveryDisplayedTimeAppearsVerbatimInCSV() throws {
        let url = Bundle(for: TimetableTests.self).url(forResource: "namaz_timings", withExtension: "csv")
            ?? Bundle.main.url(forResource: "namaz_timings", withExtension: "csv")!
        var printedTimes = Set<TimeOfDay>()
        for r in try TimetableParser.parse(try String(contentsOf: url, encoding: .utf8)) {
            printedTimes.formUnion([r.fajr, r.sunrise, r.zuhr, r.asrShafi, r.asrHanafi, r.maghrib, r.isha])
        }
        var d = date(2025, 1, 1)
        for _ in 0..<365 {
            let day = Self.table.day(for: d)
            for p in Prayer.allCases {
                XCTAssertTrue(printedTimes.contains(day.time(of: p, madhab: .hanafi)), "\(d) \(p)")
                XCTAssertTrue(printedTimes.contains(day.time(of: p, madhab: .shafi)), "\(d) \(p)")
            }
            d = Self.ist.date(byAdding: .day, value: 1, to: d)!
        }
    }

    func testEveryDayResolvesAndIsOrdered() {
        var d = date(2025, 1, 1)
        for _ in 0..<365 {
            let day = Self.table.day(for: d)
            XCTAssertLessThan(day.fajr, day.sunrise, "\(d)")
            XCTAssertLessThan(day.sunrise, day.zuhr, "\(d)")
            XCTAssertLessThan(day.zuhr, day.asrShafi, "\(d)")
            XCTAssertLessThan(day.asrShafi, day.asrHanafi, "\(d)")
            XCTAssertLessThan(day.asrHanafi, day.maghrib, "\(d)")
            XCTAssertLessThan(day.maghrib, day.isha, "\(d)")
            d = Self.ist.date(byAdding: .day, value: 1, to: d)!
        }
    }

    func testConsecutiveDaysNeverJumpMoreThanThreeMinutes() {
        var prev = Self.table.day(for: date(2025, 1, 1))
        var d = date(2025, 1, 2)
        for _ in 0..<365 {
            let cur = Self.table.day(for: d)
            for p in Prayer.allCases {
                let a = prev.time(of: p, madhab: .hanafi).minutesOfDay
                let b = cur.time(of: p, madhab: .hanafi).minutesOfDay
                XCTAssertLessThanOrEqual(abs(a - b), 3, "\(d) \(p)")
            }
            prev = cur
            d = Self.ist.date(byAdding: .day, value: 1, to: d)!
        }
    }

    func testMonthLengths() {
        XCTAssertEqual(Self.table.month(containing: date(2025, 2, 10)).count, 28)
        XCTAssertEqual(Self.table.month(containing: date(2024, 2, 10)).count, 29)
        XCTAssertEqual(Self.table.month(containing: date(2025, 12, 10)).count, 31)
        XCTAssertEqual(Self.table.month(containing: date(2025, 4, 10)).count, 30)
    }

    func testPhantomRowsDoNotLeak() {
        let feb = Self.table.month(containing: date(2025, 2, 1))
        XCTAssertEqual(feb.last!.date, date(2025, 2, 28))
        // Feb 28 is not printed -> carries forward the printed Feb 27 row.
        XCTAssertEqual(feb.last!.fajr, TimeOfDay(hour: 5, minute: 36))
        XCTAssertEqual(feb.last!.maghrib, TimeOfDay(hour: 18, minute: 41))
    }

    func testNextAndCurrentEventAcrossMidnight() {
        let repo = PrayerRepository(timetable: Self.table)
        let lateNight = Self.ist.date(from: DateComponents(year: 2025, month: 1, day: 1, hour: 23, minute: 0))!
        let next = repo.nextEvent(after: lateNight, madhab: .hanafi)
        XCTAssertEqual(next.prayer, .fajr)
        XCTAssertTrue(Self.ist.isDate(next.at, inSameDayAs: date(2025, 1, 2)))
        let current = repo.currentEvent(at: lateNight, madhab: .hanafi)
        XCTAssertEqual(current.prayer, .isha)
    }

    func testHijriConversion() {
        // 1 Ramadan 1446 = 1 March 2025 (Umm al-Qura)
        let h = HijriDate.from(date(2025, 3, 1), calendar: Self.ist)
        XCTAssertEqual(h.year, 1446)
        XCTAssertEqual(h.month, 9)
        XCTAssertEqual(h.day, 1)
        XCTAssertEqual(h.monthName, "Ramadan")
        let minus = HijriDate.from(date(2025, 3, 1), adjustmentDays: -1, calendar: Self.ist)
        XCTAssertEqual(minus.month, 8)
        XCTAssertEqual(minus.monthName, "Sha'ban")
    }

    func testTimeFormatting() {
        XCTAssertEqual(TimeFormat.time(TimeOfDay(hour: 5, minute: 38), use24Hour: false), "5:38 AM")
        XCTAssertEqual(TimeFormat.time(TimeOfDay(hour: 17, minute: 5), use24Hour: true), "17:05")
        XCTAssertEqual(TimeFormat.timeNumber(TimeOfDay(hour: 0, minute: 7), use24Hour: false), "12:07")
        XCTAssertEqual(TimeFormat.amPm(TimeOfDay(hour: 12, minute: 0), use24Hour: false), "PM")
        XCTAssertEqual(TimeFormat.countdown(3725), "01:02:05")
        XCTAssertEqual(TimeFormat.relative(2 * 3600 + 15 * 60), "in 2h 15m")
    }

    func testJumuah() {
        XCTAssertTrue(Prayer.zuhr.isJumuah(on: date(2025, 1, 3), calendar: Self.ist))   // Friday
        XCTAssertFalse(Prayer.zuhr.isJumuah(on: date(2025, 1, 4), calendar: Self.ist))
        XCTAssertFalse(Prayer.asr.isJumuah(on: date(2025, 1, 3), calendar: Self.ist))
        XCTAssertEqual(Prayer.zuhr.name(on: date(2025, 1, 3), calendar: Self.ist), "Jumu'ah")
    }
}
