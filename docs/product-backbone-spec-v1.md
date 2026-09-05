# Product Backbone Specification v1

This document is the detailed implementation contract beneath `docs/product-architecture-v1.md`.

Current sequencing/status lives only in `LIVING_IMPLEMENTATION_PLAN.md`.

## 1. Global desktop frame

Top-level navigation follows the first-release owner ruling (2026-09-05):

`Getting started | Builder | Custom projects | Apollo | Data organization | Settings`

Getting started is today's Home cockpit. Builder / Custom projects open Progress | Full settings | Results against that native archive. Custom projects uses the official SQX list row structure (name, Tasks, Engine, Results, progress, transport, databanks/strategies, gear) with a 2026 facelift. Documented enumerated Full settings attributes render as choice controls. Data organization discovers native data and explicitly captures provider metadata/history; native import and task application retain separate readiness and provenance. Apollo is the full-page bounded `/api/assistant` (textarea composer, Ask, Speak, Quant-Guild, approved tools). Home jumps to `/apollo` instead of mounting a second thread. Native AlgoWizard block authoring stays in StrategyQuant X. Explore and Research-as-pipeline are not rail labels.

The frame is the desktop chrome (`references/ui-authority` Home zones + 2026 module-rail facelift):

- left rail: brand, the SQX module surfaces, a workspace card (`/api/status` application), a custody-progress card (custody stages with at least one record plus the one legal next action from `/api/research/next-action`), an account card (`/api/status` account), and a version line;
- market ticker: one cell per operator-configured watchlist symbol (`TRADERCOCKPIT_WATCHLIST`) with `last`/`change` only from a `current` provider record (otherwise `—`), a structural sparkline slot, and a market-state cell bound to the market read model;

The global workspace/readiness/search/notification strip and live-account footer were
removed by the owner. Do not reserve space for them. The persistent strategy databank dock
is part of the Builder / Custom projects work area and has actual selection/file controls;
it is not the deleted live-account footer. Page-specific controls remain available.

Rules:

- `/home` is the first-launch route; later desktop launches restore the last registered
  session path (`/api/desktop/session`) including Research custody query keys; explicit
  `--start-path` still wins;
- `/research` is the canonical historical-research route; `/research?workspace=<id>&tab=<id>` selects one of the four registered workspaces and its tabs; pre-prototype `stage`/`tab` links canonicalise to those routes while preserving custody selection parameters; in-Research chrome hops copy the same identities, and Home Quick Actions start without leftover IDs;
- Apollo is the full-page bounded assistant; Home links to it and Research retains its compact assistant. Both use `/api/assistant`, backend provider/model policy and the same approved custody tools. Readiness and unavailable states remain truthful; the assistant does not own result truth or bypass exact approval;
- no frontend-owned master list of providers/models/native capabilities;
- no fabricated runtime, market, account, candidate, result, or deployment identity in global chrome;
- one `web/` tree of vanilla ES modules; no framework or build system.

## 2. Home contract

Home answers: **what matters now and where should the user go next?**

Home is the live/current Cockpit Home. Neon chrome and card density come from
`references/ui-authority`. Card titles in `cockpit-home.png` are illustrative framing, not the
Home zone contract.

1. hero — first-release research orientation and navigation into the supported six surfaces;
2. eight zones, in this order, each bound as follows;
3. persistent Apollo assistant (not a Home zone) from `/api/status` assistant/model/provider and `/api/assistant`.

| # | Zone | Read model | Truthful state |
| --- | --- | --- | --- |
| 1 | Market Overview | `/api/market/quotes` + `/api/status` `market_data` | operator watchlist quotes; live producer context; `—` / not-connected until a provider exists |
| 2 | System Status | `/api/status` | application, research backend, custody, native execution, live market data, provider, account, model, extensions — each with its own readiness |
| 3 | Alpha Stack | `/api/research/candidates` | current Candidate custody only; promoted / exported / deployed stay distinct and unavailable until those authorities exist |
| 4 | Pipeline Overview | Research custody catalogs | lifecycle counts (idea, configuration, native job, candidate, historical result, proof); never a validation or promotion verdict |
| 5 | Signals | none yet | "Live signals not connected"; historical backtests are not live signals |
| 6 | Risk | none yet | "Live risk state not connected"; exposure / drawdown `—` |
| 7 | Performance | none yet | "Current performance not connected"; live and historical scopes stay explicit |
| 8 | Quick Actions | none | navigation into owning surfaces only |

