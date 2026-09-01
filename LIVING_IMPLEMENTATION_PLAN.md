# Living Implementation Plan

This is the **single mutable implementation plan** for the repository.

The architecture and backbone define what the product is. This file defines what is being built now, what comes next, and what is complete. Do not create a second roadmap, checklist, recovery issue, donor plan, or competing implementation sequence.

## Canonical references

- `docs/product-architecture-v1.md` — product ownership and producer boundaries.
- `docs/product-backbone-spec-v1.md` — detailed application, UI, API, custody, security, and integration contract.
- `AGENTS.md` — coding/review discipline.
- `LIVING_IMPLEMENTATION_PLAN.md` — current implementation sequence and status.

## Product shape

Top-level desktop surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

Home preserves:

`Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`

Research contains:

- `Construct | Backtest | Proof`
- Construct: `Idea | Specification | Build | Candidates`
- Backtest: `Overview | Trades | Robustness | Configuration`

StrategyQuant X / SQX is a native historical-research backend producer identity when provenance, runtime, or configuration details require it. It is not the platform name and not a user-facing workspace label.

## Native integration authority

The authorized installed SQX 144.2953 program is the primary executable specification whenever it is available.

For native integration work:

- inspect and run the real program first when behavior is directly observable;
- inspect the actual saved project/configuration/result artifacts produced by those runs;
- reuse user-supplied screenshots and previously captured real-runtime scenarios as valid supporting observations;
- use retained/decompiled source and archived reference material to clarify non-observable details, not as a prerequisite gate before implementation;
- do not create a separate evidence checkpoint when the same question can be answered by exercising the installed producer;
- do not require a current native project/configuration to equal one retained Git blob before TraderCockpit can accept it as producer-valid;
- keep runtime trust, artifact custody, and producer validity as separate code/test authorities; identity records what ran and does not by itself establish validity;
- exercise the installed producer inside the implementation/acceptance loop rather than implementing from static evidence and postponing the real run.

The prohibition on invented TraderCockpit quantitative behavior remains absolute. Removing evidence gates means connecting the actual producer sooner, not recreating it.

## Current clean baseline

The repository intentionally contains only the product foundation and end-to-end Research mechanics already proven safe:

- one Python application server;
- one `web/` product UI;
- one desktop host around that same server/UI;
- one packaged Windows desktop path that bundles the canonical `web/` tree and forces WebView2/EdgeChromium;
- one desktop lifecycle owner that seals shutdown, stops the loopback server, and terminates every explicitly registered long-lived worker with bounded escalation;
- one canonical runtime/status read model consumed by Home/System Status;
- typed namespaced research identities plus content-addressed immutable custody/CAS primitives bound to one canonical per-user application data root;
- immutable/revisioned Research Idea/source custody exposed through the canonical loopback-only application API and the Construct/Idea desktop surface;
- exact native Builder configuration compilation/review/approval custody, with immutable source/executable identity and fail-closed structural/runtime validation;
- read-only native runtime/build/launcher trust descriptor; descriptor-time launcher verification is never per-request launch authorization;
- one trusted native Builder control path that stages only exact approved XML bytes and performs the source-proven `loadconfig -> start` sequence with fresh runtime/launcher/config verification before every subprocess;
- durable native-job custody created before native control side effects, with immutable submitted/failed control receipts and idempotent retry behavior;
- exact native Builder Results archive capture plus immutable Candidate/archive/strategy/settings custody, explicit job/archive association provenance, loopback API, and Construct/Candidates desktop readback;
- one trusted native Retester task-1 control path using isolated TraderCockpit-generated native projects, installed-project task-1 structure, fresh launcher verification, exact `SQTradingLib.jar` execution provenance, exact Candidate evidence staging, durable historical-result lifecycle/readback, and Backtest Overview presentation;
- Backtest Trades reads the exact SQX `orders.bin` producer record only from an immutable completed Historical Result revision, preserves native Portfolio filled/non-control selection semantics, and renders native rows without synthetic trade reconstruction;
- Backtest Configuration reconstructs one explicitly selected completed Retester chain from canonical reads and renders only after exact approved-configuration -> submitted Builder-job -> Candidate -> historical-result revision/hash bindings all agree;
- one producer-backed native Backtest Robustness method, Higher Precision, using the installed Retester project as executable configuration authority, isolated exact-baseline staging, the trusted Retester task-1 gateway, durable prepared/completed/failed/interrupted Proof custody, exact catalog/readback, and the canonical desktop Robustness surface;
- read-only native runtime/preset inspection;
- read-only native Builder project configuration custody;
- read-only native Custom Project topology custody;
- physical path containment for native build markers, launcher, preset/project/output/control configuration reads and Retester project/result capture;
- production-boundary checks rejecting reference/Futures/Phase01/Apollo/duplicate-Builder and legacy generic execution abstractions.

