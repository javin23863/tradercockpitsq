# TraderCockpit Product Backbone Specification v1

## Status and authority

This document is the **binding detailed implementation specification** beneath:

- `docs/product-architecture-v1.md` — product architecture and producer ownership;
- `AGENTS.md` — implementation and anti-drift policy;
- `IMPLEMENTATION_CHECKLIST.md` — execution order and acceptance gates.

It does not create a competing roadmap. It defines how the architecture is represented in the application, how each user surface maps to backend contracts, how the native StrategyQuant X producer is controlled, and how future add-ons extend the product without changing the backbone.

No implementation may contradict this document merely because an older branch, PR body, route name, prototype fixture, or isolated test uses a different structure.

## Evidence vocabulary

Every implementation decision in this specification belongs to one of three classes.

### `PROVEN`

Directly supported by retained SQX screenshots, saved `.cfx`/task configuration, native runtime/output evidence, accepted TraderCockpit UI authority, or currently verified product code.

### `SPECIFIED`

A TraderCockpit product/interaction/application decision selected to expose proven backend behavior with lower friction. It is binding product design, but it is not a claim about hidden SQX internals.

### `OPEN EVIDENCE`

A native mapping, result meaning, progress field, command seam, or capability detail that still requires executable/native evidence before implementation may claim it works. An `OPEN EVIDENCE` item must remain unavailable or constrained; it must not be replaced with a speculative TraderCockpit producer.

## 1. Product backbone in one sentence

A user brings an idea or source into TraderCockpit, resolves only the information needed to form an exact executable native SQX configuration, lets the real SQX Builder produce and initially evaluate strategies, inspects the surviving native candidates, runs the desired native validation/retest/optimization funnel, and receives durable Backtest and Proof views tied to the exact configuration, producer build, strategy archive, results, trades, and validation outcomes.

## 2. Producer ownership

### `PROVEN` — StrategyQuant X owns research computation

SQX 144.2953 owns:

- Builder strategy generation;
- genetic/evolutionary mechanics;
- native strategy/block semantics;
- the backtesting engine;
- native Builder fitness/ranking/filtering;
- cross-check/robustness algorithms;
- Retester execution;
- optimizer and Walk-Forward execution;
- Custom Project task/databank execution;
- native `.sqx` strategy/result artifacts.

### `SPECIFIED` — TraderCockpit owns the application

TraderCockpit owns:

- desktop shell and native-worker supervision;
- source/idea intake;
- Apollo conversation and guidance;
- deterministic gap detection from registered native requirements;
- configuration editing and explicit approval;
- exact native configuration snapshot custody;
- native job launch/control/readback;
- immutable product identity/custody around native artifacts;
- Candidate Lab;
- Backtest read models;
- Proof/provenance;
- capability discovery and add-on registration;
- presentation and navigation.

TraderCockpit never fills a missing native producer seam with another strategy generator, backtester, robustness engine, optimizer, or task engine.

## 3. Navigation decision: enough tabs, fewer top-level workspaces

### Decision

The current product has enough surface area. **Do not add more permanent top-level research tabs. Consolidate the research workflow into three stable stages:**

```text
CONSTRUCT  →  BACKTEST  →  PROOF
```

This is the persistent stage bar.

### Auxiliary/global surfaces

These are outside the three-stage research bar:

- **Cockpit/Home** — orientation, recent work, native runtime health, resumable work;
- **Explore** — searchable capability/catalog drawer that may expand to a full route;
- **Automation** — native SQX Custom Project/workflow control, visible only when supported;
- **Operate** — runs, performance, execution/risk for capabilities that actually exist;
- **Settings/Runtime** — installation, worker, provider and add-on configuration;
- **Apollo** — persistent assistant dock, not a workspace tab.

Add-ons do not append arbitrary permanent tabs to the three-stage bar.

## 4. Global application frame

### 4.1 Top bar

The top bar contains:

1. TraderCockpit/Home control;
2. fixed stage switcher: `Construct | Backtest | Proof`;
3. compact active-context identity — current idea/strategy/candidate/run as applicable;
4. Explore/search control;
5. native runtime status;
6. optional auxiliary-workspace launcher (`Automation`, `Operate`, installed capabilities);
7. account/settings.

Rules:

- no run identity before a run/job exists;
- no candidate identity before native Builder output exists;
- no market symbol, metric, phase count, asset class, add-on or provider is hard-coded into global chrome;
- runtime status comes from one backend runtime read model.

### 4.2 Left context rail