Each zone reads only the producer that owns its state. Historical research values never
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
- `Signals & Models` — actual OHLC bar chart from the bar-series read model (toolbar, tools, price/volume/CVD frames; `unavailable` until a bar producer exists; last/change quotes are not bars; native trade overlays when a Historical Result is selected), the **Native Strategy Specification** (the exact native Builder task: strategy shape, market identity, historical data setup, trading assumptions, building blocks, money management, search/build mode with distinct Random Discovery and Genetic Evolution lanes, ranking & filters, validation profile, source provenance; the native `Blocks`/`Rankings`/`CrossChecks`/`MoneyManagement` subtrees as collapsible read-only inspectors; capability coverage), Strategy Panel (enabled native signal blocks), Signal Pulse and Active Models (no live producer / ML modality not connected), and the bottom row Confluence · Market State · Session Context · Risk Overlay · Assistant.
- `Order Flow | Footprint | Volume Profile | Liquidity Map | Replays` — full chart frames with explicit provider requirements.
- `Alerts` — alert table (no alert producer yet). `Reports` — immutable Research Proofs.

### Workspace `evolution` — Evolutionary Search

Strip: `State` (latest native job state) · `Objective Set` (native `FitnessCriteria` ranking + acceptance condition count) · `Optimization` · `Search Mode` (exact native `BuildMode@generationType` as Genetic Evolution / Random Discovery / other) · `Deterministic Seed` (not exposed) · `Budget` (native `MaxStrategies` + stop condition) · `Time Elapsed` (not exposed) · `Pause`/`Stop` (disabled; no native control seam).

Cards: Search Configuration, Population (islands), Generations, Pareto Frontier, Variation Operators, Fitness Evolution, Islands Overview, Archive & Objectives (native acceptance conditions), Top Candidates (Candidate import bound to exact native Results archives), Deterministic Seed/Budget, and the exact configuration custody workspace (compile → review → approve → launch through the trusted gateway). Every value is the native tag's exact text with the tag name shown; live GA telemetry frames stay `no data` because the native Builder does not stream it.

### Workspace `validate` — Test & Validate

Tabs: `Overview | Initial Test | Trades | Robustness | Configuration | Evidence`.

- `Overview` — candidate stage history, progress/counts, metrics, equity and result/evidence rows bind to the selected candidate and attempt. The active stage list comes from the approved native graph, including loops and failed attempts. Existing cockpit verdict categories (`Initial Test | Fast Validation | Golden Validation | Scenario Tests | Stress Tests | Out-of-Sample | Evidence`) remain attributed policy summaries, not a fixed native execution funnel. Missing counts or outcomes remain unavailable/incomplete; research next actions do not require an Operate producer.
- `Initial Test` — native Retester execution/readback. `Trades` — exact native trade rows. `Robustness` — producer-backed methods catalogued from the exact native CrossChecks subtree (launchability and verified result coverage are reported per method; profile presence alone does not establish execution or a passing result). `Configuration` — the executed chain. `Evidence` — Research Proof.

### Workspace `catalog` — Indicators & Models

Pills: `All Components | Indicators | Models | Strategies | Utilities | My Components`. Components are the exact native building blocks (`signals`, `indicators`, `stopLimitBlocks`, `orderTypes`, `exitTypes` with category/enabled/weight/parameter attributes), native templates, imported native strategies and Ideas. Search and category filtering run over the loaded set; market fit, timeframe fit, rating, dependencies and performance render `—` because no producer exposes them. `Models` carries the platform-owned ML modality: fit allowlisted sklearn classifiers on native trades from one completed Historical Result, then bind the catalog digest onto an existing native Candidate. SQX still owns backtest and robustness. `Utilities` hosts native project topology and preset verification.

Route/query state may select only registered workspaces/tabs. Arbitrary query text never creates new product states or durable identities.

### Stage contracts

The owner-approved executable pipeline is the actual native graph, not a fixed seven-stage
sequence. The seven historical cockpit-verdict categories described above are a separate
policy summary. They must not hide actual task names, repeated loop visits, failed native
filters, skipped tasks, partial results or capture gaps. The workspace/tab mapping below
describes existing presentation locations; it does not prescribe native execution order.

