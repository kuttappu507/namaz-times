"use client";

/** 1 Hz ticker + derived "Today" state — mirrors AppViewModel.kt's todayState flow. */
import { useEffect, useMemo, useState } from "react";
import {
  currentEvent,
  dayFor,
  nextEvent,
  type AsrMadhab,
  type PrayerEvent,
  type PrayerDay,
} from "./prayers";
import { hijriFrom, hijriFormatted, hijriMonthArabic, type HijriDate } from "./hijri";
import type { UserSettings } from "./settings";

export interface TodayState {
  now: Date;
  today: PrayerDay;
  hijri: HijriDate;
  hijriLabel: string;
  current: PrayerEvent; // includes Sunrise ("Fajr has ended")
  next: PrayerEvent;
  timeToNextMs: number;
  /** 0..1 — how far we are through the current prayer window (between two salah). */
  progress: number;
}

export function useNow(intervalMs = 1000): Date {
  const [now, setNow] = useState<Date>(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}

export function useTodayState(now: Date, madhab: AsrMadhab, hijriAdjustment: number): TodayState {
  return useMemo(() => {
    const today = dayFor(now);
    const current = currentEvent(now, madhab, true);
    const currentSalah = currentEvent(now, madhab, false);
    const next = nextEvent(now, madhab, false);
    const windowMs = Math.max(1000, next.at.getTime() - currentSalah.at.getTime());
    const elapsedMs = Math.min(windowMs, Math.max(0, now.getTime() - currentSalah.at.getTime()));
    const hijri = hijriFrom(now, hijriAdjustment);
    return {
      now,
      today,
      hijri,
      hijriLabel: `${hijriFormatted(hijri)}  ·  ${hijriMonthArabic(hijri)}`,
      current,
      next,
      timeToNextMs: Math.max(0, next.at.getTime() - now.getTime()),
      progress: elapsedMs / windowMs,
    };
  }, [now, madhab, hijriAdjustment]);
}
