# TraderCockpit Product Implementation Checklist

**Purpose:** durable cross-repository execution checklist for turning the recovered TraderCockpit product authority into a working product. This is an execution index, not a replacement architecture. Preserve the existing TraderCockpit `PRODUCT.md` / living `STATUS.md`, the Futures Phase-1 checkpoint, and the existing engine/runtime implementations.

**UI authority:** `references/ui-authority/README.md`, `manifest.json`, previews, and panel snapshots.

**Current implementation rule:** recover and connect existing code first. Add code only where an executable missing seam is proven. Documentation never substitutes for executable proof.

## Product model that must not regress

TraderCockpit is a **capability graph**, not one mandatory funnel. The UI may present a useful next action, but runtime authority comes from backend-produced capability/plan records.

- Initial Test is an optional quick backtest/screen.
- Fast and Golden are independent robustness/validation lanes. Fast does not unlock Golden; Golden does not require Fast.
- Fast produces stored **B Champions** when its authoritative criteria are satisfied.
- Golden produces stored **A+ Champions** when its authoritative criteria are satisfied.
- Scenario, stress, OOS/walk-forward, cost and other checks are stages inside authoritative backend plans; do not hardcode a universal order or count in the UI.
- Prop Simulation is optional and rule-set selected. It may target an eligible strategy, candidate or champion; it is not a required post-Golden step.
- Evolutionary Search is first-class discovery. Evolution score, Pareto rank and search fitness are not validation, proof or champion status.
- Apollo guides users over deterministic backend authority. It may explain, recommend with evidence, and ask a plain-English question when real ambiguity remains; it must not silently change strategy meaning, start compute, promote/certify results, export code, or delete evidence.
- The frontend and Apollo must not maintain independent feature inventories. Capability availability, data requirements and gaps come from backend contract authority.

## Global adversarial gate for every feature

A feature is not `DONE` merely because a route, card or mock screen exists. Before checking it off, require all applicable items below:

- [ ] Existing implementation/producer was searched for before adding code.
- [ ] One authoritative backend owner/producer is named.
- [ ] Input identity and run/config/data/strategy/candidate identity are explicit.
- [ ] Unsupported or unavailable semantics fail closed instead of being approximated.
- [ ] Positive executable test proves the real path.
- [ ] Negative/refusal test proves unsupported/malformed input cannot pass.
- [ ] Cross-run, cross-strategy, stale, tampered and cross-tenant substitution are rejected where applicable.
- [ ] Numeric outputs are producer-owned and covered by deterministic or numerical regression; the UI does not invent professional statistics.
- [ ] The UI consumer reads typed backend output rather than classifying route names, ID prefixes, labels or local whitelists.
- [ ] Evidence/provenance survives status/results/resume and cannot be silently compacted away.
- [ ] A concrete UI state exists for ready, running, passed, failed/refused and unavailable when those states apply.
- [ ] No duplicate subsystem, second pipeline or speculative fallback was introduced.

---

# 0. Authority and custody

- [x] Recover the multicolor ESQ TraderCockpit prototype as product UI authority.
- [x] Supersede the old dark-blue `Chart / Backtest / Proof` shell as frontend authority.
- [x] Pin five canonical screenshot identities/hashes in `references/ui-authority/manifest.json`.
- [x] Commit repository-visible visual previews for the desktop agent.
- [x] Create `references/ui-authority/panel-snapshots/` intake contract for panel/state reference captures.
- [ ] Add uploaded panel snapshots and a panel manifest as they are supplied; never silently replace or regenerate source captures.

# 1. ACTIVE — prove the engine can approve one real stored strategy

This is the current blocking implementation milestone. Do not skip it to build later screens.

**Futures recovery context at the reviewed handoff:**

- repository: `javin23863/futures`
- working branch at handoff: `codex/phase1-strategy-binding-main`
- last verified handoff head: `9372ee5fbdf867fa248b9a040f5b96c41e46d41a`
- frozen checkpoint: `docs/checkpoints/PHASE1-2026-08-30-worker-phase01.md`
- recovery code anchor: `324d0cbe7ace93a1dd8cc6384df5497fd91da8c8`

