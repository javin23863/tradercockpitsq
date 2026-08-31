# Repository Consolidation v1

## Status

This document records the repository cleanup governed by Issue #37. It does not replace the product architecture in `docs/product-architecture-v1.md`; it maps the current source tree onto that architecture and defines the development-desktop delivery trunk.

`docs/home-strategyquant-surface-authority-v1.md` is a binding consolidation clarification: the live/current Cockpit Home and the dedicated StrategyQuant X historical-research screen are separate top-level product surfaces. Older wording that presents `Construct | Backtest | Proof` as the global application navigation is superseded by that clarification.

## Canonical branch and product rule

`main` is the product trunk.

Every future implementation branch starts from the current reviewed `main`, changes one non-overlapping product slice, and must leave the runnable development desktop truthful. Historical branches/PRs are evidence or donor material only until explicitly re-integrated.

The GitHub repository default branch is still a separate repository setting and must also be changed to `main` so normal browsing and tool defaults do not land on an evidence branch.

## Production module inventory

### KEEP — application/domain/custody envelopes

These modules define TraderCockpit application state, identity, custody, lifecycle, server, and producer-neutral contracts. They do not by themselves own SQX quantitative semantics:

- `product/tradercockpit/app_server.py`
- `product/tradercockpit/domain/**`
- `product/tradercockpit/storage/**`
- `product/tradercockpit/engine/contracts.py`
- `product/tradercockpit/engine/evaluator.py`
- `product/tradercockpit/engine/lifecycle.py`
- `product/tradercockpit/engine/read_model.py`
- `product/tradercockpit/engine/run_service.py`

The `engine` package name is historical. Its retained role is application execution/custody envelope only. No generic evaluator may become a competing trading/backtest producer. Native SQX adapters must provide the actual historical/research producer behavior where SQX owns it.

### SALVAGE_NATIVE_ADAPTER — native SQX boundaries

These are retained as native-SQX integration material and must continue to fail closed on unverified runtime/configuration/artifacts:

- `product/tradercockpit/sqx_builder_config.py`
- `product/tradercockpit/sqx_outputs.py`
- `product/tradercockpit/sqx_presets.py`
- `product/tradercockpit/sqx_retester.py`

PR #15 supplies vetted Custom Project topology-custody donor material.

PR #23 supplies vetted native candidate/Retester/run/readback donor material, including its corrected trusted `sqcli.exe` launcher boundary. Because #23 also changes shared engine/server/UI contracts, its native pieces are integrated only after this consolidation tree is green.

### QUARANTINE_REMOVE — duplicate producer authority

Removed from the production package in this consolidation:

- `product/tradercockpit/builder/evolution.py`
- `product/tradercockpit/builder/__init__.py`
- `tests/product/test_builder_evolution.py`

Those files implemented/tested TraderCockpit-owned reproductions of SQX GA/evolution behavior and conflict with the landed native-SQX producer authority. Git history preserves them as evidence; production does not.

### REPLACE — historical frontend product spine

The old five-workspace/21-state frontend and persistent Apollo authority are not the current product architecture. The following historical shell authority is replaced rather than carried forward:

- old `web/model.mjs` workspace model;
- old `web/app.mjs` workspace/Apollo composition;
- old `web/candidates-authority.mjs` product handoff surface;
- old UI/browser tests that assert the superseded navigation/Apollo contract.

The accepted Cockpit Home prototype is not discarded with that old workspace shell. Its eight distinct Home zones are retained as product authority:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`.

Reusable lower-level native/read modules may remain as donors when they consume canonical backend truth:

- `web/run-read.mjs`
- `web/sqx-presets.mjs`
- `web/sqx-outputs.mjs`

They are not automatically mounted into the new shell merely because the files exist.

## Anti-leakage boundary

`tools/check_production_boundary.py` is the executable production import/architecture gate.

It rejects:

- runtime imports rooted at `sources`, `references`, or legacy `futures`;
- a production `tradercockpit/builder` package;
- `phase01_intake` architecture;
- the rejected `tradercockpit.builder-strategy.v1` schema;
- copied `javin23863/futures` repository markers;
- Apollo product-spine markers in production Python.

The point is not to erase design history. It is to prevent history/evidence from silently becoming production authority again.

## Canonical desktop surfaces after consolidation

The top-level desktop product is:

- **Home** — current/live cockpit orientation;
- **StrategyQuant X** — one dedicated historical strategy-research screen;
- **Explore** — capability/catalog discovery;
- **Automation** — native SQX Custom Projects/workflows where supported;
- **Operate** — live/deployed runs, performance, execution and risk where supported;
- **Settings** — account, allowance, model/runtime/provider/add-on configuration.

### Home

Home preserves exactly eight accepted cockpit zones:

1. Market Overview;
2. System Status;
3. Alpha Stack;
4. Pipeline Overview;
5. Signals;
6. Risk;
7. Performance;
8. Quick Actions.

Home is primarily live/current product orientation. It must not substitute historical SQX results for live market, signal, risk, execution, account or performance truth. Unconnected producers remain visibly unavailable.

### StrategyQuant X

StrategyQuant X is one top-level screen at `/strategyquant`. Inside that screen the historical research workflow remains:

- `Construct | Backtest | Proof`;
- Construct: `Idea | Specification | Build | Candidates`;
- Backtest: `Overview | Trades | Robustness | Configuration`.

Those are internal research states, not top-level TraderCockpit workspaces. Compatibility redirects from older `/construct/*`, `/backtest/*`, or `/proof` paths are allowed, but they do not define product navigation.

There is no persistent Apollo surface and no permanent Optimizer/Monte Carlo/LLM/MCP/add-on research tab.

## Development desktop authority

The development desktop is a thin host around the same canonical application server and `web/` surface used by browser acceptance.

Selected host: Python + `pywebview` as an optional desktop dependency.

Why:

- the backend/runtime is already Python;
- the UI is already HTML/JS;
- pywebview uses the native WebView/WebView2 surface rather than adding an Electron/Tauri application backend;
- the desktop process can own the canonical local HTTP server lifecycle;
- future packaging can wrap this same entrypoint without changing product APIs.

The desktop host must not implement product state, SQX control, account authority, or provider routing itself. It starts the canonical server, opens its URL in one native window, and shuts the server down when the desktop exits.

Development command after installing the desktop extra:

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

Environment/configuration remains backend-owned, including `TRADERCOCKPIT_STATE_ROOT` and `SQX_HOME`.

## Delivery rule after this consolidation

A user-facing feature is not complete when only its unit tests pass.

Required path:

`reviewed branch -> canonical main -> development desktop -> same feature visible/inspectable there -> product/browser/desktop acceptance`

Backend-only infrastructure is allowed only when its state is truthfully observable through an appropriate Home/system/account/runtime surface or is a prerequisite for an already defined desktop path.

## Remaining consolidation sequence

1. Make the corrected Home + dedicated StrategyQuant X shell/browser acceptance green.
2. Make the development desktop runtime/launcher acceptance green.
3. Integrate PR #15's read-only Custom Project topology custody in the smallest native boundary.
4. Integrate the vetted native portions of PR #23 without importing obsolete shared product assumptions.
5. Re-audit the consolidated tree for producer and cross-repository leakage.
6. Land consolidation to `main` and change the GitHub default branch to `main`.
7. Rebuild consumer account/OpenRouter work from the clean trunk.
8. Resume the native SQX Foundation Vertical through the dedicated StrategyQuant X screen while Home evolves separately from live/current producers.
