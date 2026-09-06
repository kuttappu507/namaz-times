import SwiftUI

// MARK: - Cards

/// The app's standard elevated surface: soft shadow, large radius, subtle vertical gradient and
/// a hairline highlight border so cards read clearly on both light and dark backgrounds.
struct GlassCard<Content: View>: View {
    @Environment(\.theme) private var theme
    var cornerRadius: CGFloat = 28
    var padding: CGFloat = 0
    @ViewBuilder var content: () -> Content

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content()
            .padding(padding)
            .background(
                LinearGradient(colors: [theme.cardStart, theme.cardEnd], startPoint: .top, endPoint: .bottom)
            )
            .clipShape(shape)
            .overlay(shape.strokeBorder(theme.cardBorder, lineWidth: 1))
            .shadow(
                color: theme.isDark ? .black.opacity(0.45) : Palette.emerald40.opacity(0.16),
                radius: theme.isDark ? 10 : 16, x: 0, y: 6
            )
    }
}

/// Small rounded label such as "NOW" or "NEXT".
struct Pill: View {
    let text: String
    var container: Color
    var content: Color

    var body: some View {
        Text(text)
            .font(AppFont.labelMedium)
            .foregroundStyle(content)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(container, in: Capsule())
    }
}

/// Section heading with a small gold rule.
struct SectionHeader<Trailing: View>: View {
    @Environment(\.theme) private var theme
    let title: String
    @ViewBuilder var trailing: () -> Trailing

    init(_ title: String, @ViewBuilder trailing: @escaping () -> Trailing) {
        self.title = title
        self.trailing = trailing
    }

    var body: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(AppFont.titleMedium).foregroundStyle(theme.onSurface)
                Capsule()
                    .fill(LinearGradient(colors: [theme.gold, theme.gold.opacity(0)], startPoint: .leading, endPoint: .trailing))
                    .frame(width: 28, height: 3)
            }
            Spacer()
            trailing()
        }
        .padding(.horizontal, 4)
    }
}

extension SectionHeader where Trailing == EmptyView {
    init(_ title: String) {
        self.init(title) { EmptyView() }
    }
}

/// Gradient circle with the prayer's SF Symbol.
struct PrayerIconBadge: View {
    @Environment(\.theme) private var theme
    let prayer: Prayer
    var size: CGFloat = 44
    var active: Bool = true

    var body: some View {
        let colors = active ? prayer.gradient : [theme.surfaceHigh, theme.surfaceHigh.opacity(0.7)]
        ZStack {
            Circle()
                .fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                .shadow(color: active ? colors[0].opacity(0.5) : .clear, radius: 6, y: 3)
            Image(systemName: prayer.symbolName)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(active ? Color.white : theme.onSurfaceVariant)
                .contentTransition(.symbolEffect(.replace))
        }
        .frame(width: size, height: size)
        .animation(.easeInOut(duration: 0.3), value: active)
    }
}

// MARK: - Motion

/// Fades + slides + un-scales content in the first time it appears, with a per-item delay.
struct StaggeredEntrance: ViewModifier {
    let index: Int
    var stagger: Double = 0.075
    var base: Double = 0.09
    var offsetY: CGFloat = 28
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : offsetY)
            .scaleEffect(shown ? 1 : 0.97)
            .onAppear {
                guard !shown else { return }
                withAnimation(.spring(response: 0.55, dampingFraction: 0.8).delay(base + Double(index) * stagger)) {
                    shown = true
                }
            }
    }
}

extension View {
    func staggeredEntrance(_ index: Int, stagger: Double = 0.075, base: Double = 0.09, offsetY: CGFloat = 28) -> some View {
        modifier(StaggeredEntrance(index: index, stagger: stagger, base: base, offsetY: offsetY))
    }

    /// Scales down slightly while pressed – the "tactile card" feel.
    func pressScale(_ scale: CGFloat = 0.97) -> some View {
        buttonStyle(PressScaleStyle(scale: scale))
    }
}

struct PressScaleStyle: ButtonStyle {
    var scale: CGFloat = 0.97
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

/// Diagonal shine sweeping across the parent, drawn on the GPU via `TimelineView`.
struct ShimmerOverlay: View {
    var color: Color = .white.opacity(0.17)
    var period: Double = 3.6

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 60)) { context in
            let t = context.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period) / period
            GeometryReader { geo in
                let w = geo.size.width
                LinearGradient(colors: [.clear, color, .clear], startPoint: .topLeading, endPoint: .bottomTrailing)
                    .frame(width: w * 0.34)
                    .offset(x: -w * 0.6 + w * 2.2 * t)
            }
        }
        .allowsHitTesting(false)
    }
}

