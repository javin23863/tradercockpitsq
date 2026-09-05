# Programming guide and current integration

**Ingested:** 2026-09-05
**Primary entry:** https://strategyquant.com/doc/programming-for-sq/introduction-2/
**Kind:** `read-model` / `write` / `native-run` / `unavailable`, distinguished below
**Code snapshot:** `codex/research-state-reliability`, HEAD `59c6c9c12aa331d37115b584817e781a4c41716c` plus concurrent working-tree changes, inspected 2026-09-05.

This is a source catalog and integration assessment under the existing [catalog](README.md), [architecture](../product-architecture-v1.md), and [living implementation plan](../../LIVING_IMPLEMENTATION_PLAN.md). It does not establish a new roadmap. “Implemented” below means a current application caller and backend seam exist; it does **not** mean this research exercised or accepted the feature in the installed producer. No native commands, configuration changes, extension imports, or engine execution were performed for this document.

## Findings that affect integration

1. **Native CLI readiness and native web readiness are separate.** Official documentation describes ordinary CLI operation without the UI, and `-gui` as the command exposing browser UI at `localhost:8080`. Existing HTTP and WebSocket callers need that web-capable runtime mode, not merely a successful command request on port 5050. The desktop lane reported `/call?cmd=-h` working on 5050 while the servlet/WebSocket surface was absent in its CLI-only run. That is a lane observation, not an independent observation by this research. [CLI introduction](https://strategyquant.com/doc/cli-command-line/introduction-to-cli/), [GUI server command](https://strategyquant.com/doc/cli-command-line/gui-starts-webserver-to-access-gui-remotely/).
2. **The old introduction is not the last word on plugins.** Its 2020 statement that plugin development is outside the manual predates official examples for databank-action/servlet plugins from Build 136 Dev 2 and complete Task plus Settings plugins from Build 138, with updated examples for 140+. Native extension support is documented; importing an arbitrary native UI into TraderCockpit is a different, unimplemented integration. [Databank-action plugin](https://strategyquant.com/doc/programming-for-sq/filter-by-correlation-plugin-example/), [complete custom task](https://strategyquant.com/doc/programming-for-sq/example-plugin-a-complete-custom-project-task/).
3. **A saved archive, a running databank, and a result selection are different states.** SQX's `ResultsGroup` holds strategy XML, settings, and multiple results. Metrics are selected by result, direction, P/L unit, and sample. Cross-check presence cannot be inferred just by counting ordinary result keys: Monte Carlo and optimization-profile results use special storage, and WFO/WFM can share result-key forms. Preserve native selection and missing values. [ResultsGroup](https://strategyquant.com/doc/programming-for-sq/working-with-resultsgroup/), [per-strategy analysis](https://strategyquant.com/doc/programming-for-sq/example-per-strategy-custom-analysis/).
4. **Configuration is native data, not an archived-template allowlist.** The guide documents cloning native task configuration, changing its XML, and applying it with the native task API; a separate UI refresh is needed after programmatic changes. Its `.cfx` examples are ZIP containers, but task-save and whole-project layouts must be distinguished using the installed producer. The current Research launch packages exact approved Settings with the Task declaration from that same preserved source. [Task configuration](https://strategyquant.com/doc/programming-for-sq/changing-task-config-programmatically/), [building-block selection](https://strategyquant.com/doc/programming-for-sq/selecting-building-blocks-programmatically/), [current launch custody](../../product/tradercockpit/research_native_jobs.py).
5. **Native extensibility does not remove data or export constraints.** The documented `HistoryDataLoader` refuses protected SQ Futures/SQ Equities raw data. External indicator values are bound to the original symbol, timeframe, and matching historical data. An indicator running in SQX still needs target-platform translation and an implementation on that platform before exported strategies can use it. [History loading](https://strategyquant.com/doc/programming-for-sq/loading-history-data-in-snippets/), [external indicators](https://strategyquant.com/doc/strategyquant/external-indicators/), [custom JARs](https://strategyquant.com/doc/programming-for-sq/using-custom-jar-libraries/).

## Operational modes and evidence boundaries

| Mode or channel | Officially documented scope | Current integration / limit |
|---|---|---|
| Command invocation / interactive `sqcli` | Commands from external programs; interactive background engine; `-run` for command files and `-exit` for orderly interactive shutdown. Improved CLI described from SQX 127. [Introduction](https://strategyquant.com/doc/cli-command-line/introduction-to-cli/) | Trusted launcher controls exist in [gateway](../../product/tradercockpit/sqx_gateway.py) and [project control](../../product/tradercockpit/sqx_custom_project_launch.py). Submission, native acceptance, completion, and generated results remain separate. |
| `-project` | List/start/stop/pause/resume/status, load/save configuration, removal. [Command](https://strategyquant.com/doc/cli-command-line/project-manage-projects/) | Product permits bounded project actions; does not expose arbitrary CLI strings. Research approved Builder launch is a distinct custody path. |
| `sqcli -gui` | Starts web server for browser access at the example URL `http://localhost:8080`; no arguments listed. [Command](https://strategyquant.com/doc/cli-command-line/gui-starts-webserver-to-access-gui-remotely/) | The guide does not specify bind-address overrides, Windows process-tree lifetime, an OS-window guarantee, or the wire schema of every internal servlet. These require installed-runtime observation. |
| Native GUI/Remote Access HTTP | Official Remote Access walkthrough enables the feature in configuration and displays the address; a restart may be needed. Its example uses 8081. [Walkthrough and transcript](https://strategyquant.com/blog/how-to-control-strategyquant-remotely-from-your-browser-or-laptop/) | [Native web adapter](../../product/tradercockpit/sqx_native_web.py) reads the installed `WebServerPortUsed` and `BrowserToken`, sends requests server-side, and keeps the token out of browser payloads. Do not hard-code the example port or treat “localhost” in prose as proof of the actual listener address. |
| Native WebSocket engine/task channels | No public wire contract was found in the programming/CLI guides or targeted official-document searches. The servlet API documents extension handlers, not task-manager frame schemas. [IServletPlugin](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/servlet/IServletPlugin.html) | [Engine progress](../../product/tradercockpit/sqx_engine_progress.py) uses `/main/getWebSocketPort`, engine and task-manager channels, and native chart metadata. Existing code is integration evidence; frame/content fidelity still needs real runtime readback. Missing channel values must stay unavailable. |
| Native MCP | May 2026 documentation lists `list_projects`, `list_strategies`, `list_databanks`, `get_strategy_stats`, `run_project`, and `stop_project`; `/mcp` shares Remote Access's port, default 8080 or next available. [MCP guide](https://strategyquant.com/doc/strategyquant/mcp-integration/) | No product MCP adapter was found in the inspected application seams. This documented first release is not full settings, snippet, trade-export, or robustness parity. Actual availability/authentication in the selected 144.2953 mode was not tested here. |

The Remote Access walkthrough advises local/VPN access rather than publishing the native service openly. This research changed no listeners, firewall rules, tokens, or connection settings. The programming guides do not establish a hosted multi-user service contract.

## Feature-to-seam map

| Native capability | Current frontend and backend seam | Current kind and practical gap |
|---|---|---|
| Builder, Retester, Optimizer module layout | [automation-workflows.mjs](../../web/automation-workflows.mjs), [sqx-modules.mjs](../../web/sqx-modules.mjs), `/api/sqx-module`, [sqx_run_module.py](../../product/tradercockpit/sqx_run_module.py) | `read-model` plus native control. Existing project archives drive Progress / Full settings / Results. Archive `ready` is not proof the web channels or executor are ready. |
| Native settings, task selection, configuration differences | [automation-full-settings.mjs](../../web/automation-full-settings.mjs), [automation-settings-controls.mjs](../../web/automation-settings-controls.mjs), `/api/sqx-project-settings`, [settings writer](../../product/tradercockpit/sqx_custom_project_settings.py) | `write`: existing XML attributes/text and exact selected task. Preserves native names; does not create every possible native task or implement its quantitative behavior. Whole native UI parity is not established by a generic field writer. |
| Approved Research Builder launch | [research-build.mjs](../../web/research-build.mjs), [research-build-launch.mjs](../../web/research-build-launch.mjs), [research_native_jobs.py](../../product/tradercockpit/research_native_jobs.py), [sqx_gateway.py](../../product/tradercockpit/sqx_gateway.py) | `write` / `native-run`: immutable compile/approval custody, same-source Task-rooted CFX, confirmed load, supervised start, durable receipts. A submitted job does not prove candidates, backtests, or robustness success. |
| Custom Project orchestration and module start/pause/resume/stop | [automation-workflows.mjs](../../web/automation-workflows.mjs), `/api/sqx-projects`, `/api/sqx-project-topology`, `/api/sqx-project-control`, [sqx_custom_project.py](../../product/tradercockpit/sqx_custom_project.py) | `read-model` / `native-run`: preserves numbered task identities and delegates control to SQX. Native project loops, optimization, and custom-analysis task semantics remain native. Full create/remove/reorder task editing is not provided by the inspected settings seam. |
| Research Candidate and historical Retester result | [research-candidates.mjs](../../web/research-candidates.mjs), [research-backtest.mjs](../../web/research-backtest.mjs), [research_candidates.py](../../product/tradercockpit/research_candidates.py), [research_retester.py](../../product/tradercockpit/research_retester.py) | `write` / `native-run`: imports exact producer artifacts; stages an isolated native Retester project; reads durable output. Native run acceptance is distinct from fixture assertions. |
| Native cross-checks and optimization | [research-backtest-robustness.mjs](../../web/research-backtest-robustness.mjs), [research_robustness.py](../../product/tradercockpit/research_robustness.py), native module settings/control above | Dedicated Research launch currently supports Higher Precision. Other present native profiles return `native_method_execution_not_wired`; absent profiles return their own refusal. A native Retester/Optimizer/Custom Project can execute its saved settings, but that does not establish separate Research custody launch/readback for every check. |
| Progress, chart types, task statistics, logs | `/api/sqx-project-progress`, `/api/sqx-engine-chart-selection`, [sqx_engine_progress.py](../../product/tradercockpit/sqx_engine_progress.py), [producer log reader](../../product/tradercockpit/sqx_custom_project_launch.py) | `read-model`; chart selection is `write`. The common dependency is a reachable native web/WS session. Do not infer generated/accepted/rejected counts from archive counts or logs lacking those values. |
| Databank strategy grid | [custom-project-results.mjs](../../web/custom-project-results.mjs), `/api/sqx-project-results`, [sqx_databank_grid.py](../../product/tradercockpit/sqx_databank_grid.py) | `read-model` of saved `.sqx` archives, stored settings/stats, and views. The broader official CLI load/save/sync/copy/move/clear/delete/export toolset is not a complete customer databank-management UI here. Unsaved in-memory results need native readback/save; disk presence is not the entire active databank. |
| Trades, equity, strategy settings comparison | [automation-results.mjs](../../web/automation-results.mjs), `/api/sqx-project-strategy`, [sqx_custom_project_strategy.py](../../product/tradercockpit/sqx_custom_project_strategy.py), [sqx_orders.py](../../product/tradercockpit/sqx_orders.py) | `read-model`; applying configuration writes overlapping existing fields. Stored orders and sample/result identity are the source. The official native trade-column extension mechanism is broader than this fixed presentation. |
| Native Overview and trades-on-chart | `/api/sqx-overview`, `/api/sqx-results-chart`, [sqx_results_overview.py](../../product/tradercockpit/sqx_results_overview.py), [sqx_results_chart.py](../../product/tradercockpit/sqx_results_chart.py) | Native HTTP `read-model` requests; may first load the exact saved archive into the native live databank. These are not purely disk reads. Chart data must have been stored and be available from the producer; the adapter bounds its returned bars. |
| Strategy source generation and EA export | [automation-results.mjs](../../web/automation-results.mjs), `/api/sqx-sourcecode`, [sqx_sourcecode.py](../../product/tradercockpit/sqx_sourcecode.py) | Native generation/read and `write` via `print`, `saveEA`, `getDataPath`, `saveMTPaths`. Needs native web session and native generator support. No general Java snippet editor/compiler or external-platform execution is implied. |
| Calibration, template choices, fitness types, installed data, symbol bars | [automation-workflows.mjs](../../web/automation-workflows.mjs), [sqx_calibrate.py](../../product/tradercockpit/sqx_calibrate.py), [sqx_settings_lists.py](../../product/tradercockpit/sqx_settings_lists.py) | Native `read-model`, settings `write`, and calibration `native-run`. Existing endpoints do not amount to a complete historical data downloader/importer or external-indicator editor. |
| Data manager and AlgoWizard | [sqx-modules.mjs](../../web/sqx-modules.mjs), [sqx_run_module.py](../../product/tradercockpit/sqx_run_module.py) | Explicit `unavailable` editor: `editor_wired: false`, with archive presence/absence inspection. Official native CLI/data/snippet capabilities exist but the full customer authoring/import workflow is not connected here. |
| CodeEditor, Java snippets, FreeMarker templates, custom libraries | Native CodeEditor and documented `user/extend`, `user/libs`; [packaged plugin boundary](../../product/tradercockpit/native_plugins/README.md) | No general product snippet editing/compilation, arbitrary JAR management, or indicator tester was found. Authoring helpers packaged in the catalog are not a replacement CodeEditor or an installed native capability by themselves. |
| Results extensions and native packages | `/api/capabilities`, [native_plugins](../../product/tradercockpit/native_plugins/__init__.py), `/api/sqx-results-plugin`, `/api/sqx-results-plugins`, [sqx_results_plugins.py](../../product/tradercockpit/sqx_results_plugins.py) | Bounded catalog/staging, native Results-plugin discovery/content access and creation exist. This does not expose every native plugin extension point or allow arbitrary plugins to rewrite TraderCockpit navigation. |

## Native extension contracts worth preserving

- Indicators use Java `OnBarUpdate`; signals use boolean `OnBlockEvaluate`; annotations describe parameters, outputs, and return types. Compilation is not semantic fidelity: the official tester compares indicator values with exports from the target platform using matching parameters/data. [Indicators/signals](https://strategyquant.com/doc/programming-for-sq/adding-indicators-and-signals/), [indicator testing](https://strategyquant.com/doc/programming-for-sq/testing-new-indicator-in-sq-x-vs-data-from-mt/).
- Snippet `compute()` and display-time `getValue()` are not interchangeable. Display-only columns cannot automatically become ranking filters or dependencies. Per-strategy custom analysis has its own true/false filter; per-databank analysis runs as a Custom Project task and can remove strategies omitted from its output. [Backtest settings in metrics](https://strategyquant.com/doc/programming-for-sq/using-backtest-settings-in-strategy-metrics/), [per-databank analysis](https://strategyquant.com/doc/programming-for-sq/example-per-databank-custom-analysis/).
- Programmatic backtest/optimization examples call SQX's own `BacktestEngine`/`OptimizationEngine` from native Custom Analysis. Optimization listeners may report the original retest and multiple later results. Robustness examples configure native cross-check plugins from native XML and invoke `runTest()`. These are extension mechanisms, not instructions to reproduce engines in Python/JavaScript. [Backtests](https://strategyquant.com/doc/programming-for-sq/running-strategy-backtests-programmatically/), [optimizations](https://strategyquant.com/doc/programming-for-sq/running-optimizations-programmatically/), [cross-check execution](https://strategyquant.com/doc/programming-for-sq/backtesting-strategy-programmatically-including-robustness-tests/).
- Native portfolio result merging merges backtest results/orders, not strategy executable logic. Native money-management, commission, What-If, trade-column and analysis-chart snippets are separate extension categories. [Portfolio merging](https://strategyquant.com/doc/programming-for-sq/merging-multiple-results-into-portfolio/).
- The guide documents Jython, external Python processes, and HTTP calls from native snippets. These examples establish possible native extension techniques; they do not justify adding a second product server or bypassing desktop worker supervision. [Python from SQ Java](https://strategyquant.com/doc/programming-for-sq/calling-python-from-sq-java-introduction/).

## Version and installed-document observations

- The [current download page](https://strategyquant.com/download) identifies Build **144.2953**, released **2026-05-20**. The owner-named installation is `C:/Users/MSI/Downloads/SQX_144_2953_win_20260601`; its inspected `internal/web/SQUANT/build.dat` contains `2953`. Directory naming is not independent proof of runtime trust.
- `sqcli.config`, `StrategyQuantX.config`, and `CodeEditor.config` are launcher/JVM option files. The inspected files contain JVM/network/GC options and `-Xms4g`; they do not document CLI commands or a web bind-address setting. No configuration was edited.
- `internal/extend/{Code,Snippets}`, `user/extend/{Code,Plugins,ResultsPlugins,Snippets}`, and `user/libs` exist. `internal/` also contains native web/Electron, libraries, templates and other runtime assets. Directory presence alone does not establish a supported application API. No binaries were extracted or decompiled for this research.
- `custom_indicators/` contains platform-specific helper folders including MetaTrader4, MetaTrader5, JForex and Tradestation. `VolumeProfile/` contains a native Custom Project `.cfx`, strategy template `.sqx` files and TPO examples. Only file placement was inspected; their quantitative behavior and import readiness were not tested or inferred.
- The installed [Extending_SQX.pdf](../../../SQX_144_2953_win_20260601/Extending_SQX.pdf), **page 1 of 1**, was text-extracted and visually inspected. It redirects readers to `https://strategyquant.com/codebase/`; it contains no offline programming chapters. The Spanish duplicate was not read.
- Documentation dates range from 2020 to 2026. Specific gates include CLI 127, JARs 130, task XML changes 136, history access 136 Dev 2, metrics-settings timing 136 Dev 5, custom tasks 138 with 140+ examples, and multi-value external indicators 142. Public Javadoc does not pin all signatures to installed 144.2953. Verify exact native behavior instead of copying old example version predicates.

## Reading coverage

The programming guide's HTML navigation contains **38 article URLs**, including entries omitted from the simplified browser extraction. All 38 article bodies were retrieved and their explanatory text reviewed. All **14 CLI navigation pages** were also retrieved and reviewed. Examples were read for their contract/purpose; downloadable `.sxp`, `.sqx`, `.cfx`, `.jar`, and ZIP implementations were not downloaded, imported, executed, or comprehensively code-audited. The inventories below distinguish guide reading from targeted API inspection.

No article fetch in these inventories failed. The lack of a documented WebSocket wire contract or bind-address option is a **documentation gap**, not a failed fetch and not proof the producer cannot support it. Unread scope is explicit after the inventories.

### Programming guide: 38 visited articles

| Source | Contract reviewed |
|---|---|
| [Envelopes indicator](https://strategyquant.com/doc/programming-for-sq/adding-envelopes-indicator-step-by-step/) | Multi-output indicator creation, native testing, and target-platform template calls. |
| [Adding indicators and signals](https://strategyquant.com/doc/programming-for-sq/adding-indicators-and-signals/) | Java indicator/signal lifecycle and annotated inputs/outputs. |
| [SQN InSample / OutOfSample ratio](https://strategyquant.com/doc/programming-for-sq/adding-new-databank-column-advanced/) | IS/OOS cross-sample display column; compile/restart and view selection. |
| [Adding databank column / filter](https://strategyquant.com/doc/programming-for-sq/adding-new-databank-column/) | Metric computation from orders or dependencies; column formatting. |
| [ForceIndex indicator](https://strategyquant.com/doc/programming-for-sq/adding-new-indicator-snippet-forceindex/) | Custom Java indicator creation; source language is not interchangeable with MQL. |
| [ATR Volatility Simple Sizing](https://strategyquant.com/doc/programming-for-sq/atr-volatility-simple-sizing/) | Native Money Management snippet and trade-size callback. |
| [Backtesting strategy programmatically including robustness tests](https://strategyquant.com/doc/programming-for-sq/backtesting-strategy-programmatically-including-robustness-tests/) | Native backtest plus configured cross-check plugins; results stay in ResultsGroup. |
| [Calling Python from SQ (Java) – Introduction](https://strategyquant.com/doc/programming-for-sq/calling-python-from-sq-java-introduction/) | Jython, supervised external-process concern, and HTTP invocation alternatives. |
| [Changing strategy parameters programmatically](https://strategyquant.com/doc/programming-for-sq/changing-strategy-parameters-programmatically/) | Apply selected optimization parameters through native helper. |
| [Changing task config programmatically](https://strategyquant.com/doc/programming-for-sq/changing-task-config-programmatically/) | Clone/edit/reapply task XML; separate UI refresh; Build136+. |
| [Chart – Accepted strategies per hour](https://strategyquant.com/doc/programming-for-sq/chart-accepted-strategies-per-hour/) | Engine chart snippet uses existing producer counters; compile/restart. |
| [ForceIndex Signal blocks](https://strategyquant.com/doc/programming-for-sq/creating-signals-blocks-based-on-indicators/) | Boolean signal blocks using an indicator; parameters and Shift. |
| [Equity moving average simulation](https://strategyquant.com/doc/programming-for-sq/equity-moving-average-simulation/) | What-If order-list filter; native equity-control simulation example. |
| [Example – per databank custom analysis](https://strategyquant.com/doc/programming-for-sq/example-per-databank-custom-analysis/) | Databank-wide analysis runs as native custom task; returned list can filter/remove. |
| [Example – per strategy custom analysis](https://strategyquant.com/doc/programming-for-sq/example-per-strategy-custom-analysis/) | Per-strategy analysis/filter, special cross-check storage, display-column timing. |
| [Example plugin – a complete custom project task](https://strategyquant.com/doc/programming-for-sq/example-plugin-a-complete-custom-project-task/) | Paired Task and Settings plugins; Java plus native AngularJS; 138/140+ examples. |
| [Exporting Strategy Data to a File Using a Template and Action Block](https://strategyquant.com/doc/programming-for-sq/exporting-strategy-data-to-a-file-using-a-template-and-action-block/) | AlgoWizard export action block activated for Retester; source-native file output. |
| [Filter by correlation – plugin example](https://strategyquant.com/doc/programming-for-sq/filter-by-correlation-plugin-example/) | Databank action plus Java servlet and native UI registration; versioned example. |
| [Import / Export custom indicators and other snippets](https://strategyquant.com/doc/programming-for-sq/import-export-custom-indicators-and-other-snippets/) | Custom .sxp import/export; compile and restart lifecycle. |
| [Introduction](https://strategyquant.com/doc/programming-for-sq/introduction-2/) | Plugins versus snippets; Java and FreeMarker Code categories; older scope. |
| [Calling another indicator from indicator snippet](https://strategyquant.com/doc/programming-for-sq/introduction-to-scripter/calling-another-indicator-from-indicator-snippet/) | Indicator composition using native cache, ChartData and typed output buffers. |
| [Loading history data in snippets](https://strategyquant.com/doc/programming-for-sq/loading-history-data-in-snippets/) | Native history access and protected-data refusals; Build136Dev2+. |
| [Logging and DebugConsole](https://strategyquant.com/doc/programming-for-sq/logging-and-debugcolsole/) | DebugConsole versus persistent native log; older Build128 caveat. |
| [Merging multiple results into portfolio](https://strategyquant.com/doc/programming-for-sq/merging-multiple-results-into-portfolio/) | Merge ResultsGroup results/orders into portfolio, not executable strategies. |
| [Minimum Commission Example](https://strategyquant.com/doc/programming-for-sq/minimum-comission-example/) | Commission callbacks on open/close and native snippet parameters. |
| [Public API Javadoc](https://strategyquant.com/doc/programming-for-sq/public-api-javadoc/) | Link to public Java API reference; not an HTTP API specification. |
| [Recognizing results in WF Matrix around custom field](https://strategyquant.com/doc/programming-for-sq/recognizing-results-in-wf-matrix-around-custom-field/) | Custom analysis of chosen WFM cell and surrounding native results. |
| [Running optimizations programmatically – update](https://strategyquant.com/doc/programming-for-sq/running-optimizations-programmatically/) | OptimizationEngine settings/listeners; separate140+ sample and fitness API caveat. |
| [Running strategy backtests programmatically](https://strategyquant.com/doc/programming-for-sq/running-strategy-backtests-programmatically/) | Native BacktestEngine settings, simulator precision, and result capture. |
| [Save databank results to DB](https://strategyquant.com/doc/programming-for-sq/save-databank-results-to-db/) | Native Custom Analysis writes selected databank view via JDBC; SQLite example. |
| [Selecting building blocks programmatically](https://strategyquant.com/doc/programming-for-sq/selecting-building-blocks-programmatically/) | Native block keys and use attribute; change existing task and reapply. |
| [Testing new indicator in SQ X vs data from MT](https://strategyquant.com/doc/programming-for-sq/testing-new-indicator-in-sq-x-vs-data-from-mt/) | Native indicator test against target-platform-exported values and exact parameters. |
| [Example – Trade Edge Ratio](https://strategyquant.com/doc/programming-for-sq/trade-edge-ratio/) | Native trade-list column getValue(Order) extension. |
| [Trade Analysis – Avg Edge Ratio by hour](https://strategyquant.com/doc/programming-for-sq/trader-analysis-avg-edge-ratio-by-hour/) | Native trade-analysis chart extension using recorded orders. |
| [Using backtest settings in strategy metrics](https://strategyquant.com/doc/programming-for-sq/using-backtest-settings-in-strategy-metrics/) | compute/getValue timing and filtering dependency distinction;136Dev5+. |
| [Using custom JAR libraries](https://strategyquant.com/doc/programming-for-sq/using-custom-jar-libraries/) | user/libs JAR loading after restart; target-platform translation still required. |
| [Viewing and changing strategy parameters – version 2](https://strategyquant.com/doc/programming-for-sq/viewing-and-changing-strategy-parameters-version-2/) | Versioned helper lists/changes strategy parameters; old import-order issue. |
| [Working with ResultsGroup](https://strategyquant.com/doc/programming-for-sq/working-with-resultsgroup/) | ResultsGroup, subresults, portfolio, native stats direction/unit/sample selection. |

### CLI guide: 14 visited articles

| Source | Contract reviewed |
|---|---|
| [-data Manage data](https://strategyquant.com/doc/cli-command-line/data-manage-data/) | Native update/import/export/clone/timezones; symbols must already exist. |
| [-databank Manage databanks](https://strategyquant.com/doc/cli-command-line/databank-manage-databanks/) | Native databank management, synchronization, copies/moves and view export. |
| [-deletefile Deletes the specific file](https://strategyquant.com/doc/cli-command-line/deletefile-deletes-the-specific-file/) | File deletion command exists; not executed or exposed by this research. |
| [-execute Calls external script](https://strategyquant.com/doc/cli-command-line/execute-calls-external-script/) | External executable/script command; not an unrestricted product tool. |
| [-exit Exit](https://strategyquant.com/doc/cli-command-line/exit-exit/) | Orderly exit of interactive CLI. |
| [-gui Starts webserver to access GUI remotely](https://strategyquant.com/doc/cli-command-line/gui-starts-webserver-to-access-gui-remotely/) | Explicit browser GUI webserver mode; example port8080. |
| [Importing Multiple External Indicator Values Using CLI Command.](https://strategyquant.com/doc/cli-command-line/importing-multiple-external-indicator-values-using-cli-command-2/) | Build142+ multiple external values; preserve exact types/data format and verify native import. |
| [-instrument Manage instruments](https://strategyquant.com/doc/cli-command-line/instrument-manage-instruments/) | Native instrument list/add/edit/delete and point/tick/cost fields. |
| [Introduction to CLI](https://strategyquant.com/doc/cli-command-line/introduction-to-cli/) | One-shot, interactive, command files, output redirection; versioned introduction. |
| [-project Manage projects](https://strategyquant.com/doc/cli-command-line/project-manage-projects/) | Project actions include load/save configuration and control. |
| [-run Runs commands from the file](https://strategyquant.com/doc/cli-command-line/run-runs-commands-from-the-file/) | Sequential commands from a file; native runner owns execution. |
| [-symbol Manage symbols](https://strategyquant.com/doc/cli-command-line/symbol-manage-symbols/) | Native symbol list/add/edit/delete/clear and data-source fields. |
| [-tools Tools](https://strategyquant.com/doc/cli-command-line/tools-tools/) | Native orders-to-CSV/XLSX tools; command options need current-help verification. |
| [-waitfor Waits for user/file](https://strategyquant.com/doc/cli-command-line/waitfor-waits-for-user-file/) | Wait for user/file; not a product scheduler contract. |

### Additional visited references

| Source | Review depth |
|---|---|
| [MCP integration](https://strategyquant.com/doc/strategyquant/mcp-integration/) | Complete article text and documented six-tool list; no connection made. |
| [Download/build notes](https://strategyquant.com/download) | Current build/release and runtime distribution sections; no download. |
| [Remote Access walkthrough](https://strategyquant.com/blog/how-to-control-strategyquant-remotely-from-your-browser-or-laptop/) | Article transcript reviewed; video not played. |
| [Public API overview](https://strategyquant.com/sqxapi/) | Package index reviewed; not every package/class traversed. |
| [DataSeries](https://strategyquant.com/sqxapi/com/strategyquant/datalib/DataSeries.html) | DataSeries type/indexing and method overview. |
| [HistoryDataLoader](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataLoader.html) | Complete short reference; get parameters, protected data and exception behavior. |
| [HistoryDataNotAvailableExeption](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryDataNotAvailableExeption.html) | Exception constructor/reference. |
| [HistoryOHLCData](https://strategyquant.com/sqxapi/com/strategyquant/lib/HistoryOHLCData.html) | Complete short reference; OHLCV arrays plus symbol/timeframe/session/range. |
| [DatabankColumn](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/DatabankColumn.html) | Class/method overview; native metrics and display extension roles. |
| [Directions](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Directions.html) | Direction constants overview; no new numeric mapping inferred. |
| [Order](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Order.html) | Order field/method overview; not a binary-format specification. |
| [OrdersList](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/OrdersList.html) | OrdersList overview and native filtering/cloning methods; no codec reimplementation. |
| [PlTypes](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/PlTypes.html) | P/L type constants and formatting overview. |
| [Result](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/Result.html) | Result overview; stats selection and cross-check result storage. |
| [ResultsGroup](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/ResultsGroup.html) | ResultsGroup overview; main/subresult/portfolio/orders/custom values. |
| [SQStats](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/SQStats.html) | SQStats overview and typed getters; missing-value semantics need native verification. |
| [SampleTypes](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/SampleTypes.html) | SampleTypes overview; no new numeric values inferred. |
| [IServletPlugin](https://strategyquant.com/sqxapi/com/strategyquant/tradinglib/servlet/IServletPlugin.html) | Complete short interface reference; Jetty getHandler contract. |
| [</> Codebase](https://strategyquant.com/codebase/) | Catalog/entry description and linked example context; attachments not audited. |
| [ATR Volatility Money Management Sizing](https://strategyquant.com/codebase/atr-volatility-money-mannagement-sizing/) | Catalog/entry description and linked example context; attachments not audited. |
| [Avg Edge Ratio by Hour](https://strategyquant.com/codebase/avg-edge-ratio-by-hour/) | Catalog/entry description and linked example context; attachments not audited. |
| [Detrended Price Oscillator (DPO)](https://strategyquant.com/codebase/detrended-price-oscillator-dpo/) | Catalog/entry description and linked example context; attachments not audited. |
| [What if – Equity Moving Average Trading Simulation](https://strategyquant.com/codebase/equity-moving-average-trading-simulation/) | Catalog/entry description and linked example context; attachments not audited. |
| [ForceIndex](https://strategyquant.com/codebase/forceindex/) | Catalog/entry description and linked example context; attachments not audited. |
| [Live coding sessions](https://strategyquant.com/codebase/live-coding-sessions/) | Catalog/entry description and linked example context; attachments not audited. |
| [Size Based Minimum Comission](https://strategyquant.com/codebase/size-based-minimum-comission/) | Catalog/entry description and linked example context; attachments not audited. |
| [External indicators](https://strategyquant.com/doc/strategyquant/external-indicators/) | Complete explanatory text; symbol/timeframe/history and target-code constraints. |
| [Edge Ratio background article](https://strategyquant.com/blog/edge-ratio-in-strategyquant-x/) | Fetched as a linked reference; not reviewed as a formula authority. |

Older `/doc/programming-sq/` aliases were also fetched and returned the same tutorial text:

- [Envelopes indicator](https://strategyquant.com/doc/programming-sq/adding-envelopes-indicator-step-by-step/)
- [Adding indicators and signals](https://strategyquant.com/doc/programming-sq/adding-indicators-and-signals/)
- [ForceIndex indicator](https://strategyquant.com/doc/programming-sq/adding-new-indicator-snippet-forceindex/)
- [ForceIndex Signal blocks](https://strategyquant.com/doc/programming-sq/creating-signals-blocks-based-on-indicators/)

### Explicit unread / unverified remainder

- Downloadable guide attachments and third-party libraries were not downloaded or executed. Embedded code examples were not compiled or exhaustively audited; this is capability research, not a verified extension implementation.
- The Javadoc package index links onward to more classes, inherited methods, constant-value tables and serialized forms. Only the targeted class/member references listed above were inspected; the entire API tree is not claimed read.
- The codebase index has additional entries and pagination, including newer authoring/Results extensions. The first page and the specifically linked entries above were read. The whole sharing catalog is not claimed reviewed or supported by the installed product.
- Outbound FreeMarker, MetaQuotes/MQL, TA4J, Jython, Python, JDBC, general trading explanations and community discussions were not treated as native SQX contracts. Their full documentation, forums and videos remain outside this bounded pass.
- Guide screenshots/videos were not exhaustively inspected. The local English PDF was inspected in full; the Spanish duplicate and arbitrary files under the installation were not.
- Targeted official searches did not reveal an HTTP5050 or WebSocket protocol specification with authentication, lifetime and address-binding guarantees. Forum results were not substituted for such a contract.
- Real producer mode activation, native calculations, filesystem changes made by native runs, extension compilation, platform exports and packaged desktop acceptance remain separate runtime work. This note creates no native execution or commercial-readiness claim.

### Reproduction / evidence

The guide URLs above were read on 2026-09-05 through browser retrieval and parallel read-only HTTPS retrieval of their article bodies. Local scratch extraction and URL inventories are under `.git/sqx-doc-research/`; these are disposable inspection artifacts, not runtime dependencies. The local PDF was extracted with `pypdf` and its single page rendered for visual inspection. Code mapping used read-only file inspection and `rg`; no code tests were needed for this documentation-only addition.

## Data setup automation — additional owner sources, 2026-09-05

The customer should select a trading source and instrument; the application should
populate technical settings from the producer and surface unresolved facts. Automatic
column detection does not establish timezone, broker identity, or contract economics.

- [SQX MT5 direct import](https://strategyquant.com/doc/quantdatamanager/metatrader5-data-import/)
  documents the build-144 native adapter for terminal bars and symbol metadata. Reuse
  that producer's conversions rather than recreating tick/point mathematics. Its update
  descriptions conflict about incremental import, so append behavior requires installed
  runtime evidence before being offered to customers.
- [SQX broker profiles](https://strategyquant.com/doc/strategyquant/broker-profiles/)
  carry broker-specific instruments, sessions, and timezone settings. The maintained
  MT5 export script supplies instrument/session XML. The guide warns that these settings
  do not remove provider price differences and that exported point values assume USD.
- [MetaQuotes bar API](https://www.mql5.com/en/docs/python_metatrader5/mt5copyratesfrom_py)
  defines UTC for this specific API and identifies available-history limits. That is
  not a contract for every broker CSV. [Symbol properties](https://www.mql5.com/en/docs/constants/environment_state/marketinfoconstants)
  distinguish point, tick size/value, contract size and spread; current spread is not a
  historical cost model.
- [TradingView time](https://www.tradingview.com/pine-script-docs/concepts/time/)
  distinguishes absolute UNIX timestamps, exchange timezone, and chart display timezone.
  [Chart export](https://www.tradingview.com/support/solutions/43000537255-how-to-export-chart-data/)
  depends on loaded chart coverage. [Chart information](https://www.tradingview.com/pine-script-docs/concepts/chart-information/)
  supplies feed/symbol metadata; it does not establish equivalence with a separate broker's
  instrument. Preserve exact feed, session, adjustments and timestamp representation.
- [SQX CLI data controls](https://strategyquant.com/doc/cli-command-line/data-manage-data/)
  expose import parameters. An explicit timestamp convention and source clock remain
  necessary when the source carries no clock metadata. No user's computer timezone or
  present broker offset establishes historical DST rules.

The supplied [Forex Data Settings video](https://vimeo.com/1162194873) could not be
inspected: the public player displayed “Unable to play media” and the skill downloader
received HTTP 401. Only its title/owner were observed; no video advice is attributed here.

Installed read-only evidence: `user/data/data.db` stores datasets, native instruments,
broker profiles and sessions. Native `DATA.DATATYPE` is the bar timestamp convention
(`1` start, `2` end), while `INSTRUMENTS.DATATYPE` is the asset class. Catalog settings
are observed saved configuration, not proof that a newly selected CSV came from that
provider. Data organization must keep these two evidence sources separate and retain
explicit missing/conflict states. Import, native task application, and backtest acceptance
remain separate mutations with their own source identity and readback requirements.

MT5 connection continuation (2026-09-05): the owner selected MetaTrader 5 terminal
and broker settings as the first automatic source. The installed 144.2953 adapter's
`loadAvailableSymbols` action invokes the bundled `mt5api.py` with portable mode;
its public discovery response contains symbol labels, not broker/account currency.
`symbol_price_info` emits native derived pricing values with broker overrides, so it
must not be treated as raw broker metadata or copied into an independent conversion.
The bundled Python runtime already contains MetaTrader5; no pip installation is needed.

The first direct connection reads raw terminal/broker/symbol metadata using that
installed package, under an explicit user action. MetaQuotes documents that
[initialize](https://www.mql5.com/en/docs/python_metatrader5/mt5initialize_py) can
launch a terminal when required; there is no attach-only flag. An already-running
terminal precondition and before/after identity checks reduce ambiguity but cannot
prevent a terminal closing during initialization. The UI must explain this behavior.
[Terminal info](https://www.mql5.com/en/docs/python_metatrader5/mt5terminalinfo_py)
and [account info](https://www.mql5.com/en/docs/python_metatrader5/mt5accountinfo_py)
are filtered to connection identity and broker/currency fields; account numbers,
names, balances and credentials are not product data for this setup step. Raw
tick values/current spread are observations, not a historical commission/slippage
model. No terminal API clock value establishes a historical broker DST policy.

Live authenticated continuation (2026-09-05): the logged-in MetaQuotes-Demo terminal
exceeded the bounded 4,096-symbol catalog. Data organization now accepts a literal
symbol search and uses MetaQuotes' documented
[symbols_get group filter](https://www.mql5.com/en/docs/python_metatrader5/mt5symbolsget_py).
Only the adapter adds the contains wildcards; user-supplied wildcard/exclusion/list
syntax is rejected. Limits stay unchanged and results are never silently truncated.
The observed filter is bound onto the returned source alongside the observation time.
An actual EURUSD read returned broker company/server/USD account currency and raw
symbol contract/tick fields. This resolves metadata connection acceptance, not native
bar import, broker-session/DST inference or backtest configuration application.

History continuation (2026-09-05): MetaQuotes documents
[copy_rates_range](https://www.mql5.com/en/docs/python_metatrader5/mt5copyratesrange_py)
as UTC bar-open timestamps with inclusive native endpoints. The product's date
selection uses an exclusive ending UTC day, translated explicitly at that API
boundary. Returned OHLC, volumes and spread remain producer values; a successful
read does not prove complete history because the terminal's available chart history
limits the response. Source UTC does not identify a historical broker session/DST rule.

Installed `internal/web/SQUANT/help.txt` confirms a supported headless import path:
create an instrument with `-instrument action=add`, create its dataset with
`-symbol action=add datasource=file`, then use `-data action=import` with explicit
timeframe, timezone and error handling. Each new target must have justified native
instrument economics, broker profile and cost settings. Never accept native defaults
as evidence of zero costs. Native catalog and exported OHLC readback must verify the
exact new dataset before applying it to a research task. This import/application
step remains unimplemented; captured MT5 history does not establish backtest readiness.
