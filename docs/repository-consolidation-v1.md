# Repository Consolidation v1

## Status

This document records the cleanup governed by Issue #37. It maps the current source tree onto the product architecture and defines the development-desktop delivery trunk.

`docs/home-research-surface-authority-v1.md` is binding: **Home** is the live/current cockpit and **Research** is the historical strategy-research workspace. StrategyQuant X / SQX remains a backend producer identity where technically relevant; it is not a top-level product label.

Older wording that makes `Construct | Backtest | Proof` global navigation or names the workspace after the backend vendor is superseded.

## Canonical branch and product rule

`main` is the product trunk.

Future implementation branches start from reviewed `main`, change one non-overlapping slice, and leave the runnable development desktop truthful. Historical branches/PRs are evidence or donors only until explicitly re-integrated.

The GitHub repository default branch must also be changed to `main` so browsing/tool defaults do not land on evidence branches.

## Production module inventory

### KEEP — application/domain/custody envelopes

Retain application state, identity, custody, lifecycle, server, and producer-neutral contracts. These do not own quantitative semantics.

The historical `engine` package is application execution/custody envelope only. No generic evaluator becomes a competing historical trading/backtest producer.

### SALVAGE_NATIVE_ADAPTER — native producer boundaries

Retain source-bound adapters such as:

- `product/tradercockpit/sqx_builder_config.py`
- `product/tradercockpit/sqx_outputs.py`
- `product/tradercockpit/sqx_presets.py`
- `product/tradercockpit/sqx_retester.py`

They fail closed on unverified runtime/configuration/artifacts.

PR #15 supplies vetted Custom Project topology-custody donor material.

PR #23 supplies vetted native candidate/Retester/run/readback donor material, including trusted launcher boundary work. Integrate native pieces only after the consolidation base is green.

### QUARANTINE_REMOVE — duplicate producer authority

Removed from production:

- `product/tradercockpit/builder/evolution.py`
- `product/tradercockpit/builder/__init__.py`
- `tests/product/test_builder_evolution.py`

Git history preserves them as evidence; production does not.

### REPLACE — historical frontend spine

The old five-workspace/21-state shell and persistent Apollo authority are not current architecture. Replace old workspace/Apollo composition and tests rather than carrying them forward.

The accepted Home prototype is retained with exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`.

Lower-level read/native helper modules may remain as donors when they consume canonical backend truth. Their existence does not automatically mount them into the product shell.

## Anti-leakage boundary

`tools/check_production_boundary.py` rejects major production leakage including:

- runtime imports rooted at `sources`, `references`, legacy `futures`;
- production `tradercockpit/builder` package;
- `phase01_intake` architecture;
- rejected duplicate-builder schema;
- copied Futures repository markers;
- Apollo product-spine markers.

The purpose is to keep evidence/history from silently becoming production authority.

## Canonical desktop surfaces after consolidation

Top-level product:

- **Home** — current/live cockpit orientation;
- **Research** — historical strategy research;
- **Explore** — capability/catalog discovery;
- **Automation** — registered native workflows/projects where supported;
- **Operate** — live/deployed runs, performance, execution, risk where supported;
- **Settings** — account, allowance, model/runtime/provider/add-on configuration.

### Home

Preserves exactly eight accepted cockpit zones and never substitutes historical research for live market, signal, risk, execution, account, or performance truth. Unconnected producers remain visibly unavailable.

### Research

Canonical route: `/research`.

Inside Research:

- `Construct | Backtest | Proof`;
- Construct: `Idea | Specification | Build | Candidates`;
- Backtest: `Overview | Trades | Robustness | Configuration`.

Those are internal states, not top-level workspaces.

`/strategyquant` is compatibility-only and redirects into `/research`. Older `/construct/*`, `/backtest/*`, and `/proof` routes may redirect as well.

There is no persistent Apollo surface and no permanent Optimizer/Monte Carlo/LLM/MCP/add-on research tab.

## Native backend naming rule

StrategyQuant X 144.2953 may appear in:

- backend provenance;
- runtime/configuration diagnostics;
- exact source/build identity;
- adapter/API names tied to native producer contracts;
- logs and evidence.

It must not appear as the platform name or top-level Research workspace label.

## Development desktop authority

The development desktop is a thin host around the same canonical application server and `web/` surface used by browser acceptance.

Selected host: Python + optional `pywebview`.

The desktop host does not implement product state, native producer logic, account authority, or provider routing. It starts the canonical server, opens one native window, and shuts the server down when the desktop exits.

Its private control server is loopback-only, rejects invalid Host values, and refuses cross-origin browser mutations.

Development command:

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

Runtime configuration remains backend-owned, including `TRADERCOCKPIT_STATE_ROOT` and `SQX_HOME`.

## Delivery rule

A user-facing feature is not complete because unit tests pass.

Required path:

`reviewed branch -> canonical main -> development desktop -> same feature visible/inspectable -> product/browser/desktop acceptance`

Backend-only infrastructure is allowed only when truthfully observable through an appropriate system/account/runtime surface or required by an already defined desktop path.

## Remaining consolidation sequence

1. Keep Home + Research shell/browser acceptance green.
2. Keep development desktop lifecycle/security acceptance green.
3. Integrate PR #15 read-only native workflow topology custody in the smallest boundary.
4. Integrate vetted native portions of PR #23 without obsolete product assumptions.
5. Re-audit consolidated tree for producer/cross-repository leakage.
6. Land consolidation to `main` and change GitHub default branch to `main`.
7. Rebuild consumer account/OpenRouter work from clean trunk.
8. Resume the native-research Foundation Vertical through **Research** while Home evolves separately from live/current producers.
