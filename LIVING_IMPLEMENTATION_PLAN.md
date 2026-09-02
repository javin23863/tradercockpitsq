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

Accepted Research workflow:
`Idea → Construct → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`,
with Construct modalities Random Discovery, Genetic/Evolutionary search (native SQX), and
Machine Learning / Models (platform-owned). Apollo is the persistent bounded assistant.

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
- [x] Canonical shell + Cockpit Home restored to the multicolor authority, reconnected to real read models (top chrome chips, market ticker, and Market Overview now read `/api/status` and `/api/market/quotes`), with acceptance updated to assert the accepted product.
- [x] Five accepted authority screens committed byte-for-byte under `references/ui-authority/screenshots/` with a truthful pinned manifest.
- [x] Live-market provider seam added: `/api/market/quotes` (`tc.market-quotes.v1`) with an operator watchlist and truthful `provider_not_configured`; no hard-coded ticker symbols or values remain in the frontend.

Exit: launching the desktop shows the multicolor Cockpit Home matching `references/ui-authority`,
with truthful read-model state; the dark-blue shell is gone. Pixel-level fidelity of the dense
Research screens (Signals & Models, Evolutionary Search, Test & Validate) is completed in M1 as
each surface is reconnected to its producer read models.

### M1 — Research assembled on the accepted UX

- Reconnect the existing custody chain (Idea → Configuration → Native job → Candidate →
  Historical result → Proof) into the accepted Research surfaces (Signals & Models, Evolutionary
  Search, Test & Validate funnel, Indicators & Models catalog, Proof).
- Random Discovery vs Genetic/Evolutionary presented as distinct modalities.
- Restart/reopen preserves identities.

Exit: the owner runs Idea → … → Proof in the desktop against real read models without route
knowledge.

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

Recovery (M0) is in progress on `cursor/recovery-ui-authority-5d85` (based on `main`). The next
coherent lane after the shell/Home restoration lands is M1 (reconnect the custody chain into the
accepted Research surfaces). Real installed-SQX runtime and packaged-Windows verification are
performed on a Windows desktop by the owner's desktop agent; the Linux CI covers browser
acceptance and the frozen WebView2 build/launch.

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing changes,
and delete the branch after merge.