The left rail is a **research-context navigator**, not a second stage system. It can show, when available:

- Ideas/drafts;
- saved/native strategies;
- candidates for the active idea/build;
- saved validation profiles;
- native Custom Project automations;
- recent runs/proofs.

The categories and counts come from backend read models/capability descriptors. The frontend must not invent a local catalog.

### 4.3 Central workspace and chart

A real market chart is a primary operating surface once market context exists.

The central area supports:

- candlesticks/bars from the configured data producer;
- indicator/block overlays with typed visual descriptors;
- entries/exits and selected trades;
- equity/performance layers where appropriate;
- robustness/validation overlays where meaningful;
- source/provenance layers using the accepted Exposure Sheets interaction language.

Before market/data context exists, the central area shows the idea/specification workspace rather than a fabricated chart.

### 4.4 Persistent Apollo dock

Apollo remains persistent across navigation and retains conversational context, but factual application state always comes from backend records.

Apollo may:

- ingest text, documents, links and supported sources;
- summarize what the user actually supplied;
- identify unresolved native configuration requirements;
- explain choices in plain English;
- prepare explicit configuration changes;
- explain native results and failed gates;
- navigate to the next valid action.

Apollo may not:

- invent missing trading meaning;
- silently approve a plan;
- silently change an approved configuration;
- launch/cancel compute without explicit user action;
- promote/certify a candidate;
- fabricate producer state or result truth;
- create its own capability list from route names or model memory.

## 5. CONSTRUCT stage

Construct answers: **What are we trying to build, what does SQX require to build it, and what exact native configuration will run?**

Construct has four core secondary tabs. These are fixed backbone tabs.

```text
Idea | Specification | Build | Candidates
```

### 5.1 Idea tab

Purpose: begin from human intent, not from Retester or GA controls.

Supported entry modes are capability-driven and may include:

- plain-language trading idea;
- pasted text/notes;
- uploaded paper/document;
- supported URL/video/source ingestion;
- existing native strategy;
- template;
- catalog concept/indicator/model.

UI composition:

- central hypothesis/idea editor;
- source list and provenance sheets;
- optional chart when symbol/data context is already known;
- Apollo in the persistent dock;
- one clear next action: resolve the next material gap or review the specification.

Backend authority:

- immutable/revisioned `IdeaRevisionV1`-class record;
- source references and evidence records;
- no candidate/run identity;
- capability manifest tells the UI which intake modes are installed/available.

State behavior:

- edits create a new idea revision;
- previous approved builds/results remain historical and are not mutated;
- if a new revision changes execution meaning, current downstream eligibility becomes stale until a new specification/configuration is approved.

### 5.2 Specification tab

Purpose: present the resolved Builder requirements in plain English without reproducing SQX's eleven dense settings tabs.

The UI groups native requirements into expandable sections:

1. **Strategy shape** — what to build, direction/style, simple/multi-timeframe/template/improve-existing;
2. **Entry/conditions** — conditions, periods, selected indicators/signals/building blocks;
3. **Exit/risk rules** — order/exit behavior, stop loss, profit target, ATM if supported;
4. **Market & data** — symbol, timeframe(s), dates, IS/OOS, precision, source;
5. **Trading assumptions** — session/trading options and native costs/settings actually used;
6. **Sizing** — money-management method and starting capital where native configuration supports it;
7. **Search** — build mode, genetic options when genetic evolution is selected;
8. **Ranking & filters** — native objective/basic filtering;
9. **Validation profile** — optional native cross-check settings/filtering to run with or after Builder.

