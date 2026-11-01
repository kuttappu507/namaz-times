"use client";

/** Monthly timetable — ported from MonthlyScreen.kt. */
import { useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronLeft, ChevronRight, CalendarCheck } from "lucide-react";
import {
  PRAYERS,
  monthDays,
  timeOf,
  type AsrMadhab,
  type PrayerDay,
} from "@/lib/namaz/prayers";
import { timeNumber } from "@/lib/namaz/time";
import {
  hijriFrom,
  hijriMonthName,
  hijriMonthShort,
  type HijriDate,
} from "@/lib/namaz/hijri";
import type { UserSettings } from "@/lib/namaz/settings";
import { Stagger } from "./effects";

const COLUMN_WEIGHTS = [1.4, 1, 1, 1, 1, 1, 1];
const LABELS = ["Date", "Fajr", "Sunrise", "Zuhr", "Asr", "Maghrib", "Isha"];

export function MonthlyScreen({
  settings,
  today,
}: {
  settings: UserSettings;
  today: Date;
}) {
  const [yearMonth, setYearMonth] = useState(() => ({
    year: today.getFullYear(),
    month: today.getMonth() + 1,
  }));
  // Navigation direction so the month slides the right way (MonthlyScreen.kt).
  const [direction, setDirection] = useState(1);
  const forward = direction >= 0;

  const use24 = settings.use24Hour;
  const madhab = settings.madhab;
  const isCurrentMonth = yearMonth.year === today.getFullYear() && yearMonth.month === today.getMonth() + 1;

  const days = useMemo(
    () => monthDays(yearMonth.year, yearMonth.month),
    [yearMonth.year, yearMonth.month]
  );

  const hijriRange = useMemo(() => {
    if (days.length === 0) return "";
    const firstDay = new Date(days[0].year, days[0].month - 1, days[0].day);
    const lastDay = new Date(
      days[days.length - 1].year,
      days[days.length - 1].month - 1,
      days[days.length - 1].day
    );
    const first = hijriFrom(firstDay, settings.hijriAdjustment);
    const last = hijriFrom(lastDay, settings.hijriAdjustment);
    if (first.month === last.month && first.year === last.year)
      return `${hijriMonthName(first)} ${first.year} AH`;
    if (first.year === last.year)
      return `${hijriMonthName(first)} – ${hijriMonthName(last)} ${first.year} AH`;
    return `${hijriMonthName(first)} ${first.year} – ${hijriMonthName(last)} ${last.year} AH`;
  }, [days, settings.hijriAdjustment]);

  const monthName = new Intl.DateTimeFormat("en", { month: "long" }).format(
    new Date(yearMonth.year, yearMonth.month - 1, 1)
  );

  const shift = (delta: number) => {
    setDirection(delta);
    setYearMonth(({ year, month }) => {
      const d = new Date(year, month - 1 + delta, 1);
      return { year: d.getFullYear(), month: d.getMonth() + 1 };
    });
  };

  const jumpToday = () => {
    setDirection(
      (today.getFullYear() * 12 + today.getMonth()) -
        (yearMonth.year * 12 + yearMonth.month - 1)
    );
    setYearMonth({ year: today.getFullYear(), month: today.getMonth() + 1 });
  };

  return (
    <div className="flex h-full flex-col">
      <Stagger index={0}>
        <MonthHeader
          monthName={monthName}
          year={yearMonth.year}
          hijriRange={hijriRange}
          isCurrentMonth={isCurrentMonth}
          onPrevious={() => shift(-1)}
          onNext={() => shift(1)}
          onToday={jumpToday}
        />
      </Stagger>
      <Stagger index={1}>
        <ColumnHeaders madhabLabel={madhab === "HANAFI" ? "Hanafi" : "Shafi'i"} />
      </Stagger>

      <div className="min-h-0 flex-1 overflow-hidden">
        <AnimatePresence mode="popLayout" initial={false} custom={forward ? 1 : -1}>
          <motion.div
            key={`${yearMonth.year}-${yearMonth.month}`}
            custom={forward ? 1 : -1}
            initial={{ x: (forward ? 1 : -1) * 120, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: (forward ? -1 : 1) * 120, opacity: 0 }}
            transition={{ duration: 0.34, ease: [0.33, 1, 0.68, 1] }}
            className="h-full"
          >
            <MonthDayList
              days={days}
              today={today}
              yearMonth={yearMonth}
              use24={use24}
              madhab={madhab}
              hijriAdjustment={settings.hijriAdjustment}
            />
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  );
}

