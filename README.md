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

## Repository shape

- `product/tradercockpit/**` — application, domain, storage, native integration, desktop host.
- `web/**` — the one product UI used by browser acceptance and the desktop host.
- `tests/**` — product, runtime, browser, and desktop acceptance.
- `docs/**` — only canonical architecture and backbone documents.
- `tools/check_production_boundary.py` — rejects prohibited foreign/reference/legacy architecture leakage.

The product does not contain a second platform-owned Builder/GA/backtest/robustness/optimizer/Custom Project quantitative engine.

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
