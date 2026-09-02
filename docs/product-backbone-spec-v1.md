# Product Backbone Specification v1

This document is the detailed implementation contract beneath `docs/product-architecture-v1.md`.

Current sequencing/status lives only in `LIVING_IMPLEMENTATION_PLAN.md`.

## 1. Global desktop frame

Top-level navigation is exactly:

`Home | Research | Explore | Automation | Operate | Settings`

The frame is the prototype chrome (`references/ui-authority`):

- left rail: brand, the six surfaces, a workspace card (`/api/status` application), a research-progress card (custody stages with at least one record), an account card (`/api/status` account), and a version line;
- top bar: workspace chip, `Data Feeds | Broker | Compute | Automation` chips reading `/api/market/quotes` and `/api/status` (`market_data`/quotes, `account`, `research_backend`, `extensions`), a search field that is disabled until a search producer exists, and a notifications bell whose count is the number of status components not ready;
- market ticker: one cell per operator-configured watchlist symbol (`TRADERCOCKPIT_WATCHLIST`) with `last`/`change` only from a `current` Finnhub record when `TRADERCOCKPIT_MARKET_API_KEY` is set (otherwise `—`), a structural sparkline slot, and a market-state cell bound to the market read model;
- bottom status bar: `Live Runs | Positions | Daily P&L | Buying Power | Drawdown` (each `—` with a "requires live execution/account producer" reason until Operate exists) and `Last Run` from Research custody (latest native Retester result or Builder job; never a verdict).

Rules:

- `/home` is the first-launch route; later desktop launches restore the last registered
  session path (`/api/desktop/session`) including Research custody query keys;
- `/research` is the canonical historical-research route; `/research?workspace=<id>&tab=<id>` selects one of the four registered workspaces and its tabs; pre-prototype `stage`/`tab` links canonicalise to those routes while preserving custody selection parameters;
- the Assistant is a bounded, functional card (Apollo identity) on Home and in Research backed by `/api/assistant` (OpenRouter, operator credential, backend model policy); `/api/assistant` retrieves ingested Quant-Guild lecture excerpts as reference data (never a runtime import of that repository) and may call backend-only `retrieve_quant_guild` mid-turn; it is never disabled, reports provider and knowledge-library readiness truthfully, and is not a product/result authority and never mutates native state directly;
- no frontend-owned master list of providers/models/native capabilities;
- no fabricated runtime, market, account, candidate, result, or deployment identity in global chrome;
- one `web/` tree of vanilla ES modules; no framework or build system.

## 2. Home contract

Home answers: **what matters now and where should the user go next?**

Home is the Cockpit Home board of the `cockpit-home` prototype screen:

1. hero — "Turn Research into Decisions that Compound.", the workflow `Research → Build → Validate → Simulate → Deploy`, and the `New Research` (Signals & Models / Overview) and `Build Strategy` (Evolutionary Search) actions;
2. Recent Activity — the latest record of each custody kind (proof, historical result, candidate, native job, configuration, idea) labelled by kind; no timestamps are invented;
3. eight numbered cards, in this order, each bound as follows.

| # | Card | Read model | Truthful state |
| --- | --- | --- | --- |
| 1 | Research | `/api/research/ideas` | latest Ideas; score box `—` (no scoring producer) |
| 2 | Build & Backtest | `/api/research/historical-results` | latest native Retester result and state; net profit / Sharpe / win rate / max DD `—` (not read from the native archive); equity frame `no data` |
| 3 | Prop Firm Simulation | none yet | "No simulation account"; balance / P&L `—`; challenge progress empty |
| 4 | Proof & Evidence | `/api/research/proofs` | proof count and unread producer outcome; grades `Not graded`; radar frame empty |
| 5 | Active Builds | `/api/research/native-jobs` + lifecycle catalogs | native Builder jobs (state, next step) and the pipeline lifecycle counts binder |
| 6 | Candidate Review | `/api/research/candidates` | imported Candidates with score `—`; the Alpha Stack binder keeps candidate / promoted / exported / deployed distinct |
| 7 | System Health | `/api/status` | application, research backend, custody, native execution, live market data, provider, account, model, extensions — each with its own readiness |
| 8 | Assistant | `/api/status` assistant/model/provider + `/api/assistant` | functional bounded Apollo assistant thread; never disabled — an unconfigured provider answers with its exact `provider_not_configured` state |

