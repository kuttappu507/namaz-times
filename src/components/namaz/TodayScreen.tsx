"use client";

/**
 * Home screen — ported from TodayScreen.kt. The greeting/date header and hero card
 * are pinned; the "Today's timings" list below scrolls under them with a fade edge.
 */
import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Bell, BellOff, MapPin, TriangleAlert } from "lucide-react";
import {
  PRAYERS,
  prayerName,
  timeOf,
  type AsrMadhab,
  type PrayerDay,
  type PrayerId,
} from "@/lib/namaz/prayers";
import { isNotifyOn, type UserSettings } from "@/lib/namaz/settings";
import { amPm, countdown, formatLongDate, greetingFor, timeNumber } from "@/lib/namaz/time";
import { skyPaletteFor } from "@/lib/namaz/sky";
import { hijriFormatted, hijriMonthArabic } from "@/lib/namaz/hijri";
import type { TodayState } from "@/lib/namaz/use-today-state";
import {
  AmbientOrbs,
  CountdownRing,
  Pressable,
  ShimmerOverlay,
  Starfield,
  Stagger,
  hexAlpha as hexA,
} from "./effects";
import { PrayerIconBadge } from "./PrayerIconBadge";
import { Pill, SectionHeader } from "./bits";

export function TodayScreen({
  state,
  settings,
  onOpenSettings,
}: {
  state: TodayState;
  settings: UserSettings;
  onOpenSettings: () => void;
}) {
  const use24 = settings.use24Hour;
  const madhab = settings.madhab;
  const now = state.now;

  const notifBlocked =
    typeof window !== "undefined" &&
    "Notification" in window &&
    typeof Notification !== "undefined" &&
    Notification.permission === "denied";
  const warning =
    notifBlocked && settings.notificationsEnabled
      ? "Notifications are blocked · enable them in browser settings"
      : null;

  return (
    <div className="flex h-full flex-col">
      {/* ---------- Pinned part ---------- */}
      <div className="px-[18px]">
        <div className="h-2.5" />
        <Stagger index={0}>
          <Header state={state} />
        </Stagger>
        <div className="h-3.5" />
        <Stagger index={1}>
          <HeroCard state={state} settings={settings} />
        </Stagger>
        {warning && (
          <div className="mt-3">
            <Stagger index={2}>
              <WarningCard text={warning} />
            </Stagger>
          </div>
        )}
        <div className="h-[18px]" />
        <Stagger index={3}>
          <SectionHeader
            title="Today's timings"
            trailing={
              <Pill
                text={madhab === "HANAFI" ? "HANAFI" : "SHAFI'I"}
                container="var(--gold-container)"
                content="var(--on-gold-container)"
              />
            }
          />
        </Stagger>
        <div className="h-1.5" />
      </div>

      {/* ---------- Scrolling list ---------- */}
      <PrayerList state={state} settings={settings} onOpenSettings={onOpenSettings} />
    </div>
  );
}

/* ------------------------------- Header ------------------------------- */

function Header({ state }: { state: TodayState }) {
  const greeting = greetingFor(state.now.getHours());
  return (
    <div className="flex items-center px-0.5">
      <div className="min-w-0 flex-1">
        <p className="text-[16px] font-medium" style={{ color: "var(--primary)" }}>
          Assalamu Alaikum · {greeting}
        </p>
        <h1 className="mt-0.5 truncate text-[33px] leading-11 font-semibold" style={{ color: "var(--on-surface)" }}>
          {formatLongDate(state.now)}
        </h1>
        <p className="mt-0.5 truncate text-[16px]" style={{ color: "var(--on-surface-variant)" }}>
          {hijriFormatted(state.hijri)}  ·  {hijriMonthArabic(state.hijri)}
        </p>
      </div>
      <div
        className="ml-3 flex shrink-0 items-center gap-1 rounded-full px-3 py-2"
        style={{ background: "var(--primary-container)" }}
      >
        <MapPin size={16} style={{ color: "var(--on-primary-container)" }} />
        <span className="text-[16px] font-medium" style={{ color: "var(--on-primary-container)" }}>
          Hubli
        </span>
      </div>
    </div>
  );
}

/* ------------------------------- Hero card ------------------------------- */

