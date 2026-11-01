"use client";

/** Everything the user can configure — mirrors UserSettings.kt, persisted in localStorage. */
import { useCallback, useState } from "react";
import { PRAYERS, type AsrMadhab, type PrayerId } from "./prayers";

export type ThemeMode = "SYSTEM" | "LIGHT" | "DARK";

export interface UserSettings {
  madhab: AsrMadhab;
  use24Hour: boolean;
  themeMode: ThemeMode;
  notificationsEnabled: boolean;
  notifyFor: Record<PrayerId, boolean>;
  reminderMinutes: number;
  vibrate: boolean;
  hijriAdjustment: number;
}

export const DEFAULT_SETTINGS: UserSettings = {
  madhab: "HANAFI",
  use24Hour: false,
  themeMode: "DARK",
  notificationsEnabled: true,
  notifyFor: PRAYERS.reduce(
    (acc, p) => ({ ...acc, [p.id]: p.isSalah }),
    {} as Record<PrayerId, boolean>
  ),
  reminderMinutes: 0,
  vibrate: true,
  hijriAdjustment: 0,
};

const KEY = "namaz-hubli-settings-v1";

export function isNotifyOn(s: UserSettings, prayer: PrayerId): boolean {
  return s.notificationsEnabled && (s.notifyFor[prayer] ?? false);
}

/** Load settings once (client only), merged over defaults so new keys survive upgrades. */
function load(): UserSettings {
  if (typeof window === "undefined") return DEFAULT_SETTINGS;
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return DEFAULT_SETTINGS;
    const parsed = JSON.parse(raw) as Partial<UserSettings>;
    return {
      ...DEFAULT_SETTINGS,
      ...parsed,
      notifyFor: { ...DEFAULT_SETTINGS.notifyFor, ...(parsed.notifyFor ?? {}) },
    };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

export function useSettings(): {
  settings: UserSettings;
  update: (patch: Partial<UserSettings>) => void;
  setNotify: (prayer: PrayerId, value: boolean) => void;
} {
  // Client-only app shell mounts after hydration, so a lazy initializer is safe.
  const [settings, setSettings] = useState<UserSettings>(() =>
    typeof window === "undefined" ? DEFAULT_SETTINGS : load()
  );

  const update = useCallback((patch: Partial<UserSettings>) => {
    setSettings((prev) => {
      const next = { ...prev, ...patch };
      try {
        window.localStorage.setItem(KEY, JSON.stringify(next));
      } catch {
        // storage unavailable (private mode) — keep in memory only
      }
      return next;
    });
  }, []);

  const setNotify = useCallback((prayer: PrayerId, value: boolean) => {
    setSettings((prev) => {
      const next = { ...prev, notifyFor: { ...prev.notifyFor, [prayer]: value } };
      try {
        window.localStorage.setItem(KEY, JSON.stringify(next));
      } catch {
        // ignore
      }
      return next;
    });
  }, []);

  return { settings, update, setNotify };
}
