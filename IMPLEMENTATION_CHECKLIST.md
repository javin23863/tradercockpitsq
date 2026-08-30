# TraderCockpit Product Implementation Checklist

This checklist is the binding implementation and acceptance index for the clean `tradercockpitsq` product line. It supersedes the earlier Futures-bound Phase-1 interpretation and must be read with `AGENTS.md` and `docs/product-architecture-v1.md`.

The product-code baseline audited before this authority repair was `main@6be6c78608500cd2d635f632a73a3f8f0030588b`. That baseline passed the complete GitHub Actions gate: production package install, production-boundary check, product tests, frontend syntax checks, UI/run-read tests, real server startup, and Chromium browser regression.

## Non-negotiable product rules

- TraderCockpit is a capability graph, not one mandatory funnel.
- Initial Test is optional.
- Fast and Golden are independent peer validation lanes.
- Search/evolution fitness, Pareto rank, and objective score are discovery evidence, not validation or champion status.
- B and A+ champion status must be separate promotion/evidence records produced by qualifying Fast and Golden results respectively.
- Validation stage names/counts come from backend-owned plans, not frontend constants.
- Prop Simulation is optional and real-producer/rule-set bound.
- Apollo is guide-only authority; it cannot silently alter strategy semantics, launch compute, certify/promote results, export, or delete evidence.
- Frontend capability/data availability comes from backend authority; unsupported capability is unavailable/refused rather than simulated.
- Production code must not import SQX recovered/reference trees or another repository at runtime.
- `javin23863/futures` is quarantined unless the user explicitly reverses that decision.

## 0. Repository authority and branch hygiene

- [x] `main` contains the consolidated TraderCockpit product code, accepted UI composition, product server, tests, and CI.
- [x] Accepted Cockpit Home and Signals & Models implementation/test blobs were verified identical after transplantation into `main` even though their historical commit ancestry is not preserved.
- [x] Stale semantic-preflight PR was closed after its head was proven fully represented in `main`.
- [x] CI push acceptance runs on `main`.
- [ ] GitHub repository default branch is `main`. **Current external settings blocker:** GitHub still reports `codex/sqx-engine-extract` as default; change this before normal coding resumes.
- [ ] Protect `main` against force-push/deletion and require the product acceptance workflow before merge/push according to repository policy.
- [ ] Protect or otherwise make accepted checkpoint refs immutable. Current branch inventory reports them unprotected.

### Reference lanes retained deliberately

These are evidence sources, not production bases:

- UI product authority: `codex/ui-prototype-authority@53645acfff750672805efd6b20623a0abf36dff1`.
- UI behavior acceptance overlay: `codex/ui-reference-acceptance@26221dccee1541c1fc672f24b75a380cf4371c32`.
- Accepted Signals & Models composition: `codex/ui-signals-models-authority@9086e19e33d5a1f8526d4eb3f8e99d38014db586`; relevant UI/test blobs are already content-identical on `main`.
- SQX capability/parity evidence: `codex/sqx-capability-parity@6f6fb81b450844024e8585503845c4a3316472de`.
- SQX authorized runtime-smoke evidence: `codex/sqx-runtime-smoke@766cdc6e8c2f42e6dee86fd59b38e2862ef235a6`.
- SQX extraction/workflow-history lane: `codex/sqx-engine-extract@c1ae24d2e62acfb8ae8be1aea318a82225490c4b`.

Do not merge these branches wholesale into `main`. Bring across only a deliberately reviewed product change or compact evidence fixture required by an active acceptance target.

## 1. VERIFIED FOUNDATION — identity, custody, lifecycle, evidence, read seam

The following foundation exists and is executable on `main`:

- [x] TraderCockpit-owned production namespace under `product/tradercockpit`.
- [x] Production-boundary enforcement rejects runtime dependency on recovered/reference trees.
- [x] Deterministic canonical serialization and content-addressed identities.
- [x] Immutable `StrategySpecV1`, `CandidateSpecV1`, `DataSpecV1`, `ExecutionSpecV1`, `EngineBuildSpecV1`, and `BacktestRunSpecV1` custody objects.
- [x] Exact run input resolution rejects missing, type-confused, stale, tampered, and cross-object substitutions.
- [x] `BacktestEvaluatorV1` protocol and producer descriptor enforce build/schema compatibility and strategy semantic preflight.
- [x] Result-contract validation requires exact run/build/schema identity.
- [x] Durable run receipt, lifecycle state, result, validation decision, and evidence custody.
- [x] Verified read model cross-checks lifecycle and evidence rather than inferring status from artifact presence.
- [x] Filesystem-backed object/lifecycle persistence.
- [x] Product server exposes a narrow read-only `/api/run-read` seam and refuses when durable producer state is not configured.
- [x] Accepted five-workspace/21-state UI shell, Cockpit Home zones, Signals & Models zones, opaque strategy-reference preservation, persistent Apollo surface, and shared RunSurface are present on `main`.
- [x] Browser acceptance runs against the actual product server in Chromium on CI.

### Foundation is deliberately incomplete

Do not infer these capabilities from the existing contracts/UI:

