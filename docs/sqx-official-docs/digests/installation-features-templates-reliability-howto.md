# SQX Official Docs Digest — Installation, Features, Templates, Reliable Backtesting, How-to

Scope: every Markdown file under `docs/sqx-official-docs/strategyquant/` in directories `02-installation/`, `04-new-features/`, `08-strategy-templates-custom-blocks-and-indicators/`, `10-reliable-backtesting-trading/`, `11-how-to/`, `12-sq-4-business-mql-market/`, `zz-unlisted/` (46 files, fetched 2026-09-04). Facts only; where the docs are silent or contradictory this is stated explicitly. Source paths are relative to `docs/sqx-official-docs/`.

Note on duplicates: `zz-unlisted/custom-data-indicators.md` is byte-for-byte the same content as `08-.../external-indicators.md`, and `zz-unlisted/developing-strategies-using-custom-strategy-templates.md` is the same content as `08-.../strategy-templates.md` (different source URLs, identical body).

---

## 1. Installation, data migration, folder structure

### 1.1 Installer (current installer, screenshots dated 2026/05)

- Welcome screen offers **Install** (fresh install) or **Migration Tool** (copy data from an existing install without installing again).
- Installer screen: **installation folder** default `C:\StrategyQuantX<version>` — the folder must be empty; checkbox *I agree to the Licensing and Service terms* enables Install; optional *Create desktop shortcut*.
- *What's new in this build* opens the changelog; from the **Changelog** you can click **Install this version** on any listed build to install that specific build. Each version installs into its own separate folder.
- Installer downloads the package (shows MB, %, speed), verifies checksum, extracts, and **sets an optimized Java heap size based on your RAM**.
- After install: **Yes, start the copying process** (opens Migration Tool) or **Finish setup**.
- *Proxy settings* link at bottom of installer screen (use before download if a proxy is required).
- Installer writes `InstallatorLog.txt` into the installation folder; deleted automatically on success, kept on error.

Source: `strategyquant/02-installation/how-to-install-strategyquant-x-and-transfer-your-data-from-a-previous-version.md`

### 1.2 Migration Tool

- Old installation is never modified; data is **copied**, not moved.
- **Source folder** = root of old install (folder containing `StrategyQuantX.exe`, e.g. `C:\StrategyQuantX143`). **Destination folder** = new install (auto-filled). Three categories checked by default; click **Start Cloning**.
- SQX must be closed (file locks). `user\data` can be several GB.
- **Advanced options** per-item table (default ON unless noted):

| Item | Default | Copies |
| --- | --- | --- |
| Historical data | ON | `user\data` (instruments and timeframes) |
| External indicators data | ON | `user\customdata` |
| Projects | ON | `user\projects` (all strategies, databanks, projects) |
| Strategies | ON | only `.sqx` strategy files; uncheck to copy project structure without strategies |
| Templates | ON | `user\templates` |
| Views | ON | databank, trade list, walk-forward and other saved views |
| Settings | ON | `user\settings\settings.xml` (application settings, recent paths) |
| Extend folder | ON | `user\extend` (custom indicators, code snippets) |
| Custom blocks | ON | `user\settings\customBlocks.xml` |
| Random groups | ON | `user\settings\blockGroups.xml` |
| SQ4Business | ON | `user\sqxbusiness` |
| App configs | **OFF** | `StrategyQuantX.config`, `sqcli.config`, `CodeEditor.config` — off because the new install sets an optimized memory config that the old files would overwrite |
| Java runtime (j64) | **OFF** | replaces bundled Java runtime with the old one; leave off unless customized |

Source: `strategyquant/02-installation/how-to-install-strategyquant-x-and-transfer-your-data-from-a-previous-version.md`

### 1.3 Manual data migration (older how-to)

