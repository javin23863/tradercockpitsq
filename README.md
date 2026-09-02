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
5. `docs/features/` — subordinate implementation guides (ML, Indicator Zoo,
   Wave Intelligence, lifecycle / edge decay, live chart, SQX add-ons).
   Not a second roadmap.
6. `references/quant-guild/` — curated Quant-Guild excerpt bundle
   (reference DATA only; production must not import it).

There are no compatibility planning documents or secondary implementation checklists.

## Current repository shape

- `product/tradercockpit/app_server.py` — the one canonical application server.
- `product/tradercockpit/desktop.py` — thin native desktop host around that server/UI.
- `product/tradercockpit/sqx_runtime.py` — exact native runtime/build/launcher trust descriptor.
- `product/tradercockpit/sqx_gateway.py` — bounded trusted native control gateway; no product mutation endpoint is bound yet.
- `product/tradercockpit/sqx_presets.py` — read-only native runtime/preset verification.
- `product/tradercockpit/sqx_builder_config.py` — read-only Builder project configuration custody.
- `product/tradercockpit/sqx_outputs.py` — read-only Builder output archive inspection.
- `product/tradercockpit/sqx_custom_project.py` — read-only native project topology custody.
- `web/**` — the one product UI used by browser acceptance and the desktop host.
- `tests/**` — current product/runtime/browser/desktop acceptance only.
- `docs/**` — canonical architecture and backbone, plus subordinate
  `docs/features/` implementation guides.
- `references/quant-guild/` — curated Quant-Guild excerpts (data only).
- `tools/check_production_boundary.py` — rejects prohibited foreign/reference/legacy architecture leakage.
- `tools/build_windows_desktop.py` — freezes the same desktop host and canonical `web/` tree into one Windows executable.

The clean baseline intentionally has **no platform strategy schema, generic backtest engine/evaluator/run framework, native Retester implementation, candidate/run/result store, or product feature/API bound to native mutation**. Those capabilities are implemented from the current architecture and living plan rather than inherited from removed legacy abstractions.

## Native backend state

The product can inspect verified runtime/preset/configuration/output/project evidence. One bounded internal native control gateway is implemented for the source-proven Builder `loadconfig -> start` sequence, but no product feature or HTTP endpoint is currently authorized to invoke native mutation.

Launcher/configuration trust is freshly verified at the gateway immediately before native process creation. A read-only status snapshot is never launch authorization.

## Desktop

The desktop is a thin native window around the same canonical local server and `web/` UI. It does not create a second backend or second UI source tree.

Source/development launch:

```bash
python -m pip install -e ".[desktop]"
tradercockpit-desktop
```

The desktop private server is loopback-only, validates its exact Host, and rejects cross-origin browser mutations.

### Windows packaged desktop

Windows uses pywebview with the `edgechromium` renderer explicitly selected, so the packaged desktop requires Microsoft Edge WebView2 Runtime rather than silently accepting a legacy web renderer.

Build from a Windows checkout with Python 3.12:

```powershell
python -m pip install -e ".[desktop,desktop-build]"
python tools/build_windows_desktop.py
```

The output is:

```text
dist/windows/TraderCockpit.exe
```

The PyInstaller build bundles the repository's existing `web/` directory at build time. There is no packaged copy of the UI maintained as a second source tree.

Manual launch:

```powershell
.\dist\windows\TraderCockpit.exe
```

The same executable is exercised in Product Runtime Acceptance on `windows-latest`: acceptance requires the frozen executable to serve the canonical `/home` HTML and keep a real Windows desktop window alive while the host forces WebView2/EdgeChromium. The workflow publishes the tested executable as the `TraderCockpit-windows` artifact.

## Development verification

```bash
python -m pip install --no-deps -e .
python tools/check_production_boundary.py --root .
python -m unittest discover -s tests/product -p 'test_*.py' -v
npm test
```

Product Runtime Acceptance additionally runs Chromium acceptance on Linux and builds/launches the frozen WebView2 desktop on Windows.

## Development rule

Every new implementation branch starts from current `main`, follows the first incomplete applicable item in `LIVING_IMPLEMENTATION_PLAN.md`, and is deleted after merge. User-facing progress must appear in the same development desktop rather than accumulating as disconnected backend fragments.
