# TraderCockpit Recovery Audit and UI Chronology (2026-09-01)

Status: historical recovery evidence. This is not a competing roadmap. The single
canonical authority after recovery is `README.md`, `AGENTS.md`,
`docs/product-architecture-v1.md`, `docs/product-backbone-spec-v1.md`, and
`LIVING_IMPLEMENTATION_PLAN.md`. This document records what was found so those
canonical files could be corrected.

## 1. Snapshot

- `main` = `ea567c32148947d614cb8514c7505abb5d531409`.
- Open PRs at audit time:
  - #72 Research end-to-end vertical (`research/workflow-correction-integrity-audit`, head `c12b50a7…`, base `main`). Product Runtime Acceptance #805 pending.
  - #74 Home capability cockpit (`ui/capability-cockpit-home`, head `50d847c7…`, stacked on #72). Product Runtime Acceptance #815 pending.
  - #73 Cloud Agent environment (`cursor/setup-dev-environment-5d85`, head `f948cf70…`, based on the stale `codex/sqx-engine-extract`, not `main`).
  - #75 Recovery UI-authority evidence (`docs/fable-visual-recovery-handoff`, head `e81a0e0…`, base `main`).
- `main` branch protection could not be read with the agent token (HTTP 403). Enabling protection + required checks remains an owner action.

## 2. The core failure: the pinned UI authority left the active tree

The repository already had an explicitly pinned, accepted visual product authority — the
multicolor "ESQ TraderCockpit" prototype — and it was later removed from the active tree
by a cleanup commit that misclassified it as disposable.

Chronology (oldest to newest):

| Commit | Title | Effect |
|---|---|---|
| `1391251c` | Pin canonical TraderCockpit UI prototype lineage | Added `references/ui-authority/**` + `tools/sync-ui-authority.ps1`; declared the multicolor prototype the frontend authority superseding the dark-blue `Chart/Backtest/Proof` shell |
| `813ff73` | Add prototype UI previews for desktop agent | Added repository-visible WebP previews; required agents to inspect them before changing frontend framing |
| `53645ac` | Pin reviewed product implementation checklist | Head of retained authority branch `codex/ui-prototype-authority` |
| `725caab` | Consolidate accepted UI authority | Consolidated the accepted set |
| `e03f47c` | Restore actual canonical UI authority and remove invented mockups | Reasserted the authority set |
| `3d878d2` | chore(product): prune non-product recovery material | **Removed `references/ui-authority/**` (and `references/code-templates/**`, several docs) from the active tree, treating the pinned visual authority as "non-product recovery material"** |
| `cb8b527` | Align workflow authority to StrategyQuant X parity | Continued development on the simplified shell |

Result: current `main` has no `references/` directory at all, and the visible desktop
renders the simplified dark home shell in `web/app.mjs` (`renderHome`), which emits mostly
`unavailable(...)` placeholders. Development kept passing exact-head acceptance while the
visible product no longer matched the accepted authority. That is a repository-integrity
failure, not merely a styling drift.

Answer to "where was the authority removed": commit `3d878d2`. It was an intentional
cleanup, but the classification was wrong — the previews and manifest are product authority,
not recovery debris.

## 3. Documentation contradictions on `main`

- `README.md` still claims the clean baseline has "no native Retester implementation, candidate/run/result store, or product feature/API bound to native mutation." False: `product/tradercockpit/` on `main` contains `research_retester.py`/`research_retester_http.py`, `research_candidates.py`, `research_configurations.py`, `research_native_jobs.py`, `research_trades.py`, `research_robustness.py`, `research_proof*.py`, and a full custody chain served over HTTP.
- `AGENTS.md`, `docs/product-architecture-v1.md`, `docs/product-backbone-spec-v1.md`, and `LIVING_IMPLEMENTATION_PLAN.md` define Home as "preserves exactly" the eight zones `Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`. The pinned Cockpit Home authority contains those same zones plus a persistent Apollo assistant — so the eight zones were never the problem; the dark-blue visual execution was. PR #74 wrongly demoted those zones.
- None of the canonical docs reference `references/ui-authority`, the accepted 8-stage workflow (`Idea → Construct → Build → Candidates → Backtest → Robustness → Proof → Delivery/Simulation`), a Machine Learning / Models research modality, or the Apollo assistant + knowledge library.

## 4. Backend capability to accepted-UI matrix

Backend API surface on `main` (defined in `product/tradercockpit/` and consumed in `web/`):

