# TraderCockpit

TraderCockpit is a new desktop trading platform with a live/current Cockpit Home and a dedicated **Research** workspace.

The platform owns the product identity and user experience. StrategyQuant X 144.2953 is one current native historical-research producer/backend; it is not the platform name and it is not a user-facing top-level workspace label.

## Current authority

Read these before implementation:

1. `AGENTS.md` — repository execution and anti-drift policy.
2. `docs/product-architecture-v1.md` — producer ownership and lifecycle.
3. `docs/product-backbone-spec-v1.md` — product/UI/API contract.
4. `docs/home-research-surface-authority-v1.md` — binding navigation/naming authority: Home is live/current; Research is the historical-research workspace.
5. `IMPLEMENTATION_CHECKLIST.md` — release gates and implementation order.
6. `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/optional `sqx-lab` backend boundary.
7. `docs/consumer-openrouter-account-authority-v1.md` — Google/OpenRouter consumer account boundary.
8. `docs/repository-consolidation-v1.md` — repository cleanup and development-desktop policy.

`main` is the canonical product branch. GitHub's default branch may need to be changed separately to `main`.

## Desktop product surfaces

```text
Home | Research | Explore | Automation | Operate | Settings
```

### Home

Home is the live/current cockpit and preserves:

```text
Market Overview | System Status | Alpha Stack | Pipeline Overview
Signals | Risk | Performance | Quick Actions
```

Those zones remain visible but truthful when their live producers are unavailable. Historical research does not masquerade as live market, signal, risk, execution, account, or performance state.

### Research

Research is one dedicated historical strategy-research workspace. Inside it:

```text
Construct -> Backtest -> Proof
```

Construct:

```text
Idea | Specification | Build | Candidates
```

Backtest:

```text
Overview | Trades | Robustness | Configuration
```

The canonical path is `/research`. `/strategyquant` is compatibility-only and redirects to Research.

Backend/vendor identity may appear in technical provenance, native configuration, runtime diagnostics, or source-bound controls. It does not determine the platform's navigation or branding.

There is no persistent Apollo product spine.

## Producer boundary

The current native SQX backend owns the historical quantitative behavior already proven to belong to it, including native authoring/AlgoWizard, Builder generation/search, historical backtesting, ranking/filter calculations, cross-check/robustness, Retester, optimization/Walk-Forward, Custom Project execution, and native strategy/result artifacts.

TraderCockpit does not reconstruct those algorithms. The platform owns application mechanics including:

- Home and the other product workspaces;
- Google consumer identity/account state;
- bounded OpenRouter/model-routing policy;
- exact native configuration custody and approval;
- runtime verification/control/readback;
- candidate/result/proof identities and durable custody;
- live Operate/risk/performance surfaces when their producers exist;
- desktop lifecycle/UI state;
- structured refusal when required native or live capability is unavailable.

## Repository boundary

- `product/tradercockpit/**` — application/domain/storage/native adapter code.
- `web/**` — the one web UI rendered by browser acceptance and the desktop host.
- `tests/**` — product, browser, and native-boundary acceptance.
- `tools/check_production_boundary.py` — rejects reference/Futures/legacy producer leakage.
- recovered/reference branches are evidence, not loose production runtime dependencies.

The old platform-owned `builder/evolution.py` producer has been removed on the consolidation line. Historical parity work remains in git history as evidence/test-donor material.

## Development desktop

The desktop application is a thin native window around the same canonical local server and `web/` UI; it does not create a second backend.

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

`TRADERCOCKPIT_STATE_ROOT` and `SQX_HOME` remain backend/runtime configuration. Future user-facing features must become visible or inspectable through this same development desktop as they land.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

GitHub Product Runtime Acceptance additionally starts the canonical server and runs Chromium/browser integration.

## Current consolidation checkpoint

Issue #37 remains the temporary coordination checkpoint. Feature expansion is paused until:

- the production tree is free of superseded producer/cross-repository leakage;
- Home and Research are green in browser acceptance;
- one development desktop host launches the canonical runtime/UI;
- vetted native donor work from PRs #15 and #23 is integrated without restoring old architecture;
- Product Runtime Acceptance and desktop acceptance are green on the consolidated exact head.

After that, consumer-account work and the native-research Foundation Vertical resume from the consolidated platform rather than from isolated backend fragments.
