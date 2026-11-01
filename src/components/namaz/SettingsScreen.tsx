"use client";

/** Settings — ported from SettingsScreen.kt, plus web-specific install row. */
import { useState } from "react";
import { motion } from "framer-motion";
import {
  BellRing,
  Download,
  Info,
  Minus,
  Plus,
  Smartphone,
  TriangleAlert,
  Vibrate,
} from "lucide-react";
import { PRAYERS, MADHAB_DESCRIPTION, MADHAB_LABEL, type PrayerId } from "@/lib/namaz/prayers";
import { type ThemeMode, type UserSettings } from "@/lib/namaz/settings";
import {
  hijriFormatted,
  hijriFrom,
} from "@/lib/namaz/hijri";
import { requestNotificationPermission } from "@/lib/namaz/notify";
import { Stagger } from "./effects";
import { PrayerIconBadge } from "./PrayerIconBadge";
import { GlassCard } from "./bits";

export function SettingsScreen({
  settings,
  today,
  update,
  setNotify,
  installSupported,
  onInstall,
}: {
  settings: UserSettings;
  today: Date;
  update: (patch: Partial<UserSettings>) => void;
  setNotify: (prayer: PrayerId, value: boolean) => void;
  installSupported: boolean;
  onInstall: () => void;
}) {
  const notifPermission =
    typeof Notification !== "undefined" ? Notification.permission : "denied";

  return (
    <div className="nice-scroll h-full overflow-y-auto px-[18px] pt-3 pb-7">
      <Stagger index={0}>
        <div className="px-1 pb-1">
          <h1 className="text-[28px] leading-9 font-semibold" style={{ color: "var(--on-surface)" }}>
            Settings
          </h1>
          <p className="text-sm" style={{ color: "var(--on-surface-variant)" }}>
            Personalise alerts and display
          </p>
        </div>
      </Stagger>

      {/* ---------- Notifications ---------- */}
      <Stagger index={1}>
        <section aria-label="Notifications" className="mt-3">
          <SectionTitle text="Notifications" />
          <GlassCard>
            <div className="py-1">
              <SwitchRow
                icon={<BellRing size={20} style={{ color: "var(--primary)" }} />}
                title="Prayer-time notifications"
                subtitle="Alert when each prayer begins"
                checked={settings.notificationsEnabled}
                onCheckedChange={(v) => update({ notificationsEnabled: v })}
              />
              {settings.notificationsEnabled && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: "auto", opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.25 }}
                  className="overflow-hidden"
                >
                  {notifPermission !== "granted" && (
                    <WarningRow
                      text={
                        notifPermission === "denied"
                          ? "Notifications are blocked — allow them in browser settings to get alerts"
                          : "Notification permission is required"
                      }
                      action={notifPermission === "denied" ? "Help" : "Allow"}
                      onClick={async () => {
                        await requestNotificationPermission();
                      }}
                    />
                  )}
                  <ThinDivider className="mt-2" />
                  <p
                    className="px-[18px] pt-3 pb-0.5 text-xs font-semibold tracking-[1px]"
                    style={{ color: "var(--on-surface-variant)" }}
                  >
                    NOTIFY ME FOR
                  </p>
                  {PRAYERS.map((p) => {
                    const on = settings.notifyFor[p.id] ?? false;
                    return (
                      <div
                        key={p.id}
                        className="flex w-full cursor-pointer items-center px-[18px] py-1"
                        onClick={() => setNotify(p.id, !on)}
                        role="button"
                        aria-label={`${p.displayName} notifications ${on ? "on" : "off"}`}
                      >
                        <PrayerIconBadge prayer={p.id} size={34} active={on} />
                        <div className="ml-3.5 min-w-0 flex-1">
                          <p className="text-base" style={{ color: "var(--on-surface)" }}>
                            {p.displayName}
                          </p>
                          <p className="text-xs" style={{ color: "var(--on-surface-variant)" }}>
                            {p.isSalah ? p.arabicName : "Marks the end of Fajr"}
                          </p>
                        </div>
                        <AppSwitch
                          checked={on}
                          onCheckedChange={(v) => setNotify(p.id, v)}
                          ariaLabel={`${p.displayName} notifications`}
                        />
                      </div>
                    );
                  })}
                  <ThinDivider className="mt-1.5" />
                  <ReminderRow
                    minutes={settings.reminderMinutes}
                    onMinutes={(m) => update({ reminderMinutes: m })}
                  />
                  <ThinDivider />
                  <SwitchRow
                    icon={<Vibrate size={20} style={{ color: "var(--primary)" }} />}
                    title="Vibrate"
                    subtitle={null}
                    checked={settings.vibrate}
                    onCheckedChange={(v) => update({ vibrate: v })}
                  />
                </motion.div>
              )}
            </div>
          </GlassCard>
        </section>
      </Stagger>

      {/* ---------- Asr calculation ---------- */}
      <Stagger index={2}>
        <section aria-label="Asr calculation" className="mt-4">
          <SectionTitle text="Asr calculation" />
          <GlassCard>
            <div className="p-[18px]">
              <Segmented
                options={[
                  { value: "SHAFI", label: MADHAB_LABEL.SHAFI },
                  { value: "HANAFI", label: MADHAB_LABEL.HANAFI },
                ]}
                value={settings.madhab}
                onChange={(v) => update({ madhab: v as UserSettings["madhab"] })}
                ariaLabel="Asr school"
              />
              <p className="mt-2.5 text-xs" style={{ color: "var(--on-surface-variant)" }}>
                {MADHAB_DESCRIPTION[settings.madhab]}
              </p>
            </div>
          </GlassCard>
        </section>
      </Stagger>

      {/* ---------- Display ---------- */}
      <Stagger index={3}>
        <section aria-label="Display" className="mt-4">
          <SectionTitle text="Display" />
          <GlassCard>
            <div className="py-1">
              <SwitchRow
                icon={<Smartphone size={20} style={{ color: "var(--primary)" }} />}
                title="24-hour time"
                subtitle={null}
                checked={settings.use24Hour}
                onCheckedChange={(v) => update({ use24Hour: v })}
              />
              <ThinDivider />
              <div className="p-[18px]">
                <p className="text-base" style={{ color: "var(--on-surface)" }}>
                  Theme
                </p>
                <div className="mt-2.5">
                  <Segmented
                    options={[
                      { value: "SYSTEM", label: "System" },
                      { value: "LIGHT", label: "Light" },
                      { value: "DARK", label: "Dark" },
                    ]}
                    value={settings.themeMode}
                    onChange={(v) => update({ themeMode: v as ThemeMode })}
                    ariaLabel="Theme"
                  />
                </div>
              </div>
              <ThinDivider />
              <div className="flex items-center px-[18px] py-3.5">
                <div className="min-w-0 flex-1">
                  <p className="text-base" style={{ color: "var(--on-surface)" }}>
                    Hijri date adjustment
                  </p>
                  <p className="text-xs" style={{ color: "var(--on-surface-variant)" }}>
                    Today: {hijriFormatted(hijriFrom(today, settings.hijriAdjustment))}
                  </p>
                </div>
                <StepperButton
                  label="Minus one day"
                  disabled={settings.hijriAdjustment <= -2}
                  onClick={() => update({ hijriAdjustment: settings.hijriAdjustment - 1 })}
                >
                  <Minus size={18} />
                </StepperButton>
                <div
                  className="mx-2 flex h-[38px] w-11 items-center justify-center rounded-full"
                  style={{ background: "var(--surface-highest)" }}
                >
                  <span className="text-base font-semibold" style={{ color: "var(--on-surface)" }}>
                    {(settings.hijriAdjustment > 0 ? "+" : "") + settings.hijriAdjustment}
                  </span>
                </div>
                <StepperButton
                  label="Plus one day"
                  disabled={settings.hijriAdjustment >= 2}
                  onClick={() => update({ hijriAdjustment: settings.hijriAdjustment + 1 })}
                >
                  <Plus size={18} />
                </StepperButton>
              </div>
            </div>
          </GlassCard>
        </section>
      </Stagger>

      {/* ---------- Install (web/PWA) ---------- */}
      <Stagger index={4}>
        <section aria-label="Install app" className="mt-4">
          <SectionTitle text="App" />
          <GlassCard>
            <SwitchRow
              icon={<Download size={20} style={{ color: "var(--primary)" }} />}
              title="Install Namaz Times"
              subtitle={
                installSupported
                  ? "Add to your home screen for a full-screen, offline app"
                  : "Use your browser's “Add to Home Screen” to install"
              }
              action={
                <button
                  type="button"
                  onClick={onInstall}
                  className="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold"
                  style={{ background: "var(--primary)", color: "var(--on-primary)" }}
                >
                  Install
                </button>
              }
            />
          </GlassCard>
        </section>
      </Stagger>

      {/* ---------- About ---------- */}
      <Stagger index={5}>
        <section aria-label="About" className="mt-4">
          <SectionTitle text="About" />
          <GlassCard>
            <div className="flex items-start p-[18px]">
              <div
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full"
                style={{ background: "linear-gradient(135deg, #0B7A5B, #19B58A)" }}
              >
                <Info size={20} className="text-white" />
              </div>
              <div className="ml-3.5">
                <p className="text-[17px] font-semibold" style={{ color: "var(--on-surface)" }}>
                  Namaz Times · Hubli
                </p>
                <p className="text-[11px] font-medium" style={{ color: "var(--gold)" }}>
                  Version 1.0.0 (Web)
                </p>
                <p className="mt-1.5 text-xs leading-5" style={{ color: "var(--on-surface-variant)" }}>
                  Timings are taken from the printed annual timetable for Hubli, Karnataka. The
                  sheet lists odd dates; a date that is not printed uses the previous printed
                  day&apos;s times exactly as on the sheet. Hijri dates follow the Umm al-Qura
                  calendar and may differ by a day from local moon sighting – use the adjustment
                  above. Alerts work while the app is open or installed.
                </p>
              </div>
            </div>
          </GlassCard>
        </section>
      </Stagger>

      {/* Reset (handy on web) */}
      <Stagger index={6}>
        <p className="mx-auto mt-5 block text-center text-xs font-medium" style={{ color: "var(--on-surface-variant)" }}>
          Settings are saved on this device
        </p>
      </Stagger>
    </div>
  );
}

