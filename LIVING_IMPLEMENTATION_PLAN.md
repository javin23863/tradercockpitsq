# Living Implementation Plan

This is the single mutable implementation plan. The architecture and backbone define ownership
and contracts; this file records owner intent, honest status, and the next coherent lane.
Do not create a second roadmap. Historical recovery evidence under `docs/recovery/` is not a
second authority.

## Canonical references

- `references/ui-authority/` — accepted visual/product authority (inspect before UI work).
- `docs/product-architecture-v1.md` — product ownership and producer boundaries.
- `docs/product-backbone-spec-v1.md` — detailed application/UI/API/custody/security contract.
- `AGENTS.md` — coding/review discipline.
- Quant-Guild Library (reference data only, never a runtime import):
  `https://github.com/romanmichaelpaolucci/Quant-Guild-Library`

## Owner intent — the product this plan is for

The owner is building a desktop product for researching, testing, trading, and maintaining
**indicators, strategies, and models** in financial markets. The user is led through one job at
a time. As much as is safe to automate is automated. Tabs flow in sequence. The product must
not look like StrategyQuant X's eleven Builder tabs.

A complete owner path looks like this:

1. See the **actual bar chart** for the instrument and timeframe under study (real OHLC from a
   market-data or historical-bar producer; native trades overlay when a result exists).
2. Speak or type to Apollo: paste a URL, drop a research paper, or describe an idea.
3. Apollo turns that source into a draft **indicator, strategy, or model** specification, and
   **asks typed clarifying questions** wherever the source is ambiguous (symbol, session, costs,
   IS/OOS, exits, fitness, robustness, model family, leakage controls).
4. The owner answers. Apollo drives the product through approved tools (navigate, draft Idea,
   fill unresolved Specification fields, request compile, request launch) after confirmation.
5. Native SQX builds/searches/backtests/robustness-tests strategies and indicators it owns.
   The platform-owned Models modality fits allowlisted estimators. Neither path invents trades.
6. Test & Validate shows the producer-backed funnel. Custom Projects are the plug-and-play
   native runner for a predefined backtest/robustness task sequence (Automation), not a cloned
   SQX Custom Projects window.
7. Proof binds the chain. Maintenance is new revisions of the same indicator/strategy/model
   identity — not a new scattered workspace.
8. Operate/trade stays empty until live producers exist. Historical green never becomes live P&L.

Apollo may control the product. Apollo may not hallucinate prices, signals, bars, trades,
Sharpe, expectancy, or “this will work live.” Every numeric claim cites a read-model field or
is refused. Native mutation still passes custody → approval → trusted gateway.

## Harsh grade of the previous plan (why this rewrite)

The previous living plan marked M1 and most of M2 complete and named “Windows verification”
as the only remainder. That is not a functioning research-and-trade product.

| Intent | Previous plan | Actual head (`main` `1dbc68af`) | Grade |
| --- | --- | --- | --- |
| Sequential guided Research | Claimed “Idea → Proof without route knowledge” | Four workspaces and tab rows exist; no next-step lock, no “only the legal next action” | Fail |
| Actual bar chart | Authority screen has a chart card | `chartFrame` is an SVG slot; Signals & Models is `data-chart-state="unavailable"`; `/api/market/quotes` is last/change only — no OHLC bar series | Fail |
| Paper / URL → indicator or strategy | Not in the plan | Idea text/source only; no ingest, no quote-and-hash, no ambiguity questionnaire | Fail |
| Apollo asks the right questions | Not in the plan | Chat explains read models; no typed questions bound to Specification fields | Fail |
| Apollo controls the product | Explicitly forbidden (`native_mutation: false`, one tool `retrieve_quant_guild`) | Composer cannot navigate, compile, launch, or fill custody | Fail vs intent; Pass vs old contract |
| Voice / microphone | Not in the plan | No capture, no STT, no desktop mic permission | Fail |
| Indicators, strategies, models as maintained objects | Catalog pills + sklearn bind | Native blocks listed; Models fit binds a digest onto an existing Candidate; no revisioned indicator/strategy/model identity, no maintain loop | Partial |
| Custom Project plug-and-play backtests | Deferred to M4 one-liner | Read-only topology (`execution.supported: false`); leftover SQX Custom Projects are contamination, not product | Fail |
| Anti-hallucination at PhD density | Quant-Guild titles/URLs + cockpit notes | 27 catalog entries; lecture math not stored (correct); no primary-literature register; Apollo still cannot cite a bar or a paper passage that was never ingested | Partial |
| Daily Windows use of real SQX | Marked remaining M2 | Two real Launch Builder stops on Windows (see branch inventory); leftover `Strategy 3.3.115.sqx` is not success | Fail |
| Must not clone SQX UI | Implicit | Correct — do not import Builder Cross checks / Ranking / WFO dialogs as product tabs | Pass |

