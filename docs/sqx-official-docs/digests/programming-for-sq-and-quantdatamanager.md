# Technical digest: SQX "Programming for StrategyQuant X" + QuantDataManager docs

Scope: every file under `docs/sqx-official-docs/programming-for-sq/` (38 files) and `docs/sqx-official-docs/quantdatamanager/` (8 files). Only facts stated in those files are recorded. Where a requested topic is not covered by the files, this is stated explicitly. Source paths are relative to `docs/sqx-official-docs/`.

Not covered by any file in scope (do not infer from this digest): `OppositeBlocks`, calibration semantics, numeric values of `SampleTypes`/`Directions`/`PlTypes` constants (only XML attribute literals appear, see §6), QDM on-disk data folder layout, and the newer QDM/SQX CLI (Build 119+), which lives in the sibling folder `cli-command-line/` (out of scope here).

---

## 1. SQX extensibility model

### 1.1 Plugins vs snippets, CodeEditor, Code vs Snippets trees
- SQX was "built from scratch as an open, extendable platform"; most functionality is either a **plugin** or a **snippet**.
- **Plugin** = larger module with UI + background code. Example: the whole Builder screen; every settings tab and result tab inside it is itself a plugin. Plugin development is described as "very technical and complex" and only partly documented (see §4).
- **Snippet** = a short Java class implementing one function. Every Money-management model, indicator, building block and databank column is a snippet.
- Snippets are edited in the **CodeEditor** (icon top-right of SQX). The Navigator tree on the right has two top categories:
  - `Code` – Freemarker templates translating strategies from internal XML into target-platform code (MQL, EasyLanguage, …). A new indicator/signal also needs its translation template here.
  - `Snippets` – Java files, organised hierarchically by category.
- Source: `programming-for-sq/01-introduction/introduction-2.md`

### 1.2 Snippet kinds, base classes, on-disk folders and Java packages

| Snippet kind (Create-new type) | Base class | Folder shown in docs | Java package |
|---|---|---|---|
| Indicator | `IndicatorBlock` (`SQ.Internal.IndicatorBlock`) | `Snippets -> SQ -> Blocks -> Indicators/<Name>/<Name>.java` (one folder per indicator) | `SQ.Blocks.Indicators.<Name>` |
| Signal | `ConditionBlock` | same folder as its indicator (e.g. `SQ -> Blocks -> Indicators -> ForceIndex`) | `SQ.Blocks.Indicators.<Name>` (implied by location) |
| Action block (e.g. ExportData) | `ActionBlock` (`SQ.Internal.ActionBlock`) | not stated | `SQ.Blocks.OtherActions` |
| Databank column | `DatabankColumn` | not stated (created via Create new → Databank column) | `SQ.Columns.Databanks` |
| List of trades column | `TradelistColumn` | `User/Snippets/SQ/Columns/Trades` | `SQ.Columns.Trades` |
| Custom analysis (CA) | `CustomAnalysisMethod` | `User/Snippets/SQ/CustomAnalysis` | `SQ.CustomAnalysis` |
| Trade analysis chart | `TradeAnalysisChart` | `User/Snippets/SQ/TradeAnalysis` | `SQ.TradeAnalysis` |
| What-If | `WhatIf` | `User/Snippets/SQ/Whatif` (doc spelling) | `SQ.WhatIf` |
| Commissions | `CommissionsMethod` | clone from `Builtin/Snippets/Trading/Commissions` → appears in `User/Snippets/Trading/Commission` (doc spelling) | `SQ.Trading.Commissions` |
| Money management | `MoneyManagementMethod` | `User/Snippets/SQ/MoneyManagement` | `SQ.MoneyManagement` |
| Engine chart | `EngineChart` | `Snippets -> SQ -> Engine charts` (no wizard; clone `AverageStrategiesPerHour.java`) | `SQ.EngineCharts` |
| Helper classes | – | e.g. `SQ/Calculators/AverageCalculator.java`; helpers placed in `SQ.Utils` | `SQ.Calculators`, `SQ.Utils` |
| Trading options | `TradingOption` | visible in CodeEditor | `SQ.TradingOptions.*` (e.g. `ExitAtEndOfDay`, `ExitOnFriday`, `LimitTimeRange`, `MinMaxSLPT`) |

- Standard imports used by templates: `com.strategyquant.lib.*`, `com.strategyquant.datalib.*`, `com.strategyquant.tradinglib.*`.
- Note the doc's own inconsistencies: `Whatif` folder vs `SQ.WhatIf` package; `Trading/Commission` folder vs `Trading/Commissions` builtin folder and `SQ.Trading.Commissions` package. Treat the Java package names as authoritative for code; verify folder names on the installed runtime.
- Sources: `programming-for-sq/02-indicators-signals-step-by-step/*.md`, `05-custom-analysis/*.md`, `06-trade-analysis/trader-analysis-avg-edge-ratio-by-hour.md`, `07-what-if/equity-moving-average-simulation.md`, `08-trades-columns/trade-edge-ratio.md`, `09-commissions/minimum-comission-example.md`, `10-position-sizing/atr-volatility-simple-sizing.md`, `04-charts-step-by-step/chart-accepted-strategies-per-hour.md`, `11-coding-sessions-examples/exporting-strategy-data-to-a-file-using-a-template-and-action-block.md`

### 1.3 Compile / load cycle
- Toolbar: **Create new** (dialog: name + snippet type; for Signal type also "indicator the signal is based on"), **Compile**, **Compile all**, **Test indicators**, **Import/Export**, right-click **Clone**, right-click **Add all missing** (templates).
- After compile, **restart SQX** for new indicators/signals to appear in building blocks, new databank columns to appear in "Manage view", new MM/commission methods to appear in their tabs, new trade columns in the trades list, new engine charts in engine panels.
- Databank column usage: Databank → Manage view → create/edit view → add the column → select view.
- Sources: `02-indicators-signals-step-by-step/adding-envelopes-indicator-step-by-step.md`, `03-databanks-columns-filters-step-by-step/adding-new-databank-column-advanced.md`, `05-custom-analysis/example-per-strategy-custom-analysis.md`

### 1.4 Import / export of snippets and plugins
- CodeEditor top menu `Import/Export -> Export extensions` → select multiple snippets → file dialog → file gets **`.sxp`** extension. Only custom snippets can be exported (default set identical on all installs).
- `Import/Export -> Import extensions` → pick `.sxp` → all contained snippets imported → Refresh Navigator → **Compile all**.
- Plugins are also distributed as `.sxp` (the ZIP variants are "for convenience", not for import). Import via CodeEditor Import; plugin folders then appear; restart SQX.
- Order-dependency caveat: `StrategyParametersHelperV2` must be imported and compiled before `StrategyParameters` ("issue will be solved in the next SQ Build 136").
- `SQExtensionExportData2.sxp` is the distributed ExportData action-block snippet.
- Sources: `01-introduction/import-export-custom-indicators-and-other-snippets.md`, `13-plugins/example-plugin-a-complete-custom-project-task.md`, `11-coding-sessions-examples/viewing-and-changing-strategy-parameters-version-2.md`, `11-coding-sessions-examples/exporting-strategy-data-to-a-file-using-a-template-and-action-block.md`

### 1.5 Logging and DebugConsole
- `debug(category, message)` → DebugConsole (icon top-right, "debug icon"); filterable by category.
- `fdebug(category, message)` → standard log file in **`/user/log/StrategyQuant`** (also written `user/Log/StrategyQuant`).
- Both available in all snippet classes "in builds from 128 up"; in Build 128 they are missing in indicator classes → use `DebugConsole.log()` and `Log.info()`.
- Snippets also use SLF4J: `public static final Logger Log = LoggerFactory.getLogger(...)`, `Log.info/debug/error`.
- Sources: `01-introduction/logging-and-debugcolsole.md`, `08-trades-columns/trade-edge-ratio.md`

### 1.6 Custom JAR libraries
- "New and experimental functionality available from SQ X Build 130." Copy JARs into **`/user/libs`** under the SQX installation (create `/libs` under `/user` if missing), restart SQX.
- Example: `ta4j-core-0.13.jar` with an `RSIta4j` indicator (`org.ta4j.core.BarSeries`, `BaseBarSeries`, `RSIIndicator`, `ClosePriceIndicator`), using `OnInit()` and `Input.Time()` de-dup guard.
- Same folder is used for Jython (`jython-standalone-2.7.2.jar`) and JDBC drivers (MySQL/MariaDB; SQLite JDBC is already bundled).
- Caveat: a JAR-backed indicator is computed only inside SQX; target-platform code still needs a template plus a platform implementation.
- Sources: `01-introduction/using-custom-jar-libraries.md`, `12-sq-python/calling-python-from-sq-java-introduction.md`, `11-coding-sessions-examples/save-databank-results-to-db.md`

### 1.7 Public API Javadoc
- `https://strategyquant.com/sqxapi/` (e.g. `com/strategyquant/tradinglib/ResultsGroup.html`, `.../lib/HistoryDataLoader.html`, `.../tradinglib/servlet/IServletPlugin.html`).
- Source: `01-introduction/public-api-javadoc.md`

