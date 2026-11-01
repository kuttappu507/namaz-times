/** Time-of-day sky palettes for the hero card — mirrors Color.kt (SkyPalette). */

export interface SkyPalette {
  top: string;
  mid: string;
  bottom: string;
  glow: string;
  accent: string;
}

export const SKY_NIGHT: SkyPalette = { top: "#0B1630", mid: "#15264F", bottom: "#223B6E", glow: "#4A6DB0", accent: "#F5C451" };
export const SKY_DAWN: SkyPalette = { top: "#2A2E62", mid: "#7A4B7C", bottom: "#E88A64", glow: "#FFB27A", accent: "#FFE7A6" };
export const SKY_MORNING: SkyPalette = { top: "#06483A", mid: "#0E8A66", bottom: "#3FBF93", glow: "#8CE7C2", accent: "#F5C451" };
export const SKY_NOON: SkyPalette = { top: "#0B5F49", mid: "#14A47A", bottom: "#61CFA6", glow: "#C9F5E4", accent: "#F5C451" };
export const SKY_AFTERNOON: SkyPalette = { top: "#14524A", mid: "#2C8A72", bottom: "#D9A62F", glow: "#FFD873", accent: "#FFFFFF" };
export const SKY_DUSK: SkyPalette = { top: "#2E1B4E", mid: "#7A3A6D", bottom: "#E5643F", glow: "#FF9C6B", accent: "#FFE7A6" };
export const SKY_EVENING: SkyPalette = { top: "#14183A", mid: "#2B2F6B", bottom: "#4B3F8C", glow: "#7C6CC7", accent: "#F5C451" };

/** Palette for a given local hour of day. */
export function skyPaletteFor(hour: number): SkyPalette {
  if (hour >= 4 && hour <= 5) return SKY_DAWN;
  if (hour >= 6 && hour <= 10) return SKY_MORNING;
  if (hour >= 11 && hour <= 14) return SKY_NOON;
  if (hour >= 15 && hour <= 17) return SKY_AFTERNOON;
  if (hour === 18) return SKY_DUSK;
  if (hour >= 19 && hour <= 21) return SKY_EVENING;
  return SKY_NIGHT;
}

/** Per-prayer accent gradient used on badges (Common.kt). */
export const PRAYER_GRADIENT: Record<string, [string, string]> = {
  FAJR: ["#5B6CD6", "#9A7BD9"],
  SUNRISE: ["#F7A54B", "#FFD36B"],
  ZUHR: ["#16A37A", "#5CD6AA"],
  ASR: ["#D9A62F", "#F2CB6A"],
  MAGHRIB: ["#E5643F", "#F59A6B"],
  ISHA: ["#2B3A8C", "#5B6CD6"],
};
