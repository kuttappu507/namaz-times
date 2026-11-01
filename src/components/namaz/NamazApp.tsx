"use client";

/**
 * Root app shell — the web counterpart of MainActivity.kt:
 * animated splash, background wash, kept-alive swipeable pager with the three
 * screens, floating pill navigation, PWA install prompt and prayer alerts.
 */
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useTheme } from "next-themes";
import { Splash } from "./Splash";
import { TodayScreen } from "./TodayScreen";
import { MonthlyScreen } from "./MonthlyScreen";
import { SettingsScreen } from "./SettingsScreen";
import { FloatingNavBar } from "./FloatingNavBar";
import { InstallSheet, isStandalone, useInstallPrompt } from "./InstallPrompt";
import { useSettings } from "@/lib/namaz/settings";
import { useNow, useTodayState } from "@/lib/namaz/use-today-state";
import { requestNotificationPermission, usePrayerNotifications } from "@/lib/namaz/notify";
import { SWRegister } from "./SWRegister";

const SPLASH_KEY = "namaz-splash-seen";
const emptySubscribe = () => () => {};

export default function NamazApp() {
  const { settings, update, setNotify } = useSettings();
  const now = useNow(1000);
  const state = useTodayState(now, settings.madhab, settings.hijriAdjustment);
  const { setTheme } = useTheme();
  const [tab, setTab] = useState(0);
  // Client-only rendering (dates and storage are device-local).
  const mounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );
  // Splash plays once per browser session (≈ once per cold start on Android).
  const [splashDone, setSplashDone] = useState(
    () => typeof window !== "undefined" && sessionStorage.getItem(SPLASH_KEY) === "1"
  );

  const install = useInstallPrompt();

  // Theme mode follows the settings (System / Light / Dark).
  useEffect(() => {
    setTheme(settings.themeMode.toLowerCase());
  }, [settings.themeMode, setTheme]);
  const finishSplash = useCallback(() => {
    setSplashDone(true);
    try {
      sessionStorage.setItem(SPLASH_KEY, "1");
    } catch {}
  }, []);

  // PWA prompt a couple of seconds after each visit.
  useEffect(() => {
    const cleanup = install.maybeShowOnVisit();
    return typeof cleanup === "function" ? cleanup : undefined;
  }, [install.maybeShowOnVisit]);

  // Prayer alerts while the app is open / installed.
  usePrayerNotifications(settings, now);

  const handleUpdate = useCallback(
    (patch: Parameters<typeof update>[0]) => {
      if (patch.notificationsEnabled) void requestNotificationPermission();
      update(patch);
    },
    [update]
  );

  if (!mounted) {
    // Pre-hydration shell: just the wash and a tiny crescent placeholder.
    return (
      <div className="relative h-[100dvh] w-full overflow-hidden" style={{ background: "var(--background)" }}>
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              "radial-gradient(circle at 12% 2%, color-mix(in srgb, var(--primary) 16%, transparent), transparent 55%)," +
              "radial-gradient(circle at 100% 48%, color-mix(in srgb, var(--secondary) 12%, transparent), transparent 50%)," +
              "radial-gradient(circle at 25% 102%, color-mix(in srgb, var(--primary) 11%, transparent), transparent 55%)",
          }}
          aria-hidden
        />
        <div className="flex h-full items-center justify-center" aria-busy="true">
          <div
            className="breathe h-12 w-12 rounded-full"
            style={{
              background: "linear-gradient(135deg, #F5C451, #B8891A)",
              clipPath:
                "path('M 24 2 A 22 22 0 1 0 24 46 A 17.5 17.5 0 1 1 24 2 Z')",
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="relative h-[100dvh] w-full overflow-hidden" style={{ background: "var(--background)" }}>
      {/* Soft colour wash behind everything (BackgroundWash) */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(circle at 12% 2%, color-mix(in srgb, var(--primary) 16%, transparent), transparent 55%)," +
            "radial-gradient(circle at 100% 48%, color-mix(in srgb, var(--secondary) 12%, transparent), transparent 50%)," +
            "radial-gradient(circle at 25% 102%, color-mix(in srgb, var(--primary) 11%, transparent), transparent 55%)",
        }}
        aria-hidden
      />

      <Pager tab={tab} onTab={setTab}>
        <TodayScreen state={state} settings={settings} onOpenSettings={() => setTab(2)} />
        <MonthlyScreen settings={settings} today={new Date()} />
        <SettingsScreen
          settings={settings}
          today={now}
          update={handleUpdate}
          setNotify={setNotify}
          installSupported
          onInstall={install.promptInstall}
        />
      </Pager>

      <FloatingNavBar selected={tab} onSelect={setTab} />

      <InstallSheet
        open={install.sheetOpen}
        platform={install.platform}
        canPrompt={install.canPrompt}
        onDismiss={install.dismiss}
        onInstall={install.promptInstall}
      />

      {!splashDone && <Splash onFinished={finishSplash} />}
      <SWRegister />
    </div>
  );
}

