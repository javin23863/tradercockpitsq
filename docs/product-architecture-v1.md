# Product Architecture v1

This document is the stable architecture authority for the platform.

## 1. Product identity

TraderCockpit is one desktop trading platform. The first sellable release is a Windows
desktop for building, backtesting, and validating strategies with a separately licensed
native engine. The owner-approved customer surfaces (2026-09-05) are:

`Getting started | Builder | Custom projects | Apollo | Data organization | Settings`

The platform owns its product identity and user experience. It is not named StrategyQuant X.
Quantitative click-into screens wrap the native SQX backend (Progress | Full settings | Results
against that module's `project.cfx`). The chrome is a 2026 facelift — new color and a tighter
layout — not pixel-cloned Java and not a second product spine.

The accepted visual/product authority for Home zones remains the neon cockpit pinned in
`references/ui-authority/` (`screenshots/*.png` + `manifest.json`). The 2026-09-03 owner
override for the **pipeline rail** supersedes the prototype's `Home | Research | Explore |
Automation` labels: those invented workspace names (`Signals & Models`, `Evolutionary Search`,
`Order Flow`, `Footprint`) are not SQX modules and must not sit on the left rail. Explore is
not a top-level tab; packaged SQX plugins stay in Settings / SQX Results.

The frontend is vanilla ES modules with no framework or build step.

The 2026-09-03 owner-intent revision is an explicit product-authority change for *behavior*,
not Home-zone chrome: actual OHLC bars, indicator/strategy/model maintenance, paper/URL ingest,
clarifying questions, Apollo product-control tools, and voice. Home's eight zones stay
exactly as pinned.

Every surface shares the six-section rail and the market ticker (one cell per watchlist
symbol plus a market-state cell from the market read model). The owner removed the global
workspace/readiness/search/notification strip and live-account status footer on 2026-09-05;
neither their DOM nor reserved layout space belongs in the product. Page-specific controls
and truthful readiness remain. Missing producer values remain unavailable. The strategy
databank dock described below is working research content, not a replacement status footer.

StrategyQuant X / SQX 144.2953 is a native historical-research backend producer where currently proven. It is not the platform name and not a user-facing workspace label.

## 2. Home and Research are separate domains

### Home

Home is the live/current Cockpit Home. Its zones are:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

plus an Apollo jump to the full-page `/apollo` rail. Each zone reads from the producer that actually owns the
current/live state. Market Overview reads `/api/market/quotes` and the live market context on
`/api/status`; System Status reads `/api/status`; Alpha Stack and Pipeline Overview may summarise
Research custody only as historical/research evidence, never as live or promoted truth; Signals,
Risk, and Performance stay explicitly unavailable until live producers exist; Quick Actions are
navigation only. Unavailable live producers render unavailable/stale/pending/error state rather
than fabricated values. Neon chrome and card density come from `references/ui-authority`; card
titles in `cockpit-home.png` are illustrative framing, not the Home zone contract.

### Research

Research is the historical strategy-research surface, composed of four workspaces — one per
prototype screen — selected by `/research?workspace=<id>&tab=<id>`:

- `signals` — **Signals & Models** (`Overview | Signals & Models | Order Flow | Footprint | Volume Profile | Liquidity Map | Replays | Alerts | Reports`). Overview holds Idea/source custody; Signals & Models shows the chart frame, the exact native Builder specification (strategy shape, market identity, data setup, blocks, rankings, cross-checks, money management, native search mode) and the Strategy Panel of enabled native signal blocks; the analytics tabs carry their full frames until a market-data provider exists; Reports lists immutable Proofs.
- `evolution` — **Evolutionary Search**: the native `BuildMode` GA parameters (population, generations, islands/migration, crossover/mutation, fresh blood, restart) and native `Rankings` fitness/acceptance conditions/stop condition from the **approved** configuration executable XML (not the live installed task), exact configuration compile → review → approve → launch custody, native job state, and Top Candidates (native Results import). Random Discovery vs Genetic Evolution is shown from that approved native selector.
- `validate` — **Test & Validate** (`Overview | Initial Test | Trades | Robustness | Configuration | Evidence`): candidate stage history follows the approved native graph, including repeated visits and failed attempts. Metrics, equity, trades, native acceptance outcomes and separate cockpit-policy summaries bind to the selected result/attempt. The historical seven-category verdict is not a fixed native funnel. Configuration and Evidence preserve the executed chain; prop analysis and supported exports keep the same candidate identity.
- `catalog` — **Indicators & Models** (`All Components | Indicators | Models | Strategies | Utilities | My Components`): every native building block from the exact Builder task with category/enabled/weight/parameter attributes, native templates, imported native strategies and Ideas; Models is the platform-owned ML modality (fit allowlisted sklearn classifiers on native trades, then bind the catalog digest onto an existing Candidate); Utilities hosts native project topology and preset verification.

The custody chain `Idea → Specification → Build → Candidates → Backtest → Robustness → Proof →
Delivery / Simulation` is folded into those workspaces rather than condensed; pre-prototype
`stage`/`tab` links canonicalise to the workspace routes so bookmarks and custody selections
(`configuration`, `proofEntity`, `validationRef`) survive. In-Research workspace and tab hops
copy those same non-structural identities; Home Quick Actions start without leftover IDs.
Prop analysis and supported exports remain attached to the selected research strategy in
Builder / Custom projects. They do not require an Operate rail item. Hosted delivery and
live trading remain outside this first-release scope.

Construct modalities stay distinct and feed the same downstream custody: Random Discovery and
Genetic / Evolutionary search (native SQX) and Machine Learning / Models (platform-owned, see 3).

Research objects the owner builds and maintains are **indicator**, **strategy**, and **model**.
Each has immutable revisions. Text, a URL, or a paper does not create a Candidate. A Custom
Project is the native plug-and-play runner for an approved backtest/robustness task sequence;
results still render on Test & Validate. The product must lead the user through the custody
chain (selected candidate, current native stage, and allowed next actions) and must not present StrategyQuant X Builder
tabs (What to build, Genetic options, Cross checks, Ranking, Notes, Money management) as
product navigation.

The Signals & Models chart card is an **actual bar chart**: OHLC bars from a market-data or
historical-bar producer for the selected symbol/timeframe, with native trade overlays when a
Historical Result is selected. Last/change quotes are not a substitute for bars. Unavailable
producers render `unavailable`; the UI never synthesizes candles.

## 3. Producer ownership

### Native historical-research producer

Native SQX owns:

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy generation/search;
- GA/evolutionary mechanics;
- native strategy/block semantics;
- historical backtesting;
- native fitness/ranking/filter calculations;
- robustness/cross-check methods;
- Retester;
- Optimizer and Walk-Forward execution;
- Custom Project execution/task semantics;
- native `.sqx` strategy/result artifacts.

The authorized installed SQX 144.2953 program is the primary executable specification for how those capabilities are configured, persisted, launched, and read back whenever that runtime is available. Direct runtime observation, saved native artifacts/configurations, screenshots, and real bounded scenario runs establish integration behavior. Retained/decompiled source and archived references are supporting implementation aids, not prerequisite evidence gates when the running producer can answer the question directly.

A missing integration seam does not transfer this authority into platform-owned substitute algorithms. The product must inspect/run the actual producer, use source/reference material only where it clarifies non-observable details, or expose the capability as unavailable. It must not block an observable integration on a separate retained-evidence checkpoint.

### Platform application authority

The platform owns:

- desktop lifecycle and navigation;
- Home/live-current presentation from correct producers, including the live-market provider seam (`tradercockpit.market_data.MarketDataProvider` → `/api/market/quotes` and `/api/market/bars`) with an operator watchlist and truthful `provider_not_configured` — never fabricated symbols, prices, timestamps, or OHLC bars;
- consumer identity/account state;
- bounded external model access and policy, including Apollo product-control tools, voice-to-text into `/api/assistant`, and source ingest (URL/document → hashed Idea spans);
- idea/source revisioning and provenance;
- exact native configuration mapping, review, approval, and custody;
- native runtime verification/control/readback;
- product identities around jobs/candidates/results/proofs;
- Candidate Lab presentation;
- Backtest and Proof presentation;
- Automation presentation/control boundaries;
- capability/add-on registration;
- structured refusal when a producer is unavailable.

Producer-neutral lifecycle/custody envelopes are allowed. They must not become hidden alternative quantitative engines.

### Machine Learning / Models modality (platform-owned)

The Machine Learning / Models modality is platform-owned and distinct from SQX's owned
semantics. It applies standard, well-known ML libraries (decision trees, forests, gradient
boosting, neural nets, classifiers) across indicators/strategies/assets to produce
signals/features/models. Its artifacts flow into the same Candidate → Backtest → Robustness →
Proof custody, where historical evaluation and robustness remain owned by native SQX wherever
SQX owns that behavior. This modality is NOT a substitute for SQX Builder/GA/backtest/robustness/
optimizer/Custom-Project execution (which remain forbidden to duplicate); it is a separate,
explicitly-scoped research capability. Model mathematics is grounded against the curated quant
knowledge library rather than invented. Fitted catalog artifacts bind onto an existing native
Candidate as a digest pointer; the bind never creates a Candidate from a pickle and never loads
the estimator. Historical evaluation and robustness remain native SQX. The catalog reports
truthful unavailable state when the sklearn backend is not installed.

### Assistant (Apollo) and knowledge library (platform-owned)

The full-page Apollo rail (`/apollo`) is the bounded LLM surface. Home shows a jump to that rail
instead of a second thread. Research workspaces keep the compact widget. The backend transport
(`product/tradercockpit/assistant.py`, `/api/assistant` GET status / POST message, loopback only) calls OpenRouter's OpenAI-compatible
chat endpoint with the operator credential from `OPENROUTER_API_KEY`, the backend model policy
(`z-ai/glm-5.3-flash` default, `TRADERCOCKPIT_ASSISTANT_MODEL`,
`TRADERCOCKPIT_ASSISTANT_FALLBACK_MODELS`, `TRADERCOCKPIT_ASSISTANT_MAX_OUTPUT_TOKENS`), a
grounding system prompt and a secret-free read-model context (runtime status, custody counts).
The widget (`web/assistant.mjs`) keeps a bounded in-session thread, posts `{message, history}`
and renders the typed reply (`tc.assistant-reply.v1`) or the backend's exact error; it is never
disabled — `/api/status` (`assistant`, `model`, `provider`) describes readiness truthfully and an
unconfigured provider answers `provider_not_configured` in the thread. It is grounded
against a curated Quant-Guild catalog
(`https://github.com/romanmichaelpaolucci/Quant-Guild-Library`) for anti-hallucination:
public lecture titles and source URLs plus platform-authored cockpit notes, retrieved into
`/api/assistant` replies as citations. Lecture notebooks and transcripts are not stored.
Request-time retrieval still runs on every message. Mid-turn the backend may advertise one
approved tool, `retrieve_quant_guild`, for extra catalog notes (max two extra rounds).
Unknown tool names and extra argument keys fail closed and never execute. The knowledge
library is reference data (ingested/retrieved), never a runtime code import (section 11).

Owner intent expands Apollo from retrieve-only to a **bounded product operator**:

- **Source ingest** — a URL or document becomes an Idea revision with content hash and
  quoted spans. Apollo drafts indicator vs strategy vs model meaning only from those spans.
- **Clarifying questions** — unresolved Specification fields are asked as typed questions
  with allowed answers; Build stays locked while required meaning is unresolved.
- **Approved product tools** (in addition to `retrieve_quant_guild`) may navigate, draft
  Idea revisions, propose Specification fields, request compile, and request launch. Launch
  still requires exact approval and the trusted gateway. Apollo never writes executable XML,
  never invokes `sqcli` from the browser, and never skips runtime verification.
- **Voice** — microphone audio is speech-to-text into the same `/api/assistant` message
  path. The transcript is shown. Mutation still requires confirmation. Missing mic/STT is
  `unavailable`, not a second assistant.
- **TradingView and MetaTrader MCP** — process-side Apollo/LLM tools
  (`TRADERCOCKPIT_TRADINGVIEW_MCP_URL`, `TRADERCOCKPIT_METATRADER_MCP_URL`) so the
  assistant can interact with those platforms. They are not Automation, not Custom
  Project control, and not Home/Operate market or broker producers. Tokens never enter
  the read model.

Apollo never owns producer truth, never becomes a result/quantitative authority, and never
mutates native state *directly*. Product-control tools are application mechanics that call
the same custody APIs a human click would. This bounded assistant is explicitly distinct
from the forbidden legacy "persistent Apollo product spine" (section 11).

Primary literature is a second citation catalog (White 2000; Hansen 2005; Bailey et al.
2014; Bailey & López de Prado 2014; Harvey, Liu, Zhu 2016; López de Prado 2018; Pardo;
Tharp R-expectancy; Sharpe 1966/1994; Wilder 1978) stored as platform-authored notes with
bibliographic pointers — the same no-transcript, no-formula-invention rule as Quant-Guild.

### Cockpit validation verdict (platform-owned)

The seven named categories below describe an existing cockpit policy over bound results.
They are not a required seven-task native pipeline, a scheduler, or a substitute for the
user's approved native graph. The active pipeline display follows that graph's actual
tasks, branches and loop visits. Policy summaries identify their evaluator and inputs
separately from native execution stages. A native filter failure is executed work with a
failed outcome; it must not disappear or become a failed-to-execute result.

StrategyQuant X produces the backtest and its exact native trade records; the cockpit owns the
verdict. `product/tradercockpit/research_verdicts.py` attaches `cockpit_verdict`
(`tc.research-cockpit-verdict.v1`) to the Historical Result detail read model:

- **Statistics** — the SQX databank columns used by native acceptance conditions (`NetProfit`,
  `GrossProfit`, `GrossLoss`, `NumberOfTrades`, `WinningPct`, `ProfitFactor`, `Drawdown`,
  `DrawdownPct`, `ReturnDDRatio`, `AvgTradesPerMonth`, `Expectancy`, `MaxConsecLosses`, …) are
  recomputed from the native trade rows with the published SQX column formulas (initial capital
  from the native `MoneyManagement`), per native sample type (in-sample 10–19, out-of-sample
  20–30, full 127). `AvgTradesPerMonth` uses the result `settings.xml` Setup `dateFrom`/`dateTo`
  chart history range when exactly one dated Setup is present; otherwise it uses the traded
  span and reports `months_basis`.
- **Stages 1–2 (native conditions)** — the exact Rankings conditions and
  `RetestWithHigherPrecision` acceptance conditions of the approved Builder task (read through
  custody: result → candidate → configuration → executable task XML) are evaluated over the
  Retester result and the bound Higher Precision result respectively. Columns the cockpit cannot
  recompute (walk-forward, confidence-level Monte Carlo, parameter stability) remain
  `unevaluated` and make the stage `incomplete` until the native result archive carries those
  producer-recorded last-result values. Bound robustness results for the catalogued CrossChecks
  methods (additional markets, What-If, permutation, Monte Carlo, walk-forward) feed the matching
  policy categories. A method's launch/readback coverage is reported independently; presence
  in the catalog does not establish a working adapter or successful native execution.
- **Stages 3–6 (cockpit policy)** — Golden Validation (Initial criteria re-verified on the
  higher-precision result plus profitable calendar years), Scenario Tests (profitable calendar
  quarters, single-year profit concentration), Stress Tests (seeded trade-order shuffle with
  random trade skipping over the native trade list: 5th-percentile net profit, 95th-percentile
  drawdown vs observed, max consecutive losses) and Out-of-Sample (native out-of-sample trades:
  count, net profit, profit factor, retention vs in-sample). Thresholds live in
  `DEFAULT_VERDICT_POLICY` with a `TRADERCOCKPIT_VERDICT_POLICY` JSON override and are reported
  in the read model.
- **Stage 7** — Proof custody bound to the result.
- Stage states are `pass | fail | incomplete | not_run`; the overall verdict is
  `pass` ("Robust & Deployable"), `fail` ("Rejected"), `incomplete` or `in_progress`. The verdict
  never re-runs a strategy, is always attributed to the cockpit (`authority: tradercockpit`), and
  the UI renders only the backend read model.

## 4. Native authoring/control hierarchy

Use the smallest actual native capability that serves the user path:

1. native SQX AI Wizard / AI Assistant + AlgoWizard / Builder for native authoring/generation;
2. verified StrategyQuant X runtime + trusted launcher for Custom Project start/stop and native task execution;
3. optional `sqx-lab` custom native-artifact tooling only when explicitly needed;
4. platform orchestration/custody/presentation around those producer capabilities.

Official StrategyQuant X MCP documentation exists, but this product has no SQX MCP adapter. Do not invent an implemented tool list or producer identity. TradingView and MetaTrader MCP are Apollo/LLM tools only; they are not Custom Project control.

Custom Project Full settings are the actual `<Settings>` children of the saved task XML. The desktop may write only attributes or existing text on those existing elements. It must not invent extra SQX parameters, engines, symbols, Condition rows, What-If scenarios, or a closed tab enum.

When exact native behavior is uncertain and the installed runtime is accessible, determine it by exercising the program before designing another platform abstraction. Source/decompiled inspection is secondary to that executable observation unless the required detail is not externally observable.

## 5. Consumer account and model access

Google authenticates the consumer to the platform; it is not an OpenRouter login.

Required architecture:

`verified Google identity -> stable platform account -> configured allowance -> provider-bounded spend authority -> backend-selected model policy -> account-attributed usage/readback`

Rules:

- provider provisioning/management credentials remain server-side;
- provider-enforced per-consumer limit/reset/expiry/revocation is the monetary boundary;
- a local credit counter is not the sole hard spend limit;
- starter/plan amounts and renewal rules are configuration, not source-code guesses;
- account history and model policy are separate authorities;
- current default workhorse is `z-ai/glm-5.3-flash`, replaceable through backend configuration;
- exhausted/revoked/lapsed state refuses before further spend;
- account grant admission requiring monetary authority must be correct across multiple writer processes.

External LLM transport may assist with intent, summaries, approved tools, and extensions. It does not own quantitative producer truth.

## 6. One application/runtime family

The product has:

- one canonical Python application server;
- one `web/` UI;
- one desktop host around that same server/UI;
- one state/custody family;
- one native-research gateway/runtime-verification family;
- one product identity chain for idea/configuration/job/candidate/result/proof.

Do not create a second server, account authority, result authority, strategy engine, or UI product spine to avoid integration conflicts.

The desktop private server is loopback-only, validates its exact Host, and rejects cross-origin browser mutations. Browser code never invokes native processes directly. The last registered product path, including Research custody query keys, is stored in the data-root session file and restored on the next launch unless `--start-path` is supplied. That session is launch restore only; it is not a second product spine.

## 7. Native runtime trust

Before native execution:

- verify expected build/runtime identity;
- verify the executable launcher identity using a trusted digest where required;
- verify relevant native engine artifacts separately where required;
- resolve project/configuration paths physically and keep them inside the authorized runtime;
- reject symlink/junction/path escape;
- preserve exact configuration/archive bytes and hashes;
- fail closed on missing/mismatched runtime, launcher, configuration, project, or artifact state.

Runtime trust is a security/integrity boundary. It is not a requirement that a user’s current native project/configuration bytes equal one archived reference blob.

Three identities must remain separate:

- runtime trust authorizes the installed build and configured launcher to execute;
- artifact custody preserves the exact bytes and hashes used by each operation;
- producer validity is established by required native structure and the authorized producer's own load/execute/output behavior.

An installed engine-library digest may be captured as immutable execution provenance without becoming a compiled-in validity oracle. Pinning a library digest for authorization requires an explicit independent security policy and configuration authority; a hash recovered from retained evidence is not sufficient justification.

## 8. Identity, custody, and proof

- Text entry alone does not create candidate or run identity.
- A candidate identity is bound to a real producer artifact.
- Exact native configuration bytes and producer build identity are durable custody.
- Custody hashes identify what was used; they do not by themselves prove that only those bytes are producer-valid.
- Native archive/result identity is preserved by content/provenance.
- Mutable current pointers reference immutable events/objects rather than rewriting history.
- Generated, tested, passed, promoted, exported, and deployed remain distinct states.
- Proof binds idea/source, approved configuration, producer/runtime/job, data/settings, native artifact, result/trades, validation outcomes, and current product status.

### Candidate, databank, and stage continuity — owner-approved 2026-09-05

A **Candidate** is the maintained strategy identity. A **Candidate revision** identifies
exact native artifact content. A **databank membership** places a candidate in a native
working bank; membership is not ownership of its history. An external archive import records
its actual source and native compatibility without inventing a Builder job or past tests.
Generated candidates retain their real build/configuration/job ancestry.

A **pipeline** is an approved native Custom Project graph, including its actual tasks,
conditions, copy/move/clear actions and loops. A **stage attempt** is one candidate's visit
to one task in one execution; a loop creates another attempt rather than overwriting the
first. Each attempt binds its input revision, task/configuration, producer, outputs,
statistics and outcome. Missing output or capture remains explicit.

The approved mode is **automatic with traceability**. After the user approves the exact
native graph, native SQX executes it automatically; the cockpit captures and presents the
execution without requiring another click at each ordinary stage. This does not authorize
a platform-owned task-loop engine or unapproved changes to native semantics. Synchronous,
non-filtering native Custom Analysis (CA) capture checkpoints may be explicit parts of an approved derivative native
configuration to retain failed Retests and survive destructive databank actions. Preserve
the original user configuration. Unknown capture coverage refuses a fully tracked launch;
it must not silently run and infer missing history afterward.

Failed candidates and their files/history remain retained until the user chooses deletion.
There are two distinct actions:

- **Remove from this databank** removes the selected membership only. Candidate history and
  retained files remain available.
- **Delete candidate and retained files** previews affected results/history and reclaimable
  bytes, then actually purges unreferenced retained content after explicit confirmation.
  Shared content remains while referenced by another candidate. Keep only a small deletion
  record; do not hide large copies in staging, backups, caches or evidence storage.

Immutable revisions are not edited in place; explicit user deletion governs their retention.
No automatic purge of failed candidates is authorized. Copy/move/rename/export and retries
must preserve identity and unrelated user files. Native synchronization that can delete
unrelated disk-only archives is not a safe substitute for a selected-record operation.
Discarding a pending import is likewise explicit preview/confirmation. Only a prepared,
never-submitted import may release its unreferenced retained copies. Submitted imports
remain retained for resume and ordinary Candidate deletion: idle status and absent files
do not prove that an outstanding native import writer has stopped. No automatic cleanup
or deletion of ambiguous native output is authorized.
The dock retains an explicitly confirmed import-deletion request across reloads and
retries that deletion after an uncertain response. Only a distinct, verified pre-intent
refusal permits a new preview or import resume; an error from deletion already underway
must not release the confirmed intent. Discard removes no original desktop import or
independently saved export and never publishes a fictitious saved Candidate.
Each journaled import/rename/copy/move/remove/clear or explicit reconciliation action has a distinct user-intent operation identity: retries reuse that
identity, while a later intentional repeat gets a new one. Completed copy/remove journals
must not suppress a subsequent copy to the same destination. Native lifecycle acceptance
also requires archive preservation after shutdown; exit code 0 alone proves neither a
completed save nor safe shutdown. The current observed lifecycle defect and recovery are
recorded in the living plan; Gate 1 remains unaccepted.

An imported archive may receive a separate metadata derivative with a newly reserved
Candidate token; its original bytes remain retained. The token binds an already known
Candidate and location, never discovers lineage or proves validation. Native reserialization
requires explicit same-location, prior-revision/hash-bound reconciliation and complete
artifact verification before updating membership. GET readback never mutates custody.
Unmarked legacy archives do not reconnect automatically. Importing an exported marked
archive creates a new reviewed derivative and unknown import history, not inherited results
or validation authority.

The candidate remains selected across the persistent databank dock, stage history, metrics,
trades, charts, prop analysis and export. Each view names the selected revision/attempt and
data/sample/direction scope. Native databank statistics and cockpit-recomputed metrics
identify their separate authorities and explain any differences.

Owner clarification (2026-09-05): persistence is within the selected module/project and
workflow task context, not application-wide. Tasks reference project-owned input/output
banks and may share a bank. The user's authored Custom Projects define this flow; the UI
must expose those bindings and follow the selected task rather than carry an unrelated bank
through task changes. A current shared bank is not a historical snapshot of each task visit.

Prop analysis is first-release research functionality bound to actual candidate trades and
an explicit challenge-rule version, capital/sizing/cost assumptions and clock conventions.
Unsupported daily/trailing drawdown, deadlines or other rules cannot yield a firm-specific
qualification claim. A rendered native plugin is not proof of calibrated challenge behavior.
Exports to supported MetaTrader, TradingView and Python targets retain the same source chain;
download, compilation and behavioral validation are distinct states.

## 9. Native SQX modules and Custom projects

The left rail is `Getting started | Builder | Custom projects | Apollo | Data organization | Settings`.
Builder is a module archive (`GET /api/sqx-module?module=Builder`) bound to
`user/projects/Builder/project.cfx`. Retester and Optimizer are native SQX module archives,
not left-rail items; `/retester` and `/optimizer` redirect to `/builder`. A Custom Project
may still contain a Retest task. Custom projects remain the saved named workflows under
`user/projects` excluding those module folders (`GET /api/sqx-projects`). Builder and
Custom projects open the same Progress | Full settings | Results shell against that archive.
Data organization discovers existing native data and supports explicit provider-backed
metadata/history capture with provenance. Capture, native import, and application to an
approved task are separate states. Missing broker timezone, sessions, or costs are not inferred.
Apollo is the full-page bounded assistant on the former AlgoWizard rail slot. Native
AlgoWizard / AI Wizard authoring stays in StrategyQuant X; this desktop does not invent
a block editor.

Custom Project task execution remains native. The owner-facing job is one confirmed “run this
approved project” action; results render from that module's databanks. The platform must not
invent a task-loop engine or pixel-clone StrategyQuant X Java chrome.

The 2026-09-03 owner override for Custom projects is the official list structure with a 2026
facelift: each saved archive is a flat row with name, `[ Tasks (n) ]` `[ Engine ]` `[ Results ]`,
progress, Stop / Pause / Start, `DATABANKS` / `STRATEGIES`, and a gear into Full settings.
Create new project stays fail-closed until a native create path exists. Open existing is the
verified `user/projects` catalog, not a browser path picker. Personal SQX project names are
not hard-coded as product rows.

Custom projects presents the saved native Custom Projects that actually exist under the verified
runtime (`GET /api/sqx-projects`). Each workflow shows its native task pipeline, engine, symbol,
timeframe, dates, money-management, and CrossChecks `use` flags from the saved XML. Custom
projects opens Progress, Full settings, and Results for the selected saved project. Full settings
panes bind documented SQX groups (What to build, Data, Trading options, Building blocks, ATM,
Money management, Ranking, Cross checks, Genetic options, Parts to improve) to existing task
XML paths; Genetic options is its own tab when BuildMode is genetic, and Parts to improve when
What to build is improve or the observed improve-existing alias. What to build StrategyType
choices are the official SQX set (`simple`, `multi-tf`, `template`, `improve`) plus the current
saved value if it is already on the node. Template Browse/Reload and Ranking fitness `@type`
come from installed SQX `buildType/listFiles`, `buildType/getTemplateConfig`, and
`fitnessMethodStrategyResult/list`. Documented enumerated attributes (engine, timeframe,
generationType family, StrategyType, ranking comparators) render as native choice controls
instead of free-typed fields. Money-management Method siblings that already have `use` flags
are one exclusive radio group. Unknown native values stay text inputs. Extra Settings children
such as Databanks, Resources, and Notes still appear if present. Nested Ranking conditions and
Cross-check Settings/Filtering stay in that tree. Writes update only existing native attributes
or existing text via `POST /api/sqx-project-settings`. Calibrate now posts
`POST /api/sqx-calibrate` to the running SQX `indyTester/calibrate` servlet and applies
returned min/max/step onto existing blocks; it fails closed when SQX local web is down.
Start/Stop (`run_project` / `stop_project`) call official `project/start` (POST) and
`project/stop` when the running SQX web is open — the same servlets as the Electron
control panel. If that web is down they fall back to `sqcli -project action=start|stop`
and the start process registers with the desktop worker supervisor. Progress streams
producer log files and databank counts; generated/rejected/rate/percent bind from the
SQX engine WebSocket when published. Pause/Resume call `project/pause` and `project/resume`.
The path fails closed without a verified runtime, matching launcher digest, saved project, or
supervisor registration. Native settings are adjustable in this desktop; they
are not a second SQX window and are not “go adjust it in StrategyQuant X.”

Read-only topology custody may expose task order, native task kind, selected fields, databank references, and exact project archive identity. Unknown native task semantics should be resolved first from the running producer when observable; only genuinely non-observable details remain opaque pending source-level inspection.

The platform must not create a replacement task-loop engine.

Native databanks under `user/projects/<name>/databanks/` are listed on that module's Progress
and Results as producer archives. Selecting an inspectable `.sqx` on the module
Results shows List of trades and equity from `orders.bin` (`GET /api/sqx-project-strategy`);
strategy config compares archive `settings.xml` with the current task; trades on chart stay
unavailable unless that archive stored chart data. Those files are not Historical Results until
custody bind. TradingView and MetaTrader MCP do not belong on this surface.

## 10. Capability/add-on model

One backend capability authority supplies typed descriptors used by UI and language/tool surfaces
(`GET /api/capabilities`). Packaged native StrategyQuant X plugins (SQX Lab, Custom Block,
RunCompare, LucidFlex Prop Evaluator, Edge Decay Analyzer, 2-Step Challenge Analyzer, Source
Code Translator) are the default catalog. Empty operator add-on storage is still ready; it does
not hide those packaged plugins. `POST /api/capabilities` `{action:"stage",id}` copies a known
Results plugin into the verified SQX runtime from loopback. Plugin settings stay in StrategyQuant X
Results. The desktop does not inject plugin HTML/JS or invent PASS/FAIL.

Add-ons may contribute only through registered typed extension slots. They may not:

- inject arbitrary frontend JavaScript/HTML;
- maintain a competing capability catalog;
- rewrite top-level product navigation;
- rewrite Research core stages;
- claim producer truth they do not own.

Unknown descriptor versions fail closed.

## 11. Repository boundary

Production code must not import recovered/source/reference/Futures repositories as runtime dependencies.

Forbidden production architecture includes:

- copied Futures quantitative architecture;
- Phase01 intake architecture;
- a persistent "Apollo product spine" as a second product/result/state authority (the bounded Apollo *assistant* surface in section 3 is a distinct, allowed UI/LLM surface and is not this);
- platform-owned replacements for native Builder/GA/backtest/robustness/optimizer/Custom Project execution (the platform-owned Machine Learning / Models modality in section 3 is a distinct, allowed capability and is not this);
- importing the Quant-Guild-Library (or any reference/source repository) as a runtime code dependency (it is reference/knowledge data only);
- copied personal/customer credentials or machine-specific state.

`tools/check_production_boundary.py` enforces the major prohibited path/import/marker rules and complements manual review.

## 12. Delivery model

Every user-facing feature is delivered through the same development desktop.

Required development path:

`current main -> one bounded implementation branch -> inspect/run actual native producer when relevant -> exact-head acceptance/review -> merge -> delete branch -> feature visible/inspectable in desktop`

Do not split “implementation” and “real installed-producer evidence” into separate completion tracks when the authorized runtime is available. The runtime exercise is part of implementing and accepting the feature.

Implementation order and current status live only in `LIVING_IMPLEMENTATION_PLAN.md`.
