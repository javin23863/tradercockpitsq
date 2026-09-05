# StrategyQuant X — operating guide for TraderCockpit development

Synthesized from the official documentation mirrored in this folder (186 pages fetched 2026-09-04,
see `README.md` and `manifest.json`). Every section names the mirror files it rests on, so a claim
can be checked against the official text. This guide is reference material for agents and
reviewers: it explains what the producer does and how it is operated so that TraderCockpit
integrates the real program instead of guessing. It is not a roadmap and it does not override
`AGENTS.md`, the architecture, the backbone spec, or the living plan.

Authority order when they disagree: the installed SQX 144.2953 runtime (observe it) > the
official docs mirrored here > the agent digests under `digests/` > memory. The docs span many
builds (SQ4 era through 144); where a page is dated or contradicts another page, this guide says so.

Companion catalog: `../sqx-native-features/README.md` already maps the Builder Full-settings
panes, Progress and Results tabs to product widgets and kinds (`write` / `native-run` /
`read-model` / `unavailable`). This guide is the broader program model behind that catalog.

---

## 1. What the program is

StrategyQuant X (SQX) is a Windows/Mac/Linux Java desktop program (HTML/JS UI, Java servlet
backend, Electron shell) that **generates, retests, optimizes and robustness-tests algorithmic
trading strategies**, then exports them as source code for MetaTrader 4/5 (MQL), Tradestation /
MultiCharts (EasyLanguage), pseudo code, or Strategy XML. It is not an EA and never trades live.

Core mental model:

1. A strategy is a set of `IF condition THEN action` rules built from **building blocks**:
   indicators, price values, time values, comparison/logic operators, signals (pre-built
   conditions like "CCI crossed above 0"), order types (Market, Stop, Limit, Enter/Reverse),
   exits (SL, PT, trailing, move-to-BE, exit after X bars, EOD/Friday exits, ATM).
   ~250+ built-in blocks; users can add their own (custom blocks, Java snippets, imported data).
2. **Random generation** picks blocks randomly under validity constraints (price is not compared
   with time, etc.) and backtests each candidate on the configured data. Thousands per hour.
3. **Genetic evolution** takes a random initial population and evolves it across generations
   using fitness, crossover, mutation, islands and migration. Faster convergence, more overfitting.
4. Every candidate is backtested by SQX's own engine emulating the target platform's fills
   (MetaTrader 4, MetaTrader 5 hedging/netting, Tradestation, MultiCharts, JForex, Stockpicker).
   **Build in the engine you will trade in.**
5. Survivors are stored in **databanks** (bounded, ranked by fitness, filtered by conditions).
6. **Cross checks** (robustness tests) run after the main backtest as a funnel: cheap ones first,
   failing strategies dismissed before expensive ones run.
7. **Retester** re-runs saved strategies on other data/settings; **Optimizer** runs simple,
   Walk-Forward and Walk-Forward Matrix optimizations; **Custom projects** chain all of these
   into unattended task workflows.
8. Output is a `.sqx` strategy file (ZIP with strategy XML, settings and results including the
   `orders.bin` trade list) and generated platform source code.

Official product framing to keep straight: "generating is only 50% of the work"; it is common
that only ~1 in 1000 profitable generated strategies passes rigorous robustness tests.

Sources: `strategyquant/01-introduction/introduction.md`, `how-does-strategyquant-work.md`,
`whats-new-in-strategyquant-x.md`, `03-quick-start/different-build-modes.md`, `workflow.md`.

### 1.1 Strategy styles (architecture)

- **SQ3 style**: four independent rules (Long/Short entry, Long/Short exit). Ambiguous when both
  sides fire on the same bar.
- **SQ X style** (default): one **Signal rule** computes `LongEntrySignal`, `ShortEntrySignal`,
  `LongExitSignal`, `ShortExitSignal`; entry/exit rules act only when the opposite/conflicting
  signals are false.
- **SQ X with Fuzzy Logic**: the signal is a vote — N conditions listed without AND/OR, a
  percentage of them must be true. Only meaningful with ≥3–4 conditions per signal
  (set `# of Conditions` minimum ≥3).
- **Custom strategy templates**: any architecture built in AlgoWizard with random placeholders.

Source: `strategyquant/03-quick-start/strategy-style.md`.

### 1.2 Symmetry

Entry and exit rules may be **symmetrical** (short = negated long: `CCI > 0` / `CCI < 0`) or
generated independently per side. Symmetry is set separately for entry and exit rules.
Negation defaults come from block snippets (`>`↔`<`, `=`↔`<>`) and can be overridden in
`user/settings/OppositeBlocks.csv` (`Block;OppositeBlock` per line, Java snippet names, restart).

Sources: `05-program-screens/what-to-build.md`, `11-how-to/use-oppositeblocks-configuration-to-control-the-negation.md`.

---

## 2. Program layout and module shell

Left rail (official program layout, `03-quick-start/program-layout.md`):

| # | Module | Purpose |
|---|---|---|
| 1 | **Getting started** | sample settings (predefined Builder configs), example custom projects, news, links |
| 2 | **Builder** | build new strategies or improve existing ones — the core |
| 3 | **Retester** | retest existing strategies on different data/settings, optionally with cross checks (no filtering required) |
| 4 | **Optimizer** | simple, Walk-Forward, Walk-Forward Matrix, Sequential optimization |
| 5 | **Data manager** | download/import/export/clone history data, symbols, instruments, sessions, stock groups, external indicators, broker profiles |
| 6 | **Custom projects** | user-defined flow of tasks |
| 7 | **AlgoWizard** | visual strategy editor; also templates, custom blocks, random groups (Customize menu) |
| 8 | Interactive tutorials | |
| 9 | Advanced workflows | sample custom projects |
| 10 | Debug console | messages from snippets (`debug(category, msg)`) |
| 11 | Code Editor | Java snippets (indicators, blocks, columns, MM…) and Freemarker code templates; Import/Export `.sxp`; Test indicators |
| 12 | Grid Control | tasks on a computing grid |
| 13 | Global config | program configuration (memory, GPU, …) |
| 14 | Report Bug | |
| 15 | Build version | |

Added later: **Portfolio Master** (Automatic Portfolio Builder, from build 138, port of the
QuantAnalyzer feature) and **Portfolio Composer** (build 141, weights + margin simulation,
Markowitz efficient-frontier automatic computation). Sources: `03-quick-start/automatic-portfolio-builder.md`,
`04-new-features/portfolio-composer.md`, `automatic-portfolio-construction.md`.