/* ------------------------------- pieces ------------------------------- */

function SectionTitle({ text }: { text: string }) {
  return (
    <p
      className="px-1.5 pt-2 pb-2 text-xs font-semibold tracking-[1px] uppercase"
      style={{ color: "var(--primary)" }}
    >
      {text}
    </p>
  );
}

function ThinDivider({ className }: { className?: string }) {
  return (
    <div
      className={`mx-[18px] h-px ${className ?? ""}`}
      style={{ background: "color-mix(in srgb, var(--outline-variant) 50%, transparent)" }}
      aria-hidden
    />
  );
}

function AppSwitch({
  checked,
  onCheckedChange,
  ariaLabel,
}: {
  checked: boolean;
  onCheckedChange: (v: boolean) => void;
  ariaLabel: string;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={ariaLabel}
      onClick={(e) => {
        e.stopPropagation();
        onCheckedChange(!checked);
      }}
      className="relative h-6 w-10 shrink-0 cursor-pointer rounded-full transition-colors duration-300"
      style={{
        background: checked ? "var(--primary)" : "var(--surface-highest)",
        border: checked ? "none" : "1px solid var(--outline-variant)",
      }}
    >
      <motion.span
        className="absolute top-[3px] h-[18px] w-[18px] rounded-full"
        animate={{ left: checked ? 19 : 3 }}
        transition={{ type: "spring", stiffness: 500, damping: 30 }}
        style={{
          background: checked ? "#FFFFFF" : "var(--on-surface-variant)",
        }}
      />
    </button>
  );
}

