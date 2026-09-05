# StrategyQuant X official documentation mirror

Markdown mirror of <https://strategyquant.com/doc/> fetched 2026-09-04. Reference material for agents and reviewers; not a roadmap, not a runtime dependency, and not a substitute for exercising the installed producer. Page order follows the official documentation sidebar. Images stay as remote links to strategyquant.com.

Start with `SQX_PROGRAM_GUIDE.md` (synthesized operating guide with a concept-to-TraderCockpit map). `digests/` holds section digests that cite these pages. `manifest.json` records the exact source URL and SHA-256 per page. Refresh with `python tools/mirror_sqx_docs.py`.

## StrategyQuant X

### Introduction

- [Introduction – What is StrategyQuant](strategyquant/01-introduction/introduction.md)
- [What’s new in StrategyQuant X?](strategyquant/01-introduction/whats-new-in-strategyquant-x.md)
- [How does StrategyQuant work?](strategyquant/01-introduction/how-does-strategyquant-work.md)
- [System requirements](strategyquant/01-introduction/system-requirements.md)
- [Installation](strategyquant/01-introduction/installation.md)
- [Backtesting engines – MetaTrader 4,MetaTrader 5, Tradestation](strategyquant/01-introduction/backtesting-engines-metatrader-4metatrader-5-tradestation-ninjatrader.md)
- [Multiple exits generation (scale out, ATM)](strategyquant/01-introduction/multiple-exits-generation-scale-out-atm.md)
- [Update of folder structure from Build 131](strategyquant/01-introduction/update-of-folder-structure-from-build-131.md)

### Installation

- [Setting up firewall for StrategyQuant X on Linux](strategyquant/02-installation/setting-up-firewall-for-strategyquant-x-on-linux.md)
- [How to solve an issue when StrategyQuant X is not able to launch – awt.ddl can’t find dependent librearies](strategyquant/02-installation/how-to-solve-an-issue-when-strategyquant-x-is-not-able-to-launch-awt-ddl-cant-find-dependent-librearies.md)
- [How to install StrategyQuant X and transfer your data from a previous version.](strategyquant/02-installation/how-to-install-strategyquant-x-and-transfer-your-data-from-a-previous-version.md)

### Quick start

- [Automatic Portfolio Builder](strategyquant/03-quick-start/automatic-portfolio-builder.md)
- [Program layout](strategyquant/03-quick-start/program-layout.md)
- [Builder layout](strategyquant/03-quick-start/builder-layout.md)
- [Databanks and files](strategyquant/03-quick-start/databanks-and-files.md)
- [Different build modes](strategyquant/03-quick-start/different-build-modes.md)
- [Strategy style](strategyquant/03-quick-start/strategy-style.md)
- [Cross checks – automated strategy robustness tests](strategyquant/03-quick-start/cross-checks-automated-strategy-robustness-tests.md)
- [Types of robustness tests in SQX](strategyquant/03-quick-start/types-of-robustness-tests-in-sqx.md)
- [Recommended workflows for building strategies](strategyquant/03-quick-start/workflow.md)
- [Fit strategy to existing portfolio](strategyquant/03-quick-start/fit-strategy-to-existing-portfolio.md)
- [Custom analysis](strategyquant/03-quick-start/custom-analysis.md)
- [New Benchmarking feature](strategyquant/03-quick-start/new-benchmarking-feature.md)

### New features

- [Results Plugins](strategyquant/04-new-features/results-plugins.md)
- [Broker profiles](strategyquant/04-new-features/broker-profiles.md)
- [Portfolio Composer](strategyquant/04-new-features/portfolio-composer.md)
- [Automatic Portfolio Construction](strategyquant/04-new-features/automatic-portfolio-construction.md)

### Program screens

