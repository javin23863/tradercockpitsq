# TraderCockpit

TraderCockpit is the production trading research and validation application.

This branch contains only product code and the tests/build infrastructure that protect the shipped product. Recovered StrategyQuant sources, design-reference material, donor archaeology, and planning records are intentionally kept outside this product line.

## Current product kernel

The implemented backend kernel provides:

- immutable, content-addressed strategy/run/data/execution identities
- exact run-input resolution and custody checks
- evaluator semantic preflight
- durable run lifecycle state
- immutable run receipts, results, validation decisions, and evidence manifests
- filesystem-backed object and lifecycle persistence
- a verified read model for the first validation UI slice

The current package is `tradercockpit-core` and requires Python 3.12 or newer.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
```

## Current execution boundary

The orchestration and persistence kernel is implemented. A production trading evaluator must still be bound only after the recovered known-PASS/known-FAIL controls can be reproduced against their exact data and economics. Synthetic pass fixtures and replacement evaluator architectures are not accepted as production substitutes.
