import Foundation

/// The six daily events shown by the app. `sunrise` is not a prayer, but it marks the end of
/// Fajr and is printed in every timetable, so it is modelled alongside the five salah.
enum Prayer: String, CaseIterable, Codable, Identifiable {
    case fajr, sunrise, zuhr, asr, maghrib, isha

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .fajr: return "Fajr"
        case .sunrise: return "Sunrise"
        case .zuhr: return "Zuhr"
        case .asr: return "Asr"
        case .maghrib: return "Maghrib"
        case .isha: return "Isha"
        }
    }

    var arabicName: String {
        switch self {
        case .fajr: return "الفجر"
        case .sunrise: return "الشروق"
        case .zuhr: return "الظهر"
        case .asr: return "العصر"
        case .maghrib: return "المغرب"
        case .isha: return "العشاء"
        }
    }

    var isSalah: Bool { self != .sunrise }

    /// SF Symbol used for the prayer's badge.
    var symbolName: String {
        switch self {
        case .fajr: return "sun.horizon.fill"
        case .sunrise: return "sunrise.fill"
        case .zuhr: return "sun.max.fill"
        case .asr: return "sun.min.fill"
        case .maghrib: return "sunset.fill"
        case .isha: return "moon.stars.fill"
        }
    }

    /// Zuhr on a Friday is Jumu'ah.
    func isJumuah(on date: Date, calendar: Calendar = .current) -> Bool {
        self == .zuhr && calendar.component(.weekday, from: date) == 6 // 1 = Sunday … 6 = Friday
    }

    /// Display name for a specific date ("Jumu'ah" on Fridays).
    func name(on date: Date, calendar: Calendar = .current) -> String {
        isJumuah(on: date, calendar: calendar) ? "Jumu'ah" : displayName
    }

    static let salah: [Prayer] = allCases.filter { $0.isSalah }
}

/// School of thought used for the Asr time. Both columns are present in the timetable.
enum AsrMadhab: String, CaseIterable, Codable, Identifiable {
    case shafi, hanafi

    var id: String { rawValue }

    var label: String {
        switch self {
        case .shafi: return "Shafi'i"
        case .hanafi: return "Hanafi"
        }
    }

    var description: String {
        switch self {
        case .shafi: return "Shadow equals object length (also Maliki, Hanbali)"
        case .hanafi: return "Shadow equals twice the object length"
        }
    }
}