Each card reads only the producer that owns its state. Historical research values never
masquerade as live/current truth; live/account values in the chrome stay `—` until their producers
exist. Home state vocabulary: `current`, `stale`, `pending`, `unavailable`, `error`.

## 3. Research contract

Research answers: **what historical strategy is being constructed/tested, what did the native producer execute, and what evidence supports the result?**

Research is four workspaces, one per prototype screen, with the exact tab rows of the pictures.
The custody chain `Idea → Specification → Build → Candidates → Backtest → Robustness → Proof →
Delivery / Simulation` is folded into them, never condensed away.

### Workspace `signals` — Signals & Models

Tabs: `Overview | Signals & Models | Order Flow | Footprint | Volume Profile | Liquidity Map | Replays | Alerts | Reports`.

- `Overview` — Idea/source custody (saved Ideas, immutable revisions, editor) plus a workflow rail.
- `Signals & Models` — chart card (toolbar, tools, price/volume/CVD frames; `no data` until a market-data provider exists), the **Native Strategy Specification** (the exact native Builder task: strategy shape, market identity, historical data setup, trading assumptions, building blocks, money management, search/build mode with distinct Random Discovery and Genetic Evolution lanes, ranking & filters, validation profile, source provenance; the native `Blocks`/`Rankings`/`CrossChecks`/`MoneyManagement` subtrees as collapsible read-only inspectors; capability coverage), Strategy Panel (enabled native signal blocks), Signal Pulse and Active Models (no live producer / ML modality not connected), and the bottom row Confluence · Market State · Session Context · Risk Overlay · Assistant.
- `Order Flow | Footprint | Volume Profile | Liquidity Map | Replays` — full chart frames with explicit provider requirements.
- `Alerts` — alert table (no alert producer yet). `Reports` — immutable Research Proofs.

### Workspace `evolution` — Evolutionary Search

Strip: `State` (latest native job state) · `Objective Set` (native `FitnessCriteria` ranking + acceptance condition count) · `Optimization` · `Search Mode` (exact native `BuildMode@generationType` as Genetic Evolution / Random Discovery / other) · `Deterministic Seed` (not exposed) · `Budget` (native `MaxStrategies` + stop condition) · `Time Elapsed` (not exposed) · `Pause`/`Stop` (disabled; no native control seam).

Cards: Search Configuration, Population (islands), Generations, Pareto Frontier, Variation Operators, Fitness Evolution, Islands Overview, Archive & Objectives (native acceptance conditions), Top Candidates (Candidate import bound to exact native Results archives), Deterministic Seed/Budget, and the exact configuration custody workspace (compile → review → approve → launch through the trusted gateway). Every value is the native tag's exact text with the tag name shown; live GA telemetry frames stay `no data` because the native Builder does not stream it.

### Workspace `validate` — Test & Validate

Tabs: `Overview | Initial Test | Trades | Robustness | Configuration | Evidence`.

- `Overview` — KPI strip (`Total Runs` from custody; `Pass Rate`, `Avg. Ret/DD`, `Out-of-Sample PF`, `Max Drawdown`, `Expectancy`, `Profit Factor` from the cockpit verdicts of completed native results, `—` with a truthful note until one exists), Validation Funnel with the seven stages `Initial Test | Fast Validation | Golden Validation | Scenario Tests | Stress Tests | Out-of-Sample | Evidence` (per stage: results passing / results judged, `pass | fail | incomplete | not_run` tallies, native `CrossChecks` enable flags for context), Performance Overview (equity curve from the native trade records of the latest judged result), Return Distribution across judged results, seven stage cards (latest stage verdict chip, pass rate, checks passed, stage metric, per-check dots), Run & Evidence Table (native runs, robustness runs, failed attempts, proofs with SQX-formula net profit / Ret/DD / drawdown / profit factor and the verdict chip), Validation Conclusions (overall verdict label plus Statistical Robustness / Risk Controls / Regime Resilience / Overfitting Risk derived from the stress, scenario and out-of-sample stages), Next Actions (disabled until Operate producers exist). Every verdict value comes from the backend `cockpit_verdict` read model (`tc.research-cockpit-verdict.v1`).
- `Initial Test` — native Retester execution/readback. `Trades` — exact native trade rows. `Robustness` — producer-backed methods (Higher Precision). `Configuration` — the executed chain. `Evidence` — Research Proof.

### Workspace `catalog` — Indicators & Models

