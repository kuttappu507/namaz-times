"use client";

/** Floating pill navigation bar with a sliding highlight (FloatingNavBar in MainActivity.kt). */
import { motion } from "framer-motion";
import { CalendarDays, Settings, Sunrise } from "lucide-react";
import type { LucideIcon } from "lucide-react";

const TABS: { label: string; icon: LucideIcon }[] = [
  { label: "Today", icon: Sunrise },
  { label: "Monthly", icon: CalendarDays },
  { label: "Settings", icon: Settings },
];

const TAB_W = 96;
const TAB_H = 54;

export function FloatingNavBar({
  selected,
  onSelect,
}: {
  selected: number;
  onSelect: (index: number) => void;
}) {
  return (
    <nav
      aria-label="Main navigation"
      className="bottom-safe-nav fixed left-1/2 z-50 -translate-x-1/2 rounded-full"
      style={{
        filter: "drop-shadow(0 10px 24px rgba(11,122,91,0.30))",
      }}
    >
      <div
        className="relative flex rounded-full p-[5px]"
        style={{
          background: "var(--nav-glass)",
          backdropFilter: "blur(18px)",
          WebkitBackdropFilter: "blur(18px)",
          border: "1px solid var(--outline-variant)",
        }}
      >
        {/* Sliding indicator */}
        <motion.div
          className="absolute top-[5px] left-[5px] rounded-full"
          style={{
            width: TAB_W,
            height: TAB_H,
            background:
              "linear-gradient(90deg, var(--primary) 0%, color-mix(in srgb, var(--primary) 82%, transparent) 100%)",
          }}
          animate={{ x: selected * TAB_W }}
          transition={{ type: "spring", stiffness: 340, damping: 34 }}
          aria-hidden
        />
        {TABS.map((tab, i) => {
          const isSel = i === selected;
          const Icon = tab.icon;
          return (
            <button
              key={tab.label}
              type="button"
              onClick={() => onSelect(i)}
              aria-label={tab.label}
              aria-current={isSel ? "page" : undefined}
              className="relative z-10 flex flex-col items-center justify-center gap-[2px] rounded-full"
              style={{ width: TAB_W, height: TAB_H }}
            >
              <motion.span
                animate={{ scale: isSel ? 1.08 : 1 }}
                transition={{ type: "spring", stiffness: 500, damping: 20 }}
                className="flex"
              >
                <Icon
                  size={22}
                  strokeWidth={isSel ? 2.4 : 2}
                  className="transition-colors duration-200"
                  style={{ color: isSel ? "var(--on-primary)" : "var(--on-surface-variant)" }}
                />
              </motion.span>
              <span
                className="text-[11px] font-medium tracking-[0.3px]"
                style={{ color: isSel ? "var(--on-primary)" : "var(--on-surface-variant)" }}
              >
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
