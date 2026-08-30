# TraderCockpit Product Implementation Checklist

This is the binding implementation and acceptance index for the clean `tradercockpitsq` product line. Read it with `AGENTS.md` and `docs/product-architecture-v1.md`.

## Product rules that must not regress

- TraderCockpit is a capability graph, not a mandatory funnel.
- Initial Test is optional.
- Fast and Golden are independent peer validation lanes.
- Search/evolution fitness, Pareto rank, and objective score are discovery evidence, not validation or champion status.
- B and A+ champion status are separate promotion/evidence records produced only by qualifying Fast and Golden results.
- Validation stage names and counts come from backend-owned plans, not frontend constants.
- Prop Simulation is optional and must be bound to a real producer/rule set.
- Apollo is guide-only authority; it cannot silently alter strategy semantics, launch compute, certify/promote results, export, or delete evidence.
- Frontend capability/data availability comes from backend authority; unsupported capability is unavailable/refused rather than simulated.
- Production code must not import recovered SQX/reference trees or another repository at runtime.
- `javin23863/futures` is quarantined unless the user explicitly reverses that decision.

## 0. REPOSITORY CLEANUP — COMPLETE

- [x] `main` is the canonical TraderCockpit production line.
- [x] `main` contains the consolidated product kernel, accepted UI composition, product server, tests, and CI.
- [x] Accepted Cockpit Home and Signals & Models implementation/test blobs were verified after transplantation into `main`.
- [x] Product acceptance CI runs on `main` and has passed package install, production-boundary checks, product tests, frontend syntax, UI/run-read tests, real server startup, and Chromium regression.
- [x] All remote branches were enumerated and classified before coding resumed.
- [x] Historical product/UI branches are evidence only, not active implementation spines.
- [x] SQX extraction/parity/runtime-smoke/plugin branches are reference/experimental evidence only, not production bases.
- [x] The original divergent `codex/sqx-engine-extract` tip was preserved at `archive/sqx-engine-extract-2026-08-30@c1ae24d2e62acfb8ae8be1aea318a82225490c4b`.
- [x] GitHub's legacy-configured default branch name `codex/sqx-engine-extract` is pinned to the exact canonical `main` head, so a default checkout cannot land on the old divergent SQX tree. `main` remains the branch to name explicitly for all implementation and acceptance work.
- [x] Repository operating authority is restored in `AGENTS.md`; concurrent lanes require isolated worktrees and the primary assistant owns review/correction.
- [x] Clean product architecture is restored in `docs/product-architecture-v1.md`.
- [x] No production, frontend, or test implementation was lost during cleanup.

### Retained evidence branches

Do not merge these branches wholesale into `main`. Use only deliberately reviewed evidence required by an active acceptance target.

- UI product authority: `codex/ui-prototype-authority@53645acfff750672805efd6b20623a0abf36dff1`.
- UI behavior acceptance overlay: `codex/ui-reference-acceptance@26221dccee1541c1fc672f24b75a380cf4371c32`.
- Accepted Signals & Models composition: `codex/ui-signals-models-authority@9086e19e33d5a1f8526d4eb3f8e99d38014db586`.
- SQX capability/parity evidence: `codex/sqx-capability-parity@6f6fb81b450844024e8585503845c4a3316472de`.
- SQX authorized runtime-smoke evidence: `codex/sqx-runtime-smoke@766cdc6e8c2f42e6dee86fd59b38e2862ef235a6`.
- Preserved pre-cleanup SQX extraction/workflow history: `archive/sqx-engine-extract-2026-08-30@c1ae24d2e62acfb8ae8be1aea318a82225490c4b`.

## 1. VERIFIED FOUNDATION

The following exists and is executable on `main`:

- [x] TraderCockpit-owned production namespace under `product/tradercockpit`.
- [x] Production-boundary enforcement rejects runtime dependency on recovered/reference trees.
- [x] Deterministic canonical serialization and content-addressed identities.
- [x] Immutable `StrategySpecV1`, `CandidateSpecV1`, `DataSpecV1`, `ExecutionSpecV1`, `EngineBuildSpecV1`, and `BacktestRunSpecV1` custody objects.
- [x] Exact run input resolution rejects missing, type-confused, stale, tampered, and cross-object substitutions.
- [x] `BacktestEvaluatorV1` and producer descriptor enforce build/schema compatibility and strategy semantic preflight.
- [x] Result-contract validation requires exact run/build/schema identity.
- [x] Durable run receipt, lifecycle state, result, validation decision, and evidence custody.
- [x] Verified read model cross-checks lifecycle and evidence rather than inferring status from artifact presence.
- [x] Filesystem-backed object/lifecycle persistence.
- [x] Product server exposes the verified read-only `/api/run-read` seam and refuses when producer state is not configured.
- [x] Five-workspace/21-state UI shell, Cockpit Home, Signals & Models, opaque strategy-reference preservation, persistent Apollo, and shared RunSurface are present.
- [x] Browser acceptance runs against the actual product server in Chromium.

