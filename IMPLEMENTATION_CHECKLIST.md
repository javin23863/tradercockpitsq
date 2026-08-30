# TraderCockpit Product Implementation Checklist

This checklist is binding for the clean `tradercockpitsq` product direction. It supersedes the earlier Futures-bound interpretation of Phase 1.

Read first:

- `docs/product-architecture-v1.md`
- `docs/agent-delegation-2026-08-30.md`
- `references/ui-authority/DESKTOP_AGENT.md`

## Non-negotiable boundaries

- [x] Production code imports nothing from `sources/**`.
- [x] Production code imports nothing from `references/**`.
- [x] Production code has no runtime dependency on `javin23863/futures`; the package declares no runtime dependencies, installs with `--no-deps`, and the production import gate explicitly refuses legacy `futures.*` imports while allowing stdlib `concurrent.futures`.
- [ ] Recovered SQX and Futures behavior enters production only through deliberate parity evidence or a reviewed minimal port.
- [ ] Unsupported semantics fail closed.
- [ ] No feature is marked done from screenshots, docs, types, mocks, fabricated metrics, or recovered class names alone.

## Product rules

- [ ] Initial Test remains optional.
- [ ] Fast and Golden remain independent peer lanes.
- [ ] B Champion is a promotion/evidence record produced by a qualifying Fast run.
- [ ] A+ Champion is a promotion/evidence record produced by a qualifying Golden run.
- [ ] Evolution/search score never implies validation/champion status.
- [ ] Validation stage names/counts come from backend-owned plans rather than frontend constants.
- [ ] Prop Simulation remains optional and real-producer/rule-set bound.
- [ ] Apollo guides over deterministic backend authority and cannot silently change semantics, launch compute, promote/certify, export, or delete evidence.

# 0. Reference and custody lanes

- [x] Recover five canonical multicolor TraderCockpit UI baselines.
- [x] Pin their source identities/hashes and repository-visible previews.
- [ ] Ingest and verify all 35 supplied StrategyQuant/Builder panel snapshots.
- [ ] Recover provenance-backed legacy known-pass and known-fail parity fixtures.
- [ ] Recover compact SQX capability/parity evidence without making recovered code production-importable.

# 1. ACTIVE — clean production foundation

## 1A. Runtime boundary

- [x] Create the production namespace under `product/`.
- [x] Add an executable check that rejects production imports from `sources/**` and `references/**`.
- [x] Keep parity/reference tests outside the production namespace.
- [x] Run the production-boundary and product-domain suite in a clean checkout on every product-domain push.

## 1B. Canonical identity

- [x] Define deterministic canonical serialization for production specs.
- [x] Define content-addressed refs with explicit schema/kind/version boundaries.
- [x] Prove dictionary key order cannot change identity.
- [x] Refuse non-canonical/unsupported payload types rather than stringifying them silently.
- [x] Reserve canonical type-tag namespace so typed values cannot alias ordinary user mappings.
- [x] Strictly decode canonical bytes: duplicate keys, JSON floats, unknown tags, alternate Decimal text, or noncanonical bytes fail closed.

## 1C. Immutable executable specs

- [x] `StrategySpecV1` — immutable strategy meaning under an explicit semantic-schema version; engine support remains separate.
- [x] `CandidateSpecV1` — fully materialized candidate identity with immutable parent/origin lineage.
- [x] `DataSpecV1` — exact data/timeframe/source/session/range identity with canonical UTC range boundaries.
- [x] `ExecutionSpecV1` — capital plus uniquely keyed execution-model assumptions with order-invariant identity.
- [x] `EngineBuildSpecV1` — exact implementation revision and artifact digest identity.
- [x] `BacktestRunSpecV1` — binds candidate + data + execution + engine build identity and optional run seed.
- [x] Semantic changes produce new content identities.
- [x] Notes/display metadata do not accidentally change executable semantic identity because they are not part of `StrategySpecV1`.
- [ ] Semantic-schema validation proves executable StrategySpec/CandidateSpec contains no unresolved search ranges. Pending recovered rule vocabulary/parity evidence.

