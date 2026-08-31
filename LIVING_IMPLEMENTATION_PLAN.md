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
- exact native Builder configuration compilation/review/approval custody with retained SQX producer-evidence validation and fail-closed Specification gating;
- read-only native runtime/build/launcher trust descriptor; descriptor-time launcher verification is never per-request launch authorization;
- one trusted native Builder control path that stages only exact approved XML bytes and performs the source-proven `loadconfig -> start` sequence with fresh runtime/launcher/config verification before every subprocess;
- durable native-job custody created before native control side effects, with immutable submitted/failed control receipts and idempotent retry behavior;
- read-only native runtime/preset inspection;
- read-only native Builder project configuration custody;
- read-only native Builder output archive inspection plus exact bounded archive snapshot capture for Candidate import;
- read-only native Custom Project topology custody;
- physical path containment for native build markers, launcher, preset/project/output/control configuration reads;
- production-boundary checks rejecting reference/Futures/Phase01/Apollo/duplicate-Builder and legacy generic execution abstractions.

The baseline intentionally does **not** contain:

- a platform strategy schema;
- a generic backtest evaluator/run engine;
- Retester execution yet;
- producer-backed historical result/trade/robustness custody yet;
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
- [x] Desktop packaging/manual Windows WebView2 launch is verified.
- [x] Closing the desktop cannot orphan the local server or any long-lived worker registered with the desktop lifecycle owner.
- [x] Completion policy requires every future user-facing feature to be visible or inspectable through this same desktop application; feature slices that do not satisfy that rule are not complete.

### 2. Research end-to-end vertical — CURRENT

Required real desktop path:

`Research -> Construct/Idea -> Specification -> Build -> Candidates -> Backtest -> Proof`

- [x] Persist immutable/revisioned Idea/source custody.
- [x] Resolve actual native configuration requirements without inventing producer semantics.
- [x] Compile, review, and approve one exact native configuration snapshot.
- [x] Implement the exact-approval-gated native Builder launch path through the trusted native gateway, including durable native-job custody/readback.
- [ ] Exercise that launch path against the actual installed SQX runtime and observe one real Builder submission/output; CI/package acceptance proves the product path but does not substitute for installed-SQX producer evidence.
- [ ] Import real native survivor(s) into newly defined Candidate Lab custody with exact archive/provenance identity.
- [ ] Implement native Retester candidate/run/readback on the new canonical contracts, including trusted launcher verification before process execution.
- [ ] Execute one real downstream native validation/retest.
- [ ] Backtest Overview/Trades/Robustness/Configuration show producer-backed historical state only.
- [ ] Proof binds exact idea/config/runtime/job/artifact/result/validation identities.
- [ ] Restart/reopen resolves the same identities.

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
- [ ] Preserve unknown native task kinds opaquely until semantics are evidenced.
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
5. Update this plan only when actual implementation status or sequence changes.
6. During prototype construction, merge only after exact-head focused tests, Product Runtime Acceptance, and relevant browser/desktop acceptance are clean. Do not block each intermediate implementation slice on Codex review.
7. Run the comprehensive adversarial Codex review/closure pass on the assembled end-of-plan prototype candidate, then fix findings before declaring the prototype complete.
8. Delete/supersede implementation branches after merge; do not preserve parallel product branches as future authorities.

A feature is complete only when the real user path works in the one development desktop through canonical application/read-model/native-producer boundaries and durable truthful state returns to the correct product surface. Where CI cannot contain the actual licensed/installed producer, implementation completion and installed-producer execution evidence must be stated separately rather than conflated.

## Current next work

**Research end-to-end vertical: finish Candidate Lab custody/import on the exact native Results archive contract, then continue directly into native Retester control/readback and producer-backed Backtest/Proof surfaces. Exercise the actual installed SQX Builder path as soon as producer access is available; do not invent output/job semantics while that runtime evidence is pending.**

Do not begin a separate feature roadmap. New work advances this file from top to bottom unless the architecture is explicitly changed first.