- [Indicators calibration (new in B131)](strategyquant/05-program-screens/indicators-calibration.md)
- [Builder](strategyquant/05-program-screens/builder.md)
- [Databanks](strategyquant/05-program-screens/databank.md)
- [Progress – project logs, performance stats and charts](strategyquant/05-program-screens/project-logs-performance-stats-and-charts.md)
- [Full settings](strategyquant/05-program-screens/settings.md)
- [Settings – What to build](strategyquant/05-program-screens/what-to-build.md)
- [Settings – Parts to improve](strategyquant/05-program-screens/parts-to-improve.md)
- [Settings – Genetic options](strategyquant/05-program-screens/genetic-options.md)
- [Settings – Data](strategyquant/05-program-screens/data.md)
- [Settings – Trading options](strategyquant/05-program-screens/trading-options.md)
- [Settings – Building blocks](strategyquant/05-program-screens/building-blocks.md)
- [Settings – ATM](strategyquant/05-program-screens/settings-atm.md)
- [Settings – Money management](strategyquant/05-program-screens/money-management.md)
- [Settings – Cross checks](strategyquant/05-program-screens/cross-checks-robustness-tests.md)
- [Settings – Ranking](strategyquant/05-program-screens/ranking-options.md)
- [Settings – Notes](strategyquant/05-program-screens/notes-2.md)
- [Results – Overview](strategyquant/05-program-screens/results-overview.md)
- [Results – List of trades](strategyquant/05-program-screens/results-list-of-trades.md)
- [Results – Strategy analysis metrics](strategyquant/05-program-screens/strategy-analysis-metrics.md)
- [Results – Equity chart](strategyquant/05-program-screens/results-equity-chart.md)
- [Results – Trade analysis](strategyquant/05-program-screens/results-trade-analysis.md)
- [Results – Strategy correlation](strategyquant/05-program-screens/results-strategy-correlation.md)
- [Results – Trades on chart](strategyquant/05-program-screens/results-trades-on-chart.md)
- [Results – Strategy config](strategyquant/05-program-screens/results-strategy-config.md)
- [Results – Source code](strategyquant/05-program-screens/results-source-code.md)
- [Builder – Dismiss similar strategies in databank](strategyquant/05-program-screens/builder-dismiss-similar-strategies-in-databank.md)

### Cross checks - robustness tests

- [Use Cross checks build in Builder and Retester](strategyquant/06-cross-checks-robustness-tests/use-cross-checks-build-builder-retester.md)
- [Retest with higher precision](strategyquant/06-cross-checks-robustness-tests/retest-higher-precision.md)
- [Monte Carlo trades manipulation](strategyquant/06-cross-checks-robustness-tests/monte-carlo-trades-manipulation.md)
- [Retest on additional markets](strategyquant/06-cross-checks-robustness-tests/retest-additional-markets.md)
- [Monte Carlo retest methods](strategyquant/06-cross-checks-robustness-tests/monte-carlo-retest-methods.md)
- [What If simulations](strategyquant/06-cross-checks-robustness-tests/what-if-simulations.md)
- [Optimization Profile and System Parameter Permutation in StrategyQuant](strategyquant/06-cross-checks-robustness-tests/optimization-profile-system-parameter-permutation-strategyquant.md)

### Optimization

- [Walk-Forward Optimization](strategyquant/07-optimization/walk-forward-optimization.md)
- [Walk-Forward Matrix](strategyquant/07-optimization/walk-forward-matrix.md)
- [Description of advanced Walk-Forward values that can be used in filters / databank](strategyquant/07-optimization/description-advanced-walk-forward-values-can-used-filters-databank.md)
- [Simple Optimization](strategyquant/07-optimization/simple-optimization.md)
- [Recommended optimization parameters](strategyquant/07-optimization/recommended-optimization-parameters.md)
- [Sequential optimization](strategyquant/07-optimization/sequential-optimization.md)

### Strategy templates, custom blocks and indicators

- [Strategy templates](strategyquant/08-strategy-templates-custom-blocks-and-indicators/strategy-templates.md)
- [Custom blocks](strategyquant/08-strategy-templates-custom-blocks-and-indicators/custom-blocks.md)
- [Random groups](strategyquant/08-strategy-templates-custom-blocks-and-indicators/random-groups.md)
- [External indicators](strategyquant/08-strategy-templates-custom-blocks-and-indicators/external-indicators.md)
- [Configuring parameter ranges for standard and custom blocks](strategyquant/08-strategy-templates-custom-blocks-and-indicators/configuring-parameter-ranges-for-standard-and-custom-blocks.md)

### Custom projects and tasks