- Never install a new SQX into the same location as a previous install; always use a new destination.
- Back up the `user` folder (close SQX first). Migrate only **one version up** (e.g. build 141 → 142); larger jumps (e.g. 1.38 → 1.42) "most likely will not work".
- Most user data lives in `<install>/user/`. Folders/files listed by the doc:
  - `settings/blockGroups.xml` and `settings/customBlocks.xml` — **NOTE: this doc labels `blockGroups.xml` as "Custom Blocks" and `customBlocks.xml` as "Custom Groups", which is the reverse of the installer table in 1.2 (`customBlocks.xml` = custom blocks, `blockGroups.xml` = random groups). The docs conflict; the file names themselves are consistent.**
  - `data` folder (and possibly `custom_data/`) — historical data feed
  - `extend` — custom indicators and snippets; may need recompiling in the code editor after a Java version change
  - `projects` — strategies, databanks, custom projects
  - SQ4Business projects (video: a "StrategyQuant for Business" directory in `user`)
  - `settings/` — settings, default conditions, config files, databank views; backups of custom blocks/groups are in `settings` under "block groups – backups" and "custom blocks – backups"
  - Custom Data folder — external indicator data
  - Templates folder
  - Databank views: `<install>\user\settings\views\`
  - `<install>\user\strategies\` — only if you saved strategies there (not default)
- Video transcript: data is auto-synchronized to the `projects` directory every hour (period can be shortened); logs need not be migrated.

Source: `strategyquant/11-how-to/data-migration-between-strategyquant-version.md`

### 1.4 Downgrade

1. Download older build ZIP from the Download page, section **Previous builds**.
2. Rename current install folder (e.g. `C:\StrategyQuant` → `C:\StrategyQuant_backup`).
3. Extract the older ZIP to `C:\StrategyQuant`.
4. Copy the whole `/user` folder from the backup into the new folder; start `StrategyQuantX.exe`.
- Builds are **backwards compatible, not forward compatible** (a `/user` from build 126 copied into build 120 may not be recognized properly).

Source: `strategyquant/11-how-to/how-to-downgrade.md`

### 1.5 Switching license

- Go to **About** and change the license; restart SQX after applying.

Source: `strategyquant/11-how-to/how-to-switch-license-in-current-installation.md`

### 1.6 Memory settings

- SQX picks a max RAM on first start (a portion of available RAM); as a Java app it cannot exceed the configured maximum.
- UI path: top-right configuration icon → **Memory** tab → set RAM → restart SQX. Recommended 80–100% of physical RAM (16 GB → 12–16 GB). Value is a maximum, not a guarantee of usage.
- The docs do not name the parameter/file for the heap value in this article; the installer doc says memory config lives in `StrategyQuantX.config`, `sqcli.config`, `CodeEditor.config` (see 1.2). Exact key name not stated.

Source: `strategyquant/11-how-to/starting-sq-with-more-memory.md`

### 1.7 Debug logging

- File: `<install>/internal/web/SQUANT/log_config.xml` (logback).
- Change `<root level="INFO">` to `<root level="DEBUG">`.
- FILE appender pattern: `./user/log/StrategyQuant/log_%d{yyyy_MM_dd}.log` (daily rolling). Loggers `oshi.util.platform.windows.WmiUtil` and `org.eclipse.jetty` pinned at `INFO`.

Source: `strategyquant/11-how-to/switching-logs-to-debug-mode.md`

### 1.8 Internal web server port

- Add `<WebServerPort>` to `<install>/user/settings/settings.xml`, e.g. `<WebServerPort>4971</WebServerPort>`.
- Default port is not stated in this article. The MCP article states Remote Access/MCP default is **8080** or the first available port; the Linux firewall article lists UI ranges `8080-8099` and `5050-5059`.

Source: `strategyquant/11-how-to/manually-configure-internal-web-server-port.md`

### 1.9 GPU acceleration

- Default: enabled. UI: options → **Configuration** → GPU acceleration switch → restart SQX.
- File: `C:/StrategyQuantX/user/settings/settings.xml`, key `<gpuAccelerated>true</gpuAccelerated>` → `false`.

Source: `strategyquant/11-how-to/how-enable-or-disable-gpu-acceleration-in-strategyquant-x.md`

### 1.10 Blurry UI (Windows 10 scaling ≠ 100%)

- From SQX **build 132 dev2**: run `<install>/fixDPI.bat` once.

Source: `strategyquant/11-how-to/fixing-blurry-user-interface.md`

### 1.11 Linux firewall

Ports used by SQX: `25,465,587` (SMTP), `443`, `80` (Web), `8080-8099` (User interface), `5050-5059` (User interface). Ubuntu ufw: `ufw allow 8080:8099/tcp`, `ufw allow 80`, `ufw allow 443`, `ufw allow 25`, `ufw allow 465`, `ufw allow 587`, `ufw allow 5050:5059/tcp`; verify with `ufw status`.

Source: `strategyquant/02-installation/setting-up-firewall-for-strategyquant-x-on-linux.md`

### 1.12 AWT dll problem

- Symptom: SQX cannot launch; "awt.dll can't find dependent libraries". Fix: install all packages from the Microsoft Visual C++ Redistributable page (`https://docs.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-160`), reboot if needed; support: `support@strategyquant.com`.

Source: `strategyquant/02-installation/how-to-solve-an-issue-when-strategyquant-x-is-not-able-to-launch-awt-ddl-cant-find-dependent-librearies.md`

### 1.13 Build 133 → 134 upgrade problems

- Updater bug caused multiple issues; recommended fix was waiting for build 135, or fresh install from the 134 ZIP, or manual deletes:
  - MT4 compile problem → delete `internal/extend/Snippets/SQ/TradingOptions/UseInitialSL.java`
  - Custom project auto-changes symbols/timeframes → delete folders `internal/plugins/SettingsSQXRangerStart` and `internal/plugins/TaskSQXRangerStart`
  - Crash loading plugins → delete `internal/web/PAYMENTDIALOG`
- Script: `https://cdn.strategyquant.com/install/fixBuild134.bat`, copied into the install folder and run there.

Source: `strategyquant/zz-unlisted/problems-after-upgrading-from-build-133-to-134.md`

---

## 2. Results plugins

### 2.1 What they are

- Custom Results plugins are **static HTML files loaded into the Results tab** of a strategy. Each plugin is its own subfolder under `user/extend/ResultsPlugins/` containing a single `index.html`; communication with SQX is **exclusively via `postMessage`** (PostMessage API); body must use `overflow:hidden`; Vue 3 is supported via a **local** runtime file `vue.global.prod.js` (SQX runs offline; all dependencies must be local).
- From **build 144**, the SQX team ships a `CLAUDE.md` in `user/extend/ResultsPlugins/` documenting plugin architecture, the complete PostMessage API, available stats fields, theming rules, disclaimer requirements and tooltip patterns, intended for AI-assisted ("vibecoding") plugin authoring with Claude Code.
- License: **Pro or Ultimate**; **Starter does not support custom plugins**.
- Enabling/reload: close and restart SQX, or click the **reload icon in the Results tab**; the plugin then appears as a tab alongside existing plugins when a strategy is opened from a Databank. Debug via right-click inside plugin → Inspect (DevTools).
- PostMessage messages named in the doc: `GET_STATS` (stats; can be called for both directions / long only / short only), `GET_ORDERS` (full order list), `SET_THEME` (light/dark skin change), `SET_LANGUAGE` (i18n). Locale files are flat JSON in a `locales/` subfolder.
- No plugin-specific settings file is described; per-plugin inputs live inside the plugin UI.

### 2.2 Plugins named in the doc

