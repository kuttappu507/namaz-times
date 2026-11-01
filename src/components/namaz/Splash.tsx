"use client";

/**
 * Animated intro shown once per session — the web counterpart of SplashScreen.kt.
 * Crescent draws itself, fills with glow, a gold star pops in, the title letters
 * cascade up, then the whole screen zooms and cross-fades into the app (~2s).
 */
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { AmbientOrbs, Starfield } from "./effects";

const TITLE = "Namaz Times";
const TAGLINE = "Hubli · Karnataka";

export function Splash({ onFinished }: { onFinished: () => void }) {
  const [draw, setDraw] = useState(0);
  const [fill, setFill] = useState(0);
  const [glow, setGlow] = useState(0);
  const [star, setStar] = useState(0);
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    const ids: ReturnType<typeof setTimeout>[] = [];
    ids.push(setTimeout(() => setDraw(1), 120));
    ids.push(setTimeout(() => { setFill(1); setGlow(1); }, 700));
    ids.push(setTimeout(() => setStar(1), 850));
    ids.push(setTimeout(() => setExiting(true), 1850));
    ids.push(setTimeout(onFinished, 2350));
    return () => ids.forEach(clearTimeout);
  }, [onFinished]);

  return (
    <motion.div
      className="fixed inset-0 z-[80] flex items-center justify-center"
      style={{
        background: "linear-gradient(180deg, #0A1410 0%, #0A1F1A 55%, #06483A 130%)",
      }}
      animate={exiting ? { opacity: 0, scale: 1.08 } : { opacity: 1, scale: 1 }}
      transition={{ duration: exiting ? 0.45 : 0.3, ease: "easeInOut" }}
      aria-hidden
    >
      <AmbientOrbs colors={["#0B7A5B", "#19B58A", "#F5C451"]} alpha={0.28} seed={3} />
      <Starfield count={60} seed={11} />

      <div className="relative flex flex-col items-center">
        {/* Crescent + star */}
        <div className="relative" style={{ width: 128, height: 128 }}>
          {/* glow bloom */}
          <motion.div
            className="absolute rounded-full"
            style={{
              inset: -18,
              background: "radial-gradient(circle, rgba(245,196,81,0.35) 0%, rgba(245,196,81,0) 70%)",
            }}
            animate={{ opacity: glow }}
            transition={{ duration: 0.9, ease: "easeOut" }}
          />
          <svg viewBox="0 0 128 128" width={128} height={128}>
            <defs>
              <linearGradient id="splash-crescent" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#F5C451" />
                <stop offset="100%" stopColor="#B8891A" />
              </linearGradient>
              <mask id="splash-crescent-mask">
                <rect width="128" height="128" fill="black" />
                {/* outer circle minus offset inner circle = crescent */}
                <circle cx="60" cy="64" r="44" fill="white" />
                <circle cx="82" cy="58" r="40" fill="black" />
              </mask>
            </defs>
            <g mask="url(#splash-crescent-mask)">
              <motion.circle
                cx="60"
                cy="64"
                r="44"
                fill="none"
                stroke="url(#splash-crescent)"
                strokeWidth={5}
                strokeLinecap="round"
                strokeDasharray={276.5}
                animate={{ strokeDashoffset: 276.5 * (1 - draw) }}
                transition={{ duration: 0.6, ease: "easeInOut" }}
              />
              <motion.circle
                cx="60"
                cy="64"
                r="42.5"
                fill="url(#splash-crescent)"
                animate={{ opacity: fill }}
                transition={{ duration: 0.35 }}
              />
            </g>
            {/* gold star */}
            <motion.g
              initial={{ scale: 0, opacity: 0 }}
              animate={star ? { scale: 1, opacity: 1 } : {}}
              transition={{ type: "spring", stiffness: 320, damping: 12 }}
              style={{ transformOrigin: "96px 34px" }}
            >
              <StarPath x={96} y={34} r={16} fill="#FFE7A6" />
            </motion.g>
          </svg>
        </div>

        {/* Title cascade */}
        <div className="mt-7 flex items-baseline overflow-hidden">
          {TITLE.split("").map((ch, i) => (
            <motion.span
              key={i}
              className="text-4xl font-bold tracking-tight text-[#E6EFEA]"
              initial={{ y: 30, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.95 + i * 0.045, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
            >
              {ch === " " ? "\u00A0" : ch}
            </motion.span>
          ))}
        </div>
        <motion.p
          className="mt-1 text-sm font-medium tracking-[0.2em] text-[#8FE3C6]"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.55, duration: 0.5 }}
        >
          {TAGLINE}
        </motion.p>
      </div>
    </motion.div>
  );
}

function StarPath({ x, y, r, fill }: { x: number; y: number; r: number; fill: string }) {
  // 5-point star
  const points: string[] = [];
  for (let i = 0; i < 10; i++) {
    const radius = i % 2 === 0 ? r : r * 0.42;
    const angle = (Math.PI / 5) * i - Math.PI / 2;
    points.push(`${x + radius * Math.cos(angle)},${y + radius * Math.sin(angle)}`);
  }
  return <polygon points={points.join(" ")} fill={fill} />;
}