- [Introduction to custom projects](strategyquant/09-custom-projects-and-tasks/introduction-to-custom-projects.md)
- [Main concepts](strategyquant/09-custom-projects-and-tasks/custom-projects-main-concepts.md)
- [Build strategies task](strategyquant/09-custom-projects-and-tasks/build-strategies-task.md)
- [Retest strategies task](strategyquant/09-custom-projects-and-tasks/retest-strategies-task.md)
- [Automatic retest task](strategyquant/09-custom-projects-and-tasks/automatic-retest.md)
- [Automatic retest – Data setting](strategyquant/09-custom-projects-and-tasks/automatic-retest-data.md)
- [Optimize strategies task](strategyquant/09-custom-projects-and-tasks/optimization.md)
- [Stop & Start task](strategyquant/09-custom-projects-and-tasks/stop-and-start.md)
- [Filter strategies task](strategyquant/09-custom-projects-and-tasks/filtering.md)
- [Go To Task](strategyquant/09-custom-projects-and-tasks/go-to-task.md)
- [Notification task](strategyquant/09-custom-projects-and-tasks/notification.md)
- [Create portfolio task](strategyquant/09-custom-projects-and-tasks/create-portfolio.md)
- [Clear databanks task](strategyquant/09-custom-projects-and-tasks/clear-databanks.md)
- [Load from files task](strategyquant/09-custom-projects-and-tasks/load-from-files.md)
- [Save to files task](strategyquant/09-custom-projects-and-tasks/save-to-files.md)
- [Call external script task](strategyquant/09-custom-projects-and-tasks/call-external-script.md)
- [Delete a file](strategyquant/09-custom-projects-and-tasks/delete-file.md)
- [Wait for user/file](strategyquant/09-custom-projects-and-tasks/wait-for.md)
- [Update data task](strategyquant/09-custom-projects-and-tasks/update-data.md)
- [Log databank contents](strategyquant/09-custom-projects-and-tasks/log-databank-stats.md)

### Reliable backtesting & trading

- [Reliable backtesting in MetaTrader](strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-metatrader.md)
- [Reliable backtesting in Tradestation / MultiCharts](strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-tradestation-multicharts.md)
- [The strategy tried to place stop/limit order at incorrect price](strategyquant/10-reliable-backtesting-trading/the-strategy-tried-to-place-stop-limit-order-at-incorrect-price.md)
- [Reliable backtesting of futures in MT5 – Trading sessions](strategyquant/10-reliable-backtesting-trading/reliable-backtesting-of-futures-in-mt5-trading-sessions.md)
- [Not supported for engine](strategyquant/10-reliable-backtesting-trading/not-supported-for-engine.md)
- [Reliable backtesting in JForex](strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-jforex.md)
- [Comparing and using the same backtest settings](strategyquant/10-reliable-backtesting-trading/comparing-and-using-the-same-backtest-settings.md)
- [Exporting data from Tradestation – recommendation](strategyquant/10-reliable-backtesting-trading/exporting-data-from-tradestation-recommendation.md)
- [Best practices for multi-TF strategies backtesting and trading](strategyquant/10-reliable-backtesting-trading/best-practices-for-multi-tf-strategies-backtesting-and-trading.md)
- [Stockpicker Backtest Limitations](strategyquant/10-reliable-backtesting-trading/sp-backtest-limitations.md)

### How to...

- [Data migration between StrategyQuant versions](strategyquant/11-how-to/data-migration-between-strategyquant-version.md)
- [Export strategy from StrategyQuant and test or trade it in MetaTrader](strategyquant/11-how-to/export-strategy-strategyquant-test-trade-metatrader.md)
- [How to install Strategy Quant indicators to Metatrader 4/5](strategyquant/11-how-to/how-to-install-strategy-quant-indicators-to-metatrader-45.md)
- [How to run the Metatrader in portable mode and what it is good for?](strategyquant/11-how-to/how-to-run-the-metatrader-in-portable-mode-and-what-it-is-good-for.md)
- [How to load and save build config](strategyquant/11-how-to/how-to-load-and-save-build-config.md)
- [Manually configure internal web server port](strategyquant/11-how-to/manually-configure-internal-web-server-port.md)
- [Merge / Split Portfolio](strategyquant/11-how-to/merge-split-portfolio.md)
- [Switching logs to debug mode](strategyquant/11-how-to/switching-logs-to-debug-mode.md)
- [Fixing blurry user interface](strategyquant/11-how-to/fixing-blurry-user-interface.md)
- [How to enable or disable GPU acceleration in StrategyQuant X](strategyquant/11-how-to/how-enable-or-disable-gpu-acceleration-in-strategyquant-x.md)
- [How to switch license in current installation](strategyquant/11-how-to/how-to-switch-license-in-current-installation.md)
- [Starting StrategyQuantX with more memory](strategyquant/11-how-to/starting-sq-with-more-memory.md)
- [How to downgrade](strategyquant/11-how-to/how-to-downgrade.md)
- [Use OppositeBlocks configuration to control the negation](strategyquant/11-how-to/use-oppositeblocks-configuration-to-control-the-negation.md)
- [Multiple orders to the same direction](strategyquant/11-how-to/multi-orders-to-same-direction.md)
- [Troubleshooting](strategyquant/11-how-to/troubleshooting.md)

### SQ 4 Business (MQL Market)