| Capability | Backend seam (API) | Producer | Accepted-UI destination | Current UI | Gap |
|---|---|---|---|---|---|
| Runtime/system status | `/api/status` | platform | Cockpit Home: Engine & System Status, System Alerts, Resource Usage | Home "Runtime attention" placeholder | Reconnect into rich cockpit status/gauge |
| Idea custody | `/api/research/ideas` | platform custody | Research: Idea (Construct) | Construct/Idea editor (works) | Restyle to authority; keep custody |
| Exact configuration | `/api/research/configurations` | platform + SQX config | Research: Specification/Build | Build placeholder | Reconnect Specification/Build |
| Native Builder jobs | `/api/research/native-jobs` | SQX Builder gateway | Research: Build launch / Evolutionary Search status | Build placeholder | Reconnect job custody/readback |
| Candidates | `/api/research/candidates` | SQX Builder output | Research: Candidates; Evolutionary Search candidate table | Candidates placeholder | Reconnect Candidate Lab |
| Historical results | `/api/research/historical-results` | SQX Retester | Test & Validate: Initial Test/Overview/Trades | Backtest placeholder | Reconnect Backtest Overview/Trades |
| Robustness/Proof | `/api/research/proofs` (+ robustness) | SQX robustness | Test & Validate: Fast/Golden funnel; Proof & Evidence | Robustness/Proof placeholder | Reconnect validation funnel + Proof |
| SQX presets | `/api/sqx-presets` | SQX runtime | Research: capability discovery | research-presets binder | Fold into Indicators & Models catalog |
| SQX builder config | `/api/sqx-builder-config` | SQX | Research: Specification/native disclosure | research binders | Reconnect native disclosure |
| SQX outputs | `/api/sqx-outputs` | SQX | Candidates / results provenance | binder | Reconnect |
| SQX project topology | `/api/sqx-project-topology` | SQX Custom Project | Automation | Automation placeholder | Reconnect Automation |
| Market context | `home_market.py` | live producer (not configured) | Cockpit Home: Market Overview | Home placeholder | Keep truthful-unavailable, styled |

Missing backend for accepted screens (new work, later lanes): live market/signal/risk/performance producers; Machine Learning / Models modality; Apollo LLM gateway + Quant-Guild knowledge retrieval; Prop Simulation; Delivery/Simulation stage.

## 5. UI findings

- The shell (`web/app.mjs`) is a real render model, but ~18 sibling modules (`web/home-*.mjs`, `web/research-*.mjs`) each attach a `MutationObserver` to the document and re-mount their own content into placeholders after every render. This binder-as-architecture is fragile and makes the accepted multicolor composition hard to express directly.
- The visible product is the dark-blue shell; it does not resemble any of the five pinned authority screens.
- PR #74 changes Home into a capability inventory. That is the wrong level of correction and should not become the authority.

## 6. Test / CI findings

- Acceptance is strong (production boundary, product unittests, node UI tests, Playwright browser regression, Windows frozen-desktop launch) and gated exact-head.
- But browser/desktop tests currently lock in the dark-blue shell composition, so passing them did not protect the accepted product. Tests must be updated to assert the accepted product.

## 7. Keep / fix / remove decisions

- KEEP: the entire `product/tradercockpit/` backend + custody chain; SQX gateway/runtime-trust separation; acceptance harness structure; `references/ui-authority/**` (restore into `main`).
- FIX: `README.md` baseline claims; canonical docs' Home/workflow definitions; the shell render model and global visual language; browser/desktop tests.
- REMOVE/SUPERSEDE: reliance on the `MutationObserver` binder pattern as shell architecture (migrate incrementally); PR #74's capability-cockpit-as-Home direction.

## 8. Answers to the handoff's key questions

1. Authority removed at `3d878d2` (misclassified as recovery debris).
2. Removal was an intentional cleanup with a wrong classification; no product-authority document authorized replacing the multicolor cockpit with the dark-blue shell.
3. Divergent UI files: `web/app.mjs` `renderHome` + the dark-blue `web/styles.css`; all `web/home-*.mjs` render into a non-authority composition.
4. Tests locking the wrong UI: `tests/browser-regression.mjs`, `tests/run-browser-regression.mjs`, `tests/research-vertical-browser-regression.mjs` (on #72), and `tests/home-capability-cockpit.test.mjs` (on #74).
5. PR #72 is largely backend-complete but not product-complete (its UI is still the dark-blue shell).
6. PR #74 should be closed and mined for mechanics, not adopted as design.
7. Canonical first launch = the multicolor Cockpit Home authority with real read models and truthful unavailable states.
8. The accepted lineage already contains the SQX-native Research workflow (Signals & Models, Evolutionary Search, Test & Validate, Indicators & Models catalog); reconnect existing read models into those surfaces.
9. Backend present but not visible in accepted UI: full custody chain (ideas→proofs), SQX inspection APIs.
10. Accepted-screen capabilities not yet in backend: live market/signal/risk/performance, ML/Models modality, Apollo, Prop Simulation, Delivery/Simulation.
11. Shortest path to daily-usable: restore the cockpit shell + Home + Research on real read models; keep truthful unavailable states.
12. Remaining for sellable beta: installer/signing/updater/auth/license/onboarding/support (see roadmap M5–M6).