| Name | Type | Location | What it shows |
| --- | --- | --- | --- |
| **Prop analytics** | shipped example ("official") | `user/extend/ResultsPlugins/Prop analytics/` | Prop-firm evaluation dashboard (FTMO, TopStep style). Inputs: Starting Capital, Daily DD Limit, Contract Size, Max Leverage, Post-Streak Threshold. Cards: Prop Firm Constraints, Daily Profit & Loss, Margin & Leverage Risk (True Leverage), Post-Streak Edge, Drawdown Duration, Holding Time Behavior, Consistency Score, Tail Risk (95%) VaR/CVaR, Regime Performance. Uses `GET_ORDERS`; 12 locales (en, cs, de, es, fr, id, it, pl, pt, ru, zh-CN, zh-TW). |
| **Prop Monte Carlo** | shipped example | `user/extend/ResultsPlugins/Prop Monte Carlo/` | Monte Carlo reshuffle of trade sequence vs prop-firm rules; inputs: iterations, confidence level, starting capital, daily loss limit, max drawdown limit, profit target; canvas chart; pass/fail probability; same 12 locales. |
| **Robustness Scorecard** (`RobustnessScorecard/`) | tutorial example, downloadable | `ResultsPlugins/RobustnessScorecard/` | 0–100 score across six robustness pillars, letter grade, verdict, pillar bars; three parallel `GET_STATS` calls; <100 trades caps Sample Size pillar at 30; Symmetry pillar penalizes one-sided strategies. |

- Additional idea list (not shipped): Traffic-Light Checklist, Overfitting Risk Meter, IS/OOS Degradation Analyzer, SQN-Based Grading, Monte Carlo Stress Score, Tail Risk & Fat Trade Detector, Time Consistency Analyzer, Risk-Adjusted Ratio Panel, Robustness Radar Chart.
- The doc does **not** list the built-in (non-plugin) Results tabs.

Source: `strategyquant/04-new-features/results-plugins.md`

---

## 3. Automatic portfolio construction, Portfolio Composer, Broker profiles

### 3.1 Portfolio Composer (module, **Build 141**)

- Purpose: simulate a portfolio of strategies including **weights** (money allocated per strategy) to find the optimal composition. Created with Stockpicker/stock strategies in mind but usable for other assets.
- Difference vs **Portfolio Master**: Portfolio Master only chooses composition (brute force or genetic). Portfolio Composer also computes **weight**, recomputing position sizing per weight.
- UI: Portfolio Composer module; load strategies into the grid on the left panel; select strategies and weights; **Recompute portfolio** button; right panel shows **Overview**, **List of trades**, **Equity chart**, and a **PortfolioComposer Log** (daily actions: orders taken, size modifications, orders skipped for low free margin).
- Weight semantics: 100% = original MM; 200% = 2×; 50% = half. Example: Risk 10% of account at 250% → 25% per trade; Risk $1000 at 200% → $2000.
- Simulation also uses portfolio **Account balance and leverage** ($10,000 with leverage 2 → effectively $20,000) and **free margin**: each day it opens only as many trades as free margin allows; a strategy that cannot open due to margin is skipped that day and the next strategy is considered.

Source: `strategyquant/04-new-features/portfolio-composer.md`

### 3.2 Automatic portfolio construction (Automatic Computation in Portfolio Composer)

- Removes manual weighting; uses **Markowitz Efficient Frontier** model ("more will be added"). Optimal portfolio = highest Sharpe Ratio (Expected Return / standard deviation).
- Metrics: daily return, strategy volatility (std dev of daily returns, 95% confidence), **VaR** at 95% (z = 1.65, σ_portfolio, horizon T), plus **Expected Shortfall / CVaR**.
- Method 1 — single portfolio: after **Recompute**, the log's last line per strategy shows Daily expected return, Daily Standard Deviation, Value at Risk, correlation between strategies; portfolio-level: Daily expected return, Daily Standard Deviation, VaR, Sharpe Ratio, Expected Shortfall.
- Method 2 — Automatic Computation tab: select strategies, configure **Money Management** tab (example: leverage 100), choose **fitness** and **number of simulations** (example: 500 or 1000), **Risk-Free Rate**. Fitness types named: **Sharpe Ratio**, **Return/Drawdown Ratio**, **CAGR vs Max Drawdown**, **CAGR vs Average Drawdown**. Result chart: optimal portfolio highlighted **yellow**, minimum-risk portfolio **green**; optimal portfolios lie on the frontier line.
- AlgoCloud Stockpicker: in **Configuration – Account MM** set **leverage = 1**; initial capital $25,000 suggested; slower because each strategy spans hundreds of instruments.
- No file outputs are described.

Source: `strategyquant/04-new-features/automatic-portfolio-construction.md`

### 3.3 Broker profiles (**Build 141**)

- A "profile" for a broker capturing time zone, instrument settings (point value, tick step, etc.) and trading sessions, to make backtests match the broker.
- Two functions: (1) limit the stock universe traded in the **Stockpicker** engine (example: XTB subset of stocks); (2) use broker instrument settings and time zone instead of SQX defaults (MetaTrader 5 functionality — broker-specific **Instruments** and **Sessions**).
- Predefined profiles shipped in 141: **IC Markets, RoboForex, PepperStone, OANDA, Darwinex, Dukascopy, 5ers, FTMO, Monevis**.
- Data download from **Dukascopy** or **Darwinex**: a broker profile can be selected when adding data; choosing a non-"SQ Default" profile lets you pick instrument settings, and **downloaded data are converted from source timezone to the broker's timezone**. Caveat: this does not resolve data differences between sources; broker data from MT5 recommended. Point values are computed in USD; other account currencies require manual update.
- Import own broker settings: MT5 script (originally by user Karish, now SQ-maintained) iterates Market Watch symbols and outputs instrument info + sessions to XML files, importable into the broker profile. Script path: `<install>\custom_indicators\BrokerProfileInstrumentsSessionsScripts\Update_SQX_Instruments_information.ex5`.