- [What is MQL Market and what it offers?](strategyquant/12-sq-4-business-mql-market/what-is-mql-market-and-what-it-offers.md)
- [Creating MQL4 / MQL 5 product on MQL Market step by step](strategyquant/12-sq-4-business-mql-market/creating-mql4-mql-5-product-on-mql-market-step-by-step.md)

### Pages linked from the documentation but not in the sidebar

- [External indicators](strategyquant/zz-unlisted/custom-data-indicators.md)
- [Strategy templates](strategyquant/zz-unlisted/developing-strategies-using-custom-strategy-templates.md)
- [Market Profile](strategyquant/zz-unlisted/market-profile.md)
- [MCP Integration](strategyquant/zz-unlisted/mcp-integration.md)
- [Problems after upgrading from build 133 to 134](strategyquant/zz-unlisted/problems-after-upgrading-from-build-133-to-134.md)
- [Understanding automatic dismissal rules](strategyquant/zz-unlisted/understanding-automatic-dismissal-rules.md)
- [The Volume Profile](strategyquant/zz-unlisted/volume-profile.md)

## CLI (command line)

### Introduction

- [Introduction to CLI](cli-command-line/01-introduction/introduction-to-cli.md)

### Commands (SQ only)

- [Importing Multiple External Indicator Values Using CLI Command.](cli-command-line/02-commands-sq-only/importing-multiple-external-indicator-values-using-cli-command-2.md)
- [-project Manage projects](cli-command-line/02-commands-sq-only/project-manage-projects.md)
- [-databank Manage databanks](cli-command-line/02-commands-sq-only/databank-manage-databanks.md)
- [-tools Tools](cli-command-line/02-commands-sq-only/tools-tools.md)

### Commands (SQ & QDM)

- [-symbol Manage symbols](cli-command-line/03-commands-sq-qdm/symbol-manage-symbols.md)
- [-instrument Manage instruments](cli-command-line/03-commands-sq-qdm/instrument-manage-instruments.md)
- [-data Manage data](cli-command-line/03-commands-sq-qdm/data-manage-data.md)
- [-run Runs commands from the file](cli-command-line/03-commands-sq-qdm/run-runs-commands-from-the-file.md)
- [-gui Starts webserver to access GUI remotely](cli-command-line/03-commands-sq-qdm/gui-starts-webserver-to-access-gui-remotely.md)
- [-waitfor Waits for user/file](cli-command-line/03-commands-sq-qdm/waitfor-waits-for-user-file.md)
- [-deletefile Deletes the specific file](cli-command-line/03-commands-sq-qdm/deletefile-deletes-the-specific-file.md)
- [-execute Calls external script](cli-command-line/03-commands-sq-qdm/execute-calls-external-script.md)
- [-exit Exit](cli-command-line/03-commands-sq-qdm/exit-exit.md)

## Programming for StrategyQuant X

### Introduction

- [Introduction](programming-for-sq/01-introduction/introduction-2.md)
- [Public API Javadoc](programming-for-sq/01-introduction/public-api-javadoc.md)
- [Working with ResultsGroup](programming-for-sq/01-introduction/working-with-resultsgroup.md)
- [Adding indicators and signals](programming-for-sq/01-introduction/adding-indicators-and-signals.md)
- [Adding databank column / filter](programming-for-sq/01-introduction/adding-new-databank-column.md)
- [Logging and DebugConsole](programming-for-sq/01-introduction/logging-and-debugcolsole.md)
- [Using custom JAR libraries](programming-for-sq/01-introduction/using-custom-jar-libraries.md)
- [Import / Export custom indicators and other snippets](programming-for-sq/01-introduction/import-export-custom-indicators-and-other-snippets.md)

### Indicators / Signals step-by-step

- [Calling another indicator from indicator snippet](programming-for-sq/02-indicators-signals-step-by-step/calling-another-indicator-from-indicator-snippet.md)
- [Testing new indicator in SQ X vs data from MT](programming-for-sq/02-indicators-signals-step-by-step/testing-new-indicator-in-sq-x-vs-data-from-mt.md)
- [ForceIndex indicator](programming-for-sq/02-indicators-signals-step-by-step/adding-new-indicator-snippet-forceindex.md)
- [Envelopes indicator](programming-for-sq/02-indicators-signals-step-by-step/adding-envelopes-indicator-step-by-step.md)
- [ForceIndex Signal blocks](programming-for-sq/02-indicators-signals-step-by-step/creating-signals-blocks-based-on-indicators.md)

### Databanks columns / Filters step-by-step

- [SQN InSample / OutOfSample ratio](programming-for-sq/03-databanks-columns-filters-step-by-step/adding-new-databank-column-advanced.md)

