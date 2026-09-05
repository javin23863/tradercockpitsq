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

### One working product theme — owner direction 2026-09-05

**Owner approval to publish — 2026-09-05.** After the scrolling and dropdown fixes,
the owner instructed: "Publish to main so we don't lose it and now continue with
the rest of the pages and the buildout." This approves publication of the reviewed
Results baseline and its complete application dependencies. Published through PRs
[#151](https://github.com/javin23863/tradercockpitsq/pull/151) and
[#152](https://github.com/javin23863/tradercockpitsq/pull/152), with final main
`5260ed24ae6314f74153b504766294450ff521ee`. Both PRs passed Linux Product Runtime
Acceptance and Windows desktop packaging; post-merge main run `33960230825` passed.
Continue the accepted layout through Builder Progress / Full settings and the shared
Custom projects workspace, then Getting started, Apollo, Data organization, and Settings.
Preserve native controls, producer scope, safeguards, and explicit unavailable states.
This approval does not close the remaining native or commercial acceptance gates.

**Results-first redesign — implemented for owner review, 2026-09-05.** The owner
rejected the color-only proposal below and approved implementing a visibly different
Results workspace before carrying the design across the other five surfaces. The
current working app now uses the full Results width, a compact task selector,
strategy/date/instrument header, six filtered metric cards, a balance/drawdown
dashboard, detailed time/direction/outcome/duration analysis, and an expandable
databank drawer. Existing result tabs, native prop scope, source/file actions, and
producer ownership remain in place. The earlier paired-drawdown and yearly-only
analysis gaps described below are resolved in this implementation.

The existing strategy endpoint adds optional validated `sample`, `direction`, and
`period_by` query parameters and a read-only `analytics` field. A shared filtered
native trade set drives cards, series, profile, symbols, and trade rows. Capital
for these analytics comes only from the result archive; missing capital and
timestamps stay explicit. Native drawdown helpers are reused before rounding.
Bulk databank selection no longer silently relabels the inspected result URL;
double-click opens the new archive, with filters preserved. Native price bars and
runtime-only capabilities remain unavailable when the corresponding producer input
or running runtime is absent. This does not close full native desktop acceptance.

Review: `.git/ui-review/results/index.html` (before/after and 15 actual captures),
with live Results on `http://127.0.0.1:4383/builder?tab=results&task=1&databank=Results&archive=Strategy+3.3.115.sqx&resultView=overview`.
Source hashes: `.git/ui-review/results/source-hashes.json`. Verification on the
current uncommitted source: production-boundary PASS, 679 Python tests (17 skipped),
268 UI tests, and all three browser regressions PASS
(`.git/native-acceptance/20260905T091617Z-results-review-regressions/receipt.json`).
`tests/results-dashboard-browser.mjs` exercises the real application using
`RESULTS_REVIEW_URL`; its receipt is `.git/ui-review/results/receipt.json`.
Filters/reload, chart zoom/reset/keyboard inspection, databank expansion/collapse,
source export, strategy switching, and 960px layout pass. The native archive's
on-disk SHA-256 matches the read model and remains unchanged. Owner visual approval
and publication to main are complete as recorded above. The complete dependency
diff, including the previously uncommitted work, is preserved on main. Applying the
accepted design to the remaining surfaces continues on `codex/workspace-buildout`
from that main commit.
The configured vault sync was retried at this handoff and remains blocked by the
existing control-plane/Futures hygiene and stale Inbox checks
(`.git/ui-review/results-vault-sync.log`).

**Results scrolling and dropdown correction — 2026-09-05.** Owner review found
that the nested table scroller's horizontal bar was clipped by the compact drawer;
the prior programmatic checks did not prove visible scrollbar access. The databank
now has one focusable scroll region for both axes, sticky strategy identities,
visible scrollbars, compact rows, and a working IS/OOS column selector over the
native saved values. The upper Results pane scrolls independently. Column changes
preserve drawer expansion and selection; the native standard-report dropdown now
opens its previously collapsed panel. File operations and cancellation remain intact.

`tests/results-scroll-browser.mjs` failed on the original horizontal scroll owner
and now passes at 1440 and 960 pixels, including mouse/keyboard selectors, a
browser-only 42-row overflow fixture, separate scrolling, and reachable cancellation.
The real in-app browser also changed Columns to Out-of-sample successfully.
`tests/results-dashboard-browser.mjs` passes with visible-scrollbar captures; the
Overview header, metrics, chart, and compact drawer fit at 1440×1000. Archive SHA-256
remains `ccd41e713a879e030115d2b62be50547b65cdd096f3f3f3568dc4f183a74ab39`.
Production-boundary, 679 Python tests (17 skipped), 268 UI tests, and all three
browser regressions pass on this working source. One initial general browser run
timed out on `/backtest/trades`; the confirmation run passed unchanged:
`.git/native-acceptance/20260905T093641Z-results-scroll-confirm-regressions/receipt.json`.
Focused logs are `.git/ui-review/results-scroll-*.log`; refreshed visual evidence
and source hashes remain in `.git/ui-review/results/`. Vault sync was retried and
remains blocked by existing control-plane, Futures hygiene/graph, and stale Inbox
checks (`.git/ui-review/results-scroll-vault-sync.log`). Visual approval and publishing
to main remain pending; this correction does not close native acceptance gaps.

**Earlier color-only pass (rejected; historical status):**

Before further backend expansion, reconcile the futuristic five-screen references
with the working six-surface application. Preserve native task/settings/result tabs,
forms, filters, databanks, and connected actions. Apply one midnight navy, violet,
and cyan theme throughout, including the embedded native prop-analysis panels.
Show the actual running pages for every rail and tab for owner visual approval
before saving the accepted baseline to main. Mockups and passing tests do not prove
that every visible button is connected.

The additional owner screenshots (`tester4.png`, `tester3.png`, `tester2.png`,
`rester1.png`, `builderpage31.png`, `builder30.png`, `builder29.png`,
`builderpage28.png` under `C:/Users/MSI/Pictures/Screenshots`) establish the analytical
context to modernize: equity with drawdown, performance by hour/day/month/year,
direction and duration breakdowns, detailed trades, ranking conditions, and native
cross-check dialogs. Preserve real producer data and controls. The current equity
pane lacks the paired drawdown panel and its time labels need readable formatting;
the current trade-analysis pane exposes only yearly profit. Theme approval does not
close those functional gaps.

Current proposal is uncommitted on `codex/research-state-reliability`, based on
`59c6c9c12aa331d37115b584817e781a4c41716c`, with earlier working changes protected.
Actual-page review: `.git/ui-review/index.html` and `captures.json`; live app on
`http://127.0.0.1:4383`. Theme verification: 266 Node tests pass; all three browser
regression commands pass (`.git/native-acceptance/20260905T084221Z-theme-final-regressions/receipt.json`).
The full Python suite also passes (676 tests, 17 skipped) with the operator provider
credential removed from the test process; the earlier voice-readiness failure was
caused by that inherited credential. Production-boundary check passes.
The real HTTP/native-archive check verifies shared plugin colors and that hidden
Overview frames remain hidden (`.git/ui-review/theme-live-check.json`). Full native
button acceptance and owner visual approval remain open; no new native run was launched.
Configured vault sync (`vault_sync.py --no-push`) was attempted and remains blocked
by existing control-plane collector, Futures hygiene/graph, and stale Inbox checks
(`.git/ui-review/vault-sync.log`).

Gate 1 recovery work is paused for this UI decision. The bounded legacy refusal/save
check preserved original bytes and custody across reload
(`.git/native-acceptance/20260905T082259Z-legacy-live-browser/receipt.json`);
this does not close legacy migration or full native desktop acceptance.

### First sellable release — owner ruling 2026-09-05

The customer surface is deliberately limited to **Getting started | Builder |
Custom projects | Apollo | Data organization | Settings**, with consistent shared
navigation, controls, layout, and state language. Knowing the full native feature
catalog does not mean presenting the whole platform to customers. Operate and the
broader plugin inventory are not primary first-release navigation.

The foundation is trustworthy native **backtesting, genetic strategy search, and
Custom Project execution**. Apollo helps customers turn internet sources, research
papers, and YouTube material into reviewable coded projects, then drives the same
approved build/test workflow. MetaTrader, TradingView, and Python are the customer's
implementation/export destinations; each needs explicit supported conversion,
validation, and unavailable states rather than assumed parity.

**Prop-firm simulation and analysis are part of this core workflow**, attached to
actual strategy/backtest results with explicit challenge rules and costs. Reuse
existing native prop-analysis integrations before adding behavior. A historical
simulation or LLM-generated project is not a guarantee of passing a live challenge.
Other plugin refinements follow after this complete customer path works.

Acceptance follows one coherent path: source/idea → reviewed project → native
build/search or Custom Project → native backtest and validation → prop-firm analysis
→ supported target-platform code. The older broad product description below remains
background context; this dated release boundary controls implementation priority.

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
6. Test & Validate shows the selected candidate through its actual approved native graph, with separate cockpit-policy summaries. Custom Projects are the plug-and-play
   native runner for a predefined backtest/robustness task sequence (Automation), not a cloned
   SQX Custom Projects window.
7. Proof binds the chain. Maintenance is new revisions of the same indicator/strategy/model
   identity — not a new scattered workspace.
8. Operate/trade stays empty until live producers exist. Historical green never becomes live P&L.

Apollo may control the product. Apollo may not hallucinate prices, signals, bars, trades,
Sharpe, expectancy, or “this will work live.” Every numeric claim cites a read-model field or
is refused. Native mutation still passes custody → approval → trusted gateway.

## Historical assessment — recovered main `1dbc68af` (superseded status snapshot)

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

## Historical branch inventory — pre-2026-09-05 reconciliation (preserve evidence)

The following tip and lane states were recorded before the 2026-09-05 reconciliation; they are not current branch authority. Current gate status appears below.

Recorded `origin/main` tip:

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

Historical loadconfig follow-up requested the following receipt (subsequent native acceptance below supersedes this stop):
`C:\tc-win-accept-task-cfx-20260903\ASSESSMENT.md` (or equivalent) reports HEAD `9cf27d64`,
staged CFX SHA ≠ `fff5ed70…`, inner `config.xml` SHA = approved `executable_xml_sha256`,
exact `file=` argv, SQX log, native-jobs JSON, and pre/post Results.

The earlier personal SQX screenshots do not prove the integrated product journey. They remain valid supporting native observations; they are not a replacement for the dated application/native acceptance receipts.

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
- No platform replacement for native bar-dependent Monte Carlo / Walk-Forward / optimizer / Custom Project execution. The separately attributed cockpit policy may analyze/resample recorded native trades within the architecture contract.
- No Kelly fraction, deflated Sharpe, SPA p-value, or CPCV score unless a named producer
  or an approved Models diagnostic actually emitted that field.
- Paper ingest stores hash + quoted spans the owner can see; Apollo may not “remember” a
  formula that was not in those spans.

## Product shape — current owner rulings 2026-09-05

Top-level surfaces follow the approved first-release boundary. The global top strip and live-account footer are removed; the market ticker remains. The persistent candidate databank dock is research work content, not that footer:

`Getting started | Builder | Custom projects | Apollo | Data organization | Settings`

Getting started is today's Home (eight live zones). Builder / Custom projects open the same Progress | Full settings | Results shell against that
module's native archive. Data organization separates provider-backed history capture, native import and approved task application; unavailable data or unresolved settings remain explicit. Apollo is the full-page
bounded assistant (`/api/assistant` chat, Speak/STT, Quant-Guild citations, and
approved tools with confirm). Home jumps to `/apollo` instead of mounting a second thread. Native AlgoWizard block authoring stays
in StrategyQuant X; this desktop does not invent a block editor. Explore and
Research-as-pipeline are not left-rail labels.

Home zones stay exactly:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

plus an Apollo jump on Home (not a Home zone). The full assistant is the Apollo rail.

Research workspaces stay the four authority screens. The custody chain

`Idea → Specification → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

is folded into them as the product custody chain. Actual native task order, branches and loop visits follow the approved graph; external imports have honest source lineage without a fictitious Build stage. Construct modalities remain Random
Discovery, Genetic/Evolutionary search (native SQX), and Machine Learning / Models
(platform-owned).

First-class research objects (all three, all maintainable):

| Object | Authoring | Historical test | Maintenance |
| --- | --- | --- | --- |
| Indicator | Native SQX blocks and/or paper/URL → Specification | Native backtest when the indicator is used by a strategy; catalog listing otherwise | New immutable revision; do not silently edit the live block |
| Strategy | Native AlgoWizard/Builder and/or paper/URL → Specification | Native SQX backtest + robustness; Custom Project when that is the approved runner | New approved configuration revision |
| Model | Platform ML modality (allowlisted libraries) | Fit on producer features/trades; historical evaluation still native SQX where SQX owns it | New catalog digest bound onto an existing Candidate; never a Candidate-from-pickle |

Backtests the owner “just runs” are approved **Custom Projects** (native task order) presented
as one Launch action on Automation, with results on Test & Validate. That is plug-and-play.
It is not a clone of SQX Custom Projects / Retester / Cross checks UI.

## Historical milestone grouping — current sequencing is Gates 0–6 below

These milestone rows preserve earlier scope and implementation records; checked source features are not commercial gate completion. The owner-approved Gates 0–6 below are the current execution order. A gate passes only through its stated real desktop acceptance.

### M0 — Repository and UI-authority recovery

- [x] Audit + UI chronology; authority screens; canonical docs; milestone file.
- [x] Cockpit Home eight zones + quotes seam + prototype Research workspaces on `main`.
- [x] Assistant transport + Quant-Guild catalog + mid-turn retrieve on `main`.
- [x] Cockpit verdict + Models bind + session restore on `main`.
- [ ] Owner PR/branch disposition and `main` protection (owner action).

Exit: desktop shows the prototype with truthful read models. **Met on `main` chrome. Not met
as a daily research tool (see M1 remainder).**

### M1 — Research loop (historical grouping)

Remainder — none of these are optional relative to owner intent:

- [ ] **Windows Launch Builder** — **deferred by the owner (2026-09-03)** until the Linux
  desktop is a complete product, including the plugins/add-ons already in the plan
  (Quant-Guild, Models, capability slots, native blocks). A working Linux program unpacks
  to Windows later; do not block Linux slices on `sqcli` loadconfig. Keep the two real
  Windows stops in the inventory so they are not forgotten. This historical deferral is
  superseded by the 2026-09-05 Windows-first ruling and recorded native runs below.
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
  projects keep the task pipeline. Data manager fails closed without a substitute
  downloader. Apollo is the full-page bounded assistant. Native AlgoWizard authoring
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
  down only Start may fall back to `sqcli -project action=start`. Stop must reach
  the running instance. The official SQX MCP is documented but has no adapter in
  this product; see the 2026-09-05 programming-guide integration catalog.
- [x] TradingView and MetaTrader 5 MCP are Apollo/LLM tool identities (Settings / Home
  System Status). They are not Automation, not Custom Project control, and not Operate
  live producers. Process-side URLs only; no fabricated live state.
- [x] List native Custom Project databanks and `.sqx` archives on Automation
  Progress and Test & Validate from the verified SQX home
  (`GET /api/sqx-project-results`). The Results databank grid is native
  **Default - Main data**: Strategy Name, Filters result, then the IS metric
  block (Fitness through Exposure) followed by the matching OOS block. OOS cells
  use native ``background-oos``. Cell values are producer ``SQStats`` /
  ``Fitnesses`` / ``SpecialValues`` from each `.sqx` ``settings.xml``, including
  ``MEC_IS_Main`` / ``MEC_OOS_Main`` sparklines and any producer ``oos``/``isv``
  index ranges. Missing fields stay dashes; recorded OOS zeros stay zeros. Selecting an inspectable databank `.sqx` on
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
  SQX web is open, otherwise Start uses trusted `sqcli -project action=start
  name=<project>`. Stream producer log files / databank counts on Progress.
  Generated/rejected/rate/percent bind from the SQX engine WebSocket when
  published. Progress chart series bind from `engineCharts` when published.
  Pause/Resume use `project/pause` and `project/resume`. The GUI-open path is official `project/start` /
  `project/stop`. CLI start while the GUI is open is not a success path:
  the second instance dies on port 5050, so the cockpit now refuses that
  exit. `POST /api/sqx-project-control` `run_project` on
  `GBPUSD H1 - Dukascopy` through `:4320` reached `Project started` and
  the matching `stop_project` reached `Project stopped`. CLI-while-GUI-closed
  has now been probed in an isolated runtime (see current status); normal CLI-only
  Stop is still unproven. Linux fixtures prove the CLI contract
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
  `user/projects` children only; DJ/Gold/NQ rows are not invented.

Historical intended exit: one confirmed project launch with results in the candidate workflow. Gates 2–5 below now define the missing measurable capture, automation and continuity acceptance.

### M5 — Commercial readiness

Installer, signing, updater/rollback, migration, backup/export, crash diagnostics, secrets,
account/license/auth, entitlement, onboarding, customer-readable errors, docs/support,
privacy/telemetry, SQX distribution/licensing review, voice/STT provider terms.

### M6 — Public beta / release

Clean-machine install, first-run (mic, data feed, SQX, Google account), representative
indicator/strategy/model workflows, upgrade and failure recovery, support runbook.

## Current status and next lane

2026-09-05 owner decision: the first sellable release is a **Windows desktop for
building, backtesting, and validating trading strategies**, with a **separately
licensed native engine**. Hosted web delivery and live trading are not first-release
acceptance requirements. TraderCockpit is the customer product; SQX remains a truthful
technical producer identity. No redistribution entitlement is inferred.

Fresh clone verified `origin/main` at `59c6c9c12aa331d37115b584817e781a4c41716c`.
Local branch `codex/research-state-reliability` repairs two observed UI integration
defects: the saved Builder launch gate repeatedly fetched itself after painting, and
durable Research mutations did not refresh shared progress/validation state. The gate
now binds once per host/selection, clears launch authority during reads, and ignores
late responses for a different configuration. Validated compile/approve/import/native
job/Retester/Higher Precision/Proof responses notify the existing custody refresh path.
Shared refresh preserves active Proof/Robustness workspaces so delayed completion
reads cannot reset their selection; actual navigation still replaces the workspace.
The continuation incorporates the registry/legacy-route/socket repair from PR #145,
then aligns navigation with the owner's six-surface ruling above. Windows add-on stores now reject
junction redirects as well as symbolic links. Native Builder now packages source-bound
CFX, requires native load confirmation, registers its start process with desktop
supervision, and serializes same-process retries. Nothing here is merged or released.

Dated verification history on this checkout (each result applies to its named receipt/build, not later edits):

- `python tools/check_production_boundary.py --root .`: PASS.
- `npm test`: 231 passed. The new lifecycle regression fails against the original
  launch module (93 requests instead of 3) and passes against the repair; it includes
  stale GET/POST responses, missing configuration, failed status, and host replacement.
- `node tests/run-browser-proof-regression.mjs`: PASS using its isolated fixture,
  including restart, exact persisted identity, and Proof completion while its status
  response is delayed behind a faster shared refresh. That delayed-response test
  fails with the original shell and passes with the repair. In-app browser: created another
  Proof through the UI; Home showed two Proofs without reload. Fixture evidence only.
- `node tests/run-browser-regression.mjs`: PASS, all 28 canonical routes after the
  six-surface change; legacy `/operate` redirects to Getting started. Build assertions await the actual approval/runtime gate;
  the old test observed only its temporary disabled placeholder. Exact revision,
  approval, and unavailable-runtime launch refusal checks remain.
- `node tests/run-browser-robustness-regression.mjs`: PASS through the current Home
  status-bar route. Proof restart acceptance also passed again in this sequence.
- Windows full Python baseline: 529 tests, 2 failures, 2 errors, 14 skips. One failure
  was inherited OpenRouter environment (all 6 voice tests pass with that variable
  removed). The POSIX-only launcher expectation is now platform-native (10/10 tests
  pass). Capability tests: 15 passed, two explicit Windows symlink privilege skips;
  duplicate rejection stays unconditional and four real inside/outside junction
  cases execute. The combined Windows Python suite now passes: 557 run, 16 skipped
  (541 passed), including archive format/build separation and malformed XML encodings.
  A later Windows connection reset exposed a chart fixture that did not consume its
  POST body. The fixture now does; the shared native-web client also maps socket/
  incomplete-body failures to its existing unavailable state. Six open/body error
  cases fail against the original client and pass against the fix.
- The exact-main Windows CI package build, WebView launch/shutdown, and persistence
  smoke passed. This repair also fixes omitted knowledge/plugin package data in the
  frozen build. A fresh development executable was built in
  `dist/development-clean-20260905/TraderCockpit.exe` using an isolated packaging
  environment. Its own hidden WebView window/HTTP lifecycle acceptance passed:
  `.git/package-acceptance/run-20260905T005113Z-eb2d338c/receipt.json`, executable
  SHA `b1cbc4b5…`, seven plugins and 27 knowledge entries, exact Idea persistence
  across restart, two graceful closes, no forced cleanup. The initial probe's
  visible-window lookup was corrected to inspect the deliberately hidden window.
  This binary precedes the Stop/copy follow-up changes; it is not the final build,
  a signed installer, a visible-interaction test, or clean-machine acceptance.
- Previous development build: `dist/development-owned-stop-20260905/TraderCockpit.exe`,
  SHA `426b638c7b1e7ad8651ccfeb1e646b9de7cebc8e8cfa8a9db6b644678e1879d0`.
  Packaged hidden-window/HTTP lifecycle acceptance passed at
  `.git/package-acceptance/run-20260905T011225Z-d2461468/receipt.json`: exact seven-plugin
  catalog and 27 knowledge entries, saved Idea identity after restart, two graceful
  closes, no forced cleanup. It includes owned Stop and transport-error handling.
  This unsigned build predates the six-surface/archive-identity changes; native Stop
  acceptance below exercises source.
- Current development build: `dist/development-core-prop-20260905/TraderCockpit.exe`,
  SHA `266c9ddbff14c428b8e02c06661c6f284ee125b1524b062eb11a9af990634dc2`.
  It includes the six surfaces, archive identity/encoding fixes, shared layout fixes,
  and explicit prop-analysis scope/assumptions. Hidden WebView/HTTP lifecycle passed:
  `.git/package-acceptance/run-20260905T014840Z-001f8b0a/receipt.json`, exact six-surface/
  seven-plugin inventory, 27 knowledge entries, saved Idea reopened unchanged,
  two graceful closes, no forced cleanup. This remains an unsigned development
  executable; clean-machine, signed release, and full native validation are open.
- Isolated native load acceptance: PASS, report
  `.git/native-acceptance/20260905T002157Z-load-only/report.json`. Actual SQX 144.2953
  returned `Config loaded.`; Settings SHA `6392eeea…` remained exact, CFX SHA
  `f9f37c7c…`, inner Task SHA `f2cbd540…`. Protected Downloads Builder/Retester and
  old outputs stayed unchanged; no native processes remained.
- Desktop HTTP start acceptance: `submitted_only`, report
  `.git/native-acceptance/20260905T002247Z-http-run/report.json`. The owned native
  worker stayed alive past the former 60-second ceiling, logged `Project started`
  and loaded native backtest data. Supervisor shutdown left no native processes.
  No new strategy output was observed.
- The longer 300-second generation probe correctly refused acceptance, report
  `.git/native-acceptance/20260905T003040Z-generation-run/report.json`. No new or
  changed strategy output was observed; generated/rejected/accepted counters were
  unavailable. Normal Stop falsely reported submission after a second CLI instance
  was refused on port 5050. Supervisor cleanup stopped the owned worker; ordinary
  Stop and settled-result persistence were unproven at that receipt. The native command service
  and full web/WS service are distinct. Do not enable the latter until its network
  access boundary is verified.
- The isolated command-service probe demonstrated native status and Stop using
  percent-20 spaces and literal equals signs:
  `.git/native-acceptance/20260905T005437Z-command-stop/report.json`. SQX reported
  1,993 generated, 100% rejected, zero accepted. The listener matched the registered
  worker before both requests. Native logs confirmed Stop, saving one existing
  strategy with zero removals, and ordinary process exit. The overall strict-byte
  receipt remains refused: graceful save reserialized the existing archive.
  Independent comparison found identical strategy/trade/equity bytes and equal
  decoded settings/statistics; no new accepted result or data loss was observed.
  Product HTTP integration of this Stop path is implemented and independently
  reviewed: unique live supervised handle, trusted launcher/project/argv, matching
  listener PID, fixed bounded request, and positive native confirmation. No owned
  worker preserves the existing web refusal; an authenticated web refusal cannot
  be bypassed. The command fallback currently supports single ASCII project-name
  tokens; names requiring native quoting still need the web control interface.
  Product-path native Stop acceptance subsequently passed with authorized temporary
  firewall isolation, as recorded below.
  The native listener
  was `0.0.0.0:5050`; loopback requests do not establish a loopback-only server.
  Native network exposure remains a release-security blocker.
- Stop now refuses the unavailable native web control path instead of spawning
  another CLI instance. A permanent test preserves the live worker and checks that
  no runner, factory, or progress invalidation is called on that refusal. Direct
  CLI stop also rejects the observed second-instance error at exit code zero.
  The owned-command fallback above replaces this refusal only after ownership and
  native confirmation succeed. The active Public
  firewall profile has enforced inbound allow rules for the exact probe and owner
  SQX executables. The narrowly scoped temporary rule helper lives at
  `.git/native-acceptance/isolate-probe.ps1`; it affects only the test executable's
  inbound TCP 5050. The owner approved its use: the rule was applied, verified
  enforced, used for one bounded run, then removed and verified absent. The owner's
  runtime and all existing firewall rules remain unchanged.
- Desktop API Stop acceptance passed:
  `.git/native-acceptance/20260905T012102Z-http-stop/report.json`, verdict
  `native_stop_observed_no_result_claim`. The desktop POST used the owned command
  service; native logs confirm Stop, saving two results with zero removals, and
  ordinary process exit. Files settled, protected snapshots matched, and no workers
  or shutdown errors remained. New archive `Strategy 4.2.186.sqx` is 64,176 bytes,
  SHA `97ee75f0759ee6980911d13e8584bf6b532bec087aefdef3e4a8d3a0d2c238c9`, with a
  valid native strategy and 192 Portfolio trades. Existing result reserialization
  preserved its trade/strategy/equity bytes and decoded statistics. This proves
  generation/save/Stop, not profitability, robustness, or target-code parity.
  Candidate import exposed and fixed an identity bug: archive format `1` was
  mistaken for runtime build `144.2953`. Shared readers now verify format separately
  from `StrategyFile/AppVersion`; strict Research checks require the supported build.
  Unsupported XML encodings become per-archive refusals, preserving healthy rows.
- Real-output HTTP custody acceptance passed:
  `.git/native-acceptance/20260905T013720Z-candidate-http-710cb599/report.json`.
  The exact new archive imported with HTTP 201, reopened at the same identity/revision
  after server restart, and duplicate import reused that Candidate with HTTP 200.
  Native trade readback returned 192 trades. Original receipt/store and both native
  snapshots stayed unchanged; no engine launched. This is Candidate custody plus
  Builder trade readback, not a Retester/Historical Result or validation pass.
- Native Retester continuation (2026-09-05): the first HTTP run returned 201/completed,
  but independent native evidence review refused acceptance:
  `.git/native-acceptance/20260905T015724Z-retester-http-2e65500a/native-review.json`.
  Native project start-to-finish was 1 ms with no Retest task execution; changed archive
  bytes were only equivalent settings serialization. That Historical Result is not
  verified execution and must not become a Higher Precision/Proof authority.
  Installed native code explains the skip: CLI `startOnlyTask` selects a task's generic
  name, while execution compares its custom name (`Retest strategies` versus `Retest`).
  The documented task index 1 is correct. A controlled `action=start` differential,
  guarded to one active Retest task, did execute the strategy:
  `.git/native-acceptance/20260905T020644Z-retester-http-be1b0dff/diagnostic-native-review.json`.
  Native counts were tested 1, passed 0, failed 1 (profit-factor filter). Its saved
  Retester profile is GBPUSD/H1/MT4, 2003-05-05 through 2018-08-30, with 683 output
  trades; the input Candidate was DJ/H1/MT5 with 192 trades. This demonstrates the
  command correction, not same-market parity, a filter pass, or completed cross-checks.
  Both runs preserved existing native projects/source stores and ended with no native
  processes. Temporary firewall isolation was removed after each run.
  The corrected production API then passed execution, restart, and duplicate reuse:
  `.git/native-acceptance/20260905T022425Z-retester-http-7afba440/report.json`.
  Requested and executed commands were identical (`action=start`); native evidence
  records one input, one tested, zero passed, one failed, and 683 returned trades.
  The shared gateway permits only one active Retest task and requires ordered task
  progress plus a fresh project log with a positive tested count. Legacy receipts
  remain readable as unverified and cannot authorize reuse, Higher Precision, or Proof.
  The frontend now previews the saved Retester market, engine, dates, and source hash;
  these are informational, and launch still re-derives settings from exact Candidate
  identity. A remaining failed Higher Precision HTTP receipt guard was corrected to
  accept current `start` and legacy `startOnlyTask` failures without accepting arbitrary
  actions. Current checks: 563 Python tests (547 passed, 16 skipped), 238 Node tests,
  and production boundary passed; subsequent native/browser/package gates are separate.
  Higher Precision acceptance from the verified baseline stopped before engine launch:
  `.git/native-acceptance/20260905T022929Z-higher-precision-http-3b2e3436/report.json`.
  `robustness_other_crosscheck_enabled` reports Additional Markets and Sequential
  Optimization enabled in the saved profile. No native process or new project was
  created; native/source snapshots stayed unchanged and temporary isolation was removed.
  This is an observed preflight refusal, not Higher Precision execution or validation.
- Owner-supplied historical assets were inspected read-only on 2026-09-05:
  `.git/native-acceptance/supplied-archives-inspection-20260905.json` and
  `.git/native-acceptance/supplied-data-inspection-20260905.json` preserve identities.
  The owner reports earlier full-pipeline passes; the supplied SQX archives expose Main
  results but no saved cross-check result members. `dow 1 hr.sqx` has 3,682 decoded trades
  on `@dow_ts/H1` and lacks producer-build metadata; `nqtrade.sqx` reports build 137.1749.
  `DOW BEST.sqx` is an unsupported non-ZIP native format, not presumed corrupted.
  The supplied workflow is a build-135.868, twelve-task project using `@ES_TS/H1`,
  including databank clearing and looping. It is context, not an approved blind launch.
  Five supplied price files passed basic CSV, OHLC, ordering, and duplicate checks;
  timezone/session/rollover coverage and exact native symbol mapping remain unverified.
  Read-only SQLite checks confirm `@dow_ts` is absent from both installed data catalogs
  and supplied Data.xml. Its archived Dow_TS tick size 0.25 / point value 50 differs
  from supplied YM/YR/DJ profiles; renaming those symbols would change test semantics.
  Next user-asset acceptance should load/migrate a copied existing strategy through
  the installed engine and preserve its original provenance; never invent build metadata.
  The owner clarified that the Dow strategy is intended for micros and its saved values
  are wrong. A separate prepared Settings copy now corrects only Dow_TS tickSize/tickStep
  to 1.0 and pointValue to 0.5, matching CME Micro E-mini Dow specifications:
  `.git/native-acceptance/dow-micro-profile-20260905/receipt.json` with runnable
  `verify_profile.py`. Original strategy/results are unchanged; this profile has not
  been imported or executed. Old zero-cost assumptions and data/timezone mapping still
  require verification; earlier pipeline passes do not establish corrected-MYM results.
  The additional `es 60 data.txt` was inspected separately: 117,845 valid OHLC rows,
  but 115,068 shared timestamps have different OHLC from `es1real.txt`. Neither source
  was merged or substituted. Six supplied price files contain 592,476 rows in total.
- Continuation browser/package acceptance (2026-09-05): 28 routes, Robustness, and Proof
  restart passed. The saved Retester preview matches exact native settings at 1440/960px
  with no overflow or page errors. GET-only visual inspection intentionally blocked two
  desktop-session POSTs; those induced console errors are recorded rather than hidden.
  Evidence: `.git/browser-acceptance/20260905T022242Z/`; all owned servers stopped.
  Fresh development build: `dist/development-retester-20260905/TraderCockpit.exe`,
  SHA-256 `9483acf5c8e7760950a1ecb518161271412e596e4df5c522c967069601aaba62`.
  `.git/package-acceptance/run-20260905T023209Z-33097fbd/receipt.json` passed two
  hidden native-window/HTTP lifecycle cycles, exact Idea reopen, six surfaces, seven
  plugins, and 27 knowledge entries with graceful shutdown and no forced cleanup.
  This is a development artifact; signed distribution, clean-machine acceptance,
  long-running Retester lifecycle, complete validation, and production readiness remain open.
- Data setup automation (owner request 2026-09-05): customers should choose their trading
  source/broker/instrument and receive source-backed settings, rather than reconstruct
  OHLC columns, timezone/DST, bar boundaries, and contract economics themselves.
  Official SQX broker profiles and build-144 MT5 direct import are the preferred native
  implementation seams. TradingView chart timezone is display context; Python has no
  universal broker clock contract. User geography never supplies historical-data timezone.
  First implementation slice is bounded native catalog/selection readback plus local CSV
  inspection in Data organization. Selected saved native metadata and detected file facts
  remain separate; neither authorizes native import or backtesting. Snapshot-bound selection
  refuses stale choices, and missing/conflicting metadata cannot become a green ready state.
  The supplied Vimeo `1162194873` page identifies “Forex Data Settings,” but its player
  reports “Unable to play media” and the inspection downloader received HTTP 401; its
  tutorial content has not been reviewed. Official documentation supplies the current
  technical evidence. No native process, account connection, or source-data mutation is
  authorized implicitly by reading a catalog or inspecting a file.
- Data setup implementation acceptance (2026-09-05): Data organization now reads the
  installed catalog and fills source-labelled reference fields from a snapshot-bound
  broker/dataset selection. The isolated installed catalog contains 129 datasets and
  1,080 shared session definitions; session availability is not an active-session claim.
  Raw CSV inspection detects columns, dates, finite OHLC errors, duplicate/order issues,
  clock ambiguity and spacing. Competing timestamp/date columns are refused. Native
  DATATYPE on DATA means start/end-of-bar convention, distinct from instrument asset class.
  Real browser uploads preserved exact original hashes for Dow (115,587 rows) and
  ES (117,845 rows), with missing timezone correctly unresolved. Native selection and
  file analysis survived shared custody refresh. 1440/960px checks found no horizontal
  overflow, page errors or console errors. Receipt: `.git/browser-acceptance/20260905T025519Z-data-setup/browser-receipt.json`;
  cleanup receipt confirms the owned server stopped and all three original hashes unchanged.
  Node suite: 243 passed. Final Python suite: 577 run, 560 passed, 17 skipped in 67.573s.
  An existing non-JSON HTTP test exposed a Windows unread-body connection-close race;
  its test now sends headers only while retaining the same 415/error assertions. The
  complete suite passed after that test-only correction. Production-boundary and diff checks passed. Independent review
  verified the conflicting-clock refusal. Direct MT5 import, persistent data selection,
  broker-source confirmation and applying settings to a native task remain unfinished;
  no file is imported and neither selection nor inspection grants backtest-ready status.
  Fresh development executable: `dist/development-data-setup-20260905/TraderCockpit.exe`,
  SHA-256 `93338062ada6984b6b07dd4261e29c8f7ef456fa313f4f1bdbcfa8bbc663b378`.
  `.git/package-acceptance/run-20260905T030958Z-1cd79116/receipt.json` passed two hidden
  native-window/HTTP lifecycle cycles, exact Idea reopen and packaged CSV inspection
  with the native installation deliberately absent; both cycles closed gracefully,
  no forced cleanup. This remains an unsigned development artifact, not a release gate.
- MT5 source decision (2026-09-05): owner chose MetaTrader 5 terminal and broker
  settings as the first automated source. Continue from Data organization with an
  explicit metadata connection to a selected running terminal, using the verified
  SQX installation's bundled MetaTrader5 Python dependency. Preserve raw symbol
  units and observed broker/currency context; do not turn current spread or tick
  values into historical execution assumptions. No automatic login, terminal launch
  on page load, trading commands or bar import belongs to this metadata step.
  MetaQuotes initialize can reopen a terminal that closes during connection; this
  API behavior must be stated, not hidden behind an impossible attach-only claim.
- MT5 metadata implementation and acceptance (2026-09-05): new explicit connection
  in Data organization discovers running terminal64 processes and binds a read to
  PID, executable path and creation time. The worker uses the installed dependency,
  verifies terminal/data-directory context before/after read, rejects broker changes,
  and exposes only broker company/server/currency plus allowlisted raw symbol fields.
  No account number, name, balance or credentials are returned. Reads are serialized,
  supervised and bounded to 30 seconds / 4 MiB; discovery never initializes MT5.
  Full Python suite: 588 run, 571 passed, 17 skipped (70.104s); Node: 247 passed.
  Focused native-adapter tests include numeric API-error red-to-green regression.
  Production-boundary/diff checks and independent privacy/lifecycle review passed.
  Real terminal discovery found mt5-17552, but initialization returned native API
  error -6 (authorization failed). `.git/native-acceptance/20260905T032651Z-mt5-metadata/receipt.json`
  records HTTP409 and zero remaining owned workers. Successful broker metadata
  readback remains blocked on signing in inside the user's MT5 terminal; no credentials
  were requested by or sent through the product. Do not call this successful connection.
  One actual UI read reproduced the same sanitized refusal at 1440/960px while keeping
  the selected native dataset and original Dow file analysis across shared refresh.
  Receipt: `.git/browser-acceptance/20260905T032754Z-mt5-one-read/browser-receipt.json`.
  No page errors or horizontal overflow; the expected HTTP409 console notice is retained.
  Owned browser server stopped and original hashes were unchanged; user MT5 stays open.
  Development build: `dist/development-mt5-setup-20260905/TraderCockpit.exe`, SHA-256
  `2a5670f88f013ee9027fbac55c3c785c42449528f0c086980e725215dca07117`.
  `.git/package-acceptance/run-20260905T032918Z-2e8d7a1b/receipt.json` passed two native
  window/HTTP restart cycles, exact Idea reopen, CSV inspection and MT5 missing-runtime
  refusal with graceful cleanup. The isolated probe source is bundled in the executable;
  a signed release and successful packaged live broker read are still unverified.
  Next dependent work: authenticate and verify actual broker/symbol readback, then
  connect producer-backed bar import and exact native task application. Commission,
  slippage, historical timezone/DST and sessions remain explicitly unresolved.
  Authentication follow-up (2026-09-05): owner supplied a local credential file and
  authorized its use. One explicit login attempt read its Server/Login/Password
  fields inside the isolated process; MT5 again returned API -6 (authorization
  failed). No secret values entered commands, receipts or repository files.
  `.git/native-acceptance/20260905T033450Z-mt5-signin/receipt.json` records the
  sanitized refusal; the same user terminal remains running. Verify credentials
  or complete login directly in MT5 before retrying. Product code was unchanged.
  Authenticated follow-up (2026-09-05): owner updated credentials and logged in.
  The product attached to the new running terminal (mt5-5996) without reading or
  resending the credential file. Authentication is now verified. The first full
  catalog read hit the real 4,096-symbol bound; literal native symbol search was
  added with unchanged caps, no silent truncation, wildcard/list syntax rejection,
  and exact filter provenance. Regression tests went red then green for the large
  catalog/filter path. `.git/native-acceptance/20260905T034834Z-mt5-metadata/receipt.json`
  records HTTP200, MetaQuotes-Demo/USD, one EURUSD symbol and zero owned workers.
  This demo server is the observed metadata source; it does not establish parity
  with another broker or the owner's historical Dow micro profile.
  Actual UI acceptance at 1440/960 matched displayed fields to the same HTTP200
  response, retained DJ dataset/Dow file analysis across refresh, and proved typing
  another search neither reconnects nor relabels the prior observed snapshot.
  `.git/browser-acceptance/20260905T035035Z-mt5-filtered-read/browser-receipt.json`
  and cleanup receipt show no page/console errors, owned server stopped, unchanged
  originals and the user's MT5 terminal left running. Final checks: 589 Python tests
  run, 572 passed, 17 skipped (69.319s); 249 Node tests passed; production-boundary,
  diff checks and independent filter/provenance review passed.
  Fresh build: `dist/development-mt5-search-20260905/TraderCockpit.exe`, SHA-256
  `c2ed2ed05bac75da07abb34e89569510e3d8d2e8333f3c57a698d3fe0e805850`.
  `.git/package-acceptance/run-20260905T035200Z-76b19721/receipt.json` passed two
  missing-runtime restart cycles and a third native-window/HTTP cycle performing
  the actual packaged EURUSD metadata read. All closed gracefully, with no forced
  cleanup; exact Idea reopen and unchanged native catalog verified. This closes
  broker metadata connection acceptance, including the packaged worker. Native
  history import, exact task application, historical costs/sessions/DST, signed
  distribution and commercial production acceptance remain open.
  History continuation (2026-09-05): the same supervised MT5 worker now supports
  explicit, bounded price-history capture for an exact broker, symbol, timeframe
  and UTC date range. Source bars are checked for numeric/OHLC validity, ordering,
  range and elapsed nominal bar duration. UTC/start-of-bar comes from the MT5 API
  contract; broker timezone/DST and sessions remain unresolved. A hash-bound CSV
  and manifest use the existing immutable evidence store, with verified CSV export.
  Data organization exposes this read and download; changing source/range clears
  stale results, and neither input changes nor downloads reconnect automatically.
  `.git/native-acceptance/20260905T041143Z-mt5-history/receipt.json` verifies
  96 actual MetaQuotes-Demo EURUSD/H1 bars, 2026-08-31T00:00 through
  2026-09-03T23:00 UTC, exact CSV hash/bytes and zero remaining owned workers.
  The first request returned no history; a subsequent bounded availability probe
  found current bars and the unchanged requested range then succeeded. The initial
  refusal is retained at `20260905T040952Z-mt5-history`; this is not proof of
  reliable cold-terminal history availability. Empty responses remain explicit,
  with retry guidance, and do not create captures.
  `.git/browser-acceptance/20260905T040704Z-mt5-history` verifies the same real
  flow and downloaded bytes at 1440/960, stale-result clearing without another
  read, and the corrected themed 34px date fields. The owned server stopped;
  original Dow file/native catalog hashes and the user's MT5 terminal were preserved.
  Checks: 598 Python tests run, 581 passed, 17 skipped (70.041s); 252 Node tests
  passed; 17 MT5 tests passed again after the retry guidance; production boundary
  passed. Independent review closed the missing-store and unfinished-bar findings.
  Packaged history acceptance passed four graceful open/close cycles at
  `.git/package-acceptance/run-20260905T041537Z-84ee2da6/receipt.json`, including
  actual history capture and identical CSV export after reopening without SQX.
  Native import and task application still require justified native
  broker/instrument/cost settings.
- Owner UI ruling (2026-09-05, `ui.png` and `ui2.png`): remove the obsolete global
  workspace/provider/compute/search/notification strip and live-account status footer.
  Both renderers, refresh targets and dedicated styles were deleted. The six-section
  rail and page controls remain; the separately rendered market ticker is unchanged.
  `.git/browser-acceptance/20260905T041808Z-six-surface-space` checks all six sections
  at 1440/960: no legacy DOM, no document overflow, and 846px of content height in a
  900px viewport (108px recovered). All 251 JavaScript tests passed; one obsolete
  footer-summary test was removed. Independent review found no remaining blockers.
  Updated build: `dist/development-data-workspace-20260905/TraderCockpit.exe`, SHA-256
  `67b8b94e30057f6a7f66d7a0a217d4e1ca082a193d49d23342eb0947aac3b461`.
  `.git/package-acceptance/run-20260905T042013Z-1250959e/receipt.json` passed all four
  cycles, actual 96-bar history capture, durable identical CSV re-export and graceful
  shutdown, with zero forced cleanup. This remains a development build; signed
  distribution and full commercial production acceptance are still open.
  Final browser checks passed: `node tests/run-browser-regression.mjs` (28 routes,
  using the owned runtime-absent base server), `run-browser-robustness-regression.mjs`
  (Home page link through Robustness), and `run-browser-proof-regression.mjs`
  (durable Proof restart). Initial broad harness setup lacked its base server;
  after binding the documented runtime-absent base, the unchanged gate passed.
- Six customer surfaces agree across frontend, backend registry, and Apollo navigation.
  Builder and Custom projects share their wrapper; Prop Monte Carlo and Prop analytics
  follow Overview for the selected strategy. Optional plugin catalog/install presentation
  is hidden while backend inventory remains intact. Getting started points toward the
  core workflow. Actual 1440px/960px browser inspection found and fixed the oversized
  task column and logs overflowing their scroll container. Installed prop panes render
  with the native archive. GET-only browser acceptance captured both iframe
  `ORDERS_RESPONSE` payloads with 192 orders and statistics with `NumberOfTrades=192`,
  including URLs requesting OOS/Long. The plugins always request full sample/both
  directions; their toolbar now states that fixed scope instead of offering ineffective
  selectors. Monte Carlo uses an unseeded binary win/loss model and omits daily/trailing
  loss rules and deadlines. Prop analytics starts from editable, unverified account and
  contract assumptions. Each pane now explains those limits; challenge calibration and
  reproducible firm-specific validation remain open. No native plugin math was changed.
- [Programming-guide integration catalog](docs/sqx-native-features/programming-guide-integration.md):
  38 programming articles, 14 CLI articles, and targeted linked API/reference pages
  reviewed against current callers. Scope exclusions are explicit. Official SQX MCP
  documentation exists; the product has no SQX MCP adapter. The former universal
  claim that no SQX MCP exists has been corrected in product descriptions.
- Linux acceptance remains unverified locally: `ssh sinbox` timed out; no running
  WSL distribution or Docker daemon is available. Do not claim a Linux release gate.
- Independent review at the band-removal repair checkpoint found no remaining findings in that bounded slice; this is not a review of subsequent databank drafts or commercial completion.
- Vault synchronization was attempted through the configured Manager projection
  tool with `--no-push`; it remains red on unrelated repository-sprawl/stale-graph
  and degraded source-collector checks. No duplicate project notes or replacement
  vault structure were created. The dated facts above remain the project source.

Earlier release-blocker inventory (preserved context; Gates 0–6 below control current sequencing):

1. Browser acceptance restored locally using #145; retain all three browser gates
   while native integration proceeds. This does not establish native acceptance.
2. Builder load/start/new-output/Stop and exact Candidate import/reopen now have
   isolated native receipts. Resolve the production native network boundary and
   supported control/telemetry connection. Retester/Higher Precision still have a
   60-second synchronous process ceiling; preserve source/configuration identity while
   completing their native acceptance on the isolated runtime.
3. Complete Candidate → Historical Result → validation → Proof native acceptance
   and Custom Project custody. Most robustness methods remain unlaunchable through
   Research. Verify prop-analysis input/rule assumptions and destination-code parity;
   MetaTrader export, Pine/Python conversion, and Apollo source-to-code workflows have
   distinct gaps recorded in the programming-guide integration catalog.
4. Review and complete existing consumer identity/membership/provider-credit lanes
   #99–#102 before commercial use. They are absent on main. Review must address
   locally editable identity/entitlement state, subscription expiry, and concurrent or
   repeated key provisioning; #104's replacement of a disabled key must not reset the
   intended consumer allowance. Do not bulk-merge this stack or rebuild its authority
   in a competing module.
5. Finish M5/M6: signed installer/update/recovery, first-run setup, clean-machine
   acceptance, support/privacy material, and the applicable engine/provider terms.
   Existing #105/#108/#116 contain related work and must be reconciled before reuse.

Older open PRs also describe functionality already on main; PR count is not a measure
of missing implementation. Preserve active lanes and the canonical module rail.
No native runtime, live-market values, validation success, or production readiness
is inferred from this audit or from fixture data.

## Approved execution ledger — Gates 0–6 (2026-09-05)

This ledger is the current sequencing within this existing plan, not a second roadmap.
The owner authorized the full agreed plan. Architecture defines ownership and behavior;
the backbone defines the detailed contracts and measurable acceptance. Older milestone
checkboxes, branch inventories and receipt counts above remain dated historical evidence.
They do not pass a gate or describe the latest uncommitted source automatically.

### Approved behavior

Keep the six surfaces and the removed global bands as ruled above. Builder and Custom
projects provide a persistent working databank dock, candidate details and stage history.
Import, reopen, rename, complete-archive save, copy, move, remove and clear operate on real
native artifacts without changing unrelated user files. Candidate identity is independent
of its current name or bank membership; externally imported candidates never receive an
invented Builder job or past validation.

Execution is **automatic with traceability after approval of the exact native graph**.
Native SQX owns its tasks, branches, loops and copy/move/clear semantics. Every visit is a
distinct stage attempt. Synchronous non-filtering native Custom Analysis (CA) capture checkpoints may be explicit parts
of an approved derivative native configuration so failed Retests and outputs survive
destructive bank operations. Preserve the user's original configuration. Unverified capture
coverage refuses a fully tracked launch. Do not implement a substitute native task engine.

Failed candidates retain their files and history until the user chooses deletion. Expose
both **Remove from this databank** (membership only) and **Delete candidate and retained
files** (preview affected results/history and reclaimable bytes, then explicit confirmation).
Permanent deletion actually purges unreferenced application-managed content, including large
staging/backup copies; shared content remains referenced and only a small deletion record is
retained. No autonomous cleanup of failed candidates is authorized.

### Current baseline and evidence boundaries

- Working lane: `codex/research-state-reliability`, HEAD
  `59c6c9c12aa331d37115b584817e781a4c41716c`, with protected concurrent uncommitted work.
  No new commit, merge or release is established by this ledger.
- Existing merged work includes #141's Custom Project list and #150's native SQStats
  databank grid. Open #148 contains translation/data/grid work and #149 template/title
  work. Gate 0 branch readback confirms `origin/main` equals the HEAD above; #148's
  `origin/cursor/pine-python-source-translation-79c6` is `d2f1b7b` and #149's
  `origin/cursor/custom-project-template-catalog-79c6` is `f1cbc40`. Reuse the already
  merged native grid/list. Omit bulk reuse of #148's competing computed grid and its
  reverted title/template flow; #149 template changes remain individually reviewable,
  outside this storage slice. Neither branch supplies the requested file operations or
  persistent dock. Protected dirty work remains in this same lane; no branch switch,
  reset, cleanup of source files, or blind merge was performed.
- The databank dock and native file actions are drafts. Whole-bank synchronization was
  found capable of deleting unrelated disk-only archives; these actions are not accepted
  for customer data. Scoped-save changes address the synchronization finding in code;
  actual native CRUD acceptance remains open. The first native attempt created
  a databank (HTTP 200), then refused archive load (HTTP 409) on a genuine colon-bearing
  ZIP member. That parser issue was corrected; the next actual blocker was a registered
  empty databank without a materialized directory, also corrected in code. The remaining
  actual archive/bulk/purge journey is unaccepted; neither create nor fixture success closes it.
- The `20260905T051239Z-databank` idle native GUI run (owned PID 14512) exited normally
  with code 0 and no forced cleanup. Its
  `.git/native-acceptance/20260905T051239Z-databank/cleanup.json` readback at
  `2026-09-05T05:30:35.1400460Z` verifies nine protected files unchanged, zero remaining
  native listeners and zero owned firewall rules. These nine protected files belong to
  the original user installation. Subsequent comparison of this run's probe snapshots
  found all nine pre-existing Builder/Retester result archives missing after exit; resource
  cleanup did not prove probe preservation. This is neither safe lifecycle acceptance nor
  Stop of an executing native project; the earlier forced-stop receipt below stays separate.
- The bounded roundtrip in
  `.git/native-acceptance/20260905T054207Z-databank/http-055027.json` passed actual
  create/load/save/repeated-load/rename/save. Repeated load retained the same Candidate
  and revision; renamed output SHA-256 is
  `614298c55c9c6f31fcd6e4b8971cd9631d4efd2be5a279092edf502331922e13`.
  The old name correctly refused readback and the source stayed unchanged. This receipt
  covers those operations before shutdown, not bulk operations, purge or Gate 1 acceptance.
- That run's `shutdown-preservation.json` records a later exit-0 preservation failure:
  four archives truncated to 54-byte ZIP headers and two others rewritten. Installed native
  code shows GUI-mode CLI `-exit` starts asynchronous databank synchronization without
  joining the writers, then exits the JVM. Original user-installation files stayed unchanged.
  `shutdown-recovery.json` verifies the nine original probe inputs restored exactly and
  the successful renamed output restored from custody. One corrupt, unreceipted output
  from a failed owned attempt was quarantined; it is not claimed recovered. This does not
  establish restoration of every older generated test project. The harness now omits CLI
  `-exit`; preservation through native `/main/exitapp` remains unverified.
- In `20260905T060535Z-databank`, firewall elevation was canceled. Root started the probe
  before checking that prerequisite, then immediately force-stopped exact owned PID 9988.
  `root-stop.json` at `2026-09-05T06:06:16.6615844Z` overrides the misleading
  `runtime.json` field `forced_cleanup: false`: exit was 4294967295 and the stop was forced.
  The runtime receipt's complete before/after probe and protected snapshots are identical;
  `cleanup.json` at `2026-09-05T06:15:14.5621920Z` confirms no remaining rules/listeners
  and records `native_acceptance: not_passed`. Root also stopped owned HTTP port 19600
  and verified two source hashes unchanged. No native retry is authorized without new
  user authorization at that point; the later reauthorized run is recorded below. The harness now checks both exact enforced ActiveStore isolation
  rules before spawning; `.git/native-acceptance/isolation-refusal-check.log` records
  the expected exit-1 refusal with those rules absent.
- Root observed three fresh Retester websocket states after the native `/engine/getInfo`
  refresh. Installed `EngineServlet.onGetInfo` clears the selected project's cached
  `engine-channel` data; the shared poller now uses that exact request after subscription.
  Twelve focused engine tests passed. No global `appSwitched` mutation was added.
- The repeated-intent defect (copy A to B, remove B, copy A to B again)
  is addressed by an explicit `operation_id` on rename/copy/move/remove/clear. Backend/API
  and browser changes are implemented; bounded independent review found no confirmed
  nonce or completed-journal garbage-collection defect. Exact retry/request binding,
  shared-reference retention and interrupted purge remain explicit contracts. Final focused
  reruns reported 26 action tests, 14 Candidate membership tests and 12 engine progress tests
  passed; the new-copy regression failed before the nonce fix and passed afterward.
  These checks do not accept whole-candidate purge,
  bulk operations against actual native archives, or the complete browser workspace.
- Full Python verification in `.git/gate1-python-tests.log` ran 640 tests in 106.178 seconds:
  OK, with 17 skipped. The final `.git/gate1-node-tests.log` rerun records 261 passed
  after all UI changes, including retry selection. Backend focused results remain
  26 action, 14 membership and 12 engine tests passed.
- `.git/databank-controls-browser.log` passes actual DOM selection, confirmation and
  retry/reload checks with injected API responses and zero real API requests. Interrupted
  Clear removes A, reloads with only B remaining, then retries the same original snapshot
  and operation ID without requesting another snapshot; successful Clear clears selection.
  The browser retains the full exact uncertain request and exposes pending retry after a
  lost rename response or partial Clear. Root also corrected retry selection, strict purge
  journal shape, and late receipt handling so an older operation cannot erase a newer ID.
  Final independent frontend review found no confirmed issue, including pending retry
  and selection. Browser contract results do not establish actual native bulk/purge or
  safe shutdown acceptance.
- The native `--full` harness includes operation IDs and the repeated-copy cycle. Its
  initial enforced-rule prerequisite refused before native execution. Root stopped owned
  HTTP PID 19064 on port 4383, verified two source hashes unchanged and zero listeners.
  The user subsequently reauthorized temporary isolation; both exact inbound block rules
  were enforced at `2026-09-05T06:30:32Z`, with all firewall profiles enabled.
- `.git/native-acceptance/20260905T063114Z-databank/http-063258.json` passes the actual
  isolated HTTP import/save/idempotent-load/rename/copy/same-ID retry/remove/fresh-ID copy/
  export/move/purge/reimport/clear sequence. Mutations were confined to fresh probe banks;
  source preservation was asserted. Purge readback verifies previewed files absent and
  shared artifacts unchanged. Independently measured existing reclaimed file bytes are
  2,973,715; the product reports 2,979,186, also including remove journals created during
  confirmation. These are distinct measurements, not an asserted equality.
- This run's normal native `/main/exitapp` returned exit 0 without forced cleanup and
  preserved the protected original installation. All eleven pre-existing probe archive
  hashes changed, but the corrected `shutdown-preservation.json` has no invalid archives:
  ZIP CRC and member sets are valid. The initial verifier incorrectly required
  `strategy.xml` instead of the actual `strategy_Portfolio.xml`; its result is retained in
  `shutdown-initial-verifier.json`. The corrected exact-byte preservation check still fails.
  `shutdown-semantic-comparison.json` independently verifies eleven exact strategy members,
  eleven exact orders members and 22 exact daily-equity members unchanged. All 276 scoped
  SQStats maps retain 38,124 named values and 38,136 full binary records, including twelve
  duplicate-name records. Result settings/values and native instrument settings are equal;
  eight rewritten `lastSettings.xml` members are equal after formatting normalization.
  Four archives changed serialization only; seven also changed display metadata: empty
  Note removal and cached equity display values, including extra Portfolio caches for
  GBPUSD. Existing statistics, strategy, trade and equity values did not change. The new
  archive hashes remain distinct custody identities; semantic equality does not authorize
  automatic reassociation. This is not evidence of corruption or loss on normal UI
  shutdown. No automatic restore or weakened association rule was applied.
  `cleanup.json` at `2026-09-05T06:38:11.8660757Z` confirms exit 0, no force, protected
  files unchanged, zero owned firewall rules and zero acceptance listeners; rule removal
  was authorized and succeeded. `custody-reopen.json` passes a fresh Python-process readback
  with the engine stopped: the removed Candidate retains its exact revision, two membership
  history revisions and zero active memberships; the purged Candidate refuses with
  `revision_deleted`. Native file-hash rewriting still makes the existing file association
  stale at that stage. This custody reopen
  does not close that defect. Gate 1 remains unaccepted pending live native reassociation
  and the remaining required acceptance. Gate 2 is limited to read-only seam
  investigation and has not advanced.
- The subsequent marker probe in
  `.git/native-acceptance/20260905T064949Z-candidate-token-fa2ea062/native-receipt.json`
  and `shutdown-receipt.json` proves a separate derivative's
  `SpecialValuesMap/SettingsMap/TraderCockpitCandidateTokenV1` survived native load, copy,
  rename, scoped save and normal shutdown. Original strategy, orders and equity members
  remained exact; original source bytes were unchanged. This is marker persistence evidence,
  not acceptance of the newly integrated import path or Retester lineage.
  `.git/native-acceptance/20260905T065226Z-databank/cleanup.json` at
  `2026-09-05T07:03:26.7719389Z` verifies exit 0, no force, protected files unchanged and
  zero owned processes, rules or acceptance listeners.
- New import/reconciliation code reserves the Candidate identity/token and original/prepared
  bytes before effects; retains an unpublished import root; verifies native output before
  publishing its linked revision and membership; and records explicit same-location
  reserialization with exact prior-hash/revision compare-and-set. GET remains read-only and
  matching tokens cannot establish global lineage. Completed import journals participate
  in owned purge; deleted operation IDs cannot resurrect a purged Candidate.
  `.git/gate1-import-actions-tests.log` records 30 passed; root reports 50 custody/helper
  tests and 264 Node tests passed, with mocked DOM acceptance. Bounded independent review
  found no confirmed blocking data-loss, identity, retry or purge defect and no plan drift.
  `first-import-phase-verifier-receipt.json` in the marker probe verifies retained actual
  native output with only the explicit filename-bound `ResultsGroup/@ResultName` allowance;
  generic reopen verification remains strict. This comparison made no new native call.
  At that update the integrated native import/reconcile flow had not been exercised; the
  subsequent actual receipts below supersede that narrower status. Unmarked legacy data
  still cannot automatically reconnect; pending failed imports have no cancel/reclaim UI.
  Gate 1 remains open; Gates 2–6 do not advance on tests or marker observations.
- `.git/native-acceptance/20260905T072909Z-databank/http-073011.json` passes 22 actual
  scoped HTTP operations through the new integrated import/CRUD/purge path. Reclaimed
  file-content bytes are 3,346,363 reported versus 3,340,892 independently measured before
  confirmation; the 5,471-byte difference is newly created removal journals.
  `http-073318.json` retains a Candidate for restart verification.
- After a real native restart,
  `.git/native-acceptance/20260905T073440Z-databank/reopen-http.json` preserves the same
  Candidate/revision. The old hash refuses with 409; explicit reconciliation and retry
  succeed, GET association returns, and save plus another rename succeed. Membership
  history is `admit / rename / reserialize / rename`; old and new archives remain retained.
  `interrupted-import-http.json` uses the supplied Dow archive and injects one local journal
  write failure after an actual native save: initial 409, then fresh HTTP/store
  `load-resume` succeeds twice with one native load and one Candidate. Original bytes
  remain unchanged, import history is unknown, and no backtest or project task ran.
- Both native sessions exited 0 without force and preserved the protected installation.
  Their `cleanup.json` receipts at `2026-09-05T07:39:35Z` verify zero owned processes,
  firewall rules and acceptance listeners. This completes those bounded integrated storage,
  restart and recovery observations. New-flow browser evidence still uses mocked APIs
  (264 Node tests); actual live browser/API acceptance is incomplete. Gate 1 remains open
  on legacy handling, pending-import cancellation/reclamation and remaining acceptance.
- The bounded Gate 1 pending-import discard slice implements explicit preview/confirm through
  the existing purge mechanism, limited to phase `prepared` and native disposition
  `not_submitted`. A bounded source audit could not prove import-writer quiescence from
  engine idle plus memory/disk absence: installed `libs.js:85761–85785` awaits the response
  without a drain/cancel token, and `_context` engine flags do not synchronize import workers.
  A timed-out HTTP request may still run. The purger rejects `confirmed_absent`; submitted
  intents refuse discard and remain available for resume plus ordinary Candidate purge.
  A future verified process/worker termination barrier requires proof before widening this
  scope. Never delete ambiguous output. Preserve originals/shared content,
  reject stale previews and retain an operation-ID tombstone. Independent plan review finds
  this aligned with approved explicit deletion and no automatic failed-candidate cleanup;
  no Gate 2 scope or new roadmap is introduced.
- The dock now previews/discards an exact never-submitted import and retains its confirmed
  deletion hash across reloads. A verified pre-intent refusal permits a fresh preview or
  resume; ambiguous failures and general purge errors retain the deletion retry. Independent
  review found and corrected both a stale-preview dead end and an ambiguous error-code
  interpretation. Neither correction permits deletion of submitted native work.
  Actual browser/HTTP evidence at
  `.git/native-acceptance/20260905T080933Z-discard-live-browser/receipt.json` uses the supplied
  Dow archive with a seeded never-submitted journal produced by the custody helpers. The
  production dock reads actual Builder banks and issues real preview/confirm requests.
  Preview dismissal changes nothing; the first successful confirmation response is deliberately
  dropped, then browser reload and exact retry complete with the same deletion. Both retained
  original/derivative blobs are absent, zero Candidates were published, and the original
  desktop file and native bank contents are unchanged. No SQX process, import or backtest was
  launched in this discard check. `cleanup.json` confirms the owned HTTP server stopped.
  This accepts that bounded local discard path, not the full customer/native databank journey.
  Gate 1 remains open for unmarked legacy handling and remaining whole-application acceptance.
- Current checks are recorded in `.git/gate1-discard-adapter-tests.log`,
  `.git/gate1-discard-full-python.log`, `.git/gate1-discard-node.log` and
  `.git/gate1-discard-browser.log`: 35 adapter tests passed; the complete product suite ran
  675 tests with 17 skips and no failures; all 265 Node tests and production-boundary checks
  passed. The browser control regression covers retained deletion
  after reload, unrelated selection/intent preservation, new previews after pre-intent
  refusal and exact load resume after a submitted-state refusal. The three required browser
  suites passed in `.git/native-acceptance/20260905T080600Z-discard-regressions/receipt.json`;
  its owned base server stopped. Fixture results remain distinct from the actual HTTP
  discard receipt and the earlier actual SQX CRUD/restart receipts.
  `.git/gate1-discard-cleanup.json` verifies the owned static server was stopped and zero
  listeners remained on the three regression ports. No commit, merge or release was made.
- Vault synchronization remains unresolved: the fresh `--no-push` run in
  `.git/gate1-discard-vault-sync.log` returned exit 1 for existing external control-plane/graph
  checks and an older unconsumed Inbox note. No unrelated remediation was attempted.
  Repository Gate 0 reconciliation does not imply a green or fresh vault synchronization.
- Cleanup readback at `2026-09-05T05:02:48.094377Z` is recorded in
  `.git/native-acceptance/20260905T043722Z-databank/cleanup.json`: nine protected original
  files verified, zero remaining owned rules and zero acceptance listeners. Native status
  is stopped; `native_forced_cleanup: true` records the watchdog-forced native stop.
  Successful resource cleanup is not graceful Stop acceptance.
- Earlier Builder/Candidate and corrected Retester execution/reopen/reuse receipts remain
  valid for their bounded claims. The Retester receipt has one tested, zero passed and one
  failed strategy; Higher Precision stopped at its profile guard. Neither is a completed
  tracked native graph or a passing robustness chain.
- Prop plugin data delivery and MT5 metadata/history capture have receipts above. Prop
  calibration, native data import/task application and destination-code parity remain open.
  The last recorded package is an unsigned development artifact, not commercial acceptance.

| Gate | Scope | Actual state at this update | Required acceptance before advancing |
| --- | --- | --- | --- |
| **0 — Reconcile and stabilize** | Reconcile the three authorities, preserve existing work, resolve branch reuse and clean up owned acceptance resources. | Complete for this stabilized baseline: all three authorities reconciled and independently checked on 2026-09-05; branch reuse/omission recorded above, protected dirty work preserved, cleanup verified by the cited receipt. Root-requested Gate 0 review is complete; no downstream gate is accepted. | All three documents agree with the approved behavior; no protected edits lost; specific reused/omitted branch changes recorded; owned test/native processes and temporary rules accounted for; root accepts the stabilized baseline. |
| **1 — Durable candidate storage** | Real-archive import/reopen/rename/save/copy/move/remove/clear, idempotence and permanent deletion. | In progress, not accepted. Integrated native CRUD/purge, real restart with explicit hash reconciliation, and interrupted-import recovery passed their bounded receipts. Native exit/cleanup completed without force and protected originals stayed unchanged. Earlier CLI shutdown corruption/recovery stays separate. Prepared-only import discard/reclamation now passes real browser/HTTP recovery with original/native files preserved. Unmarked legacy handling and remaining whole-application native acceptance are still open; temporary isolation is cleaned up. | Run every operation through the actual desktop/native path, reopen the same candidate/revision, repeat imports without duplicates, distinguish retries from later intentional actions, refuse stale/colliding requests, preserve unrelated files through shutdown, recover partial operations, preview deletion and prove unreferenced space is reclaimed with no hidden large copies. |
| **2 — Lossless stage capture** | Capture every admitted input and each visit through the approved native graph, including failure and destructive operations. | Not accepted. Individual execution receipts do not prove graph capture coverage. | Execute an approved native pipeline with pass and filter-fail outcomes, a loop and a destructive bank action. Reconcile every admitted input/visit with native logs, output identities and metrics; retain failed candidate files/history; prove capture checkpoints precede loss and do not alter native filtering or the original project. |
| **3 — Automatic tracked execution** | Run an approved batch/graph automatically with durable control and recovery. | Not accepted. Prior supervised start/stop/reopen evidence covers only narrower paths. | Exercise normal completion, Stop, crash, restart, retry, duplicate requests/events and partial outputs. No duplicate native run or overwritten attempt; all known outputs retained; unknown states stay explicit; worker cleanup and native process identity verified. |
| **4 — Coherent candidate workspace** | One candidate across banks, history, metrics, charts/trades, details and navigation. | Partially evidenced by previous route/custody tests; new dock and complete stage continuity unaccepted. | Trace the same selected candidate/revision/attempt through every view and reopen. Verify keyboard and bulk selection, empty/loading/error/stale/partial states, large grids and usable 1440px/960px layouts; no hidden required controls or substituted candidate. |
| **5 — Complete sellable research flow** | Data/profile continuity, native validation, calibrated prop analysis and supported destination export on the same chain. | Not accepted. MT5 capture, Retester execution and plugin render have distinct receipts; native import/profile application, validation coverage, prop calibration and export parity remain open. | Execute one complete real strategy journey with approved broker/instrument/data/engine/cost/sample context. Reconcile native and cockpit metrics under matching scope; verify required native validations, supported prop-rule boundary cases and destination compilation/behavioral checks. Unsupported rules/constructs and unverified translations remain explicit. |
| **6 — Commercial release** | Customer install, licensing/account/spend, reliability, update/recovery and support on an exact release build. | Not accepted. Current packages are development artifacts; signed distribution and consumer authority remain open. | On clean Windows, install the signed exact build, configure an authorized separately licensed engine/data, complete the full journey and reopen it, upgrade/rollback/recover without data loss, verify membership expiry/revocation and provider-enforced credit limits, and complete licensing/privacy/support/error guidance. |

Every gate records exact source/build identity, focused and repository checks, actual native
or provider observations where applicable, browser/desktop readback, restart/recovery evidence,
and independent review. A fixture, HTTP success, changed hash, rendered plugin or old receipt
cannot replace the specific required outcome. Gate 0 completion does not accept the Gate 1
draft; a passed gate is recorded only after its required work and evidence exist.

Document update order is architecture → backbone → this ledger. Update existing sections
when decisions or evidence change; do not append a competing architecture or roadmap.

## Discipline

Start every branch from current `main`, inspect `references/ui-authority` before UI work, keep
one branch to one coherent slice, update this plan only when real status or sequencing
changes, and delete the branch after merge. Do not switch/reset/clean another active lane’s
checkout.

### Publication verification — 2026-09-05

The owner-approved application dependency diff was reviewed and published in a 76-file native/read-model foundation (PR151) and a 46-file UI/documentation landing (PR152). The final dependency split includes matching native browser validators and intermediate surface compatibility; together the landings preserve the complete 119-file source change. Backend review found no actionable issue (114 focused tests, one skipped). UI review found stale inspected charts after archive removal/Refresh and missing-capital balance labels; both were corrected, including preservation of bulk selection and drawer state on unchanged Refresh.

Local final-source verification: production boundary passed; Python 679 tests, 17 skipped; UI 268 tests passed; all three required browser regressions passed in `.git/native-acceptance/20260905T095849Z-publication-diagnostic-regressions/receipt.json`. Earlier attempts recorded a Windows connection abort and intermittent route startup failures; the browser harness now includes bounded script/request diagnostics. Results scrolling/removal/Refresh acceptance passed at 1440 and 960 pixels. The inspected Strategy 3.3.115.sqx still hashes to `ccd41e713a879e030115d2b62be50547b65cdd096f3f3f3568dc4f183a74ab39`. Remote PR runs `33960028921` and `33960030861` passed Linux runtime acceptance and Windows packaging. Both PRs merged; final main `5260ed24ae6314f74153b504766294450ff521ee` passed post-merge run `33960230825`. The owned temporary publication worktree and merged implementation branches were removed.

### Shared workspace buildout — 2026-09-05

`codex/workspace-buildout` starts from published main `5260ed2`. Builder Progress
now leads with native run counters and chart frames, with saved setup beside it;
producer logs and editable quick settings remain accessible in disclosures. Full
settings uses grouped strategy choices and the existing native configuration
dialogs. All workflow tabs use a compact task selector and preserve the ordered
native task pipeline in an expandable sequence. Results keeps its independent
analysis/databank scrolling and sample controls.

Custom projects adds a case-insensitive search that survives native catalog polls.
Getting started retains its eight live/current zones and adds three working setup
links. Apollo adds keyboard-accessible prompt drafts that require Ask before
sending. Data organization places installed settings alongside file inspection
and terminal inputs. Settings groups existing read models into four anchored
sections. These are changes inside the existing application and native handlers;
no new dependencies, execution API, or quantitative producer were added.

Review artifacts: `.git/ui-review/pages/index.html`, with before/after captures of
seven views at 1440 and 960 pixels. `node tests/workspace-buildout-browser.mjs`
checks native disclosures, tab navigation, project filtering across a poll, Apollo
draft/focus, populated installed-data selection and CSV inspection, settings anchors,
six rails and page overflow. Fixture CSV inspection performs no native import or
backtest. Code review found and fixed the obsolete rule hiding the Full settings
task sequence and the missing Apollo composer focus outline; both review axes are
now clean. Production boundary, 679 Python tests (17 skipped), and 268 UI tests
passed. All three required browser regressions passed in
`.git/native-acceptance/20260905T103628Z-workspace-buildout-regressions/receipt.json`.
Final live page acceptance passed (`.git/ui-review/buildout-browser-acceptance.log`),
including task switching, Start cancellation, independently scrolling native
settings, and visible composer focus. Results scrolling/removal/Refresh checks
passed in `.git/ui-review/buildout-results-scroll.log`; the complete Results
dashboard confirmation and captures are in `.git/ui-review/buildout-results/receipt.json`.
The inspected native archive still has the SHA-256 recorded above. Plan audit:
ON PLAN. This continuation awaits visual approval before publication to main.
Native live charts require producer telemetry; missing bars, full native acceptance,
legacy archives, and commercial gates remain open as recorded in the gate ledger.
