# TraderCockpit

TraderCockpit is a new desktop trading platform with one canonical application/runtime and one development desktop.

## Product surfaces

`Home | Research | Explore | Automation | Operate | Settings`

### Home

Home is the live/current cockpit and preserves:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

Historical research never substitutes for live market, signal, risk, execution, account, or performance truth.

### Research

Research is the historical strategy-research workspace.

Inside Research:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

Canonical route: `/research`.

StrategyQuant X / SQX is a native backend producer identity where technical provenance/runtime/configuration requires it. It is not the platform name and not a workspace label.

## Canonical repository authority

Read in this order:

1. `docs/product-architecture-v1.md` — product ownership and producer boundaries.
2. `docs/product-backbone-spec-v1.md` — detailed UI/API/custody/security contract.
3. `LIVING_IMPLEMENTATION_PLAN.md` — the single current implementation sequence.
4. `AGENTS.md` — implementation and review discipline.

There are no compatibility planning documents or secondary implementation checklists.

## Current repository shape

- `product/tradercockpit/app_server.py` — the one canonical application server.
- `product/tradercockpit/desktop.py` — thin native desktop host around that server/UI.
- `product/tradercockpit/sqx_presets.py` — read-only native runtime/preset verification.
- `product/tradercockpit/sqx_builder_config.py` — read-only Builder project configuration custody.
- `product/tradercockpit/sqx_outputs.py` — read-only Builder output archive inspection.
- `product/tradercockpit/sqx_custom_project.py` — read-only native project topology custody.
- `web/**` — the one product UI used by browser acceptance and the desktop host.
- `tests/**` — current product/runtime/browser/desktop acceptance only.
- `docs/**` — exactly the canonical architecture and backbone documents.
- `tools/check_production_boundary.py` — rejects prohibited foreign/reference/legacy architecture leakage.

The clean baseline intentionally has **no platform strategy schema, generic backtest engine/evaluator/run framework, native Retester implementation, candidate/run/result store, or native mutation endpoint**. Those will be implemented from the current architecture and living plan rather than inherited from removed legacy abstractions.

## Native backend state

Current SQX integration is deliberately read-only. The product can inspect verified runtime/preset/configuration/output/project evidence, but all native POST/mutation actions are refused until the trusted native gateway and new custody/identity contracts are implemented.

This prevents an unverified `sqcli.exe` or stale generic evaluator path from becoming production authority.

## Desktop

The desktop is a thin native window around the same canonical local server and `web/` UI. It does not create a second backend.

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

The desktop private server is loopback-only, validates its exact Host, and rejects cross-origin browser mutations.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

Product Runtime Acceptance additionally starts the canonical server and runs Chromium acceptance.

## Development rule

Every new implementation branch starts from current `main`, follows the first incomplete applicable item in `LIVING_IMPLEMENTATION_PLAN.md`, and is deleted after merge. User-facing progress must appear in the same development desktop rather than accumulating as disconnected backend fragments.