The existing custody stage locations are retained; each is hosted in the workspace/tab named here:
Idea → `signals` / Overview; Specification → `signals` / Signals & Models; Build → `evolution`;
Candidates → `evolution` (Top Candidates); Backtest Overview → `validate` / Initial Test;
Trades → `validate` / Trades; Robustness → `validate` / Robustness; Configuration → `validate` /
Configuration; Proof → `validate` / Evidence.

### Candidate storage and databank operations

Import accepts a real native strategy archive with its exact bytes/hash and source. Native
compatibility is observed or refused; older format/build metadata is not silently replaced.
External import does not fabricate generation, approval, backtest or validation history.
Repeated import of the same source into the same destination reuses the recorded operation
and candidate/revision rather than duplicating it. Intentional copy adds a membership.

Before native import effects, reserve a random Candidate identity, operation ID and
`TraderCockpitCandidateTokenV1` plus exact original and separately stamped derivative bytes
in the mutation journal. Retain an immutable import root with `user_import` origin and
unknown history; it has no active native membership. Publish the verified native output
as an exact child revision and admit its membership only after token, whole-artifact and
location checks. A first import may accept the native `ResultsGroup/@ResultName` assignment
only when it matches the explicitly requested filename; this exception does not apply to
generic reserialization. Exported marked archives imported anew receive a new token and
Candidate identity while preserving the original file and unknown history.

Explicit reconciliation is a POST bound to the same project/bank/archive, Candidate revision,
previous and observed archive hashes, and expected membership revision. It retains both
archives and records verified reserialization in membership history without changing the
original Candidate revision. A matching token alone, a filename or a GET request never
updates association. During a prepared Candidate purge, the same explicit reconciliation
may follow a confirmed membership's verified storage-hash history. It cannot change its
location or Candidate revision, and is blocked once custody deletion begins. Purge preview
returns the retained intent on reload; retry accepts only confirmed locations and their
verified reserialization history without replacing the original preview or its hash.
Unmarked legacy archives refuse automatic reassociation. Prepared
failed imports remain retained and pending until an explicit user action. The implemented
pending-import discard reuses purge preview/confirmation for this action, limited
to phase `prepared` with native disposition `not_submitted`. Submitted imports refuse
discard and offer resume followed by ordinary Candidate purge. Engine idle and exact
memory/disk absence are insufficient: a timed-out HTTP import may still have an active
writer. The native source audit found no supported drain/cancel acknowledgment. Reject
`confirmed_absent` as a discard authority and recheck the exact prepared phase before
effects. Any future terminated-process/worker barrier requires independent proof before
expanding this scope. Never delete ambiguous native output. Preserve original/shared evidence and
an operation-ID tombstone so retry cannot resurrect the discarded import. Verify stale
preview, repeated confirmation, restart and measured unreferenced-space reclamation.
`POST /api/sqx-databank/import-discard-preview` takes the exact original load request
(`project`, `databank`, `archive`, `source_sha256`, `operation_id`). The corresponding
`import-discard-confirm` adds `expected_preview_sha256`. Both bind the reserved import
identity and journal; no current Candidate or membership is invented. The preview carries
`cancel_import` with its exact request, journal digest, phase and native disposition.
Persist the confirmed preview hash with the browser's original pending load request before
confirmation. A lost response or interrupted purge retries that same deletion after reload.
Only adapter pre-intent refusals `databank_import_discard_preview_changed` and
`databank_import_submitted` permit releasing that hash for a new preview or load resume.
The general purger's `candidate_purge_preview_changed` can occur after intent creation;
it is not authority to clear the pending deletion. Dismissing an unconfirmed preview is local.

The persistent databank dock in Builder and Custom projects supports import, save/export of
the complete archive, rename, copy, move, remove and clear. It remains available while the
selected candidate's details or stage history are open. File names and mutable bank positions
must not become the candidate's durable identity. Reopening preserves membership, selection,
revision and history. Empty, loading, failed, partial and stale states are explicit.

Task scope (owner clarification 2026-09-05): derive input/output/clear bank references from
the selected task's saved native configuration. Show these roles and use the task's output
context when switching tasks; do not carry another task's bank/archive implicitly. Preserve
selection when the same bank remains applicable, and allow explicit inspection of the
project's other banks. The project list and unrelated rail pages are not a global working
databank. Shared task banks remain shared; historical per-visit results require the stage
capture contract, not invented copies or one bank manufactured per block.