function MonthHeader({
  monthName,
  year,
  hijriRange,
  isCurrentMonth,
  onPrevious,
  onNext,
  onToday,
}: {
  monthName: string;
  year: number;
  hijriRange: string;
  isCurrentMonth: boolean;
  onPrevious: () => void;
  onNext: () => void;
  onToday: () => void;
}) {
  return (
    <div className="flex items-center px-3.5 py-2.5">
      <IconButton onClick={onPrevious} label="Previous month">
        <ChevronLeft size={22} />
      </IconButton>
      <div className="min-w-0 flex-1 text-center">
        <div className="flex items-baseline justify-center">
          <h2 className="text-[28px] font-semibold" style={{ color: "var(--on-surface)" }}>
            {monthName}
          </h2>
          <span className="ml-1.5 text-[21px] font-semibold" style={{ color: "var(--gold)" }}>
            {year}
          </span>
        </div>
        <p className="truncate text-[14px]" style={{ color: "var(--on-surface-variant)" }}>
          {hijriRange}
        </p>
      </div>
      <IconButton onClick={onNext} label="Next month">
        <ChevronRight size={22} />
      </IconButton>
      <div className="w-1.5" />
      <button
        type="button"
        onClick={onToday}
        disabled={isCurrentMonth}
        aria-label="Jump to current month"
        className="flex h-11 w-11 items-center justify-center rounded-full transition-opacity"
        style={{
          background: isCurrentMonth ? "var(--surface-high)" : "var(--primary)",
          color: isCurrentMonth ? "var(--on-surface-variant)" : "var(--on-primary)",
          opacity: isCurrentMonth ? 0.6 : 1,
          cursor: isCurrentMonth ? "default" : "pointer",
        }}
      >
        <CalendarCheck size={20} />
      </button>
    </div>
  );
}

function IconButton({
  onClick,
  label,
  children,
}: {
  onClick: () => void;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="flex h-11 w-11 cursor-pointer items-center justify-center rounded-full transition-transform active:scale-95"
      style={{ background: "var(--surface-high)", color: "var(--on-surface)" }}
    >
      {children}
    </button>
  );
}

function ColumnHeaders({ madhabLabel }: { madhabLabel: string }) {
  return (
    <div
      className="mx-3.5 flex items-center rounded-2xl px-2 py-2.5"
      style={{
        background: "linear-gradient(90deg, #0B7A5B, #19B58A)",
        boxShadow: "0 8px 20px rgba(11,122,91,0.35)",
      }}
    >
      {LABELS.map((label, i) => (
        <div
          key={label}
          style={{ width: `${(COLUMN_WEIGHTS[i] / 7.4) * 100}%` }}
          className={i === 0 ? "text-left" : "text-center"}
        >
          <span className="text-[14px] font-medium whitespace-nowrap text-white">{label}</span>
          {label === "Asr" && (
            <span className="block text-[11px] whitespace-nowrap text-white/80">{madhabLabel}</span>
          )}
        </div>
      ))}
    </div>
  );
}

