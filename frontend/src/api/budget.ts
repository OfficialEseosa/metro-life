import type { HomeBudget, HomeUpgrade, Sim } from "./types";

type Forecast = {
  expectedPay?: number;
  food?: number;
  homes?: HomeBudget[];
  upgrade?: HomeUpgrade | null;
};

const forecast = (sim: Sim | null | undefined) =>
  (sim?.economy?.forecast ?? null) as Forecast | null;

const dollars = (n: number) => `$${Math.round(Math.abs(n))}`;

/** The server's daily budget for one neighbourhood of the player's home city, if listed. */
export function homeBudget(
  sim: Sim | null | undefined,
  neighborhood: string,
): HomeBudget | null {
  return (
    forecast(sim)?.homes?.find((h) => h.neighborhood === neighborhood) ?? null
  );
}

/** The home worth moving to next, or null while no dearer home is sustainable on one shift a day. */
export function homeUpgrade(sim: Sim | null | undefined): HomeUpgrade | null {
  return forecast(sim)?.upgrade ?? null;
}

/**
 * One line for a move review, e.g. "Each day here: +$21 after a $70 shift, $25 rent and $24 meals."
 * Empty without a job (there is no shift to count on).
 */
export function budgetLine(sim: Sim | null | undefined, neighborhood: string) {
  const f = forecast(sim),
    home = homeBudget(sim, neighborhood);
  if (!f || !home || !f.expectedPay) return "";
  const sign = home.dailyNet >= 0 ? "+" : "-";
  return `Each day here: ${sign}${dollars(home.dailyNet)} after a ${dollars(f.expectedPay)} shift, ${dollars(home.rent)} rent and ${dollars(f.food ?? 0)} meals.`;
}
