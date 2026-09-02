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
bounded card.

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

Exit: launching the desktop shows the prototype Cockpit Home and Research workspaces with truthful
read-model state; the placeholder shell is gone.

### M1 — Research depth on the accepted UX

Already visible from the exact native Builder task: GA parameters, ranking objectives and
acceptance conditions, cross-check enable flags, 536 native building blocks, templates, project
topology. Next:

- read native result metrics/equity/trade series from the exact result archives so Build &
  Backtest, Test & Validate KPIs, performance/distribution frames and the Run & Evidence table show
  producer values instead of `—`;
- connect further native cross-check methods (additional markets, Monte Carlo, walk-forward,
  what-if, parameter permutation) so the funnel stages beyond Fast Validation count real runs;
- Random Discovery vs Genetic Evolution controls (read-only today) through the approved
  configuration seam;
- restart/reopen preserves identities across the new routes.

Exit: the owner runs Idea → … → Proof in the desktop against real read models without route
knowledge, and the seven-stage funnel counts native runs.

### M2 — Daily personal-use reliability

- Meaningful launch/recent-work state; saved selection/context persistence.
- SQX runtime discovery/setup + verification on Windows; clear error recovery.
- Machine Learning / Models modality first end-to-end path; Apollo assistant connected
  (LLM gateway + Quant-Guild knowledge retrieval) under the account/model boundary.

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

Recovery (M0) is complete on `cursor/recovery-ui-authority-5d85` (based on `main`) pending the
owner actions above. The next coherent lane is M1 (native result metrics and further cross-check
seams into the prototype workspaces). Real installed-SQX runtime and packaged-Windows verification
are performed on a Windows desktop by the owner's desktop agent; the Linux CI covers browser
acceptance and the frozen WebView2 build/launch.

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing changes,
and delete the branch after merge.