/* ------------------------------ Pager ------------------------------ */

function Pager({
  tab,
  onTab,
  children,
}: {
  tab: number;
  onTab: (t: number) => void;
  children: React.ReactNode[];
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(1);
  const [dragX, setDragX] = useState(0);
  const touch = useRef<{ x: number; y: number; locked: "h" | "v" | null } | null>(null);

  useEffect(() => {
    const measure = () => setWidth(containerRef.current?.clientWidth ?? 1);
    measure();
    window.addEventListener("resize", measure);
    return () => window.removeEventListener("resize", measure);
  }, []);

  const onTouchStart = (e: React.TouchEvent) => {
    const t = e.touches[0];
    touch.current = { x: t.clientX, y: t.clientY, locked: null };
  };
  const onTouchMove = (e: React.TouchEvent) => {
    const st = touch.current;
    if (!st) return;
    const t = e.touches[0];
    const dx = t.clientX - st.x;
    const dy = t.clientY - st.y;
    if (!st.locked) {
      if (Math.abs(dx) < 12 && Math.abs(dy) < 12) return;
      st.locked = Math.abs(dx) > Math.abs(dy) ? "h" : "v";
    }
    if (st.locked === "h") {
      const atEdge = (tab === 0 && dx > 0) || (tab === children.length - 1 && dx < 0);
      setDragX(atEdge ? dx * 0.35 : dx);
    }
  };
  const onTouchEnd = () => {
    const st = touch.current;
    touch.current = null;
    if (st?.locked === "h") {
      if (Math.abs(dragX) > width * 0.18) {
        const next = Math.max(0, Math.min(children.length - 1, tab - Math.sign(dragX)));
        onTab(next);
      }
    }
    setDragX(0);
  };

  const dragging = dragX !== 0;
  const dragFrac = dragging ? dragX / width : 0;

  return (
    <div
      ref={containerRef}
      className="absolute inset-0 overflow-hidden"
      onTouchStart={onTouchStart}
      onTouchMove={onTouchMove}
      onTouchEnd={onTouchEnd}
      onTouchCancel={onTouchEnd}
    >
      {children.map((child, i) => {
        const d = Math.min(1, Math.max(0, Math.abs(i - tab - dragFrac)));
        const active = d < 0.5;
        return (
          <div
            key={i}
            className="absolute inset-0 flex h-full flex-col"
            style={{
              transform: `translateX(calc(${(i - tab) * 100}% + ${dragX}px)) scale(${1 - 0.05 * d})`,
              transition: dragging ? "none" : "transform 0.34s cubic-bezier(0.22, 1, 0.36, 1)",
              paddingTop: "env(safe-area-inset-top, 0px)",
              paddingBottom: "calc(env(safe-area-inset-bottom, 0px) + 88px)",
            }}
            aria-hidden={!active}
          >
            {/* Dim the page in proportion to how far off-centre it is */}
            <div className="relative min-h-0 flex-1" style={{ opacity: active ? 1 : 0 }}>
              {child}
            </div>
            <div
              className="pointer-events-none absolute inset-0 transition-opacity duration-200"
              style={{ background: "var(--background)", opacity: 0.55 * d }}
              aria-hidden
            />
          </div>
        );
      })}
    </div>
  );
}
