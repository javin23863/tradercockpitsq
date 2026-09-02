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
rows, must not condense tabs, and must not invent a new direction without an explicit
product-authority change. The frontend is vanilla ES modules with no framework or build step.

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

Home is the Cockpit Home board of the `cockpit-home` screen: a hero ("Turn Research into Decisions
that Compound." · `Research → Build → Validate → Simulate → Deploy` · New Research / Build
Strategy), a Recent Activity rail, and exactly eight numbered cards:

`Research | Build & Backtest | Prop Firm Simulation | Proof & Evidence | Active Builds | Candidate Review | System Health | Assistant`

Cards 1, 2, 4, 5 and 6 read the canonical Research custody catalogs (ideas, historical results,
proofs, native jobs and lifecycle counts, candidates with promotion/export/deployment kept
distinct); card 7 reads `/api/status`; card 3 and the live/account values in the chrome remain
explicit "not connected" states until their producers exist; card 8 is the bounded Assistant.
Metrics the producer has not exposed (net profit, Sharpe, scores, grades) render as `—` with the
reason, never as invented numbers. Historical research is summarised only with explicit scope and
never becomes live prices, signals, account risk, execution state, or current performance.

### Research

Research is the historical strategy-research surface, composed of four workspaces — one per
prototype screen — selected by `/research?workspace=<id>&tab=<id>`:

- `signals` — **Signals & Models** (`Overview | Signals & Models | Order Flow | Footprint | Volume Profile | Liquidity Map | Replays | Alerts | Reports`). Overview holds Idea/source custody; Signals & Models shows the chart frame, the exact native Builder specification (strategy shape, market identity, data setup, blocks, rankings, cross-checks, money management, native search mode) and the Strategy Panel of enabled native signal blocks; the analytics tabs carry their full frames until a market-data provider exists; Reports lists immutable Proofs.
- `evolution` — **Evolutionary Search**: the native `BuildMode` GA parameters (population, generations, islands/migration, crossover/mutation, fresh blood, restart), native `Rankings` fitness/acceptance conditions/stop condition, exact configuration compile → review → approve → launch custody, native job state, and Top Candidates (native Results import). Random Discovery vs Genetic Evolution is shown from the exact native selector.
- `validate` — **Test & Validate** (`Overview | Initial Test | Trades | Robustness | Configuration | Evidence`): KPI strip, the seven-stage funnel `Initial Test | Fast Validation | Golden Validation | Scenario Tests | Stress Tests | Out-of-Sample | Evidence` (every stage carries the cockpit verdict per completed native result from the `cockpit_verdict` read model — native acceptance conditions for stages 1–2, cockpit policy over the native trade records for stages 3–6, Proof custody for stage 7 — with the native `CrossChecks` enable flags shown for context), Performance Overview (equity from the native trade records of the latest judged result), Return Distribution across judged results, seven stage cards with per-check dots, Run & Evidence table with SQX-formula statistics and the verdict chip, Validation Conclusions (`Robust & Deployable | Rejected | Verdict incomplete | Validation in progress`), next actions; the tool tabs host the native Retester, native trade rows, producer-backed robustness, the executed configuration chain, and Proof.
- `catalog` — **Indicators & Models** (`All Components | Indicators | Models | Strategies | Utilities | My Components`): every native building block from the exact Builder task with category/enabled/weight/parameter attributes, native templates, imported native strategies and Ideas; Models is the platform-owned ML modality (not connected); Utilities hosts native project topology and preset verification.

The custody chain `Idea → Specification → Build → Candidates → Backtest → Robustness → Proof →
Delivery / Simulation` is folded into those workspaces rather than condensed; pre-prototype
`stage`/`tab` links canonicalise to the workspace routes so bookmarks and custody selections
(`configuration`, `proofEntity`, `validationRef`) survive. Delivery / Simulation lives in
Operate after Proof.

Construct modalities stay distinct and feed the same downstream custody: Random Discovery and
Genetic / Evolutionary search (native SQX) and Machine Learning / Models (platform-owned, see 3).

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
- Home/live-current presentation from correct producers, including the live-market provider seam (`tradercockpit.market_data.MarketDataProvider` → `/api/market/quotes`) with Schwab (operator) preferred over Finnhub, an operator watchlist, truthful `provider_not_configured`, FRED as a separate macro series producer, and historical FX/indices remaining in native SQX Dukascopy — never fabricated symbols, prices, or timestamps, and never a second Dukascopy pipeline;
- consumer identity/account state;
- bounded external model access and policy;
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
knowledge library rather than invented, and the modality exposes truthful unavailable state
until its backend is connected.

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
unconfigured provider answers `provider_not_configured` in the thread. It is grounded against the
curated Quant-Guild knowledge library
(`https://github.com/romanmichaelpaolucci/Quant-Guild-Library`) for anti-hallucination; the
knowledge library is reference data (ingested/retrieved), never a runtime code import
(section 11). Apollo assists with intent, explanation, summaries, and approved tools; it never
owns producer truth, never becomes a result/quantitative authority, and never mutates native
state directly. This bounded assistant is explicitly distinct from the forbidden legacy
"persistent Apollo product spine" (section 11).

