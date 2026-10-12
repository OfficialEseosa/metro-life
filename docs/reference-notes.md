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












### Movement and camera follow

**Two distinct scales of play.** The world map selects places and starts timed trips with a transport choice. It is not a freely walking street-level avatar in the captured implementation. `sN` creates travel state containing origin, destination, mode, total and elapsed time; `sP` changes location on arrival. `rp` interpolates a vehicle between the two lots using elapsed/total. The world camera is attached to MapControls, with panning/zoom and rotation disabled, rather than following a player position. Selecting a place is separate from travelling there.

Inside a venue, the UI mounts a separate `Venue3D`. Its `onMove` writes local x/z/facing, while floor taps clear the queue and write a new local destination/facing. The action processor advances `vpos` toward the selected station's spot before starting the action. At home, an actual waypoint path advances `pos`, shifts completed waypoints and turns facing in the movement direction. Thus local avatar movement and clicking a destination are supported by the captured code. A continuous walking journey through the exterior city is not established. Keyboard walking, joystick behavior, the avatar's rendered walk cycle and the venue/home camera's following behavior remain unverified; keyboard handlers visible in the wrapper primarily select stations and panels.


### Buildings, rooms and actions

**Yes: separate venue and home views exist in the shipped UI/code.** The venue view has an entry point, stations, NPCs, generated crowd, other players, speech and floor/station picking. The home logic has rooms, furniture, object targets, blocked-path handling and queued actions. This is a scene/view transition after arrival, not proof of a seamless doorway into every exterior building. Venue definitions include outdoor places as well as interiors.

Actions use both spatial presentation and panels: a station tap opens its action list; choosing an action queues it; local movement reaches the target before timed progress begins. Action definitions carry pose/bubble/needs metadata, including sitting, talking and sleeping. That establishes the intended pose-driven character response alongside progress and choice panels, but the exact interior appearance and finished animation cannot be claimed from the anonymous landing page. The lazy `Venue3D` implementation is absent from the captured files; its exact public chunk URL returned HTTP403 to an unauthenticated read. We did not bypass access or create an account.


### Default camera and visual density

The world-map camera defaults to position `[3.75,37.5,26.25]`, perspective FOV38degrees, near1/far700. Its target is `[1.5*CITY,0,CITY]`; the captured scale constant is `CITY=1.5`, so target `[2.25,0,1.5]`. Derived distance is44.956scene units, with56.527degrees of elevation above the ground. A supplied focused view uses height18 by default and z offset0.66times height. Rotation is disabled; minimum orbit distance8, with responsive maximum distances. These are original scene units and initial defaults, not a runtime measurement of an authenticated player's camera.

The inspected1280x720 anonymous capture has broad lawn/venue pads with small buildings and narrow roads, tight repeated housing rows on the right, many large upright billboards distributed through the centre/edges, and a packed rectangular sea-ad field. A wide horizontal water band separates the two major land areas; another channel runs down the right. Water fills the scene beyond the land, rather than ending in an empty backdrop. The code's base water plane is440x360 units. Billboards are unusually large relative to buildings: the three defined face sizes are4.8x2.4,3.4x1.7 and2.6x1.3. Housing repetition, ads, roads and water supply much of the apparent density; the central venue pads themselves have open spacing.

Evidence: `3hix095xid-pq.js` `camera:{position:`, MapControls target, ground/water component following `rt=`, and bounded WorldMap record above; `1tnl62xgfe1fa.js` exported CITY and BOARD_SIZE values. Live capture Evidence above and `original-live-verified.png`. Camera-value semantic probes had zero seed matches and are retained as limitations; the numbers come from the original source and arithmetic, not those failed matches.

### What makes the city busy

**Ads, repeated development, traffic and local crowds work together.** The two inspected passive captures show different billboard creative at the same camera without input. Captured billboard code schedules rotation every15s with450ms fades. Ads occur on elevated roadside boards and dense sea plots; none of their artwork/text is reused. The car system loads four GLB vehicle types, merges/instances geometry for a dense stationary traffic population, then animates a small separate loop (six vehicles on laptop/four on phone). Standing figures and walking paths add localized ambient life, particularly around the airport. Venue wrappers receive named NPCs, deterministic crowd and live players, with speech/social picking. World-map housing labels filter to online/selected homes. The displayed global online number does not prove that many visible avatars are rendered, nor was server-side presence accuracy independently checked.

Evidence: `3hix095xid-pq.js` `t1`, `t9`, `se`, `Dummies`, `Walkers`, online-house filter and billboard field; `1tnl62xgfe1fa.js` rotation/fade constants; `2a6i2oyqx4umq.js` venue `people/crowd/players/mySay` props. Bounded traffic record `ev_e74301d83544d3c980e2d99a12dc3eb7cb89c51b0fd2c4412768be0c5367b797` and airport crowd `ev_26aadc93af6f955e154e9b46732c4b58d1e1edc935b9237b274de0b8e69f0ba6`, with exact source ranges in feature-manifest.json. This supports a presentation model, not claims about autonomous resident AI or actual concurrency.


## V2: creating a life in the shared city


Direct original-source inspection of the Randomise callback confirms a two-body choice and separate hair/style/skin/hair-colour/top/bottom palettes, with name randomisation when the user has not supplied one. Adjacent syntax shows staged progress indicators. This establishes static UI structure, not live creator execution. Metro Life uses the same principle of visible appearance choices with a shared3Dmodel preview, but only original casual/street starting outfits, new US city choices, pronouns, a150-dollar studio start and an original four-card arrival. It deliberately removes login/world selection: every browser joins the same local public city. None of the reference names, outfits, copy or images are copied.
