# TraderCockpit Product Backbone Specification v1

## Status and authority

This is the **binding detailed implementation specification** beneath:

- `docs/product-architecture-v1.md` — producer ownership and lifecycle;
- `AGENTS.md` — implementation/anti-drift policy;
- `IMPLEMENTATION_CHECKLIST.md` — execution order and release gates;
- `docs/sqx-authoring-authority-v1.md` — native SQX authoring/MCP/`sqx-lab` boundary;
- `docs/consumer-openrouter-account-authority-v1.md` — Google account/OpenRouter/model-routing boundary.

No implementation may contradict this document because an older branch, route name, prototype fixture or isolated test uses another structure.

## Evidence vocabulary

### `PROVEN`

Directly supported by retained SQX product/runtime/configuration evidence, accepted TraderCockpit product authority, or verified product code.

### `SPECIFIED`

A binding TraderCockpit product/application decision selected to expose proven capabilities with lower friction. It is not a claim about hidden SQX internals.

### `OPEN EVIDENCE`

A native mapping, result meaning, progress field, command seam or capability detail still requiring executable evidence. It remains unavailable/constrained until proved and may not be replaced with a speculative TraderCockpit producer.

---

# 1. Backbone in one sentence

A consumer signs into TraderCockpit, receives only the configured bounded model allowance, brings an idea/source into the product, resolves only the information required for an exact executable native SQX configuration, lets the real SQX producer author/build/test strategies, and receives durable Candidate Lab, Backtest and Proof views tied to exact account, configuration, producer, native strategy, result and validation identities.

# 2. Producer and platform ownership

## 2.1 `PROVEN` — StrategyQuant X owns strategy/research computation

SQX 144.2953 owns:

- native AI-assisted strategy authoring and AlgoWizard strategy semantics;
- Builder strategy generation;
- genetic/evolutionary mechanics;
- native strategy/block semantics;
- backtest engine behavior;
- native Builder fitness/ranking/filtering;
- cross-check/robustness algorithms;
- Retester execution;
- optimizer/Walk-Forward execution;
- Custom Project task/databank execution;
- native `.sqx` strategy/result artifacts.

## 2.2 `SPECIFIED` — TraderCockpit owns the product/application

TraderCockpit owns:

- Google consumer identity verification and stable internal account subject;
- account entitlement/allowance/read models;
- OpenRouter provisioning/credential custody and routing policy;
- desktop shell and native-worker supervision;
- source/idea intake and revisioning;
- deterministic native-requirement gap detection;
- configuration editing/review/approval;
- exact native configuration snapshot custody;
- native job launch/control/readback;
- immutable product identities around native artifacts;
- Candidate Lab, Backtest, Proof and auxiliary presentation;
- capability discovery and add-on registration;
- structured refusal when native or external capability is unavailable.

TraderCockpit never fills a missing native producer seam with another strategy generator, backtester, robustness engine, optimizer or task engine.

## 2.3 `SPECIFIED` — OpenRouter owns external inference/spend enforcement

OpenRouter is the consumer external-LLM transport/billing fabric. Provider-enforced key limits/reset/expiry form a hard model-spend boundary.

OpenRouter does not own:

- TraderCockpit account truth;
- native SQX strategy truth;
- candidate/result/proof truth.

The current default workhorse policy is `z-ai/glm-5.3-flash`, but exact model/provider/fallback policy is backend configuration, not a frontend constant.

---

# 3. Native authoring hierarchy

Apollo is deferred and is not part of the backbone.

Use this hierarchy:

1. **Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder** — primary native strategy authoring/generation authority.
2. **Native SQX MCP (`ServletMCP`)** — first-party integration/control. Retained 144.2953 exposes `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, `stop_project`.
3. **`sqx-lab`** — optional external-LLM/custom-artifact extension for install-derived blocks, groups, templates and projects.
4. **TraderCockpit** — orchestration/custody/approval/control/readback/UI.

Rules:

- Do not invent MCP authoring methods absent from the retained registry.
- Do not route every idea through `sqx-lab` merely because external LLMs can call it.
- `sqx-lab` output is accepted only when install-derived, validated and loaded/accepted by the target SQX runtime.
- A missing direct native-AI invocation seam does not move strategy authority into TraderCockpit.

---

# 4. Navigation decision

The core research workflow has exactly three persistent stages:

```text
CONSTRUCT  →  BACKTEST  →  PROOF
```

Construct has exactly four core tabs:

```text
Idea | Specification | Build | Candidates
```

Backtest has exactly four core tabs:

```text
Overview | Trades | Robustness | Configuration
```

Do not add Optimizer, Monte Carlo, Walk Forward, Compare, Prop, LLM, MCP or individual add-ons as permanent research tabs.

## Auxiliary/global surfaces

- **Home/Cockpit** — orientation, recent work, account summary, native runtime health, resumable work;
- **Explore** — searchable backend capability/catalog surface;
- **Automation** — native SQX Custom Projects/workflows;
- **Operate** — runs/performance/execution/risk only for capabilities that truthfully exist;
- **Account/Settings** — Google account, plan/allowance, model usage, runtime/provider/add-on configuration.

There is **no required persistent Apollo dock**. Language assistance may appear contextually, but it is a bounded consumer tool surface, not a stage or independent product spine.

---

# 5. Global application frame

## 5.1 Top bar

The top bar contains:

1. Home control;
2. fixed stage switcher `Construct | Backtest | Proof`;
3. compact active-context identity;
4. Explore/search;
5. native runtime status;
6. optional auxiliary launcher (`Automation`, `Operate`, installed capabilities);
7. account/allowance/settings.

Rules:

- no run identity before a run/job exists;
- no candidate identity before native Builder output exists;
- no fabricated symbol/metric/phase count/provider/model appears in chrome;
- runtime status comes from one backend native-runtime read model;
- account/remaining allowance comes from one backend account/usage read model;
- model/provider routing policy is not selected by arbitrary browser parameters.

## 5.2 Left context rail

The left rail is a research-context navigator, not a second stage system. It may show backend-provided:

- ideas/drafts;
- saved/native strategies;
- candidates;
- validation profiles;
- native Custom Project automations;
- recent runs/proofs.

Counts/categories come from backend read models/capability descriptors.

## 5.3 Central workspace and chart

When market context exists, the central area supports:

- candlesticks/bars from configured data authority;
- indicator/block overlays with typed visual descriptors;
- entries/exits and selected trades;
- equity/performance layers where producer data exists;
- robustness/validation overlays where meaningful;
- provenance/evidence layers.

Before valid market/data context exists, show the idea/specification workspace rather than a fabricated chart.

## 5.4 Contextual language assistance

A language-assistance surface may:

- summarize user-supplied text/documents/supported sources;
- identify unresolved registered native requirements;
- explain choices/evidence;
- prepare explicit configuration proposals/diffs;
- explain native results and failed gates;
- propose/navigation to the next valid action;
- invoke allowed tools through backend authorization.

It may not:

- invent missing trading meaning;
- silently approve a plan;
- silently mutate an approved configuration;
- launch/cancel consequential compute without required authorization;
- waive native validation;
- promote/certify a candidate;
- fabricate state/result truth;
- maintain an independent capability catalog;
- bypass account/allowance/tool authorization.

---

# 6. CONSTRUCT stage

Construct answers: **What are we building, what does native SQX require, and what exact configuration will execute?**

## 6.1 Idea tab

Purpose: begin from human intent, not from Retester/GA controls.

Entry modes are capability-driven and may include:

- plain-language idea;
- pasted text/notes;
- uploaded document/source;
- supported URL/source ingestion;
- existing native strategy;
- template;
- catalog indicator/concept/model.

Composition:

- central idea/hypothesis editor;
- source/provenance list;
- optional chart when valid context is known;
- contextual language-assistance entry where enabled;
- one clear next action: resolve material gap or review specification.

Backend authority:

- immutable/revisioned `IdeaRevisionV1`-class record;
- source/evidence references;
- no candidate/run identity;
- capability manifest declares installed intake modes.

State behavior:

- edits create new revisions;
- historical approved builds/results never mutate;
- execution-meaning changes stale downstream eligibility until new configuration is approved.

## 6.2 Specification tab

Purpose: present resolved Builder requirements in plain language without cloning SQX's dense settings UI.

Group native requirements into:

1. Strategy shape;
2. Entry/conditions;
3. Exit/risk rules;
4. Market & data;
5. Trading assumptions;
6. Sizing;
7. Search/build mode;
8. Ranking & filters;
9. Validation profile.

This presentation maps over proven SQX Builder categories:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks → Ranking → Notes`.

