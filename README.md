# TraderCockpit

TraderCockpit is the clean product line for a trading research, validation, and execution application. The current repository contains an accepted product kernel and browser foundation; it does **not** yet contain an accepted genuine trading evaluator/provider.

## Before coding

Read these in order:

1. `AGENTS.md` — execution, delegation, worktree, and review policy.
2. `IMPLEMENTATION_CHECKLIST.md` — binding implementation/acceptance index and current blocker.
3. `docs/product-architecture-v1.md` — clean product authority and reference boundaries.

`main` is the canonical product branch. If GitHub's repository default branch still points elsewhere, explicitly select `main`; do not treat a default checkout of an SQX/reference branch as product authority.

## Production boundary

- `product/**` contains TraderCockpit-owned backend code.
- `web/**` contains the product frontend.
- `tests/product/**`, `tests/*.mjs`, and production-boundary checks protect those surfaces.
- Recovered SQX/source/reference trees are not production runtime dependencies.
- `javin23863/futures` is quarantined and is not a recovery source, implementation dependency, acceptance gate, or execution path unless the user explicitly reverses that rule.
- SQX extraction, capability-parity, runtime-smoke, and lab/plugin branches are reference or experimental lanes only unless a deliberately reviewed capability is bound through a TraderCockpit-owned contract.

## Current product kernel

The implemented backend kernel provides:

- immutable, content-addressed strategy/run/data/execution identities;
- exact run-input resolution and custody checks;
- evaluator semantic preflight and strict result-contract enforcement;
- durable run lifecycle state;
- immutable run receipts, results, validation decisions, and evidence manifests;
- filesystem-backed object and lifecycle persistence;
- a verified read model for initial-run state;
- a narrow read-only HTTP seam for verified run state.

The frontend provides the accepted five-workspace/21-state shell, canonical Cockpit Home and Signals & Models compositions, opaque strategy-reference preservation, persistent Apollo, shared run surface, truthful unavailable/pending producer states, and real Chromium browser acceptance against the product server.

The current package is `tradercockpit-core` and requires Python 3.12 or newer.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

The GitHub product acceptance workflow additionally starts the real product server and runs the browser regression in Chromium.

## Current execution boundary

A genuine trading evaluator/provider is not yet bound as accepted production execution. The next backend task is therefore to select one real producer, implement/bind it through the existing `BacktestEvaluatorV1` contract, preserve exact strategy/candidate/data/execution/build custody, and return producer-owned numerical `ResultArtifactV1` output with deterministic or explicitly bounded regression evidence.

Do not recreate an older repository/pipeline, introduce a second run system, or use recovered class names/runtime experiments as capability proof. Unsupported capability remains unavailable and must fail closed rather than being simulated.