Source: `strategyquant/04-new-features/broker-profiles.md`

---

## 4. Strategy templates, custom blocks, external indicators, random groups, parameter ranges

### 4.1 Strategy templates

- A strategy template is a strategy with **Random placeholders** that SQX fills randomly. Placeholders: **`RandomCondition`**(*name*, e.g. `RandomConditionLong`) and **`NegatedCondition`**(*name*) which inserts the negation of the named random condition (used so Short = negated Long). Other placeholder types referenced in the random-groups doc: `RandomValue` (price/numeric values, e.g. Stop/Limit entry price) and `RandomAction` (entry order and other actions).
- Default generation source for `RandomCondition`: **Full settings → Building blocks** plus What-to-build settings (number of conditions, Period and Shift ranges).
- Templates are edited in **AlgoWizard**. The docs do **not** specify the template file format; the only stated location is the `user\templates` folder (installer table) / "Templates" folder (migration doc).

Source: `strategyquant/08-strategy-templates-custom-blocks-and-indicators/strategy-templates.md` (duplicate: `strategyquant/zz-unlisted/developing-strategies-using-custom-strategy-templates.md`)

### 4.2 Custom blocks (visual, since **Build 127**; alternative is a Java snippet)

- UI: embedded **AlgoWizard** → **Customize** menu → **Custom blocks** tab → **Add block**.
- Fields: **Block key** (unique ID; letters and numbers only, no spaces/special chars; e.g. `CCIRising3Bars`), **Block name** (display name in Building blocks), **Block type**: **Condition** (true/false; appears in Signals section of Building blocks) or **Price level** (price for stop/limit orders; appears in Stop/Limit Price Levels under Stop/Limit Entry blocks).
- Body built with **Add another condition** using standard building blocks; comparison blocks e.g. **Is Greater (>)**, **Is Lower (<)**.
- **Parameters** section: mandatory `Chart`; add via **Add parameter**; types: `chart`, `period` (bounded by generator period range), `shift` (bar index; 0 = current bar; bounded by shift range), `int`, `double`, `bool`. Bind a parameter by opening a condition and clicking the blue **[…]** next to the indicator parameter.
- Saved blocks appear in AlgoWizard editor and Builder Building blocks; source code generation for target platforms works automatically because the block is composed of existing blocks.
- **Opposite block**: click **Not set** link to create/choose the opposite block; SQX creates it by **negating** the original once. If no opposite is defined the same block is used for both directions. Later edits are not propagated; on the **opposite** block click **Negate from opposite block** to re-derive.
- Storage: `user\settings\customBlocks.xml` (installer table).

Source: `strategyquant/08-strategy-templates-custom-blocks-and-indicators/custom-blocks.md`

### 4.3 Parameter ranges (standard and custom blocks)

- Standard blocks: **Builder → Building block** settings, per-block ranges; by default Period and Shift use **global** ranges from **Builder → What to build**; can be switched per block to a custom range.
- Custom blocks: their `period`/`shift` parameters have **Min, Max, Step**; these **take precedence** over Building blocks settings (example: 33–66 step 3). Set **Min = Max = Step = 0** to fall back to the global What-to-build ranges.

Source: `strategyquant/08-strategy-templates-custom-blocks-and-indicators/configuring-parameter-ranges-for-standard-and-custom-blocks.md`

### 4.4 External indicators (formerly "Custom Data Indicators"; since **Build 127**)

- Two options for unsupported indicators: (1) implement in Java in SQX CodeEditor; (2) **import indicator values as data** (this doc). Imported values are valid only for the exact symbol + timeframe they were computed on, and SQX and the source platform must use the **same history data**.
- Step 1 — **Data manager → External indicators → Add new**: **Name** (unique, no special chars; recommend embedding symbol/TF e.g. `RVI_EURUSD_H1`); **Return type**: `Number`, `Price`, `Price range`, `Signal`; **Indicator values (lines)** in the same order as the indicator outputs; optional per-line source code for MT4, MT5, EasyLanguage (needed only for code generation), e.g. `iCustom(NULL, 0, "RVI", 10, 0, #Shift#)` — `#Shift#` is replaced at generation for MT4; MT5 does not need it.
- Step 2 — export values with EA `SqIndicatorValuesExportEA` from `<install>\custom_indicators\MetaTrader5\Experts` (doc path rendered without separators: `{StrategyQuant}custom_indicatorsMetaTrader5Experts`); copy to MT5 `MQL5\Experts`; edit `OnInit` (`fileName = "EURUSD_H1_RVI_10.csv"`, `indicatorHandle = iCustom(NULL, 0, "Examples\RVI", 10)`), `OnTick` (`indicatorBufferIndex` / `FillArraysFromBuffers(...)` per line; buffers `IndicatorBuffer[]`, `SignalBuffer[]`), and the `FileWrite(handle, currentTime, open, high, low, close, tick_volume, IndicatorBuffer[0], SignalBuffer[0])` call. Run in MT5 Strategy Tester; output CSV is under the **Tester** agent folder `…/MQL5/Files`, not the Terminal data folder.
- Step 3 — **Import indicator data**: map columns (last two → `Value1`, `Value2`), **Start import**.
- Step 4 — appears in **Builder → Settings → Building blocks** only when **Settings → Data** timeframe matches the imported TF.
- Storage: `user\customdata` (installer table).

Source: `strategyquant/08-strategy-templates-custom-blocks-and-indicators/external-indicators.md` (duplicate: `strategyquant/zz-unlisted/custom-data-indicators.md`)

### 4.5 Random groups