A passing historical verdict is not a live edge. Quant-Guild lecture 77
(“Profitable vs Tradable”), lecture 97 (backtest pitfalls), and lecture 96 (search is not
alpha) already say this; the old plan never made it an exit criterion.

## Branch and commit inventory (do not lose this work)

`origin/main` tip:

`ed955106` — *Merge pull request #141* (Custom Projects stack on the recovered line).

Recovered-line parent remains:

`1dbc68af` — *Land the recovered product line as current main.*

Landed on that tip (keep; do not re-implement):

- UI authority PNGs + market-quotes seam + Cockpit Home eight zones
- `/api/assistant` OpenRouter transport, Quant-Guild catalog grounding, mid-turn
  `retrieve_quant_guild` (`e4b16777`, `dd5abe27`, `df308e37`, `76123c47`)
- Cockpit verdict over native trades (`d4f81227`, `774f4693`, `905700e1`)
- Models fit + catalog digest bind onto an existing Candidate (`dec7f94f`, `2b0803f6`, `d767de83`)
- Approved Random vs Genetic bind (`1242a6bd`)
- Reopen IDs + desktop session restore (`fb8b5d1f`, `79c47bee`)
- Idea / configuration / native-job / candidate / Retester / Proof custody chain

In-flight lanes (do not switch or clean these checkouts; do not duplicate their files):

| Branch | Tip | Owns | Status |
| --- | --- | --- | --- |
| `cursor/recovery-ui-authority-5d85` | `48a0fbc5` | Recovery checkout in `/workspace` — **protected** | Ancestor of `main`; do not reset |
| `cursor/native-builder-load-5d85` | `86b0d7cf` | Stage `{digest}.cfx`, fail-closed `sqx_loadconfig_failed`, supervised `start` | Windows: load of `project.cfx` copy refused (`missing Task element`) |
| `cursor/native-task-cfx-5d85` | `9cf27d64` | Pack Task-rooted CFX (`config.xml` = approved `Build-Task1.xml`) | Linux exact-head green; **Windows package not yet back** |
| `cursor/windows-verify-runbook-5d85` | `fb9ae305` | Windows acceptance runbook | Landed into `main` |
| `cursor/source-ingest-5d85` | `aeed52f1` | URL/document Idea ingest | Linux product tip under this slice |
| `cursor/clarifying-questions-5d85` | `3cdc5069` | Typed Specification questions | Stacked parent of this slice |
| `cursor/apollo-product-tools-5d85` | `124d47d9` | Apollo product tools (propose + confirm) | Stacked parent of this slice |
| `cursor/apollo-voice-5d85` | `f2cec47c` | Desktop mic → STT → `/api/assistant` | Stacked parent of this slice |
| `cursor/bar-trade-overlay-5d85` | `40ce38d6` | Native trade overlay on producer bars | Stacked parent of this slice |
| `cursor/capability-addon-registry-5d85` | `b57c7ffc` | Typed add-on registry / native plugins | In flight; parent of this slice |
| `cursor/automation-workflows-5d85` | `04dabf79` | Custom Project workflows + TV/MT MCP | Linux CI green; list/show/Start fail-closed. Does not yet run or stream results |
| Parallel `/tmp/tc-*` worktrees | various | Home, verdict, cross-checks, knowledge, models, session | Already in `main` lineage; treat as merged history, not a second spine |