### 1.8 Other paths named in the files
- `/user/projects/<Your_Project>/project.cfx` — project archive; `.cfx` is a ZIP containing `config.xml` and one XML per task.
- `.sqx` strategy file — also a ZIP containing settings XML.
- `{SQ}/custom_indicators/MetaTrader4/Experts/SqIndicatorValuesExportEA.mq4` — MT4 EA to dump indicator values (file header names it `SQ_IndicatorValuesExportEA.mq4`).
- `{SQ installation}/tests/Indicators/MetaTrader4` — where Indicator Tester looks for CSVs (create if missing).
- `{SQ installation}/custom_indicators/MetaTrader4/Scripts/ExportProperties.mq4` — MT4 script exporting symbol properties to `{MT4 Data folder}/MQL4/Files/mt4.properties`.
- `{SQ installation}/user/extend/Plugins` (also written `user/extend/plugins`) — user plugins.
- `SQ X install folder\internal\electron\resources\userData` — UI cache to delete if a plugin does not appear.
- `MainApp.getDataPath()` (`com.strategyquant.lib.app.MainApp`) — runtime data path used to locate `sqpython.py`.
- Sources: `11-coding-sessions-examples/changing-task-config-programmatically.md`, `11-coding-sessions-examples/using-backtest-settings-in-strategy-metrics.md`, `02-indicators-signals-step-by-step/testing-new-indicator-in-sq-x-vs-data-from-mt.md`, `quantdatamanager/03-how-to/test-strategy-metatrader-4-tick-precision.md`, `13-plugins/*.md`, `12-sq-python/calling-python-from-sq-java-introduction.md`

---

## 2. Indicator / signal snippet anatomy

### 2.1 Class-level annotations
- `@BuildingBlock(name="(EP) Envelopes", display="Envelopes(#MA_Period#, #Deviation#)[#Shift#]", returnType = ReturnTypes.Price)` — `name` shown when choosing blocks; `display` shown in Wizard with `#Param#` placeholders; `returnType` governs what may be compared with what.
- `ReturnTypes` seen: `Price` (drawn on price chart: MA, Bollinger), `Number` (own pane: CCI, RSI, MACD), `PriceRange` (ATR), `Boolean` (signals), `Action` (action blocks).
- `@Indicator(oscillator=true, middleValue=100, min=96, max=104, step=0.1)` (Momentum) / `@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=0.5)` (RSI).
- `@Help("...")`, `@Description("...")`, `@SortOrder(100)`, `@CategoryOrder(400)`, `@IgnoreInBuilder` (ExportData is excluded from Builder generation).
- Non-block snippets (WhatIf, Commissions, MM) use `@ClassConfig(name="...", display="... #Param# ...")` instead of `@BuildingBlock`.
- Sources: `01-introduction/adding-indicators-and-signals.md`, `02-indicators-signals-step-by-step/adding-envelopes-indicator-step-by-step.md`, `11-coding-sessions-examples/exporting-strategy-data-to-a-file-using-a-template-and-action-block.md`, `07-what-if/...`, `09-commissions/...`, `10-position-sizing/...`

### 2.2 Field-level annotations
- `@Parameter(...)` attributes seen: `defaultValue="14"`, `minValue`, `maxValue`, `step`, `builderMinValue`, `builderMaxValue`, `builderStep`, `isPeriod=true`, `name="Method"`, `category="Default"|"Path"|"Value"`, `defaultChartIndex=0`.
  - `minValue/maxValue` = allowed range; `builderMinValue/builderMaxValue/builderStep` = optional narrower range/step Builder uses when generating; `isPeriod=true` marks period parameters so Builder's period min/max config applies; `defaultValue` = default.
- `@Editor(type=Editors.Selection, values="Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")` — combo box; also `values="Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6"`.
- `@Output(name="Upper", color=Colors.Green)` — output buffer, type `DataSeries`; multiple outputs allowed; order of `@Output` fields defines 0-based `Line` index.
- Input types: `DataSeries Input` (single price array) vs `ChartData Input`/`Chart` (whole chart: `.Open .High .Low .Close .Volume .Median .Typical`, `getSeries(int appliedPrice)`, `Time()`, `Time(shift)`, `Open(shift)` etc.). Rule: single price without applied-price choice → `DataSeries`; multiple prices or applied-price choice, or calling other indicators from an indicator → `ChartData`.
- `Shift`: hidden default parameter in every indicator; **in signals it must be added explicitly** (`@Parameter public int Shift;`). `Shift` is a reserved word (MQL `MA_Shift` was renamed `MA_Moved`).
- Sources: `02-indicators-signals-step-by-step/adding-envelopes-indicator-step-by-step.md`, `adding-new-indicator-snippet-forceindex.md`, `calling-another-indicator-from-indicator-snippet.md`, `creating-signals-blocks-based-on-indicators.md`

### 2.3 Required methods and runtime model
- Indicator: `protected void OnBarUpdate() throws TradingException` — called for every bar; compute only the latest value and store via `Value.set(0, v)` (or `Upper.set(v)`). Optional `protected void OnInit()`.
- `CurrentBar` = index of bar being computed (0 = first bar). Buffer access `Value.get(0)` current, `get(1)` previous; `getRounded(shift)` / `getRounded(shift, 4)`.
- Signal: `public boolean OnBlockEvaluate() throws TradingException`; typically `ForceIndex indicator = Strategy.Indicators.ForceIndex(Chart, Period, MAMethod, AppliedPrice); double v = indicator.Value.getRounded(Shift); return v > 0;`. Signal parameters must mirror indicator parameters (or be fixed).
- Action block: `public void OnAction() throws TradingException`.
- Indicator-from-indicator: `Indicators.SMA(MAInput, MA_Period).Value.get(MA_Moved)`, `Indicators.MA(series, Period, MAMethod)`, `Indicators.ATR(Input, ATRPeriod)`, `Indicators.RSI(Input.Close, RSIPeriod)`, `Indicators.DPO(Input, DPOPeriod)`; import concrete classes `SQ.Blocks.Indicators.ATR.ATR` etc. `Indicators` is a cache class of all indicators in a trading setup. Signals use `Strategy.Indicators.X(...)`.
- Helper: `SQ/Calculators/AverageCalculator.java` (`onBarUpdate(value, CurrentBar)`, `getValue()`).
- Sources: `01-introduction/adding-indicators-and-signals.md`, `02-indicators-signals-step-by-step/*.md`

### 2.4 Becoming selectable in Builder
- After Compile + restart, a new indicator appears in the **Random Indicators Signals** section of Building blocks (randomly combined with comparisons/numbers).
- Signals (created with type Signal, based on an indicator) appear after compile + restart as a **new section named after the indicator** in signals. Docs recommend signals over raw indicators (fewer degrees of freedom).
- Sources: `02-indicators-signals-step-by-step/adding-new-indicator-snippet-forceindex.md`, `creating-signals-blocks-based-on-indicators.md`

### 2.5 Strategy XML representation of a block and template functions
- Block XML: `<Item key="Envelopes" name="(EP) Envelopes" display="..." help="..." mI="Envelopes" returnType="price" categoryType="indicator">` with children `<Param key="#MA_Period#" name="MA _ Period" type="int" defaultValue="14" genMinValue="-1000003" genMaxValue="-1000004" paramType="period" controlType="jspinnerVar" minValue="2" maxValue="1000" step="1" builderStep="1">14</Param>`, `controlType="combo"`, `controlType="dataVar"` (`#Chart#`), `paramType="shift"` (`#Shift#`), auto-added `<Param key="#Line#" ... values="Upper=0,Lower=1">`. Comparison wrapper: `<Item key="IsGreater" ...><Block key="#Left#">…</Block><Block key="#Right#"><Item key="Number" ...>` .
- Templates: `Code/<Platform>/blocks/<Block>.tpl`, Freemarker, one line, they **call** the indicator (do not compute it). Right-click `.java` → **Add all missing** creates default templates for all platforms (`Code -> Pseudo code -> blocks -> Envelopes.tpl`). Default content: `Envelopes(<@printInput block true /> <@printParam block "#Param1#" />, <@printParam block "#Param2#" />, <@printShift block shift />)`.
- Macros: `<@printInput block true />`, `<@printParam block "#NAME#" />`, `<@printParamOptions block "#NAME#" "0=Simple,1=Exponential,…" />`, `<@printShift block shift />`.
- MT4 template examples: `iEnvelopes(<@printInput block />, <@printParam block "#MA_Period#" />, …, <@printParam block "#Line#" />+1, <@printShift block shift />)`; custom indicator: `iCustom(<@printInput block />, "Envelopes", …, <@printParam block "#Line#" />+1, <@printShift block shift />)` (MQL mode is 1-based, SQ `Line` is 0-based → `+1`).
- Source: `02-indicators-signals-step-by-step/adding-envelopes-indicator-step-by-step.md`