Every mutation binds the selected project/bank/candidate revision and expected archive hash,
rejects collisions or stale identity, and uses the native producer where it owns the action.
Browser inputs never choose arbitrary filesystem/executable paths. Upload size, expanded ZIP
size, members, path aliases and native-input structure are bounded before native invocation.
Readback must confirm the intended native and persisted result; a successful HTTP response
alone does not establish completion. No operation changes unrelated archives or projects.

Copy preserves source membership and creates the selected destination membership. Move
removes source membership only after destination persistence is confirmed. Rename preserves
candidate identity and history. Clear removes the explicitly previewed bank memberships; it
does not silently delete their retained candidate evidence. Ambiguous partial completion
remains recoverable and refuses blind retry.
Import/rename/copy/move/remove/clear and reconciliation requests carry an explicit `operation_id` for the user intent. A retry
uses the same ID and exact bound request; a new intentional action uses a new ID, including
copy A to B after B was removed. Persisted journals must distinguish these cases and refuse
reuse of an ID with different inputs. Import deduplication remains bound to source and
destination identity rather than manufacturing another candidate for a transport retry.
Retain the complete uncertain request across reload and provide an explicit retry action.
For interrupted Clear, replay the originally confirmed snapshot and operation ID even when
some rows are already absent; do not replace that intent with a snapshot of the remaining rows.

Native lifecycle verification includes archive integrity and preservation after process
exit, covering pre-existing banks as well as the selected output. Do not treat successful
selected-record persistence or exit code 0 as proof that background native writers finished.
The observed GUI plus CLI `-exit` path is unsafe. Normal native UI exit has preserved the
verified strategy/trade/equity/statistic content while rewriting archive serialization and
display caches. Preserve those distinct byte identities; acceptance still requires explicit
verified reassociation and live reopen, not a silent relaxation of archive-hash checks.
The bounded integrated native restart/reconciliation has now been exercised as recorded in
the living plan; broader Gate 1 and live browser acceptance remain incomplete.

**Remove from this databank** and **Delete candidate and retained files** are separate actions.
The latter requires a preview of affected candidate revisions, stage results/history,
memberships and reclaimable bytes, then explicit confirmation bound to that preview. Delete
unreferenced content from all application-managed evidence, staging, backup and cache stores;
retain shared blobs while another candidate references them and retain only a small deletion
record. Do not claim reclaimed space while hidden large copies remain. External original
files outside the preview are untouched. Failed candidates receive no automatic cleanup.

Acceptance covers import/reopen/idempotent retry, rename/save/copy/move/remove/clear, stale
requests and collisions, partial failure, shared-content deletion and actual space recovery.
Verify unrelated native files and the source archive remain unchanged unless explicitly
included in the user-confirmed operation.

### Lossless native stage capture

Each execution binds the exact approved graph/configuration, producer/runtime and input batch.
Each admitted candidate input is accounted for. Each task visit, including every loop visit,
has a distinct attempt with input revision, task identity, start/completion state, native
counts, output identities, metrics and native acceptance outcome. Distinguish executed/pass,
executed/filter-fail, skipped, refused, interrupted and capture-incomplete. Execution success
does not imply a profitable strategy or successful validation.

Capture must precede destructive native copy/move/clear or replacement when needed to retain
the candidate's stage evidence. Synchronous, non-filtering native Custom Analysis (CA) capture checkpoints belong to the
reviewed derivative native graph and its approval/diff. Preserve the user's original project
bytes. The checkpoint does not calculate a substitute backtest, alter native filters or
decide the native task loop. A fully tracked launch refuses if any required capture boundary
is unverified. A later reconciliation cannot manufacture an unobserved result.

Acceptance executes an actual approved native pipeline containing pass and filter-fail
outcomes, a loop and a destructive databank operation. Every admitted input and task visit
must be represented after completion/reopen, with the failed candidate's files/history still
available. Stage totals and result identities reconcile with native task logs and archives.

### Automatic execution with traceability

One approval binds the batch, exact graph, required data/settings and capture checkpoints.
Native SQX then advances the approved graph automatically. The cockpit records durable
execution/attempt transitions and presents them on the selected candidate; it does not own
a replacement scheduler for native task semantics. Controls reach the owned/current native
process. Stop preserves partial results; crash/restart displays the last observed state and
reconciles the native process before permitting retry. Duplicate events and requests must
not duplicate attempts, erase earlier visits or launch an unintended second run.