## 1D. Strict engine input custody

- [x] Resolve run candidate/data/execution/engine objects by exact content address.
- [x] Resolve the exact strategy through `candidate.strategy_ref`.
- [x] Missing immutable objects fail closed.
- [x] Type-confused resolver substitutions fail closed.
- [x] Stale/tampered object substitutions fail closed when their computed content address differs.
- [x] Cross-run data and candidate/strategy substitutions are rejected before engine evaluation.
- [x] Persist and reopen all immutable run/evidence objects through a content-addressed filesystem store.
- [x] Wrong-path, corrupt, hash-mismatched, or noncanonical stored objects fail closed.

# 2. First real engine proof

- [x] Define a narrow backtest/evaluator interface using TraderCockpit-owned specs.
- [x] Separate evaluator preflight from execution so build/schema incompatibility is refused before compute.
- [ ] Execute the legacy donor known-pass fixture through the new engine and obtain the expected pass behavior.
- [ ] Execute the known-fail fixture and obtain the expected failure behavior.
- [x] Preserve exact strategy/candidate/data/execution/engine identity in result custody.
- [x] No generic/default candidate substitution exists in the evaluator/run-service path.
- [ ] Important numerical outputs have deterministic regression evidence from the real evaluator.
- [x] Same canonical run request can reproduce the same result identity when the injected evaluator declares deterministic behavior.
- [x] Durable run service resolves exact input refs, persists launch receipt before compute, persists only validated results, and constructs the complete initial evidence chain.
- [x] Producer failure leaves a durable launch receipt without manufacturing a result/decision/evidence success artifact.

# 3. Initial validation, lifecycle, and evidence

- [x] Define `RunReceiptV1` that freezes launch identities.
- [x] Define typed `ResultArtifactV1` outputs.
- [x] Define `InitialValidationPlanV1` / initial gate policy independently of donor implementation names.
- [x] Define `ValidationDecisionV1` with deterministic gate ordering and comparison-verified outcomes.
- [x] Define `EvidenceManifestV1` tying evaluated results/provenance together.
- [x] Define immutable producer-owned run lifecycle events with explicit `ready`, `running`, `passed`, `failed`, and `refused` states.
- [x] Persist lifecycle history and the current invocation head separately from evidence artifacts; stale transitions, terminal rewrites, and tampered/noncanonical heads fail closed.
- [x] Drive the initial run service from explicit lifecycle transitions rather than inferring status from artifact presence.
- [x] Define a verified initial-run read model that cross-checks lifecycle, receipt, result, plan, decision, and evidence custody before exposing current state to an API/UI consumer.
- [x] Missing lifecycle state is reported as missing state rather than inferred from existing run artifacts.
- [ ] Prove donor known-pass -> PASS and donor known-fail -> FAIL without weakened policy.
- [x] Missing/tampered/stale/mismatched evidence fails closed for the implemented initial evidence chain.
- [x] Cross-run receipt/result substitutions and forged validation decisions or forged passed lifecycle states are rejected.

# 4. First real UI vertical slice

Backend prerequisites now include an explicit lifecycle producer and verified read model, but no UI checkbox below is complete until the product UI actually consumes them.

- [ ] Strategy workspace binds to real StrategyDraft/StrategySpec identity.
- [ ] Initial Test launches one real BacktestRunSpec.
- [ ] Running state in the UI comes from the real lifecycle producer.
- [ ] Results panels use real ResultArtifact values only.
- [ ] Evidence panel opens the exact EvidenceManifest for the selected run.
- [ ] Enabled buttons either perform a real operation or navigate to a real record.
- [ ] Unsupported panels/actions show unavailable/refused state rather than prototype success.

# 5. Strategy construction and deterministic gap resolution

- [ ] Mutable `StrategyDraft` is distinct from executable `StrategySpecV1`.
- [ ] Entry, exit, stop, target, sizing, concurrency, fills, fees/slippage, session/overnight semantics are explicit atomic gaps.
- [ ] Strategy Gap Planner uses backend capability authority rather than LLM invention.
- [ ] Research/source facts retain citations/applicability.
- [ ] A draft cannot execute until required semantic gaps are resolved.