These groups are a `SPECIFIED` TraderCockpit presentation mapping over the `PROVEN` SQX Builder settings categories:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks → Ranking → Notes`.

Each field has a backend-produced state:

- `resolved_source` — explicitly supplied by user/source;
- `resolved_native_default` — proven native value/template retained without user change;
- `recommended` — evidence-backed suggestion requiring explicit acceptance;
- `ambiguous` — user choice required;
- `unsupported` — no executable native mapping yet;
- `not_applicable` — field does not apply to chosen build mode.

The frontend never decides these states itself.

Backend authority:

- `ConstructPlanV1`-class immutable/revisioned record;
- native requirement registry/configuration schema;
- exact evidence/source link for non-user-supplied recommendations where applicable;
- explicit plan completeness/eligibility decision.

Run eligibility remains locked while any required field is `ambiguous` or `unsupported`.

### 5.3 Build tab

Purpose: show exactly what will execute and control the real native Builder job.

Pre-launch view:

- chosen native SQX project/template/source snapshot;
- exact build mode;
- human-readable configuration diff;
- native SQX build/runtime health;
- validation profile summary if enabled;
- explicit approval/custody receipt;
- `Start Build` only when the approved plan compiles to a complete native configuration.

Advanced native configuration is available through progressive disclosure. The UI does not show every SQX field by default.

During execution:

- native job state;
- only progress values actually observable from SQX;
- Builder/databank output count where proven;
- stop/cancel only if the native control seam supports it truthfully;
- native producer errors as closed structured errors.

Backend authority:

- approved `ConstructPlan`;
- exact `SqxConfigSnapshot` artifact;
- one `NativeJob` envelope with kind `sqx.builder`;
- native runtime adapter/gateway;
- native output scanner/importer.

**No TraderCockpit genetic algorithm is called from this tab.**

### 5.4 Candidates tab — Candidate Lab

Purpose: inspect real surviving native Builder strategies.

Candidate Lab consumes imported native Builder output only.

Each row/card may display:

- canonical candidate/strategy identity;
- native archive identity;
- originating Idea and Construct/config identity;
- originating native Builder job;
- source project/databank;
- producer-backed native metrics whose meaning is typed and proven;
- custody state;
- validation state if a downstream validation result actually exists.

Never display a TraderCockpit substitute score as native Builder fitness.

Primary actions:

- open selected candidate in Backtest;
- compare compatible candidates/results;
- archive/retain according to product policy;
- start an explicitly selected native validation profile.

Candidate Lab is not another generator.

## 6. BACKTEST stage

Backtest answers: **How did this exact native candidate perform, what trades did it make, how robust was it under the chosen native validation plan, and what configuration produced those results?**

The accepted backbone uses exactly four secondary tabs:

```text
Overview | Trades | Robustness | Configuration
```

Do not add Optimizer, Monte Carlo, Walk Forward, Compare, Prop, or individual add-ons as permanent Backtest tabs. They render through the appropriate tab or auxiliary action.

### 6.1 Overview tab

Purpose: immediate professional summary of the selected exact candidate/run.

Composition:

- persistent candlestick/equity workspace;
- lifecycle/status of the exact run/job;
- producer-backed summary metrics only;
- selected validation profile name/status;
- current native check funnel summary;
- links to Trades, Robustness and Proof;
- compare action where compatible comparison data exists.

`Fast` and `Golden` are backend validation-profile identities, not UI phase counts. The Overview renders whatever ordered plan the backend returns.

### 6.2 Trades tab

Purpose: inspect actual producer-owned trades and tie them to price action.

Composition:

- native trade table;
- candlestick chart;
- selected trade highlights entry/exit and relevant excursion/annotations if native data supports them;
- filtering/sorting may be application-side only over already-loaded exact trade records and must not alter producer truth;
- trade analysis panels appear only for typed supported fields.

No synthetic trade is ever generated for presentation.

### 6.3 Robustness tab

Purpose: render the **native SQX validation funnel** rather than a second robustness engine.

Methods are populated dynamically from the selected backend validation profile/native plan. Current evidenced SQX method families include:

- What If simulations;
- Monte Carlo trades manipulation;
- Higher backtest precision;
- Backtests on additional markets;
- Monte Carlo retest methods;
- Sequential Optimization;
- Optimization Profile / System Parameter Permutation;
- Walk-Forward Optimization;
- Walk-Forward Matrix.

The frontend does not keep this as a closed source-code enum for business logic. The capability/profile read model supplies installed/supported methods and their presentation descriptors.

Each method card/row can expand to:

- native method identity;
- exact settings used;
- exact filtering conditions;
- native job/task state;
- result identity;
- pass/fail/refusal state where producer/filter evidence exists;
- links to associated native result/trade/config artifacts.

Native short-circuit behavior is preserved: if a configured filter dismisses a strategy, later checks do not get fabricated `not failed` results; they are `not executed`/`not evaluated`.

### 6.4 Configuration tab

Purpose: show what actually executed, not what the UI intended to execute.

Composition:

- exact native SQX build identity;
- exact native project/task/config snapshot identity;
- source template/project identity;
- exact symbol/timeframe/data ranges available from the executed configuration;
- session/trading/cost assumptions actually encoded;
- sizing/build/ranking/cross-check values actually encoded;
- approved Construct plan identity;
- user-visible diff from source template to executed snapshot;
- content hashes/artifact custody.

A hash alone does not make this tab `ready`. If the immutable executable configuration artifact/read model is unavailable, the tab says unavailable.

## 7. PROOF stage

Proof answers: **Can we demonstrate exactly what was asked, what executed, what artifact was produced, what was tested, and what evidence supports the current status?**

Proof is one consolidated stage rather than another row of permanent tabs. Its sections are progressive disclosure:

1. **Intent** — Idea/source revision and explicit approved interpretation;
2. **Configuration** — exact native executable snapshot and diff;
3. **Producer** — SQX build/runtime/job identity;
4. **Strategy** — native `.sqx` artifact and candidate custody;
5. **Results** — native result/trade artifacts;
6. **Validation** — exact profile, methods, filters and outcomes;
7. **Status** — generated/tested/passed/refused/promoted/exported states kept distinct;
8. **Delivery** — currently available export/delivery targets supplied by the capability registry.

The Proof stage may visually lock accepted evidence sheets/receipts, but the receipt is not a substitute for the underlying native artifact.

Delivery targets are dynamic. TradingView/Pine, MetaTrader/MQL5, Python/API and future add-ons are capability contributions, not fixed permanent tabs.

## 8. Explore

Explore is a searchable capability/catalog surface accessible globally. It is **not** the normal path users must understand before constructing a strategy.

It covers backend-registered:

- concepts;
- indicators/building blocks;
- strategy/templates;
- data/market capabilities;
- native build modes;
- validation methods/profiles;
- output/delivery targets;
- installed add-ons.

Search/facets come from capability metadata. The frontend must not hard-code the master indicator list, asset classes, providers or feature families.

A capability selected in Explore hands an exact descriptor/reference back into the relevant Construct/Backtest/Proof context.

## 9. Automation — native Custom Projects

Automation is an auxiliary workspace for native SQX Custom Projects.

It presents:

- saved/imported native `.cfx` project identity;
- ordered native task topology;
- task kind and exact task configuration where typed;
- source/target databank relationships where proven;
- running task/progress state where observable;
- loops/go-to behavior where native evidence proves it;
- native output/result custody.

TraderCockpit parses topology for presentation and controls/observes the native Custom Project. It does **not** translate the project into its own task executor.

Unknown future native task kinds are preserved as opaque native task descriptors until a typed renderer/control contract exists.

## 10. Operate

Operate remains auxiliary and capability-gated. Its backbone can retain:

- **Runs** — all current/historical application-native jobs;
- **Performance** — account/strategy performance only where producer scope is explicit;
- **Execution & Risk** — live/deployment/broker risk only when real execution capabilities exist.

Research/backtest results never imply live execution state.

## 11. Backend architecture

### 11.1 One canonical application server

`product/tradercockpit/app_server.py` remains the canonical application HTTP authority unless a deliberate later platform migration replaces it as one whole server.

Do not create separate Builder, robustness, workflow or add-on servers merely to avoid integration work.

The server delegates to domain/application services and one native SQX gateway family.

### 11.2 Logical application layers

The target ownership model is:

```text
HTTP / UI read models
        ↓