### Deliberately not yet proven

- [ ] No genuine production trading evaluator/provider is accepted yet.
- [ ] No producer-owned numerical positive/negative backtest proof has passed through `BacktestEvaluatorV1` yet.
- [ ] No accepted launch/write HTTP API exists yet.
- [ ] No accepted mutable `StrategyDraft` service or strategy-gap planner exists yet.
- [ ] No accepted backend `CapabilityManifestV1` authority exists yet.
- [ ] No accepted Fast/Golden plan producer, champion promotion system, evolutionary-search producer, prop-simulation producer, or live execution/monitoring producer exists yet.

## 2. ACTIVE IMPLEMENTATION GATE — PROVE ONE GENUINE EVALUATOR/PROVIDER

Do not begin production search, Fast/Golden, or broad strategy-construction infrastructure before this gate passes.

### Provider contract

- [ ] Search current TraderCockpit and retained evidence lanes before adding provider code.
- [ ] Name one real execution producer and the exact evidence supporting its use.
- [ ] Bind it through existing `BacktestEvaluatorV1`; do not create a parallel run service, engine abstraction, or generic candidate fallback.
- [ ] Bind exact `EngineBuildSpecV1` revision/artifact digest to the provider actually executed.
- [ ] Define only the strategy semantic schema required by the first proven fixtures; unresolved ranges/search choices must not enter executable specs.
- [ ] Unsupported entry/exit/sizing/order/session/data/execution semantics fail closed rather than being approximated.

### Acceptance proof

- [ ] Execute one genuine non-reference positive strategy through exact candidate/data/execution/build custody and obtain producer-owned `ResultArtifactV1` numerical output.
- [ ] Execute one genuine negative/refused case and prove it cannot manufacture a passed result/evidence chain.
- [ ] Preserve exact strategy/candidate/data/execution/build/run identities through execution and result custody.
- [ ] Reject stale/tampered/cross-run/cross-strategy substitutions where applicable.
- [ ] Cover important numerical outputs with deterministic regression evidence or explicitly bounded/documented nondeterminism.
- [ ] Do not use a fake evaluator, synthetic pass output, placeholder market series, or tuned acceptance threshold as proof.

**Stop rule:** if no genuine producer can execute the selected semantics with adequate provenance, stop on that named gap. Do not invent an evaluator or weaken the contract.

## 3. FIRST REAL PRODUCT VERTICAL SLICE — AFTER SECTION 2

- [ ] Add the smallest producer-backed launch/write seam needed to create one exact run; do not duplicate `/api/run-read`.
- [ ] Bind real strategy/candidate selection to exact TraderCockpit identities.
- [ ] Launch one optional Initial Test/backtest through the accepted provider.
- [ ] UI running/terminal states come from producer lifecycle records.
- [ ] Results panels render producer-owned typed values only.
- [ ] Evidence opens the exact manifest/receipt/result chain for the selected invocation.
- [ ] Unsupported actions remain unavailable/refused; every enabled action performs a real operation or navigates to a real record.
- [ ] Add browser/integration proof for launch -> lifecycle -> read -> results/evidence without fabricated state.

## Global adversarial gate

Before declaring any feature complete, require all applicable:

- [ ] Existing implementation/reference searched first.
- [ ] One authoritative producer named.
- [ ] Exact input/output identities explicit.
- [ ] Unsupported/malformed input fails closed.
- [ ] Positive executable proof exists.
- [ ] Negative/refusal proof exists.
- [ ] Cross-run/cross-strategy/stale/tampered substitution rejected where applicable.
- [ ] Numeric outputs are producer-owned and regression-covered.
- [ ] UI consumes typed backend output rather than labels/routes/local whitelists.
- [ ] Evidence/provenance survives lifecycle/status/resume.
- [ ] Ready/running/passed/failed/refused/unavailable states exist where applicable.
- [ ] No duplicate subsystem, second pipeline, or speculative fallback was introduced.

## Current next action

Repository cleanup is complete. Begin Section 2 only: select and prove one genuine execution producer against the existing TraderCockpit evaluator/custody contract.
