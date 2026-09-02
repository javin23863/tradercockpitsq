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
- `docs/windows-desktop-acceptance-runbook.md` — how this Windows desktop proves a native slice.

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

## How this Windows lane works

This machine has the authorized StrategyQuant X **144.2953** runtime. Product work is accepted
when the owner path works in the development desktop here. Merge, hosted Actions, and Linux CI
are a later packaging concern — they are not the process for finishing the product.

Do not invent DualRuntimeProof greens, a substitute SQX engine, live prices, or broker/account
state. A missing producer stays an explicit not-connected read model.

One branch is one slice. The shared clone `C:\Users\MSI\sq\tradercockpitsq` is read-only while it
is dirty. Parked clone `C:\Users\MSI\repos\tradercockpit-app` is forbidden.

## Milestone roadmap

Each milestone has a user-visible desktop exit criterion. A milestone is not complete because
tests pass; it is complete when the owner can perform the intended path on this Windows desktop
and it matches the accepted authority.

### M0 — Repository and UI-authority recovery

Product recovery is done on `cursor/recovery-ui-authority-5d85` (PR #76). Remaining rows are
owner git/GitHub actions, not product slices.

- [x] Audit + UI chronology recorded (`docs/recovery/2026-09-01-audit-and-ui-chronology.md`).
- [x] `references/ui-authority/**` restored into the `main` lineage.
- [x] Canonical docs reconciled (README, AGENTS, architecture, backbone) to one product story.
- [x] Milestone roadmap replaces the checkbox plan.
- [ ] PR/branch disposition recorded and actioned by the owner (see recovery disposition doc).
- [ ] `main` branch protection + required checks enabled (owner action).
- [x] Five accepted authority screens committed byte-for-byte under `references/ui-authority/screenshots/` with a truthful pinned manifest; previews regenerated from those PNGs.
- [x] Live-market provider seam added: `/api/market/quotes` (`tc.market-quotes.v1`) with an operator watchlist and truthful `provider_not_configured`; no hard-coded ticker symbols or values remain in the frontend.
- [x] `web/` rebuilt to the five screens: global chrome, Cockpit Home board, four Research workspaces with exact tab rows; Explore/Automation/Operate/Settings in the same grammar.
- [x] Existing read-model binders kept and re-pointed; legacy `stage`/`tab` links canonicalise.
- [x] Acceptance rewritten to the prototype (ui-shell, browser regression, robustness/proof).
- [x] Assistant made functional over OpenRouter with the operator credential.
- [x] Cockpit verdict on the Historical Result detail; Test & Validate renders it.

Exit: launching the desktop shows the prototype Cockpit Home and Research workspaces with truthful
read-model state; the placeholder shell is gone.

### M1 — Research depth on the accepted UX

Already visible from the exact native Builder task: GA parameters, ranking objectives and
acceptance conditions, cross-check enable flags, 536 native building blocks, templates, project
topology.

- Home Build & Backtest + Trades tab render `cockpit_verdict` statistics/equity, and
  `AvgTradesPerMonth` uses native `settings.xml` chart range when present —
  **in progress off this stack** on dirty `cursor/loadconfig-cfx-5d85` (Home/Trades/verdict
  files). Do not restack those files. This stack still follows the backbone: Home card 2 metrics
  stay `—` until that lane lands. Sharpe stays `—` (no Sharpe producer).
- Native CrossChecks start and feed the funnel (Additional Markets, Monte Carlo retest,
  Walk-Forward / Matrix, What-If, System Parameter Permutation, Monte Carlo manipulation,
  Sequential Optimization) —
  **in progress off this stack** on PR #79 (`cursor/crosschecks-parity-5d85`) plus the loadconfig
  lane. Sequential stays unavailable until the installed profile has Sequential Settings. Native
  columns the cockpit cannot recompute (`WF*`, confidence-level Monte Carlo) stay `unevaluated`
  unless the producer SQStats actually contain them. Do not mix those files into a new slice.
- Assistant Quant-Guild grounding — **done 2026-09-02** PR #80 (`cursor/assistant-quant-guild-5d85`).
- Random Discovery vs Genetic Evolution from the approved configuration XML —
  **done 2026-09-02** PR #81 (`cursor/random-genetic-controls-5d85`).
- Restart/reopen preserves custody query keys across `workspace`/`tab` —
  **done 2026-09-02** PR #82 (`cursor/reopen-route-identity-5d85`).
- Per-consumer provider-enforced spend limits — **not M1**. Waits for M5 account authority.
  Do not invent them on the operator credential.

Exit: the owner runs Idea → … → Proof in this Windows desktop against real read models without
route knowledge, and every funnel stage carries a cockpit verdict backed by native runs.

### M2 — Daily personal-use reliability

- Launch restores the last registered session path — **done 2026-09-02** PR #83.
- SQX 144.2953 discovery/setup on Windows; Settings binds by `candidate_id` only —
  **done 2026-09-02** PR #84. Env/CLI pin stays read-only.
- Machine Learning / Models first fit (allowlisted sklearn on native trades) —
  **done 2026-09-02** PR #85. SQX still owns backtest/robustness.
- Bind a fitted model into existing Candidate custody (pointer on one imported native Candidate;
  never a fabricated candidate from a pickle) — **open**. First unclaimed slice on this stack once
  sibling `tests/product/test_research_candidates.py` is free. Until then the Models catalog stays
  fit-only.
- Apollo `retrieve_quant_guild` tool use — **done 2026-09-02** PR #86. Operator credential;
  `spend_boundary.provider_enforced` stays false.
- Daily Idea → Proof on this Windows desktop against the real SQX runtime —
  **open** (runbook `docs/windows-desktop-acceptance-runbook.md`). This is product use, not a
  Linux merge.

Exit: the owner uses the app daily on Windows with the real SQX runtime.

### M3 — Live / Operate

Live values must come from a live/current producer. Historical SQX results and Finnhub last
prices are not live runs, positions, P&L, risk, order flow, or a prop account.

- Live quotes (Home ticker, Data Feeds, Market Overview) — **done 2026-09-02** PR #87
  (Finnhub REST behind `TRADERCOCKPIT_MARKET_API_KEY`; watchlist symbols requested as-is).
- Live **signal** producer for Signals & Models analytics tabs (Order Flow, Footprint, Volume
  Profile, Liquidity Map, Replays, Signal Pulse) — **open**. Needs time-and-sales / depth, not
  last-price quotes and not backtest trades.
- Live **risk** producer (status-bar Drawdown, Operate risk limits, Signals risk overlay) —
  **open**. Needs an account/execution producer.
- **Scoped-performance** producer (status-bar Live Runs, Positions, Daily P&L, Buying Power;
  Operate KPI strip) — **open**. Same broker/execution producer. Never copy Research last-run
  into those cells.
- **Paper / prop simulation** (Home card 3, Operate paper/prop account) — **open**.
- **Promotion** (Alpha Stack promoted / exported / deployed kept distinct; Operate live runs) —
  **open**. Promotion is not Candidate import and not Proof.
- Top-bar **search** over custody catalogs (currently disabled) — **open**. Platform-owned;
  does not invent identities.

Exit: Operate shows truthful live/current state distinct from historical research.

### M4 — Automation and capability expansion

- Native Custom Project **topology** is already a read-only custody inspector.
- Native Custom Project **run/stop/readback** through the trusted gateway (MCP
  `run_project` / `stop_project` where the installed runtime supports it) — **open**.
  Do not build a platform task-loop engine.
- Scheduling of native workflows — **open** (depends on control).
- Capability / add-on **registry** (typed slots; `/api/status` extensions) — **open**.
  Add-ons cannot inject script/HTML or rewrite top-level navigation.

### M5 — Commercial readiness

Named because the product is a sold desktop, not because this wave jumps to an installer.

- Google consumer authentication (not OpenRouter login) — **open**.
- Platform account + **$150/month Stripe subscription** auto-created — **open**.
- **$30/month OpenRouter credits** per user, provider-enforced (not a local counter) — **open**.
- Secrets storage for operator/provider credentials (never in `web/`) — **open**.
- Windows installer, code signing, updater/rollback — **open**.
- Config/data migration, backup/export, crash recovery/diagnostics — **open**.
- Onboarding, customer-readable errors, docs/support, privacy/telemetry policy — **open**.
- SQX distribution/licensing review — **open**.

### M6 — Public beta / release

- Clean-machine Windows install, first-run onboarding, representative customer workflows,
  upgrade and failure-recovery tests, support runbook, release acceptance.

## Current status and next lane

**Now (2026-09-02):** finish the product on this Windows desktop. Do not run a Linux merge
workflow.

Stacked, unmerged product PRs on `cursor/recovery-ui-authority-5d85`: #76 recovery UI, #80
Quant-Guild, #81 Random/Genetic, #82 reopen identity, #83 launch session, #84 SQX discovery,
#85 ML fit, #86 assistant tools, #87 Finnhub quotes. CrossChecks remain PR #79. Home/Trades
verdict remains the dirty loadconfig lane.

**Do not mix** Home/Trades/verdict files or CrossChecks files into a new slice.

**Next unclaimed slice on this stack:** bind a fitted ML model to one existing native Candidate
(when candidate tests are not owned by the loadconfig lane). If those files stay contended, the
next unblocked slice is top-bar search over existing custody catalogs, then M3 only when a real
broker/execution or level-2 producer exists.

Per-consumer spend limits, Stripe, and packaging wait for M5. They are named so they cannot
disappear; they are not the current lane.

## Discipline

Start every branch from current `main` (or the current stack tip when explicitly stacking),
inspect `references/ui-authority` before UI work, keep one branch to one coherent slice, update
this plan only when real status or sequencing changes, and delete the branch after merge.
