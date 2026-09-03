# Living Implementation Plan

This is the single mutable implementation plan for the repository. The architecture and
backbone define what the product is; this file defines the milestones, current status, and the
next coherent lane. Do not create a second roadmap, checklist, recovery issue, or competing
sequence. Historical recovery evidence under `docs/recovery/` is not a second authority.

## Canonical references

- `references/ui-authority/` — accepted visual/product authority (inspect before UI work).
- `docs/product-architecture-v1.md` — product ownership and producer boundaries.
- `docs/product-backbone-spec-v1.md` — detailed application/UI/API/custody/security contract.
- `AGENTS.md` — coding/review discipline.

## Product shape

Top-level surfaces: `Home | Research | Explore | Automation | Operate | Settings`.

The five prototype screens in `references/ui-authority/screenshots/` are the definitive structure:
Cockpit Home (hero, Recent Activity, eight numbered cards) and four Research workspaces —
Signals & Models (nine tabs), Evolutionary Search, Test & Validate (six tabs, seven-stage funnel),
Indicators & Models (six pills). The custody workflow
`Idea → Specification → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`
is folded into those workspaces, with Construct modalities Random Discovery, Genetic/Evolutionary
search (native SQX) and Machine Learning / Models (platform-owned). The Assistant (Apollo) is a
bounded, functional card over the backend OpenRouter transport. StrategyQuant X produces the
backtest and its trade records; the cockpit computes the validation verdict.

## Milestone roadmap

Each milestone has a user-visible, packaged-desktop exit criterion. A milestone is not complete
because tests pass; it is complete when the owner can perform the intended path in the real
desktop and it visibly matches the accepted authority.

### M0 — Repository and UI-authority recovery (CURRENT)

- [x] Audit + UI chronology recorded (`docs/recovery/2026-09-01-audit-and-ui-chronology.md`).
- [x] `references/ui-authority/**` restored into the `main` lineage.
- [x] Canonical docs reconciled (README, AGENTS, architecture, backbone) to one product story.
- [x] Milestone roadmap replaces the checkbox plan.
- [ ] PR/branch disposition recorded and actioned by the owner (see recovery disposition doc).
- [ ] `main` branch protection + required checks enabled (owner action).
- [x] Five accepted authority screens committed byte-for-byte under `references/ui-authority/screenshots/` with a truthful pinned manifest; previews regenerated from those PNGs.
- [x] Live-market provider seam added: `/api/market/quotes` (`tc.market-quotes.v1`) with an operator watchlist and truthful `provider_not_configured`; no hard-coded ticker symbols or values remain in the frontend.
- [x] `web/` rebuilt to the five screens: global chrome (rail with workspace/progress/account cards, Data Feeds/Broker/Compute/Automation chips, market ticker, status bar), Cockpit Home board (hero, Recent Activity, eight numbered cards), and the four Research workspaces with their exact tab rows; Explore/Automation/Operate/Settings in the same grammar. All values come from read models; everything without a producer is an explicit not-connected/no-data state.
- [x] Existing read-model binders kept and re-pointed (`researchLocationMatches`, `data-research-host` hooks); native subtree inspectors collapsed into `<details>`; legacy `stage`/`tab` links canonicalise.
- [x] Acceptance rewritten to the prototype: `tests/ui-shell.test.mjs`, browser regression over 28 routes (specification binding, native GA values, catalog blocks, seven validation stages), robustness/proof acceptance through the prototype navigation.
- [x] Assistant made functional: `/api/assistant` over OpenRouter with the operator credential and backend model policy (`z-ai/glm-5.3-flash`), secret-free read-model grounding, `/api/status` `assistant`/`model`/`provider` readiness; the widget is never disabled and the browser acceptance exercises the truthful `provider_not_configured` round trip.
- [x] Cockpit verdict: `cockpit_verdict` (`tc.research-cockpit-verdict.v1`) on the Historical Result detail — SQX-formula statistics over the native trade records, exact native Rankings/Higher Precision acceptance conditions for stages 1–2, cockpit policy (Golden / Scenario / seeded Monte Carlo Stress / Out-of-Sample) for stages 3–6, Proof custody for stage 7; Test & Validate renders funnel tallies, stage verdicts, equity curve, distribution, run statistics and conclusions from it.

Exit: launching the desktop shows the prototype Cockpit Home and Research workspaces with truthful
read-model state; the placeholder shell is gone.

### M1 — Research depth on the accepted UX