The baseline intentionally does **not** contain:

- a platform strategy schema;
- a generic backtest evaluator/run engine;
- fabricated producer metrics/trades/validation truth;
- a TraderCockpit-reconstructed robustness verdict or quantitative validation algorithm;
- complete UI coverage of all currently exposed Research capabilities yet;
- consumer account/model-spend state yet.

Those capabilities are implemented from the clean contracts below rather than inherited from removed legacy abstractions.

## Current implementation sequence

### 1. Application foundation — COMPLETE

The existing desktop shell is now the real development application used for every subsequent feature.

- [x] One canonical runtime/status read model reports application, native research backend, data/provider, account/model, and extension readiness without fabricated state.
- [x] Home/System Status consumes that read model.
- [x] Define the new product custody/identity foundation required for Idea/configuration/job/candidate/result/proof without recreating a generic strategy/backtest engine.
- [x] Native runtime descriptor includes exact installed build/readiness and trusted launcher identity before execution can be enabled.
- [x] Implement one trusted native control gateway; native POST/mutation remains disabled until this is complete.
- [x] Desktop packaging/manual Windows WebView2 launch is verified. Ordinary packaged startup must display the TraderCockpit `/home` shell and must not launch StrategyQuant X; an EXE-launch/close receipt that does not prove which UI became visible is not sufficient. The Windows install identity is `%LOCALAPPDATA%\Programs\TraderCockpitSQ` plus Start Menu `TraderCockpitSQ.lnk`; the canonical data root is `%LOCALAPPDATA%\TraderCockpitSQ`, never tradercockpit-app's `%LOCALAPPDATA%\TraderCockpit` or `launch-tradercockpit.cmd`.
- [x] Closing the desktop cannot orphan the local server or any long-lived worker registered with the desktop lifecycle owner.
- [x] Completion policy requires every future user-facing feature to be visible or inspectable through this same desktop application; feature slices that do not satisfy that rule are not complete.

### 2. Research end-to-end vertical — CURRENT

Required real desktop path:

`Research -> Construct/Idea -> Specification -> Build -> Candidates -> Backtest -> Proof`

#### Research UI completeness contract

The already-landed custody/execution chain is the Research **spine**, not the definition of a complete Research product. Research remains incomplete until the practical TraderCockpit interface covers the supported Research capability surface that the backend and installed native producer expose.

The backend/native capability inventory is the source of truth for UI completeness. For every supported user-editable Research capability exposed through the canonical backend/native read models, TraderCockpit must provide a discoverable, usable desktop control or inspector in the correct Research surface. A supported backend capability may not remain silently orphaned because the current UI has no control for it.

Coverage includes, where exposed by the installed producer and canonical backend:

- all supported strategy/build modes, including Random Discovery and Genetic Evolution as distinct workflows rather than one generic search form;
- all supported indicator, signal, raw-indicator, operator/comparison, price/candle, time-condition, order-action, exit, custom-block, and other native rule/block families;
- every supported parameter representation required by those blocks and settings, including booleans, enums, scalar numeric/text values, ranges, fixed values, selected-value lists, weighted values, parameter sets, nested structures, dependent/conditional settings, and chart/symbol/timeframe bindings;
- Genetic Evolution controls exposed by the producer, including population/generation settings, crossover, mutation, islands, migration, starting population, initial-population filtering/decimation, diversity, duplicate handling, fresh blood, weakest replacement, cadence, restart/stagnation behavior, and final-generation behavior where available;
- Random Discovery controls exposed by the producer without leaking irrelevant Genetic controls into that mode;
- data, historical-range, precision, spread/slippage/commission/session and other native backtest/input settings that are user-editable through the supported seam;
- trading/risk, money-management, ATM, ranking/fitness, databank, automatic-dismissal, acceptance-filter, scope, and selection settings exposed by the producer;
- producer-backed validation/Robustness families and their method-specific parameters as they become available through the supported seam, rather than stopping permanently at one representative method;
- native Custom Project topology, task kinds, task parameters, dependencies, controls, and readback that are exposed by the backend, while keeping native execution native and not recreating a TraderCockpit task-loop engine;
- all applicable Build, Candidate, Backtest, Proof, reopen, navigation, selection, inspection, and execution controls required to operate those capabilities from the TraderCockpit desktop.

