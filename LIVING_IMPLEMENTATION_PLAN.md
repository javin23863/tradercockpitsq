# Living Implementation Plan

> **THIS IS THE SINGLE LIVING IMPLEMENTATION PLAN FOR THIS REPOSITORY.**
>
> All development sequencing, current status, blockers, active lanes, and completion gates are maintained here. Architecture documents define product constraints and ownership; this file defines **what we implement next and when a phase is complete**.
>
> Do not create a competing roadmap/checklist in another document, issue, PR body, or agent prompt. If execution order changes, update this file in the same branch that changes the implementation.

## Authority order

1. `docs/product-architecture-v1.md` — stable producer/product ownership.
2. `docs/product-backbone-spec-v1.md` — stable detailed product/API/UI contract.
3. `docs/home-research-surface-authority-v1.md` — stable Home vs Research placement/naming.
4. `AGENTS.md` — implementation/review rules.
5. **`LIVING_IMPLEMENTATION_PLAN.md` — mutable execution order and completion state.**

Issue #37 is temporary consolidation coordination only. It must close when the consolidation gate below closes and must not become a second roadmap.

---

# Current product shape

Top-level desktop surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

Research contains:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

Home preserves exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

StrategyQuant X / SQX is a native backend producer identity where provenance/runtime details require it. It is not the platform name and not a user-facing workspace label.

---

# Phase 0 — Repository consolidation and desktop baseline

**Status: ACTIVE. Feature expansion is frozen until every required item below is complete.**

## Canonical trunk and queue

- [x] Architecture authority landed onto `main`.
- [x] Review-governance workflow landed onto `main`.
- [x] Consolidation branch replaces obsolete product shell with Home + Research.
- [x] Duplicate platform-owned Builder/evolution producer removed from production.
- [x] Production-boundary guard rejects known Futures/reference/Phase01/Apollo/duplicate-Builder leakage.
- [x] Thin desktop host uses the canonical server/UI and is loopback/Host/Origin hardened.
- [x] PR #15 read-only Custom Project topology/path custody is integrated into the consolidation branch.
- [ ] PR #15 closed as integrated donor history.
- [ ] PR #23 closed as pinned donor history; do **not** merge its 26-file shared-contract branch wholesale.
- [ ] PR #38 exact current head passes Product Runtime Acceptance after all consolidation changes.
- [ ] PR #38 exact current head receives final substantive/manual review with no unresolved blocker.
- [ ] Consolidation lands to `main` as one clean product baseline.
- [ ] Open PR queue after landing contains no stale overlapping implementation branch.
- [ ] GitHub repository default branch is `main`.

## Repository integrity

- [x] No production runtime imports from `sources`, `references`, or legacy `futures`.
- [x] No production `tradercockpit.builder` duplicate producer package.
- [x] No Phase01 product architecture.
- [x] No persistent Apollo product spine.
- [x] No copied Futures repository marker in production.
- [x] Custom Project project paths resolve inside verified native runtime and symlink/junction escape is refused.
- [ ] Final consolidated-tree audit confirms no second application server, quantitative producer, account authority, candidate/result identity family, or UI product spine.

## Desktop/browser baseline

- [x] `/home` is the default application route.
- [x] Home renders all eight accepted zones.
- [x] `/research` is a separate top-level workspace.
- [x] Research internal stage/tab routing is bounded to registered states.
- [x] `/strategyquant` is compatibility-only and redirects to `/research`.
- [x] Desktop lifecycle/security has headless tests.
- [ ] Packaged/manual Windows WebView2 launch verified on a Windows development environment.
- [ ] Desktop close cannot orphan the canonical server/native worker in the packaged/manual path.

## Phase 0 exit condition

Phase 0 closes only when `main` is the clean/default trunk, PR #38 is landed from an exact green/reviewed head, stale donor PRs are closed, and no overlapping branch can accidentally merge superseded architecture.

When Phase 0 closes, update this section to `COMPLETE`, close Issue #37, and begin Phase 1 from the resulting `main`.

---

# Phase 1 — Real development application foundation

**Status: BLOCKED BY PHASE 0.**

Goal: turn the clean desktop shell into the actual development application where every accepted feature is visible/inspectable as it lands.

## 1A. Canonical runtime/status backbone

- [ ] One runtime descriptor reports application, native research backend, data/provider, and extension readiness without fabricated state.
- [ ] One canonical state-root/custody family.
- [ ] One native research gateway/runtime-verification family.
- [ ] One product identity family for idea/config/job/candidate/result/proof.
- [ ] System Status on Home reads this canonical status model.

## 1B. Research Foundation Vertical

Required real desktop path:

`Research -> Construct/Idea -> Specification -> Build -> Candidates -> Backtest -> Proof`

