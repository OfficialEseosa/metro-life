# Reference study

## M1 — captured mechanics and API structure


Observed in `31rkf5nfrt4lz.js` and `2wn1at3jbyiyr.js`: activities are data records with duration, costs, need deltas, social tags and poses; prerequisite logic rejects unavailable actions before effects; mood derives from needs and modifiers; skill/need rewards and travel effects are applied centrally. These are static observations, not claims of runtime behavior. The large bundle contains many unrelated features; this is a bounded inspection.

Metro Life choices: keep four needs and seven skills, expand canonical labels into explicit server-side actions, make busy time short and visible, cap offline decay, and persist user/world state. The backend owns validation rather than trusting browser effects. Guest upgrades keep the same user and world. Do not reuse source dialogue, user names, ads, emoji or place content.

## M2 — camera, lighting and map



## M3 — panels, needs, actions and onboarding


Metro Life choices: one clear play entry, a compact world picker, one-page city/home/dream/look setup, and immediate direction to an entry-level job. Keep needs numeric with semantic meters and visible labels. Requirements and closed hours stay next to actions; busy time has a progress indicator and blocks duplicates. Results describe effects and remain dismissible. Server state replaces browser-calculated rewards, and a focus-managed story dialog presents pending choices. No source wording or visual icons copied.

## M4 — travel and map viewing


Metro Life choices: free map viewing has a persistent "visiting this map" notice when the sim is elsewhere. Flights are canonical airport actions with a visible $180 price and short busy state. The authoritative response changes the sim city and the map, preserving home/job/world. New York water divides Manhattan/Brooklyn; Bay water separates the peninsula and East Bay. Every new model has a sheet before geometry; no original place names, dialogue or ad artwork are reused.

M5–M7 studies will be added at each milestone start.

## Owner HUD redesign — source study before implementation
