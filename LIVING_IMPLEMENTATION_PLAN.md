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

The five prototype screens in `references/ui-authority/screenshots/` supply neon chrome and
Research workspace structure. Home is the live/current cockpit with exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

plus persistent Apollo. Card titles in `cockpit-home.png` are illustrative framing, not the Home
zone contract. Research is four workspaces — Signals & Models (nine tabs), Evolutionary Search,
Test & Validate (six tabs, seven-stage funnel), Indicators & Models (six pills). The custody workflow
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
- [x] `web/` rebuilt to the five screens: global chrome (rail with workspace/progress/account cards, Data Feeds/Broker/Compute/Automation chips, market ticker, status bar), Cockpit Home live/current zones (`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`) plus persistent Apollo, and the four Research workspaces with their exact tab rows; Explore/Automation/Operate/Settings in the same grammar. All values come from read models; everything without a producer is an explicit not-connected/no-data state.
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

- [x] surface the cockpit verdict statistics/equity on the Trades tab (Test & Validate Overview
  already renders them) and read the native chart history range from result `settings.xml`
  (`Setup dateFrom`/`dateTo`) so `AvgTradesPerMonth` uses the producer's data span when exactly
  one dated Setup exists; otherwise keep the traded span and report `months_basis`;
- [x] connect further native cross-check methods (additional markets, Monte Carlo retest, walk-forward,
  what-if, parameter permutation) so their native results feed the same stages and the native
  columns the cockpit cannot recompute (`WF*`, confidence-level Monte Carlo) stop being
  `unevaluated` when the result archive carries those producer-recorded values; launch remains
  Higher Precision only;
- [x] Assistant grounding against the curated Quant-Guild knowledge library (public lecture
  titles/URLs plus platform-authored catalog notes, retrieved into `/api/assistant`;
  lecture notebooks and transcripts are not stored). Per-consumer provider-enforced
  spend limits remain deferred until consumer account authority exists;
- [x] Random Discovery vs Genetic Evolution controls through the approved configuration
  seam (Evolutionary Search binds `BuildMode` / `Rankings` from approved executable XML,
  not the live installed task; Genetic-only operators stay hidden in Random Discovery);
- [x] restart/reopen preserves identities across the new routes (Research chrome hops
  copy `configuration` / `proofEntity` / `validationRef`; Home Quick Actions start
  without leftover IDs).

Exit: the owner runs Idea → … → Proof in the desktop against real read models without route
knowledge, and every funnel stage carries a cockpit verdict backed by native runs.

### M2 — Daily personal-use reliability

- [x] Meaningful launch/recent-work state; saved selection/context persistence
  (data-root `desktop-session.json` via `/api/desktop/session`; launch restores
  the last registered path including Research custody IDs; `--start-path` wins).
- SQX runtime discovery/setup + verification on Windows; clear error recovery.
- [x] Machine Learning / Models first end-to-end path: fit allowlisted sklearn
  classifiers on native trades and bind the catalog digest onto an existing
  native Candidate (SQX still owns backtest and robustness).
- [x] Apollo assistant mid-turn tool use under the consumer account/model
  boundary (`retrieve_quant_guild` over the curated catalog; unknown tools and
  extra keys fail closed; no native mutation). Quant-Guild request-time
  retrieval remains on `cursor/assistant-knowledge-ground-5d85`.

Exit: the owner uses the app daily on Windows with the real SQX runtime.

### M3 — Live / Operate

- Live market/signal/risk/scoped-performance producers; paper/prop simulation; promotion.

Exit: Operate shows truthful live/current state distinct from historical research.

### M4 — Automation and capability expansion

- Native Custom Project automation/control where supported; capability/add-on registry.

### M5 — Commercial readiness

- Installer, code signing, updater/rollback, config/data migration, backup/export, crash
  recovery/diagnostics, secrets storage, account/license/auth, subscription/entitlement,
  onboarding, customer-readable errors, docs/support, privacy/telemetry policy, SQX
  distribution/licensing review.

### M6 — Public beta / release

- Clean-machine install, first-run onboarding, representative customer workflows, upgrade and
  failure-recovery tests, support runbook, release acceptance.

## Current status and next lane

Recovery (M0) chrome and Research four-workspace IA are on `cursor/recovery-ui-authority-5d85`.
Home is restored to the eight live/current cockpit zones on `cursor/cockpit-home-zones-5d85`.
M1 Trades verdict + `settings.xml` chart-history `AvgTradesPerMonth` is on
`cursor/verdict-trades-span-5d85`. Native CrossChecks catalog + producer-column evaluation
(`WF*`, confidence-level Monte Carlo) is on `cursor/native-crosscheck-seams-5d85`. Assistant
Quant-Guild catalog grounding is on `cursor/assistant-knowledge-ground-5d85`. Random vs
Genetic controls on the approved configuration seam are on
`cursor/approved-search-mode-5d85`. Restart/reopen identity across the new Research
routes is on `cursor/reopen-route-ids-5d85`. Desktop launch restore of the last
registered route is on `cursor/session-restore-5d85`. Models catalog bind onto
an existing native Candidate is on `cursor/models-candidate-bind-5d85`. Apollo
mid-turn `retrieve_quant_guild` is on `cursor/apollo-midturn-retrieve-5d85`.
The one product-line stack is this recovered UX line (`recovery-ui-authority` →
Home/Trades/CrossChecks/knowledge/search/reopen/session-restore/models-bind →
this slice). Parallel desktop M2–M5 feature stacks are donor reference only
and must not be continued or merged as a second spine. Owner PR-disposition
and `main` branch-protection actions remain. Remaining M2 on this stack is
Windows-verification-only: real SQX 144.2953 discovery/setup, installer
signing, live broker keys. Do not open more product-feature PRs on a second
stack.

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing changes,
and delete the branch after merge.