**Every project module (Builder, Retester, Optimizer, Custom projects) has the same shell**
(`05-program-screens/builder.md`, `03-quick-start/builder-layout.md`):

- Top tabs: **Progress | Full settings | Results**.
- **Progress**: Start / Pause / Stop, engine log, performance and memory charts (configurable
  engine charts: strategies per hour, accepted per hour…), settings overview, dismissal
  statistics link, Load/Save project configuration.
- **Full settings**: every settings pane of that module (see §4).
- **Results**: tabs for the strategy currently selected (double-clicked) in the databank (see §5).
- Screen columns: **Engine** (start/stop/log) · **Settings** summary tabs · **Results** mini
  panels for the top three strategies · **Databank** grid at the bottom (minimizable).

Project configuration can be saved/loaded from the Progress tab; the CLI writes it as a
`.cfx` archive (`-project action=saveconfig name=Builder file=Builder.cfx`). The docs page on
Load/Save build config is screenshot-only; the file format is proven by the CLI page and the
programming docs (`.cfx` = ZIP with `config.xml` and one XML per task).

---

## 3. Data model, files and folders

### 3.1 Install root and `user/` tree

Never install under `C:\Program Files`; the program writes into its own tree. `StrategyQuantX.exe`
marks the install root (default `C:\StrategyQuantX<version>`; each build installs to its own folder).

| Path (relative to install root) | Meaning |
|---|---|
| `StrategyQuantX.exe`, `sqcli.exe`, `StrategyQuantX.config`, `sqcli.config`, `CodeEditor.config` | executables and Java/memory config (heap set by installer / Global config → Memory) |
| `user/data` | history data (instruments, timeframes); can be GB |
| `user/customdata` | external indicator values imported as data |
| `user/projects/<Project>/project.cfx` | project archive (ZIP: `config.xml` + one XML per task); project databanks live under `user/projects/<Project>/databanks/<Bank>/*.sqx` (observed at runtime, see §12) |
| `user/templates` | strategy templates |
| `user/strategies` | only if the user saves strategies there (not default) |
| `user/settings/settings.xml` | app settings, e.g. `<WebServerPort>`, `<gpuAccelerated>` |
| `user/settings/customBlocks.xml`, `user/settings/blockGroups.xml` | custom blocks, random groups (installer table; one older how-to swaps the labels) |
| `user/settings/views/` | databank / trade list / WF views |
| `user/settings/calibrationSettings.txt` | advanced indicator calibration instances |
| `user/settings/OppositeBlocks.csv` (+ `_example.csv`) | negation overrides |
| `user/extend/Code`, `user/extend/Snippets`, `user/extend/Plugins`, `user/extend/Libs` (docs also say `user/libs`) | user extensions since build 131 |
| `user/extend/ResultsPlugins/<Plugin>/index.html` | HTML Results plugins (build 144 ships `CLAUDE.md` there) |
| `user/log/StrategyQuant/log_YYYY_MM_DD.log` | daily log; level in `internal/web/SQUANT/log_config.xml` |
| `user/sqxbusiness` | SQ4Business |
| `internal/` | built-in extensions (read-only), `internal/testfiles` (backtest cache, safe to delete when closed), `internal/electron/resources/userData` (UI cache) |
| `custom_indicators/MetaTrader4|MetaTrader5|Tradestation` | SQ custom indicators to copy into the trading platform (`SQ.eld`, `SQ_MC.pla`, `.mq4/.mq5`), helper EAs/scripts |
| `tests/Indicators/MetaTrader4` | Indicator Tester CSV inputs |
| `fixDPI.bat` | blurry-UI fix (build 132 dev2+) |

Sources: `01-introduction/installation.md`, `update-of-folder-structure-from-build-131.md`,
`02-installation/how-to-install-strategyquant-x-and-transfer-your-data-from-a-previous-version.md`,
`11-how-to/data-migration-between-strategyquant-version.md`, `switching-logs-to-debug-mode.md`,
`manually-configure-internal-web-server-port.md`, `how-enable-or-disable-gpu-acceleration-in-strategyquant-x.md`,
`digests/*`.

### 3.2 Strategy files and databanks

- Strategies are saved as **`.sqx`** (older docs: `.str`). A `.sqx` is a ZIP with the strategy
  XML, the last backtest settings and results (the trade list is `orders.bin`; the CLI can dump
  it: `-tools action=orderstocsv|orderstoxlsx file="Strategy 0.1487.sqx" [data=main|all]`).
- Exported `.mq4/.mq5/.el` source is one-way; SQX cannot read it back. Always keep the `.sqx`.
- **Databank** = bounded ranked storage of strategies with results. Each module has its own
  databanks; custom projects can create unlimited named databanks (e.g. Results, Final,
  Initial population, Existing portfolio). Capacity and sort are Ranking settings.
- Databank actions: Load, Save (multiple formats; export table to CSV/XLS), Delete/Clear all,
  **Retest** (Builder → Retester with exact backtest config), Edit (parameters or rules in
  AlgoWizard), **Portfolio/Merge** (Simulated portfolio | Strategies merged to one EA
  [experimental] | Ensemble signals [experimental]; Split reverses since build 127),
  **Compare** two strategies' last-backtest configs (build 130), Manage views (columns).
- Strategy naming as seen in docs: `Strategy 0.1487`, `Strategy 1.2.3`.

Sources: `03-quick-start/databanks-and-files.md`, `05-program-screens/databank.md`,
`11-how-to/merge-split-portfolio.md`, `10-reliable-backtesting-trading/comparing-and-using-the-same-backtest-settings.md`,
`cli-command-line/02-commands-sq-only/tools-tools.md`, `digests/programming-for-sq-and-quantdatamanager.md` §1.8, §6.

### 3.3 Results object model (from the programming docs)

One databank row = one `ResultsGroup`: strategy XML, main backtest `Result`, sub-results
(cross checks, additional markets, WF runs), a computed `Portfolio` result when >1 result,
`orders()`, `specialValues()` (Symbol, Timeframe, HistoryFrom/To, DateGenerated…), last settings
XML. Stats are keyed by databank-column class names (`NetProfit`, `Drawdown`, `NumberOfTrades`,
`SQN`…) and computed per **direction** (Long/Short/Both) × **P/L type** (Money/Percent/Pips) ×
**sample type** (InSample/OutOfSample/FullSample). Result keys observed: `Main: EURUSD_M1/H1`,
`Portfolio`, `CrossCheck_WhatIf`, `CrossCheck_HigherPrecision`, `AdditionalMarket: GBPUSD_M1/H1`,
`WF: 10 runs : 20 % OOS`. Cross-check acceptance XML uses literals such as `direction="0"
sampleType="127" plType="10"`; the numeric enum ids are otherwise undocumented — TraderCockpit's
existing reading of native sample types (IS 10–19, OOS 20–30, full 127) comes from runtime
observation, not from these docs.

