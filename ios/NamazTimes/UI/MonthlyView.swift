import SwiftUI

/// Full-month timetable with Hijri dates; today is highlighted and scrolled into view.
struct MonthlyView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var settingsStore: SettingsStore
    @Environment(\.theme) private var theme
    @State private var forward = true

    private static let monthFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_IN")
        f.dateFormat = "MMMM yyyy"
        return f
    }()

    var body: some View {
        let state = model.monthly
        let settings = settingsStore.settings
        VStack(spacing: 0) {
            MonthHeader(
                title: Self.monthFormatter.string(from: state.monthStart),
                hijriRange: hijriRange(state),
                isCurrentMonth: model.isCurrentMonthSelected,
                onPrevious: { forward = false; withAnimation(.snappy) { model.previousMonth() } },
                onNext: { forward = true; withAnimation(.snappy) { model.nextMonth() } },
                onToday: { forward = true; withAnimation(.snappy) { model.jumpToCurrentMonth() } }
            )
            .staggeredEntrance(0)
            .padding(.horizontal, 18)
            .padding(.top, 10)

            ColumnHeaders(madhabLabel: settings.madhab.label)
                .staggeredEntrance(1)
                .padding(.horizontal, 18)
                .padding(.top, 14)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(Array(state.days.enumerated()), id: \.element.id) { index, day in
                            DayRow(
                                day: day,
                                isToday: day.date == state.today,
                                use24: settings.use24Hour,
                                madhab: settings.madhab,
                                hijriAdjustment: settings.hijriAdjustment,
                                striped: index % 2 == 1
                            )
                            .id(day.id)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.top, 8)
                    .padding(.bottom, 110)
                    .id(state.monthStart)
                    .transition(.asymmetric(
                        insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity),
                        removal: .move(edge: forward ? .leading : .trailing).combined(with: .opacity)
                    ))
                }
                .scrollIndicators(.hidden)
                .onAppear { scrollToToday(proxy, state) }
                .onChange(of: state.monthStart) { _, _ in scrollToToday(proxy, state) }
            }
        }
    }

    private func scrollToToday(_ proxy: ScrollViewProxy, _ state: MonthlyState) {
        guard model.isCurrentMonthSelected else { return }
        let day = Calendar.current.component(.day, from: state.today)
        let target = state.days[max(0, min(state.days.count - 1, day - 3))]
        DispatchQueue.main.async { proxy.scrollTo(target.id, anchor: .top) }
    }

    private func hijriRange(_ state: MonthlyState) -> String {
        guard let first = state.days.first, let last = state.days.last else { return "" }
        let a = HijriDate.from(first.date, adjustmentDays: model.settings.hijriAdjustment, calendar: model.calendar)
        let b = HijriDate.from(last.date, adjustmentDays: model.settings.hijriAdjustment, calendar: model.calendar)
        if a.month == b.month && a.year == b.year { return "\(a.monthName) \(a.year) AH" }
        if a.year == b.year { return "\(a.monthName) – \(b.monthName) \(a.year) AH" }
        return "\(a.monthName) \(a.year) – \(b.monthName) \(b.year) AH"
    }
}

private struct MonthHeader: View {
    @Environment(\.theme) private var theme
    let title: String
    let hijriRange: String
    let isCurrentMonth: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onToday: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            RoundIconButton(symbol: "chevron.left", action: onPrevious)
            VStack(spacing: 2) {
                Text(title)
                    .font(AppFont.titleLarge)
                    .foregroundStyle(theme.onSurface)
                    .contentTransition(.numericText())
                Text(hijriRange)
                    .font(AppFont.bodySmall)
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .animation(.easeInOut(duration: 0.25), value: title)
            RoundIconButton(symbol: "chevron.right", action: onNext)
        }
        .overlay(alignment: .bottom) {
            if !isCurrentMonth {
                Button("Today", action: onToday)
                    .font(AppFont.labelMedium)
                    .foregroundStyle(theme.onPrimary)
                    .padding(.horizontal, 12).padding(.vertical, 5)
                    .background(theme.primary, in: Capsule())
                    .offset(y: 34)
                    .transition(.scale.combined(with: .opacity))
            }
        }
        .padding(.bottom, isCurrentMonth ? 0 : 30)
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: isCurrentMonth)
    }
}

private struct RoundIconButton: View {
    @Environment(\.theme) private var theme
    let symbol: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(theme.onPrimaryContainer)
                .frame(width: 40, height: 40)
                .background(theme.primaryContainer, in: Circle())
        }
        .pressScale(0.9)
    }
}

private struct ColumnHeaders: View {
    @Environment(\.theme) private var theme
    let madhabLabel: String
    var body: some View {
        HStack(spacing: 0) {
            Text("Date").frame(width: 58, alignment: .leading)
            ForEach(["Fajr", "Rise", "Zuhr", "Asr", "Magh", "Isha"], id: \.self) { label in
                Text(label).frame(maxWidth: .infinity)
            }
        }
        .font(AppFont.labelSmall)
        .foregroundStyle(theme.onSurfaceVariant)
        .padding(.horizontal, 10)
        .overlay(alignment: .topTrailing) {
            Text("Asr · \(madhabLabel)")
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(theme.gold)
                .offset(y: -12)
        }
    }
}

private struct DayRow: View {
    @Environment(\.theme) private var theme
    let day: PrayerDay
    let isToday: Bool
    let use24: Bool
    let madhab: AsrMadhab
    let hijriAdjustment: Int
    let striped: Bool

    private static let weekday: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_IN"); f.dateFormat = "EEE"; return f
    }()

    var body: some View {
        let hijri = HijriDate.from(day.date, adjustmentDays: hijriAdjustment)
        let isFriday = Calendar.current.component(.weekday, from: day.date) == 6
        let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 4) {
                    Text("\(Calendar.current.component(.day, from: day.date))")
                        .font(AppFont.outfit(16, weight: .bold))
                        .foregroundStyle(isToday ? theme.onPrimary : (isFriday ? theme.primary : theme.onSurface))
                    Text(Self.weekday.string(from: day.date))
                        .font(AppFont.labelSmall)
                        .foregroundStyle(isToday ? theme.onPrimary.opacity(0.85) : theme.onSurfaceVariant)
                }
                Text("\(hijri.day) \(hijri.monthShortName)")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(isToday ? theme.onPrimary.opacity(0.85) : theme.gold)
            }
            .frame(width: 58, alignment: .leading)
            ForEach(Prayer.allCases) { prayer in
                Text(TimeFormat.timeNumber(day.time(of: prayer, madhab: madhab), use24Hour: use24))
                    .font(AppFont.outfit(13, weight: prayer.isSalah ? .medium : .regular))
                    .monospacedDigit()
                    .foregroundStyle(isToday ? theme.onPrimary : (prayer.isSalah ? theme.onSurface : theme.onSurfaceVariant))
                    .frame(maxWidth: .infinity)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background {
            if isToday {
                shape.fill(LinearGradient(colors: [theme.primary, theme.primary.opacity(0.82)], startPoint: .leading, endPoint: .trailing))
                    .shadow(color: theme.primary.opacity(0.4), radius: 10, y: 4)
            } else if striped {
                shape.fill(theme.isDark ? Color.white.opacity(0.03) : Color.black.opacity(0.025))
            }
        }
        .padding(.vertical, 1)
    }
}