function SwitchRow({
  icon,
  title,
  subtitle,
  checked,
  onCheckedChange,
  action,
}: {
  icon?: React.ReactNode;
  title: string;
  subtitle: string | null;
  checked?: boolean;
  onCheckedChange?: (v: boolean) => void;
  action?: React.ReactNode;
}) {
  return (
    <div
      className="flex w-full items-center px-[18px] py-3"
      onClick={() => onCheckedChange?.(!checked)}
      role={onCheckedChange ? "button" : undefined}
    >
      {icon && <span className="mr-3 flex">{icon}</span>}
      <div className="min-w-0 flex-1">
        <p className="text-base" style={{ color: "var(--on-surface)" }}>
          {title}
        </p>
        {subtitle && (
          <p className="text-xs" style={{ color: "var(--on-surface-variant)" }}>
            {subtitle}
          </p>
        )}
      </div>
      {action ?? (onCheckedChange ? <AppSwitch checked={!!checked} onCheckedChange={onCheckedChange} ariaLabel={title} /> : null)}
    </div>
  );
}

function WarningRow({
  text,
  action,
  onClick,
}: {
  text: string;
  action: string;
  onClick: () => void;
}) {
  return (
    <div
      className="mx-3 my-1 flex cursor-pointer items-center px-3 py-2.5"
      onClick={onClick}
      role="button"
      style={{
        background: "color-mix(in srgb, var(--error-container) 60%, transparent)",
        borderRadius: 14,
      }}
    >
      <TriangleAlert size={20} style={{ color: "var(--on-error-container)" }} />
      <p
        className="ml-2.5 min-w-0 flex-1 text-xs"
        style={{ color: "var(--on-error-container)" }}
      >
        {text}
      </p>
      <span className="text-sm font-semibold" style={{ color: "var(--primary)" }}>
        {action}
      </span>
    </div>
  );
}

