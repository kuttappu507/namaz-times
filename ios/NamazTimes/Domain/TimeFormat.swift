import Foundation

enum TimeFormat {
    /// "5:38 AM" or "05:38".
    static func time(_ t: TimeOfDay, use24Hour: Bool) -> String {
        if use24Hour { return String(format: "%02d:%02d", t.hour, t.minute) }
        return "\(timeNumber(t, use24Hour: false)) \(amPm(t, use24Hour: false))"
    }

    /// The numeric part only – "5:38" / "05:38" – for layouts that render AM/PM separately.
    static func timeNumber(_ t: TimeOfDay, use24Hour: Bool) -> String {
        if use24Hour { return String(format: "%02d:%02d", t.hour, t.minute) }
        var h = t.hour % 12
        if h == 0 { h = 12 }
        return String(format: "%d:%02d", h, t.minute)
    }

    /// "AM" / "PM" or empty in 24-hour mode.
    static func amPm(_ t: TimeOfDay, use24Hour: Bool) -> String {
        use24Hour ? "" : (t.hour < 12 ? "AM" : "PM")
    }

    /// "01:23:45" style countdown.
    static func countdown(_ interval: TimeInterval) -> String {
        let total = max(0, Int(interval.rounded(.down)))
        return String(format: "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    /// Human phrase such as "in 2h 15m" or "in 40 sec".
    static func relative(_ interval: TimeInterval) -> String {
        let total = max(0, Int(interval))
        let h = total / 3600
        let m = (total % 3600) / 60
        switch (h, m) {
        case let (h, m) where h > 0 && m > 0: return "in \(h)h \(m)m"
        case let (h, _) where h > 0: return "in \(h)h"
        case let (_, m) where m > 0: return "in \(m) min"
        default: return "in \(total) sec"
        }
    }
}
