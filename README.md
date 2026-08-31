# TraderCockpit

TraderCockpit is a desktop trading application with a live/current Cockpit Home and a dedicated StrategyQuant X 144.2953 historical-research screen.

StrategyQuant X remains the strategy/research producer. It is one backend/product surface inside TraderCockpit, not the application Home screen and not the source of unrelated live market, account, signal, execution, risk, or performance truth.

TraderCockpit owns the application experience around all product surfaces: account/auth, live/current presentation, exact native SQX configuration and approval, native job control/readback, durable custody, Candidate Lab, historical Backtest/Proof presentation, automation/operate surfaces, and desktop delivery.

## Current authority

Read these before implementation:

1. `AGENTS.md` — repository execution and anti-drift policy.
2. `docs/product-architecture-v1.md` — producer ownership and lifecycle.
3. `docs/product-backbone-spec-v1.md` — detailed product/UI/API contract.
4. `docs/home-strategyquant-surface-authority-v1.md` — binding navigation clarification: Home is the live/current cockpit; StrategyQuant X is one separate historical-research surface.
5. `IMPLEMENTATION_CHECKLIST.md` — release gates and implementation order.
6. `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/optional `sqx-lab` boundary.
7. `docs/consumer-openrouter-account-authority-v1.md` — Google/OpenRouter consumer account boundary.
8. `docs/repository-consolidation-v1.md` — repository cleanup and development-desktop policy.

`main` is the canonical product branch. GitHub's repository default branch may still need to be changed separately to `main`; do not infer product authority from an old evidence branch selected by default.

## Desktop product surfaces

Top-level application surfaces are:

```text
Home | StrategyQuant X | Explore | Automation | Operate | Settings
```

### Home

Home is the live/current cockpit and preserves the accepted eight-zone prototype:

```text
Market Overview | System Status | Alpha Stack | Pipeline Overview
Signals | Risk | Performance | Quick Actions
```

Those zones remain visible but truthful when their live producers are unavailable. Historical SQX results do not masquerade as live market, signal, risk, or account state.

### StrategyQuant X

StrategyQuant X is one dedicated historical-research screen. Inside that screen the research workflow is:

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

The canonical top-level path is `/strategyquant`; research stage/tab state stays inside that screen rather than becoming separate global application workspaces.

There is no persistent Apollo product spine.

## Producer boundary

StrategyQuant X owns:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy search/generation and GA behavior;
- historical backtest engine behavior;
- native ranking/filter calculations;
- cross-check/robustness algorithms;
- Retester and optimization/Walk-Forward execution;
- Custom Project task/databank execution;
- native strategy/result artifacts.

TraderCockpit does not reconstruct those producer algorithms.

TraderCockpit owns application mechanics including:

- the live/current Home product surface;
- Google consumer identity and account state;
- bounded OpenRouter/model-routing policy;
- exact native configuration custody and approval;
- native runtime verification/control/readback;
- candidate/result/proof identities and durable custody;
- live Operate/risk/performance surfaces when their producers exist;
- desktop lifecycle and UI state;
- structured refusal when required native or live capability is unavailable.

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
- Home and the dedicated StrategyQuant X screen are green in browser acceptance;
- one development desktop host launches the canonical runtime/UI;
- vetted native SQX donor work from PRs #15 and #23 is integrated without restoring old architecture;
- Product Runtime Acceptance and desktop acceptance are green on the consolidated exact head.

After that, consumer-account work and the native SQX Foundation Vertical resume from the consolidated desktop product rather than from isolated backend fragments.