The UI should make this depth easier to use than the native producer window hierarchy. Reuse the already-discussed Research interaction model: decision-oriented grouping, contextual/conditional controls, searchable rule-space taxonomy, clear Random-vs-Genetic separation, practical parameter editors, staged validation, and progressive Simple/Detailed/Native disclosure where useful. Do not flatten native depth into a handful of generic knobs, and do not expose every backend field as an undifferentiated raw form.

Research UI completeness requires an explicit coverage check: enumerate the canonical backend/native Research capability manifest or equivalent read-model surface and compare it with the controls/inspectors rendered by the desktop. Every supported user-facing capability must be mapped, intentionally hidden as non-user-facing, or explicitly unavailable with a truthful reason. Unknown/new producer capability must fail visible rather than silently disappearing.

Research is not considered visually complete from browser fixtures alone. The integrated TraderCockpit desktop must be runnable and inspectable with the real Research surfaces so the user can review the actual interaction model before the lane is closed.

- [x] Persist immutable/revisioned Idea/source custody.
- [x] Resolve native configuration requirements sufficiently to drive the current exact native Builder path.
- [x] Compile, review, and approve one exact native configuration snapshot.
- [x] Implement the exact-approval-gated native Builder launch path through the trusted native gateway, including durable native-job custody/readback.
- [x] Implement Candidate Lab exact native Results archive capture/import/custody/readback without inventing a producer-side job→archive identifier.
- [x] Implement native Retester Candidate/run/result readback on the new canonical contracts, including exact installed-engine execution provenance and fresh trusted-launcher verification before process execution.
- [x] Backtest Overview presents exact Candidate/Retester historical-result custody without fabricated metrics.
- [x] Backtest Trades reads and displays native Portfolio filled/non-control trade records from the immutable completed Retester result archive, with strict format/revision/evidence binding.
- [x] Backtest Configuration reconstructs and displays the immutable executed chain `approved configuration -> submitted Builder job -> Candidate archive -> completed Retester result`, with exact revision/evidence SHA cross-checking and fail-closed substitution handling.
- [ ] **Workflow-correction integrity audit:** re-evaluate the already-landed Specification/Build/Candidate/Retester/Trades path against the actual installed SQX runtime and existing real-runtime scenario observations. Remove or replace any static-reference-only prerequisite that can reject valid producer state, and identify any unnecessary producer-format reconstruction that should instead use a direct native seam.

The correction work must preserve these audited outcomes: mutable Builder configuration validity never depends on an archived Git identity; Candidate association remains `operator_selected_exact_native_output` unless a deterministic producer seam is directly observed; Retester task 1 is verified from the installed source project while the installed engine hash is captured as provenance; and the strict Trades adapter remains only while no authoritative direct trade-row seam exists, with a real-producer acceptance fixture guarding its output.
- [x] Inspect/run the actual SQX downstream validation/Robustness workflow sufficiently to connect one real producer-backed native method on the canonical contracts. Higher Precision is the accepted first method.
- [x] Backtest Robustness shows producer-backed Higher Precision execution/custody state only; no TraderCockpit validation algorithm or reconstructed outcome substitutes for the native producer.
- [x] Proof binds exact idea/config/runtime/job/artifact/result/validation identities and is visible in the canonical desktop.
- [x] Restart/reopen resolves the same identities across the complete Research path using the same data root.
- [x] Inventory the full supported Research capability surface currently exposed by backend/native read models and record an explicit UI mapping for each user-facing capability. The versioned coverage inventory currently maps all 12 user-operable workflow/readback capabilities exposed by canonical read models; this does not claim future producer depth is already exposed.
- [ ] Complete practical desktop UI coverage for all supported Research blocks, indicators, parameter types, search modes, selection settings, validation methods, Custom Project controls, and relevant execution/readback controls.
- [ ] Prove Research UI coverage against the backend/native capability inventory so no supported user-facing capability is silently absent.
- [ ] Run the actual integrated TraderCockpit desktop and review the complete Research interaction model before declaring this lane complete.

The existing Proof/restart chain does not by itself close Research. Additional producer-backed Research depth should be connected through the same native-authority boundaries as it is exposed and verified. No platform-owned Builder, GA, historical backtester, robustness engine, optimizer, or workflow executor may substitute for the native producer.

### 3. Home live/current track

For each Home zone, connect the actual current/live producer through one backend read model with scope and freshness.

This section is **not globally blocked by Research**. Independent non-overlapping lanes may work on Home or other top-level surfaces. The current Research lane, however, stays focused on Research until its own completion criteria above are met.

