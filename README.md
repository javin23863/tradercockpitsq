# TraderCockpit

TraderCockpit is the production trading research and validation application.

This product line contains only TraderCockpit-owned production code and the tests/build infrastructure that protect it. Recovered StrategyQuant source trees, parity evidence, runtime experiments, UI authority captures, and historical recovery work remain outside the production tree and must not become runtime dependencies.

## Production boundary

The production spine is TraderCockpit-owned.

- `product/**` contains production backend code.
- `web/**` contains the product frontend.
- `tests/product/**`, `tests/*.mjs`, and the production-boundary checks protect those surfaces.
- `sources/**` and `references/**` are not production dependencies.
- `javin23863/futures` is quarantined and is not a recovery source, implementation dependency, acceptance gate, or execution path for this product.
- SQX extraction, capability-parity, runtime-smoke, and lab/plugin branches are reference or experimental lanes only unless a deliberately reviewed capability is bound through a TraderCockpit-owned contract.

## Current product kernel

The implemented backend kernel provides:

- immutable, content-addressed strategy/run/data/execution identities
- exact run-input resolution and custody checks
- evaluator semantic preflight
- durable run lifecycle state
- immutable run receipts, results, validation decisions, and evidence manifests
- filesystem-backed object and lifecycle persistence
- a verified read model for the first validation UI slice
- a narrow HTTP read seam for verified run state

The current package is `tradercockpit-core` and requires Python 3.12 or newer.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

## Current execution boundary

The orchestration, identity, persistence, lifecycle, evidence, and read-model kernel exists. A genuine trading evaluator/provider is not yet bound as accepted production execution.

The next backend decision is therefore not to recreate an older repository or pipeline. It is to determine which execution provider should implement the existing TraderCockpit evaluator contract, prove exact strategy/data/execution/build custody through that provider, and return producer-owned numerical `ResultArtifactV1` output with deterministic or explicitly bounded regression evidence.

SQX may supply an execution provider, algorithm/reference evidence, or both, but recovered source names and runtime experiments do not by themselves make a capability production-available. Unsupported capability remains unavailable and must fail closed rather than being simulated.