### Cockpit validation verdict (platform-owned)

StrategyQuant X produces the backtest and its exact native trade records; the cockpit owns the
verdict. `product/tradercockpit/research_verdicts.py` attaches `cockpit_verdict`
(`tc.research-cockpit-verdict.v1`) to the Historical Result detail read model:

- **Statistics** — the SQX databank columns used by native acceptance conditions (`NetProfit`,
  `GrossProfit`, `GrossLoss`, `NumberOfTrades`, `WinningPct`, `ProfitFactor`, `Drawdown`,
  `DrawdownPct`, `ReturnDDRatio`, `AvgTradesPerMonth`, `Expectancy`, `MaxConsecLosses`, …) are
  recomputed from the native trade rows with the published SQX column formulas (initial capital
  from the native `MoneyManagement`), per native sample type (in-sample 10–19, out-of-sample
  20–30, full 127).
- **Stages 1–2 (native conditions)** — the exact Rankings conditions and
  `RetestWithHigherPrecision` acceptance conditions of the approved Builder task (read through
  custody: result → candidate → configuration → executable task XML) are evaluated over the
  Retester result and the bound Higher Precision result respectively. Columns the cockpit cannot
  recompute (walk-forward, confidence-level Monte Carlo, parameter stability) remain
  `unevaluated` and make the stage `incomplete`.
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

Billing model (single subscription funds the LLM allowance):

- there is exactly one consumer charge — the membership subscription;
- the LLM/OpenRouter usage allowance is funded from that single membership; it is not a
  second charge. The provider-enforced OpenRouter key limit is an internal allocation
  carved from the membership and remains the hard spend ceiling;
- the subscription price is presented only on the hosted checkout page. No price is
  rendered in Settings or any other product surface;
- Settings shows membership status and LLM usage as a non-price percentage (usage
  tracking), never a dollar figure.

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

The desktop private server is loopback-only, validates its exact Host, and rejects cross-origin browser mutations. Browser code never invokes native processes directly.

### Deployment modes and scaling seam

The same one application/runtime family runs in two explicit deployment modes
(`TRADERCOCKPIT_DEPLOYMENT_MODE`, default `personal`), reported on `/api/status`:

- `personal`: one owner, one local data root, operator-held provider credentials,
  loopback desktop. Durable single-node state uses `atomic_io` (temp file + fsync +
  `os.replace`, per-path in-process lock). This is the shipping behavior and is `ready`.
- `commercial`: many isolated consumers scaling to thousands. It additionally requires
  hosted consumer authentication, provider-enforced per-consumer spend, membership
  billing, and — the architectural blocker — per-tenant data isolation. A single local
  data root is not a tenant boundary, so commercial mode fails closed (`commercial_not_ready`)
  with the exact unmet prerequisites rather than pretending a desktop is a multi-tenant
  server.

The scaling seam is a per-tenant isolated store behind the same read-model/custody
contracts: personal binds them to the local data root; commercial binds them to a
per-tenant database/object-store namespace with the same atomic/idempotent semantics.
The commercial store, hosted auth, and per-tenant provider provisioning are the named
work required before commercial mode reports `ready`; the personal product never depends
on them.

### Security posture (LLM/vibe-coding)

- Secrets never enter a read model; provider keys/tokens stay server-side and are used only
  in outbound `Authorization` headers.
- Outbound calls target hardcoded provider hosts with URL-encoded parameters (no
  user-controlled host); POST bodies are size-capped; LLM output is HTML-escaped in the UI.
- The bounded assistant advertises only `retrieve_quant_guild`, rejects unknown tools and
  extra argument keys, caps tool rounds, and treats Quant-Guild excerpts and cockpit context
  as untrusted data (indirect-prompt-injection defense) — it cannot launch native processes
  or mutate custody.
- `tools/check_production_boundary.py` rejects dangerous constructs in production code
  (insecure deserialization via pickle/marshal/joblib/`yaml.load`, `eval`/`exec`,
  `os.system`/`os.popen`, `subprocess(..., shell=True)`); native launch uses argv lists
  through the trusted gateway.

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
  Promotion is operator Delivery custody after an immutable Research Proof (`/api/operate/promotions`); it does not create live runs, positions, or P&L.
- Proof binds idea/source, approved configuration, producer/runtime/job, data/settings, native artifact, result/trades, validation outcomes, and current product status.

## 9. Automation

Automation may inspect/configure/control/read registered native workflows. Native Custom Project task execution remains native.

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