- Specify the block set used for a specific placeholder instead of the global Building blocks. Managed in **AlgoWizard → Customize** (Random block groups editor). None exist by default.
- **Add group**: name + type — **Conditions** (for `RandomCondition`), **Values** (for `RandomValue`), **Actions** (for `RandomAction`). Add blocks via **Add block** or copy/paste from the editor; **Save**.
- In the template, configure a `RandomCondition` to use a group (example name `RandomFromGroup1`, group `Group 1`). Semantics: blocks in a group need **not** be selected in Builder Building blocks; a grouped placeholder generates **exactly one** condition (no and/or chaining); a group-less placeholder follows **Full settings → What to build → # of Conditions** and can chain and/or. Parameters generate as specified in the group — fixed values stay fixed (e.g. `CCI(18) > 0`), random ones are generated from configuration.
- Genetic evolution limitation: crossover/mutation are **not** constrained by random groups/custom blocks beyond the initial population; use **Random Generation** for strict block control.
- Storage: `user\settings\blockGroups.xml` (installer table).

Source: `strategyquant/08-strategy-templates-custom-blocks-and-indicators/random-groups.md`

### 4.6 OppositeBlocks configuration (negation override)

- Default negations are defined in block Java snippets (not UI-configurable): `>`↔`<`, `=`↔`<>`.
- Override file: `<install>\user\settings\OppositeBlocks.csv` (does not exist by default); reference example `OppositeBlocks_example.csv` lists defaults. Format: one `Block;OppositeBlock` pair per line using Java snippet names (visible in CodeEditor), e.g. `Equals;NotEquals` / `NotEquals;Equals`. Setting `Equals;Equals` makes `=` negate to `=`. **Restart SQX** after changes.

Source: `strategyquant/11-how-to/use-oppositeblocks-configuration-to-control-the-negation.md`

---

## 5. Automatic dismissal rules

- Builder automatically dismisses strategies with "wrong" properties (enabled by default). Configure in **Builder → Rankings → Configure automatic dismissal** link. The doc says "8 different strategy checks" but lists **10** (the last two were evidently added later; the doc is from the SQ4 era):

| Rule | Meaning |
| --- | --- |
| No trades | strategy has no trades |
| Too many ambiguous trades | too many trades that begin and end on the same bar (backtest cannot be accurate) |
| Too many open trades | more than 100k open trades in parallel |
| No filled trades | too many pending orders never filled |
| Zero PL trades | too many trades with zero P/L |
| Zero duration trades | trades closed right after opening |
| Unfinished trades | trades never closed, run to end of test |
| Too little trades | fewer than 20 trades |
| Outlier trade | one trade's profit > 2 × (second + third best profit) |
| Too many trades closing at the same bar | trades open and close inside the same bar |

- Checks run after **40%** of history data is processed. Trigger threshold for "too many" rules: **25%** of all placed trades.
- Any check can be disabled (not recommended). Common causes: SL too small; incorrect End-of-day / Trade range settings.
- Dismissal statistics dialog (link in Builder) shows counts per reason including custom conditions.

Source: `strategyquant/zz-unlisted/understanding-automatic-dismissal-rules.md`

---

## 6. MCP integration

- SQX ships a **built-in MCP server** exposing projects, strategies, databanks to MCP clients (Claude, OpenAI, Gemini named). Described as the **first release**; toolset covers project management and strategy inspection. **The doc does not state the build number that introduced it** (screenshots dated 2026/05).
- Server starts automatically with SQX; **shares the port with Remote Access — default 8080, or first available port**. Exact URL shown in the **MCP Server…** dialog (application menu, gear icon top-right), which also shows connection commands/JSON.
- Transport: HTTP. Claude Desktop `claude_desktop_config.json` (Settings → Developer):

```
{ "mcpServers": { "sqx": { "type": "http", "url": "http://localhost:8080/mcp" } } }
```

- Tools (verbatim):

| Tool | Description |
| --- | --- |
| `list_projects` | List all available SQX projects. |
| `list_strategies` | List strategies inside a given project. |
| `list_databanks` | List all databanks in a project. |
| `get_strategy_stats` | Performance statistics for a strategy (fitness, net profit, number of trades, Sharpe ratio, drawdown, and more). |
| `run_project` | Start running a project (e.g. launch the Builder). |
| `stop_project` | Stop a currently running project. |

- Limitations: only these six tools; "more tools will be added in future builds". No auth/security configuration is described.

Source: `strategyquant/zz-unlisted/mcp-integration.md`

---

## 7. Market Profile and Volume Profile

### 7.1 Market Profile (TPO)

- **TPO (Time Price Opportunity) indicator**, a.k.a. Market Profile — shows time spent at each price level; key levels **POC**, **VAH**, **VAL**. The doc contains no settings, parameters or data requirements, only concept + book references.
- Licensing: included in **Ultimate**; **Starter and Professional** need the separate **Market & Volume Profile add-on** (monthly/yearly) or upgrade to Ultimate.

Source: `strategyquant/zz-unlisted/market-profile.md`

### 7.2 Volume Profile

- An **indicator** (SQX chart indicator plus MetaTrader indicator variant) distributing volume by price: POC, VAH/VAL (~70% of volume), HVN/LVN.
- Parameters (SQX):
  - **Session Type**: `Previous Daily Session`, `Previous Weekly Session`, `Previous Monthly Session`, `Previous Yearly Session` (plus Swing session, Custom Hour, Multisession as advanced modes).
  - Timeframe selected per session: **M15** (Daily), **H1** (Weekly), **H4** (Monthly).
  - **BinSizeMode**: keep **"Fixed Tick Size"** for consistent backtests. **TicksPerBin**: e.g. 3 (daily) to 30 (monthly).
  - **HVN Count**, **HVN Threshold**, **LVN Threshold** (defaults usually fine); **EnableLVN**.
  - **Volume Cluster Profile (VCP)**: Gaussian-smoothed cluster peaks; **Maximum Cluster Center** ≥ 2.
  - **Swing Session**: Zig-Zag based profile per swing; swing detection by **Pivot Percentage**, **Fixed Ticks**, or **multiple of ATR**; **ShowZigZagLine**; M10 suggested for currency futures.
  - **Initial Balance**: default `0`; default periods: Daily 60 min, Weekly 720, Monthly 1440, Yearly 8640, Swing 60 (set in minutes).
  - **Store the Chart Data** (view the profile), **Show Candle Sticks**, **Show Volume Subchart**.
  - **Custom Hour Volume Profile** (specific hour; M15 recommended), **Multisession Volume Profile** (Tokyo, London, New York).
