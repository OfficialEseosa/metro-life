# Metro Life roadmap

## Status

Playable: quick start or the full creator, arrival, walking, jobs and shifts, needs, meals and groceries, furnishing, stories, aspirations, elections, plots and billboards, travel between three cities, shared presence, chat, emotes and city events.

Second round of fixes has landed: one-time live frames (waves, gifts, chat, events) are durable and no longer lost when a page falls behind, home furniture is preloaded, poses are fixed, the economy is rebalanced so one shift a day can carry the first few homes, and the phone social row and toasts are fixed.

Pacing: rent is now taken every third game day and needs fall at less than half the old rate, so an idle hour leaves a new life with most of its cash and every need out of the red.

Verified: the seven Playwright journeys (first start, resume after reload, two players, outage and reconnect, paid shift, two-way social visit, flight) pass at 1366x768 and at 667x375 with touch on the pacing build. The last pass added a 1.2 s minimum pose hold for short actions, a fixed day palette on the start screens with an eased day/night swap in play, and landscape-phone and safe-area fixes for the creator and city choice; journeys a and e (laptop) and f (phone) pass on it, but the phone layouts were not viewed by eye. The frontend builds and lints with no errors and the backend packages as an API-only JAR. The frontend is live on Vercel and the API on Fly.io; the steps are in `docs/deploy.md`.

Independent play review against The Sims 4, Animal Crossing, Two Point Hospital and Tiny Glade, scored out of 10. It was played on a software renderer at both viewports, and all 14 end-to-end journeys passed (7 journeys times 2 viewports). Overall about 7.1 out of 10 against a target of 8.5.

| Characters | World and lighting | UI | Feedback and sound | Loop and pacing | Performance and robustness | First five minutes |
|---|---|---|---|---|---|---|
| 6.5 | 6.5 | 8 | 7 | 7 | 7 | 7.5 |

## Next

1. Look at the creator and city choice on a real phone in landscape (568x320, 734x343) and check the start-screen palette; those rules landed without a screen check.
2. Listen to the mix on speakers. Levels were set by measurement; the coin cue sits about 4 dB over the bed rather than 6.
3. Load the title-screen character preview lazily, so the title paints before three.js loads, especially on phones.
4. Check rendering and frame rate on a real GPU and phone.

## Known issues

- A reload during journey b once aborted one model fetch. The journey still passed and it was not reproduced.
- A reload at laptop size just after arrival once left the 3D view black. It has not come back since.
- Residents walking through a door pass through the spot where a player stands.
- Lint reports React warnings (no errors) in 14 scene and UI files, most in `social-life.tsx` and `walking.tsx`. three.js warns that `THREE.Clock` is deprecated.
- The upgrade home is the same studio as the starter.
- On phones the amount that floats over the character (for example the -$15 rent) is not labelled; the money journal names it.
- Escape only leaves a big room from the door.
- Story beats start after an activity at the place, not on arrival.
- The phone home camera puts the room under the HUD cards.
- A stacked phone toast can cover the More button.
- Copy nits: a duplicated workplace line, Review move offered for the current home, Charisma 1 vs 2 on the election button, and the plot Book button needs a title silently.
- Other online players render as full rigs with shadows.