# 6. Fast validation lane

- [ ] Define `ValidationPlanSpecV1` as backend-owned plan authority.
- [ ] Fast plan is independently launchable when eligible.
- [ ] Every stage/gate produces typed evidence/refusal.
- [ ] Qualifying Fast output creates a separate B Champion promotion record.
- [ ] B Champion preserves exact candidate/run/config/data/evidence identity.

# 7. Stable evaluator/objectives

- [ ] Expose a bounded candidate evaluator suitable for search.
- [ ] Objective vectors are producer-owned and reproducible from evaluated artifacts.
- [ ] Search/discovery metrics remain distinct from validation metrics.

# 8. Evolutionary search

- [ ] Define `SearchSpaceSpecV1` — only dimensions search is allowed to change.
- [ ] Define `SearchRunSpecV1` — algorithm/budget/population/seed/policies/objectives only when supported.
- [ ] Candidate lineage preserves parent strategy/search-run identity.
- [ ] Mutation cannot alter undeclared dimensions.
- [ ] Invalid/unsupported search controls fail closed.
- [ ] Fitness evolution/Pareto/archive/island outputs appear only when a real producer supports them.
- [ ] Search promotion to Test & Validate requires an explicit operation.

# 9. Golden and advanced validation

- [ ] Golden remains independent of Fast completion.
- [ ] Golden plan/results come from backend-owned capability records.
- [ ] Add recovered capabilities deliberately: Monte Carlo, stress, OOS/WFO/WFM, sequential optimization, higher precision, What-If, parameter permutation, and others only when executable.
- [ ] Qualifying Golden output creates a separate A+ Champion promotion record.
- [ ] Advanced evidence preserves exact windows/configuration/evaluated-set identity.

# 10. Prop, monitoring, governance, delivery

- [ ] Prop Simulation uses explicit TraderCockpit rule/program specs; do not claim an inherited native SQX prop module.
- [ ] Prop results cannot retroactively alter Fast/Golden status.
- [ ] Monitoring consumes producer-backed lifecycle/health/evidence, not UI inference.
- [ ] Proof/delivery rungs remain distinct: source, checked, compiled, delivered, execution-proven, refused/unavailable as applicable.
- [ ] Customer status projections redact raw internal paths/tracebacks.

## Global adversarial gate for any completed feature

Before checking a feature complete, require all applicable:

- [ ] Existing implementation/reference was searched first.
- [ ] Authoritative production owner/producer is named.
- [ ] Input and output identities are explicit.
- [ ] Positive executable proof exists.
- [ ] Negative/refusal proof exists.
- [ ] Cross-run/cross-strategy/tampered/stale/cross-tenant substitution is rejected where applicable.
- [ ] Numerical outputs are deterministic or have explicitly bounded nondeterminism.
- [ ] UI reads typed backend output rather than inferring capability from names/IDs/routes.
- [ ] Evidence/provenance survives lifecycle/status/resume.
- [ ] Ready/running/passed/failed/refused/unavailable states exist where applicable.
- [ ] No duplicate subsystem or speculative fallback was introduced.

## Current verified checkpoint

Clean-checkout acceptance at `42dbf7bbea0d4b09991e56eab640d4f80bd912f7` installs `tradercockpit-core` with `--no-deps`, passes the production-boundary gate, and passes 91/91 product tests. The backend now has durable input/evidence custody, explicit lifecycle state, and a verified read model suitable for the first UI slice without inferring state.

## Current next action

The binding engine dependency is now external to this lane: consume the provenance-backed donor known-pass/known-fail fixtures when that lane lands, define only the semantic-schema vocabulary required by those fixtures, and implement the first real deterministic evaluator. Do not manufacture a synthetic trading evaluator and do not begin production evolutionary-search implementation before the real evaluator produces numerical PASS/FAIL evidence. Until donor evidence lands, additional backend infrastructure is not a substitute for that proof.
