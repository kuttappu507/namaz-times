"use client";

/**
 * PWA install experience.
 *  · Android / desktop Chrome — captures `beforeinstallprompt` and shows a custom
 *    bottom sheet with a native "Install" button.
 *  · iPhone / iPad Safari — shows "Add to Home Screen" instructions (iOS has no
 *    programmatic install prompt).
 * The sheet appears a couple of seconds after each visit until the app is installed.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Download, Share, X } from "lucide-react";
import { Pressable } from "./effects";

export type InstallPlatform = "android" | "ios" | "desktop" | "unsupported";

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

const DISMISS_KEY = "namaz-install-dismissed-session";

export function isStandalone(): boolean {
  if (typeof window === "undefined") return false;
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    window.matchMedia("(display-mode: fullscreen)").matches ||
    // iOS Safari
    (window.navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

export function detectPlatform(): InstallPlatform {
  if (typeof window === "undefined") return "unsupported";
  const ua = window.navigator.userAgent;
  const isIOS =
    /iPad|iPhone|iPod/.test(ua) ||
    // iPadOS 13+ requests desktop UA
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  if (isIOS) return "ios";
  if (/Android/.test(ua)) return "android";
  return "desktop";
}

export function useInstallPrompt() {
  // Client-only shell mounts after hydration, so lazy browser probes are safe.
  const [deferred, setDeferred] = useState<BeforeInstallPromptEvent | null>(null);
  const [platform, setPlatform] = useState<InstallPlatform>(() =>
    typeof window === "undefined" ? "unsupported" : detectPlatform()
  );
  const [installed, setInstalled] = useState(() =>
    typeof window === "undefined" ? false : isStandalone()
  );
  const [sheetOpen, setSheetOpen] = useState(false);

  useEffect(() => {
    const onBip = (e: Event) => {
      e.preventDefault();
      setDeferred(e as BeforeInstallPromptEvent);
    };
    const onInstalled = () => {
      setInstalled(true);
      setSheetOpen(false);
      setDeferred(null);
      try {
        sessionStorage.removeItem(DISMISS_KEY);
      } catch {}
    };
    window.addEventListener("beforeinstallprompt", onBip);
    window.addEventListener("appinstalled", onInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", onBip);
      window.removeEventListener("appinstalled", onInstalled);
    };
  }, []);

  const autoEligible = useMemo(() => {
    if (installed) return false;
    if (platform === "ios") return true;
    return deferred !== null;
  }, [installed, platform, deferred]);

  const maybeShowOnVisit = useCallback(() => {
    if (!autoEligible) return;
    try {
      if (sessionStorage.getItem(DISMISS_KEY) === "1") return;
    } catch {}
    const demo = typeof window !== "undefined" && new URLSearchParams(window.location.search).has("__install");
    const delay = demo ? 600 : 2600;
    const t = setTimeout(() => setSheetOpen(true), delay);
    return () => clearTimeout(t);
  }, [autoEligible]);

  const dismiss = useCallback(() => {
    setSheetOpen(false);
    try {
      sessionStorage.setItem(DISMISS_KEY, "1");
    } catch {}
  }, []);

  const promptInstall = useCallback(async () => {
    if (deferred) {
      await deferred.prompt();
      const choice = await deferred.userChoice;
      if (choice.outcome === "accepted") setInstalled(true);
      setDeferred(null);
      setSheetOpen(false);
      return;
    }
    // No native prompt available — open the sheet (contains iOS/manual guidance).
    setSheetOpen(true);
  }, [deferred]);

  const openSheet = useCallback(() => setSheetOpen(true), []);

  return {
    platform,
    installed,
    canPrompt: deferred !== null,
    sheetOpen,
    maybeShowOnVisit,
    dismiss,
    promptInstall,
    openSheet,
    setSheetOpen,
  };
}

export function InstallSheet({
  open,
  platform,
  canPrompt,
  onDismiss,
  onInstall,
}: {
  open: boolean;
  platform: InstallPlatform;
  canPrompt: boolean;
  onDismiss: () => void;
  onInstall: () => void;
}) {
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            className="fixed inset-0 z-[70]"
            style={{ background: "rgba(4, 12, 9, 0.55)", backdropFilter: "blur(2px)" }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onDismiss}
            aria-hidden
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="Install Namaz Times"
            className="fixed inset-x-0 bottom-0 z-[75] mx-auto w-full max-w-[480px] px-3 pb-[max(env(safe-area-inset-bottom),12px)]"
            initial={{ y: 340, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: 340, opacity: 0 }}
            transition={{ type: "spring", stiffness: 320, damping: 32 }}
          >
            <div
              className="relative overflow-hidden rounded-[28px] p-5"
              style={{
                background: "linear-gradient(160deg, var(--card-grad-start), var(--card-grad-end))",
                border: "1px solid color-mix(in srgb, white 60%, var(--outline-variant))",
                boxShadow: "0 24px 60px rgba(4, 20, 14, 0.45)",
              }}
            >
              <button
                type="button"
                aria-label="Dismiss"
                onClick={onDismiss}
                className="absolute top-3.5 right-3.5 flex h-8 w-8 cursor-pointer items-center justify-center rounded-full"
                style={{ background: "var(--surface-highest)", color: "var(--on-surface-variant)" }}
              >
                <X size={16} />
              </button>

              <div className="flex items-center gap-3.5">
                {/* App icon */}
                <img
                  src="/icons/icon-192.png"
                  alt="Namaz Times icon"
                  width={56}
                  height={56}
                  className="rounded-[14px]"
                  style={{ boxShadow: "0 8px 20px rgba(11,122,91,0.35)" }}
                />
                <div>
                  <p className="text-[20px] leading-7 font-semibold" style={{ color: "var(--on-surface)" }}>
                    Install Namaz Times
                  </p>
                  <p className="text-[14px]" style={{ color: "var(--on-surface-variant)" }}>
                    Namaz Times · Hubli — offline timetable
                  </p>
                </div>
              </div>

              <p className="mt-3 text-[16px] leading-6" style={{ color: "var(--on-surface-variant)" }}>
                Add the app to your home screen for a full-screen experience, live prayer alerts
                and instant offline access.
              </p>

              {platform === "ios" ? (
                <ol className="mt-3.5 flex flex-col gap-2.5">
                  <IOSStep n={1}>
                    Tap the <Share size={14} className="inline translate-y-[1px]" /> <b>Share</b>{" "}
                    button in Safari&apos;s toolbar (below).
                  </IOSStep>
                  <IOSStep n={2}>
                    Scroll and tap <b>Add to Home Screen</b>.
                  </IOSStep>
                  <IOSStep n={3}>
                    Tap <b>Add</b> — Namaz Times appears like a normal app.
                  </IOSStep>
                </ol>
              ) : (
                <ul className="mt-3.5 flex flex-col gap-1.5">
                  <Benefit>Full-screen, just like a native app</Benefit>
                  <Benefit>Prayer alerts on your lock screen</Benefit>
                  <Benefit>Works fully offline — timetable is built in</Benefit>
                </ul>
              )}

              <div className="mt-4 flex gap-2.5">
                <Pressable className="flex-1">
                  <button
                    type="button"
                    onClick={onDismiss}
                    className="h-11 w-full cursor-pointer rounded-full text-[16px] font-semibold"
                    style={{
                      background: "var(--surface-highest)",
                      color: "var(--on-surface-variant)",
                    }}
                  >
                    Not now
                  </button>
                </Pressable>
                <Pressable className="flex-1">
                  <button
                    type="button"
                    onClick={onInstall}
                    className="flex h-11 w-full cursor-pointer items-center justify-center gap-2 rounded-full text-[16px] font-semibold"
                    style={{
                      background: "linear-gradient(90deg, var(--primary), color-mix(in srgb, var(--primary) 82%, transparent))",
                      color: "var(--on-primary)",
                      boxShadow: "0 10px 24px color-mix(in srgb, var(--primary) 40%, transparent)",
                    }}
                  >
                    <Download size={16} />
                    {platform === "ios" ? "Show steps" : canPrompt ? "Install" : "How to install"}
                  </button>
                </Pressable>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

function IOSStep({ n, children }: { n: number; children: React.ReactNode }) {
  return (
    <li className="flex items-start gap-2.5">
      <span
        className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[12px] font-bold"
        style={{ background: "var(--primary-container)", color: "var(--on-primary-container)" }}
      >
        {n}
      </span>
      <p className="text-[16px] leading-6" style={{ color: "var(--on-surface-variant)" }}>
        {children}
      </p>
    </li>
  );
}

function Benefit({ children }: { children: React.ReactNode }) {
  return (
    <li className="flex items-center gap-2 text-[16px]" style={{ color: "var(--on-surface-variant)" }}>
      <span
        className="flex h-4 w-4 shrink-0 items-center justify-center rounded-full"
        style={{ background: "var(--primary-container)" }}
        aria-hidden
      >
        <span
          className="block h-1.5 w-1.5 rounded-full"
          style={{ background: "var(--primary)" }}
        />
      </span>
      {children}
    </li>
  );
}