Always inspect current HEAD before editing; do not reset legitimate newer work to these historical anchors.

## 1A. Reuse the existing execution path

Existing seams named by the reviewed handoff must be reused unless an executable failure proves a change is necessary:

- `packages/esq/genesis/strategy_run_binding.py`
- `packages/esq/genesis/strategy_symbol_binding.py`
- `scripts/engine_worker.py`
- `scripts/run_robustness.py`
- `packages/esq/robustness_pipeline/orchestrator.py`
- `packages/esq/robustness_pipeline/phases/phase01_intake.py`
- `configs/robustness.yaml`

- [ ] Execute the existing deterministic Phase-01 path and identify an already-proven **non-reference** passing strategy/candidate.
- [ ] Do not invent an easy-pass strategy and do not tune gate thresholds.
- [ ] Express the exact passing semantics as the canonical stored hypothesis.
- [ ] Verify stored `hypothesis_ref + hypothesis_sha256` content addressing.
- [ ] Compile through the existing `compile_hypothesis()` path.
- [ ] Produce exactly one executable `CandidateSpec(source="hypothesis")` with verified `spec_sha256`.
- [ ] Refuse unsupported sizing/exit/predicate/target semantics instead of approximating them.
- [ ] Inject that exact candidate through the existing candidate-loader seam; generic candidate fallback must not execute.
- [ ] Run the existing `phase01_intake` with the shipped gates unchanged.

## 1B. Phase-01 acceptance proof

The real strategy proof must require all of the following from the canonical Phase-01 artifact shape:

- [ ] entering strategy/candidate identity equals the signed customer `spec_id`.
- [ ] `surviving_real == [spec_id]`.
- [ ] `dropped == []`.
- [ ] candidate verdict is `pass`.
- [ ] reference-lane presence in `surviving` is **not** counted as success.
- [ ] strategy → hypothesis → candidate hashes/identity remain equal to persisted binding/provenance.
- [ ] replay/determinism test remains green.

Focused test order from the reviewed handoff:

1. `tests/fast/test_robustness_determinism.py`
2. compiler/binding tests such as `tests/fast/test_strategy_run_binding.py` when touched
3. `tests/fast/test_strategy_phase01_e2e.py`
4. `tests/fast/test_golden_e2e_smoke.py` only when needed to validate the selected existing control
5. narrow DSL/exit tests for any exact semantic added

## 1C. Worker/process-boundary proof

Run the same exact stored strategy through:

`engine_worker.start_run()` → persisted strategy binding → existing robustness child → `phase01_intake` → durable `run_status` → `run_manifest/provenance`.

- [ ] Same genuine Phase-01 pass appears through the real worker boundary.
- [ ] Status/results/proof preserve signed strategy, hypothesis and candidate identity.
- [ ] Resume requires identity equality and refuses missing/unreadable/tampered prior evidence.
- [ ] Generic candidate fallback remains impossible.
- [ ] Concrete child/runtime defects are fixed at their actual seam; do not redesign `engine_worker_core.py` speculatively.

## 1D. Worker lock rule

TraderCockpit's worker lock remains unchanged until all are true:

- [ ] accepted Futures source exists;
- [ ] real executable source tests pass;
- [ ] replacement worker artifact is built from accepted source;
- [ ] artifact identity/digest is verified;
- [ ] packaged worker executes the stored-strategy Phase-01 proof successfully.

Only then may the worker lock be repinned.

**Stop rule:** if no real non-reference pass exists, exact hypothesis semantics cannot represent the positive control, or a concrete worker assertion fails, stop on that named defect. Do not respond by creating a new Phase-1 runner, second robustness pipeline, new strategy execution subsystem, weakened gate profile or generic fallback.

---

# 2. Strategy construction, research and deterministic gap resolution

Prototype consumers: `order-flow-signals-models`, `indicators-models-catalog`, Cockpit Home / Alpha Stack.

