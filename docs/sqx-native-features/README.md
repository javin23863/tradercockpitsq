# SQX native features catalog

Producer settings and Results source-of-truth for LLMs. Not a product roadmap.

**Ingest date:** 2026-09-04

## Kind legend

| Kind | Meaning |
|------|---------|
| `write` | Settings fields persisted in existing project/strategy XML |
| `native-run` | Actions executed by SQX (Start, Calibrate now, Load/Save, cross-check runs) |
| `read-model` | Values read from producer artifacts (orders.bin, databank columns, charts) |
| `unavailable` | No integration seam yet |

## Index

[Programming guide and current integration](programming-guide-integration.md)
(2026-09-05) maps the complete programming/CLI guide navigation to current product
callers, native extension points, and unverified runtime dependencies.

| Feature | SQX tab | Typical XML/artifact | Widget | Kind | Source URL | Screenshot |
|---------|---------|----------------------|--------|------|------------|------------|
| Indicators calibration | Builder › Building blocks | Project/builder XML; indicator min/max/step calibration cache | Calibrate now; calibrate before start | `native-run` | [indicators-calibration](https://strategyquant.com/doc/strategyquant/indicators-calibration/) | [popup.png](screenshots/popup.png) |
| Builder module | Builder | Builder project XML; databank strategies | Progress / Full settings / Results tabs; Start/Pause/Stop | `native-run` | [builder](https://strategyquant.com/doc/strategyquant/builder/) | — |
| Databanks | Builder / Retester / Optimizer › Databank | Databank store; strategy result bundles | Load / Save; strategy selection | `native-run` | [databank](https://strategyquant.com/doc/strategyquant/databank/) | — |
| Progress logs and charts | Builder › Progress | Runtime logs; performance/memory charts | Progress screen charts | `read-model` | [project-logs-performance-stats-and-charts](https://strategyquant.com/doc/strategyquant/project-logs-performance-stats-and-charts/) | [progress_charts.png](screenshots/progress_charts.png) |
| What to build | Builder › Full settings › What to build | Builder project XML (WhatToBuild section) | Strategy type, direction, build mode, conditions, SL/PT | `write` | [what-to-build](https://strategyquant.com/doc/strategyquant/what-to-build/) | [what_to_build1.png](screenshots/what_to_build1.png) |
| Parts to improve | Builder › Full settings › Parts to improve | Builder project XML (ImproveExistingStrategy) | Entry/Exit/Order type improve options | `write` | [parts-to-improve](https://strategyquant.com/doc/strategyquant/parts-to-improve/) | — |
| Genetic options | Builder › Full settings › Genetic options | Builder project XML (GeneticOptions) | Generations, population, islands, migration, fresh blood | `write` | [genetic-options](https://strategyquant.com/doc/strategyquant/genetic-options/) | [sq_genetic_options.png](screenshots/sq_genetic_options.png) |
| Data settings | Builder › Full settings › Data | Builder project XML (Data); chart/symbol bindings | Trading engine, symbol, timeframe, date range | `write` | [data](https://strategyquant.com/doc/strategyquant/data/) | — |
| Trading options | Builder › Full settings › Trading options | Builder project XML (TradingOptions) | EOD close, chart data storage, testing conditions | `write` | [trading-options](https://strategyquant.com/doc/strategyquant/trading-options/) | — |
| Building blocks | Builder › Full settings › Building blocks | Builder project XML (BuildingBlocks) | Block checkboxes, weight/percent, calibrate before start | `write` | [building-blocks](https://strategyquant.com/doc/strategyquant/building-blocks/) | [building_blocks.png](screenshots/building_blocks.png) |
| Advanced Trade Management (ATM) | Builder › Full settings › ATM | Builder project XML (ATM) | Multiple exit configuration | `write` | [settings-atm](https://strategyquant.com/doc/strategyquant/settings-atm/) | — |
| Money management | Builder › Full settings › Money management | Builder project XML (MoneyManagement) | Position sizing method and parameters | `write` | [money-management](https://strategyquant.com/doc/strategyquant/money-management/) | — |
| Cross checks settings | Builder › Full settings › Cross checks | Builder project XML (CrossChecks / AcceptanceSettings) | Cross-check enable/config sliders and filters | `write` | [cross-checks-robustness-tests](https://strategyquant.com/doc/strategyquant/cross-checks-robustness-tests/) | [cross_checks.png](screenshots/cross_checks.png) |
| Cross checks concept | Builder / Retester › Cross checks | Cross-check result columns in databank | Robustness test definitions (reference) | `read-model` | [cross-checks-automated-strategy-robustness-tests](https://strategyquant.com/doc/strategyquant/cross-checks-automated-strategy-robustness-tests/) | — |
| Cross checks in Builder/Retester | Builder / Retester › Cross checks | Cross-check config in project XML; sample-type result columns | Simple slider or advanced per-check toggles | `write` | [use-cross-checks-build-builder-retester](https://strategyquant.com/doc/strategyquant/use-cross-checks-build-builder-retester/) | — |
| Monte Carlo trades manipulation | Builder › Cross checks › MC trades | Cross-check config; MC sample columns from existing trades | MC trades manipulation cross check | `native-run` | [monte-carlo-trades-manipulation](https://strategyquant.com/doc/strategyquant/monte-carlo-trades-manipulation/) | — |
| Retest on additional markets | Builder › Cross checks › Additional markets | Cross-check config; additional-market sample results | Retest on different symbol/timeframe | `native-run` | [retest-additional-markets](https://strategyquant.com/doc/strategyquant/retest-additional-markets/) | — |
| Monte Carlo retest methods | Builder › Cross checks › MC retest | Cross-check config; retest simulation results | Spread/slippage/parameter/data MC retests | `native-run` | [monte-carlo-retest-methods](https://strategyquant.com/doc/strategyquant/monte-carlo-retest-methods/) | — |
| What If simulations | Builder › Cross checks › What If | Cross-check config; What-If sample results | What If scenario cross check | `native-run` | [what-if-simulations](https://strategyquant.com/doc/strategyquant/what-if-simulations/) | [whatif_conf.png](screenshots/whatif_conf.png) |
| Optimization Profile & SPP | Optimizer / Cross checks | Optimization runs; profile/permutation result columns | Optimization Profile; System Parameter Permutation | `native-run` | [optimization-profile-system-parameter-permutation-strategyquant](https://strategyquant.com/doc/strategyquant/optimization-profile-system-parameter-permutation-strategyquant/) | — |
| Walk-Forward Optimization | Optimizer / Cross checks | WFO result columns; WFO equity samples | Walk-Forward Optimization | `native-run` | [walk-forward-optimization](https://strategyquant.com/doc/strategyquant/walk-forward-optimization/) | — |
| Walk-Forward Matrix | Optimizer / Cross checks | WFM result columns | Walk-Forward Matrix | `native-run` | [walk-forward-matrix](https://strategyquant.com/doc/strategyquant/walk-forward-matrix/) | — |
| Walk-Forward databank columns | Databank › Column editor | Databank column defs from WFO/WFM equity | WFO/WFM filter and column values | `read-model` | [description-advanced-walk-forward-values-can-used-filters-databank](https://strategyquant.com/doc/strategyquant/description-advanced-walk-forward-values-can-used-filters-databank/) | — |
| Simple Optimization | Optimizer (SQX module, not left rail) | Optimizer project XML; optimization runs | Optimizer Start; parameter grid/genetic search | `native-run` | [simple-optimization](https://strategyquant.com/doc/strategyquant/simple-optimization/) | — |
| Recommended optimization parameters | Optimizer / Cross checks | Optimizer XML parameter selection | Recommended parameters category | `write` | [recommended-optimization-parameters](https://strategyquant.com/doc/strategyquant/recommended-optimization-parameters/) | — |
| Sequential optimization | Optimizer / Cross checks | Sequential optimization results | Sequential optimization type/cross check | `native-run` | [sequential-optimization](https://strategyquant.com/doc/strategyquant/sequential-optimization/) | — |
| Ranking options | Builder › Full settings › Ranking | Builder project XML (Rankings / AcceptanceSettings) | Fitness criteria; databank capacity; custom conditions | `write` | [ranking-options](https://strategyquant.com/doc/strategyquant/ranking-options/) | [ranking.png](screenshots/ranking.png) |
| Results Overview | Builder › Results › Overview | Backtest statistics; databank columns | Overview statistics panel | `read-model` | [results-overview](https://strategyquant.com/doc/strategyquant/results-overview/) | [overview.png](screenshots/overview.png) |
| List of trades | Builder › Results › List of trades | orders.bin; trade list export | Trades table | `read-model` | [results-list-of-trades](https://strategyquant.com/doc/strategyquant/results-list-of-trades/) | [list_of_trades.png](screenshots/list_of_trades.png) |
| Strategy analysis metrics glossary | Builder › Results › Overview (metrics) | Metric labels in Overview readout | Metric name definitions (Total Profit, CAGR, etc.) | `read-model` | [strategy-analysis-metrics](https://strategyquant.com/doc/strategyquant/results-overview/strategy-analysis-metrics/) | — |
| Equity chart | Builder › Results › Equity chart | Equity curve series from backtest | Equity chart with stagnation marker | `read-model` | [results-equity-chart](https://strategyquant.com/doc/strategyquant/results-equity-chart/) | [result_equity.png](screenshots/result_equity.png) |
| Trade analysis | Builder › Results › Trade analysis | Trade distribution charts; yearly P/L | Trade analysis charts | `read-model` | [results-trade-analysis](https://strategyquant.com/doc/strategyquant/results-trade-analysis/) | [trade_analysis.png](screenshots/trade_analysis.png) |
| Trades on chart | Builder › Results › Trades on chart | Stored chart data; orders on OHLC | Chart with indicators and orders | `read-model` | [results-trades-on-chart](https://strategyquant.com/doc/strategyquant/results-trades-on-chart/) | [trades_on_chart.png](screenshots/trades_on_chart.png) |
| Strategy config diff | Builder › Results › Strategy config | Strategy-embedded config vs current module settings | Config diff; Apply Strategy Config | `read-model` | [results-strategy-config](https://strategyquant.com/doc/strategyquant/results-strategy-config/) | [strategy_config.png](screenshots/strategy_config.png) |
| Source code | Builder › Results › Source code | Generated platform source (MT4/MT5/etc.) | Source code viewer/generator | `read-model` | [results-source-code](https://strategyquant.com/doc/strategyquant/results-source-code/) | [source_code.png](screenshots/source_code.png) |

## Notes

- One markdown file per official doc URL; filenames use the URL slug.
- Metrics glossary (`strategy-analysis-metrics.md`) is labels only — do not treat as formulas.
- Optimizer-only docs refer to the native SQX Optimizer module, not a TraderCockpit left-rail tab.
