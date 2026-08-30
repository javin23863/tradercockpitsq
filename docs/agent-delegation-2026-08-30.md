# Multi-Agent Delegation — 2026-08-30

All active implementation lanes write only to `javin23863/tradercockpitsq`.

`javin23863/futures` is read-only donor evidence. `sources/**` and `references/**` are reference-only from the perspective of production runtime code.

## Shared operating rules

Before editing, every agent must inspect current status, branch, HEAD, and recent commits. Never reset merely to match a historical handoff SHA. Preserve legitimate concurrent work.

Core rules:

- Recover existing behavior before adding a new subsystem.
- Production code must not import from `sources/**`, `references/**`, or Futures.
- Do not create duplicate engines, candidate systems, capability registries, validation pipelines, or provenance systems.
- Unsupported semantics fail closed; never silently approximate or ignore them.
- Do not weaken tests, validation gates, identity checks, or provenance.
- Screenshots define consumers, not backend implementation.
- A recovered SQX class name does not make a feature product-available.
- Evolution/search score is discovery output, not validation/champion status.
- Keep commits small and lane-owned; do not merge another lane.
- If blocked, stop at the concrete executable failure instead of inventing architecture.

Every handoff must report repository, branch, base, head, files changed, commands/tests, pass/fail counts, executable evidence, first blocker, reference sources consulted, any deliberately ported behavior, clean status, and next exact seam.

## Lane A — Cloud A / production domain foundation

Branch: `codex/product-domain-foundation`

Owner responsibilities:

1. production/reference import-boundary enforcement;
2. canonical serialization and content addressing;
3. TraderCockpit-owned immutable production specs;
4. first deterministic backtest/evaluator interface;
5. result/evidence/initial-validation contracts;
6. integration/adversarial review of incoming lane commits.

Initial spec sequence:

`StrategySpecV1` -> `CandidateSpecV1` -> `DataSpecV1` -> `ExecutionSpecV1` -> `BacktestRunSpecV1`.

Do not implement production evolutionary search before a trusted candidate evaluator exists.

## Lane B — Desktop A / legacy donor recovery + parity fixtures

Writes only to TraderCockpit. Futures is read-only.

Objective:

- recover one genuine known-pass strategy;
- recover one genuine known-fail strategy;
- recover deterministic/numerical expected behavior and required semantics;
- create compact provenance-backed parity fixtures/tests in TraderCockpit.

This lane does not create the production backtester, StrategySpec architecture, worker model, or validation framework. It answers: `what exact behavior is worth preserving?`, not `how do we preserve the old architecture?`.

## Lane C — Desktop B / UI reference asset custody

Objective:

- ingest the 35 supplied StrategyQuant/Builder snapshots under `references/ui-authority/panel-snapshots/`;
- commit the normalized manifest;
- preserve exact original bytes;
- provide an executable fail-closed verification of dimensions, byte counts, SHA-256, unique IDs/paths, and category totals.

After the verified asset commit, stop. Frontend implementation waits for the production contracts.

## Lane D — Cloud B / SQX capability recovery + parity evidence

Uses `sources/**` as reference material only.

Objective:

- recover actual strategy-construction, backtest, genetic/search, optimizer, WFO/WFM, Monte Carlo, robustness, ranking, sizing, and related behavior;
- classify each finding as executable path identified, configuration/schema only, result/presentation only, reference algorithm identified, absent, or unverified;
- create compact product-neutral parity fixtures/evidence.

This lane does not make recovered Java production-importable, does not compile-clean the vendor tree, and does not implement production evolutionary search yet.

## Integration order

1. UI snapshot custody can land independently.
2. Legacy known-pass/known-fail parity controls can land independently.
3. SQX capability/parity evidence can land independently.
4. Production foundation establishes immutable specs and evaluator boundary.
5. First real known-pass + known-fail backtest/initial-validation proof.
6. First real UI vertical slice.
7. Fast validation lane.
8. Evolutionary search against the stable evaluator.
9. Golden and advanced validation.
10. Optional prop/monitor/delivery surfaces as real producers exist.

No lane should wait on another merely to produce its bounded evidence, but only the product-domain lane defines production runtime contracts during the foundation stage.
