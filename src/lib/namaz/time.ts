/** 12h/24h + countdown formatting — mirrors TimeFormat.kt. */
import { MADHAB_LABEL, type AsrMadhab } from "./prayers";

/** The numeric part only – "5:38" / "05:38" – for layouts that render AM/PM separately. */
export function timeNumber(minutes: number, use24: boolean): string {
  const h24 = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (use24) {
    return `${String(h24).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
  }
  const h = h24 % 12 === 0 ? 12 : h24 % 12;
  return `${h}:${String(m).padStart(2, "0")}`;
}

/** "AM" / "PM" or empty in 24-hour mode. */
export function amPm(minutes: number, use24: boolean): string {
  if (use24) return "";
  return Math.floor(minutes / 60) < 12 ? "AM" : "PM";
}

/** "01:23:45" style countdown from milliseconds. */
export function countdown(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return [h, m, s].map((v) => String(v).padStart(2, "0")).join(":");
}

/** "EEEE, d MMMM" — e.g. "Sunday, 1 November" (matches the native app's date format). */
const longDateParts = new Intl.DateTimeFormat("en", {
  weekday: "long",
  day: "numeric",
  month: "long",
});

export function formatLongDate(date: Date): string {
  const parts = longDateParts.formatToParts(date);
  const get = (t: string) => parts.find((p) => p.type === t)?.value ?? "";
  return `${get("weekday")}, ${get("day")} ${get("month")}`;
}

/** Greeting for the header, per hour of day. */
export function greetingFor(hour: number): string {
  if (hour >= 4 && hour <= 11) return "Good morning";
  if (hour >= 12 && hour <= 16) return "Good afternoon";
  if (hour >= 17 && hour <= 20) return "Good evening";
  return "Good night";
}

export const CURRENT_LABEL_DEFAULT = (name: string) => `Now · ${name}`;

export function footerNote(madhab: AsrMadhab): string {
  return `Asr per ${MADHAB_LABEL[madhab]} school · change in Settings`;
}