- [ ] Reuse/extend the existing generated backend capability registry; do not create a second feature inventory.
- [ ] Reuse/extend the deterministic Strategy Gap Plan rather than asking one giant LLM-generated strategy question.
- [ ] Represent entry, exit, stop, target, sizing, concurrency, fills, fees/slippage, session/overnight behavior as atomic semantics/gaps.
- [ ] Distinguish source-defined facts, deterministic implications, evidence-backed recommendations and genuine user choices.
- [ ] Preserve citations/applicability for recommendations; prose alone cannot mint evidence/receipts.
- [ ] Make capability/data requirements producer-owned and typed (`available`, unavailable/refused, requirements, provenance).
- [ ] Connect catalog selections (`Add to chart`, `Add to strategy`, compare) to real strategy workspace state without changing semantics silently.
- [ ] Persist the resulting signed strategy/hypothesis identity before compute becomes eligible.

# 3. Candidate generation

- [ ] Connect manual/bounded candidate construction to the approved strategy/search contract.
- [ ] Every candidate is content-addressed and tied to exact parent strategy/hypothesis identity.
- [ ] Candidate mutation may change only declared search dimensions.
- [ ] Store candidate lineage and searchable comparison metrics without treating discovery metrics as validation.
- [ ] UI candidate table opens real run/evidence records, not prototype-only IDs.

# 4. Evolutionary Search

Prototype consumer: `evolutionary_search_trading_dashboard`.

- [ ] Discover the actual existing SQX/Futures producer(s) for population/search operations before building adapters.
- [ ] Publish bounded search contracts: population, generations/evaluation budget, mutation, elite/selection mode, deterministic seed and supported objectives.
- [ ] Repeated execution with the same seed/contract/data has deterministic identity or explicitly documented allowed nondeterminism.
- [ ] Expose live search progress from producer-owned state.
- [ ] Expose fitness evolution, Pareto frontier and survivor/candidate records from real outputs.
- [ ] Preserve the rule: **Evolution Score ≠ Validation**.
- [ ] Promote a candidate to Test & Validate only through an explicit operation; search score alone never creates B/A+ champion status.
- [ ] MAP-Elites/archive UI is enabled only when a real producer and typed archive record exist.
- [ ] Island/migration controls are enabled only when a real producer exists. If absent, show unavailable/read-only state rather than fabricating four-island behavior from the prototype.

# 5. Initial Test / ordinary backtest

- [ ] Initial Test remains optional; it is not a required predecessor to Fast or Golden.
- [ ] Execute the exact selected strategy/candidate through the ordinary backtest path with no default-candidate fallback.
- [ ] Produce run-owned backtest/trade/equity/config/data identity records.
- [ ] Professional statistics come from deterministic producers/read models; missing statistics remain typed unavailable gaps.
- [ ] Costs, fees, slippage, sessions and data assumptions are explicit and attached to the run.
- [ ] UI panels open real charts/trades/config/evidence for the selected run.

# 6. Fast robustness lane

- [ ] Fast plan is fetched from backend authority; frontend does not hardcode phase names/counts.
- [ ] Run exact selected strategy/candidate set with immutable plan/config/data identity.
- [ ] Store every gate/stage result and refusal reason.
- [ ] Promote only qualifying outputs to stored **B Champions**.
- [ ] B Champion identity points back to exact strategy/candidate/run/evidence.
- [ ] Fast lane remains independently launchable when eligible; no Golden prerequisite or automatic Golden promotion.

# 7. Golden robustness / full validation lane

- [ ] Golden plan is fetched from backend authority; frontend does not assume Fast completion.
- [ ] Execute full plan with its real governance/attribution requirements.
- [ ] Scenario, stress, OOS/walk-forward, costs and other checks are rendered from the returned plan/result records rather than hardcoded universal stages.
- [ ] Promote only qualifying outputs to stored **A+ Champions**.
- [ ] A+ identity preserves exact evaluated set, configuration, data, engine and evidence boundaries.
- [ ] Fast and Golden comparison is read-only evidence comparison unless the user explicitly launches another operation.

# 8. Scenario / stress / OOS and comparison surfaces

