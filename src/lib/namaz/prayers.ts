import { TIMETABLE, type TimetableRow } from "./timetable-data";

/**
 * The six daily events shown by the app. SUNRISE is not a prayer, but it marks the
 * end of Fajr and is printed in every timetable, so it is modelled alongside the salah.
 */
export type PrayerId = "FAJR" | "SUNRISE" | "ZUHR" | "ASR" | "MAGHRIB" | "ISHA";

export interface PrayerDef {
  id: PrayerId;
  displayName: string;
  arabicName: string;
  isSalah: boolean;
}

export const PRAYERS: PrayerDef[] = [
  { id: "FAJR", displayName: "Fajr", arabicName: "الفجر", isSalah: true },
  { id: "SUNRISE", displayName: "Sunrise", arabicName: "الشروق", isSalah: false },
  { id: "ZUHR", displayName: "Zuhr", arabicName: "الظهر", isSalah: true },
  { id: "ASR", displayName: "Asr", arabicName: "العصر", isSalah: true },
  { id: "MAGHRIB", displayName: "Maghrib", arabicName: "المغرب", isSalah: true },
  { id: "ISHA", displayName: "Isha", arabicName: "العشاء", isSalah: true },
];

export const prayerDef = (id: PrayerId): PrayerDef =>
  PRAYERS.find((p) => p.id === id)!;

/** School of thought used for the Asr time. Both columns are present in the timetable. */
export type AsrMadhab = "SHAFI" | "HANAFI";

export const MADHAB_LABEL: Record<AsrMadhab, string> = {
  SHAFI: "Shafi'i",
  HANAFI: "Hanafi",
};

export const MADHAB_DESCRIPTION: Record<AsrMadhab, string> = {
  SHAFI: "Shadow equals object length (also Maliki, Hanbali)",
  HANAFI: "Shadow equals twice the object length",
};

/** One day's timings, each column in minutes since local midnight. */
export interface PrayerDay {
  year: number;
  month: number; // 1-12
  day: number; // 1-31
  fajr: number;
  sunrise: number;
  zuhr: number;
  asrShafi: number;
  asrHanafi: number;
  maghrib: number;
  isha: number;
}

export const SLOTS = 12 * 31;
const slotOf = (month: number, day: number) => (month - 1) * 31 + (day - 1);

// Index the printed rows by slot (first printed row for a slot wins, as in Timetable.kt).
const PRINTED = new Map<number, TimetableRow>();
for (const row of TIMETABLE) {
  const slot = slotOf(row[0], row[1]);
  if (!PRINTED.has(slot)) PRINTED.set(slot, row);
}

const dayCache = new Map<number, PrayerDay>();

/**
 * Prayer times for a date. An unprinted date carries forward the previous printed
 * day's times (wrapping from January 1st to December 31st) exactly as the paper
 * sheet is read — no interpolation or rounding.
 */
export function dayFor(date: Date): PrayerDay {
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const slot = slotOf(month, day);
  const cached = dayCache.get(slot);
  if (cached) {
    return cached.year === date.getFullYear() && cached.month === month && cached.day === day
      ? cached
      : { ...cached, year: date.getFullYear(), month, day };
  }
  let s = slot;
  let steps = 0;
  while (!PRINTED.has(s) && steps < SLOTS) {
    s = (s - 1 + SLOTS) % SLOTS;
    steps++;
  }
  const row = PRINTED.get(s);
  if (!row) throw new Error("Timetable has no printed rows");
  const result: PrayerDay = {
    year: date.getFullYear(),
    month,
    day,
    fajr: row[2],
    sunrise: row[3],
    zuhr: row[4],
    asrShafi: row[5],
    asrHanafi: row[6],
    maghrib: row[7],
    isha: row[8],
  };
  dayCache.set(slot, result);
  return result;
}

export function timeOf(d: PrayerDay, prayer: PrayerId, madhab: AsrMadhab): number {
  switch (prayer) {
    case "FAJR": return d.fajr;
    case "SUNRISE": return d.sunrise;
    case "ZUHR": return d.zuhr;
    case "ASR": return madhab === "HANAFI" ? d.asrHanafi : d.asrShafi;
    case "MAGHRIB": return d.maghrib;
    case "ISHA": return d.isha;
  }
}

/** Zuhr on a Friday is Jumu'ah. */
export function isJumuah(prayer: PrayerId, date: Date): boolean {
  return prayer === "ZUHR" && date.getDay() === 5;
}

export function prayerName(prayer: PrayerId, date: Date): string {
  return isJumuah(prayer, date) ? "Jumu'ah" : prayerDef(prayer).displayName;
}

/** A concrete prayer at a concrete moment. */
export interface PrayerEvent {
  prayer: PrayerId;
  at: Date;
}

/** All six events for this day, in chronological order. */
export function eventsFor(d: PrayerDay, madhab: AsrMadhab): PrayerEvent[] {
  return PRAYERS.map((p) => {
    const minutes = timeOf(d, p.id, madhab);
    return {
      prayer: p.id,
      at: new Date(d.year, d.month - 1, d.day, Math.floor(minutes / 60), minutes % 60, 0, 0),
    };
  });
}

/**
 * The first prayer event strictly after `now`, looking across midnight into the
 * following days (so after Isha, "next" is tomorrow's Fajr). Sunrise is skipped
 * unless includeSunrise — same as PrayerRepository.kt.
 */
export function nextEvent(now: Date, madhab: AsrMadhab, includeSunrise = false): PrayerEvent {
  for (let offset = 0; offset < 3; offset++) {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset);
    const events = eventsFor(dayFor(d), madhab).filter(
      (e) => (includeSunrise || e.prayer !== "SUNRISE") && e.at.getTime() > now.getTime()
    );
    if (events.length > 0) return events[0];
  }
  // Unreachable in practice: every day has five prayers.
  const fallback = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 3, 5, 0, 0, 0);
  return { prayer: "FAJR", at: fallback };
}

/** The most recent prayer event at or before `now`. */
export function currentEvent(now: Date, madhab: AsrMadhab, includeSunrise = false): PrayerEvent {
  for (let offset = 0; offset >= -3; offset--) {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset);
    const events = eventsFor(dayFor(d), madhab).filter(
      (e) => (includeSunrise || e.prayer !== "SUNRISE") && e.at.getTime() <= now.getTime()
    );
    if (events.length > 0) return events[events.length - 1];
  }
  // Unreachable in practice.
  return { prayer: "ISHA", at: new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0) };
}

/** Every real calendar day of a month, in order. */
export function monthDays(year: number, month: number): PrayerDay[] {
  const last = new Date(year, month, 0).getDate(); // month is 1-12
  const days: PrayerDay[] = [];
  for (let d = 1; d <= last; d++) {
    days.push(dayFor(new Date(year, month - 1, d)));
  }
  return days;
}
