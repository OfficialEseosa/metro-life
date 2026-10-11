# Metro Life living plan

## Progress
- [x] Preserve old game; replace checkout with Metro Life sources.
- [ ] M1 Backend core: auth, world isolation, content expansion, sims/actions/decay, persistence and tests.
- [ ] M2 Atlanta scene, architecture sheets, visual evidence and measured performance.
- [ ] M3 HUD, onboarding and playable core loop.
- [ ] M4 New York and Bay Area, landmark evidence and airport travel.
- [ ] M5 Careers, aspirations, daily goals and story chains.
- [ ] M6 Elections, policy, plots atlas, billboards, homes and rent.
- [ ] M7 Live multiplayer, social and simulated residents.
- [ ] M8 Mobile/accessibility polish, full verification and final report.

## Decisions and setup
- Copied source with robocopy /E /XD .git, including frontend/node_modules; source folder remains untouched. Remote changed locally to https://github.com/OfficialEseosa/metro-life.git. No network Git operations performed.
- Backend will use a versioned JSON store with atomic replacement and PBKDF2 passwords. Server owns rules; sessions persist across restart. Assets remain local.

## Commands and actual results
- git status --short: clean before setup; last commit f86a168.
- git branch dusk-dusk-goose; git tag dusk-dusk-goose-final: passed.
- Move-Item research to archive: passed.
- git rm -r --quiet -- .: passed.
- Move-Item remaining old untracked folders to archive: passed.
- robocopy source checkout /E /XD .git: exit 1 (successful copy).
- git remote set-url origin https://github.com/OfficialEseosa/metro-life.git: passed.

## M1 in progress
- Read frontend API types/client/store/App and backend starter. Stale v1 Java sources will be replaced under app.metrolife.
- Audit folder check pending after setup.
- No runtime check has passed yet. No local Metro Life server running yet.