- [ ] Scenario definitions are immutable, identified and evidence-bearing.
- [ ] Stress/OOS/walk-forward outputs preserve exact data windows and configuration.
- [ ] Receipt/evidence substitution across value, run, data window, strategy, tenant or field fails closed.
- [ ] Stale applicability is surfaced rather than reused silently.
- [ ] Comparison UI distinguishes different strategies, candidates, champions, lanes and data/config revisions.

# 9. Prop Simulation

- [ ] Keep Prop Simulation optional and capability/rule-set selected.
- [ ] Load real rule/program options from backend producer authority; do not invent prop-firm rules in the UI.
- [ ] Allow eligible strategy/candidate/champion targets according to typed capability policy.
- [ ] Persist rule-set/program version, account assumptions, run identity and pass/fail evidence.
- [ ] Prop output never retroactively changes Fast/Golden validation status.

# 10. Evidence, proof, governance and monitoring

- [ ] Every run exposes closed, redacted status/results projections; raw worker paths/tracebacks are not customer data.
- [ ] Run proof includes exact strategy/candidate, run, configuration, engine, data, lane/profile/evaluated-set and issuance/signature boundaries where supported.
- [ ] Delivery/proof targets keep independent rungs such as source, checked, compiled, delivered, execution-proven and refused/unavailable; do not collapse them into a generic receipt count.
- [ ] Evidence references cannot be removed by compaction.
- [ ] Monitor consumes producer-backed lifecycle/health/market-fit/edge-decay state; it does not infer health from UI presence.
- [ ] Alerts link to the exact evidence that caused the alert and state applicability limits.

# 11. Frontend product connection

The implementation target is a working vertical product, not screenshots wired to static data.

For every visible prototype panel/action:

- [ ] Name the backend capability/producer it consumes.
- [ ] If no producer exists, mark the panel/action unavailable and add the missing producer to this checklist; do not fabricate data.
- [ ] Bind selection state (strategy/candidate/champion/run) through typed IDs, not labels.
- [ ] Bind loading/running/refused/completed states to real lifecycle records.
- [ ] Make all numerical cards producer-owned.
- [ ] Keep Apollo composer context tied to the currently selected authoritative objects/evidence.
- [ ] Remove dead buttons: every enabled action must perform a real operation or navigate to a real record.
- [ ] Add one browser/integration regression for the real consumer seam, not just component snapshots.

# 12. Product-level vertical acceptance

The first product slice is complete only when a user can perform this with real state:

- [ ] open/create a strategy from research/signals;
- [ ] resolve required semantic gaps and persist a signed canonical strategy/hypothesis;
- [ ] create/select an exact candidate (manual or search-produced);
- [ ] execute an Initial, Fast or Golden operation that is genuinely eligible;
- [ ] observe real run lifecycle;
- [ ] open run-owned backtest/robustness results and evidence;
- [ ] receive a real B/A+ promotion only when the selected lane's criteria pass;
- [ ] optionally launch supported scenario/prop operations;
- [ ] return later and recover the same objects through durable IDs/provenance;
- [ ] see explicit unavailable/refusal states for unsupported capabilities instead of fake or placeholder success.

# 13. Repository/work hygiene

- [ ] Inspect current HEAD and concurrent commits before every implementation slice.
- [ ] Keep changes small and executable; one concrete seam per commit where practical.
- [ ] Reuse existing modules/test utilities before adding new ones.
- [ ] Use temporary locations for diagnostics; do not commit scratch workflows/debug fixtures.
- [ ] Do not modify frozen checkpoints; create a new timestamped checkpoint only for a materially new accepted recovery point.
- [ ] Do not move to a later implementation phase while the current acceptance gate is still unproven.
- [ ] Do not claim `DONE` from docs, mocks, type declarations or reference candidates alone.

## Current next action

**Finish Section 1 first.** Exercise the stored-strategy → canonical hypothesis → exact `CandidateSpec` → existing `phase01_intake` path against a genuine non-reference positive control, repair only the concrete failure it exposes, then repeat through the packaged worker boundary. The worker lock stays unchanged until the replacement artifact and packaged proof exist.

After that acceptance, use Sections 2–12 as the ordered implementation backlog while preserving the product's capability-graph semantics (the implementation backlog is ordered; the user runtime is not a mandatory linear funnel).
