# TraderCockpit

TraderCockpit is a desktop application around StrategyQuant X 144.2953 for guided strategy construction, native candidate generation/testing, custody, evidence, and product-facing workflow.

StrategyQuant X remains the strategy/research producer. TraderCockpit owns the application experience around that producer: account/auth, configuration, approval, native job control/readback, durable custody, Candidate Lab, Backtest, Proof, and presentation.

## Current authority

Read these before implementation:

1. `AGENTS.md` — repository execution and anti-drift policy.
2. `docs/product-architecture-v1.md` — producer ownership and lifecycle.
3. `docs/product-backbone-spec-v1.md` — detailed product/UI/API contract.
4. `IMPLEMENTATION_CHECKLIST.md` — release gates and implementation order.
5. `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/optional `sqx-lab` boundary.
6. `docs/consumer-openrouter-account-authority-v1.md` — Google/OpenRouter consumer account boundary.
7. `docs/repository-consolidation-v1.md` — current repository cleanup and development-desktop policy.

`main` is the canonical product branch. GitHub's repository default branch may still need to be changed separately to `main`; do not infer product authority from an old evidence branch selected by default.

## Product backbone

Research stages are fixed:

```text
Construct  ->  Backtest  ->  Proof
```

Construct:

```text
Idea | Specification | Build | Candidates
```

Backtest:

```text
Overview | Trades | Robustness | Configuration
```

Home, Explore, Automation, Operate, account/settings, and installed capabilities are auxiliary surfaces. There is no persistent Apollo product spine.

## Producer boundary

StrategyQuant X owns:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy search/generation and GA behavior;
- backtest engine behavior;
- native ranking/filter calculations;
- cross-check/robustness algorithms;
- Retester and optimization/Walk-Forward execution;
- Custom Project task/databank execution;
- native strategy/result artifacts.

TraderCockpit does not reconstruct those producer algorithms.

TraderCockpit owns application mechanics including:

- Google consumer identity and account state;
- bounded OpenRouter/model-routing policy;
- exact native configuration custody and approval;
- native runtime verification/control/readback;
- candidate/result/proof identities and durable custody;
- desktop lifecycle and UI state;
- structured refusal when required native capability is unavailable.

## Repository boundary

- `product/tradercockpit/**` — application/domain/storage/native-SQX adapter code.
- `web/**` — the one web UI rendered by browser acceptance and the desktop host.
- `tests/**` — product, browser, and native-boundary acceptance.
- `tools/check_production_boundary.py` — rejects reference/Futures/legacy producer leakage.
- recovered SQX/reference branches are evidence, not loose production runtime dependencies.

The old TraderCockpit-owned `builder/evolution.py` producer has been removed on the consolidation line. Historical parity work remains available in git history only as evidence/test-donor material.

## Development desktop

The desktop application is a thin native window around the same canonical local TraderCockpit server and `web/` UI. It does not create a second backend.

The consolidation line uses Python + optional `pywebview`/WebView2:

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

On Windows, pywebview uses the native WebView2 surface. `TRADERCOCKPIT_STATE_ROOT` and `SQX_HOME` remain backend/runtime configuration.

Every future user-facing feature must become visible or inspectable through this same development desktop as it lands.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

GitHub Product Runtime Acceptance additionally starts the canonical server and runs Chromium/browser integration.

## Current consolidation checkpoint

Issue #37 is the temporary highest-priority coordination checkpoint. Feature expansion is paused until:

- the production tree is free of superseded producer/cross-repository leakage;
- the architecture-aligned shell is green;
- one development desktop host launches the canonical runtime/UI;
- vetted native SQX donor work from PRs #15 and #23 is integrated without restoring old architecture;
- Product Runtime Acceptance and desktop acceptance are green on the consolidated exact head.

After that, consumer-account work and the native SQX Foundation Vertical resume from the consolidated desktop product rather than from isolated backend fragments.