Windows producer stops already observed (do not paper over):

1. `origin/main` `1dbc68af` — gateway staged `{digest}.xml`; SQX 144.2953 appended `.cfx` and
   looked for `*.xml.cfx`; exit 0 treated as success; leftover Builder ran; 60s kill; HTTP 409.
2. `cursor/native-builder-load-5d85` `86b0d7cf` — staged `.cfx`, `file=` without extension;
   SQX: `Cannot load config. Invalid task config, missing Task element.` No `start`.

Do not open a third Linux loadconfig-format slice until
`C:\tc-win-accept-task-cfx-20260903\ASSESSMENT.md` (or equivalent) reports HEAD `9cf27d64`,
staged CFX SHA ≠ `fff5ed70…`, inner `config.xml` SHA = approved `executable_xml_sha256`,
exact `file=` argv, SQX log, native-jobs JSON, and pre/post Results.

Leftover personal SQX screenshots (Custom Projects, Retester Results (7), Ranking, Monte Carlo,
WFO/WFM, Prop analytics, `<StrategyFile>` XML, clocks 8/30/2026) are **not** product evidence
and are not `ASSESSMENT.md`.

## Quantitative grounding register

The product deals with mathematics and financial markets. Grounding is citation plus refusal,
not a substitute engine.

### Curated lecture catalog (already wired)

`product/tradercockpit/knowledge/quant_guild_catalog.json` — 27 public Quant-Guild lecture
titles/URLs plus platform-authored cockpit notes. Notebooks and transcripts are not stored
and must not be imported as code. When Apollo uses a note it cites the lecture title.

Use these lectures as the first anti-hallucination layer for the topics they already cover:
Sharpe interpretation (101, 129), backtest pitfalls / Markov / expectancy (97, 71, 75, 118),
profitable vs tradable (77), Monte Carlo scoped to bound trades (33), Kelly as native MM not
a cockpit bet (36), volatility drag / CAGR (117, 136, 125), tail risk vs live Risk (126),
search ≠ alpha (96, 78), ergodicity (81), non-stationarity / WF / OOS (93), misleading
metrics (48), model breakage / NNs (58, 63, 24), expected returns (21), portfolio
optimization stays native (20), performance over time (4).

### Primary literature (platform-authored notes only — do not paste copyrighted text)

Add a second catalog, same rules as Quant-Guild: citation key, bibliographic pointer, cockpit
note, mapped product stage. Apollo may retrieve these notes. It may not reproduce proofs or
invent formulas from the titles.

| Key | Work | Product mapping |
| --- | --- | --- |
| `white-2000-reality-check` | White, H. (2000). A Reality Check for Data Snooping. *Econometrica*. | Builder/search multiples; do not treat the best databank row as a confirmed edge |
| `hansen-2005-spa` | Hansen, P. R. (2005). A Test for Superior Predictive Ability. *Journal of Business & Economic Statistics*. | Competing candidates; SPA is not a cockpit-owned test until a producer or an approved Models diagnostic exists |
| `bailey-2014-charlatanism` | Bailey, Borwein, López de Prado, Zhu (2014). Pseudo-Mathematics and Financial Charlatanism. *Notices of the AMS*. | Why Proof exists; why a green in-sample is not deployable |
| `bailey-2014-deflated-sharpe` | Bailey & López de Prado (2014). The Deflated Sharpe Ratio. *J. Portfolio Management*. | Sharpe on the verdict is the bound sample; do not invent a deflated Sharpe unless that column is produced |
| `harvey-2016-haircut` | Harvey, Liu, Zhu (2016). …and the Cross-Section of Expected Returns. *Review of Financial Studies*. | Multiple-testing haircut is not a hard-coded KPI |
| `lopezdeprado-2018-aifml` | López de Prado (2018). *Advances in Financial Machine Learning*. | Models modality: purged / combinatorial purged CV language; no embargoed labels as platform theater |
| `pardo-wf` | Pardo, R. *The Evaluation and Optimization of Trading Strategies*. | Walk-Forward stages use producer `WF*` columns; cockpit does not re-run WF |
| `tharp-r-expectancy` | Tharp, V. *Trade Your Way to Financial Freedom*. | Native ranking “R Expectancy (Van Tharp)” is producer text; cockpit expectancy is mean P&L on bound trades |
| `sharpe-1966-1994` | Sharpe (1966; 1994). | Interpret producer Sharpe; do not replace the SQX column |
| `wilder-1978` | Wilder, J. W. (1978). *New Concepts in Technical Trading Systems*. | RSI/ATR/ADX meaning; native SQX owns the block implementation |

