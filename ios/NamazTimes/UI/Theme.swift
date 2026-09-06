import SwiftUI

// MARK: - Palette (mirrors the Android Color.kt)

enum Palette {
    static let emerald05 = Color(hex: 0x031A14)
    static let emerald10 = Color(hex: 0x06261E)
    static let emerald20 = Color(hex: 0x06483A)
    static let emerald30 = Color(hex: 0x0B5F49)
    static let emerald40 = Color(hex: 0x0B7A5B)
    static let emerald50 = Color(hex: 0x139A72)
    static let emerald60 = Color(hex: 0x19B58A)
    static let emerald70 = Color(hex: 0x4CCBA6)
    static let emerald80 = Color(hex: 0x8FE3C6)
    static let emerald90 = Color(hex: 0xC3F3E2)

    static let gold40 = Color(hex: 0x8F6A05)
    static let gold60 = Color(hex: 0xD9A62F)
    static let gold70 = Color(hex: 0xE8BC52)
    static let gold80 = Color(hex: 0xF5C451)
    static let gold90 = Color(hex: 0xFFE7A6)

    static let ink = Color(hex: 0x0B1512)
    static let ink2 = Color(hex: 0x131E19)
    static let ink3 = Color(hex: 0x1B2A25)
    static let mist = Color(hex: 0xF4FAF6)
    static let mist2 = Color(hex: 0xEAF3EE)
    static let slate = Color(hex: 0x5C6B64)
}

/// Sky gradient for the hero card, keyed by hour of day.
struct SkyPalette: Equatable {
    let top: Color
    let mid: Color
    let bottom: Color
    let glow: Color
    let accent: Color

    static let night = SkyPalette(top: Color(hex: 0x0B1630), mid: Color(hex: 0x15264F), bottom: Color(hex: 0x223B6E), glow: Color(hex: 0x4A6DB0), accent: Palette.gold80)
    static let dawn = SkyPalette(top: Color(hex: 0x2A2E62), mid: Color(hex: 0x7A4B7C), bottom: Color(hex: 0xE88A64), glow: Color(hex: 0xFFB27A), accent: Palette.gold90)
    static let morning = SkyPalette(top: Color(hex: 0x06483A), mid: Color(hex: 0x0E8A66), bottom: Color(hex: 0x3FBF93), glow: Color(hex: 0x8CE7C2), accent: Palette.gold80)
    static let noon = SkyPalette(top: Color(hex: 0x0B5F49), mid: Color(hex: 0x14A47A), bottom: Color(hex: 0x61CFA6), glow: Color(hex: 0xC9F5E4), accent: Palette.gold80)
    static let afternoon = SkyPalette(top: Color(hex: 0x14524A), mid: Color(hex: 0x2C8A72), bottom: Color(hex: 0xD9A62F), glow: Color(hex: 0xFFD873), accent: .white)
    static let dusk = SkyPalette(top: Color(hex: 0x2E1B4E), mid: Color(hex: 0x7A3A6D), bottom: Color(hex: 0xE5643F), glow: Color(hex: 0xFF9C6B), accent: Palette.gold90)
    static let evening = SkyPalette(top: Color(hex: 0x14183A), mid: Color(hex: 0x2B2F6B), bottom: Color(hex: 0x4B3F8C), glow: Color(hex: 0x7C6CC7), accent: Palette.gold80)

    static func forHour(_ hour: Int) -> SkyPalette {
        switch hour {
        case 0..<5: return .night
        case 5..<7: return .dawn
        case 7..<11: return .morning
        case 11..<15: return .noon
        case 15..<18: return .afternoon
        case 18..<20: return .dusk
        case 20..<22: return .evening
        default: return .night
        }
    }
}

extension Prayer {
    var gradient: [Color] {
        switch self {
        case .fajr: return [Color(hex: 0x4A5BD6), Color(hex: 0x8E7BFF)]
        case .sunrise: return [Color(hex: 0xF7A54B), Color(hex: 0xFFD36B)]
        case .zuhr: return [Color(hex: 0x16A37A), Color(hex: 0x5CD6AA)]
        case .asr: return [Color(hex: 0xD9A62F), Color(hex: 0xF2CB6A)]
        case .maghrib: return [Color(hex: 0xE5643F), Color(hex: 0xF59A6B)]
        case .isha: return [Color(hex: 0x2B3A8C), Color(hex: 0x5B6CD6)]
        }
    }
}