Already visible from the exact native Builder task: GA parameters, ranking objectives and
acceptance conditions, cross-check enable flags, 536 native building blocks, templates, project
topology. Next:

- surface the cockpit verdict statistics/equity on Build & Backtest and the Trades tab as well
  (Test & Validate already renders them), and read the native chart history range from
  `settings.xml` so `AvgTradesPerMonth` uses the producer's data span instead of the traded span;
- connect further native cross-check methods (additional markets, Monte Carlo retest, walk-forward,
  what-if, parameter permutation) so their native results feed the same stages and the native
  columns the cockpit cannot recompute (`WF*`, confidence-level Monte Carlo) stop being
  `unevaluated`;
- Assistant grounding against the curated Quant-Guild knowledge library — **done 2026-09-02**
  on `cursor/assistant-quant-guild-5d85` (ingested lecture markdown retrieved into `/api/assistant`;
  never a runtime import). Per-consumer provider-enforced spend limits wait until account
  authority exists;
- Random Discovery vs Genetic Evolution controls through the approved configuration seam —
  **done 2026-09-02** on `cursor/random-genetic-controls-5d85` (search/rankings parsed from the
  approved executable XML; Evolutionary Search no longer reads the live installed task);
- restart/reopen preserves identities across the new routes — **done 2026-09-02**
  on `cursor/reopen-route-identity-5d85` (workspace/tab chrome copies `configuration`,
  `proofEntity`, `validationRef`, and other non-structural query keys; Home New Research /
  Build Strategy stay identity-free).

Exit: the owner runs Idea → … → Proof in the desktop against real read models without route
knowledge, and every funnel stage carries a cockpit verdict backed by native runs.

### M2 — Daily personal-use reliability

- Meaningful launch/recent-work state; saved selection/context persistence —
  **done 2026-09-02** on `cursor/launch-recent-work-5d85` (data-root session path;
  default desktop launch restores it; `--start-path` still wins).
- SQX runtime discovery/setup + verification on Windows; clear error recovery —
  **done 2026-09-02** on `cursor/sqx-runtime-discovery-5d85` (well-known homes +
  `SQX_HOME`; Settings binds by `candidate_id` only; env/CLI pin stays read-only).
- Machine Learning / Models modality first end-to-end path —
  **done 2026-09-02** on `cursor/ml-models-e2e-5d85` (allowlisted sklearn fit on
  native Historical Result trades; SQX still owns backtest/robustness; Candidate
  bind remains a later slice).
- Apollo assistant tool use and Quant-Guild knowledge retrieval under the consumer
  account/model boundary —
  **done 2026-09-02** on `cursor/assistant-tool-use-5d85` (OpenRouter `retrieve_quant_guild`
  tool loop; operator credential and non-provider-enforced spend_boundary unchanged).

Exit: the owner uses the app daily on Windows with the real SQX runtime.

### M3 — Live / Operate

- Live market/signal/risk/scoped-performance producers; paper/prop simulation; promotion.
  Live quotes producer — **done 2026-09-02** on `cursor/live-market-quotes-5d85` (Finnhub
  REST behind `TRADERCOCKPIT_MARKET_API_KEY`) and **extended 2026-09-02** on
  `cursor/schwab-fred-feeds-5d85`: operator Schwab Market Data is preferred when
  `SCHWAB_CLIENT_ID` / `SCHWAB_CLIENT_SECRET` plus `SCHWAB_REFRESH_TOKEN` or loopback
  OAuth are present; otherwise Finnhub. FRED (`FRED_API_KEY`, `TRADERCOCKPIT_FRED_SERIES`)
  is a separate `/api/status` `macro_series` producer (operator now, consumer-capable later),
  not the ticker. Historical FX/indices stay in native SQX Dukascopy Data Manager — no
  second download pipeline. Top-bar custody search — **done 2026-09-02** on
  `cursor/custody-search-5d85` (filters loaded Research catalogs; jumps to existing
  `configuration` / `proofEntity` / workspace routes; does not invent identities).
  Operator promotion after Proof — **done 2026-09-02** on `cursor/operate-promotion-5d85`
  (`/api/operate/promotions`; Delivery custody distinct from export, deployment, and live
  runs). Live signals, risk, and scoped performance — **done 2026-09-02** on
  `cursor/operate-live-state-5d85` (`/api/status` `live_signals`, `live_risk`,
  `scoped_performance`; fail-closed until execution/account producers exist; Operate KPIs,
  status bar, and Home System Health bind truthfully). Paper/prop simulation — **done 2026-09-02** on
  `cursor/operate-paper-prop-5d85` (`/api/status` `prop_simulation` `tc.prop-simulation.v1`;
  fail-closed `simulation_account_not_connected`; Home card 3 and Operate simulation card bind
  truthfully).   Delivery export custody — **done 2026-09-02** on
  `cursor/operate-export-5d85` (`/api/operate/exports`; requires existing Promotion;
  Home Alpha Stack and Operate export card bind truthfully). Live deployment custody —
  **in progress 2026-09-02** on `cursor/operate-deploy-5d85` (`/api/operate/deployments`;
  requires existing Export; `/api/status` `live_deployment` stays fail-closed
  `execution_not_connected`; Home Alpha Stack and Operate live-runs table bind deployment
  custody rows without claiming broker send, fills, positions, or P&L).

