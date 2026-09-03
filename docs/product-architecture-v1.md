# Product Architecture v1

This document is the stable architecture authority for the platform.

## 1. Product identity

TraderCockpit is one desktop trading platform with these top-level surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

The platform owns its product identity and user experience.

The accepted visual/product authority is the five-screen neon TraderCockpit prototype pinned in
`references/ui-authority/` (`screenshots/*.png` + `manifest.json`). It supersedes the earlier
dark-blue `Chart / Backtest / Proof` shell and the earlier "ESQ" mockups. The pictures are the
definitive structure of the one `web/` tree: UI-impacting work must match their layout and tab
rows, must not condense tabs, and must not invent a new visual direction without an explicit
product-authority change. The frontend is vanilla ES modules with no framework or build step.

The 2026-09-03 owner-intent revision is an explicit product-authority change for *behavior*,
not chrome: actual OHLC bars, indicator/strategy/model maintenance, paper/URL ingest,
clarifying questions, Apollo product-control tools, and voice. Tab rows and Home zones stay
exactly as pinned.

Every surface shares the prototype chrome: left rail (brand, six-surface navigation, workspace /
research-progress / account cards), top bar (workspace chip, `Data Feeds | Broker | Compute |
Automation` readiness chips from `/api/status` and `/api/market/quotes`, search, notifications),
market ticker (one cell per watchlist symbol plus a market-state cell, from `/api/market/quotes`
and the market read model), and the bottom status bar (`Live Runs | Positions | Daily P&L | Buying
Power | Drawdown | Last Run`). Cells whose producer does not exist show `—` with an explicit
"not connected" reason; the last-run cell reads Research custody.

StrategyQuant X / SQX 144.2953 is a native historical-research backend producer where currently proven. It is not the platform name and not a user-facing workspace label.

## 2. Home and Research are separate domains

### Home

Home is the live/current Cockpit Home. Its zones are:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

plus the persistent Apollo assistant. Each zone reads from the producer that actually owns the
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
- `validate` — **Test & Validate** (`Overview | Initial Test | Trades | Robustness | Configuration | Evidence`): KPI strip, the seven-stage funnel `Initial Test | Fast Validation | Golden Validation | Scenario Tests | Stress Tests | Out-of-Sample | Evidence` (every stage carries the cockpit verdict per completed native result from the `cockpit_verdict` read model — native acceptance conditions for stages 1–2, cockpit policy over the native trade records for stages 3–6, Proof custody for stage 7 — with the native `CrossChecks` enable flags shown for context), Performance Overview (equity from the native trade records of the latest judged result), Return Distribution across judged results, seven stage cards with per-check dots, Run & Evidence table with SQX-formula statistics and the verdict chip, Validation Conclusions (`Robust & Deployable | Rejected | Verdict incomplete | Validation in progress`), next actions; the tool tabs host the native Retester, native trade rows, producer-backed robustness, the executed configuration chain, and Proof.
- `catalog` — **Indicators & Models** (`All Components | Indicators | Models | Strategies | Utilities | My Components`): every native building block from the exact Builder task with category/enabled/weight/parameter attributes, native templates, imported native strategies and Ideas; Models is the platform-owned ML modality (fit allowlisted sklearn classifiers on native trades, then bind the catalog digest onto an existing Candidate); Utilities hosts native project topology and preset verification.

The custody chain `Idea → Specification → Build → Candidates → Backtest → Robustness → Proof →
Delivery / Simulation` is folded into those workspaces rather than condensed; pre-prototype
`stage`/`tab` links canonicalise to the workspace routes so bookmarks and custody selections
(`configuration`, `proofEntity`, `validationRef`) survive. In-Research workspace and tab hops
copy those same non-structural identities; Home Quick Actions start without leftover IDs.
Delivery / Simulation lives in Operate after Proof.

Construct modalities stay distinct and feed the same downstream custody: Random Discovery and
Genetic / Evolutionary search (native SQX) and Machine Learning / Models (platform-owned, see 3).

Research objects the owner builds and maintains are **indicator**, **strategy**, and **model**.
Each has immutable revisions. Text, a URL, or a paper does not create a Candidate. A Custom
Project is the native plug-and-play runner for an approved backtest/robustness task sequence;
results still render on Test & Validate. The product must lead the user through the custody
chain (current stage + one legal next action) and must not present StrategyQuant X Builder
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

The Assistant card ("Your trading copilot", Apollo identity) appears on Home and in the Research
workspaces as the prototype shows; it is a functional, bounded LLM surface under the consumer
account/model boundary (section 5). The backend transport (`product/tradercockpit/assistant.py`,
`/api/assistant` GET status / POST message, loopback only) calls OpenRouter's OpenAI-compatible
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

Apollo never owns producer truth, never becomes a result/quantitative authority, and never
mutates native state *directly*. Product-control tools are application mechanics that call
the same custody APIs a human click would. This bounded assistant is explicitly distinct
from the forbidden legacy "persistent Apollo product spine" (section 11).

Primary literature is a second citation catalog (White 2000; Hansen 2005; Bailey et al.
2014; Bailey & López de Prado 2014; Harvey, Liu, Zhu 2016; López de Prado 2018; Pardo;
Tharp R-expectancy; Sharpe 1966/1994; Wilder 1978) stored as platform-authored notes with
bibliographic pointers — the same no-transcript, no-formula-invention rule as Quant-Guild.

### Cockpit validation verdict (platform-owned)

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
  funnel stages; only Higher Precision is launchable from the desktop.
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
2. retained native MCP for its published inspection/control tools only;
3. optional `sqx-lab` custom native-artifact tooling only when explicitly needed;
4. platform orchestration/custody/presentation around those producer capabilities.

The retained MCP tool set in 144.2953 is limited to:

`list_projects | list_databanks | list_strategies | get_strategy_stats | run_project | stop_project`

Do not invent additional MCP authoring methods.

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

## 9. Automation

Automation may inspect/configure/control/read registered native workflows. Native Custom Project
task execution remains native. The owner-facing job is one confirmed “run this approved project”
action (plug-and-play backtest/robustness sequence); results render on Test & Validate. The
platform must not clone the StrategyQuant X Custom Projects window or invent a task-loop engine.

Read-only topology custody may expose task order, native task kind, selected fields, databank references, and exact project archive identity. Unknown native task semantics should be resolved first from the running producer when observable; only genuinely non-observable details remain opaque pending source-level inspection.

The platform must not create a replacement task-loop engine.

## 10. Capability/add-on model

One backend capability authority supplies typed descriptors used by UI and language/tool surfaces.

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