### 2.6 Indicator Tester (SQ vs MT4)
- Steps: run `SqIndicatorValuesExportEA.mq4` in MT4 Strategy Tester (≥1000 bars; one run per output buffer; file name should encode params, e.g. `Envelopes_14_0_0_0_0.1_upper.csv`); output to `{MT4 Data folder}/tester/files/<name>.csv`, `;`-delimited rows `time;Open;High;Low;Close;Volume;value`; copy to `{SQ installation}/tests/Indicators/MetaTrader4`; CodeEditor → **Test indicators** → **Add new test** → set **Test file name** and **Test parameters** → **Start**.
- Sources: `02-indicators-signals-step-by-step/testing-new-indicator-in-sq-x-vs-data-from-mt.md`, `adding-envelopes-indicator-step-by-step.md`

### 2.7 Other snippet APIs (non-block)
- DatabankColumn constructor: `super(name, DatabankColumn.Decimal2|Text, ValueTypes.Maximize|Minimize, target, avgMin, avgMax)`; `setWidth(80)`, `setTooltip`, `setDependencies("NetProfit","Drawdown")`, `printsSpecialValue(true)`; return `NOT_AVAILABLE` for N/A. Methods: `compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort[, Result result])` (7-arg form with `Result` only from Build 136 Dev 5, released 3 Nov 2022) — used for filtering/dependencies; `String getValue(ResultsGroup, String resultKey, byte direction, byte plType, byte sampleType)` — called at display time, text only, **cannot** be used in Ranking filters or other columns; `getNumericValue(...)`. Helpers `getPLByStatsType(order, combination)`, `round2`, `safeDivide`, `SQUtils.safeDivide`, `order.isBalanceOrder()`.
- TradelistColumn: `super("EdgeRatioTrade", Decimal2)`, `Object getValue(Order order)`; Order fields `PipsMAE`, `PipsMFE`, `ATROnOpen`, `Ticket`, `AccountBalance`, `PL`, `SetupName`, `IsInPortfolio`, `getTimeByPeriodType(tradePeriod)`.
- TradeAnalysisChart: `this.name = L.tsq(...)`; `AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod)`; chart types `BarChart`, `ScatterChart`, `PriceChart`; `chart.invertIfNegative(true)`, `chart.addValue(series, x, y)`.
- WhatIf: `void filter(OrdersList orders)`; build `new OrdersList("name")`, `sort(new OrderComparatorByOpenTime())`, `orders.replaceWithList(newOL)`.
- CommissionsMethod: `double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue)` / `computeCommissionsOnClose(...)`; `order.getSize()`. Appears in the **Data** section.
- MoneyManagementMethod: `double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue)`; `strategy.MarketData.Chart(symbol).Close(1)`, `strategy.getATRValue(chart, period, shift)`. Appears in **Money Management** tab.
- EngineChart: `super(L.t("..."), TYPE_PROJECT_BASED)`, `TimeSeriesLineChart`, `TimeSeries(name, 20)`, `ChartsConst.COLOR_GREEN`, `JSONObject print()` (`DATA_TYPE_CHART`), `addNextValue()`, `project.loadTrackingInfo(projectRunInfo,false)`, `ProjectRunInfo.strategiesPerHour / acceptedStrategiesPerHour / totalJobsDone`.
- Sources: `01-introduction/adding-new-databank-column.md`, `03-databanks-columns-filters-step-by-step/...`, `11-coding-sessions-examples/using-backtest-settings-in-strategy-metrics.md`, `08-trades-columns/...`, `06-trade-analysis/...`, `07-what-if/...`, `09-commissions/...`, `10-position-sizing/...`, `04-charts-step-by-step/...`

---

## 3. Programmatic control (Custom Analysis "coding session" examples)

### 3.1 CustomAnalysisMethod contract
- Constructor `super("Name", TYPE)`; types: `TYPE_FILTER_STRATEGY` (default, per strategy), `TYPE_PROCESS_DATABANK` (per databank, runs only from a **CustomAnalysis task in a custom project**), `TYPE_BOTH`.
- `boolean filterStrategy(String project, String task, String databankName, ResultsGroup rg)` — called per strategy before saving to databank (Builder/Retester Ranking tab or custom task); `false` dismisses.
- `ArrayList<ResultsGroup> processDatabank(String project, String task, String databankName, ArrayList<ResultsGroup> databankRG)` — returned list = strategies to keep (acts as filter); to add strategies use `Databank.add(rg, true)`.
- Custom values: `rg.specialValues().set(key, Object)`, `.getInt(key, -1)`, `.getString(key, NOT_AVAILABLE)`, `.setString`, `.getLong`, `.getDouble`, `.containsKey`.
- Access: `ProjectEngine.get(project)` → `SQProject`; `.getDatabanks().get("Results")` → `Databank`; `databank.getRecords()`, `.getView()` → `DatabankTableView`, `.add(rg, refreshGrid)`, `.remove(name, true,true,true,true,true,null)`, `.updateBestResults()`.
- Sources: `05-custom-analysis/example-per-strategy-custom-analysis.md`, `example-per-databank-custom-analysis.md`

### 3.2 Running a backtest programmatically (`CAStrategyTestByProgramming`)
- `ITradingSimulator simulator = new MetaTrader4Simulator();` alternatives: `MetaTrader5SimulatorHedging(OrderExecutionTypes.EXCHANGE)`, `MetaTrader5SimulatorNetting(OrderExecutionTypes.EXCHANGE)`, `TradestationSimulator()`, `MultiChartsSimulator()`, `JForexSimulator()` (package `com.strategyquant.tradinglib.simulator.impl.*`).
- `simulator.setTestPrecision(Precisions.getPrecision(Precisions.PRECISION_BASE_TF));` constants: `PRECISION_SELECTED_TF` ("Selected timeframe only (fastest)"), `PRECISION_BASE_TF` ("1 minute data tick simulation (slow)"), `PRECISION_TICK_CUSTOM_SPREADS`, `PRECISION_TICK_REAL_SPREADS`, `PRECISION_OPEN_PRICES` ("Trade On Bar Open"). Tick precision requires tick data.
- `BacktestEngine backtestEngine = new BacktestEngine(simulator); backtestEngine.setSingleThreaded(true); backtestEngine.addSetup(settings); ResultsGroup rg = backtestEngine.runBacktest().getResults();` (throws on error).
- `SettingsMap settings`: `ChartSetup("History", "EURUSD_M1", TimeframeManager.TF_H1, SQTimeOld.toLong(2008,4,20), SQTimeOld.toLong(2009,6,29), 3.5 /*spread*/, Session.Forex_247)` → `settings.set(SettingsKeys.BacktestChart, chartSetup)`; `SettingsKeys.StrategyObject` = `StrategyBase.createXmlStrategy(elStrategy.clone(), strategyName)`; `SettingsKeys.MinimumDistance` 0; `SettingsKeys.InitialCapital` 100000d; `SettingsKeys.MoneyManagement` = `MoneyManagementMethodsList.create("FixedSize", 0.1)` (or `create("RiskFixedPctOfAccount", 5, 100, 0.1, 0.5)` — snippet name + params in exact order); `SettingsKeys.TradingOptions` = `TradingOptions` list of `ExitAtEndOfDay{ExitAtEndOfDay,EODExitTime}`, `ExitOnFriday{ExitOnFriday,FridayExitTime}`, `LimitTimeRange{LimitTimeRange,SignalTimeRangeFrom=700,SignalTimeRangeTo=1900,ExitAtEndOfRange}`, `MinMaxSLPT{MinimumSL,MaximumSL,MinimumPT,MaximumPT}`.
- Symbol string "must match the name in Data Manager"; `"History"` first arg is constant.
- Reading result: `rg.portfolio().stats(Directions.Both, PlTypes.Money, SampleTypes.FullSample).getInt(StatsKey.NUMBER_OF_TRADES)` / `.getDouble(StatsKey.NET_PROFIT)`.
- Source: `11-coding-sessions-examples/running-strategy-backtests-programmatically.md`