Indicator definitions that native SQX already implements stay native. The platform does not
re-code RSI/ATR/ADX “to be more correct.” Models that need leakage-safe splits say so in
Specification and refuse to fit when the split is unresolved.

### Refusal rules (PhD attention, product law)

- No fabricated OHLC, volume, CVD, footprint, or liquidity.
- No fabricated trades, MAE/MFE, or equity points.
- No live P&L, positions, buying power, or risk from a Historical Result.
- No platform Monte Carlo / Walk-Forward / optimizer / Custom Project executor.
- No Kelly fraction, deflated Sharpe, SPA p-value, or CPCV score unless a named producer
  or an approved Models diagnostic actually emitted that field.
- Paper ingest stores hash + quoted spans the owner can see; Apollo may not “remember” a
  formula that was not in those spans.

## Product shape (unchanged chrome, expanded objects)

Top-level surfaces (owner override 2026-09-04): official SQX program-layout modules
plus Getting started / Operate / Settings. The former AlgoWizard rail slot is Apollo:

`Getting started | Builder | Data manager | Custom projects | Apollo | Operate | Settings`

Getting started is today's Home (eight live zones). Builder / Custom projects open the same Progress | Full settings | Results shell against that
module's native archive. Data manager stays fail-closed unless the verified runtime
has that module's native evidence — no substitute downloader. Apollo is the full-page
bounded assistant (`/api/assistant` chat, Speak/STT, Quant-Guild citations, and
approved tools with confirm). Home jumps to `/apollo` instead of mounting a second thread. Native AlgoWizard block authoring stays
in StrategyQuant X; this desktop does not invent a block editor. Explore and
Research-as-pipeline are not left-rail labels.

Home zones stay exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

plus an Apollo jump on Home (not a Home zone). The full assistant is the Apollo rail.

Research workspaces stay the four authority screens. The custody chain

