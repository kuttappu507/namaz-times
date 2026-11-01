/**
 * Islamic (Hijri) date derived from the browser's built-in Umm al-Qura calendar
 * (Intl islamic-umalqura — the same calendar used by java.time's HijrahChronology
 * on Android), with a user-adjustable offset to match local moon sighting.
 */

export interface HijriDate {
  day: number;
  month: number; // 1..12
  year: number;
}

export const HIJRI_MONTHS = [
  "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
  "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
  "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah",
];

export const HIJRI_MONTHS_SHORT = [
  "Muh", "Saf", "Rab I", "Rab II", "Jum I", "Jum II",
  "Raj", "Sha", "Ram", "Shw", "DhQ", "DhH",
];

export const HIJRI_MONTHS_AR = [
  "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
  "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
  "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
];

export function hijriMonthName(h: HijriDate): string {
  return HIJRI_MONTHS[h.month - 1] ?? "";
}
export function hijriMonthShort(h: HijriDate): string {
  return HIJRI_MONTHS_SHORT[h.month - 1] ?? "";
}
export function hijriMonthArabic(h: HijriDate): string {
  return HIJRI_MONTHS_AR[h.month - 1] ?? "";
}
export function hijriFormatted(h: HijriDate): string {
  return `${h.day} ${hijriMonthName(h)} ${h.year} AH`;
}

// Try the Umm al-Qura calendar first, then fall back to the generic Islamic ones.
let formatter: Intl.DateTimeFormat | null = null;
for (const cal of ["islamic-umalqura", "islamic-civil", "islamic"]) {
  try {
    const f = new Intl.DateTimeFormat(`en-u-ca-${cal}-nu-latn`, {
      day: "numeric",
      month: "numeric",
      year: "numeric",
      timeZone: "UTC",
    });
    // Probe: some engines silently fall back instead of throwing — check the
    // resolved calendar actually matches what we asked for when possible.
    formatter = f;
    const resolved = (f as Intl.DateTimeFormat & { resolvedOptions: () => { calendar: string } })
      .resolvedOptions().calendar;
    if (resolved === cal) break;
  } catch {
    // try next calendar
  }
}

const padCache = new Map<string, HijriDate>();

export function hijriFrom(date: Date, adjustmentDays = 0): HijriDate {
  const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}-${adjustmentDays}`;
  const cached = padCache.get(key);
  if (cached) return cached;

  const adjusted = new Date(date.getFullYear(), date.getMonth(), date.getDate() + adjustmentDays);
  let result: HijriDate;
  if (formatter) {
    // Format at local-noon-in-UTC to avoid DST edge cases: use the UTC midnight of
    // the adjusted date by shifting by the local-to-UTC offset.
    const utcMs = adjusted.getTime() - adjusted.getTimezoneOffset() * 60_000;
    const parts = formatter.formatToParts(new Date(utcMs));
    const get = (t: string) => parseInt(parts.find((p) => p.type === t)?.value ?? "0", 10);
    result = { day: get("day"), month: get("month"), year: get("year") };
  } else {
    // Last-resort arithmetic estimate (≈ tabular Islamic calendar, 8-year leap cycle).
    const days = Math.floor(adjusted.getTime() / 86_400_000) + 10_530;
    const years = Math.floor(days / 354.367);
    let dayOfYear = Math.floor(days - Math.floor(years * 354.367));
    const monthLengths = [30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29];
    let month = 0;
    while (month < 12 && dayOfYear > monthLengths[month]) {
      dayOfYear -= monthLengths[month];
      month++;
    }
    result = { day: Math.max(1, dayOfYear), month: Math.min(12, month + 1), year: years + 1 };
  }
  padCache.set(key, result);
  return result;
}