- MetaTrader variant: **Show Session Statistics Panel**; **VP** button (bottom center) toggles view, off by default for performance; **Max Sessions to Display**; **VCP Max Cluster Peaks**, **VCP Gaussian Sigma**. Indicator values remain available with view disabled.
- Data requirement: volume data at the listed intraday timeframes; no explicit tick-data requirement stated.
- Licensing same as 7.1.

Source: `strategyquant/zz-unlisted/volume-profile.md`

---

## 8. Reliable backtesting / trading

### 8.1 MetaTrader 4/5

1. Import SQ custom indicators into MT (post-installation step).
2. Use the same data as the broker (export from MT and import into SQ) or ensure Dukascopy data match broker **timezone** (matters for Exit at end of day / Friday).
3. Correct engine (MetaTrader 4 or 5) and bar type **"Timestamp is start of bar time"** for imported MT data.
4. Same spread/date range in MT Strategy Tester; disconnect MT4 from network via dummy proxy (Tools → Options → Server → Enable proxy server; Server `localhost`, dummy login/password; restart) to freeze spread.
5. On differences: exit SQ, delete all files in `/internal/testfiles`, restart.

Source: `strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-metatrader.md`

### 8.2 Tradestation / MultiCharts

1. Import SQ custom indicators into TS/MC.
2. Data: **Option 1** export exact chart data (MC: File → Export Data; TS: Data Window → Save) and set **Session = No Session** in Trading options; **Option 2** import minute data and set the session in SQ **exactly** as in TS/MC.
3. Engine Tradestation or MultiCharts; bar type **"Timestamp is end of bar time"** (the doc's sentence says "used by MetaTrader" — apparent copy error; the TS/MC recommendation is end-of-bar).
4. Time-based exits (**Exit At End Of Day**, **Exit On Friday**): set exit time `00:00` (end of session) or a time of an existing bar before the day's last bar.
5. Clear `/internal/testfiles` on differences.
6. Subcharts (Data2, Data3) not yet tested for match.
7. Only **'Selected Timeframe'** precision available for TS/MC engine (sufficient for market/stop/limit).
8. Less-tested indicators: **Pivots**, **Fibo**.

Source: `strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-tradestation-multicharts.md`

### 8.3 Exporting data from Tradestation

- Export **M1**, Session **Regular**, Time zone **Exchange**, bar building **Session hours**. In SQX Data Manager create the symbol with **Timestamp as end of bar**; import minute data; compute higher TFs and sessions in SQX Trading Options.

Source: `strategyquant/10-reliable-backtesting-trading/exporting-data-from-tradestation-recommendation.md`

### 8.4 JForex

- JForex fills SL/PT at the **triggering tick price**, not the SL/PT level (MetaTrader fills at the level). Lower precisions (Selected timeframe, M1) are "not really usable"; **real tick precision required**.
- Workflow: generate/cross-check with MT4 engine at Selected TF/M1, then retest finalists in JForex with tick precision.
- Many JForex indicators are implemented differently from SQ/MT; reimplementation not planned near-term.

Source: `strategyquant/10-reliable-backtesting-trading/reliable-backtesting-in-jforex.md`

### 8.5 Futures in MT5 — trading sessions (**Build 140**)

- MT5 symbol **Specification** shows Quotes and Trade sessions (example ES: 01:00–23:15, 23:30–24:00); orders outside → "Market is closed".
- SQX **Build 140** adds trading option **`MarketOpenSession`** for MetaTrader 5 engines; session defined in **Data Manager → Sessions** (use `23:59`, SQX does not accept `24:00`). Without it SQX trades during MT5-closed periods; with it backtests match MT5 given identical data/settings.

Source: `strategyquant/10-reliable-backtesting-trading/reliable-backtesting-of-futures-in-mt5-trading-sessions.md`

### 8.6 Stockpicker backtest limitations

- Evaluation timing: **Before Bar Open** (Shift=0 refers to previous finished day; all blocks safe), **On Bar Open** (Shift=0 = current day; only Open price blocks safe; Close[0] returns Open), **On Bar Close** (Shift=0 accurate for all). Indicators built from unknown prices (EMA of Close[0], ATR[0] on Bar Open) are incorrect. Weekly/monthly data available; current week/month via bar [0].
- Backtest uses **daily OHLC only** (no minute/tick). Rules: market entry with only SL or only PT → evaluated same bar; stop/limit entry with SL/PT → SL/PT applied only next day; both SL and PT hittable in one bar → **SL taken (pessimistic)**. Avoid very tight SL/PT.
- Backtest fills at exact Open/Close; live fills slightly after open / before close.

Source: `strategyquant/10-reliable-backtesting-trading/sp-backtest-limitations.md`

### 8.7 Multi-TF best practices (esp. TS/MC)

- Verify one or two strategies match between SQ and platform before long builds; recheck on symbol/session changes.
- Lowest TF on the main chart, higher TFs on additional charts.
- Use small periods, especially on higher TFs (bar reservation and indicator "cool off").
- Daily data in Tradestation (e.g. ES.D) have their own sessions; match in SQ.
- Avoid **Volume**/**AverageVolume** blocks; **Fractals** outputs zeros (use only for entry conditions, not SL/PT); **Is rising / Is falling** not working well with Daily/Weekly/Monthly blocks in EasyLanguage (shift not applied correctly).

Source: `strategyquant/10-reliable-backtesting-trading/best-practices-for-multi-tf-strategies-backtesting-and-trading.md`

### 8.8 Comparing / reusing backtest settings

- Result differences between Builder and Retester/Optimizer are always settings or data changes.
- **Strategy config** tab (Results tab of a strategy) shows project config vs last-backtest config; **Apply strategy config** button applies the strategy's settings to the project.
- **Compare** button (since **Build 130**): select two databank strategies; shows both last-backtest configs side by side, differences in red.

Source: `strategyquant/10-reliable-backtesting-trading/comparing-and-using-the-same-backtest-settings.md`

### 8.9 "Not supported for engine"

- Blocks/parameters unsupported by an engine are flagged with a **red flag** in Building blocks. Example: **Draw Down Arrow** unsupported for **EasyLanguage** (Tradestation and MultiCharts).

Source: `strategyquant/10-reliable-backtesting-trading/not-supported-for-engine.md`

### 8.10 Stop/limit order at incorrect price (MT5 log)

- Message: `Based on its logic, the strategy tried to place stop/limit order at incorrect price. Market price: …, min. price allowed: …, stop/limit order price: …` (preceded by `No pending orders of that type`). **Not an error**: entry conditions were met but the computed price was out of market, so the order was skipped.

Source: `strategyquant/10-reliable-backtesting-trading/the-strategy-tried-to-place-stop-limit-order-at-incorrect-price.md`

---

## 9. How-to items

### 9.1 Export strategy to MetaTrader and test/trade

- Native strategy file format stated in this (2018) doc: **`.str`**; the current installer doc refers to **`.sqx`** strategy files. Both extensions appear in the docs.
- Databank → double-click strategy → Result details → **Source code** tab → select **MetaTrader4 Expert Advisor** → **Save to file**. Copy to MT **File → Open data folder → `MQL4/Experts`** (example `C:\Users\John\AppData\Roaming\MetaQuotes\Terminal\<id>\MQL4\Experts`). **Tools → MetaQuotes Language Editor** (F4) → **Compile** (warnings about unused functions are normal). Then **Strategy Tester** → select EA, Symbol, Timeframe, dates → **Start**. Results may differ slightly; both engines are approximations.

Source: `strategyquant/11-how-to/export-strategy-strategyquant-test-trade-metatrader.md`

### 9.2 Install SQ indicators into MT4/MT5

- Source: `C:\StrategyQuantX\custom_indicators\MetaTrader4` → `<MT4>\MQL4\Indicators`; `C:\StrategyQuantX\custom_indicators\MetaTrader5` → `<MT5>\MQL5\Indicators` (via File → Open data folder). **Restart MT** to compile/reload.

Source: `strategyquant/11-how-to/how-to-install-strategy-quant-indicators-to-metatrader-45.md`

### 9.3 Load / save build config

- The article is screenshot-only (Save config / Load build config buttons). **File extension and storage location are not stated in the docs.**

Source: `strategyquant/11-how-to/how-to-load-and-save-build-config.md`

### 9.4 Merge / Split portfolio

- Action above the databank. **Merge strategies** dialog: **Name** (default `Portfolio`), **Save to databank**. Three merge types: **Simulated portfolio** (merged trades, statistics only, not backtestable); **Strategies merged to one (trading in parallel)** — EXPERIMENTAL, one compound EA for MT4/5 with per-strategy symbols/TFs, trading options (Exit on Friday, Limit Trading Range) are global; **Ensemble signals** — EXPERIMENTAL, one main strategy whose entries are fuzzy-combined with the others' entries (trade only if a given % of signals are valid). **Split** reverses a Merge; works only on portfolios created in **Build 127 or newer**.

Source: `strategyquant/11-how-to/merge-split-portfolio.md`

### 9.5 Multiple orders in the same direction

- Not generated by default; possible manually in AlgoWizard (multiple `EnterAtMarket` / `EnterAtStop/Limit`). MT4/5: works only with a **unique MagicNumber per EnterAtXXX**. TS/MC: **not supported** (exits apply to all same-direction orders). "Scaling In" feature announced for future builds.

Source: `strategyquant/11-how-to/multi-orders-to-same-direction.md`

### 9.6 MetaTrader portable mode

- Add `/portable` to the shortcut target (Properties → Target). Do it right after installing MT. Windows blocks portable mode under `Program Files`; use e.g. `C:\MT4portable\MT4-1`. Benefits: data under the MT folder (e.g. `C:\mt4\MQL`), easy copy/backup/VPS move.

Source: `strategyquant/11-how-to/how-to-run-the-metatrader-in-portable-mode-and-what-it-is-good-for.md`

### 9.7 Troubleshooting (Builder produces nothing)

- **Genetic evolution with too big population**: e.g. 8 islands × 1000 = 8000 initial population, Decimation 2 → 16,000 strategies before evolution starts. Use smaller population, **Decimation = 1**, check rejection statistics, try Random generation first.
- **Too strict filters**: check Rejection statistics; Initial population filter likely too strict.
- **Automatic filter: No trades**: too many conditions; set **Settings → What to build → # of Conditions** max to 1–2.
- **Too many trades closing at the same bar / ambiguous trades**: SL/PT too small; **Settings → What to build → Stop Loss / Profit Target**, ATR multiple minimum ≥ **1.5**, appropriate fixed-pips minimum.

Source: `strategyquant/11-how-to/troubleshooting.md`

### 9.8 MQL Market (SQ 4 Business)

- MQL5 Market: MetaQuotes marketplace; **20% fee** per transaction; products encrypted per buyer/hardware; **minimum 5 activations** (adjustable any time). Pitched for **SQX Ultimate** ("Strategy Provider").
- Steps: mql5.com → **Market** → **My Products** → **Add product** → choose app type → fill Strategy Name (not all caps), Product category `experts`, Experts category (usually `Trend`), free/paid, price (one-time or rent 1/3/6/12 months), activations, accept terms → **Add** → upload logo **200×200**, description (logic, indicators, parameters e.g. `MagicNumber`, `CustomComment`), screenshots **640×480** (else rejected), optional video → upload **`.ex4`** → automatic validation → publish.

Source: `strategyquant/12-sq-4-business-mql-market/what-is-mql-market-and-what-it-offers.md`, `strategyquant/12-sq-4-business-mql-market/creating-mql4-mql-5-product-on-mql-market-step-by-step.md`

---

## 10. Facts an integrator must not get wrong

**Folders / files (relative to install root)**
- `StrategyQuantX.exe` marks the install root; default install folder `C:\StrategyQuantX<version>`.
- `user\data` (historical data), `user\customdata` (external indicator data), `user\projects` (strategies/databanks/projects, `.sqx` strategy files), `user\templates`, `user\extend` (snippets/indicators), `user\extend\ResultsPlugins\<Plugin>\index.html` (+ `CLAUDE.md`, `vue.global.prod.js`, `locales/`), `user\sqxbusiness`, `user\strategies` (non-default), `user\settings\settings.xml`, `user\settings\customBlocks.xml` (custom blocks), `user\settings\blockGroups.xml` (random groups), `user\settings\views\` (databank views), `user\settings\OppositeBlocks.csv` (+ `OppositeBlocks_example.csv`), `user\log\StrategyQuant\log_YYYY_MM_DD.log`.
- `internal\web\SQUANT\log_config.xml` (debug logging), `internal\testfiles` (backtest cache — safe to delete when SQX is closed), `internal\extend\Snippets\SQ\...`, `internal\plugins\...`.
- Root-level: `StrategyQuantX.config`, `sqcli.config`, `CodeEditor.config` (memory/app configs), `fixDPI.bat`, `InstallatorLog.txt` (transient), `custom_indicators\MetaTrader4`, `custom_indicators\MetaTrader5` (incl. `Experts\SqIndicatorValuesExportEA`), `custom_indicators\BrokerProfileInstrumentsSessionsScripts\Update_SQX_Instruments_information.ex5`.
- Strategy file extensions appearing in docs: `.sqx` (current), `.str` (older doc).

**Config keys**
- `settings.xml`: `<WebServerPort>` (e.g. 4971), `<gpuAccelerated>true|false</gpuAccelerated>` (default true).
- `log_config.xml`: `<root level="INFO|DEBUG">`.
- Heap size parameter name: not stated in docs (set via UI Memory tab; stored in the `.config` files).

**Ports**
- Web UI / Remote Access / MCP: default **8080** (or first available); MCP endpoint `http://localhost:8080/mcp`, transport `http`.
- Firewall ranges: `8080-8099/tcp`, `5050-5059/tcp` (UI), `80`, `443`, `25`, `465`, `587`.

**MCP tools (exact)**: `list_projects`, `list_strategies`, `list_databanks`, `get_strategy_stats`, `run_project`, `stop_project`. Introducing build: not stated.

**Automatic dismissal rules**: No trades; Too many ambiguous trades; Too many open trades (>100k); No filled trades; Zero PL trades; Zero duration trades; Unfinished trades; Too little trades (<20); Outlier trade (>2× 2nd+3rd best); Too many trades closing at the same bar. Evaluated at 40% of data; threshold 25% of placed trades.

**Results plugins named**: `Prop analytics`, `Prop Monte Carlo`, `RobustnessScorecard` (example). Messages: `GET_STATS`, `GET_ORDERS`, `SET_THEME`, `SET_LANGUAGE`. Requires Pro/Ultimate; build 144 for the `CLAUDE.md` workflow.

**Builds**: 127 (custom blocks, external indicators, split-able portfolios), 130 (Compare), 132 dev2 (`fixDPI.bat`), 133→134 updater bug / 135 fix, 140 (`MarketOpenSession`), 141 (Portfolio Composer, Broker profiles, predefined brokers IC Markets/RoboForex/PepperStone/OANDA/Darwinex/Dukascopy/5ers/FTMO/Monevis), 144 (results-plugin vibecoding).

**Engine limitations**
- MetaTrader import bar type: *Timestamp is start of bar*; TS/MC and Tradestation exports: *Timestamp is end of bar*.
- TS/MC: only *Selected Timeframe* precision; subcharts untested; no independent multi-entry exits; `Draw Down Arrow` unsupported for EasyLanguage; Pivots/Fibo less tested; exported-chart data requires *Session = No Session*.
- JForex: SL/PT fill at tick price → tick precision mandatory; indicator implementation differences.
- MT5 futures: use `MarketOpenSession` with session ending `23:59`.
- Stockpicker: daily OHLC only; pessimistic SL when SL and PT both hittable; SL/PT after stop/limit entry applied next day.
- Multi-order same direction in MT needs unique MagicNumber per entry.
- External indicator data are bound to one symbol + timeframe and to the same history data as the source platform.
- Random groups are enforced only for the initial population under genetic evolution.
- Custom block Min/Max/Step override global ranges; all-zero falls back to What-to-build.
- Migration: one build up at a time; never install over an existing installation; downgrade is backward-compatible only.

**Unclear / not stated in the docs**: build-config file extension and location; template file format; heap parameter key; MCP introducing build; default `WebServerPort` value (only inferable from MCP/firewall docs); which of `customBlocks.xml`/`blockGroups.xml` the migration how-to meant (conflicts with installer table).