Pills: `All Components | Indicators | Models | Strategies | Utilities | My Components`. Components are the exact native building blocks (`signals`, `indicators`, `stopLimitBlocks`, `orderTypes`, `exitTypes` with category/enabled/weight/parameter attributes), native templates, imported native strategies and Ideas. Search and category filtering run over the loaded set; market fit, timeframe fit, rating, dependencies and performance render `—` because no producer exposes them. `Models` carries the platform-owned ML modality in its not-connected state; `Utilities` hosts native project topology and preset verification.

Route/query state may select only registered workspaces/tabs. Arbitrary query text never creates new product states or durable identities.

### Stage contracts

The custody stage contracts below are unchanged; each is hosted in the workspace/tab named here:
Idea → `signals` / Overview; Specification → `signals` / Signals & Models; Build → `evolution`;
Candidates → `evolution` (Top Candidates); Backtest Overview → `validate` / Initial Test;
Trades → `validate` / Trades; Robustness → `validate` / Robustness; Configuration → `validate` /
Configuration; Proof → `validate` / Evidence.

### Construct / Idea

- capture idea/source/provenance;
- open existing native strategy/template when applicable;
- preserve revision identity;
- identify unresolved native requirements;
- allow bounded language assistance without inventing trading meaning.

Text entry alone does not create candidate or run identity.

### Construct / Specification

Resolve the smallest complete set of native requirements for one exact executable plan.

Native requirement families may include strategy shape, parts to improve, conditions/periods, exits/stops/targets, historical data/symbol/timeframe/date/IS-OOS/precision, trading/session/cost assumptions, building blocks/parameter ranges, ATM, sizing/money management, search/build mode, genetic options where selected, ranking/basic filters, cross-checks/filters, and notes/provenance.

Each field/group has explicit state such as proven default, user selected, unresolved, unsupported, or not applicable. Missing required native meaning locks Build.

### Construct / Build

Compile/review/approve one exact native configuration.

Custody must include:

1. source native template/project/task identity;
2. verified native build/runtime identity;
3. source bytes/hash;
4. typed approved changes;
5. exact executable bytes/hash;
6. human-readable diff;
7. approval bound to that exact revision;
8. native job/control identity after launch.

Untouched native fields remain untouched. Native Builder owns generation/search/GA behavior/initial testing/ranking/filtering/databank output.

Build refuses when runtime identity is unverified, source/path/hash is invalid, required meaning is unresolved, or approval does not match the exact executable revision.

### Construct / Candidates

Candidate Lab consumes real native survivors only.

Custody chain:

`Idea/source -> approved configuration -> native job -> exact native artifact -> product candidate identity`

Requirements:

- immutable archive/content identity;
- idempotent import;
- no candidate fabricated from UI text;
- no cross-job/config substitution;
- producer-backed data plus product provenance only.

### Backtest / Overview

Historical producer-backed summary and validation lifecycle for the selected candidate/run.

### Backtest / Trades

Actual native historical trade/order records and chart context. No synthetic trades to fill the UI.

### Backtest / Robustness

Selected native cross-check/retest/optimization methods rendered dynamically from an exact producer-backed plan. Methods are capabilities, not permanent navigation tabs.

### Backtest / Configuration

Shows the immutable configuration that actually executed, including source identity, executed bytes/hash, approval/diff, producer build/job identity, and data/trading settings.

### Proof

Durable chain connecting idea/source, approved configuration, runtime/launcher/job, historical data/settings, native strategy artifact, native result/trades, validation method/outcomes, and current product status.

## 4. Native runtime and control contract

Before any native compute:

- verify expected build/runtime markers;
- verify trusted launcher identity when the executable is part of the trust boundary;
- verify other pinned engine artifacts separately where required;
- resolve project/configuration paths physically;
- reject symlink/junction/path escape outside authorized runtime;
- verify expected preset/config/artifact hashes when part of the contract;
- never accept browser-selected executable/runtime filesystem paths;
- expose structured refusal/error state.

The native path keeps three decisions independent:

1. Runtime trust verifies the authorized SQX build and configured launcher boundary.
2. Artifact custody captures exact project/configuration/engine/result identities and preserves their ancestry.
3. Producer validity requires the native archive/task structure and the authorized producer's own load, execution, or output acceptance.

Archived Git blob identity is not a runtime validity predicate for mutable native projects. An exact engine/library hash is execution provenance unless a separately documented and configured security policy explicitly makes it a trust anchor.

