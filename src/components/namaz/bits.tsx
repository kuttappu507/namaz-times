"use client";

/** GlassCard, Pill and SectionHeader — ported from ui/components/Common.kt. */
import type { CSSProperties, ReactNode } from "react";

/** The app's standard elevated surface: gradient, hairline border, soft shadow. */
export function GlassCard({
  className,
  style,
  children,
  rounded = 28,
}: {
  className?: string;
  style?: CSSProperties;
  children: ReactNode;
  rounded?: number;
}) {
  return (
    <div
      className={`rounded-[28px] ${className ?? ""}`}
      style={{
        borderRadius: rounded,
        background: `linear-gradient(180deg, var(--card-grad-start), var(--card-grad-end))`,
        border: "1px solid color-mix(in srgb, white 60%, var(--outline-variant))",
        boxShadow: "0 10px 28px rgba(11, 122, 91, 0.12)",
        ...style,
      }}
    >
      {children}
    </div>
  );
}

/** Small rounded label such as "NOW", "NEXT" or "UP NEXT". */
export function Pill({
  text,
  container,
  content,
  outlined = false,
}: {
  text: string;
  container: string;
  content: string;
  outlined?: boolean;
}) {
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-[3px] text-[13px] font-semibold tracking-[1px] whitespace-nowrap"
      style={
        outlined
          ? { border: `1px solid ${content}`, color: content }
          : { background: container, color: content }
      }
    >
      {text}
    </span>
  );
}

/** Section heading with a small gold rule. */
export function SectionHeader({
  title,
  trailing,
}: {
  title: string;
  trailing?: ReactNode;
}) {
  return (
    <div className="flex items-center justify-between px-1">
      <div>
        <h2 className="text-[20px] font-semibold" style={{ color: "var(--on-surface)" }}>
          {title}
        </h2>
        <div
          className="mt-1 h-[3px] w-7 rounded-full"
          style={{
            background:
              "linear-gradient(90deg, var(--gold), color-mix(in srgb, var(--gold) 0%, transparent))",
          }}
          aria-hidden
        />
      </div>
      {trailing}
    </div>
  );
}
