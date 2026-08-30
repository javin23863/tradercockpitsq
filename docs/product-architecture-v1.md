# TraderCockpit Clean Product Architecture v1

## Decision

`javin23863/tradercockpitsq` is the writable product/engine repository.

Two large bodies of material remain reference-only:

- `sources/**` — recovered/decompiled StrategyQuant material.
- `references/**` — UI authority, vendor material, parity fixtures, legacy evidence, research, and other non-runtime references.

`javin23863/futures` is read-only legacy donor evidence. TraderCockpit production code must not depend on it.

Production code must not import from either `sources/**` or `references/**`.

## Authority layers

```text
REFERENCE AUTHORITY
SQX source + Futures evidence + research + UI snapshots
              |
              | recovery / parity only
              v
TRADERCOCKPIT DOMAIN AUTHORITY
immutable TraderCockpit-owned specs and capability contracts
              |
              v
PRODUCTION IMPLEMENTATION
engine + validation + search + API + browser
```

Recovered code and screenshots can prove that a behavior or consumer exists. They do not define the public production object model.

## Core object model

### StrategyDraft
Mutable workspace state. It may contain unresolved semantic gaps, source material, recommendations, annotations, and user choices. A draft is not executable.

### StrategySpecV1
Immutable, fully resolved trading meaning. It contains only semantics that affect execution, such as entries, exits, stops, targets, sizing, concurrency, order behavior, and session behavior. Once accepted it is canonically serialized and content-addressed. A semantic change creates a new identity; display-name or note changes do not.

### SearchSpaceSpecV1
Defines what a search operation is allowed to change. Ranges, selectable building blocks, parameter bounds, and structural alternatives belong here rather than inside an executable strategy.

### SearchRunSpecV1
Defines how a search space is explored: algorithm, budget, population, seed when supported, mutation/crossover/selection policies, objectives, and other producer-supported settings. Unsupported search concepts remain unavailable instead of being inferred from screenshots.

### CandidateSpecV1
A fully materialized executable strategy candidate with no unresolved ranges. It preserves parent strategy and optional search lineage. Search score and validation status do not mutate the candidate.

### DataSpecV1
Exact market-data identity and assumptions: instrument, timeframe, source/dataset revision, timezone/session calendar, range, and adjustment policy as applicable.

### ExecutionSpecV1
Exact simulation assumptions: capital, fees, slippage, fill/order timing, sizing execution, latency when applicable, and other execution-model choices.

### BacktestRunSpecV1
The reproducible execution request. At minimum it binds candidate, data, execution assumptions, and engine build identity.

### ValidationPlanSpecV1
Backend-owned validation plan. Fast and Golden are independent peer lanes. The browser must render returned plan records rather than hard-code phase names or counts.

### RunReceiptV1 / ResultArtifactV1 / EvidenceManifestV1
Run receipt freezes launch identities. Result artifacts are typed producer outputs. Evidence manifests bind the evaluated artifacts and provenance so evidence cannot be silently substituted or compacted away.

### ChampionRecordV1
Promotion record created only when a validation policy genuinely passes. B and A+ status live on promotion/evidence records, not on CandidateSpec itself.

### CapabilityManifestV1
Backend authority for what can actually execute. A recovered class or screenshot control is not sufficient to mark a capability available.

## Product rules that must not regress

- TraderCockpit is a capability graph, not one mandatory funnel.
- Initial Test is optional.
- Fast and Golden are independent peer lanes.
- Fast can promote qualifying outputs to B Champions.
- Golden can promote qualifying outputs to A+ Champions.
- Evolution/search score is discovery evidence, not validation or champion status.
- Scenario, stress, OOS, walk-forward, Monte Carlo, cost, and other checks are rendered from backend-owned plans/results.
- Prop Simulation is optional and producer/rule-set bound; no native SQX prop-firm module is assumed.
- Apollo is a guide over deterministic product authority, not the source of strategy semantics, capability availability, validation decisions, or evidence.
- Unsupported semantics fail closed. No silent approximation or generic-candidate substitution.

## Production layout

The production namespace begins under `product/`. Reference and parity material remains outside it.

```text
product/
  tradercockpit/
    domain/
    engine/
    validation/
    search/
    api/

tests/
  product/
  parity/
  acceptance/

sources/       # reference-only
references/    # reference-only
```

This is a logical boundary first; the recovered trees do not need to be moved.

## Mandatory boundary enforcement

An executable repository check must reject imports from production code into:

- `sources/**`
- `references/**`

Parity/reference tests may read fixtures and documented reference material, but vendor/decompiled classes are never a production runtime dependency.

## Implementation order

1. Production import-boundary enforcement.
2. Canonical serialization and content-addressed identity primitive.
3. StrategySpecV1, CandidateSpecV1, DataSpecV1, ExecutionSpecV1, BacktestRunSpecV1.
4. Deterministic engine interface and one ordinary backtest path.
5. Prove one known-pass and one known-fail strategy against donor parity fixtures.
6. ResultArtifactV1, initial validation policy, EvidenceManifestV1.
7. First real UI vertical slice: strategy -> Initial Test -> running -> results -> evidence.
8. ValidationPlanSpecV1 and Fast lane.
9. Stable evaluator/objective interface.
10. SearchSpaceSpecV1 / SearchRunSpecV1 and evolutionary search.
11. Golden and advanced validation capabilities.
12. Optional prop simulation, monitoring, delivery/execution surfaces as real producers become available.

Evolutionary search production implementation intentionally follows a trusted evaluator. Otherwise search code would be forced to invent candidate/evaluation contracts that later need consolidation.

## Adoption from legacy/reference systems

For any behavior taken from Futures or SQX, ask:

1. What exact behavior is worth preserving?
2. What executable or numerical evidence proves it?
3. Does TraderCockpit already have an equivalent?
4. What is the smallest clean implementation or parity fixture needed?

Do not ask how to preserve the donor architecture.

Any deliberate port should retain enough provenance to identify the donor source/commit or SQX reference path while exposing only TraderCockpit-owned production contracts.