TraderCockpit application services
Ideas | Construct | Jobs | Candidates | Backtest | Proof | Capabilities
        ↓
Native SQX gateway
Runtime | Config | Builder | Retester | Optimizer | Projects | Outputs
        ↓
Verified StrategyQuant X 144.2953 runtime
```

Existing code may be refactored incrementally; directory names are less important than these ownership boundaries.

### 11.3 Native SQX gateway

One coherent gateway family owns:

- SQX home/runtime discovery;
- exact build verification;
- launcher identity verification where required;
- exact project/config/template reads;
- deterministic configuration compilation/staging;
- Builder launch/control;
- Retester launch/control;
- Optimizer/Walk-Forward launch/control;
- Custom Project launch/control;
- native databank/output discovery;
- structured producer errors.

The frontend never calls SQX directly.

The gateway never silently falls back to a Python substitute.

## 12. Core domain/custody model

Reuse the existing canonical/content-addressed storage system. Do not create another identity store.

### Existing identities to retain

- `ContentAddress`;
- `FileObjectStore` / canonical immutable object custody;
- lifecycle/evidence stores;
- `StrategySpecV1` as the canonical strategy descriptor envelope;
- `CandidateSpecV1` as canonical candidate identity;
- existing result/evidence types where compatible.

For native SQX strategies, `StrategySpecV1` must remain an envelope such as `sqx.native-archive.v1`; it must **not** become a replacement strategy-rule language.

### Required backbone records

Names below are normative concepts; exact Python class placement may adapt to existing domain code without duplicating authority.

#### `IdeaRevisionV1`

Immutable user/source interpretation revision.

Required relationships:

- previous revision when applicable;
- source/evidence refs;
- user-authored/approved facts;
- no run/candidate identity.

#### `ConstructPlanV1`

Immutable resolved plan for one idea revision.

Contains:

- idea revision ref;
- selected native construction capability/build mode;
- field states (`resolved`, `ambiguous`, `unsupported`, etc.);
- exact user decisions;
- selected validation profile ref if applicable;
- completeness/eligibility status.

#### `ConstructApprovalV1`

Binds explicit approval to one exact Construct plan revision. Approval of one revision never automatically applies to a later revision.

#### `SqxConfigSnapshotV1`

Exact executable native configuration custody.

Contains/references:

- source template/project artifact identity;
- exact compiled `.cfx`/task/config bytes or artifact refs;
- source and compiled hashes;
- typed field mapping/diff;
- SQX build compatibility;
- originating approved Construct plan.

#### `NativeJobV1`

One immutable application request to a native SQX operation.

Kinds include at least:

- `sqx.builder`;
- `sqx.retester`;
- `sqx.optimizer`;
- `sqx.custom-project`.

Binds:

- producer build/runtime identity;
- configuration snapshot/project ref;
- input candidate/strategy refs where applicable;
- invocation identity;
- output/databank scope.

Lifecycle state is stored separately so the immutable request is never rewritten.

#### Native artifact custody

The exact native `.sqx`, result, configuration and trade artifacts must be retained or referenced by immutable artifact records. Hash-only descriptors are insufficient when the product claims it can reopen/prove the executed artifact.

`StrategySpecV1` and `CandidateSpecV1` point into this custody rather than replacing it.

#### `ValidationProfileV1`

Defines one ordered native validation plan:

- profile identity/name/version;
- ordered native method descriptors;
- exact method settings;
- exact filter settings;
- applicability/capability requirements.

`Fast`, `Golden` and user/add-on profiles are instances of this contract. Their method counts are data, not frontend constants.

#### `ProofBundleV1`

Read-model/custody aggregate linking the complete exact chain:

`idea → plan → approval → native config → native job → candidate artifact → result/trades → validation plan/outcomes → delivery/export status`.

It does not copy producer truth into untraceable prose.

## 13. Native configuration compiler

This is one of the most important application components.

### Principle

TraderCockpit does not independently create an approximate SQX configuration from generic fields. It starts from a proven compatible native project/task/config snapshot and applies only registered, typed, supported changes.

### Compilation sequence

1. Read one exact source `.cfx`/configuration snapshot.
2. Verify native build compatibility and source artifact hash.
3. Resolve the `ConstructPlan` against the native requirement/mapping registry.
4. Reject any required `ambiguous` or `unsupported` mapping.
5. Apply only registered typed changes to known native entries/selectors.
6. Preserve all unrelated native bytes/fields semantically where archive serialization permits; record exact output bytes regardless.
7. Produce a new private executable snapshot.
8. Hash and persist the exact snapshot/artifacts.
9. Produce a human-readable diff/read model.
10. Bind launch to that exact compiled snapshot.

No arbitrary XPath/XML path supplied by the frontend is permitted.

No project→preset relationship is inferred from names.

### Mapping registry

Each mutable native field mapping records:

- stable field id;
- native source/task kind;
- target entry/selector;
- value type;
- allowed values/range when proven;
- applicability conditions;
- required/optional status;
- evidence/provenance status;
- UI presentation descriptor.

If a native requirement is not in this registry, the UI cannot pretend it is editable.

## 14. State, revisions and staleness

### State chain

```text
Idea revision
   ↓
