# TraderCockpit SQX Engine Extraction

This branch contains a source-oriented extraction of the backend and extension
surfaces from StrategyQuant X Build 144.2953. It is organized so the new
TraderCockpit backend can study and reuse the engine boundaries independently:

- backtesting and trading-engine execution
- retesting, optimization, walk-forward, and databank flows
- genetic/evolutionary strategy generation
- Monte Carlo and robustness testing
- indicators, conditions, building blocks, order blocks, and strategy controls
- stop-loss, profit-target, trailing-stop, break-even, and time-exit logic
- data, grid, jobs, plugin APIs, web-gui support, and task orchestration
- plugin assets, workflow samples, templates, custom indicators, and embedded
  configuration resources

## Layout

| Area | Location | Contents |
| --- | --- | --- |
| Core engine | [`sources/engine-core`](sources/engine-core) | `SQTradingLib` decompiled source: backtest engine, simulator, GP/evolution, optimization, WFO, robustness, results, databank, risk, orders, and strategy model |
| Platform runtime | [`sources/platform-runtime`](sources/platform-runtime) | Decompiled embedded `SQLib` runtime classes used by the launcher and engine |
| Launcher | [`sources/launcher-app`](sources/launcher-app) | Decompiled embedded application bootstrap classes |
| Indicators and blocks | [`sources/indicators-building-blocks`](sources/indicators-building-blocks) | `Snippets.jar` source, including `SQ/Blocks`, `SQ/ExitMethods`, formulas, trading options, risk, money management, Monte Carlo, and statistics |
| Supporting libraries | [`sources/data-lib`](sources/data-lib), [`sources/grid-lib`](sources/grid-lib), [`sources/jobs-lib`](sources/jobs-lib), [`sources/plugin-api`](sources/plugin-api), [`sources/web-gui-lib`](sources/web-gui-lib), [`sources/wizard-business`](sources/wizard-business) | Decompiled SQX support APIs and services |
| Plugins | [`sources/plugins`](sources/plugins) | All 176 plugin roots present in the package, including builder, retester, optimizer, automatic retest/portfolio, cross-check, result, settings, data, and task modules |
| Non-class references | [`references`](references) | Plugin assets, templates, workflows, custom-indicator text, snippets, and readable archive resources |
| Product UI authority | [`references/ui-authority`](references/ui-authority) | Canonical multicolor TraderCockpit prototype lineage, exact asset manifest, engine-facing consumer chain, and fail-closed asset verifier |
| Documentation | [`docs`](docs) | Module map, extraction report, source inventory, authorization scope, and prop-firm findings |

Start with [`docs/module-map.md`](docs/module-map.md) for the feature-to-source
map and [`docs/extraction-report.md`](docs/extraction-report.md) for the exact
inventory and verification status. Engine integration work must also read
[`references/ui-authority/README.md`](references/ui-authority/README.md) so the
backend is framed against the canonical product consumers rather than an older
prototype shell.

## Important scope notes

The Java files are decompiled/derived source representations, not the original
SQX source tree. The repository intentionally does not include installers,
license keys, activation services, or unmodified application binaries. Third-
party libraries remain external build dependencies rather than copied vendor
artifacts.

The package exposes generic account, loss, drawdown, risk, Monte Carlo,
robustness, and What-If components that can compose a prop-firm simulation. No
standalone native `PropFirm` module was found in this exact build; see
[`docs/prop-firm-gap.md`](docs/prop-firm-gap.md).

## Verification

Run the source inventory check from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\verify-source.ps1
```

The extraction report records the current state of full-source compilation.
Because decompilers reconstruct generics, synthetic bridge methods, and control
flow imperfectly, a clean full compile is a separate porting task and is not
claimed by this extraction.

## Attribution

See [`NOTICE`](NOTICE), [`LICENSE`](LICENSE), and
[`docs/authorization-scope.md`](docs/authorization-scope.md).
