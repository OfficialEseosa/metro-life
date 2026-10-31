/**
 * The HUD palette follows the game clock, but the start screens keep the day palette so a
 * dialog never restyles while the player is filling it in.
 */
let night = false,
  held = 0;
function apply() {
  document.documentElement.dataset.time = night && !held ? "night" : "day";
}
export function setNightTheme(value: boolean) {
  night = value;
  apply();
}
/** Holds the day palette until the returned release is called. */
export function holdDayTheme() {
  held++;
  apply();
  return () => {
    held--;
    apply();
  };
}
