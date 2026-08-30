# TraderCockpit SQX Recovery + Clean Product Engine

This repository has two deliberately separated roles:

1. **Reference recovery** — source-oriented extraction of backend and extension behavior from StrategyQuant X Build 144.2953 plus UI/legacy/parity references.
2. **TraderCockpit production implementation** — a clean product domain and engine that may recover proven behavior but does not import or recreate the recovered vendor architecture.

Read [`docs/product-architecture-v1.md`](docs/product-architecture-v1.md) and [`IMPLEMENTATION_CHECKLIST.md`](IMPLEMENTATION_CHECKLIST.md) before production implementation.

## Hard production boundary

- `sources/**` is recovered/decompiled reference material, not production code.
- `references/**` is UI/vendor/legacy/parity/reference material, not production runtime code.
- `javin23863/futures` is read-only donor evidence and is not a TraderCockpit runtime dependency.
- Production code under `product/**` must not import from `sources/**` or `references/**`.

## Reference extraction layout

| Area | Location | Contents |
| --- | --- | --- |
| Core engine | [`sources/engine-core`](sources/engine-core) | `SQTradingLib` decompiled source: backtest engine, simulator, GP/evolution, optimization, WFO, robustness, results, databank, risk, orders, and strategy model |
| Platform runtime | [`sources/platform-runtime`](sources/platform-runtime) | Decompiled embedded `SQLib` runtime classes used by the launcher and engine |
| Launcher | [`sources/launcher-app`](sources/launcher-app) | Decompiled embedded application bootstrap classes |
| Indicators and blocks | [`sources/indicators-building-blocks`](sources/indicators-building-blocks) | `Snippets.jar` source, including `SQ/Blocks`, `SQ/ExitMethods`, formulas, trading options, risk, money management, Monte Carlo, and statistics |
| Supporting libraries | [`sources/data-lib`](sources/data-lib), [`sources/grid-lib`](sources/grid-lib), [`sources/jobs-lib`](sources/jobs-lib), [`sources/plugin-api`](sources/plugin-api), [`sources/web-gui-lib`](sources/web-gui-lib), [`sources/wizard-business`](sources/wizard-business) | Decompiled SQX support APIs and services |
| Plugins | [`sources/plugins`](sources/plugins) | Builder, retester, optimizer, cross-check, result, settings, data, and task plugin roots |
| Non-runtime references | [`references`](references) | Plugin assets, templates, workflows, custom-indicator text, snippets, UI authority, and readable archive resources |
| Product UI authority | [`references/ui-authority`](references/ui-authority) | Canonical multicolor TraderCockpit prototype lineage, manifests, engine-facing consumers, and panel/state references |
| Recovery documentation | [`docs`](docs) | Module map, extraction report, source inventory, authorization scope, prop-firm findings, and clean product architecture |

Start with [`docs/module-map.md`](docs/module-map.md) for the feature-to-source recovery map and [`docs/extraction-report.md`](docs/extraction-report.md) for the extraction inventory. Production work must additionally follow [`docs/product-architecture-v1.md`](docs/product-architecture-v1.md).

## Production direction

The first clean production slice is intentionally smaller than the recovered SQX system:

```text
immutable StrategySpec
       +
CandidateSpec + DataSpec + ExecutionSpec
       ↓
BacktestRunSpec
       ↓
deterministic backtest/evaluator
       ↓
typed result + initial validation
       ↓
evidence manifest
```

Only after that evaluator is trusted do Fast validation and evolutionary search become production implementation targets.

## Reference verification

The extraction source inventory can be checked from the repository root with:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\verify-source.ps1
```

The decompiled source tree is not claimed to compile cleanly as one application. It exists to recover behavior and parity evidence, not to become the TraderCockpit production classpath.

## Scope notes

The repository intentionally does not include installers, license keys, activation services, or unmodified SQX application binaries. Third-party libraries remain external dependencies rather than copied vendor artifacts.

No standalone native `PropFirm` module was found in this exact SQX build; relevant primitives are documented in [`docs/prop-firm-gap.md`](docs/prop-firm-gap.md).

## Attribution

See [`NOTICE`](NOTICE), [`LICENSE`](LICENSE), and [`docs/authorization-scope.md`](docs/authorization-scope.md).