### 3.3 Backtest + cross checks programmatically
- Cross checks are plugins implementing `ICrossCheck`; obtain via `SQPluginManager.getPlugins(ICrossCheck.class)`, match `getSettingName()`, `clone(null)`, then `readSettings(Element, null)`; `setStopPauseEngine(new StopPauseEngine())`; run `crossCheck.runTest(mainResultRG, i, globalATR, null, true, null, strategyName)` → boolean pass.
- Config XML: save Builder/Retester project to `.cfx` (ZIP) → `config.xml` → copy the wanted child of `<CrossChecks>` to its own file; load with `XMLUtil.fileToXmlElement(new File(path))`; root element name = plugin setting name.
- Root elements/structures shown: `<RetestWithHigherPrecision use="true"><Settings><Precision>2</Precision><Spread>3</Spread></Settings><AcceptanceSettings><Conditions CrossCheck="RetestWithHigherPrecision"><Condition use="true"><Left-Side valueType="column"><Column-Value column="NetProfit" columnType="0" format="Decimal2PL" resultType="RetestWithHigherPrecision" direction="0" sampleType="127" plType="10" confidenceLevel="50" market="1" subresult="30" pctRatio="0" class="NetProfit"/></Left-Side><Comparator value="&gt;="/><Right-Side valueType="column"><Column-Value ... resultType="main" ... pctRatio="80" .../></Right-Side></Condition>…`.
- `<MonteCarloManipulation use="true"><Settings><Methods><Method use="true" type="RandomizeTradesOrder"><Params><Param key="Method" type="String">resampling</Param></Params></Method><Method use="true" type="RandomlySkipTrades"><Params><Param key="Probability" type="Integer">10</Param></Params></Method></Methods><NumberOfSimulations>10</NumberOfSimulations><MCUseFullSample>false</MCUseFullSample></Settings><AcceptanceSettings><Conditions/></AcceptanceSettings></MonteCarloManipulation>`.
- `<MonteCarloRetest>` method types: `RandomizeHistoryData` (ProbabilityUp, MaxChangeUp, ProbabilityDown, MaxChangeDown, KeepConnected), `RandomizeHistoryDataFixedRange` (MaxChange), `RandomizeMinDistance` (Min, Max Double), `RandomizeSlippage`, `RandomizeSpread`, `RandomizeStartingBar` (MaxChange), `RandomizeStrategyParameters` (Probability, MaxChange, Symmetric), `RandomizeStrategyParametersCustom` (Probability, MaxChange, Periods, Constants, Shifts, OtherParams, EntryLevels, EntryLogic, ExitParamsUsed, ExitParamsUnused, BooleanParams, TradingOptions, Symmetric); plus `<NumberOfSimulations>`, `<MCUseFullSample>`, `<MCBacktestPrecision>-1</MCBacktestPrecision>`.
- Source: `11-coding-sessions-examples/backtesting-strategy-programmatically-including-robustness-tests.md`

### 3.4 Running optimizations programmatically (`CAOptimizationByProgramming`)
- Versions: "_sqx140" variant for 140+, "_136" for older. `Parameter` constructor not public before Build 136 (reflection hack shown). Build 136 Dev extended `IFitnessFunction` (add `getMetricsForFitness()`, `getMetricValue(...)`, `clone()`).
- Flow: `SettingsMap settings = prepareSettings(...)`; `ProgressEngine progressEngine = new ProgressEngine("name")`; `OptimizationEngine engine = new OptimizationEngine("CustomCAOptimization", progressEngine, null, "OptimTask", null); engine.initialize(settings); engine.setNewResultListener(INewResultListener{newResult(ResultsGroup)}); engine.setLogListener(IEngineLogListener{processMessage}); engine.setStepsListener(IStepsListener{step(int)}); engine.optimize();` (blocking). Listener fires first for retest of the original strategy, then per result (Simple) or once (WF/WFM).
- Settings keys used: `SettingsKeys.TestPrecision` = `Precisions.SelectedTF`, `StrategyXml`, `StrategyObject` (`StrategyBase.createXmlStrategy(el)` + `transformToVariables(symmetry, paramTypes)`), `StrategyName`, `FitnessFunction`, `TradingOptions` (`.getClone()`), `ATM` null, `InitialCapital` 100000d, `Slippage` 0d, `MinimumDistance` 0d, `BacktestChart`, `ChartSetups` (`ChartSetups.add(chartSetup)`), `OptimizationSettings`, `DateGenerated` (from `SpecialValues.DateGenerated`). `chartSetup.setTestPrecision(...)`, `chartSetup.setBacktestEngine(Engines.MetaTrader4)`.
- `ParametersSettings`: `paramTypes.set(ParametrizationTypes.ParamTypeRecommended, true)`, `symmetry`, `params` (`OptimizationParams.addParam(Parameter)`), `distributionUp/Down = 20`, `maxSteps = 20`, `manualMode`. Skip `MagicNumber` and signal variables (`<signal variable="...">` elements via `XMLUtil.getNestedElements(el, "signal")`). `Parameter(name, type, true, start, stop, step, original)`; `Variable` types `TypeDouble/TypeInt/TypeTime/TypeBoolean`; `ParametrizationTypes.ParamTypePeriod/ParamTypeOtherParam/ParamTypeTradingOptions`.
- `OptimizationSettings("CustomCAOptimization")`: `type` = `OptimizationTypes.Simple | WalkForward | WalkForwardMatrix`; `optimizationMethod` = `OptimizationMethods.BruteForce | GeneticOptimization`; `optimizationType = OptimizationConst.WF_TYPE_SIMIS_EXACTOOS`; `maxOptimizationBacktests = 1000`; `parameters`; `dontSaveOriginalStr`; `periodInPercent = true`; `param1Start/Stop/Step` (20/40/10 = OOS %), `param2Start/Stop/Step` (5/20/5 = runs; "must be set properly" even for WF).
- Fitness: custom `SQ.Utils.FitnessFromStrategyResultForCA implements IFitnessFunction` ("NetProfit" or "Weighted"; reads `<Ranking type=...><Goal use= type= weight= valueType= target=/>`; `getFitnessKey()` returns `ComputeFromStrategyResult`; `getProduct()` "SQUANT"; uses `DatabankColumns.get().findClassByName(name)`, `c.getNumericValue(...)`, `c.transformToFitnessRange(val,target,valueType)`, trade-count penalties <20→×0.3, <30→×0.4, <50→×0.6, <70→×0.8, <100→×0.85, <150→×0.9; `StatsComputer.getAllDependentStatValues`).
- Result handling: `SQProject.isMemoryProtectionUsed()` → `MemoryUsageChecker.checkAvailableMemory()`; `newRG.removeUnsavableSettings()`; `targetDatabank.add(newRG, true)`; `newRG.clear()` on error.
- Source: `11-coding-sessions-examples/running-optimizations-programmatically.md`

### 3.5 Changing task / project config programmatically (Build 136+)
- `SQProject sqProject = ProjectEngine.get(project); ISQTask buildTask = sqProject.getTaskByName("Build strategies");` (or `sqProject.getTasks()`).
- `Element elConfig = buildTask.getConfig();` (JDOM, "SQ 136 up") → **must clone**: `Element elNewConfig = elConfig.clone();` → modify → `buildTask.setConfig(elNewConfig, true);`.
- Date range: `XMLUtil.getChildElem(elNewConfig, "Data")` → `.getChild("Setups")` → `.getChildren("Setup")` → first `<Setup>` attributes `dateFrom`, `dateTo` formatted `yyyy.MM.dd` (`SQTime.formatDate(SQTime.toLong(2007,1,1), DateTimeFormat.forPattern("yyyy.MM.dd"))`).
- Alternative: load whole task XML from file `XMLUtil.fileToXmlElement(new File("c:/BuildTask2009_2019.xml"))`.
- UI refresh: `SQWebSocketManager.addToDataQueue(new DataToSend(WebSocketConst.UpdateProject, new JSONObject().put("projectConfig", sqProject.toJSON())), SQConst.CODE_TASKMANAGER);` (without it the change works but is not visible).
- Task XML structure viewable in `/user/projects/<Project>/project.cfx` (ZIP, one XML per task).
- Source: `11-coding-sessions-examples/changing-task-config-programmatically.md`

### 3.6 Selecting building blocks programmatically
- Blocks live under `<Blocks> -> <BuildingBlocks> -> <Block key="..." use="true|false">`. Key conventions: signal/comparison = snippet name (`ADXChangesDown`, `IsGreater`); indicator = `Indicators.ADX`; price = `Prices.High`; stop/limit level = `Stop/Limit Price Levels.TEMA`; stop/limit range = `Stop/Limit Price Ranges.BBWidthRatio`. Bollinger signals prefixed `BB…` (`BBBarClosesAboveDown`), Keltner `KC…` (`KCBarClosesAboveLower`).
- Enable/disable = set `use` attribute; then `setConfig` + WebSocket refresh as above. If `<Blocks>` missing, task is not a Builder task.
- Source: `11-coding-sessions-examples/selecting-building-blocks-programmatically.md`

### 3.7 Changing / reading strategy parameters
- Helper `SQ.Utils.StrategyParametersHelper.setParameters(ResultsGroup rg, String "A=1,B=2.5", boolean symmetricVariables, boolean modifyLastSettings)`: `StrategyBase.createXmlStrategy(rg.getStrategyXml())`, `transformToVariables(symmetric)`, `variables().get(i).setFromString(...)`, `transformToNumbers()`, `rg.portfolio().addStrategyXml(xmlS.getStrategyXml())`, `rg.specialValues().setString(StatsKey.OPTIMIZATION_PARAMETERS, parameters)`; last-settings path `Options/BuildTradingOptions/Params/Param[@className,@key]` via `TradingOptionsList.getInstance().getAvailableClasses()`, `IPGParameter`, `ParametersTableItemProperties.TYPE_TIME` (HHMM → `SQTime.HHMMToMinutes(v)*60`); `rg.setLastSettings(XMLUtil.elementToString(el))`.
- WF source: `WalkForwardMatrixResult mwf = (WalkForwardMatrixResult) rg.mainResult().get(SettingsKeys.WalkForwardResult); WalkForwardResult best = mwf.getWFResult(rg.getBestWFResultKey(), false); WalkForwardPeriod last = best.wfPeriods.get(last); last.testParameters` (comma-separated `key=value`). Symmetry check: `ParametersSettings.setFromXML(lastSettingsEl, rg.getStrategyXml()).symmetry`.
- V2 `StrategyParametersHelperV2` (`ValueDelimiter "="`, `ParamDelimiter ","`): `setParameters`, `getParameterNames(rg, ValuesMap parameterTypes, symmetric)`, `getParameterValues(...)` → `HashMap<String,String>`, `getParameterValue(rg, symmetric, name)`, `toString(map)`. Skips `LongEntrySignal`, `ShortEntrySignal`, `LongExitSignal`, `ShortExitSignal`, `MagicNumber`. `ParametrizationTypes` keys: `ParamTypeRecommended, ParamTypePeriod, ParamTypeShift, ParamTypeConstant, ParamTypeOtherParam, ParamTypeEntryLevel, ParamTypeEntryLogic, ParamTypeExitUsed, ParamTypeExitUnused, ParamTypeBoolean, ParamTypeTradingOptions` (recommended default = Period + EntryLevel + ExitUsed).
- Sources: `11-coding-sessions-examples/changing-strategy-parameters-programmatically.md`, `viewing-and-changing-strategy-parameters-version-2.md`