Exit: Operate shows truthful live/current state distinct from historical research.

### M4 — Automation and capability expansion

- Native Custom Project automation/control where supported — **complete 2026-09-02** on
  `cursor/custom-project-control-5d85` (`/api/sqx-project-control` GET/POST; trusted gateway
  `action=start|stop` mapped from `run|stop`; Automation control card binds Run/Stop; Schedule
  stays disabled).
- Capability/add-on registry — **in progress 2026-09-02** on `cursor/addon-registry-5d85`
  (`tc.capability-registry.v1` built-in descriptors; `/api/status` `extensions` ready with
  `addons: []`; `/api/extensions` POST refuses script/HTML/nav rewrite; optional data-root
  `extensions.json` typed slots fail closed).

### M5 — Commercial readiness

- [x] Windows installer, Authenticode signing seam, updater/rollback custody (`docs/windows-packaging.md`; stacked on OpenRouter credits #101).
- [ ] Config/data migration, backup/export, crash recovery/diagnostics, secrets storage, account/license/auth, subscription/entitlement, onboarding, customer-readable errors, docs/support, privacy/telemetry policy, SQX distribution/licensing review.

### M6 — Public beta / release

- Clean-machine install, first-run onboarding, representative customer workflows, upgrade and
  failure-recovery tests, support runbook, release acceptance.

## Current status and next lane

Recovery (M0) is complete on `cursor/recovery-ui-authority-5d85` (based on `main`) pending the
owner actions above. Assistant Quant-Guild grounding is on PR #80. Random Discovery vs Genetic
Evolution controls bind the approved configuration seam on PR #81. Restart/reopen identity
preservation across `workspace`/`tab` routes is on PR #82. Desktop launch now restores the last
registered session path on `cursor/launch-recent-work-5d85`. SQX runtime discovery/setup is on
`cursor/sqx-runtime-discovery-5d85`. Machine Learning / Models first fit is on
`cursor/ml-models-e2e-5d85`. Assistant tool use is on `cursor/assistant-tool-use-5d85`.
Home/Trades verdict and CrossChecks
files remain owned by concurrent loadconfig / PR #79 lanes and are not mixed into this slice.
M2 living-plan items on this stack are complete; remaining M2 is daily Windows use of the
real SQX runtime. Per-consumer provider-enforced spend limits still wait for account authority.
The first M3 producer is live quotes on `cursor/live-market-quotes-5d85`, extended on
`cursor/schwab-fred-feeds-5d85` with operator Schwab (preferred), FRED `macro_series`, and
native SQX Dukascopy left as the historical FX/indices pipeline. Top-bar custody search is on
`cursor/custody-search-5d85`. Operator promotion after Proof is on
`cursor/operate-promotion-5d85`. Live signals, risk, and scoped performance status records
are on `cursor/operate-live-state-5d85`. Paper/prop simulation status is on
`cursor/operate-paper-prop-5d85`. Delivery export custody is on
`cursor/operate-export-5d85`. Live deployment custody is on
`cursor/operate-deploy-5d85`. Native Custom Project control is on
`cursor/custom-project-control-5d85`. Capability/add-on registry is on
`cursor/addon-registry-5d85`. Google consumer identity, Stripe $150 membership, and
OpenRouter $30 provider-enforced credits are on PRs #99–#101. M5 Windows packaging
(installer / signing / updater-rollback) is on `cursor/windows-packaging-5d85` stacked on #101.
Next after merge: M6 Windows Idea→Proof on real SQX (`docs/windows-desktop-acceptance-runbook.md`).

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing changes,
and delete the branch after merge.