Each field receives a backend-produced state:

- `resolved_source`;
- `resolved_native_default`;
- `recommended`;
- `ambiguous`;
- `unsupported`;
- `not_applicable`.

Frontend code never decides these states itself.

Backend authority:

- immutable/revisioned `ConstructPlanV1`-class record;
- native requirement registry/config schema;
- evidence/source links for recommendations;
- explicit completeness/eligibility decision.

Run eligibility remains locked while a required field is ambiguous or unsupported.

## 6.3 Build tab

Purpose: show exactly what will execute and control the real native Builder job.

Pre-launch view:

- chosen native project/template/source snapshot;
- exact build mode;
- human-readable config diff;
- native SQX runtime/build health;
- validation profile summary if enabled;
- explicit approval receipt;
- `Start Build` only when plan compiles to complete native config.

During execution:

- native job state;
- only observable progress fields;
- output/databank count only where proven;
- stop/cancel only through supported native control;
- native producer errors as structured closed errors.

Backend authority:

- approved Construct plan;
- exact `SqxConfigSnapshot` artifact;
- one native job envelope such as `sqx.builder`;
- native gateway;
- native output scanner/importer.

No TraderCockpit genetic algorithm is called from this tab.

## 6.4 Candidates tab — Candidate Lab

Consumes imported native Builder output only.

Candidate rows/cards may display:

- canonical candidate/strategy identity;
- native archive identity;
- originating Idea/Construct/config identity;
- originating native Builder job;
- source project/databank;
- typed producer-backed metrics;
- custody state;
- downstream validation state only when such result exists.

Never display a TraderCockpit substitute score as native Builder fitness.

Actions:

- open selected candidate in Backtest;
- compare compatible candidates/results;
- retain/archive according to product policy;
- start explicitly selected native validation profile.

Candidate Lab is not a generator.

---

# 7. BACKTEST stage

Backtest answers: **How did this exact native candidate perform, what trades did it make, how robust was it under the selected native plan, and what configuration actually executed?**

## 7.1 Overview

Composition:

- chart/equity workspace where data exists;
- exact job/run lifecycle;
- producer-backed summary metrics only;
- selected validation profile identity/status;
- native check-funnel summary;
- links to Trades, Robustness and Proof;
- compatible compare action.

`Fast`/`Golden` are backend validation-profile identities, not fixed UI phase counts.

## 7.2 Trades

- native trade table;
- candlestick chart;
- selected trade entry/exit highlighting when native data supports it;
- application-side filtering/sorting may operate only over loaded exact records;
- typed analysis panels only for supported fields.

No synthetic trades.

## 7.3 Robustness

Render native SQX validation profile/methods dynamically.

Current evidenced families include:

- What If;
- Monte Carlo trades manipulation;
- Higher backtest precision;
- Additional markets;
- Monte Carlo retest;
- Sequential Optimization;
- Optimization Profile/System Parameter Permutation;
- Walk-Forward Optimization;
- Walk-Forward Matrix.

Each method can expose:

- native method identity;
- exact settings;
- filter conditions;
- native job/task state;
- result identity;
- pass/fail/refusal only where evidence exists;
- links to native result/trade/config artifacts.

Native short-circuit behavior is preserved. If a candidate is dismissed, later checks become `not executed/not evaluated`, not fabricated passes.

## 7.4 Configuration

Show what actually executed:

- SQX build identity;
- native project/task/config snapshot identity;
- source template/project identity;
- symbol/timeframe/data ranges encoded;
- session/trading/cost assumptions encoded;
- sizing/build/ranking/cross-check values encoded;
- approved Construct plan identity;
- source→executed diff;
- exact artifact custody/content hashes.

A hash alone does not make the tab ready. The immutable executable config artifact/read model must be available.

---

# 8. PROOF stage

Proof answers: **What was requested, what executed, what artifact was produced, what was tested, and what evidence supports current status?**

Progressive sections:

1. Intent — Idea/source revision and approved interpretation;
2. Configuration — exact executable snapshot/diff;
3. Producer — SQX build/runtime/job identity;
4. Strategy — native `.sqx` artifact/candidate custody;
5. Results — native result/trade artifacts;
6. Validation — exact profile/methods/filters/outcomes;
7. Status — generated/tested/passed/refused/promoted/exported kept distinct;
8. Delivery — available targets from capability registry.