function HeroCard({ state, settings }: { state: TodayState; settings: UserSettings }) {
  const use24 = settings.use24Hour;
  const palette = skyPaletteFor(state.now.getHours());
  const nextName = prayerName(state.next.prayer, state.next.at);
  const arabicName = PRAYERS.find((p) => p.id === state.next.prayer)?.arabicName ?? "";
  const nextMinutes = timeOfDayMinutes(state.next.at);
  const currentLabel =
    state.current.prayer === "SUNRISE"
      ? "Fajr has ended"
      : `Now · ${prayerName(state.current.prayer, state.current.at)}`;
  const nextIsTomorrow = !isSameDay(state.next.at, state.now);
  const isNight = state.now.getHours() >= 19 || state.now.getHours() < 5;

  return (
    <motion.div
      className="relative overflow-hidden"
      style={{
        borderRadius: 30,
        background: `linear-gradient(135deg, ${palette.top} 0%, ${palette.mid} 52%, ${palette.bottom} 100%)`,
        boxShadow: `0 22px 44px -12px ${hexA(palette.mid, 0.55)}`,
      }}
      initial={false}
    >
      {/* Sky crossfade when the time-of-day palette changes */}
      <div className="pointer-events-none absolute inset-0" aria-hidden>
        <AnimatePresence initial={false}>
          <motion.div
            key={`${palette.top}-${palette.mid}-${palette.bottom}`}
            className="absolute inset-0"
            style={{
              background: `linear-gradient(135deg, ${palette.top} 0%, ${palette.mid} 52%, ${palette.bottom} 100%)`,
            }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 1.5 }}
          />
        </AnimatePresence>
      </div>

      <ShimmerOverlay />
      <AmbientOrbs colors={[palette.glow, palette.accent, "#FFFFFF"]} alpha={0.4} seed={2} />
      {isNight && <Starfield count={28} seed={3} />}

      {/* Decorative circles */}
      <div
        className="pointer-events-none absolute rounded-full"
        style={{
          width: "180%",
          aspectRatio: "1",
          right: "-40%",
          top: "-55%",
          background: "rgba(255,255,255,0.07)",
        }}
        aria-hidden
      />
      <div
        className="pointer-events-none absolute rounded-full"
        style={{
          width: "120%",
          aspectRatio: "1",
          left: "-5%",
          bottom: "-110%",
          background: "rgba(255,255,255,0.05)",
        }}
        aria-hidden
      />

      <div className="relative flex items-center gap-2.5 px-[22px] py-5">
        <div className="min-w-0 flex-1">
          <Pill
            text={nextIsTomorrow ? "NEXT · TOMORROW" : "UP NEXT"}
            container="rgba(255,255,255,0.18)"
            content="#FFFFFF"
          />
          <div className="mt-2.5 overflow-hidden">
            <AnimatePresence mode="popLayout" initial={false}>
              <motion.div
                key={nextName + state.next.at.toDateString()}
                initial={{ y: 26, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                exit={{ y: -26, opacity: 0 }}
                transition={{ duration: 0.32, ease: [0.33, 1, 0.68, 1] }}
              >
                <p className="text-[39px] leading-[48px] font-bold text-white">
                  {nextName}
                </p>
                <p className="text-[20px] font-semibold" style={{ color: palette.accent }}>
                  {arabicName}
                </p>
              </motion.div>
            </AnimatePresence>
          </div>
          <div className="mt-2.5 flex items-baseline">
            <span className="text-[37px] leading-[46px] font-semibold text-white">
              {timeNumber(nextMinutes, use24)}
            </span>
            {!use24 && (
              <span className="ml-1.5 pb-1 text-[16px] font-medium text-white/85">
                {amPm(nextMinutes, use24)}
              </span>
            )}
          </div>
          <div className="mt-1 flex items-center">
            <span
              className="live-dot block h-[7px] w-[7px] rounded-full"
              style={{ background: palette.accent }}
              aria-hidden
            />
            <span className="ml-2 text-[16px] text-white/85">{currentLabel}</span>
          </div>
        </div>
        <CountdownRing
          progress={state.progress}
          label={countdown(state.timeToNextMs)}
          accent={palette.accent}
        />
      </div>
    </motion.div>
  );
}

/* ------------------------------- Prayer list ------------------------------- */

function PrayerList({
  state,
  settings,
  onOpenSettings,
}: {
  state: TodayState;
  settings: UserSettings;
  onOpenSettings: () => void;
}) {
  const listRef = useRef<HTMLDivElement>(null);
  const [scrolled, setScrolled] = useState(false);
  const use24 = settings.use24Hour;
  const madhab = settings.madhab;

  const currentIndex = isSameDay(state.current.at, state.now)
    ? PRAYERS.findIndex((p) => p.id === state.current.prayer)
    : -1;
  const nextIndex = isSameDay(state.next.at, state.now)
    ? PRAYERS.findIndex((p) => p.id === state.next.prayer)
    : -1;
  const anchor = currentIndex >= 0 ? currentIndex : nextIndex >= 0 ? nextIndex : 0;
  const nextIsTomorrow = !isSameDay(state.next.at, state.now);

  // The list opens on the ongoing prayer so NOW and NEXT are on screen (PrayerRepository).
  useEffect(() => {
    const el = listRef.current;
    if (!el) return;
    const target = el.querySelector<HTMLElement>(`[data-prayer-index="${anchor}"]`);
    if (target) el.scrollTo({ top: Math.max(0, target.offsetTop - 8), behavior: "smooth" });
  }, [anchor]);

  const onScroll = () => setScrolled((listRef.current?.scrollTop ?? 0) > 2);

  return (
    <div className="relative min-h-0 flex-1">
      {/* scroll scrim so rows slide under the pinned hero */}
      <div
        className="pointer-events-none absolute inset-x-0 top-0 z-10 h-3.5 transition-opacity duration-200"
        style={{
          opacity: scrolled ? 1 : 0,
          background: "linear-gradient(180deg, var(--scrim-color), transparent)",
        }}
        aria-hidden
      />
      <div
        ref={listRef}
        onScroll={onScroll}
        className="nice-scroll relative h-full overflow-y-auto px-[18px] pt-2 pb-[16px]"
      >
        <div className="flex flex-col gap-2.5">
          {PRAYERS.map((p, index) => {
            const at = dateOf(state.today, p.id, madhab);
            return (
              <div key={p.id} data-prayer-index={index}>
                <Stagger index={3 + Math.max(0, index - anchor)} staggerMs={55}>
                  <PrayerCard
                    prayer={p.id}
                    name={prayerName(p.id, new Date(state.today.year, state.today.month - 1, state.today.day))}
                    time={timeNumber(timeOf(state.today, p.id, madhab), use24)}
                    amPm={amPm(timeOf(state.today, p.id, madhab), use24)}
                    isCurrent={index === currentIndex}
                    isNext={index === nextIndex}
                    isPast={state.now.getTime() >= at.getTime()}
                    notifyOn={isNotifyOn(settings, p.id)}
                  />
                </Stagger>
              </div>
            );
          })}
          {nextIsTomorrow && (
            <Stagger index={4} staggerMs={55}>
              <PrayerCard
                prayer={state.next.prayer}
                name={prayerName(state.next.prayer, state.next.at)}
                note={`Tomorrow · ${PRAYERS.find((p) => p.id === state.next.prayer)?.arabicName ?? ""}`}
                time={timeNumber(
                  timeOfDayMinutes(state.next.at),
                  use24
                )}
                amPm={amPm(timeOfDayMinutes(state.next.at), use24)}
                isCurrent={false}
                isNext
                isPast={false}
                notifyOn={isNotifyOn(settings, state.next.prayer)}
              />
            </Stagger>
          )}
          <Stagger index={10} staggerMs={55}>
            <button
              type="button"
              onClick={onOpenSettings}
              className="block cursor-pointer px-1.5 py-1 text-left text-[14px]"
              style={{ color: "var(--on-surface-variant)" }}
            >
              Asr per {madhab === "HANAFI" ? "Hanafi" : "Shafi'i"} school · change in Settings
            </button>
          </Stagger>
        </div>
      </div>
    </div>
  );
}

function PrayerCard({
  prayer,
  name,
  time,
  note,
  amPm: amPmLabel,
  isCurrent,
  isNext,
  isPast,
  notifyOn,
}: {
  prayer: PrayerId;
  name: string;
  time: string;
  note?: string;
  amPm: string;
  isCurrent: boolean;
  isNext: boolean;
  isPast: boolean;
  notifyOn: boolean;
}) {
  const isSalah = prayer !== "SUNRISE";
  const tint = isNext
    ? "color-mix(in srgb, var(--primary) 16%, transparent)"
    : isCurrent && isSalah
      ? "color-mix(in srgb, var(--gold) 12%, transparent)"
      : "transparent";
  const border = isNext
    ? "color-mix(in srgb, var(--primary) 55%, transparent)"
    : isCurrent && isSalah
      ? "color-mix(in srgb, var(--gold) 50%, transparent)"
      : "color-mix(in srgb, white 60%, var(--outline-variant))";
  const textColor = isSalah ? "var(--on-surface)" : "var(--on-surface-variant)";

  return (
    <Pressable
      className="relative overflow-hidden"
      style={{ borderRadius: 22 }}
      role="listitem"
    >
      <motion.div
        className="relative"
        animate={{ opacity: isPast && !isCurrent ? 0.62 : 1 }}
        transition={{ duration: 0.4 }}
        style={{
          borderRadius: 22,
          background: `linear-gradient(180deg, var(--card-grad-start), var(--card-grad-end))`,
          border: `1px solid ${border}`,
          boxShadow: isNext
            ? "0 12px 30px color-mix(in srgb, var(--primary) 35%, transparent)"
            : "0 8px 20px rgba(0,0,0,0.10)",
        }}
      >
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            background: `linear-gradient(90deg, ${tint}, color-mix(in srgb, ${tint} 25%, transparent))`,
            borderRadius: 22,
          }}
          aria-hidden
        />
        {isNext && (
          <div
            className="next-glow pointer-events-none absolute inset-0"
            style={{
              border: "1.5px solid color-mix(in srgb, var(--primary) 85%, transparent)",
              borderRadius: 22,
            }}
            aria-hidden
          />
        )}
        <div className="relative flex items-center px-4 py-3.5">
          <PrayerIconBadge
            prayer={prayer}
            active={isNext || isCurrent}
            size={44}
          />
          <div className="ml-3.5 min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="text-[21px] font-semibold" style={{ color: textColor }}>
                {name}
              </span>
              {isCurrent && isSalah && (
                <Pill text="NOW" container="var(--gold-container)" content="var(--on-gold-container)" />
              )}
              {isNext && <Pill text="NEXT" container="var(--primary)" content="var(--on-primary)" />}
            </div>
            <p className="text-[14px]" style={{ color: "var(--on-surface-variant)" }}>
              {note ??
                (prayer === "SUNRISE" ? "Marks the end of Fajr" : PRAYERS.find((p) => p.id === prayer)?.arabicName)}
            </p>
          </div>
          {notifyOn ? (
            <Bell size={18} style={{ color: "var(--primary)" }} />
          ) : (
            <BellOff size={18} style={{ color: "var(--outline-variant)" }} />
          )}
          <div className="ml-3.5 flex items-baseline">
            <span className="text-[26px] font-bold" style={{ color: textColor }}>
              {time}
            </span>
            {amPmLabel && (
              <span className="ml-[3px] pb-[3px] text-[13px] font-medium" style={{ color: "var(--on-surface-variant)" }}>
                {amPmLabel}
              </span>
            )}
          </div>
        </div>
      </motion.div>
    </Pressable>
  );
}

function WarningCard({ text }: { text: string }) {
  return (
    <div
      className="flex items-center gap-3 px-4 py-3"
      style={{
        borderRadius: 18,
        background: "var(--error-container)",
        color: "var(--on-error-container)",
      }}
      role="alert"
    >
      <TriangleAlert size={20} />
      <p className="text-[16px]">{text}</p>
    </div>
  );
}

/* ------------------------------- helpers ------------------------------- */

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  );
}

function dateOf(d: PrayerDay, prayer: PrayerId, madhab: AsrMadhab): Date {
  // Only used for past/future comparison — the raw minutes suffice.
  const base = new Date(d.year, d.month - 1, d.day).getTime();
  const minutes = timeOf(d, prayer, madhab);
  return new Date(base + minutes * 60_000);
}

function timeOfDayMinutes(d: Date): number {
  return d.getHours() * 60 + d.getMinutes();
}