Construct plan
   ↓ explicit approval
Configuration snapshot
   ↓
Native Builder job
   ↓
Native candidate(s)
   ↓
Selected validation profile / Retester / Optimizer job
   ↓
Native result + trades + outcomes
   ↓
Proof
```

### Revision rule

Changing an upstream semantic input never mutates downstream historical records.

Examples:

- editing an approved Idea creates a new revision;
- changing specification creates a new Construct plan;
- recompiling creates a new native config snapshot;
- rerunning creates a new NativeJob/invocation;
- modifying a validation profile creates a new profile/version.

Old candidates/results/proofs remain inspectable historical branches.

### Stale-state rule

The UI may label an old downstream object `stale relative to current idea/config`, but must not delete or rewrite it.

### Status separation

Never conflate:

- native/application **job state**: queued/running/completed/failed/cancelled/refused;
- **result availability**: absent/available/corrupt/stale;
- **validation verdict**: passed/failed/not evaluated;
- **promotion/export/deployment state**.

A successful process exit does not mean a strategy passed validation.

## 15. Capability and add-on architecture

The backbone is deliberately extensible without allowing plugins to rewrite the IA.

### 15.1 Backend capability manifest

The backend exposes a versioned `CapabilityManifestV1` containing `CapabilityDescriptorV1` records.

Each descriptor carries at minimum:

- `id` and descriptor version;
- provider/add-on identity and version;
- customer-visible category/job;
- title/description;
- availability state + typed reason;
- producer kind: `sqx-native`, `tradercockpit-application`, or approved external producer;
- required runtime/entitlement state;
- configuration schema/descriptor;
- result schema/descriptor;
- allowed actions and their risk/mutation class;
- presentation contribution(s);
- producer/build/provenance identity.

The frontend and Apollo consume this manifest. They do not maintain independent master feature lists.

### 15.2 Stable extension slots

Add-ons can contribute into typed slots such as:

- `construct.sources`;
- `construct.specification.sections`;
- `construct.rules.blocks`;
- `construct.build.modes`;
- `construct.build.profiles`;
- `construct.candidates.actions`;
- `backtest.overview.analysis`;
- `backtest.trades.analysis`;
- `backtest.robustness.methods`;
- `proof.delivery.targets`;
- `explore.catalog`;
- `automation.native-project-types`;
- `operate.connections`.

An add-on does not append a fourth core research stage.

A major installed capability may expose an auxiliary workspace only through an explicit compatible descriptor and user enablement.

### 15.3 Presentation safety

The capability manifest does not load arbitrary HTML/JavaScript from backend data.

Descriptors select from versioned frontend presentation primitives/renderers. If the frontend does not understand a required renderer version, the capability is shown as unavailable/update-required rather than rendered unsafely.

## 16. Indicator/building-block visual contract

Do not infer indicator appearance from its name.

When a native or add-on capability can be visualized, its typed display descriptor may include:

- surface: chart overlay / separate pane;
- geometry: line / band / channel / histogram / marker / zone / candle state / multi-series;
- series identities and semantic roles;
- scale behavior;
- warm-up period;
- null/gap behavior;
- supported interactivity;
- producer support state.

Records with no proven display descriptor show `Preview not defined`; they do not receive a guessed TradingView-style rendering.

## 17. API blueprint

The canonical server may evolve existing routes toward the following capability-oriented API. Exact naming can be reconciled with current accepted routes, but there must be only one authority per resource.

### Runtime/capabilities

- `GET /api/runtime`
- `GET /api/capabilities`

### Ideas

- `POST /api/ideas`
- `GET /api/ideas/read?ideaRef=...`
- `GET /api/ideas` for authorized listing/search

### Construct

- `POST /api/construct-plans`
- `GET /api/construct-plans/read?planRef=...`
- `POST /api/construct-plans/approve`
- `POST /api/sqx-configs/compile`
- `GET /api/sqx-configs/read?configRef=...`

### Native jobs

- `POST /api/native-jobs` using a registered job capability/kind;
- `GET /api/native-jobs/read?jobRef=...&invocationId=...`;
- `GET /api/native-jobs` for Operate/history;
- explicit cancel/stop action only for native job kinds that support it.

### Candidates

- `GET /api/candidates` with exact idea/config/job filtering;
- `GET /api/candidates/read?candidateRef=...`.

### Validation/Backtest

- `GET /api/validation-profiles`;
- `POST /api/validation-profiles` for supported user-defined profiles;
- `GET /api/backtest/read?...` as the professional four-tab aggregate/read model;
- trade/result/artifact detail endpoints as needed without creating another result authority.

### Proof

- `GET /api/proof/read?...` bound to exact strategy/candidate/run/proof identity;
- immutable artifact retrieval through controlled artifact endpoints.

### Automation

- `GET /api/sqx-projects` / read topology;
- native Custom Project launch through the same native-job authority rather than a second workflow executor.

### Error envelope

All API errors are closed typed errors such as:

- `invalid_request`;
- `not_found`;
- `unsupported_capability`;
- `producer_not_configured`;
- `invalid_runtime`;
- `producer_error`;
- `invalid_state`;
- `stale`;
- `conflict`.

Raw stack traces, filesystem paths, secrets, opaque worker logs and unredacted exceptions do not cross into frontend responses.

## 18. Read-model rules

Frontend pages consume purpose-built read models. They do not reconstruct product truth by joining unrelated routes in JavaScript when the backend can enforce custody/scoping.

Every read model validates its exact context:

- idea/plan/config relationship;
- config/job relationship;
- job/candidate relationship;
- candidate/run relationship;
- producer-build/result relationship;
- proof completeness.

Cross-strategy or cross-run substitution fails closed.

Metrics shown as native truth require typed producer meaning. A field existing in a native archive is not enough by itself to give it a product semantic label.

## 19. Migration of the current UI

Current `main` workspaces are migrated, not independently expanded.

| Current surface | Target backbone disposition |
| --- | --- |
| Cockpit | Home/orientation auxiliary surface |
| Strategies → Overview | Construct Idea/Specification context or strategy detail; reuse verified read-only provenance logic |
| Strategies → Build | Construct → Build |
| Strategies → Signals & Models | Construct → Specification + Explore catalog/detail; no separate research stage |
| Strategies → Candidates | Construct → Candidates / Candidate Lab |
| Strategies → Evidence | Proof |
| Explore → Catalog | global Explore drawer/full route |
| Explore → Market Workspace | chart/data context in Construct/Backtest plus Explore detail |
| Explore → Market Data | Construct → Specification / Market & Data, with Explore management where needed |
| Test & Validate → Run Setup | contextual Build/Backtest action; native-job control, not a top-level stage |
| Test & Validate → Results | Backtest → Overview |
| Test & Validate → Stress & Robustness | Backtest → Robustness |
| Test & Validate → Compare | action/split view from Backtest/Proof; not permanent fifth tab |
| Test & Validate → Prop Simulation | capability/add-on contribution; not core tab |
| Operate → Runs | retain auxiliary Operate → Runs |
| Operate → Performance | retain auxiliary if backed by explicit producer scope |
| Operate → Execution & Risk | retain auxiliary only with real execution/risk producer |

Legacy URLs can redirect during migration, but legacy route names do not dictate architecture.

## 20. Current PR / branch salvage matrix

### Reuse / integrate

- **PR #23** — native SQX candidate archive custody, Retester execution, run lifecycle/readback and stronger native-result custody. Retester remains downstream.
- **PR #2** — verified direct native Builder control/staging/launcher hardening. Generalize into the canonical native gateway rather than retaining preset-only UX as the product architecture.
- **PR #15** — source-proven native Custom Project topology parser/custody; execution stays native SQX.
- **PR #30** — read-only Evidence/Proof concepts, after remapping to the Proof stage and verifying the new native chain.
- **PR #31** — exact compatible result comparison logic as an action/split view, not a core tab.
- **PR #32** — exact strategy/run custody overview logic where it fits the new Construct/Backtest views.
- **PR #33** — persistent route-aware Apollo behavior and autonomy refusals, expanded later to consume the capability/gap-plan contracts.
- **PR #29** — only data/trading fields that map to actual native configuration or are explicitly product-only context with a proven compiler.

### Do not merge as production producer authority

- **PR #25** — TraderCockpit-owned Builder/search/GA producer;
- **PR #27** — TraderCockpit-owned robustness producer where SQX owns the method;
- **PR #28** — TraderCockpit-owned Custom Project/task executor;
- isolated GA/robustness parity PRs — retain as evidence/test donors, not product engines.

### Main-line quarantine

`product/tradercockpit/builder/evolution.py` must not be expanded as native Builder authority. Remove it from production execution as the native spine is assembled.

## 21. Anti-hardcoding rules

The following must come from backend contracts/capability metadata, never permanent frontend constants:

- native indicators/building blocks/catalog contents;
- asset classes/symbol catalog;
- installed data providers;
- available build modes;
- available robustness/validation methods;
- Fast/Golden method counts/order;
- add-ons;
- delivery/export targets;
- producer metrics;
- run/candidate/proof identities;
- supported auxiliary workspaces.

The following are intentionally fixed backbone interaction primitives:

- core stage bar: `Construct | Backtest | Proof`;
- Construct core tabs: `Idea | Specification | Build | Candidates`;
- Backtest core tabs: `Overview | Trades | Robustness | Configuration`;
- Apollo as persistent assistant surface;
- Home and Explore as global application concepts;
- typed extension-slot mechanism.

## 22. Anti-drift executable guardrails

Implementation must add automated checks that make architecture drift expensive.

Required categories:

1. **Navigation contract test** — exactly three core research stages and exactly four core Backtest tabs.
2. **Capability-source test** — dynamic capability lists come from backend manifest/read models, not frontend master arrays.
3. **Producer-boundary test** — no production route/service invokes the quarantined TraderCockpit Builder/robustness/workflow replacement producers.
4. **Single-server/single-store test** — no second application server or candidate/result persistence authority is introduced.
5. **Native configuration custody test** — launch input is byte-identical to the approved compiled snapshot; source changes after compilation cannot alter launched bytes.
6. **No-fallback test** — unavailable/invalid SQX runtime produces typed refusal; no substitute strategy/result appears.
7. **Identity substitution tests** — cross-idea/config/candidate/run/proof substitutions fail closed.
8. **Revision/stale tests** — editing upstream intent creates a new revision and leaves old evidence historical.
9. **Result-truth tests** — frontend cannot render untyped metrics as native truth.
10. **Add-on contract tests** — unknown renderer/capability versions fail closed and cannot inject arbitrary UI/script.
11. **Browser backbone test** — Foundation Vertical through real canonical server, plus reload/reopen.
12. **Desktop/runtime test** — exact SQX worker health before compute and clean worker shutdown where environment permits.

## 23. Foundation Vertical acceptance mapped to the UI

The first executable backbone proof is:

```text
Home / New Idea
  → Construct / Idea
  → Construct / Specification
  → explicit approval
  → Construct / Build
  → exact native SQX configuration snapshot
  → native Builder job
  → Construct / Candidates
  → select real native .sqx survivor
  → Backtest / Overview
  → Backtest / Trades
  → Backtest / Robustness (at least one real native downstream check)
  → Backtest / Configuration
  → Proof
  → restart application
  → reopen same idea/config/candidate/run/proof identities