/// Slowly drifting soft light blobs – ambient motion for the hero card / splash.
struct AmbientOrbs: View {
    let colors: [Color]
    var alpha: Double = 0.4
    var period: Double = 11

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30)) { context in
            let t = context.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: period) / period
            Canvas { ctx, size in
                let w = size.width, h = size.height
                for (i, c) in colors.enumerated() {
                    let phase = t * 2 * .pi + Double(i) * 2.1
                    let cx = w * (0.5 + 0.45 * cos(phase * (1 + Double(i) * 0.13)))
                    let cy = h * (0.5 + 0.45 * sin(phase * (0.8 + Double(i) * 0.17)))
                    let r = max(w, h) * (0.34 + 0.08 * sin(phase))
                    let rect = CGRect(x: cx - r, y: cy - r, width: 2 * r, height: 2 * r)
                    ctx.fill(
                        Path(ellipseIn: rect),
                        with: .radialGradient(
                            Gradient(colors: [c.opacity(alpha), c.opacity(0)]),
                            center: CGPoint(x: cx, y: cy), startRadius: 0, endRadius: r
                        )
                    )
                }
            }
        }
        .allowsHitTesting(false)
    }
}

/// Twinkling stars for night-time gradients and the splash.
struct Starfield: View {
    var count: Int = 40
    var seed: UInt64 = 7

    private var stars: [(CGFloat, CGFloat, CGFloat, Double)] {
        var rng = SeededGenerator(seed: seed)
        return (0..<count).map { _ in
            (CGFloat.random(in: 0...1, using: &rng), CGFloat.random(in: 0...1, using: &rng),
             CGFloat.random(in: 0.3...1.1, using: &rng), Double.random(in: 0...1, using: &rng))
        }
    }

    var body: some View {
        let stars = self.stars
        TimelineView(.animation(minimumInterval: 1 / 30)) { context in
            let t = context.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 4) / 4
            Canvas { ctx, size in
                for s in stars {
                    let twinkle = 0.35 + 0.65 * ((sin((t + s.3) * 2 * .pi) + 1) / 2)
                    let rect = CGRect(x: s.0 * size.width - s.2, y: s.1 * size.height - s.2, width: s.2 * 2, height: s.2 * 2)
                    ctx.fill(Path(ellipseIn: rect), with: .color(.white.opacity(twinkle * 0.9)))
                }
            }
        }
        .allowsHitTesting(false)
    }
}

struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64
    init(seed: UInt64) { state = seed &+ 0x9E3779B97F4A7C15 }
    mutating func next() -> UInt64 {
        state &+= 0x9E3779B97F4A7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58476D1CE4E5B9
        z = (z ^ (z >> 27)) &* 0x94D049BB133111EB
        return z ^ (z >> 31)
    }
}

/// Static soft colour wash behind the whole app.
struct BackgroundWash: View {
    @Environment(\.theme) private var theme

    var body: some View {
        ZStack {
            theme.background
            GeometryReader { geo in
                let w = geo.size.width, h = geo.size.height
                RadialGradient(colors: [Palette.emerald60.opacity(0.16), .clear], center: UnitPoint(x: 0.12, y: 0.02), startRadius: 0, endRadius: w * 0.95)
                RadialGradient(colors: [Palette.gold60.opacity(theme.isDark ? 0.10 : 0.12), .clear], center: UnitPoint(x: 1.0, y: 0.48), startRadius: 0, endRadius: w * 0.8)
                RadialGradient(colors: [Palette.emerald60.opacity(0.11), .clear], center: UnitPoint(x: 0.25, y: 1.02), startRadius: 0, endRadius: w * 0.85)
                    .frame(width: w, height: h)
            }
        }
        .ignoresSafeArea()
    }
}

/// Countdown text where each digit rolls when it changes (odometer style).
struct RollingText: View {
    let text: String
    var font: Font
    var color: Color

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(text.enumerated()), id: \.offset) { _, ch in
                Text(String(ch))
                    .font(font)
                    .foregroundStyle(color)
                    .monospacedDigit()
                    .contentTransition(.numericText(countsDown: true))
            }
        }
        .animation(.easeOut(duration: 0.26), value: text)
    }
}