Acceptance covers an approved batch, normal Stop, crash, desktop restart, duplicate delivery,
retry and partial output. Missing producer truth remains unknown/interrupted, never inferred
completed. All owned workers close within the configured bound; owner processes are untouched.

### Candidate workspace, metrics, prop analysis and export

The selected candidate/revision/attempt survives dock, history, details and route navigation.
Keyboard and bulk actions operate on the visible explicit selection. At 1440px and 960px,
controls and large grids remain usable with bounded scrolling and no hidden required actions.
Show useful empty/error/recovery states without replacing the selected candidate with another.

Bind metrics to their exact archive/trade set, sample, direction, currency, initial capital,
date range and formula/producer authority. Compare native statistics and cockpit-recomputed
statistics only under matching scope; disclose mismatch instead of substituting a value.
Metric acceptance uses exact native exports and documented numeric tolerances, including
trade count, net profit, profit factor, drawdown and return/drawdown. Missing values, true
zeros, non-finite ratios and unavailable cross-checks remain distinct.

Data capture, native data import and task application are separate accepted transitions.
Preview and bind broker/instrument, engine, timeframe, dates, IS/OOS, precision, sessions,
costs and timestamp conventions before the relevant execution. A cross-market retest is an
explicit configuration choice; it cannot masquerade as parity with the source Candidate.

Prop analysis binds the same result/trades to a versioned challenge rule set and explicit
capital, sizing, costs, reset clock, daily/trailing drawdown, target and deadline assumptions.
Supported rule boundaries require reproducible checks; unsupported rules refuse a specific
challenge qualification claim. Existing plugin render/data delivery is not calibration.

Each supported MetaTrader, TradingView or Python export records source candidate/revision,
result/proof references, conversion/tool version, output hash, dependencies and limitations.
Successful download, target compilation and demonstrated behavioral parity are separate
outcomes. Unsupported constructs refuse or remain explicitly unverified. An export or an
LLM translation cannot inherit native validation merely because it cites a passing source.

Acceptance follows one real candidate from native input and approved settings through stage
history, validation, calibrated supported prop rules and supported target export, then
reopens that same chain. Native and cockpit policy results retain their distinct authorities.

### Construct / Idea

- capture idea/source/provenance;
- open existing native strategy/template when applicable;
- preserve revision identity;
- identify unresolved native requirements;
- ingest a URL or document as an immutable source revision (content hash + quoted spans the
  owner can see); Apollo may draft indicator vs strategy vs model meaning only from those spans;
- allow bounded language assistance without inventing trading meaning.

Text entry alone does not create candidate or run identity.

### Construct / Specification

Resolve the smallest complete set of native (or Models) requirements for one exact executable plan.

Native requirement families may include strategy shape, parts to improve, conditions/periods, exits/stops/targets, historical data/symbol/timeframe/date/IS-OOS/precision, trading/session/cost assumptions, building blocks/parameter ranges, ATM, sizing/money management, search/build mode, genetic options where selected, ranking/basic filters, cross-checks/filters, and notes/provenance.

Model requirement families may include estimator family, feature source, leakage-safe split
(purged / combinatorial purged language from the primary-literature notes), and bind target
Candidate. Unresolved leakage controls lock fit.

Each field/group has explicit state such as proven default, user selected, unresolved, unsupported, or not applicable. Missing required meaning locks Build. Unresolved fields are the only legal
source of Apollo clarifying questions (typed allowed answers, not free-form invention).

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
- Task `name` / `active` from `config.xml` when present;
- Setup engine, symbol, timeframe, dates, money-management attributes, and CrossChecks `use` flags when present in task XML;
- only explicitly proven typed fields such as selected databank names or GoToTask target label.

`GET /api/sqx-projects` lists real `user/projects/*/project.cfx` children (module folders such as Builder are omitted). Unreadable archives are `unresolved`, not invented rows.