function ReminderRow({
  minutes,
  onMinutes,
}: {
  minutes: number;
  onMinutes: (m: number) => void;
}) {
  const options = [0, 5, 10, 15, 30];
  return (
    <div className="px-[18px] py-3">
      <p className="text-base" style={{ color: "var(--on-surface)" }}>
        Remind me before
      </p>
      <p className="text-xs" style={{ color: "var(--on-surface-variant)" }}>
        {minutes === 0 ? "Only at prayer time" : `${minutes} minutes before each prayer`}
      </p>
      <div
        className="mt-2.5 flex h-10 gap-[3px] rounded-full p-[3px]"
        style={{ background: "color-mix(in srgb, var(--surface-highest) 70%, transparent)" }}
        role="radiogroup"
        aria-label="Reminder minutes"
      >
        {options.map((m) => {
          const selected = minutes === m;
          return (
            <button
              key={m}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onMinutes(m)}
              className="flex-1 cursor-pointer rounded-full text-sm font-medium transition-colors duration-200"
              style={{
                background: selected ? "var(--primary)" : "transparent",
                color: selected ? "var(--on-primary)" : "var(--on-surface-variant)",
              }}
            >
              {m === 0 ? "Off" : `${m}m`}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function Segmented({
  options,
  value,
  onChange,
  ariaLabel,
}: {
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
  ariaLabel: string;
}) {
  return (
    <div
      className="flex h-11 w-full rounded-full p-[3px]"
      style={{ background: "color-mix(in srgb, var(--surface-highest) 70%, transparent)" }}
      role="radiogroup"
      aria-label={ariaLabel}
    >
      {options.map((o) => {
        const selected = value === o.value;
        return (
          <button
            key={o.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(o.value)}
            className="relative flex-1 cursor-pointer rounded-full text-sm font-semibold transition-colors duration-200"
            style={{
              background: selected ? "var(--primary)" : "transparent",
              color: selected ? "var(--on-primary)" : "var(--on-surface-variant)",
              border: selected ? "none" : "1px solid var(--outline-variant)",
            }}
          >
            {o.label}
          </button>
        );
      })}
    </div>
  );
}

function StepperButton({
  children,
  onClick,
  disabled,
  label,
}: {
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className="flex h-[38px] w-[38px] cursor-pointer items-center justify-center rounded-full disabled:cursor-default"
      style={{
        background: disabled ? "var(--surface)" : "var(--surface-high)",
        color: disabled ? "var(--outline-variant)" : "var(--on-surface)",
        opacity: disabled ? 0.55 : 1,
      }}
    >
      {children}
    </button>
  );
}
