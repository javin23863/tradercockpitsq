# SQX engine module map

This map is the shortest route from a requested backend capability to the
source-derived material included in this branch. Paths are relative to the
repository root.

## Backtest engine and retester

The execution path is split across the core trading library, the embedded
runtime, simulator implementations, and task plugins:

- `sources/engine-core/com/strategyquant/tradinglib/backtest/`
  - backtest connections, data feeds, caches, jobs, and trading-setup runners
- `sources/engine-core/com/strategyquant/tradinglib/backtestrunner/`
  - `BacktestRunner`, `BacktestSettings`, `BacktestResult`, listeners, duration
    statistics, population types, and strategy checks
- `sources/engine-core/com/strategyquant/tradinglib/engine/`
  - `AbstractBacktestEngine`, `BacktestEngine`, `TradingEngine`, trading setup,
    status, and portfolio backtesting
- `sources/engine-core/com/strategyquant/tradinglib/simulator/`
  - order execution and platform-specific simulators for MetaTrader 4/5,
    JForex, MultiCharts, TradeStation, and the generic simulator interfaces
- `sources/plugins/TaskRetest/`
  - retest task wiring and task configuration
- `sources/plugins/AppRetester/`
  - application-level retester module
- `sources/plugins/SettingsWhatToRetest/`
  - retest settings and data-period controls

## Strategy builder and genetic evolution

- `sources/engine-core/com/strategyquant/tradinglib/generator/`
  - generation and build support
- `sources/engine-core/com/strategyquant/tradinglib/gp/`
  - genetic programming/evolution primitives, populations, operators, and
    strategy representations
- `sources/engine-core/com/strategyquant/tradinglib/gp/strategies/`
  - evolved strategy structures and strategy-level GP operations
- `sources/plugins/TaskBuild/`
  - build task, random build engine, population/evolution task wiring
- `sources/plugins/AppBuilder/`
  - builder application surface
- `sources/plugins/SettingsWhatToBuild/`
  - build settings and generation controls
- `references/code-templates/`
  - strategy and code-generation templates used by the extension layer

The copied `TaskBuild/task_futures.xml` sample also preserves concrete build
settings for genetic evolution, population size, generations, mutation,
crossover, islands, migration, and SL/PT building-block choices.

## Optimizer and walk-forward

- `sources/engine-core/com/strategyquant/tradinglib/optimization/`
  - optimization profiles, parameter handling, objectives, and optimization
    support
- `sources/plugins/TaskOptimize/`
  - optimization task orchestration
- `sources/plugins/AppOptimizer/`
  - optimizer application surface
- `sources/plugins/SettingsOptimization/`
  - optimizer settings
- `sources/engine-core/com/strategyquant/tradinglib/wfo/`
  - walk-forward structures and processing
- `sources/plugins/CrossCheckWalkForwardOptimization/`
- `sources/plugins/CrossCheckWalkForwardMatrix/`
- `sources/plugins/ResultsWalkForward/`
- `sources/plugins/CrossCheckSequentialOptimization/`
- `sources/plugins/ResultsSequentialOptimization/`

## Monte Carlo and robustness

Core and plugin surfaces are both included:

- `sources/engine-core/com/strategyquant/tradinglib/montecarlo/`
- `sources/engine-core/com/strategyquant/tradinglib/robustnesstests/`
- `sources/indicators-building-blocks/SQ/MonteCarlo/`
- `sources/plugins/CrossCheckMonteCarloManipulation/`
- `sources/plugins/CrossCheckMonteCarloRetest/`
- `sources/plugins/ResultsRobustnessTests/`

The source inventory includes trade-order randomization, skipped trades,
execution degradation, parameter jitter, starting-bar randomization, spread,
slippage, minimum-distance, and history-data manipulations.

## Indicators and building blocks

The complete decompiled `Snippets.jar` source is under
`sources/indicators-building-blocks/`. Notable families include:

- `SQ/Blocks/Price`
- `SQ/Blocks/Indicator`
- `SQ/Blocks/Condition`
- `SQ/Blocks/Comparison`
- `SQ/Blocks/Order`
- `SQ/Blocks/Modify`
- `SQ/Blocks/StrategyControl`
- `SQ/ExitMethods`
- `SQ/Formulas`
- `SQ/TradingOptions`
- `SQ/MoneyManagement`
- `SQ/RiskManagement`
- `SQ/Columns`
- `SQ/Stats`
- `SQ/TradeAnalysis`
- `SQ/MonteCarlo`

The reference copy also preserves readable snippet extensions under
`references/extensions/snippets/`, template material under
`references/code-templates/`, and readable custom-indicator resources under
`references/custom-indicators/`.

## Stops, targets, and trade controls

The directly relevant building-block source is:

- `sources/indicators-building-blocks/SQ/ExitMethods/StopLoss.java`
- `sources/indicators-building-blocks/SQ/ExitMethods/ProfitTarget.java`
- `sources/indicators-building-blocks/SQ/ExitMethods/TrailingStop.java`
- `sources/indicators-building-blocks/SQ/ExitMethods/MoveSL2BE.java`
- `sources/indicators-building-blocks/SQ/ExitMethods/ExitAfterBars.java`
- `sources/indicators-building-blocks/SQ/Blocks/Order/Modify/SetStopLoss.java`
- `sources/indicators-building-blocks/SQ/Blocks/Order/Modify/SetProfitTarget.java`
- `sources/indicators-building-blocks/SQ/Blocks/StrategyControl/OrderSL.java`
- `sources/indicators-building-blocks/SQ/TradingOptions/UseInitialSLPT.java`
- `sources/indicators-building-blocks/SQ/TradingOptions/MinMaxSLPT.java`

These are accompanied by the engine-side order, exit, money-management, and
risk-management packages under `sources/engine-core/com/strategyquant/`.

## Data and orchestration

- `sources/data-lib/` — data model and data services
- `sources/grid-lib/` — grid/distributed execution support
- `sources/jobs-lib/` — job abstractions
- `sources/plugin-api/` — extension/plugin contracts
- `sources/web-gui-lib/` — web GUI support contracts
- `sources/wizard-business/` — wizard/business support
- `sources/plugins/AppTaskManager/` — task-manager application surface
- `sources/plugins/TaskManager/` and task-specific plugin roots — task wiring
- `references/workflows/` — readable workflow export sample
- `references/plugin-assets/` — plugin metadata, UI assets, and configuration

## Prop-firm simulation boundary

No standalone `PropFirm`, `Funding`, or `Challenge` module was present in the
Build 144.2953 archive. The included primitives relevant to a new prop-firm
policy adapter are documented in [`prop-firm-gap.md`](prop-firm-gap.md).