### 3.8 Merging results into a portfolio
- Manual: `new ResultsGroup("PortfolioMadeByCA")`; per strategy: `Result main = rg.mainResult(); key = rg.getName()` (unique via `SQUtils.generateUniqueName(key, IUniqueNameChecker)`), `new Result(key, portfolioRG, main.getSettings().clone())`, `portfolioRG.addSubresult(key, settings, result)`, `mergedResult.setString(SpecialValues.Symbol/Timeframe, ...)`, date bounds `SpecialValues.HistoryFrom/HistoryTo`, `PortfolioHistoryFrom/To`, `SettingsKeys.PortfolioDataStart/End`; orders: `rg.orders().filterWithClone(mainKey, Directions.Both, SampleTypes.FullSample)`, set `order.SetupName = key` (and `IsInPortfolio = 1` in WF variant), `portfolioRG.orders().add(order)`; `portfolioRG.symbols().add(rg.symbols())` (needed for PL). Then `new Result(ResultsGroup.Portfolio, rg, settings)`, `removeSubresult(ResultsGroup.Portfolio, true)`, `addSubresult(...)`, `specialValues().setString(SpecialValues.Symbol, ResultsGroup.Portfolio)`, `orders().sort(new OrderComparatorByOpenTime())`, `portfolioResult.computeAllStats(rg.specialValues(), rg.getOOS())`, `rg.updated = true`.
- Standard: `ResultsGroup.merge(databankRG, new String[]{ResultsGroup.AdditionalMarket}, null).createPortfolioResult(PortfolioInitialBalanceTypes.SINGLE, null)`. Default `createPortfolioResult()` ignores WF results (the WF-portfolio example reimplements it); `rg.getBestWFResultKey()`.
- Sources: `11-coding-sessions-examples/merging-multiple-results-into-portfolio.md`, `05-custom-analysis/example-per-databank-custom-analysis.md`

### 3.9 Save databank to DB
- JDBC; SQLite bundled (`Class.forName("org.sqlite.JDBC")`, `jdbc:sqlite:C:/SQDatabankExports/<table>.db`); other drivers → `{SQ}/user/libs`. Uses `DatabankTableView view = databank.getView()`, `view.columns` (`DatabankTableColumnEntry.tableColumn`), `column.tableColumn.exportValue(rg, null, Directions.Both, PlTypes.Money, SampleTypes.FullSample)`; table name `yyyy-MM-dd-HHmm__project__task__databank`, SQL table literal `databankData`, PK `strategyName`.
- Source: `11-coding-sessions-examples/save-databank-results-to-db.md`

### 3.10 Loading history data in snippets (Build 136 Dev 2+)
- `HistoryDataLoader loader = new HistoryDataLoader(); HistoryOHLCData data = loader.get("EURUSD_M1", TimeframeManager.TF_H1, SQTime.toLong(2007,1,1), SQTime.toLong(2020,12,31), Session.NoSession);` wrap in `catch(HistoryDataNotAvailableExeption e)` (sic). Arrays: `data.Time[] (long)`, `Open/High/Low/Close/Volume[] (float)`. Slow (seconds) — not for databank columns. Not available for protected SQ Futures / SQ Equities data.
- From a strategy: `rg.mainResult().getString(SpecialValues.Symbol, "N/A")`, `SpecialValues.Timeframe`, `rg.specialValues().getLong(SpecialValues.HistoryFrom/HistoryTo)`.
- Source: `11-coding-sessions-examples/loading-history-data-in-snippets.md`

### 3.11 Backtest settings in metrics
- `rg.getLastSettings()` → String → `XMLUtil.stringToElement` → `<Data><Setups detailed="true"><Setup dateFrom="2003.5.5" dateTo="2018.08.30" testPrecision="1" session="No Session" slippage="1" minDist="0" engine="Stockpicker"><Chart symbol="EURUSD_M1" timeframe="H1" spread="2"/><Commissions>…`. Access `elSettings.getChild("Data").getChild("Setups").getChild("Setup").getChild("Chart").getAttributeValue("spread")`. Cache in `specialValues()`. `compute(...)` 7-arg with `Result result` → `result.getResultsGroup()` only from Build 136 Dev 5 (3 Nov 2022); before that only display-time `getValue()`.
- Source: `11-coding-sessions-examples/using-backtest-settings-in-strategy-metrics.md`

### 3.12 Export strategy data via template + ExportData action block
- Import `SQExtensionExportData2.sxp`, restart. AlgoWizard template with rule "Export Data" using block `(EXP) ExportData` (`SQ.Blocks.OtherActions.ExportData extends ActionBlock`, `@IgnoreInBuilder`): params `Chart`, `Shift`, `Path1` (default `C:\Temp\output.txt`), `Value1..Value5` (default `999999` = ignored). Writes CSV line `yyyymmdd,HHMM,O,H,L,C,V[,Value1..]` appended (`FileWriter(path,true)`). Boolean template variable `ExportData` toggled to `True` in Retester to enable export; Builder runs with it off.
- Source: `11-coding-sessions-examples/exporting-strategy-data-to-a-file-using-a-template-and-action-block.md`

### 3.13 WF Matrix neighbour check
- `WalkForwardMatrixResult wfm = (WalkForwardMatrixResult) rg.mainResult().get(SettingsKeys.WalkForwardResult)`; `wfm.getWFResult(param1 /*OOS %*/, param2 /*runs*/)`; matrix bounds `wfm.start1/stop1/increment1` (param1) and `start2/stop2/increment2` (param2); `WalkForwardResult.param1`, `.param2`, `.passed`, `.wfPeriods`.
- Source: `11-coding-sessions-examples/recognizing-results-in-wf-matrix-around-custom-field.md`

---

## 4. Plugins

### 4.1 Plugin vs snippet; enablement
- User plugin development possible from **Build 136 Dev 2** (previously needed full source). SQX UI = AngularJS web app; backend = Java servlets. Plugins live in `{SQ installation}/user/extend/Plugins`; CodeEditor shows a **Plugins** section; right-click plugin folder → **Compile plugin** → `<Name>.jar` in folder → restart SQX. If plugin doesn't show, delete `SQ X install folder\internal\electron\resources\userData` and restart. "Updated version for SQX Build 140 and above" exists for both examples.
- Sources: `13-plugins/filter-by-correlation-plugin-example.md`, `example-plugin-a-complete-custom-project-task.md`

### 4.2 Filter-by-correlation databank-action plugin (simple)
- Files: `FilterByCorrelationServlet.java`, `module.js`, `popup.html`.
- `module.js`: `angular.module('app.resultsdatabankactions.filterByCorrelation', ['sqplugin']).config(function(sqPluginProvider, $controllerProvider){…})`; `new CustomPluginController(moduleName, $controllerProvider, callback).init()`; `sqPluginProvider.plugin("ResultsDatabankAction", 100, {title: Ltsq("Tools:Filter by correlation"), class:'btn btn-normal btn-default', controller, id:"databank-action-filterbycorrelation"})` (`Tools:` prefix nests under Tools button); `sqPluginProvider.addPopupWindow("plugins/FilterByCorrelation/popup.html", null, 'SQUANT')`; callback signature `(projectName, taskName, databankName, selectedStrategies)`; `window.parent.showPopup("#id")` / `hidePopup`.
- `popup.html`: modal id `filterByCorrelationButtonModal`; period select values `5=Hour, 10=Day, 20=Week, 30=Month`; `CustomPluginController.sendRequest("filterByCorrelation/filter?"+params, "GET", null, cb)`.
- Servlet: `@PluginImplementation public class FilterByCorrelationServlet implements IServletPlugin` — `getProduct()` returns `SQConst.CODE_SQ`, `getPreferredPosition()`, `initPlugin()`, `getHandler()` returning `ServletContextHandler(ServletContextHandler.SESSIONS)` with `setContextPath("/filterByCorrelation/")` and `addServlet(new ServletHolder(new FilterByCorrelation()), "/*")`. Inner `class FilterByCorrelation extends HttpJSONServlet { String execute(String command, Map<String,String[]> args, String method) }`, `apiErrorJSON`, `tryGetParam`. Correlation API: `CorrelationTypes.getInstance().findClassByName("ProfitLoss")`, `CorrelationComputer.computeCorrelation(false, name1, name2, orders1, orders2, CorrelationPeriods, CorrelationType)`, `CorrelationLib.getPeriod(orders)`, `CorrelationLib.generatePeriods(period, from, to)`, `TimePeriods`, `rg.getFitness()`.
- Source: `13-plugins/filter-by-correlation-plugin-example.md`