Source: `digests/programming-for-sq-and-quantdatamanager.md` §3.3, §6.

---

## 4. Builder Full settings — complete pane reference

Panes appear as top-row tabs. `Genetic options` shows only when Build mode = Genetic evolution;
`Parts to improve` only when Strategy type = Improve existing strategy.

### 4.1 What to build (`05-program-screens/what-to-build.md`)

- **Strategy type**: Simple strategy · Multi-TF or multi-symbol strategy (declare number of
  additional charts; bind them in Data) · Strategy from template (AlgoWizard template) ·
  Improve existing strategy (choose strategy; then Parts to improve).
- **Trading direction**: Long / Short / Both; symmetrical entry rules yes/no; symmetrical exit
  rules yes/no.
- **Strategy style** popup: SQ3 / SQ X / SQ X fuzzy (fuzzy % of conditions).
- **Build mode**: Random generation | Genetic evolution (adds Genetic options tab).
- **# of conditions** min/max per signal; **Shift** (lookback) min/max — recommended 1–5 and
  min shift 1 (indicators computed on close; conditions evaluated on open); **Indicator period**
  min/max — >1 and ideally <50–100. Configurable **per chart** for multi-chart strategies.
- **Stop Loss & Profit Target**: mandatory yes/no, min/max in pips (or ATR multiples — the
  troubleshooting doc recommends ATR-multiple minimum ≥1.5), desired risk/reward. Without
  mandatory SL/PT enable another exit (e.g. exit after X bars).

### 4.2 Parts to improve (`parts-to-improve.md`, `01-introduction/multiple-exits-generation-scale-out-atm.md`)

Entry rule / Exit rule / Order type (and ATM in build 131 Ultimate) each with **Add | Replace |
Add or Replace**. Alternative and more flexible: open the strategy in AlgoWizard, insert
generation placeholders, build from template.

### 4.3 Genetic options (`genetic-options.md`)