// MARK: - Semantic theme

/// Resolved colours for the current appearance – injected through the environment so every
/// view reads one value instead of branching on the colour scheme.
struct AppTheme: Equatable {
    let isDark: Bool
    let background: Color
    let surface: Color
    let surfaceHigh: Color
    let onSurface: Color
    let onSurfaceVariant: Color
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let gold: Color
    let goldContainer: Color
    let onGoldContainer: Color
    let cardStart: Color
    let cardEnd: Color
    let cardBorder: Color
    let navGlass: Color
    let outline: Color
    let error: Color
    let errorContainer: Color
    let onErrorContainer: Color

    static let dark = AppTheme(
        isDark: true,
        background: Palette.ink,
        surface: Palette.ink2,
        surfaceHigh: Color(hex: 0x2A3B34),
        onSurface: Color(hex: 0xE6F1EC),
        onSurfaceVariant: Color(hex: 0xA9B8B1),
        primary: Palette.emerald70,
        onPrimary: Palette.emerald10,
        primaryContainer: Palette.emerald30,
        onPrimaryContainer: Palette.emerald90,
        gold: Palette.gold80,
        goldContainer: Color(hex: 0x3F3000),
        onGoldContainer: Palette.gold90,
        cardStart: Color(hex: 0x1B2A25),
        cardEnd: Color(hex: 0x14211C),
        cardBorder: Color.white.opacity(0.10),
        navGlass: Color(hex: 0x1B2A25).opacity(0.96),
        outline: Color(hex: 0x3E4F48),
        error: Color(hex: 0xFFB4AB),
        errorContainer: Color(hex: 0x93000A),
        onErrorContainer: Color(hex: 0xFFDAD6)
    )

    static let light = AppTheme(
        isDark: false,
        background: Palette.mist,
        surface: .white,
        surfaceHigh: Palette.mist2,
        onSurface: Color(hex: 0x0F1F19),
        onSurfaceVariant: Palette.slate,
        primary: Palette.emerald40,
        onPrimary: .white,
        primaryContainer: Palette.emerald90,
        onPrimaryContainer: Palette.emerald10,
        gold: Palette.gold40,
        goldContainer: Palette.gold90,
        onGoldContainer: Color(hex: 0x4A3800),
        cardStart: .white,
        cardEnd: Color(hex: 0xF0F7F3),
        cardBorder: Color.white.opacity(0.9),
        navGlass: Color.white.opacity(0.94),
        outline: Color(hex: 0xC3D2CB),
        error: Color(hex: 0xBA1A1A),
        errorContainer: Color(hex: 0xFFDAD6),
        onErrorContainer: Color(hex: 0x410002)
    )
}

private struct AppThemeKey: EnvironmentKey {
    static let defaultValue: AppTheme = .dark
}

extension EnvironmentValues {
    var theme: AppTheme {
        get { self[AppThemeKey.self] }
        set { self[AppThemeKey.self] = newValue }
    }
}

// MARK: - Typography (Outfit, bundled; falls back to the system font if not registered)

enum AppFont {
    static func outfit(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        // Four static Outfit instances are bundled (see Info.plist → UIAppFonts). Anything
        // heavier than bold maps to Bold, lighter than regular maps to Regular.
        let name: String
        switch weight {
        case .black, .heavy, .bold: name = "Outfit-Bold"
        case .semibold: name = "Outfit-SemiBold"
        case .medium: name = "Outfit-Medium"
        default: name = "Outfit-Regular"
        }
        return .custom(name, size: size)
    }

    static let display = outfit(40, weight: .bold)
    static let headlineLarge = outfit(32, weight: .semibold)
    static let headlineMedium = outfit(26, weight: .semibold)
    static let titleLarge = outfit(22, weight: .semibold)
    static let titleMedium = outfit(17, weight: .semibold)
    static let body = outfit(15)
    static let bodySmall = outfit(13)
    static let labelLarge = outfit(14, weight: .medium)
    static let labelMedium = outfit(12, weight: .semibold)
    static let labelSmall = outfit(11, weight: .medium)
}

// MARK: - Helpers

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }
}