### 4.3 Complete custom project task plugin (Build 138+)
- Two interdependent plugins: **`TaskFilterByCorrelation`** (task + simple settings on Progress tab) and **`SettingsFilterByCorrelation`** (full settings tab). Mandatory packages `com.strategyquant.plugin.Task.impl.XXXXX` / `com.strategyquant.plugin.Settings.impl.XXXXX`; folder names `TaskXXXXX` / `SettingsXXXXX`. Steps: copy examples, rename, delete shipped `.jar`s, implement Java + JS/HTML.
- Settings plugin files: `FilterByCorrelationCtrl.js` (Angular controller; `settingsChanged()` → `FilterByCorrelationService.saveSettings()`; listens to `SETTINGS_TAB_RELOAD` → `init()` → `loadSettings()`), `FilterByCorrelationService.js` (singleton; `AppService.getCurrentTaskTabSettings("FilterByCorrelation")`, `getNodeFloatValue(obj,'CorrMax',0.3)`, `addNode('CorrMax', v, obj, xmlDoc)`; shared with Task plugin), `module.js` (registers `SettingsTab` plugin with `task`, `configElemName`, `templateUrl`, `controller`, `dataItem` unique; optional `title`, `help`, `helpUrl`; `task: 'FilterByCorrelation,Build,Retest'` to show for multiple tasks), `settings.html`, `styles.css`, `SettingsFilterByCorrelation.java implements ISettingTabPlugin` with `readSettings(String projectName, ISQTask task, Element elSettings, TaskSettingsData data)` using `XMLUtil.getChildElem(elSettings, getSettingName())`, `ProjectConfigHelper.getDatabankByType(projectName, "Source"|"Target"|"Existing", elSettings)`, `data.addParam(SettingsKeys.DatabankSource/DatabankTarget, db)`, `data.addParam("DatabankExisting", db)`, `CorrelationSettings.loadFromXml`, `data.addParam(SettingsKeys.Correlation, corr)`, `data.addError(...)`.
- Task plugin files: `module.js` (`sqPluginProvider.plugin("SimpleTaskSettings", 1, {taskType:'FilterByCorrelation', templateUrl:'../../../plugins/TaskFilterByCorrelation/simpleSettings/simpleSettings.html', controller:'SimpleFilterByCorrelationCtrl', getInfoPanels(xmlConfig, injector)})`), `simpleSettings/` (`SimpleFilterByCorrelationCtrl.js`, `simpleSettings.html`), `styles.css`, **`task.xml`** (default settings: 3 databanks Source/Target/Existing + correlation settings; databanks auto-created), `TaskFilterByCorrelation.java extends AbstractTask` with `start()` → `loadSettings()`, `progressEngine.setLogPrefix(taskLogPrefix)`, `.printToLog`, `.start()`, `.printToLogDebug`, `.checkPaused()`, `.isStopped()`, `.finish()`; `databankSource.getRecords()`, `strategy.clone()`, `databankTarget.add(clone, true)`, `increaseGlobalStats(bool)`, `project.onMemoryError(e)`, `databankTarget.updateBestResults()`.
- Source: `13-plugins/example-plugin-a-complete-custom-project-task.md`

---

## 5. Python bridge
- Three methods, all demonstrated as CA snippets storing output in `rg.specialValues().set("PythonOutput", result)` read by a `TestPythonOutput` databank column (`getValue` → `rg.specialValues().getString("PythonOutput", NOT_AVAILABLE)`).
- **Jython**: put `jython-standalone-2.7.2.jar` in `{SQ installation}\user\libs`, restart; `ScriptEngineManager.getEngineByName("jython")`, `engine.eval(new FileReader(MainApp.getDataPath()+"sqpython.py"), context)` with `SimpleScriptContext.setWriter(StringWriter)`. Pros: in-JVM, no external process. Cons: not all CPython packages, no longer actively developed, older Python version.
- **External Python**: `new ProcessBuilder("py", scriptPath).redirectErrorStream(true).start()`, read stdout, `waitFor()`. Requires Python installed on the machine; any libraries usable.
- **HTTP**: Python `python -m http.server 9000` (or Flask/Django); Java `HttpURLConnection` GET `http://localhost:9000`, strip tags. Best for large data (GET/POST + response).
- Parameter passing: HTTP is simple; for Jython/process use script args for small data, temp files for large data both directions.
- CA runs per strategy after retest (Retester Ranking tab) or as custom task; press Start to trigger.
- Source: `programming-for-sq/12-sq-python/calling-python-from-sq-java-introduction.md`

---

## 6. ResultsGroup
- `ResultsGroup` (`com.strategyquant.tradinglib`) stores: strategy code/XML, backtest results, optimization/cross-check results, settings used in last backtests, key-value custom store. One databank row = one ResultsGroup; `getName()` = strategy name (e.g. "Strategy 1.2.3").
- Group of `Result` objects: `subResult(resultKey)`, `getResultKeys()` (List), `getMainResultKey()`, `mainResult()`, `portfolio()` (special portfolio result auto-added when >1 result; returns main if only one), `hasResult(key)`, `addSubresult(key, settings, result)`, `removeSubresult(key, bool)`, `orders()` (`OrdersList`), `symbols()`, `specialValues()` (`SettingsMap`), `getStrategyXml()` (Element), `getLastSettings()`/`setLastSettings(String)`, `getBestWFResultKey()`, `getOptimizationProfile()`, `getOOS()`, `getFitness()`, `clone()`, `clear()`, `removeUnsavableSettings()`, `updated` field, static `merge(...)`, constants `ResultsGroup.Portfolio`, `ResultsGroup.AdditionalMarket`.
- `Result`: `stats(byte direction, byte plType, byte sampleType)` → `SQStats`; `statsOrNull(...)`; `getSettings()` (`SettingsMap`, `.clone()`), `getResultKey()`, `getResultsGroup()`, `get(key)`, `getInt/getString(key[, default])`, `setString`, `computeAllStats(specialValues, oos)`; constructor `new Result(key, rg, settings)`.
- Stats computed per **direction** (`Directions.Long/Short/Both`), **PL type** (`PlTypes.Money/Percent/Pips`), **sample type** (`SampleTypes.FullSample/InSample/OutOfSample`). `SQStats` is a key-value map keyed by databank-column class names: `getDouble("NetProfit")`, `getInt("NumberOfTrades")`, `getDouble("SQN")`, `getDouble("Drawdown")`; constants `StatsKey.NUMBER_OF_TRADES`, `StatsKey.NET_PROFIT`, `StatsKey.OPTIMIZATION_PARAMETERS`.
- **Numeric ids are not documented in these files.** The only numeric literals: cross-check XML `<Column-Value direction="0" sampleType="127" plType="10" subresult="30" market="1" confidenceLevel="50" columnType="0" resultType="main|RetestWithHigherPrecision">`; correlation period ids `5/10/20/30` (Hour/Day/Week/Month). Do not assume these map to `SampleTypes`/`PlTypes` enum values without checking the Javadoc/runtime.
- Result keys observed with all Retester cross checks on: `Portfolio`, `Main: EURUSD_M1/H1`, `CrossCheck_WhatIf`, `CrossCheck_HigherPrecision`, `AdditionalMarket: GBPUSD_M1/H1` (one per market), `WF: 10 runs : 20 % OOS` (one per runs/OOS combo; single WF and WF Matrix share the `WF:` prefix — indistinguishable). Monte Carlo stored in main result: `mainResult.getInt("MonteCarloManipulation_NumberOfSimulations")`, `"MonteCarloRetest_NumberOfSimulations"`; Opt. profile / SPP via `rg.getOptimizationProfile() != null`. Eight cross checks listed: What-if, MC trades manipulation, Higher precision, Additional markets, MC retest, Opt. profile/SPP, WF Optimization, WF Matrix.
- `SpecialValues` keys: `Symbol`, `Timeframe`, `HistoryFrom`, `HistoryTo`, `PortfolioHistoryFrom`, `PortfolioHistoryTo`, `DateGenerated`. `SettingsKeys`: `WalkForwardResult`, `PortfolioDataStart`, `PortfolioDataEnd`, plus backtest keys in §3.2/§3.4.
- Sources: `01-introduction/working-with-resultsgroup.md`, `05-custom-analysis/example-per-strategy-custom-analysis.md`, `11-coding-sessions-examples/*.md`

---

## 7. QuantDataManager (QDM)