function MonthDayList({
  days,
  today,
  yearMonth,
  use24,
  madhab,
  hijriAdjustment,
}: {
  days: PrayerDay[];
  today: Date;
  yearMonth: { year: number; month: number };
  use24: boolean;
  madhab: AsrMadhab;
  hijriAdjustment: number;
}) {
  const listRef = useRef<HTMLDivElement>(null);
  const isCurrentMonth =
    yearMonth.year === today.getFullYear() && yearMonth.month === today.getMonth() + 1;

  useEffect(() => {
    if (isCurrentMonth && listRef.current && days.length > 0) {
      const el = listRef.current.querySelector<HTMLElement>(
        `[data-day="${today.getDate()}"]`
      );
      if (el) listRef.current.scrollTop = Math.max(0, el.offsetTop - 8);
    }
  }, [isCurrentMonth, today, days.length]);

  return (
    <div ref={listRef} className="nice-scroll relative h-full overflow-y-auto px-3.5 pt-2 pb-5">
      <div className="flex flex-col gap-[2px]">
        {days.map((day, index) => (
          <div key={day.day} data-day={day.day}>
            <DayRow
              day={day}
              isToday={
                day.year === today.getFullYear() &&
                day.month === today.getMonth() + 1 &&
                day.day === today.getDate()
              }
              use24={use24}
              madhab={madhab}
              hijriAdjustment={hijriAdjustment}
              striped={index % 2 === 1}
              animate={index < 14}
              index={index}
            />
          </div>
        ))}
      </div>
    </div>
  );
}

function DayRow({
  day,
  isToday,
  use24,
  madhab,
  hijriAdjustment,
  striped,
  animate,
  index,
}: {
  day: PrayerDay;
  isToday: boolean;
  use24: boolean;
  madhab: AsrMadhab;
  hijriAdjustment: number;
  striped: boolean;
  animate: boolean;
  index: number;
}) {
  const date = new Date(day.year, day.month - 1, day.day);
  const isFriday = date.getDay() === 5;
  const fg = isToday ? "#FFFFFF" : "var(--on-surface)";
  const fgMuted = isToday ? "rgba(255,255,255,0.8)" : "var(--on-surface-variant)";
  const hijri: HijriDate = hijriFrom(date, hijriAdjustment);

  const bg: React.CSSProperties = isToday
    ? {
        background: "linear-gradient(90deg, #0B7A5B, #19B58A)",
        boxShadow: "0 10px 22px rgba(11,122,91,0.45)",
      }
    : isFriday
      ? {
          background: "color-mix(in srgb, var(--gold-container) 60%, transparent)",
          border: "1px solid color-mix(in srgb, var(--gold) 35%, transparent)",
        }
      : striped
        ? { background: "var(--surface-low)" }
        : {};

  const weekday = new Intl.DateTimeFormat("en", { weekday: "short" }).format(date);

  const row = (
    <div
      className="flex items-center rounded-[14px] px-2 py-2"
      style={bg}
    >
      <div style={{ width: `${(COLUMN_WEIGHTS[0] / 7.4) * 100}%` }} className="min-w-0">
        <div className="flex items-center">
          <span
            className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[14px] font-bold"
            style={{
              background: isToday
                ? "rgba(255,255,255,0.22)"
                : isFriday
                  ? "color-mix(in srgb, var(--gold) 25%, transparent)"
                  : "var(--surface-highest)",
              color: isFriday && !isToday ? "var(--on-gold-container)" : fg,
            }}
          >
            {day.day}
          </span>
          <span className="ml-1.5 truncate text-[13px] font-medium" style={{ color: isFriday && !isToday ? "var(--on-gold-container)" : fgMuted }}>
            {weekday}
          </span>
        </div>
        <p className="mt-0.5 pl-0.5 text-[11px] tracking-[0.2px]" style={{ color: fgMuted }}>
          {hijri.day} {hijriMonthShort(hijri)}
        </p>
      </div>
      {PRAYERS.map((p, i) => {
        const minutes = timeOf(day, p.id, madhab);
        return (
          <span
            key={p.id}
            style={{
              width: `${(COLUMN_WEIGHTS[i + 1] / 7.4) * 100}%`,
              color: p.id === "SUNRISE" ? fgMuted : fg,
              fontWeight: isToday ? 600 : 500,
            }}
            className="text-center text-[15px] whitespace-nowrap"
          >
            {timeNumber(minutes, use24)}
          </span>
        );
      })}
    </div>
  );

  if (!animate) return row;
  return <Stagger index={index} staggerMs={26} baseDelayMs={60}>{row}</Stagger>;
}