- [ ] Market Overview — live/current market-data authority.
- [ ] System Status — complete application/native/provider health presentation from canonical backend truth.
- [ ] Alpha Stack — canonical research/promotion/deployment identities.
- [ ] Pipeline Overview — current lifecycle/attention state without invented generic phases.
- [ ] Signals — live strategy plus live market context only.
- [ ] Risk — current account/execution/exposure authority.
- [ ] Performance — explicit live account/deployed strategy/historical scope.
- [ ] Quick Actions — navigation only; no hidden producer or workflow.

Historical research values never masquerade as live/current truth.

### 4. Consumer account and bounded model access

Required real path:

`Google sign-in -> stable internal account -> configured allowance -> provider-bounded OpenRouter spend -> backend-selected model -> usage attribution -> clean limit refusal -> no spend after lapse/revocation`

- [ ] Google subject binding is stable.
- [ ] Starter grant is idempotent across multiple writer processes.
- [ ] Grant-policy identity is explicit and durable.
- [ ] Operator provisioning credentials never reach browser/consumer custody.
- [ ] Provider hard limit/reset/expiry/revocation is the monetary boundary; local display is not the sole limit.
- [ ] Account/allowance read model is separate from model policy.
- [ ] Current default workhorse `z-ai/glm-5.3-flash` remains replaceable through backend configuration.
- [ ] Exhausted/revoked/lapsed state refuses before further spend.

Commercial allowance values remain configuration rather than source-code guesses.

### 5. Automation

- [ ] Expose the existing read-only native project topology through the canonical application status/read-model family.
- [ ] Present it in Automation.
- [ ] Resolve task behavior from the actual producer when observable; preserve only genuinely non-observable task details opaquely until source-level inspection resolves them.
- [ ] Keep execution native; do not build a platform task-loop executor.
- [ ] Add native control/readback only through the trusted native gateway.

### 6. Capability/add-on backbone

- [ ] One backend capability manifest/descriptor authority.
- [ ] Frontend and language/tool surfaces consume the same authority.
- [ ] Typed stable extension slots only.
- [ ] Unknown descriptor/renderer versions fail closed.
- [ ] No arbitrary backend-supplied JavaScript/HTML injection.
- [ ] Add-ons cannot rewrite top-level navigation or Research core stages.

## Working rule for every change

1. Start a product-completion lane from current `main`.
2. Within that lane, select the first incomplete applicable implementation item in this plan. Independent top-level lanes may proceed concurrently when they do not own the same product files or contracts.
3. Confirm no other active lane owns the same product files.
4. Keep sequential work for the same user-visible vertical on one long-lived branch; use tested internal commits as checkpoints instead of opening a PR for every small slice.
5. For native SQX work, inspect/run the authorized installed producer as part of the work whenever it is available. Reuse existing screenshots/scenario observations; do not add a separate evidence gate for behavior that can be observed directly.
6. Use retained/decompiled source only where it clarifies non-observable details or hardens a directly observed integration. A retained reference blob is not a validity oracle for changing user project/configuration bytes.
7. Update this plan only when actual implementation status or sequence changes.
8. Run focused tests continuously and full Product Runtime Acceptance at meaningful integration checkpoints; do not stop delivery to wait for a PR review after each internal checkpoint.
9. Open one integration PR for the completed user-visible vertical. Before merge, require exact-head Product Runtime Acceptance, relevant browser/desktop acceptance, applicable real-producer exercise, and one substantive adversarial review. The reviewer/tool is external to TraderCockpit and is not a repository or product dependency.
10. Delete/supersede the integration branch after merge; do not preserve parallel product branches as future authorities.

A feature is complete only when the real user path works in the one development desktop through canonical application/read-model/native-producer boundaries and durable truthful state returns to the correct product surface. When the authorized installed SQX runtime is available, the real producer exercise is part of implementation completion rather than a separate future “evidence” track.

## Current next work

**Current Research lane:** expand the canonical backend/native read models and practical Research UI beyond the currently mapped surface, starting with producer-backed Random Discovery vs Genetic Evolution depth, rule/block families, parameter representations, selection/ranking, additional validation/Robustness methods, and deeper Custom Project controls/readback as those seams are directly observed. Keep the versioned coverage inventory synchronized as each producer capability becomes exposed; do not mark the coverage-proof or integrated-desktop-review gates complete merely because today’s 12 read-model capabilities are mapped.

This Research focus does not block independent non-overlapping Home or other top-level lanes. Do not create a second Research roadmap; this file remains the single mutable implementation authority.