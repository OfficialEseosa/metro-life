import { useStore } from "../store";
import { money } from "./helpers";
import "./money-ledger.css";
export type MoneyEntry = {
  id: string;
  at: number;
  day: number;
  amount: number;
  reason: string;
  balance: number;
  clock?: { hour: number; minute: number };
};
export type EconomySnapshot = {
  entries: MoneyEntry[];
  forecast: {
    rent: number;
    /** Rent falls due once every this many game days. */
    rentEveryDays?: number;
    food: number;
    expectedPay: number;
    /** One game day on average: one shift, minus that day's share of the rent, minus two meals out. */
    dailyNet: number;
    nextRentDay?: number;
    nextRentAt: number;
    rentDebt: number;
    lowRent: boolean;
    warning: string;
    payAssumption: string;
  };
  ratesPerGameHour: Record<string, number>;
};
export function MoneyLedger() {
  const sim = useStore((s) => s.sim) as ReturnType<
    typeof useStore.getState
  >["sim"] & { economy?: EconomySnapshot };
  if (!sim) return null;
  const economy = sim.economy,
    f = economy?.forecast;
  return (
    <section className="money-ledger" aria-label="Money journal">
      <h3>Your daily budget</h3>
      {f ? (
        <>
          <dl>
            <div>
              <dt>
                Rent every {f.rentEveryDays ?? 3} days
                {f.nextRentDay ? ` · next on day ${f.nextRentDay}` : ""}
              </dt>
              <dd>{money(f.rent)}</dd>
            </div>
            <div>
              <dt>Meals estimate per day</dt>
              <dd>{money(f.food)}</dd>
            </div>
            <div>
              <dt>One shift estimate</dt>
              <dd>+{money(f.expectedPay)}</dd>
            </div>
            <div>
              <dt>Average change per game day</dt>
              <dd>
                {f.dailyNet >= 0 ? "+" : ""}
                {money(f.dailyNet)}
              </dd>
            </div>
          </dl>
          <small>{f.payAssumption}</small>
          {f.rentDebt > 0 && (
            <p className="rent-warning">Unpaid rent: {money(f.rentDebt)}</p>
          )}
          {f.lowRent && <p className="rent-warning">{f.warning}</p>}
        </>
      ) : (
        <p>Opening your money journal…</p>
      )}
      <h3>Money in and out</h3>
      <ol>
        {economy?.entries
          .slice()
          .reverse()
          .map((entry) => (
            <li key={entry.id}>
              <div>
                <b>{entry.reason}</b>
                <small>
                  Day {entry.day}
                  {entry.clock
                    ? ` · ${String(entry.clock.hour).padStart(2, "0")}:${String(entry.clock.minute).padStart(2, "0")}`
                    : ""}
                </small>
              </div>
              <strong className={entry.amount < 0 ? "spent" : "earned"}>
                {entry.amount >= 0 ? "+" : ""}
                {money(entry.amount)}
              </strong>
            </li>
          ))}
      </ol>
    </section>
  );
}
export function RentWarning() {
  const sim = useStore((s) => s.sim) as ReturnType<
    typeof useStore.getState
  >["sim"] & { economy?: EconomySnapshot };
  const warning = sim?.economy?.forecast;
  if (!warning?.lowRent) return null;
  return (
    <p className="rent-warning" role="status">
      Rent of {money(warning.rent)} is due
      {warning.nextRentDay ? ` on day ${warning.nextRentDay}` : " soon"}. Your
      balance may not cover rent and meals.
    </p>
  );
}