Max # of generations (5–100 recommended) · Population size per island (10–100+) · Crossover
probability · Mutation probability · **Islands** (1–10) · Migrate every Xth generation (~10) ·
Population migration rate (% ≈ 1–5 strategies) · **Initial population**: use strategies from
"Initial population" databank (unfiltered), generated decimation coefficient (X-times more
generated, best kept — expensive), initial population filter (recommended only "number of
trades") · **Evolution management**: Start again when finished; Restart evolution if fitness
stagnates (uses In Sample Validation part) · **Fresh blood**: detect same strategies and replace;
replace X % weakest with new ones · Show last generation databank (island 1 only).
Limitation: random groups/custom blocks constrain only the initial population under GE.

### 4.4 Data (`data.md`)

- **Trading engine** (backtesting engine): MetaTrader 4, MetaTrader 5, Tradestation,
  MultiCharts, JForex, Stockpicker… (`Setup engine="..."` in task XML).
- **Backtest data**: symbol, timeframe, date range; one row per chart for multi-TF/multi-symbol.
- **Test precision**: `Selected timeframe only` (4 ticks O/H/L/C per bar; fastest; weak for
  stop/limit) · `1 Minute data` · `Real tick – custom spread` · `Real tick – real spread`
  (also `Trade on bar open` in the API). Generate fast, retest precise.
- **Spread**, **Slippage** (pips/ticks added to open and close), **Min distance** (broker
  pending-order distance), **Commission & swaps** popup (swap in points / money / percent;
  formulas in the doc; commission method from a list).
- **Data range parts**: In Sample Training (IST) · In Sample Validation (ISV, used to restart GE
  on stagnation) · Out of Sample (OOS) · No Trade.
- Task XML shape: `<Data><Setups><Setup dateFrom dateTo testPrecision session slippage minDist
  engine><Chart symbol timeframe spread/><Commissions>…` (dates `yyyy.MM.dd`).

### 4.5 Trading options (`trading-options.md`)

Exit at end of day (+ time) · Exit on Friday (+ time) · Limit time range (from/to) + Exit at end
of range · Maximum trades per day · Minimum/Maximum SL and PT (0 = unlimited; computed values are
clamped) · **Session** (created in Data manager; `MarketOpenSession` for MT5 futures since build
140, sessions end `23:59`) · Reserved bars (> longest indicator period) · Realistic gaps handling
(MT4 only) · **Store chart data** (required for Results → Trades on chart). Snippet classes:
`ExitAtEndOfDay`, `ExitOnFriday`, `LimitTimeRange`, `MinMaxSLPT`.

### 4.6 Building blocks (`building-blocks.md`, `indicators-calibration.md`)

Four panels: **Signals / Indicators / Stop-Limit entry blocks** (left; signals are prebuilt
conditions such as "ADX is rising"; indicators/price values are combined randomly with
comparison operators; stop/limit blocks price pending orders) · **Order types** (Market, Stop,
Limit, Enter/Reverse) · **Exit types** (each with a percentage probability) · **Custom data
indicators** (external values, only for the matching timeframe).

Each block: checkbox, **weight** (competing blocks) or **%** (exits), and a **Default** link
opening per-block configuration with three tabs:

- **Parameter values**: every parameter (Chart, Computed from, Period, Shift, …) fixed or random;
  random = range from–to or a value list. Per-block period/shift ranges default to the global
  What-to-build ranges.
- **Parameter sets**: alternative fixed/random combinations competing by weight (weight 0 on
  plain parameters forces sets).
- **Indicator values**: expected value range used when comparing the indicator with numbers.
- Engine-unsupported blocks show a red flag (e.g. Draw Down Arrow for EasyLanguage).

**Calibration**: computes each indicator on the configured data, min = mean − σ, max = mean + σ,
step per popup settings; `Calibrate now` button or `calibrate before start` switch; advanced
multi-instance settings in `user/settings/calibrationSettings.txt` (`rounding=disabled`).
Task XML: `<Blocks><BuildingBlocks><Block key="Indicators.ADX" use="true"/>` with key prefixes
none (signals/comparisons), `Indicators.`, `Prices.`, `Stop/Limit Price Levels.`,
`Stop/Limit Price Ranges.`.

### 4.7 ATM — Advanced Trade Management (`settings-atm.md`)

Multiple exits (scale out). Enable → define exits: each exit has a size % and a type (multiple of
original SL or PT, fixed profit, trailing stop, bars-based). **ATM overrides all original exits
except Stop Loss.** Size constraints (decimals, minimum size) decide how many exits fit the MM
size. Config source: ATM saved in strategy | ATM defined here | Generate ATM (build 131
Ultimate: choose exit types/ranges and one of 7 scenarios; 2–3 exits generated randomly; usable
in Improver via Parts to improve → ATM).

### 4.8 Money management (`money-management.md`)

Fixed size (recommended for generation) · Fixed amount · Risk fixed % of account (equity) ·
Risk fixed % of balance · Stocks size by price (Use account balance) · Crypto size by price ·
Simple Martingale · extensible via `MoneyManagementMethod` snippets. Method siblings carry `use`
flags in XML (one exclusive choice). API names: `FixedSize`, `RiskFixedPctOfAccount`.

### 4.9 Ranking (`ranking-options.md`, `zz-unlisted/understanding-automatic-dismissal-rules.md`, `03-quick-start/fit-strategy-to-existing-portfolio.md`, `custom-analysis.md`)

- **Databank options**: maximum strategies to store; **Stop generation when** databank full or
  never.
- **Fitness** (0–1): **Use** main data backtest | portfolio (all additional-data trades) |
  Existing portfolio (build 130: simulate new strategy + Existing portfolio databank);
  **Determine best strategies by** a predefined criterion (Net profit, Profit factor,
  Return/DD, SQN…) or a weighted multi-goal function (`<Ranking type><Goal use type weight
  valueType target/>`).
- **Automatic filters / dismissal** ("Configure automatic dismissal"): No trades · Too many
  ambiguous trades · Too many open trades (>100k) · No filled trades · Zero PL trades · Zero
  duration trades · Unfinished trades · Too little trades (<20) · Outlier trade (> 2× 2nd+3rd
  best) · Too many trades closing at the same bar. Evaluated after 40% of data; "too many" =
  ≥25% of placed trades. Dismissal statistics dialog shows counts per reason incl. custom
  conditions.
- **Custom filtering conditions**: any databank column, per sample **IS / OOS / RT (robustness
  tests) / P (portfolio)**, in Money / Percent / Pips, for Long / Short / Both; a strategy
  matching any condition is dismissed. Columns may take their value **From backtest** = main,
  a cross check (e.g. SPP medians, WF Optimization, WF Matrix) — that is how cross-check results
  become filterable columns.
- **Correlation filter** with Existing portfolio (max correlation, P/L by hour/day/week/month).
- **Custom analysis** per strategy (snippet; optional Filter) runs after backtest + cross checks,
  before saving.
- **Dismiss similar strategies in databank**: fingerprint = trades count, net profit, drawdown
  (full sample, money) within ±5% → keep the one with higher fitness.

### 4.10 Cross checks (`cross-checks-robustness-tests.md`, `06-cross-checks-robustness-tests/*`)

Simple slider (Basic / Standard / Extensive) or advanced per-check enable + Settings + Filtering
(acceptance conditions). Detailed in §6.

### 4.11 Notes (`notes-2.md`)

Free text saved with the project config.

---

## 5. Results tabs (per selected databank strategy)

| Tab | Content | Source |
|---|---|---|
| **Overview** | statistics of the backtest (main / portfolio / sub-results switchable) | `results-overview.md`, `strategy-analysis-metrics.md` |
| **List of trades** | every trade; switch per symbol/TF or whole portfolio | `results-list-of-trades.md` |
| **Equity chart** | equity, stagnation marker, options: daily equity, balance, MAE/MFE, additional-market curves, **Benchmark** (SPY buy-and-hold with $/% drawdown / MM / exposure normalization, build 139; X axis must be Time) | `results-equity-chart.md`, `03-quick-start/new-benchmarking-feature.md` |
| **Trade analysis** | yearly performance + configurable charts (per hour, weekday, day of month…) | `results-trade-analysis.md` |
| **Trades on chart** | bars + indicators + orders; requires Trading options → Store chart data | `results-trades-on-chart.md` |
| **Strategy config** | module config vs config used in last backtest, differences red, **Apply Strategy Config** | `results-strategy-config.md` |
| **Source code** | Pseudo code / MT4 EA / MT5 EA / EasyLanguage / Strategy XML; options: parameter variables (`Put values to variables`, symmetric variables), MM used | `results-source-code.md` |
| **Portfolio correlation** | only when multiple results (merge or additional markets): by hour/day/week/month, P/L or trade counts | `results-strategy-correlation.md` |
| **Optimization profile** | when an optimization ran: % profitable runs, profit histogram vs average, 3D landscape; PASS/FAIL checks | `06-.../optimization-profile-system-parameter-permutation-strategyquant.md` |
| **Sys. Param. Permutation** | median of every stat over all optimization runs + histograms | same |
| **Walk-Forward** views | per-run optimization/run results, robustness score components, WF equity vs original, WFM 3D score chart, best cluster | `07-optimization/*` |
| **Results plugins** | HTML plugins in `user/extend/ResultsPlugins/*` (Prop analytics, Prop Monte Carlo, Robustness Scorecard example) via `postMessage` (`GET_STATS`, `GET_ORDERS`, `SET_THEME`, `SET_LANGUAGE`; the packaged Source Code Translator also uses `GET_SOURCE_CODE` with a `format` param — observed in the plugin, not in the docs); Pro/Ultimate | `04-new-features/results-plugins.md` |

Metrics glossary (labels; the docs give images, not formulas — do not treat as formulas):
Total Profit, Profit in pips, Yearly AVG profit, Yearly AVG % return, CAGR, Sharpe ratio,
Profit factor (≥1.3 suggested), Return/DD ratio, Winning percentage, Drawdown, % Drawdown,
Daily/Monthly AVG profit, Average trade, Annual % / Max DD %, R Expectancy, R Expectancy score,
Strategy Quality Number (Van Tharp bands 1.6–7.0), SQN score, Wins/Losses ratio, Payout ratio,
AHPR, Z-score, Z-probability, Expectancy, Deviation, Exposure, Stagnation in days / %, Gross
profit/loss, Average win/loss, Max consecutive wins/losses, Symmetry, Trades symmetry,
NSymmetry, Stability (proprietary linear-regression straightness).

---

## 6. Robustness: cross checks, optimization, walk-forward

### 6.1 The funnel

Cross checks run per generated strategy after global filters, cheapest first; a failing strategy
is dismissed and never reaches the next check. Typical order (`03-quick-start/cross-checks-automated-strategy-robustness-tests.md`):

1. main backtest at Selected timeframe → automatic + custom filters;
2. **Retest with higher precision** (M1 / tick) — result key `CrossCheck_HigherPrecision`,
   XML root `<RetestWithHigherPrecision>` (`<Precision>`, `<Spread>`);
3. **Monte Carlo trades manipulation** — no re-backtest; methods `RandomizeTradesOrder`
   (resampling) and `RandomlySkipTrades` (Probability %); `<NumberOfSimulations>`,
   `<MCUseFullSample>`; results at confidence levels;
4. **Retest on additional markets** — other symbols/timeframes; results per market
   (`AdditionalMarket: <SYMBOL>/<TF>`) plus portfolio; ok if "slightly losing";
5. **Monte Carlo retest methods** — each simulation is a full backtest: `RandomizeStartingBar`,
   `RandomizeStrategyParameters` (Probability, MaxChange %, Symmetric),
   `RandomizeStrategyParametersCustom` (per parameter class), `RandomizeHistoryData`
   (probability per bar, max change as % of ATR), `RandomizeHistoryDataFixedRange`,
   `RandomizeSpread`, `RandomizeSlippage`, `RandomizeMinDistance`; `<MCBacktestPrecision>`;
   allowed confidence levels 50,60,70,80,90,92,95,97,98,99,100 only.

Other cross checks:

- **What If** (build 129): applies scenarios to the existing trade list (trade only certain
  days/hours, omit X% most profitable trades…), fast, result key `CrossCheck_WhatIf`; filter e.g.
  "What-If net profit ≥ 80% of main". Extensible with `WhatIf` snippets.
- **Opt. Profile / Sys. Param. Permutation** (build 114): runs an optimization of Recommended
  parameters up to a maximum number of tests, then evaluates the **Optimization Profile** (share
  of profitable runs, average profit > 0, uniform distribution, best within 1σ of average, stable
  landscape) and **SPP medians** (Walton/StatisTrade). Medians become databank columns via
  "From backtest".
- **Walk-Forward Optimization** and **Walk-Forward Matrix** as cross checks (see §6.3).
- **Sequential optimization** (build 132) as optimization type and cross check: parameters
  optimized one at a time, choosing the middle of the best *stable area* rather than the best
  fitness; if no stable area for a given % of parameters, the strategy is refused.

Cross checks are also usable in **Retester** without filtering; there they add sub-results to
the strategy. Runtime cost warning: 0.2 s strategy → 10–200 s with cross checks.

Sources: `06-cross-checks-robustness-tests/*.md`, `03-quick-start/types-of-robustness-tests-in-sqx.md`,
`07-optimization/sequential-optimization.md`, `digests/programming-for-sq-and-quantdatamanager.md` §3.3.

### 6.2 Optimizer — simple optimization (`07-optimization/simple-optimization.md`, `recommended-optimization-parameters.md`)

Load strategy (added as "Original strategy") → Settings → **Parameters**: `Automatic` (Value
distribution %, Maximum steps) or `Manual` (check parameter; Start/Stop/Step; Original value;
Total combinations; **Maximum optimizations** cap). Parameter categories: **Recommended
parameters** (periods, entry multipliers, used exit parameters — the only category in
cross-check optimizations), Period, Shift, Constant, Other, Entry level, Entry logic, Exit
used/unused, Boolean, **Trading options** (optimize max trades/day, time range…). `Put values
to variables` and `Generate symmetric variables` control parametrization. Method: brute force or
genetic. Store all optimizations | store only best. `!` marks odd-behaving parameter sets.
Fewer degrees of freedom = more robust.

### 6.3 Walk-Forward Optimization and Matrix (`walk-forward-optimization.md`, `walk-forward-matrix.md`, `description-advanced-walk-forward-values-can-used-filters-databank.md`)

- **WFO**: data split into N periods, each = optimization (IS) part + run (OOS) part; **Out of
  Sample %** and **Walk-Forward runs** (or exact days). Result = one WF result in databank; equity
  shows reoptimized (blue) vs original (grey); **Robustness score** = configurable component
  table + threshold % of components that must pass. Exact-OOS type `WF_TYPE_SIMIS_EXACTOOS` in the
  API; a faster **Simulation** WF type exists (10–100×).
- **WFM**: grid of WFOs over runs (Start/Stop/Increment) × OOS % (Start/Stop/Increment). Pass =
  a configurable cluster (e.g. 3×3 with ≥7 of 9 passing); SQX proposes the optimal
  reoptimization period (middle of the cluster). 3D charts (surface/bar/heatmap) of any value.
- WF-derived databank values ("From backtest" = WF Optimization / WF Matrix): standard stats
  from the WF equity; **WF Stability** (OOS vs IS per-day normalized %, for Net profit, Drawdown,
  Return/DD, Sharpe, Profit factor, Annual % return); **WF Score** (WF result vs original
  backtest %); specials: Max drawdown in one run, Max % DD in one run, Max profit in one run
  (and as % of total), Max stagnation %, Min trades in one run, Percentage of profitable runs.

---

## 7. Custom projects (Automation runner)

A custom project is an ordered list of **tasks** sharing any number of **databanks**; tasks read
from a Source databank and write to a Target databank; **Go To Task** with a condition creates
loops (e.g. until Final databank holds ≥100 strategies). Two shipped examples (forex, futures)
on Getting started. Project archive: `user/projects/<Project>/project.cfx` (ZIP: `config.xml` +
one XML per task).

| Task | Behaviour |
|---|---|
| Build strategies | standard Builder with its full settings; target databank |
| Retest strategies | retest a databank (optionally with cross checks) into another databank |
| Optimization | simple or Walk-Forward optimization + filtering |
| **Automatic retest** (v144 rewrite) | source → target databank; **Custom data** tab overrides per setting (engine MT4/MT5/TS/MC, additional charts, symbol list incl. `[StockGroup]`, precision, timeframe list, start/end day); every symbol × timeframe combination is tested; per-parameter mode **Strategy / Custom / Instrument** for Slippage, Spread, Swap, Commission, Min distance; optionally updates data first |
| Filtering | delete / copy / move strategies between databanks with optional conditions |
| Custom analysis | per-strategy and per-databank snippet methods (4 slots) |
| Create portfolio | merge a databank into a simulated portfolio |
| Load from files / Save to files | folder ↔ databank in supported formats |
| Clear databanks | empty a databank |
| Log databank stats | write databank statistics to the project log |
| Update data | download missing recent bars (project data or all) |
| Call external script | pause, run external program, resume |
| Wait for user/file | pause until user or until a file appears |
| Delete file | delete a path (pairs with Wait for / external scripts) |
| Notification | show notification / send email; optionally pause |
| Go To task | conditional jump (loops) |
| Stop & Start | stop when conditions met (count, time); optionally start another custom project (chains) |
| Plugin tasks | user task plugins (build 138+: `TaskXXXXX` + `SettingsXXXXX`, `task.xml` defaults) |

Sources: `09-custom-projects-and-tasks/*.md`, `03-quick-start/custom-analysis.md`,
`digests/programming-for-sq-and-quantdatamanager.md` §4.3.

---

## 8. Data manager

- **Instrument** = specification (data type stock/futures/forex/cfds/etf/index/crypto, pip/tick
  size and step, point value, default spread, default commission model). **Symbol** = a data
  series bound to an instrument (`EURUSD_M1`, clones `_M1_UTC2`, postfixes `_MT5`…). Import into
  an existing symbol overwrites.
- Sources: Dukascopy (tick/M1 download), file import (MT4 M1 CSV — only M1 imported, higher TFs
  computed; generic/tab/comma formats), Darwinex, crypto exchanges, Yahoo, **MT5 direct API
  import** (build 144: `terminal64.exe` folder, Market Watch symbols, range, broker profile,
  postfix; always creates a new symbol), SQ Futures / SQ Equities protected data.
- **Sessions** (for futures/equities; `23:59` end), **Stock groups** (`[group]` references in
  Automatic retest), **External indicators** (Name, Return type Number/Price/Price range/Signal,
  value lines, optional MT4/MT5/EL source code; values exported with `SqIndicatorValuesExportEA`
  and imported; CLI `-extindicators` from build 142 allows >3 values), **Broker profiles** (build
  141: IC Markets, RoboForex, PepperStone, OANDA, Darwinex, Dukascopy, 5ers, FTMO, Monevis;
  timezone conversion on download; MT5 script exports a broker's instruments/sessions XML).
- Export to MT4 FXT/HST for 99% tick backtests (MT4 closed during export; `mt4.properties`).
- Bar timestamp convention: MetaTrader data = **start of bar**; Tradestation/MultiCharts = **end of bar**.

Sources: `digests/programming-for-sq-and-quantdatamanager.md` §7, `digests/installation-features-templates-reliability-howto.md`
§3.3, §4.4, §8, `cli-command-line/03-commands-sq-qdm/*.md`.

---

## 9. AlgoWizard, templates and extensibility

- **AlgoWizard** is the visual rule editor (also used by Databank → Edit strategy). Its
  **Customize** menu holds **Custom blocks** (build 127: Block key, name, type Condition | Price
  level, body from existing blocks, parameters `chart/period/shift/int/double/bool` with
  Min/Max/Step that override global ranges, Opposite block negation) and **Random block groups**
  (Conditions / Values / Actions groups; a grouped placeholder yields exactly one condition;
  enforced strictly only under Random generation).
- **Strategy templates**: any strategy with placeholders `RandomCondition(name)`,
  `NegatedCondition(name)`, `RandomValue`, `RandomAction`; selected in What to build → Strategy
  from template; stored under `user/templates`.
- **Java snippets** (Code Editor → Create new / Clone / Compile / Compile all; restart SQX to see
  them): indicators (`IndicatorBlock`, `@BuildingBlock`, `@Indicator`, `@Parameter`, `@Output`,
  `OnBarUpdate()`), signals (`ConditionBlock`, `OnBlockEvaluate()`, explicit `Shift`), action
  blocks, databank columns (`DatabankColumn.compute/getValue`), trade-list columns, custom
  analysis (`CustomAnalysisMethod`: `filterStrategy`, `processDatabank`), trade-analysis charts,
  What-If, commissions, money management, engine charts, trading options. Each new block also
  needs Freemarker **code templates** per platform (`Code/<Platform>/blocks/<Block>.tpl`;
  right-click → Add all missing). Snippets/plugins are exchanged as **`.sxp`**. Custom JARs in
  `user/libs` (build 130). Python via Jython / external process / HTTP.
- **Plugins** (build 136 Dev 2+): Java servlet + AngularJS module in `user/extend/Plugins`,
  compiled to a jar; e.g. databank actions, settings tabs, custom project tasks.
- **Indicator Tester** compares SQ indicator values with MT4 CSV exports.
- Programmatic control from snippets: `ProjectEngine.get(project)`, `ISQTask.getConfig()` (clone!)
  / `setConfig(el, true)` + WebSocket `UpdateProject` refresh; `BacktestEngine`,
  `OptimizationEngine`, `ICrossCheck.runTest`, `ResultsGroup.merge`.

Sources: `08-strategy-templates-custom-blocks-and-indicators/*.md`, `programming-for-sq/**`,
`digests/programming-for-sq-and-quantdatamanager.md` §1–§5.

---

## 10. Command line (`sqcli.exe`) — complete reference

`sqcli.exe` (install root) runs the full SQX engine without UI; `qdmcli.exe` for QDM. Build 127+.
First run on a fresh machine: `sqcli.exe license=XXXXX`. Modes: one-shot (`sqcli.exe -cmd
key=value …`), interactive (no args; type `-h`, exit with `-exit`), batch `-run file=C:/cmds.txt`,
output redirect `> file`. `-gui` starts the engine plus the browser UI at `http://localhost:8080`.

| Command | Actions / arguments |
|---|---|
| `-project` | `action=list|start|stop|pause|resume|remove|status|loadconfig|saveconfig`, `name=<Project>`, `file=<config.cfx>` — e.g. `-project action=loadconfig name=Builder file=Builder.cfx` then `-project action=start name=Builder` |
| `-databank` | `action=list|count|save|load|delete|clear|create|remove|synctofiles|syncfromfiles|copy|move|export`, `project`, `name`, `folder`, `destproject`, `destdatabank`, `file` (csv/xlsx), `view`, `strategies="A,B"` |
| `-tools` | `action=orderstocsv|orderstoxlsx`, `file=<.sqx or folder>`, `output`, `usecomma`, `data=main|all` |
| `-extindicators` | `action=list|add|import`, `name`, `values=v1,v2…`, `type=10 Boolean|1 price|2 number|3 price range`, `file=<csv no header: Date,Time,O,H,C,L,V,Ext1…>` (build 142) |
| `-data` | `action=update|import|export|clone|timezones`, `symbol(s)`, `filepath`, `instrument`, `bartype=startofbar|endofbar`, `errorhandling=stop|ignore`, `timezone`, `timeframe=auto|Intraday|TICK|M1|M5|M15|M30|H1|H4|D1`, `datefrom/dateto=yyyy.MM.dd`, `outputdir`, `prefix`, `format`, `postfix`, `removeWeekends`, `hours` |
| `-symbol` | `action=list|add|edit|delete|clear`, `symbols`, `instrument`, `bartype`, `datatype=M1|TICK`, `datasource=dukascopy|file|darwinex|crypto|yahoo`, `exchange`, `postfix`, `name` |
| `-instrument` | `action=list|add|edit|delete`, `instrument`, `description`, `pointvalue`, `ticksize`, `tickstep`, `defaultspread`, `datatype=stock|futures|forex|cfds|etf|index|crypto` |
| `-execute` | `file=<script>` — run external program |
| `-waitfor` | `action=user|file`, `file=` |
| `-deletefile` | `file=` |
| `-run` | `file=<commands.txt>` |
| `-gui` | start web UI on 8080 |
| `-exit` | interactive exit |

Observed but not in these docs (runtime evidence recorded in the living plan and code): a second
`sqcli` instance while the GUI is open dies on port 5050 yet may exit 0 after printing a refusal;
`loadconfig` expects a Task-rooted `.cfx` (`Cannot load config. Invalid task config, missing
Task element` for a bare XML). Treat exit code 0 as insufficient; parse output.

Sources: `cli-command-line/**`.

---

## 11. Web UI, remote access and MCP

- The UI is a local web app served by the Java backend: Remote Access / `-gui` on **8080** (or
  first free port; override `<WebServerPort>` in `settings.xml`); Linux firewall ranges
  `8080-8099`, `5050-5059`.
- **MCP server** ships built in (first release, 2026 screenshots): HTTP at
  `http://localhost:8080/mcp`; dialog under application menu → MCP Server…; tools exactly:
  `list_projects`, `list_strategies`, `list_databanks`, `get_strategy_stats`, `run_project`,
  `stop_project`. Nothing else — do not invent MCP authoring methods.
- Servlets used by SQX's own Electron control panel and observed by TraderCockpit against the
  running program (not documented on the site; keep as runtime observations): `project/start`,
  `project/stop`, `project/pause`, `project/resume`, `project/getData`, `main/getData`,
  `constants/getAll`, `constants/list`, `constants/listCommissionMethods`, `buildType/listFiles`,
  `buildType/getTemplateConfig`, `fitnessMethodStrategyResult/list`, `indyTester/calibrate`,
  `engine/getTypes`, `engine/saveSelection`, `engineCharts` (WebSocket), TASKMANAGER
  `customProjectStats` (WebSocket), `data/getSymbolData`, `overview/getOverviewContent`,
  `resultsCharts/loadChartData`, `sourcecode/saveEA`, `sourcecode/getDataPath`,
  `sourcecode/saveMTPaths`; plugin servlets register their own context paths.

Sources: `zz-unlisted/mcp-integration.md`, `cli-command-line/03-commands-sq-qdm/gui-starts-webserver-to-access-gui-remotely.md`,
`digests/installation-features-templates-reliability-howto.md` §1.8, §1.11, §6; `product/tradercockpit/sqx_*.py`.

---

## 12. Reliable backtesting and engine differences (summary)

- Copy SQ custom indicators into the platform (`custom_indicators/...`); use the same data and
  timezone as the broker; match engine; MT data = start-of-bar, TS/MC = end-of-bar; clear
  `internal/testfiles` when results diverge; small differences remain normal.
- TS/MC: only Selected-timeframe precision; time exits at `00:00` or an existing bar; subcharts
  untested; no independent same-direction multi-entry exits; Pivots/Fibo less tested.
- JForex: out of product scope (see §14.1); its doc stays for reference only.
- MT5 futures: `MarketOpenSession` (build 140) with Data manager sessions.
- Stockpicker: daily OHLC only, evaluation timing modes, pessimistic SL when SL and PT both hit.
- Multi-order same direction needs unique MagicNumber per entry in MT; not supported in TS/MC.
- "Strategy tried to place stop/limit order at incorrect price" is an informational skip.

Source: `10-reliable-backtesting-trading/*.md`, `digests/installation-features-templates-reliability-howto.md` §8.

---

## 13. Build / version gates worth remembering

114 Opt. Profile & SPP · 127 CLI, custom blocks, external indicators, split portfolios ·
128 `debug()/fdebug()` · 129 What-If · 130 ATM (experimental), Fit to existing portfolio,
Compare, custom JARs · 131 `user/extend` layout, Generate ATM (Ultimate), Custom analysis,
advanced calibration · 132 Sequential optimization, `fixDPI.bat` · 136 task config API, plugins
· 138 Portfolio Master, task plugins · 139 Benchmark · 140 `MarketOpenSession`, plugin example
updates · 141 Portfolio Composer, Broker profiles · 142 CLI `-extindicators` · 144 Automatic
retest rewrite, MT5 direct import, Results-plugin `CLAUDE.md`, MCP (dialog screenshots 2026).
Installed authority for this product: **144.2953**.

---

## 14. How SQX concepts map onto TraderCockpit

The platform never re-implements SQX quantitative behaviour; it configures, launches, reads back
and presents it. This table ties the official concept to the product code that already touches
it, so new UI work binds to the right native seam.

| SQX concept (docs) | Native seam | TraderCockpit module(s) | Product surface |
|---|---|---|---|
| Program layout modules | left rail | `web/sqx-modules.mjs`, `sqx_run_module.py` | `Getting started | Builder | Data manager | Custom projects | Apollo | Operate | Settings` |
| Builder Full settings panes (§4) | task XML inside `project.cfx`; approved `Build-Task1.xml` | `sqx_builder_config.py`, `sqx_custom_project_settings.py`, `sqx_settings_lists.py`, `research_configurations.py` | Research Specification/Build; Automation Full settings |
| What to build types, template Browse/Reload, fitness list, constants | `buildType/*`, `fitnessMethodStrategyResult/list`, `constants/*` | `sqx_native_web.py`, `sqx_settings_lists.py` | Full settings dropdowns/radios |
| Building blocks + calibration | `<Blocks>`; `indyTester/calibrate` | `sqx_calibrate.py`, `web/research-blocks.mjs`, `web/automation-full-settings.mjs` | Building blocks pane, Calibrate |
| Random vs Genetic build mode | `BuildMode` / `generationType` | `web/research-evolution.mjs`, `web/research-specification.mjs`, `research_configurations.py`, `sqx_builder_config.py` | Evolutionary Search; Genetic options tab |
| Ranking, dismissal, cross-check acceptance conditions | `<Rankings>`/`<CrossChecks>` `AcceptanceSettings` | `web/research-rankings.mjs`, `web/research-cross-checks.mjs`, `web/research-validate.mjs`, `research_verdicts.py` | Ranking / Cross checks panes; Test & Validate; cockpit verdict |
| Progress: Start/Pause/Stop, logs, engine charts | `project/start|stop|pause|resume`, `engineCharts`, TASKMANAGER stats, `sqcli -project` | `sqx_gateway.py`, `sqx_custom_project_launch.py`, `sqx_engine_progress.py` | Automation Progress; Build launch |
| Databank strategies, `.sqx` archives | `user/projects/<P>/databanks/<Bank>/*.sqx` | `sqx_outputs.py`, `sqx_custom_project_strategy.py`, `research_candidates.py` | Candidates; Automation Results |
| Results → Overview / List of trades / Equity / Trades on chart / Source code / Strategy config | `orders.bin`, `settings.xml`, `overview/getOverviewContent`, `resultsCharts/loadChartData`, `sourcecode/*` | `sqx_orders.py`, `sqx_results_overview.py`, `sqx_results_chart.py`, `sqx_sourcecode.py`, `research_trades.py` | Test & Validate tabs; Automation Results |
| Results plugins (Prop analytics, Prop MC…) | `user/extend/ResultsPlugins` | `sqx_results_plugins.py`, `capability_registry.py`, `native_plugins/` | Results plugin tabs; capability catalog |
| Retester / Higher precision cross check | isolated `TraderCockpit-Retester-*` project, `SQTradingLib.jar` provenance | `research_retester.py`, `research_robustness.py` | Backtest Overview / Robustness |
| Custom projects & tasks (§7) | `project.cfx` topology | `sqx_custom_project.py` | Automation (Custom projects list, Tasks) |
| Data manager | fail-closed unless native evidence | `sqx_runtime.py`, `runtime_status.py` | Data manager rail entry |
| CLI / runtime trust | `sqcli.exe` digest, build markers | `sqx_runtime.py`, `native_runtime_config.py` | Settings → Runtime source |
| MCP (6 tools) | `http://localhost:8080/mcp` | none — the product uses servlets/CLI, not an SQX MCP | — |

What the docs do **not** settle and must be observed on the installed 144.2953 runtime:
numeric sample-type / direction / P/L enum ids, the exact `loadconfig` archive layout, servlet
request/response shapes, `orders.bin` binary layout, Electron `browserToken` handling, the heap
config key, the `.sqx` internal file list, and any behaviour of features newer than the docs.

---

## 14.1 Product scope: code-replication targets (owner decision 2026-09-04)

TraderCockpit targets **MetaTrader 4, MetaTrader 5, TradingView, and Python**. JForex,
Tradestation/MultiCharts and NinjaTrader are out of scope for delivery (their docs stay in the
mirror for engine-behaviour reference only).

| Target | What SQX natively produces | Product seam | Verification owner |
|---|---|---|---|
| MetaTrader 4 | `Expert Advisor for MetaTrader4 (*.MQ4)` from Results → Source code (`sourcecode/print`, `sourcecode/saveEA`), plus `custom_indicators/MetaTrader4` | `sqx_sourcecode.py` formats `mq4`, `pseudo`, `xml`; Save as EA / MT folder configure | SQX produces the code; the MT4 Strategy Tester confirms it (`11-how-to/export-strategy-strategyquant-test-trade-metatrader.md`) |
| MetaTrader 5 | `Expert Advisor for MetaTrader5 (*.MQ5)` (hedging/netting engines), `custom_indicators/MetaTrader5`, `MarketOpenSession` for futures | same, format `mq5` | MT5 Strategy Tester; futures sessions from Data manager |
| TradingView (Pine Script) | **nothing native.** SQX's own route is the *Source Code Translator* Results plugin (in our packaged catalog, `native.source-translator`): it requests the native source over `postMessage` `GET_SOURCE_CODE` and asks OpenAI (user's key, in-browser) for `Pine Script v6 (TradingView)` | platform-owned **translation** of the exact native Pseudo code / Strategy XML through the bounded Apollo/OpenRouter path, hash-bound to the native source revision and labeled unverified | the owner backtests the Pine script in TradingView; TraderCockpit never claims the translation reproduces SQX results |
| Python | **nothing native** (Python appears in SQX only as a snippet bridge, §9). The same translator plugin offers `Python (backtrader)` and `Python (Zipline)` | same translation path; target framework is a user choice recorded with the translation | the owner runs the Python backtest; results are a separate, explicitly scoped evidence source |

Rules that follow: MT4/MT5 code is producer truth and is shown verbatim; Pine/Python code is a
derived artefact of the platform's assistant, must cite the native source hash it was derived
from, must not be presented as producer-verified, and must never feed a TraderCockpit backtest
engine (none exists). The vendor plugin sends source to OpenAI from the browser with a
user-pasted key; the product does the equivalent server-side through the operator/consumer
OpenRouter boundary instead, so no key reaches the browser.

## 15. Facts that must not be gotten wrong

- Random generation and Genetic evolution are two build modes of one Builder; Genetic options
  appear only in genetic mode; Parts to improve only for Improve existing strategy.
- Fitness is 0–1 and is a ranking/selection artefact; databank capacity is finite; dismissal
  rules run at 40% of data with a 25% trade threshold.
- Cross checks are a funnel; the order matters; each adds a sub-result; conditions reference
  columns "From backtest" main / cross check / WF.
- Precision names: Selected timeframe only · 1 Minute data · Real tick custom spread · Real tick
  real spread (+ Trade on bar open). TS/MC engines support only Selected timeframe.
- Data range parts: IST, ISV, OOS, No Trade. ISV drives genetic restart on stagnation.
- ATM overrides every exit except Stop Loss.
- `.sqx` = strategy (ZIP incl. `orders.bin`); `.cfx` = project/config (ZIP incl. `config.xml` +
  task XMLs); `.sxp` = snippet/plugin package; `Strategy XML` is a Source-code output option.
- `user/projects/<Project>/project.cfx` is the project; the CLI `-project` name is the folder.
- MCP has six tools; the UI/MCP port is 8080 by default; the CLI needs a verified license once.
- Only M1 (or tick) is imported; higher timeframes are computed by SQX.
- Timestamps: MetaTrader start-of-bar, Tradestation/MultiCharts end-of-bar.
- Never treat a backtest metric as live performance — the docs themselves insist on it.