- [ ] Persist immutable/revisioned Idea/source custody.
- [ ] Resolve native configuration requirements without inventing producer semantics.
- [ ] Compile/review/approve one exact native configuration snapshot.
- [ ] Launch actual native Builder through verified runtime/control boundary.
- [ ] Import real native survivor(s) into Candidate Lab with exact artifact custody.
- [ ] Rebuild the useful PR #23 Retester/candidate/readback pieces **selectively from clean `main`**, preserving launcher SHA trust and rejecting its obsolete shared UI/server assumptions.
- [ ] Execute one real downstream native validation/retest.
- [ ] Show producer-backed Backtest Overview/Trades/Robustness/Configuration.
- [ ] Proof binds exact idea/config/runtime/job/artifact/result/validation identities.
- [ ] Restart/reopen resolves the same identities.

No platform-owned Builder/GA/backtester/robustness/optimizer fallback may satisfy this phase.

## 1C. Automation read surface

The already-integrated Custom Project topology custody is read-only infrastructure.

- [ ] Expose registered native project topology through the canonical backend read model.
- [ ] Present topology inside Automation.
- [ ] Preserve unknown native task kinds opaquely.
- [ ] Keep execution native; do not build a platform task-loop executor.

---

# Phase 2 — Home live/current product track

**Status: BLOCKED BY PHASE 0; may run in parallel with later Phase 1 slices only when producer ownership is explicit.**

For each Home zone, identify the actual live/current producer and expose one backend read model with scope/freshness.

- [ ] Market Overview — current market-data producer, timestamp, session/source/freshness.
- [ ] System Status — canonical application/native/provider health.
- [ ] Alpha Stack — canonical research/promotion/deployment identities.
- [ ] Pipeline Overview — current lifecycle/attention without invented generic phases.
- [ ] Signals — live strategy + live market context only.
- [ ] Risk — current account/execution/exposure authority.
- [ ] Performance — explicit live account/deployed strategy/historical scope.
- [ ] Quick Actions — navigation only, no hidden producer/workflow.

Historical research values never masquerade as live/current truth.

---

# Phase 3 — Consumer account and bounded model access

**Status: BLOCKED BY PHASE 0. PR #36 remains donor history only.**

Rebuild from clean `main` rather than reopening PR #36.

Required proof:

`Google sign-in -> stable internal subject -> configured allowance -> provider-bounded OpenRouter spend -> backend-selected model -> usage attribution -> clean limit refusal -> no spend after lapse/revocation`

Required invariants:

- [ ] stable Google subject binding;
- [ ] starter grant idempotent across multiple writer processes;
- [ ] explicit durable grant-policy identity;
- [ ] operator provisioning credential never reaches browser/consumer;
- [ ] provider hard limit/reset/expiry/revocation is authoritative monetary boundary;
- [ ] account/allowance read model separate from model policy;
- [ ] current default workhorse `z-ai/glm-5.3-flash` remains backend-configurable;
- [ ] exhausted/revoked/lapsed state refuses before further spend.

Commercial allowance values remain configuration, not source guesses.

---

# Phase 4 — Capability/add-on backbone

- [ ] One backend capability manifest/descriptor authority.
- [ ] Frontend and language/tool surfaces consume the same authority.
- [ ] Typed stable extension slots only.
- [ ] Unknown descriptor/renderer versions fail closed.
- [ ] No arbitrary backend-supplied JavaScript/HTML injection.
- [ ] Add-ons cannot rewrite top-level navigation or Research core stages.

---

# Mandatory rule for every future slice

Before starting:

1. Read stable architecture/backbone plus this plan.
2. Start from current reviewed `main`.
3. Confirm no active branch owns the same files/product slice.
4. Update this plan if sequencing/status materially changes.

Before merge:

- [ ] exact head recorded;
- [ ] no overlapping active implementation branch;
- [ ] production-boundary checks pass;
- [ ] focused tests pass;
- [ ] full applicable Product Runtime Acceptance passes;
- [ ] browser acceptance passes for UI/routing changes;
- [ ] desktop acceptance passes for desktop/runtime changes;
- [ ] user-facing behavior is visible/inspectable in the one development desktop;
- [ ] substantive exact-head review findings are resolved/dispositioned.

A slice is product-complete only when the real desktop path works through the canonical producer/read-model/custody boundary and durable truthful state returns to the correct product surface.

---

# Current next action

**Finish Phase 0 only. Do not start a new feature slice yet.**

Execution order:

1. close PR #15 after confirming its exact three-file custody implementation is present here;
2. close PR #23 as pinned donor history, preserving exact head `c67fb434badba1822c0cf095df9dd2ab102d32cc` for selective Phase 1 reuse;
3. run Product Runtime Acceptance and final exact-head review on PR #38 after the living-plan and PR #15 integration commits;
4. land PR #38 to `main`;
5. make GitHub default branch `main`;
6. verify open PR queue has no stale overlapping implementation work;
7. close Issue #37 and mark Phase 0 complete;
8. begin Phase 1 from clean `main`.