- [ ] No genuine trading evaluator/provider is yet accepted in production.
- [ ] No producer-owned numerical backtest proof has yet established the first real positive and negative strategy execution through `BacktestEvaluatorV1`.
- [ ] No write/launch HTTP API is accepted.
- [ ] No mutable `StrategyDraft` service or deterministic strategy-gap planner is accepted.
- [ ] No backend `CapabilityManifestV1` authority is accepted.
- [ ] No Fast/Golden plan producer, champion promotion system, evolutionary-search producer, prop-simulation producer, or live execution/monitoring producer is accepted.

## 2. ACTIVE BLOCKER — bind and prove one genuine evaluator/provider

Do not begin production search, Fast/Golden, or broad strategy-construction infrastructure before this gate passes.

### 2A. Provider selection and exact contract

- [ ] Search current TraderCockpit and authorized reference lanes before adding provider code.
- [ ] Name one real execution producer and the exact evidence supporting its use.
- [ ] Bind it through the existing `BacktestEvaluatorV1`; do not introduce a parallel run service, engine abstraction, or generic candidate fallback.
- [ ] Bind exact `EngineBuildSpecV1` revision/artifact digest to the provider actually executed.
- [ ] Define only the strategy semantic schema needed for the first proven fixtures; unresolved ranges/search choices must not enter an executable `StrategySpecV1`/`CandidateSpecV1`.
- [ ] Unsupported entry/exit/sizing/order/session/data/execution semantics fail closed rather than being approximated.

If SQX is selected, the parity/runtime branches above are evidence only. A runtime-smoke trace or recovered class name does not itself satisfy the TraderCockpit evaluator contract.

### 2B. Positive, negative, and numerical proof

- [ ] Execute one genuine non-reference positive strategy through exact candidate/data/execution/build custody and obtain producer-owned `ResultArtifactV1` numerical output.
- [ ] Execute one genuine negative/refused case and prove it cannot manufacture a passed result/evidence chain.
- [ ] Prove strategy/candidate/data/execution/build/run identities are unchanged through execution and result custody.
- [ ] Prove stale/tampered/cross-run/cross-strategy substitutions fail closed where applicable.
- [ ] Important numerical outputs have deterministic regression evidence or explicitly bounded/documented nondeterminism.
- [ ] Repeated deterministic execution of the same canonical request reproduces the expected result identity/values where the provider declares deterministic behavior.
- [ ] No fake evaluator, synthetic pass output, placeholder market series, or tuned acceptance threshold is used as proof.

**Stop rule:** if no genuine producer can execute the selected semantics with adequate provenance, stop on that named gap. Do not respond by inventing an evaluator or weakening the contract.

## 3. FIRST REAL PRODUCT VERTICAL SLICE — only after Section 2

Once the evaluator gate is accepted:

- [ ] Add the smallest producer-backed launch/write seam needed to create one exact run; keep `/api/run-read` as verified read authority rather than duplicating it.
- [ ] Bind a real strategy/candidate selection to exact TraderCockpit identities.
- [ ] Launch one optional Initial Test/backtest through the accepted provider.
- [ ] UI running/terminal states come from producer lifecycle records.
- [ ] Results panels render producer-owned typed values only.
- [ ] Evidence opens the exact manifest/receipt/result chain for the selected invocation.
- [ ] Unsupported actions remain unavailable/refused; every enabled action performs a real operation or navigates to a real record.
- [ ] Add browser/integration proof for launch → lifecycle → read → results/evidence without fabricated state.

## 4. STRATEGY AND CAPABILITY AUTHORITY

After the first real execution slice establishes the semantic vocabulary actually required:

- [ ] Implement mutable `StrategyDraft` separately from immutable executable `StrategySpecV1`.
- [ ] Represent entry, exit, stop, target, sizing, concurrency, fills, fees/slippage, session/overnight behavior as explicit atomic semantics/gaps.
- [ ] Add backend capability/data-requirement authority; frontend and Apollo do not maintain independent feature inventories.
- [ ] A draft cannot execute until required semantic gaps are resolved.
- [ ] Catalog/model attachment changes strategy state only through explicit real operations.

## 5. VALIDATION AND PROMOTION LANES

- [ ] Define backend-owned validation-plan authority.
- [ ] Keep Initial optional and Fast/Golden independently launchable when eligible.
- [ ] Render returned stages/checks rather than hard-coding universal phase counts.
- [ ] Preserve exact strategy/candidate/run/config/data/engine/evidence identity through every stage.
- [ ] Qualifying Fast creates a separate B Champion promotion record.
- [ ] Qualifying Golden creates a separate A+ Champion promotion record.
- [ ] Scenario/stress/OOS/walk-forward/cost evidence remains tied to exact plan/window/configuration identity.

## 6. SEARCH, PROP, MONITORING — only with real producers

- [ ] Stable evaluator/objective interface exists before evolutionary search is production-enabled.
- [ ] Search space declares exactly which dimensions may mutate.
- [ ] Search score/Pareto/objective ranking remains separate from validation/champion evidence.
- [ ] Population, budget, mutation, crossover, selection, islands/migration, MAP-Elites, seed controls appear only when the chosen producer supports them with typed evidence.
- [ ] Prop Simulation remains optional and explicit rule-set/program bound.
- [ ] Monitoring consumes producer-backed lifecycle/health/evidence, not UI inference.

## Global adversarial gate for every feature

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

First correct the repository setting so GitHub's default branch is `main` and establish branch/checkpoint protection. Then perform Section 2 only: select and prove one genuine execution producer against the existing TraderCockpit evaluator/custody contract. Do not resume broad feature coding before that provider proof is accepted.
