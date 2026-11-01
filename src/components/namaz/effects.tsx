"use client";

/**
 * Draw-phase-only effects from Effects.kt, re-imagined for the web:
 * staggered entrance, shimmer sweep, ambient orbs, starfield, odometer digits
 * and the countdown ring with sweep gradient + glowing head.
 */
import { useMemo, useSyncExternalStore, type CSSProperties, type ReactNode } from "react";
import { AnimatePresence, motion } from "framer-motion";

const emptySubscribe = () => () => {};

/* ------------------------------ Entrance ------------------------------ */

/** Staggered fade/slide-up entrance (Modifier.staggeredEntrance). */
export function Stagger({
  index = 0,
  staggerMs = 75,
  baseDelayMs = 90,
  className,
  children,
  style,
}: {
  index?: number;
  staggerMs?: number;
  baseDelayMs?: number;
  className?: string;
  children: ReactNode;
  style?: CSSProperties;
}) {
  const delay = baseDelayMs + index * staggerMs;
  return (
    <motion.div
      className={className}
      style={style}
      initial={{ opacity: 0, y: 24, scale: 0.97 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{
        delay: delay / 1000,
        duration: 0.5,
        ease: [0.22, 1, 0.36, 1],
      }}
    >
      {children}
    </motion.div>
  );
}

/* ------------------------------ Shimmer ------------------------------ */

export function ShimmerOverlay() {
  return (
    <div className="absolute inset-0 overflow-hidden rounded-[inherit]" aria-hidden>
      <div className="shimmer-band" />
    </div>
  );
}

/* ------------------------------ Ambient orbs ------------------------------ */

/** Slowly drifting soft light blobs (AmbientOrbs). */
export function AmbientOrbs({
  colors,
  alpha = 0.4,
  seed = 0,
}: {
  colors: string[];
  alpha?: number;
  seed?: number;
}) {
  const orbs = useMemo(
    () =>
      colors.map((color, i) => ({
        color,
        size: 55 + ((seed + i * 37) % 30),
        durX: 8.4 + i * 2.6 + (seed % 5) * 0.7,
        durY: 6.9 + i * 3.1 + (seed % 7) * 0.5,
        left: 14 + i * 24,
        top: 12 + i * 21,
      })),
    [colors, seed]
  );
  return (
    <div className="absolute inset-0 overflow-hidden rounded-[inherit]" aria-hidden>
      {orbs.map((o, i) => (
        <div
          key={i}
          className="orb orb-x"
          style={
            {
              "--orb-dur-x": `${o.durX}s`,
              "--orb-dur-y": `${o.durY}s`,
              left: `${o.left}%`,
              top: `${o.top}%`,
              width: `${o.size}%`,
              aspectRatio: "1",
              transform: "translate(-50%, -50%)",
              background: `radial-gradient(circle, ${hexAlpha(o.color, alpha)} 0%, ${hexAlpha(o.color, 0)} 70%)`,
            } as CSSProperties
          }
        />
      ))}
    </div>
  );
}

export function hexAlpha(hex: string, alpha: number): string {
  const h = hex.replace("#", "");
  const full = h.length === 3 ? h.split("").map((c) => c + c).join("") : h;
  const r = parseInt(full.slice(0, 2), 16);
  const g = parseInt(full.slice(2, 4), 16);
  const b = parseInt(full.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

/* ------------------------------ Starfield ------------------------------ */

/** Deterministic star positions (module scope keeps render pure). */
function seededStars(count: number, seed: number) {
  let s = seed * 9301 + 49297;
  const rnd = () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
  return Array.from({ length: count }, () => ({
    x: rnd() * 100,
    y: rnd() * 100,
    size: 1 + rnd() * 1.8,
    dur: 3 + rnd() * 4,
    delay: rnd() * 4,
  }));
}

/** Twinkling stars for night-time gradients (Starfield). */
export function Starfield({ count = 28, seed = 7 }: { count?: number; seed?: number }) {
  const stars = useMemo(() => seededStars(count, seed), [count, seed]);
  return (
    <div className="absolute inset-0 overflow-hidden rounded-[inherit]" aria-hidden>
      {stars.map((st, i) => (
        <div
          key={i}
          className="star"
          style={
            {
              left: `${st.x}%`,
              top: `${st.y}%`,
              width: st.size,
              height: st.size,
              "--tw-dur": `${st.dur}s`,
              "--tw-delay": `${st.delay}s`,
            } as CSSProperties
          }
        />
      ))}
    </div>
  );
}

/* ------------------------------ Rolling digits ------------------------------ */

/** Odometer-style rolling digits (RollingText). */
export function RollingDigits({
  text,
  className,
}: {
  text: string;
  className?: string;
}) {
  return (
    <span className={`inline-flex items-baseline ${className ?? ""}`} aria-label={text}>
      {text.split("").map((ch, i) =>
        /\d/.test(ch) ? (
          <span key={i} className="relative inline-block overflow-hidden" style={{ height: "1.1em" }}>
            <AnimatePresence mode="popLayout" initial={false}>
              <motion.span
                key={ch}
                className="inline-block tabular-nums"
                initial={{ y: "-105%", opacity: 0 }}
                animate={{ y: "0%", opacity: 1 }}
                exit={{ y: "105%", opacity: 0 }}
                transition={{ duration: 0.26, ease: [0.33, 1, 0.68, 1] }}
              >
                {ch}
              </motion.span>
            </AnimatePresence>
          </span>
        ) : (
          <span key={i} className="tabular-nums">
            {ch}
          </span>
        )
      )}
    </span>
  );
}

/* ------------------------------ Countdown ring ------------------------------ */

/** Countdown ring with sweep gradient, glowing head and a slow "breathe". */
export function CountdownRing({
  progress,
  label,
  accent,
}: {
  progress: number;
  label: string;
  accent: string;
}) {
  const clamped = Math.min(1, Math.max(0, progress));
  const size = 132;
  const stroke = 10;
  const r = (size - stroke) / 2 - 2;
  const c = 2 * Math.PI * r;
  const angle = clamped * 360;
  const rad = ((angle - 90) * Math.PI) / 180;
  const headX = size / 2 + r * Math.cos(rad);
  const headY = size / 2 + r * Math.sin(rad);
  const gid = `ring-grad-${accent.replace("#", "")}`;
  // Draw the arc only after mount so the first paint starts from zero.
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const shown = mounted ? clamped : 0;

  return (
    <div
      className="breathe relative shrink-0"
      style={{ width: size, height: size, "--breathe-min": 0.985, "--breathe-max": 1.015, "--breathe-dur": "3.2s" } as CSSProperties}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden>
        <defs>
          <linearGradient id={gid} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={accent} />
            <stop offset="50%" stopColor="#FFFFFF" />
            <stop offset="100%" stopColor={accent} />
          </linearGradient>
          <radialGradient id={`${gid}-bg`} cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="rgba(255,255,255,0.14)" />
            <stop offset="100%" stopColor="rgba(255,255,255,0)" />
          </radialGradient>
        </defs>
        <circle cx={size / 2} cy={size / 2} r={size / 2} fill={`url(#${gid}-bg)`} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="rgba(255,255,255,0.2)"
          strokeWidth={stroke}
          strokeLinecap="round"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke={`url(#${gid})`}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - shown)}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{ transition: "stroke-dashoffset 0.9s cubic-bezier(0.33, 1, 0.68, 1)" }}
        />
        {shown > 0.006 && (
          <g>
            <circle cx={headX} cy={headY} r={stroke * 0.9} fill="rgba(255,255,255,0.55)" />
            <circle cx={headX} cy={headY} r={stroke * 0.42} fill="#FFFFFF" />
          </g>
        )}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <RollingDigits text={label} className="text-xl font-bold tracking-wide text-white" />
        <span className="text-[10px] font-medium text-white/75">remaining</span>
      </div>
    </div>
  );
}

/* ------------------------------ Press feedback ------------------------------ */

/** Tactile press-scale feedback (pressScale). */
export function Pressable({
  className,
  children,
  onClick,
  role,
  ariaLabel,
  style,
}: {
  className?: string;
  children: ReactNode;
  onClick?: () => void;
  role?: string;
  ariaLabel?: string;
  style?: CSSProperties;
}) {
  return (
    <motion.div
      className={className}
      role={role}
      aria-label={ariaLabel}
      style={style}
      whileTap={{ scale: 0.97 }}
      transition={{ type: "spring", stiffness: 400, damping: 22 }}
      onClick={onClick}
    >
      {children}
    </motion.div>
  );
}