### 7.1 Introduction page
- The mirrored intro contains only the line "Getting started with QuantDataManager (QDM)". No further content.
- Source: `quantdatamanager/01-introduction/introduction-to-qdm.md`

### 7.2 Legacy CLI (Build 117 – 118)
- Available from **Build 117**. **From Build 119 the CLI was replaced** by a new version documented under `cli-command-line/` (not in scope). Executable: `QDataManager_console.exe` (one script uses `QuantDataManager_console.exe -u` — inconsistency in the doc). Options are `key=value`; `[..]` = optional; `(..)` = default.

| Command | Purpose | Options (verbatim) |
|---|---|---|
| `-a` | Add symbols | `symbols`; `[instrument]`; `[bartype]` [startofbar, endofbar] (startofbar); `[datatype]` [M1,TICK] (M1); `[datasource]` [dukascopy,file,darwinex,crypto,yahoo] (dukascopy); `[exchange]` [Binance,Bitfinex,Coinbase,Poloniex] (Binance); `[postfix]`; `[logfile]` |
| `-e` | Edit symbol | `symbol`; `[name]`; `[instrument]`; `[bartype]`; `[logfile]` |
| `-d` | Delete symbols | `symbols`; `[logfile]` |
| `-c` | Clear symbols data | `symbols`; `[logfile]` |
| `-l` | List symbols | `csv` (export path); `[logfile]` |
| `-ia` | Add instrument | `instrument`; `[description]` (""); `[pointvalue]` (100000); `[ticksize]` (0.0001); `[tickstep]` (0.00001); `[defaultspread]` (2); `[datatype]` [stock,futures,forex,cfds,etf,index,crypto] (forex); `[logfile]` |
| `-ie` | Edit instrument | same fields as `-ia` without defaults |
| `-id` | Delete instruments | `instruments` |
| `-il` | List instruments | (doc lists `symbols` — likely copy error); `csv=`; `[logfile]` |
| `-u` | Update all data | none |
| `-di` | Import data from file | `symbol`; `filepath`; `[instrument]`; `[bartype]`; `[errorhandling]` [stop,ignore]; `[timezone]` (see `-tz`); `[timeframe]` [auto,Intraday,TICK,M1,M5,M15,M30,H1,H4,D1]; `[logfile]` |
| `-de` | Export data to csv | `symbols`; `timeframe` [TICK,M1,M5,M15,M30,H1,H4,D1]; `[datefrom]` "yyyy.MM.dd"; `[dateto]`; `[outputdir]` (default shown: `C:/Users/Tomas/workspaceSQ4/SQ4/work_directory/StrategyQuant/export`); `[prefix]` (""); `[format]` one of: Generic tick format (comma delimited), Generic bar format (comma delimited), Generic tick format (tab delimited), Generic bar format (tab delimited), MetaTrader4 tick format, MetaTrader4 bar format, Amibroker bar (aqi) format, Amibroker tick (aqi) format, Birt's CSV2FXT format, Forex Tester bar format, Forex SB bar format, Ninja Trader tick format, Ninja Trader bar format, Neuroshell Trader format, Tradestation bar format — default (MetaTrader4 bar format); `[logfile]` |
| `-dc` | Clone data | `symbols`; `[postfix]` (`_{timeframe}_{cloneTime}`); `[removeWeekends]` [true,false] (false); `[timezone]`; `[hours]` fixed shift in hours; `[logfile]` |
| `-tz` | List time zones | `[logfile]` |

- Examples verbatim: `QDataManager_console.exe -a symbols=EURUSD,GBPUSD datasource=dukascopy datatype=TICK`; `-e symbol=EURUSD name=EURUSD_OLD`; `-d symbols=EURUSD,GBPUSD`; `-c symbols=EURUSD,GBPUSD`; `-l csv=C:/data/symbols.csv`; `-ia instrument=EURUSD`; `-ie instrument=EURUSD datatype=forex`; `-id instruments=EURUSD`; `-il csv=C:/data/instruments.csv`; `-u`; `-di symbol=EURUSD instrument=EURUSD filepath=C:/data/EURUSD.csv`; `-de symbols=EURUSD_M1,GBPUSD_M1 timeframe=M1 datefrom=2018.01.01 dateto=2018.12.31 outputdir=C:/data format="Generic tick format (comma delimited)"`; `-dc symbol=AUDCAD hours=8` (note `symbol=` singular in this example vs `symbols` in option list); `-tz`.
- Time zones: special names `EETUS` ((EST+07), "UTC+2 US DST"), `EET` (UTC+02), plus IANA ids (`Etc/UCT`, `Europe/London`, `America/New_York`, `Europe/Prague`, …). Cloning to a different timezone requires the second (timezone) parameter, e.g. `timezone=EETUS`.
- Source: `quantdatamanager/02-command-line/quant-data-manager-command-line-interface-help.md`

### 7.3 Script examples (Windows batch)
- Dukascopy setup `dukas.bat` in `C:\QuantDataManager` (doc renders paths without backslashes: `C:QuantDataManagerdukas.txt`): loop over `dukas.txt` symbols → `QDataManager_console.exe -a symbols=%%A datasource=dukascopy datatype=M1`; then `-u`; then `-dc symbols=%%A timezone=EETUS postfix=_M1_UTC2`.
- Update script `dukas_update.bat`: `QuantDataManager_console.exe -u` (updates all Dukascopy-imported data).
- File import (Asirikuy) with `asirikuy.txt` lines `SYMBOLNAME;INSTRUMENT;FILE.csv` (e.g. `EURUSD_M1_UTC1_as;EURUSD;EURUSD_1_MT4.csv`): `-a symbols=%%A instrument=%%B datasource=file datatype=M1`; `-di symbol=%%A instrument=%%B timeframe=M1 timezone=Europe/Prague bartype=startofbar errorhandling=ignore filepath=C:\QuantDataManager\%%C`; `-dc symbols=%%A timezone=EETUS postfix=_UTC2`.
- Source: `quantdatamanager/02-command-line/quant-data-manager-command-line-interface-script-examples.md`

### 7.4 Symbol / instrument concepts and naming
- **Instrument** = specification: name, data type (stock/futures/forex/cfds/etf/index/crypto), Pip/Tick size (0.0001; JPY 0.01), Pip/Tick step (0.00001; JPY 0.001), Default spread (pips; CLI default 2), Point value in $ (usually 10 per pip in the MT4 how-to; CLI default `pointvalue` 100000), Default commission model.
- **Symbol** = a data series bound to an instrument, e.g. `EURUSD_M1`, `GBPUSD_M1`; clones get postfixes (`_M1_UTC2`, `_UTC2`, default `_{timeframe}_{cloneTime}`); MT5 import adds optional postfix (`_MT5`, `_BrokeX`, `_v2`). In snippets the symbol string is `"EURUSD_M1"` with timeframe `TimeframeManager.TF_H1` / XML `timeframe="H1"`. Importing into an existing symbol overwrites its data.
- Broker profile (MT5 import): groups instruments by broker for spread/commission/trading hours; `SQ default` = fallback.
- On-disk data layout is not described in these files.
- Sources: `quantdatamanager/03-how-to/import-history-data-metatrader-4.md`, `02-command-line/*.md`, `zz-unlisted/metatrader5-data-import.md`

### 7.5 MT4 import procedure
- MT4: Tools → History Center → symbol → double-click `1 Minute (M1)` → Export. "StrategyQuant supports import of only 1 Minute data, it will compute the higher timeframes automatically."
- SQX Data manager → File import → **Add symbol** (name + instrument or "Add new symbol" to define instrument) → Save → select row → **Import file…** → Predefined File Format **MetaTrader4** → Start Import.
- Source: `quantdatamanager/03-how-to/import-history-data-metatrader-4.md`

### 7.6 MT5 export / import (CSV)
- Export from MT5: Market Watch → right-click → Symbols → symbol → **Bars** tab → date range → Request → select bars → Export → folder. Broker history may be short.
- Export from QDM for MT5: select symbol → "export data for Metatrader 5" → date range (all), spread (fixed e.g. 8 points = 0.8 pips, or computed from tick data; exported spread is in **points**), output path. **MT5 cannot compute higher TFs from tick data — import both M1 and tick data**; higher TFs computed only from M1.
- Import into MT5: Ctrl+U → create custom symbol (copy broker settings) → set name + spread (floating if variable spread from ticks) → Show symbol → Bars → Import bars → choose CSV → white window = OK (red = not 99% quality).
- Sources: `quantdatamanager/03-how-to/how-to-export-data-from-metatrader-5.md`, `how-to-import-data-to-metatrader-5.md`

### 7.7 MT4 tick-precision testing (FXT & HST export)
- Data manager → select tick data → **Export to MT4 (FXT & HST)** → date range, MT4 installation path (first time), MT4 symbol/settings → Start export. **MT4 must be closed during export.** Backtest in MT4 Strategy Tester (leave "Use date" unchecked) → Report shows 99% modeling quality.
- Custom broker settings: copy `{SQ installation}/custom_indicators/MetaTrader4/Scripts/ExportProperties.mq4` to `{MT4 Data folder}/MQL4/Scripts`, restart MT4, run on a chart **while connected to broker** → `{MT4 Data folder}/MQL4/Files/mt4.properties` → in export dialog "Load other MT4 data specification file".
- Troubleshooting: no trades/Journal errors → symbol properties mismatch, use exported properties; modelling quality `n/a` with no errors and a fully green bar is acceptable (same ticks/results as a 99% install).
- Source: `quantdatamanager/03-how-to/test-strategy-metatrader-4-tick-precision.md`

