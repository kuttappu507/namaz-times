"use client";

/**
 * Web notification scheduling — the PWA counterpart of PrayerAlarmScheduler.kt.
 * While the app is open (or installed and running), a notification fires when a
 * prayer begins and optionally a pre-reminder. Re-armed every minute and whenever
 * relevant settings change.
 */
import { useEffect, useRef } from "react";
import { dayFor, eventsFor, prayerName, timeOf } from "./prayers";
import { amPm, timeNumber } from "./time";
import { isNotifyOn, type UserSettings } from "./settings";

export async function requestNotificationPermission(): Promise<NotificationPermission> {
  if (typeof window === "undefined" || !("Notification" in window)) return "denied";
  if (Notification.permission === "granted") return "granted";
  try {
    return await Notification.requestPermission();
  } catch {
    return "denied";
  }
}

export function notifySupported(): boolean {
  return typeof window !== "undefined" && "Notification" in window;
}

interface FireArgs {
  title: string;
  body: string;
  tag: string;
  vibrate: boolean;
}

function fire(args: FireArgs) {
  try {
    if (args.vibrate && "vibrate" in navigator) navigator.vibrate([220, 120, 220]);
    new Notification(args.title, {
      body: args.body,
      tag: args.tag,
      icon: "/icons/icon-192.png",
      badge: "/icons/icon-192.png",
    });
  } catch {
    // Notification constructor can throw on some platforms — ignore.
  }
}

function minuteKeyOf(d: Date): string {
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}-${d.getHours()}-${d.getMinutes()}`;
}

export function usePrayerNotifications(settings: UserSettings, now: Date) {
  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);

  const armedKey = minuteKeyOf(now);
  const notifDeps = [
    settings.notificationsEnabled,
    JSON.stringify(settings.notifyFor),
    settings.reminderMinutes,
    settings.madhab,
    settings.vibrate,
  ];

  useEffect(() => {
    if (!notifySupported()) return;
    if (!settings.notificationsEnabled || Notification.permission !== "granted") return;

    const s = settings;
    const madhab = s.madhab;

    timers.current.forEach(clearTimeout);
    timers.current = [];

    const scheduleWindow = () => {
      for (let offset = 0; offset < 2; offset++) {
        const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() + offset);
        const day = dayFor(d);
        for (const event of eventsFor(day, madhab)) {
          if (!isNotifyOn(s, event.prayer)) continue;
          const minutes = timeOf(day, event.prayer, madhab);
          const timeLabel = `${timeNumber(minutes, s.use24Hour)} ${amPm(minutes, s.use24Hour)}`.trim();
          const name = prayerName(event.prayer, d);

          const delay = event.at.getTime() - now.getTime();
          if (delay > 0 && delay <= 24 * 3600_000) {
            timers.current.push(
              setTimeout(fire, delay, {
                title: `${name} · Hubli`,
                body: `It's time for ${name} — ${timeLabel}`,
                tag: `prayer-${event.prayer}-${d.toDateString()}`,
                vibrate: s.vibrate,
              })
            );
          }
          if (s.reminderMinutes > 0) {
            const rDelay = delay - s.reminderMinutes * 60_000;
            if (rDelay > 0 && rDelay <= 24 * 3600_000) {
              timers.current.push(
                setTimeout(fire, rDelay, {
                  title: `${name} in ${s.reminderMinutes} minutes`,
                  body: `${name} begins at ${timeLabel}`,
                  tag: `reminder-${event.prayer}-${d.toDateString()}`,
                  vibrate: s.vibrate,
                })
              );
            }
          }
        }
      }
    };

    scheduleWindow();
    return () => timers.current.forEach(clearTimeout);
  }, [armedKey, ...notifDeps]);
}
