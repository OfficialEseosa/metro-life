import { useEffect, useState } from "react";
import { X } from "@phosphor-icons/react";
import { useStore } from "../store";
import { Icon } from "./primitives";
import { NEED_ICONS, NEED_LABELS } from "./needs";
import { sound } from "./sound";

/** What helps each need, in the player's words. Each line stays under 60 characters with the pay note. */
const ADVICE: Record<
  string,
  { text: string; action: string; to: "home" | "eat" | "fun" | "none" }
> = {
  energy: {
    text: "Get some sleep in your bed.",
    action: "Go home",
    to: "home",
  },
  hunger: { text: "Grab something to eat.", action: "Find food", to: "eat" },
  fun: { text: "Do something fun.", action: "Find fun", to: "fun" },
  social: { text: "Talk to someone nearby.", action: "", to: "none" },
  hygiene: { text: "Take a shower at home.", action: "Go home", to: "home" },
};
const PAY_NOTE = "Low needs reduce your pay.";

/** A critical need, five seconds after it drops below 15. Dismissable; never stacks over other news. */
export function NeedWarning() {
  const social = useStore((s) => !!s.socialNotice),
    critical = useStore((s) =>
      s.sim
        ? (Object.entries(s.sim.needs)
            .sort((a, b) => a[1] - b[1])
            .find(([, value]) => value < 15)?.[0] ?? "")
        : "",
    ),
    blocked = useStore(
      (s) =>
        !!s.lastResult ||
        !!s.sim?.pendingBeat ||
        ["creator", "settings", "city"].includes(s.panel ?? ""),
    ),
    [shown, setShown] = useState(""),
    dismissed = useStore((s) => s.needWarningDismissed),
    setDismissed = useStore((s) => s.dismissNeedWarning);
  useEffect(() => {
    const timer = setTimeout(
      () => {
        setShown(critical);
        if (!critical) setDismissed("");
        if (critical) sound("low");
      },
      critical ? 5000 : 0,
    );
    return () => clearTimeout(timer);
  }, [critical, setDismissed]);
  if (
    !critical ||
    shown !== critical ||
    dismissed === critical ||
    blocked ||
    social
  )
    return null;
  const advice = ADVICE[critical] ?? {
    text: "Look after yourself.",
    action: "",
    to: "none" as const,
  };
  return (
    <div className={`hud-toast need-warning ${critical}`} role="status">
      <span className={`semantic-tile ${critical}`}>
        <Icon name={NEED_ICONS[critical] ?? "warning"} />
      </span>
      <div>
        <strong>{NEED_LABELS[critical] ?? critical} is very low</strong>
        <p>
          {advice.text} {PAY_NOTE}
        </p>
        {advice.action && (
          <button
            className="text-button"
            onClick={() => {
              setDismissed(critical);
              const store = useStore.getState();
              if (advice.to === "home") {
                store.selectLocation("home");
                store.setFocus("home");
              } else {
                store.setMapLens(advice.to === "eat" ? "eat" : "fun");
                store.setSearchOpen(true);
              }
            }}
          >
            {advice.action}
          </button>
        )}
      </div>
      <button
        className="icon-button"
        aria-label="Dismiss need warning"
        onClick={() => setDismissed(critical)}
      >
        <X size={20} weight="bold" />
      </button>
    </div>
  );
}
