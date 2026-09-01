# Living Implementation Plan

This is the **single mutable implementation plan** for the repository.

The architecture and backbone define what the product is. This file defines what is being built now, what comes next, and what is complete. Do not create a second roadmap, checklist, recovery issue, donor plan, or competing implementation sequence.

## Canonical references

- `docs/product-architecture-v1.md` — product ownership and producer boundaries.
- `docs/product-backbone-spec-v1.md` — detailed application, UI, API, custody, security, and integration contract.
- `AGENTS.md` — coding/review discipline.
- `LIVING_IMPLEMENTATION_PLAN.md` — current implementation sequence and status.

## Active delivery gate

<!-- delivery-integrity
delivery-queue: workflow-correction-integrity-audit,research-proof,research-restart-reopen
installed-sqx-required-items: workflow-correction-integrity-audit
breadth-freeze: one-native-robustness-method-until-research-reopen
-->

The current product-completion lane is the **Research end-to-end vertical**. Work advances toward one usable desktop path rather than horizontally accumulating capabilities:

1. `workflow-correction-integrity-audit` — exercise the already-landed Builder → Candidate → Retester → Trades/Configuration path against the installed SQX runtime and remove any remaining invalid static-reference prerequisite.
2. `research-proof` — bind the exact Idea/configuration/job/Candidate/Historical Result/trades/validation identities into the real Proof surface.
3. `research-restart-reopen` — restart the desktop with the same data root and recover the exact complete Research chain.

`robustness-higher-precision` is no longer queued: producer-backed Higher Precision landed through PR #65 and is now part of the current `main` baseline. It remains the **only** native Robustness method authorized during the breadth freeze.

**Queue rule:** only the first item in `delivery-queue` is authorized for a new production PR. The PR that completes that item must remove it from the queue as part of recording actual completion; if that active item appears in `installed-sqx-required-items`, it must be removed there in the same transition. Use `installed-sqx-required-items: none` when no queued item requires a fresh installed-producer exercise, and use `delivery-queue: none` after the final queued item completes. The next item becomes authorized only after the preceding completion lands on `main`.

**Installed-producer rule:** plan items named in `installed-sqx-required-items` require a fresh exact-head installed-SQX acceptance receipt by default. In addition, any slice that changes a native integration/control/trust boundary must require the same exact-head producer acceptance even if its plan item normally would not. The current workflow-correction audit is explicitly native-required; Proof and restart/reopen do not acquire a fresh native-execution requirement merely by being later in the queue, but they cannot use that fact to bypass acceptance if they modify native integration code.

**Breadth freeze:** until `research-restart-reopen` is complete, do not add or merge a second Robustness method, optimization family, or adjacent Research capability. Additional SQX methods are reference material only until the first complete Research vertical works and reopens.

This section is parsed by repository delivery-integrity automation. Product PRs may update completion/status in this same plan, but a PR cannot authorize a new product lane for itself; authorization is read from the base `main` version of this file.

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
- one producer-backed native Backtest Robustness method, Higher Precision, using the installed Retester project as executable configuration authority, isolated exact-baseline staging, the common trusted Retester task-1 gateway, durable prepared/completed/failed/interrupted Proof custody, exact catalog/readback, and a canonical desktop surface; native process completion remains distinct from producer verdict and is recorded as `producer_result_captured_outcome_unread`;
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
- additional native Robustness methods beyond accepted Higher Precision during the breadth freeze;
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

The correction slice must preserve these audited outcomes: mutable Builder configuration validity never depends on an archived Git identity; Candidate association remains `operator_selected_exact_native_output` unless a deterministic producer seam is directly observed; Retester task 1 is verified from the installed source project while the installed engine hash is captured as provenance; and the strict Trades adapter remains only while no authoritative direct trade-row seam exists, with a real-producer acceptance fixture guarding its output.
- [x] Inspect/run the actual SQX downstream validation/Robustness workflow sufficiently to connect one real producer-backed native method on the canonical contracts without a preliminary evidence gate. Higher Precision is the accepted first method.
- [x] Backtest Robustness shows producer-backed native validation execution/custody state for Higher Precision only; TraderCockpit does not substitute a validation algorithm or reconstruct a producer outcome. Additional methods remain frozen until Proof + restart/reopen complete the vertical.
- [ ] Proof binds exact idea/config/runtime/job/artifact/result/validation identities and is visible in the canonical desktop.
- [ ] Restart/reopen resolves the same identities across the complete Research path using the same data root.
- [ ] **Breadth unlock:** only after the complete Research chain reopens may additional native Robustness methods be added, and they must reuse the common Robustness execution/custody lifecycle rather than clone it.

No platform-owned Builder, GA, historical backtester, robustness engine, optimizer, or workflow executor may substitute for the native producer.

### 3. Home live/current track

For each Home zone, connect the actual current/live producer through one backend read model with scope and freshness.

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

1. Start from current `main`.
2. Select the first incomplete applicable implementation item in this plan.
3. Confirm no active implementation branch owns the same product slice/files.
4. Keep the branch limited to that slice.
5. For native SQX work, inspect/run the authorized installed producer as part of the slice whenever it is available. Reuse existing screenshots/scenario observations; do not add a separate evidence gate for behavior that can be observed directly.
6. Use retained/decompiled source only where it clarifies non-observable details or hardens a directly observed integration. A retained reference blob is not a validity oracle for changing user project/configuration bytes.
7. Update this plan only when actual implementation status or sequence changes.
8. During prototype construction, an **intermediate** slice merge requires exact-head focused tests, Product Runtime Acceptance, relevant browser/desktop acceptance, the applicable real-producer exercise, and one substantive exact-head adversarial review. Codex may provide that review, but Codex closure is not a mandatory intermediate-slice gate.
9. Run the comprehensive adversarial **Codex review/closure** pass on the assembled end-of-plan prototype candidate. The repository label `final-prototype-review` activates that mandatory final closure gate.
10. Production implementation PRs target `main` directly and must contain the current `main` head in their ancestry. Stacked production PRs are prohibited; later slices are replayed from current `main` after their dependencies merge.
11. Delete/supersede implementation branches after merge; do not preserve parallel product branches as future authorities.

A feature is complete only when the real user path works in the one development desktop through canonical application/read-model/native-producer boundaries and durable truthful state returns to the correct product surface. When the authorized installed SQX runtime is available, the real producer exercise is part of implementation completion rather than a separate future “evidence” track.

## Current next work

**Finish the current Research vertical before adding breadth:** complete the installed-SQX workflow-correction integrity audit; then build the Proof surface over the exact existing identities, including the accepted Higher Precision validation custody; then prove full desktop restart/reopen on the same data root. Do not add System Parameter Permutation, Monte Carlo, Additional Markets, Walk-Forward, or another Research capability until those remaining vertical steps are complete. No preliminary retained-evidence gate is required when the installed SQX runtime can answer the integration question directly.**

Do not begin a separate feature roadmap. New work advances this file from top to bottom unless the architecture is explicitly changed first.
