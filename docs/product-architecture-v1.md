# TraderCockpit Clean Product Architecture v1

## Decision

`main` is the canonical TraderCockpit product line. Production implementation is TraderCockpit-owned and lives under `product/**` and `web/**`.

Recovered SQX material, runtime experiments, UI captures, parity evidence, and historical implementation branches are reference authority only. They may inform deliberate product contracts or compact evidence fixtures, but they are not runtime dependencies and must not be wholesale-merged into production.

`javin23863/futures` is quarantined unless the user explicitly reverses that decision.

## Authority layers

```text
REFERENCE / EVIDENCE AUTHORITY
UI authority + SQX parity/runtime evidence + approved research
                 |
                 | deliberate review / parity only
                 v
TRADERCOCKPIT DOMAIN AUTHORITY
canonical identities + immutable specs + producer contracts
                 |
                 v
PRODUCTION IMPLEMENTATION
provider + orchestration + persistence + API + browser consumers
```

A recovered class, screenshot control, branch name, or runtime trace can prove that a behavior/consumer exists. It does not by itself define a production capability or public product object.

## Current implemented kernel

### Domain and identity

The product has canonical serialization and content-addressed immutable identities for strategy, candidate, data, execution assumptions, engine build, and backtest run requests.

### Engine boundary

`BacktestEvaluatorV1` is the narrow producer contract. It declares exact engine build identity, supported semantic schemas, result schema, and determinism; preflight validates exact strategy semantics before computation. `BacktestInputsV1` resolves and cross-checks exact candidate → strategy plus data/execution/build custody.

This is an orchestration/custody contract, **not** a trading evaluator implementation.

### Lifecycle, evidence, and storage

The product persists immutable run receipts/results/validation/evidence plus explicit lifecycle state and validates them through a read model. Filesystem stores fail closed on corrupt, mismatched, stale, or missing state.

### Application seam

The current product server serves the accepted browser shell and one narrow read-only `/api/run-read` endpoint. It does not provide an accepted launch/write API.

### Frontend

The accepted UI foundation contains five workspaces and 21 logical states, preserves opaque strategy references without treating them as backend identity, keeps Apollo persistent, shares the RunSurface between Test & Validate and Operate, and renders unavailable/pending producer state without fabricated market/validation/execution values. Cockpit Home and Signals & Models canonical compositions are present on `main`.

## What is not yet product capability

The existence of types, panels, or placeholders does not imply:

- a genuine trading evaluator/provider;
- producer-owned numerical backtest output;
- mutable strategy-draft/gap-resolution services;
- backend capability manifest;
- Fast/Golden validation producers or champion promotions;
- evolutionary search;
- prop simulation;
- live execution, market feeds, portfolio risk, or monitoring.

Those remain unavailable until a real producer and acceptance evidence exist.

## Product rules that must not regress

- The runtime product is a capability graph, not a mandatory funnel.
- Initial Test is optional.
- Fast and Golden are independent peer lanes.
- B/A+ status belongs to promotion/evidence records, not candidate/search score.
- Evolution/search ranking is discovery evidence, not validation.
- Validation composition comes from backend-owned plans.
- Prop Simulation is optional and real-rule-set bound.
- Apollo guides over deterministic authority and cannot silently mutate semantics or certify results.
- Unsupported semantics fail closed; no generic/default candidate substitution or silent approximation.
- Frontend numeric/professional metrics are producer-owned.

## Next dependency

The next architectural dependency is a genuine execution producer implementing the existing `BacktestEvaluatorV1` boundary with exact build/semantic/result custody and real numerical positive/negative regression evidence.

Do not add production evolutionary search, broad validation infrastructure, or a second run system to work around the missing producer. The first accepted provider should prove the existing kernel rather than replace it.

## Reference branch anchors

Use these as evidence sources only and inspect current heads before relying on them:

- UI product authority: `codex/ui-prototype-authority@53645acfff750672805efd6b20623a0abf36dff1`.
- UI behavior acceptance: `codex/ui-reference-acceptance@26221dccee1541c1fc672f24b75a380cf4371c32`.
- SQX capability/parity: `codex/sqx-capability-parity@6f6fb81b450844024e8585503845c4a3316472de`.
- SQX runtime-smoke: `codex/sqx-runtime-smoke@766cdc6e8c2f42e6dee86fd59b38e2862ef235a6`.
- SQX extraction/workflow history: `codex/sqx-engine-extract@c1ae24d2e62acfb8ae8be1aea318a82225490c4b`.

No reference branch is a production base. Deliberate ports must retain enough provenance to identify the evidence used while exposing only TraderCockpit-owned contracts.
