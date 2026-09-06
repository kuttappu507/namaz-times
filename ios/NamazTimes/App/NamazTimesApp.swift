import SwiftUI
import UIKit
import UserNotifications

@main
struct NamazTimesApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .environmentObject(model.settingsStore)
                .preferredColorScheme(preferredScheme)
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { model.onBecameActive() }
                }
        }
    }

    private var preferredScheme: ColorScheme? {
        switch model.settingsStore.settings.themeMode {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

/// Shows notifications as banners even while the app is in the foreground.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        NotificationScheduler.shared.registerCategories()
        return true
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }
}

/// Splash → main app, with the theme resolved once and injected into the environment.
struct RootView: View {
    @Environment(\.colorScheme) private var colorScheme
    @State private var splashDone = false

    var body: some View {
        let theme: AppTheme = colorScheme == .dark ? .dark : .light
        ZStack {
            BackgroundWash()
            if splashDone {
                MainTabs()
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }
            if !splashDone {
                SplashView { withAnimation(.easeOut(duration: 0.5)) { splashDone = true } }
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .environment(\.theme, theme)
    }
}

// MARK: - Tabs

enum AppTab: Int, CaseIterable, Identifiable {
    case today, monthly, settings
    var id: Int { rawValue }
    var label: String {
        switch self { case .today: return "Today"; case .monthly: return "Monthly"; case .settings: return "Settings" }
    }
    var symbol: String {
        switch self { case .today: return "sun.horizon"; case .monthly: return "calendar"; case .settings: return "gearshape" }
    }
    var selectedSymbol: String {
        switch self { case .today: return "sun.horizon.fill"; case .monthly: return "calendar"; case .settings: return "gearshape.fill" }
    }
}

/// Three kept-alive pages in a paging scroll view + a floating pill nav bar whose indicator is
/// bound to the scroll position – tap or swipe, everything moves as one piece.
struct MainTabs: View {
    @Environment(\.theme) private var theme
    @State private var selected: AppTab = .today

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selected) {
                TodayView().tag(AppTab.today)
                MonthlyView().tag(AppTab.monthly)
                SettingsView().tag(AppTab.settings)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .ignoresSafeArea(.keyboard)

            FloatingNavBar(selected: $selected)
                .padding(.bottom, 14)
        }
    }
}

struct FloatingNavBar: View {
    @Environment(\.theme) private var theme
    @Binding var selected: AppTab
    private let tabWidth: CGFloat = 96
    private let tabHeight: CGFloat = 54

    var body: some View {
        ZStack(alignment: .leading) {
            Capsule()
                .fill(LinearGradient(colors: [theme.primary, theme.primary.opacity(0.82)], startPoint: .leading, endPoint: .trailing))
                .frame(width: tabWidth, height: tabHeight)
                .offset(x: CGFloat(selected.rawValue) * tabWidth)
                .animation(.spring(response: 0.42, dampingFraction: 0.86), value: selected)
            HStack(spacing: 0) {
                ForEach(AppTab.allCases) { tab in
                    let isSel = tab == selected
                    Button {
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.86)) { selected = tab }
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    } label: {
                        VStack(spacing: 2) {
                            Image(systemName: isSel ? tab.selectedSymbol : tab.symbol)
                                .font(.system(size: 18, weight: .semibold))
                                .contentTransition(.symbolEffect(.replace))
                                .scaleEffect(isSel ? 1.08 : 1)
                            Text(tab.label).font(AppFont.labelSmall)
                        }
                        .foregroundStyle(isSel ? theme.onPrimary : theme.onSurfaceVariant)
                        .frame(width: tabWidth, height: tabHeight)
                        .contentShape(Capsule())
                    }
                    .buttonStyle(PressScaleStyle(scale: 0.92))
                    .animation(.easeInOut(duration: 0.24), value: isSel)
                }
            }
        }
        .padding(5)
        .background(theme.navGlass, in: Capsule())
        .overlay(Capsule().strokeBorder(theme.isDark ? Color.white.opacity(0.10) : Color.white.opacity(0.9), lineWidth: 1))
        .shadow(color: Palette.emerald40.opacity(theme.isDark ? 0.5 : 0.3), radius: 22, y: 10)
    }
}