A receipt never substitutes for underlying native artifacts.

---

# 9. Explore

Explore is a searchable capability/catalog surface. It is not a prerequisite feature wall.

It covers backend-registered:

- concepts;
- indicators/building blocks;
- native strategies/templates;
- data/market capabilities;
- build modes;
- validation methods/profiles;
- delivery targets;
- installed add-ons.

Search/facets come from capability metadata. Frontend code does not hard-code master indicator, asset-class, provider or feature-family lists.

---

# 10. Automation — native Custom Projects

Automation presents:

- native `.cfx` project identity;
- ordered task topology;
- task kind/config where typed support exists;
- source/target databanks;
- current task/progress where native readback exists;
- structured opaque display for unknown task kinds.

Execution remains native SQX. TraderCockpit does not recreate task loops/branching as a second workflow engine.

---

# 11. Consumer account / Settings

## 11.1 Account identity read model

The backend exposes one account state containing at least:

- stable internal subject;
- verified sign-in state;
- presentation email/profile fields as allowed;
- entitlement/plan identity;
- allowance status;
- refusal/error state where relevant.

Google identity scopes stay minimal for authentication.

Repeated sign-in must resolve the same internal subject and must not duplicate starter-credit grants.

## 11.2 OpenRouter spend/read model

Backend state contains:

- bounded consumer model-spend authority identity;
- configured hard limit/reset/expiry state where applicable;
- usage/cost attributed to account;
- remaining allowance/read-model status;
- current workhorse policy identity;
- provider/model routing status/refusal where relevant.

The management/provisioning credential never enters browser code or customer-visible config.

## 11.3 Model policy

Backend policy defines:

- default model slug, currently `z-ai/glm-5.3-flash`;
- provider preference/routing constraints;
- fallback/escalation rules;
- per-capability model requirements;
- request/tool limits.

Browser input may select only explicitly exposed product choices, never arbitrary provider credentials or unbounded model IDs.

---

# 12. Native SQX gateway

Use one canonical backend gateway family for native SQX operations.

Responsibilities:

- install discovery/build verification;
- native project/config/databank identity;
- Builder launch/stop/status;
- Retester/Optimizer/Custom Project control where supported;
- native output scanning/import;
- structured error/refusal mapping;
- MCP integration for the exact published MCP tools where useful.

Browser code never calls SQX tooling directly.

Unknown/unavailable native operation fails closed rather than invoking a product substitute.

---

# 13. Native configuration compiler

The compiler operates on exact native source snapshots.

Required properties:

- registered typed fields only;
- no arbitrary browser-provided XML/XPath selectors;
- source artifact/build identity verification;
- untouched native fields preserved;
- exact output bytes/content identity retained;
- human-readable diff emitted;
- approval bound to exact plan/config revision;
- immutable launch snapshot;
- incomplete/unsupported required fields refuse before launch;
- reopen after restart.

A later source/template change cannot mutate the already-approved launch artifact.

---

# 14. Identity, custody, revision and staleness

Use one durable identity chain:

`AccountSubject → IdeaRevision → ConstructPlan → SqxConfigSnapshot → NativeJob → NativeStrategyArtifact → Candidate → NativeResult/Validation → Proof`.

Not every entity is created for every action, but identities that exist must never be silently substituted across context.

Rules:

- idea edits create new revisions;
- approved historical configs/results remain immutable;
- downstream eligibility becomes stale when upstream execution meaning changes;
- account changes do not reassign historical research ownership silently;
- candidate/result identities bind exact producer/config/source context;
- hashes supplement artifact custody; they do not replace storing required exact bytes.

---

# 15. Capability manifest and add-ons

One backend `CapabilityManifestV1`/descriptor authority drives dynamic capability discovery.

Descriptors may include:

- stable capability id/version;
- category/type;
- availability/maturity;
- required native/add-on/runtime dependency;
- configuration schema/descriptor;
- result schema/descriptor;
- allowed actions/risk class;
- presentation contribution;
- producer/build/provenance identity.

Frontend and bounded language/tool surfaces consume this manifest. They do not maintain independent master feature lists.

## Stable extension slots

Add-ons may contribute typed content to slots such as:

- Idea intake/source types;
- Specification requirement sections;
- Build configuration panels;
- Candidate detail panels;
- Backtest Overview/Trades/Robustness/Configuration detail cards;
- Proof evidence/delivery sections;
- Explore catalog entries;
- Automation task presenters;
- Operate widgets where compatible.

Add-ons may not:

- add arbitrary permanent core-stage tabs;
- inject arbitrary frontend HTML/JavaScript from backend data;
- overwrite core account/native producer authority;
- masquerade as native SQX results.

Unknown renderer/capability versions fail closed/update-required.

---

# 16. Visual/presentation descriptors

Indicator/chart previews require typed display descriptors, such as:

- overlay/pane placement;
- line/area/candle/marker representation;
- value axes/units;
- parameter labels;
- supported interaction hooks.

Missing descriptor means `Preview not defined`; do not guess visuals from model memory or names.

---

# 17. Current-work migration/disposition

Retain/reassess:

- PR #2 — native Builder control/staging/launcher direction;
- PR #23 — native candidate/Retester/custody/readback;
- PR #15 — native Custom Project topology custody;
- PR #29 — only data/trading fields truthfully mapped to native config or explicitly product-only state;
- PR #30 — Proof/Evidence UI concepts when bound to canonical native chain;
- PR #31 — compatible comparison as action/split view;
- PR #32 — exact strategy/run custody read logic where it fits;
- retained `ServletMCP` — published native control/readback tools only;
- `codex/sqx-lab-plugin` — optional custom native-artifact extension;
- earlier TraderCockpit/Futures Google/OpenRouter concept — consumer account/LLM design lineage only.

Do not merge as production producer authority:

- PR #25 — TraderCockpit Builder/search/GA producer;
- PR #27 — duplicate TraderCockpit robustness producer;
- PR #28 — duplicate TraderCockpit workflow/task executor;
- PR #33 — persistent Apollo product spine;
- isolated GA/robustness algorithm-parity modules as product engines.

---

# 18. Release acceptance

## 18.1 Consumer account/OpenRouter proof

Required path:

```text
Google sign-in
  → stable internal subject
  → configured starter/plan allowance
  → bounded provider-enforced OpenRouter authority
  → backend routes request to configured GLM 5.3 Flash policy
  → usage/cost attributed to subject
  → remaining allowance updates
  → hard limit refuses further spend
  → sign-out/lapse/revocation cannot continue spending
```

Failure cases include duplicate first-login credit grant, invalid identity, missing provider credential, exhausted allowance, revoked entitlement and unavailable model/provider.

## 18.2 Native SQX Foundation Vertical

Required path:

```text
Idea
  → native SQX authoring capability when needed
  → Specification
  → approved exact native configuration
  → native Builder
  → real .sqx survivor
  → Candidate Lab
  → native validation/retest
  → Backtest
  → Proof
  → restart/reopen same identities
```

A mock, fixture, synthetic result, external-LLM-only artifact, TraderCockpit-only strategy schema or substitute producer does not satisfy the native gate.

---

# 19. Anti-drift executable guardrails

Keep executable guards for:

- exact three-stage research navigation;
- exact four Construct tabs;
- exact four Backtest tabs;
- absence of persistent-Apollo requirement;
- one capability-manifest authority;
- no production calls to quarantined replacement producers;
- native SQX AI authority not replaced by OpenRouter/`sqx-lab`;
- one canonical native gateway;
- exact approved-config-to-launch byte custody;
- no native-runtime fallback;
- one account/allowance authority;
- provider-enforced OpenRouter spending ceiling;
- backend-configured model policy;
- no provider management credential in browser/customer source/config;
- duplicate first-login credit prevention;
- cross-context identity substitution refusals;
- revision/staleness preservation;
- typed native-result truth;
- safe add-on renderer/version behavior;
- full browser/restart Foundation proof.

---

# 20. Open-evidence discipline

The architecture is settled; implementation details still requiring evidence remain `OPEN EVIDENCE`.

Examples:

- exact callable native SQX AI/AI Wizard seam;
- additional MCP tools in future builds;
- unproved progress/readback fields;
- native XML/config selectors not yet mapped;
- untyped metric meanings;
- advanced optimizer controls;
- Custom Project readback details;
- add-on deployment/version mechanics;
- future provider/model routing characteristics.

Missing evidence is a reason to inspect, constrain or refuse. It is not permission to alter the backbone, fabricate results or create a replacement producer.
