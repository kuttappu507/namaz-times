import Foundation

/// Islamic (Hijri) date derived from the system's Umm al-Qura calendar, with a user-adjustable
/// offset of a few days to match local moon sighting.
struct HijriDate: Hashable {
    let day: Int
    let month: Int      // 1...12
    let year: Int

    var monthName: String { HijriDate.months[month - 1] }
    var monthShortName: String { HijriDate.monthsShort[month - 1] }
    var monthNameArabic: String { HijriDate.monthsArabic[month - 1] }

    var formatted: String { "\(day) \(monthName) \(year) AH" }

    static let months = [
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah",
    ]
    static let monthsShort = [
        "Muh", "Saf", "Rab I", "Rab II", "Jum I", "Jum II",
        "Raj", "Sha", "Ram", "Shw", "DhQ", "DhH",
    ]
    static let monthsArabic = [
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    ]

    private static var hijriCalendar: Calendar = {
        var c = Calendar(identifier: .islamicUmmAlQura)
        c.timeZone = .current
        return c
    }()

    static func from(_ date: Date, adjustmentDays: Int = 0, calendar: Calendar = .current) -> HijriDate {
        let adjusted = calendar.date(byAdding: .day, value: adjustmentDays, to: date) ?? date
        var hijri = hijriCalendar
        hijri.timeZone = calendar.timeZone
        let c = hijri.dateComponents([.year, .month, .day], from: adjusted)
        return HijriDate(day: c.day ?? 1, month: c.month ?? 1, year: c.year ?? 1446)
    }
}