`Idea → Specification → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

is folded into them and is the **only** sequential job. Construct modalities remain Random
Discovery, Genetic/Evolutionary search (native SQX), and Machine Learning / Models
(platform-owned).

Delivery targets (owner decision 2026-09-04): **MetaTrader 4, MetaTrader 5, TradingView, and
Python**. JForex, Tradestation/MultiCharts and NinjaTrader are out of scope. MT4/MT5 code is
exact native SQX `sourcecode/print` output. TradingView Pine Script and Python are not native
SQX outputs (SQX's own route is the packaged Source Code Translator plugin calling an LLM), so
the product delivers them as assistant translations of the native Pseudo Code, bound to the
native source SHA-256 and always `unverified_translation` until backtested in the target
platform. No TraderCockpit engine runs them.

First-class research objects (all three, all maintainable):

| Object | Authoring | Historical test | Maintenance |
| --- | --- | --- | --- |
| Indicator | Native SQX blocks and/or paper/URL → Specification | Native backtest when the indicator is used by a strategy; catalog listing otherwise | New immutable revision; do not silently edit the live block |
| Strategy | Native AlgoWizard/Builder and/or paper/URL → Specification | Native SQX backtest + robustness; Custom Project when that is the approved runner | New approved configuration revision |
| Model | Platform ML modality (allowlisted libraries) | Fit on producer features/trades; historical evaluation still native SQX where SQX owns it | New catalog digest bound onto an existing Candidate; never a Candidate-from-pickle |

Backtests the owner “just runs” are approved **Custom Projects** (native task order) presented
as one Launch action on Automation, with results on Test & Validate. That is plug-and-play.
It is not a clone of SQX Custom Projects / Retester / Cross checks UI.

## Milestone roadmap

A milestone is complete only when the owner can perform the path on the real desktop.

### M0 — Repository and UI-authority recovery

- [x] Audit + UI chronology; authority screens; canonical docs; milestone file.
- [x] Cockpit Home eight zones + quotes seam + prototype Research workspaces on `main`.
- [x] Assistant transport + Quant-Guild catalog + mid-turn retrieve on `main`.
- [x] Cockpit verdict + Models bind + session restore on `main`.
- [ ] Owner PR/branch disposition and `main` protection (owner action).

Exit: desktop shows the prototype with truthful read models. **Met on `main` chrome. Not met
as a daily research tool (see M1 remainder).**

### M1 — Research loop the owner can actually run (CURRENT)

Remainder — none of these are optional relative to owner intent:

- [ ] **Windows Launch Builder** — **deferred by the owner (2026-09-03)** until the Linux
  desktop is a complete product, including the plugins/add-ons already in the plan
  (Quant-Guild, Models, capability slots, native blocks). A working Linux program unpacks
  to Windows later; do not block Linux slices on `sqcli` loadconfig. Keep the two real
  Windows stops in the inventory so they are not forgotten.
- [x] **Sequential next-step** — `GET /api/research/next-action` names the current custody
  stage and the one legal next action; Overview, the rail, and Home Quick Actions emphasize
  that action; locked stages stay locked. Failed native load still stays failed.
- [x] **Actual bar chart** — `GET /api/market/bars` with producer OHLC + timestamp + symbol +
  timeframe; Signals & Models draws those candles; unavailable when the provider or
  `fetch_bars` is missing; quotes are never used as candles; no invented instrument.
- [x] **Bar-chart trade overlay** — when a Historical Result is selected, overlay native
  trades on the same producer bars. Do not invent fills.
- [x] **Source ingest** — `POST /api/research/ideas/ingest` accepts a public URL or UTF-8
  document, hashes the exact body, stores quoted spans, and mints an Idea revision.
  Apollo may bind a typed draft (indicator / strategy / model) only as verbatim span
  substrings; invented clauses are refused. Private/loopback URLs and binary documents
  fail closed. PDF/DOCX stay `document_type_unsupported` until a text extractor exists.
- [x] **Clarifying questions** — unresolved Specification fields become Apollo questions
  with allowed answers (`GET/POST /api/research/clarifying-questions`); Build stays
  locked until required native/model meaning is resolved. Invented answers fail closed.
  Watchlist symbols are the only legal market identities; an empty watchlist blocks
  rather than inventing a contract. Native `producer_configured` fields are not re-asked.
- [x] **Apollo product tools** (approved, fail-closed, confirmation on mutation):
  `navigate_surface`, `draft_idea_revision`, `propose_specification_fields`,
  `request_compile`, `request_launch` (launch only after exact approval). Still no direct
  `sqcli`, no invented executable XML, no skip of gateway verification. `native_mutation`
  stays false; tools propose the same custody APIs a human click would.
- [x] **Voice** — desktop microphone → STT → the same `/api/assistant` message path;
  transcript shown; mutation still confirmed; fail closed if capture or STT is unavailable.
  Same operator OpenRouter credential as chat; Speak is never a second assistant.

Exit: the owner can point at a chart, speak or paste a paper, answer Apollo’s questions, and
watch an approved native (or Models) job run without opening StrategyQuant X and without the
product inventing a bar or a trade.

### M2 — Daily personal-use reliability

- [x] Desktop session restore of the last registered path.
- [x] Models first path (fit + bind) on native trades.
- [x] Apollo retrieve-only tool.
- [x] Windows SQX discovery/setup/verification on the machine that actually runs 144.2953
  (complete unique 144.2953 + `sqcli.exe` scan; dead pins drop; hash mismatch
  fail-closes; two installs are `sqx_install_ambiguous`; verified `SQX_HOME`
  is remembered; Settings shows Runtime source, no path picker).
- [ ] Provider-enforced per-consumer spend ceiling (not only the operator key).
- [x] Recent-work list of indicator/strategy/model identities (not only last route).
  Typed Idea drafts (`indicator` / `strategy` / `model`); `idea=` session key.
  Landed as an extra left-rail **Recent work** card under Research progress.
  Chrome contract is still only workspace / research progress / account.
  Owner flagged that card as drift — do not add rail surfaces; keep, move,
  or remove that card only after an explicit product-authority decision.
- [x] **Code delivery to the owner's targets.** MT4/MT5 stay native (`/api/sqx-sourcecode`
  print / Save as EA). `POST /api/research/source-translation` prints the native Pseudo Code
  and Strategy XML from the running SQX (fail closed when it is not running), asks the bounded
  assistant without product tools (`reasoning_effort=low`, temperature 0) for Pine Script v6,
  Python (backtrader) or Python (Zipline), and stores an immutable record bound to the native
  source hash. Results → Source code shows a **Deliver to TradingView / Python** panel listing
  backend targets and stored translations, always labelled unverified with `TC-UNTRANSLATABLE`
  gap markers counted. Proven live on Linux against a fixture SQX web serving the docs' sample
  strategy and the real OpenRouter workhorse (Pine 18 s, backtrader 27 s). Not yet run against
  the installed Windows SQX 144.2953 `sourcecode/print`.

Exit: the owner uses the app daily on Windows with real SQX, real bars, and Apollo that can
drive the next legal action.

### M3 — Live / Operate

- Live bar/quote/signal/risk/account/execution producers.
- Paper/prop simulation after Proof — scoped, never merged with live.
- Promotion of a proven strategy/model into Operate without rewriting history.

Exit: Operate shows truthful live/current state. Home Signals/Risk/Performance light up only
from those producers.

### M4 — Automation (Custom Project plug-and-play)

- Inspect topology (already exists).
- [x] Capability/add-on registry (`GET /api/capabilities`); typed slots only; add-ons
  cannot inject script/HTML or rewrite top-level nav. Packaged native SQX plugins are
  the default catalog (SQX Lab, Custom Block, RunCompare, LucidFlex, Edge Decay,
  2-Step Challenge, Source Code Translator). `POST /api/capabilities` stages a known
  Results plugin into verified SQX. Results-plugin numeric settings stay in SQX Results.
- [x] Left rail is the official SQX program-layout modules (`Getting started | Builder |
  Data manager | Custom projects | Apollo | Operate | Settings`).
  Builder reads its module archive (`GET /api/sqx-module`); Custom
  projects keep the task pipeline. Data manager is a read-only view of what the running
  SQX reports (`/api/sqx-installed-data` from `constants/getAll` + `main/getData`, per-series
  range from `data/getSymbolData`, native module record): installed series with type,
  timeframe, range, bars and visibility, plus Sessions / Data types / Test precisions / Swap
  settings; it fails closed when SQX is not running and has no downloader, importer, or
  instrument editor. Apollo is the full-page bounded assistant. Native AlgoWizard authoring
  stays in StrategyQuant X. Explore and Research workspace names are not rail labels.
- [x] Automation lists real native Custom Projects and opens Progress / Full settings /
  Results. Full settings panes are documented SQX groups bound to the selected task XML
  (What to build, Data, Trading options, Building blocks, ATM, Money management, Ranking,
  Cross checks, plus Databanks/Resources/Notes when present). Genetic options is its own
  tab from nested BuildMode when generationType is genetic; Parts to improve shows when
  StrategyType is improve or the observed improve-existing alias. What to build offers
  official SQX types `simple`, `multi-tf`, `template`, and `improve` plus existing
  StrategyType fields (`additionalCharts`, `templateFile`, `improveType`). Template
  Browse/Reload call `buildType/listFiles` and `buildType/getTemplateConfig`. Ranking
  fitness `@type` comes from `fitnessMethodStrategyResult/list`. Data Chart
  `symbol`, Setup `session`, and Setup `testPrecision` come from
  `constants/getAll`; commission `Method@type` comes from
  `constants/listCommissionMethods`. Data also binds the official sq-data-box
  type/search/recent cloud, rewrites Setup dates from `constants.data` when
  the saved range cannot be used, opens Commission/Swap gear over existing
  XML, and draws the OOS graph from `data/getSymbolData` when `showGraph` is
  true. Those pickers fail closed when SQX web is down. Nested Ranking
  condition tables and Cross-check
  Settings/Filtering views come from that saved tree. `POST /api/sqx-project-settings`
  writes only existing attributes or existing text; it does not invent Condition rows,
  What-If scenarios, or extra SQX parameters. Radios, flags, and gear Save persist
  immediately and reload from that XML. Money-management Method siblings that
  already have `use` flags persist as one exclusive radio group. Calibrate now posts
  `indyTester/calibrate` through the running SQX local web and applies returned
  min/max/step onto existing blocks; it fails closed when SQX is not reachable.
- [x] Start/stop request native launch (`run_project` / `stop_project` as desktop ids).
  When the running SQX web is open they call official `project/start` (POST) and
  `project/stop` — the same servlets as the Electron control panel. If that web is
  down they fall back to `sqcli -project action=start|stop`. There is no
  StrategyQuant X MCP.
- [x] TradingView and MetaTrader 5 MCP are Apollo/LLM tool identities (Settings / Home
  System Status). They are not Automation, not Custom Project control, and not Operate
  live producers. Process-side URLs only; no fabricated live state.
- [x] List native Custom Project databanks and `.sqx` archives on Automation
  Progress and Test & Validate from the verified SQX home
  (`GET /api/sqx-project-results`). Selecting an inspectable databank `.sqx` on
  Results shows List of trades and equity from producer `orders.bin`
  (`GET /api/sqx-project-strategy`), strategy config from archive `settings.xml`
  versus the current task, Overview/SP/analysis/profile/source from `orders.bin`,
  Source Code type/MM/parameter print through the running SQX local web
  (`GET`/`POST /api/sqx-sourcecode`, Electron `browserToken` header, never
  exposed to the browser), native overview HTML from `overview/getOverviewContent`
  (loads the on-disk `.sqx` into the live databank when SQX has not indexed it),
  Save as EA / MetaTrader folder configure through `sourcecode/saveEA` /
  `getDataPath` / `saveMTPaths`, `+ New analysis` by copying `CustomPlugin`, extra
  Results plugin folders listed as tabs, Prop Monte Carlo / Prop analytics from
  the installed SQX Results plugins fed those same trades, and Trades on chart
  from stored chart members or a Tradestation OHLCV sidecar next to the `.sqx`
  with native Store Chart Data / zoom / previous-next chrome and indicator
  titles from `resultsCharts/loadChartData` (SQ4.StockChart is not ported). Fills overlay only when their
  timestamp lands on a bar.
  Generated/rejected/rate/percent bind from the SQX engine WebSocket when published.
  Progress chart series bind from the official `engineCharts` WebSocket
  (`charts[].data.chart` Chart.js datasets). The two Progress slots use
  `engine/getTypes` names and saved `settings`; changing a slot posts
  `engine/saveSelection`. Empty slots stay the official default titles and do
  not invent points. Grid/rows charts render official `items`. Fitness Evolution
  stays a native popup. Progress stats, logs, and chart slots patch in place
  every 2s while that tab is open; the bind key is not cleared. The Custom
  projects board patches row running/percent from the same catalog GET every
  2s from official TASKMANAGER `customProjectStats` plus an active CLI
  worker. Idle rows do not open a per-project WebSocket.
  Pause/Resume call native `project/pause` and `project/resume`.
  Do not invent project names or P&L.
- [x] Wire native Custom Project launch: `project/start` / `project/stop` when
  SQX web is open, otherwise trusted `sqcli -project action=start|stop
  name=<project>`. Stream producer log files / databank counts on Progress.
  Generated/rejected/rate/percent bind from the SQX engine WebSocket when
  published. Progress chart series bind from `engineCharts` when published.
  Pause/Resume use `project/pause` and `project/resume`. There is no
  StrategyQuant X MCP. The GUI-open path is official `project/start` /
  `project/stop`. CLI start while the GUI is open is not a success path:
  the second instance dies on port 5050, so the cockpit now refuses that
  exit. `POST /api/sqx-project-control` `run_project` on
  `GBPUSD H1 - Dukascopy` through `:4320` reached `Project started` and
  the matching `stop_project` reached `Project stopped`. CLI-while-GUI-closed
  has not been run on this desktop. Linux still proves the CLI contract
  with a trusted fixture launcher. Start is one confirmed action. A second
  Start is refused while TASKMANAGER `customProjectStats` says the project
  is running. Pause/Resume have not been live-proven on 144.2953 yet.
- [x] Custom projects list matches the official SQX row structure (name,
  `[ Tasks (n) ]` `[ Engine ]` `[ Results ]`, progress, Stop / Pause / Start,
  `DATABANKS` / `STRATEGIES`, gear) with a 2026 facelift — not purple cards and
  not typed-everything settings.   Documented enumerated attributes render as radios when the native list is
  short (engine, generationType family, StrategyType, MarketSides) and as
  `<select>` when it is longer (timeframe, comparators). True/false text on
  existing elements is a toggle that writes that text. Create new stays fail-closed. The catalog is real
  `user/projects` children only. List rows use the Template display labels from
  `077f8ace` (Indices / Futures / Forex / Gold families — ten native archives,
  including two distinct `Futures Template H1 Breakout` folders). Native folder
  names stay the start/stop/topology identity; they are not invented chrome.
  The task pipeline reads each saved archive: `Task@title` when present
  (else `name`), Setup symbol/timeframe/dates, Input→Output databanks,
  ClearDatabanks names, and GoToTask labels. Those ten archives are different
  shapes (clear-then-build, 9-task gold/forex loops, 10-task index/futures
  chains, NQ multi-timeframe with skipped indexes). The desktop does not
  collapse them into one GOLD template.

Exit: “Run this project” is one confirmed action; results land in the same funnel.

### M5 — Commercial readiness

Installer, signing, updater/rollback, migration, backup/export, crash diagnostics, secrets,
account/license/auth, entitlement, onboarding, customer-readable errors, docs/support,
privacy/telemetry, SQX distribution/licensing review, voice/STT provider terms.

### M6 — Public beta / release

Clean-machine install, first-run (mic, data feed, SQX, Google account), representative
indicator/strategy/model workflows, upgrade and failure recovery, support runbook.

## Current status and next lane

2026-09-04: `origin/main` `077f8ace` is the pickup head (PR #146 UI
parity `eef6322e` plus Template display labels). Chronology on main:
Custom Projects #141 (`ed955106`) → SQX discovery #142 (`cdd9f0b7`) →
recent-work #143 (`8b74a36a`) → env #144 (`912dae5e`) → UI parity #146
(`077f8ace`). Custom projects list labels are Indices / Futures / Forex /
Gold Templates over ten native `user/projects` archives; Dukascopy /
Tradestation folder names stay the producer identity. Discovery remembers a
unique complete 144.2953+`sqcli.exe` home, fail-closes dead pins and
two-install ambiguity, and shows Runtime source in Settings with no
path picker. Recent-work is `GET /api/research/recent-work` plus
`idea=` session restore; the extra rail card is parked pending an
owner chrome decision. Left-rail surfaces stay
`Getting started | Builder | Data manager | Custom projects | Apollo |
Operate | Settings`.

Next incomplete M2 item is provider-enforced per-consumer spend
(sibling PR #101, worktree `tradercockpitsq-openrouter-credits`). Do
not implement spend on this clone. Do not merge other sibling stacks
from here. Do not invent a platform executor or an SQX MCP. Do not
Start/Calibrate Builder unless the slice requires it.

Stacked ancestor PRs #127–#140 are already in #141; they are not a
second roadmap. Start the next slice from this `main`.

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing
changes, and delete the branch after merge. Do not switch/reset/clean another active lane’s
checkout.