```

A single real native indicator/building-block configuration is enough for the first vertical. The exact first block must be chosen from retained native configuration evidence whose mapping is proved; it must not be selected because a screenshot happens to display a familiar indicator name.

## 24. Open evidence register before/while coding

These do not block architecture documentation; they are implementation evidence tasks.

### Builder configuration mappings

`OPEN EVIDENCE`:

- exact editable XML/task selectors for the first simple indicator/building-block configuration;
- exact required-vs-default field rules for the first build path;
- whether archive rewriting has ordering/checksum/metadata constraints that must be preserved.

### Native Builder progress

`OPEN EVIDENCE`:

- which progress/generation/databank fields are reliably observable through the available CLI/runtime/files without UI scraping;
- exact stop/cancel semantics.

### Native result metrics

`OPEN EVIDENCE`:

- which result fields have stable verified meaning for Overview;
- exact trade export/read semantics for the professional Trades tab;
- which cross-check outputs expose structured outcomes versus only databank/filter consequences.

### Optimizer / advanced validation control

`OPEN EVIDENCE`:

- exact supported CLI/project-control seams for Optimizer, Walk-Forward and additional cross-check task execution outside already proven Retester behavior.

### Native Custom Project runtime readback

`OPEN EVIDENCE`:

- exact CLI launch/control and task-progress signals;
- loop/go-to runtime observation beyond saved topology evidence.

### Add-on packaging/entitlements

`SPECIFIED` architecture, `OPEN EVIDENCE` deployment details:

- exact plugin/add-on installation mechanism;
- entitlement source;
- signed package/descriptor policy;
- update/version negotiation.

These gaps are solved by extending the native/application adapters and registries—not by changing the three-stage backbone.

## 25. Coding order after documentation is accepted

1. Freeze this documentation set as implementation authority.
2. Reconcile live PR ownership and stop/supersede duplicate producer lanes.
3. Build the canonical native runtime/gateway spine from accepted main + useful PR #23/#2 pieces.
4. Add exact native configuration snapshot/compiler for one first proven Builder case.
5. Implement Idea + Specification contracts and Construct UI against that compiler.
6. Run native Builder and automatically import real native candidates.
7. Wire Candidate Lab.
8. Wire professional four-tab Backtest around native initial + one downstream validation result.
9. Wire Proof.
10. Prove restart/reopen and no-fallback behavior.
11. Only then widen the mapping registry, native validation methods, Automation and add-ons.

## Final invariant

No matter how many add-ons, indicators, data providers, validation methods, delivery targets or future models are installed, the user should still understand the backbone:

**Define what you want to build → see what the real backend produced and how it held up → inspect proof of exactly what happened.**
