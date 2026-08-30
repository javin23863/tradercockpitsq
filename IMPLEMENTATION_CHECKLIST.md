# TraderCockpit Product Implementation Checklist

This checklist is binding for the clean `tradercockpitsq` product direction. It supersedes the earlier Futures-bound interpretation of Phase 1.

Read first:

- `docs/product-architecture-v1.md`
- `docs/agent-delegation-2026-08-30.md`
- `references/ui-authority/DESKTOP_AGENT.md`

## Non-negotiable boundaries

- [ ] Production code imports nothing from `sources/**`.
- [ ] Production code imports nothing from `references/**`.
- [ ] Production code has no runtime dependency on `javin23863/futures`.
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

- [ ] Create the production namespace under `product/`.
- [ ] Add an executable check that rejects production imports from `sources/**` and `references/**`.
- [ ] Keep parity/reference tests outside the production namespace.

## 1B. Canonical identity

- [ ] Define deterministic canonical serialization for production specs.
- [ ] Define content-addressed refs with explicit schema/kind/version boundaries.
- [ ] Prove dictionary key order cannot change identity.
- [ ] Refuse non-canonical/unsupported payload types rather than stringifying them silently.

## 1C. Immutable executable specs

- [ ] `StrategySpecV1` — fully resolved trading semantics only.
- [ ] `CandidateSpecV1` — fully materialized executable candidate with parent lineage.
- [ ] `DataSpecV1` — exact data/timeframe/source/session/range identity.
- [ ] `ExecutionSpecV1` — capital/fees/slippage/fill/order timing and execution assumptions.
- [ ] `BacktestRunSpecV1` — binds candidate + data + execution + engine build identity.
- [ ] Semantic changes produce new content identities.
- [ ] Notes/display metadata do not accidentally change executable semantic identity.
- [ ] Unresolved search ranges do not appear inside executable StrategySpec/CandidateSpec.

# 2. First real engine proof

- [ ] Define a narrow deterministic backtest/evaluator interface using TraderCockpit-owned specs.
- [ ] Execute the legacy donor known-pass fixture through the new engine and obtain the expected pass behavior.
- [ ] Execute the known-fail fixture and obtain the expected failure behavior.
- [ ] Preserve exact strategy/candidate/data/execution/engine identity in results.
- [ ] No generic/default candidate substitution exists.
- [ ] Important numerical outputs have deterministic regression evidence.
- [ ] Same canonical run request reproduces the same result identity where the engine declares determinism.

# 3. Initial validation and evidence

- [ ] Define `RunReceiptV1` that freezes launch identities.
- [ ] Define typed `ResultArtifactV1` outputs.
- [ ] Define `InitialValidationPlanV1` / initial gate policy independently of donor implementation names.
- [ ] Define `EvidenceManifestV1` tying evaluated results/provenance together.
- [ ] Prove known-pass -> PASS and known-fail -> FAIL without weakened policy.
- [ ] Missing/tampered/stale/mismatched evidence fails closed.

# 4. First real UI vertical slice

- [ ] Strategy workspace binds to real StrategyDraft/StrategySpec identity.
- [ ] Initial Test launches one real BacktestRunSpec.
- [ ] Running state comes from a real lifecycle producer.
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

## Current next action

Complete Section 1 first while the three evidence/reference lanes proceed independently. Then consume their fixtures to complete Sections 2 and 3 before production evolutionary-search implementation begins.
