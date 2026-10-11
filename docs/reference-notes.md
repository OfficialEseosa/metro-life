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

## M5 — careers and chained choices
Metro keeps SPEC's simpler five levels, visible shifts/skills requirements, and completion-time promotions. Seven distinct six-step aspirations use durable claims, preventing switching dreams from repeating rewards. It does not copy internal grades or long tenure gates.
Metro uses original three-beat city stories triggered by completed activities. A labelled skill route pays more; a collaborative route needs no skill level. Missed checks still continue with practice XP. Server validation protects pending IDs, choices and once-only outcomes, scoped by world. No captured dialogue, names or advertiser content reused. Live pacing, original server authority and original persistence were not established by this static study.


## M6 — elections, property and local messages

Civic source observations: reference/api/gov.json separates holder/term/next ballot and caller eligibility. 010h9d9p9x0xe.js ~47928 wraps government read/run/vote/policy/announcement routes. 0nz0lvdmb76je.js ~29500 and ~40100 presents current holder/effect/countdown, candidate portraits/votes/selection, run slogan validation, and officeholder-only controls. 1tnl62xgfe1fa.js ~44500 defines modest modifiers; 2wn1at3jbyiyr.js rO (~153747) checks policy expiry and ordinary pay/cost paths apply effects. Static searches did not establish an activity that directly adds campaign votes. Metro deliberately adds the specified authoritative once-per-game-day campaign support, one vote per term and first-on-ballot tie rule. No registration fee; Charisma2 remains the gate.
Property observations: 3k7uwe7i5z19-.js ~40000 defines rectangle bounds/overlap, area price and seven-day duration. 3v9ahdlnp9txk.js ~101400 merges booked/blocked/held cells and tap/size selection; sea.json exposes rectangles but no hold TTL/owner, so those guarantees are unknown. 3hix095xid-pq.js ~2400 handles grid/world picking. 0oh2anrw0bror.js ~1200 and 1tnl62xgfe1fa.js ROTATE_MS=15000 show simultaneous rotating billboards; Metro deliberately uses its contract's exclusive scheduled bookings, maximum four per slot, with explicit start time. 2wn1at3jbyiyr.js sF/sH/sW (~199168), payRent (~257605) and rent processing (~289599) show move fees, prepayment and weekly arrears. Metro uses daily rent, a disclosed one-day move fee, nonnegative cash and persisted arrears repaid from later funds. No eviction, external payment, uploads, captured advertiser art/text or hold endpoint. Atomic immediate rectangle bookings expose an empty holds array; overlap is checked under the same server lock. Original server enforcement/expiry behavior was not observed.

## M7 — presence, social and residents
Named reference NPCs at31rkf5nfrt4lz.js305287 and2wn1at3jbyiyr.js158737 have hours/haunts, with deterministic daily availability at up to two venues. 1qbmxl20evrl8.js280402–282900 assigns spots and generates seeded crowd by venue/four-game-hour period; poses/path data occur around253850. Venue UI at2a6i2oyqx4umq.js88674–90250 passes memoized crowd/live players to a missing lazy Venue3D implementation. Actual animation/batching, reference server authorization/persistence and autonomous voting/buying/chat were not established. Metro will deliberately add server-owned residents with one location each, sparse chat and limited civic/property participation, so NPCs cannot flood chat or monopolize plots. It will use a cookie-bound world socket, city filtering, bounded histories, reconnect resync and instanced crowd models.
Spring's official WebSocket API guide (https://docs.spring.io/spring-framework/reference/web/websocket/server.html) requires serialized sends and documents ConcurrentWebSocketSessionDecorator and same-origin defaults. Use bounded outgoing buffers and independent asynchronous sends; no slow client may hold the simulation monitor.
