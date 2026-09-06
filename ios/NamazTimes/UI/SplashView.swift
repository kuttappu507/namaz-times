import SwiftUI

/// Animated intro: crescent & star draw in over a night sky, the title cascades in, then the
/// whole thing hands over to the app.
struct SplashView: View {
    let onFinished: () -> Void
    @State private var progress: CGFloat = 0
    @State private var titleShown = false
    @State private var exiting = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [Palette.emerald05, Palette.emerald10, Palette.emerald20], startPoint: .top, endPoint: .bottom)
            AmbientOrbs(colors: [Palette.emerald40, Palette.emerald60, Palette.gold80.opacity(0.6)], alpha: 0.28, period: 12)
            Starfield(count: 60, seed: 11)

            VStack(spacing: 22) {
                CrescentAndStar(progress: progress)
                    .frame(width: 120, height: 120)
                    .shadow(color: Palette.gold80.opacity(0.45 * progress), radius: 30)
                VStack(spacing: 6) {
                    CascadingTitle(text: "Namaz Times", shown: titleShown)
                    Text("HUBLI · صلاة")
                        .font(AppFont.labelLarge)
                        .tracking(3)
                        .foregroundStyle(Palette.emerald80)
                        .opacity(titleShown ? 1 : 0)
                        .offset(y: titleShown ? 0 : 8)
                        .animation(.easeOut(duration: 0.5).delay(0.55), value: titleShown)
                }
            }
            .scaleEffect(exiting ? 1.08 : 1)
            .opacity(exiting ? 0 : 1)
        }
        .ignoresSafeArea()
        .onAppear {
            withAnimation(.easeInOut(duration: 1.0)) { progress = 1 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { titleShown = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.9) {
                withAnimation(.easeIn(duration: 0.38)) { exiting = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.38) { onFinished() }
            }
        }
    }
}

private struct CrescentAndStar: View {
    let progress: CGFloat

    var body: some View {
        Canvas { ctx, size in
            let r = min(size.width, size.height) * 0.36
            let c = CGPoint(x: size.width * 0.47, y: size.height * 0.52)
            // Crescent = big circle minus offset circle, revealed by trimming an arc.
            var crescent = Path()
            crescent.addArc(center: c, radius: r, startAngle: .degrees(-90), endAngle: .degrees(-90 + 360 * Double(progress)), clockwise: false)
            let cut = Path(ellipseIn: CGRect(x: c.x + r * 0.28 - r * 0.86, y: c.y - r * 0.86, width: r * 1.72, height: r * 1.72))
            ctx.stroke(crescent, with: .color(Palette.gold80), style: StrokeStyle(lineWidth: r * 0.16, lineCap: .round))
            ctx.blendMode = .destinationOut
            ctx.fill(cut, with: .color(.black))
            ctx.blendMode = .normal
            // Star
            if progress > 0.6 {
                let s = (progress - 0.6) / 0.4
                let sc = CGPoint(x: c.x + r * 0.95, y: c.y - r * 0.55)
                var star = Path()
                let outer = r * 0.22 * s, inner = outer * 0.45
                for i in 0..<10 {
                    let a = Double(i) * .pi / 5 - .pi / 2
                    let rad = i.isMultiple(of: 2) ? outer : inner
                    let p = CGPoint(x: sc.x + cos(a) * rad, y: sc.y + sin(a) * rad)
                    if i == 0 { star.move(to: p) } else { star.addLine(to: p) }
                }
                star.closeSubpath()
                ctx.fill(star, with: .color(Palette.gold90.opacity(Double(s))))
            }
        }
    }
}

private struct CascadingTitle: View {
    let text: String
    let shown: Bool
    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(text.enumerated()), id: \.offset) { i, ch in
                Text(String(ch))
                    .font(AppFont.outfit(34, weight: .bold))
                    .foregroundStyle(.white)
                    .opacity(shown ? 1 : 0)
                    .offset(y: shown ? 0 : 14)
                    .animation(.spring(response: 0.5, dampingFraction: 0.75).delay(Double(i) * 0.035), value: shown)
            }
        }
    }
}