Unknown canonical task kinds remain opaque. Read-only topology does not imply execution support. Task `settings` expose the actual Settings children of that task XML, including nested Ranking conditions and Cross-check Settings/Filtering subtrees when present. `POST /api/sqx-project-settings` `{project, task, updates:[{path,attribute,value}|{path,text}]}` is loopback-only and writes only existing attributes or existing text on existing elements. Exclusive Method `use` radios send one update per sibling (`true` on the selected path, `false` on the others). It does not add elements, attributes, Condition rows, or What-If scenarios. `POST /api/sqx-calibrate` `{project, task, apply}` is loopback-only; it posts the saved Data/Calibration fields to SQX `indyTester/calibrate` and, when `apply` is true, writes returned min/max/step onto existing Block/`#Level#` nodes only. `POST /api/sqx-project-control` `{project, action: run_project|stop_project|pause_project|resume_project}` is loopback-only. Start and stop call official SQX `project/start` (POST) and `project/stop` when the running local web is open; if that web is down only Start may use the trusted CLI fallback with supervisor registration. Stop must address the running instance through a verified owned-process control path; a second CLI process is not proof of stopping it. Pause and resume call SQX `project/pause` and `project/resume` on the running local web. `GET /api/sqx-project-progress?project=` streams producer log files and databank counts; generated/rejected/accepted/rate/percent come from the SQX engine WebSocket channel when it publishes them. The path fails closed without a verified runtime, matching launcher digest, saved project, or supervisor registration. Official SQX MCP documentation exists; this product has no SQX MCP adapter. Native integration uses the verified supported interfaces actually implemented.

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
- native runtime descriptor/readiness;
- provider/data/model/extension readiness;
- capability/add-on registry (`GET /api/capabilities`; packaged native SQX plugins; typed slots only; `POST` stage into verified SQX).

### Home/live

- market overview (quotes);
- market bar series (`GET /api/market/bars` — OHLC + timestamp + symbol + timeframe from a bar producer; never synthesized);
- Alpha Stack;
- pipeline/attention (`GET /api/research/next-action` names the one legal next Research action);
- signals;
- risk;
- scoped performance.

Quotes last/change are not a substitute for bars. These remain unavailable until the actual producers exist.

### Research

- native preset/configuration discovery;
- exact configuration/approval custody;
- source-ingest revisions (URL/document hash + quoted spans);
- clarifying-question set bound to unresolved Specification fields;
- native job control/readback;
- native output discovery/import;
- candidates;
- exact historical run/result reads;
- native validation/retest/optimization plan/results;
- proof/evidence;
- native project topology/control/readback;
- indicator / strategy / model identity and revision pointers.

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

- immutable evidence is never rewritten as current state; explicit candidate deletion removes unreferenced retained content under the preview/confirmation contract above;
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

One backend registry is authoritative (`GET /api/capabilities`, schema
`tc.capability-addon-registry.v1`). Packaged native StrategyQuant X plugins are the default
catalog. Empty operator add-on storage is still a ready registry, not an unimplemented
manifest, and does not mean zero plugins.

A descriptor includes stable capability identity/version, owning producer, availability, supported product placement, configuration/read/action schema versions, runtime install state, and typed presentation (title, job, opens-in, SQX controls). Plugin numeric settings are adjusted in StrategyQuant X Results after install.

Registered typed slots in this product are status-card placements on Explore, Automation,
and Settings. There is no navigation slot.

Rules:

- no arbitrary script/HTML injection;
- no competing frontend capability catalog;
- no add-on-created top-level navigation without architecture change;
- no replacement for Research core stages;
- unknown descriptor versions fail closed;
- operator add-ons cannot claim native SQX producer truth;
- add-ons cannot open a mutation contract other than the canonical loopback install (`stage`);
- Results-plugin settings stay in StrategyQuant X.

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


### Commercial release acceptance

A release gate applies to one exact signed Windows build. On a clean customer machine,
verify install, first-run setup with a separately licensed authorized native engine,
provider/data configuration, the complete supported candidate journey and durable reopen.
Upgrade, rollback, backup/export, crash recovery and deletion must preserve their documented
retention guarantees and leave no unexpected workers or exposed native services.

Verify the existing account/entitlement/provider authority before sale: concurrent and
repeated provisioning cannot duplicate grants or reset intended allowances; expiry,
exhaustion and revocation refuse before spend; the ceiling is provider-enforced rather
than only a local counter. Complete applicable engine/provider licensing, signing/update
trust, privacy, customer-readable errors and support/recovery instructions. A development
executable, fixture pass or operator-key session does not satisfy these customer gates.