### 7.8 MT5 direct API import (SQX Build 144+)
- Data Manager → **Data sources** tab → **MT5 import** tile → dialog. Requirements: MT5 on same machine, launched and logged in at least once; raise Tools → Options → Charts → **Max bars in chart** (e.g. 99,999,999) and restart MT5.
- Fields: **MT5 install folder** (must contain `terminal64.exe`; standard or portable auto-detected), **Fetch symbols** (only Market Watch–visible symbols; Show All), **Filter items**, **Show types**, **Download range** (From/To, Since last date, Last 6 months/year/5 years/10 years, All time; capped by broker history — "Data Availability Disclaimer"), **Symbol table** (multi-select), **Broker profile**, **Data postfix**. **Start import**; Progress bar; Pause all / Stop all; results in **Data** tab.
- Imports OHLC bars + metadata (digits, tick size, tick value, contract size, base/profit currency, calculation mode, raw spread) → derives Pip/Tick size, Pip/Tick step, Point value, Default spread.
- **Always creates a new symbol** (not incremental despite the "Since last date" range option); to refresh, delete instrument and reimport, or use a postfix to keep both.
- Errors: "Selected folder doesn't contain terminal64.exe"; empty fetch → MT5 not launched/logged in; short range → broker depth or Max bars; missing symbol → Market Watch; wrong tick size/spread → delete & reimport, may need metadata override.
- Source: `quantdatamanager/zz-unlisted/metatrader5-data-import.md`

---

## 8. Facts an integrator must not get wrong

**Files / folders**
- Snippet export/import format is `.sxp` (CodeEditor Import/Export → Export/Import extensions); plugins are also imported as `.sxp`.
- Custom JARs: `{SQ}/user/libs` (Build 130+, restart required). Logs from `fdebug`: `{SQ}/user/log/StrategyQuant`. User plugins: `{SQ}/user/extend/Plugins`. Projects: `{SQ}/user/projects/<Project>/project.cfx`.
- `.cfx` (project) and `.sqx` (strategy) are ZIP archives; `.cfx` holds `config.xml` plus one XML per task.
- User snippet roots: `User/Snippets/SQ/{CustomAnalysis,TradeAnalysis,Whatif,Columns/Trades,MoneyManagement}`; indicators/signals under `Snippets/SQ/Blocks/Indicators/<Name>/`; commissions cloned from `Builtin/Snippets/Trading/Commissions`.
- Java packages: `SQ.Blocks.Indicators.<Name>`, `SQ.Blocks.OtherActions`, `SQ.Columns.Databanks`, `SQ.Columns.Trades`, `SQ.CustomAnalysis`, `SQ.TradeAnalysis`, `SQ.WhatIf`, `SQ.Trading.Commissions`, `SQ.MoneyManagement`, `SQ.EngineCharts`, `SQ.Utils`, `SQ.TradingOptions`, `SQ.Internal.{IndicatorBlock,ActionBlock}`.
- Platform templates: `Code/<Platform>/blocks/<BlockKey>.tpl` (Freemarker); missing template → strategy source shows an error; use right-click "Add all missing".
- Indicator Tester inputs: `{SQ}/tests/Indicators/MetaTrader4/*.csv`; MT4 helper EA `{SQ}/custom_indicators/MetaTrader4/Experts/SqIndicatorValuesExportEA.mq4`; MT4 properties script `{SQ}/custom_indicators/MetaTrader4/Scripts/ExportProperties.mq4` → `{MT4 Data}/MQL4/Files/mt4.properties`.
- UI cache to clear when a plugin does not appear: `{SQ}\internal\electron\resources\userData`.
- QDM export default dir literal in docs is a developer path (`C:/Users/Tomas/...`); always pass `outputdir`.

**Result keys / sample types / special values**
- `SampleTypes.FullSample / InSample / OutOfSample`, `Directions.Long / Short / Both`, `PlTypes.Money / Percent / Pips` — symbolic only; **numeric ids are not given in these docs**. XML literals seen: `direction="0" sampleType="127" plType="10"`.
- Result keys: `Portfolio`, `Main: <SYMBOL>/<TF>`, `CrossCheck_WhatIf`, `CrossCheck_HigherPrecision`, `AdditionalMarket: <SYMBOL>/<TF>`, `WF: <n> runs : <p> % OOS`. Monte Carlo lives in main result under `MonteCarloManipulation_NumberOfSimulations` / `MonteCarloRetest_NumberOfSimulations`; Opt. profile via `getOptimizationProfile()`.
- `SQStats` keys = databank-column class names (`NetProfit`, `Drawdown`, `NumberOfTrades`, `SQN`, `DrawdownPct`).
- Orders in a portfolio are identified by `Order.SetupName` = result key; `symbols().add(...)` is required for PL computation.

**XML element / attribute names**
- Task config: `<Data><Setups><Setup dateFrom dateTo testPrecision session slippage minDist engine><Chart symbol timeframe spread/><Commissions>`; `<Blocks><BuildingBlocks><Block key use>`; `<CrossChecks>` children `<RetestWithHigherPrecision>`, `<MonteCarloManipulation>`, `<MonteCarloRetest>` each with `<Settings>` and `<AcceptanceSettings><Conditions>`; last settings `<Options><BuildTradingOptions><Params><Param className key>`; fitness `<Ranking type><Goal use type weight valueType target/>`.
- Block key prefixes in Builder config: none for signals/comparisons, `Indicators.`, `Prices.`, `Stop/Limit Price Levels.`, `Stop/Limit Price Ranges.`.
- Strategy XML block: `<Item key name display help mI returnType categoryType>` + `<Param key="#Name#" type controlType paramType minValue maxValue step builderStep genMinValue genMaxValue>`; auto `#Shift#`, `#Chart#`, `#Line#`.
- Date attribute format `yyyy.MM.dd`.

**API / behaviour gates**
- `getConfig()` must be **cloned** before edit, then `setConfig(el, true)`; UI shows change only after `SQWebSocketManager.addToDataQueue(new DataToSend(WebSocketConst.UpdateProject, …), SQConst.CODE_TASKMANAGER)`.
- Signals must declare `@Parameter public int Shift;` explicitly; indicators get it implicitly.
- Databank `getValue()` columns are display-only text: not usable in Ranking filters or dependencies; CA-computed values likewise cannot be used in Ranking custom filters (filter via `filterStrategy()` return instead).
- Per-databank CA (`TYPE_PROCESS_DATABANK`) runs only from a CustomAnalysis task in a custom project.
- `OptimizationSettings.param2*` must be set even for plain WF; `OptimizationConst.WF_TYPE_SIMIS_EXACTOOS`.
- `HistoryDataLoader.get(...)` throws `HistoryDataNotAvailableExeption` (misspelling is the real class name in docs) and does not work for protected SQ Futures/Equities data.
- `ChartSetup` first argument is the constant `"History"`; symbol must match Data Manager name.

**Version / build gates**
- Build 128: `debug()/fdebug()` in all snippets (indicators need `DebugConsole.log()`/`Log.info()` in 128 itself).
- Build 130: custom JARs in `/user/libs`.
- Build 136: `ISQTask.getConfig()/setConfig()`; public `Parameter` constructor; `StrategyParametersHelperV2` import-order issue fixed "in the next Build 136".
- Build 136 Dev 2: `HistoryDataLoader`; user plugins + "Compile plugin".
- Build 136 Dev 5 (3 Nov 2022): `DatabankColumn.compute(...)` 7-arg with `Result`; last settings available at metric-compute time.
- Build 136 Dev (29 Sep 2022): `IFitnessFunction` gained `getMetricsForFitness()`, `getMetricValue()`, `clone()`.
- Build 138: custom project task plugins (`AbstractTask` + `ISettingTabPlugin`).
- Build 140: updated versions of both plugin examples and of `CAOptimizationByProgramming` (`_sqx140`).
- SQX Build 144: MT5 direct API import tile in Data Manager.
- QDM Build 117: legacy CLI (`QDataManager_console.exe -a/-e/-d/-c/-l/-ia/-ie/-id/-il/-u/-di/-de/-dc/-tz`); QDM Build 119+: new CLI (documented in `cli-command-line/`, not in these files).

**QDM procedures**
- Only M1 data is imported from MT4; higher TFs are computed. MT5 cannot build higher TFs from ticks — import both M1 and tick data into MT5. Exported MT5 spread is in points.
- Close MT4 before FXT/HST export; MT4 must be connected to broker when running `ExportProperties.mq4`.
- MT5 direct import always creates a new symbol; refresh = delete + reimport or use a postfix. Raise "Max bars in chart" first.
- Timezone alias `EETUS` = "UTC+2 US DST" for cloning to broker time.
