"use client";

/** Per-prayer gradient icon badges (PrayerIconBadge from Common.kt). */
import type { CSSProperties } from "react";
import { Moon, MoonStar, Sun, SunDim, SunMedium, Sunrise } from "lucide-react";
import { PRAYER_GRADIENT } from "@/lib/namaz/sky";
import { hexAlpha } from "./effects";
import type { PrayerId } from "@/lib/namaz/prayers";

function PrayerGlyph({
  prayer,
  size,
  active,
}: {
  prayer: PrayerId;
  size: number;
  active: boolean;
}) {
  const style: CSSProperties = { width: size * 0.5, height: size * 0.5 };
  const cls = active ? "text-white" : "text-[var(--on-surface-variant)]";
  switch (prayer) {
    case "FAJR":
      return <Sunrise style={style} className={cls} strokeWidth={2} />;
    case "SUNRISE":
      return <Sun style={style} className={cls} strokeWidth={2} />;
    case "ZUHR":
      return <SunMedium style={style} className={cls} strokeWidth={2} />;
    case "ASR":
      return <SunDim style={style} className={cls} strokeWidth={2} />;
    case "MAGHRIB":
      return <Moon style={style} className={cls} strokeWidth={2} />;
    case "ISHA":
      return <MoonStar style={style} className={cls} strokeWidth={2} />;
  }
}

export function PrayerIconBadge({
  prayer,
  size = 44,
  active = true,
  className,
}: {
  prayer: PrayerId;
  size?: number;
  active?: boolean;
  className?: string;
}) {
  const [from, to] = PRAYER_GRADIENT[prayer];
  const style: CSSProperties = active
    ? {
        background: `linear-gradient(135deg, ${from}, ${to})`,
        boxShadow: `0 4px 14px ${hexAlpha(from, 0.45)}`,
      }
    : { background: "var(--surface-highest)" };
  return (
    <div
      className={`flex shrink-0 items-center justify-center rounded-full transition-colors duration-300 ${className ?? ""}`}
      style={{ width: size, height: size, ...style }}
      aria-hidden
    >
      <PrayerGlyph prayer={prayer} size={size} active={active} />
    </div>
  );
}