Browser mutations pass through the canonical backend API. Browser code never starts native processes directly.

## 5. Custom Project topology contract

Read-only native topology custody may expose:

- project identity;
- exact `project.cfx` archive hash;
- internal archive entries;
- numbered native task identity/order;
- task kind;
- only explicitly proven typed fields such as selected databank names or GoToTask target label.

Unknown canonical task kinds remain opaque. Read-only topology does not imply execution support.

The selected project must be one exact direct project child inside the verified runtime after physical path resolution. Symlink/junction escape is refused.

## 6. Canonical application and desktop

There is one Python application server authority, one API family, one state/custody family, one `web/` UI, and one desktop host.

Desktop requirements:

- starts the canonical server;
- binds private desktop HTTP to literal loopback;
- requires exact loopback Host to prevent rebinding;
- rejects cross-origin browser mutations;
- opens the same canonical `web/` UI;
- shuts the local server down when the window exits;
- contains no account/native/quantitative product logic of its own.

## 7. Core backend/read-model families

### Application/runtime

- application/system status;
- native runtime descriptor/readiness, including well-known-home discovery and a candidate_id bind that persists `native-runtime.json` (process env/CLI pins stay read-only; reopen the desktop after bind);
- provider/data/model/extension readiness.

### Home/live

- market overview;
- Alpha Stack;
- pipeline/attention;
- signals;
- risk;
- scoped performance.

These remain unavailable until the actual producers exist.

### Research

- native preset/configuration discovery;
- platform-owned Models catalog (`/api/research/models`) that fits allowlisted sklearn classifiers on native Historical Result trades;
- exact configuration/approval custody;
- native job control/readback;
- native output discovery/import;
- candidates;
- exact historical run/result reads;
- native validation/retest/optimization plan/results;
- proof/evidence;
- native project topology/control/readback.

### Account/model

- active account;
- allowance/usage;
- model policy;
- authenticated session;
- provider spend-authority metadata without management secrets.

The browser cannot choose arbitrary account subjects, provider management credentials, runtime roots, executable paths, or unrestricted model routes.

## 8. Storage and identity

Use immutable/content-addressed evidence and atomic current pointers where appropriate.

Rules:

- immutable evidence is never rewritten as current state;
- mutable current pointers are explicit and atomic;
- exact native bytes/hashes remain part of custody;
- account and research identities use separate unambiguous namespaces;
- model-policy changes do not rewrite account identity/history;
- live read models carry producer/time/scope;
- monetary/entitlement state must be correct across multiple writer processes where concurrency is possible.

## 9. Consumer account/model contract

Required path:

`Google identity -> stable account -> configured entitlement -> provider-bounded spend authority -> backend model policy -> usage/readback`

Required invariants:

- normalized trusted Google issuer/subject binding;
- duplicate starter grants prevented under concurrent writers;
- explicit durable grant-policy identity;
- provider management credential never reaches client/consumer;
- provider hard limit cannot exceed product authorization;
- reset/expiry/revocation state is explicit;
- exhausted/revoked/lapsed state refuses before spend;
- account state and model policy are separate;
- current default `z-ai/glm-5.3-flash` is backend-configurable.

## 10. Capability/add-on descriptors

One backend registry is authoritative.

A descriptor includes stable capability identity/version, owning producer, availability, supported product placement, configuration/read/action schema versions, and optional typed presentation descriptors.

Rules:

- no arbitrary script/HTML injection;
- no competing frontend capability catalog;
- no add-on-created top-level navigation without architecture change;
- no replacement for Research core stages;
- unknown descriptor versions fail closed.

## 11. UI/security truthfulness

- Escape untrusted text before HTML composition.
- Routes/queries select only registered states.
- Browser-provided route values do not create durable identity.
- Browser never receives provider management secrets.
- Browser never receives arbitrary native filesystem/executable control.
- API mutation errors are structured and fail closed.
- Historical, live, simulated, and unavailable scopes remain visible and distinct.

## 12. Repository/product acceptance

For any implementation slice:

- production-boundary checks pass;
- focused tests pass;
- full applicable Product Runtime Acceptance passes on the exact head;
- browser acceptance passes when UI/routing/read models change;
- desktop acceptance passes when desktop/runtime behavior changes;
- substantive exact-head review findings are resolved;
- the real behavior is visible or inspectable in the one development desktop.

No isolated unit suite or backend-only fragment is sufficient evidence of product completion.