### Charts step-by-step

- [Chart – Accepted strategies per hour](programming-for-sq/04-charts-step-by-step/chart-accepted-strategies-per-hour.md)

### Custom analysis

- [Example – per strategy custom analysis](programming-for-sq/05-custom-analysis/example-per-strategy-custom-analysis.md)
- [Example – per databank custom analysis](programming-for-sq/05-custom-analysis/example-per-databank-custom-analysis.md)

### Trade analysis

- [Trade Analysis – Avg Edge Ratio by hour](programming-for-sq/06-trade-analysis/trader-analysis-avg-edge-ratio-by-hour.md)

### What If

- [Equity moving average simulation](programming-for-sq/07-what-if/equity-moving-average-simulation.md)

### Trades Columns

- [Example – Trade Edge Ratio](programming-for-sq/08-trades-columns/trade-edge-ratio.md)

### Commissions

- [Minimum Commission Example](programming-for-sq/09-commissions/minimum-comission-example.md)

### Position Sizing

- [ATR Volatility Simple Sizing](programming-for-sq/10-position-sizing/atr-volatility-simple-sizing.md)

### Coding sessions examples

- [Exporting Strategy Data to a File Using a Template and Action Block](programming-for-sq/11-coding-sessions-examples/exporting-strategy-data-to-a-file-using-a-template-and-action-block.md)
- [Running optimizations programmatically – update](programming-for-sq/11-coding-sessions-examples/running-optimizations-programmatically.md)
- [Recognizing results in WF Matrix around custom field](programming-for-sq/11-coding-sessions-examples/recognizing-results-in-wf-matrix-around-custom-field.md)
- [Changing strategy parameters programmatically](programming-for-sq/11-coding-sessions-examples/changing-strategy-parameters-programmatically.md)
- [Viewing and changing strategy parameters – version 2](programming-for-sq/11-coding-sessions-examples/viewing-and-changing-strategy-parameters-version-2.md)
- [Save databank results to DB](programming-for-sq/11-coding-sessions-examples/save-databank-results-to-db.md)
- [Running strategy backtests programmatically](programming-for-sq/11-coding-sessions-examples/running-strategy-backtests-programmatically.md)
- [Backtesting strategy programmatically including robustness tests](programming-for-sq/11-coding-sessions-examples/backtesting-strategy-programmatically-including-robustness-tests.md)
- [Loading history data in snippets](programming-for-sq/11-coding-sessions-examples/loading-history-data-in-snippets.md)
- [Merging multiple results into portfolio](programming-for-sq/11-coding-sessions-examples/merging-multiple-results-into-portfolio.md)
- [Changing task config programmatically](programming-for-sq/11-coding-sessions-examples/changing-task-config-programmatically.md)
- [Using backtest settings in strategy metrics](programming-for-sq/11-coding-sessions-examples/using-backtest-settings-in-strategy-metrics.md)
- [Selecting building blocks programmatically](programming-for-sq/11-coding-sessions-examples/selecting-building-blocks-programmatically.md)

### SQ Python

- [Calling Python from SQ (Java) – Introduction](programming-for-sq/12-sq-python/calling-python-from-sq-java-introduction.md)

### Plugins

- [Filter by correlation – plugin example](programming-for-sq/13-plugins/filter-by-correlation-plugin-example.md)
- [Example plugin – a complete custom project task](programming-for-sq/13-plugins/example-plugin-a-complete-custom-project-task.md)

## QuantDataManager

### Introduction

- [Introduction to QDM](quantdatamanager/01-introduction/introduction-to-qdm.md)

### Command line

- [List of available commands](quantdatamanager/02-command-line/quant-data-manager-command-line-interface-help.md)
- [Script Examples (Windows)](quantdatamanager/02-command-line/quant-data-manager-command-line-interface-script-examples.md)

### How to...

- [Import history data from MetaTrader 4](quantdatamanager/03-how-to/import-history-data-metatrader-4.md)
- [Test strategy in MetaTrader 4 with tick precision](quantdatamanager/03-how-to/test-strategy-metatrader-4-tick-precision.md)
- [How to export data from Quant Data Manager and import to Metatrader 5](quantdatamanager/03-how-to/how-to-import-data-to-metatrader-5.md)
- [How to export data from Metatrader 5](quantdatamanager/03-how-to/how-to-export-data-from-metatrader-5.md)

### Pages linked from the documentation but not in the sidebar

- [MetaTrader 5 Direct API Data Import](quantdatamanager/zz-unlisted/metatrader5-data-import.md)
