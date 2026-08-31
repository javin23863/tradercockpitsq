# TraderCockpit Native SQX Integration Plan

This is the binding implementation/acceptance map for `tradercockpitsq`. Read it with `AGENTS.md`, `docs/product-architecture-v1.md`, and **`docs/product-backbone-spec-v1.md`**.

`docs/product-backbone-spec-v1.md` is the binding detailed contract for the UI hierarchy, each core tab, backend/read-model ownership, native SQX gateway and configuration compiler, revision/staleness behavior, capability/add-on manifest, extension slots, migration map, and anti-drift tests. Implementation may not replace that structure with older route/workspace names.

## Product objective

Build one real desktop product around the StrategyQuant X 144.2953 backend:

```text
Idea / source
  → guided Construct plan
  → exact native SQX Builder configuration
  → native Builder generation + initial backtest
  → native .sqx candidates
  → Candidate Lab
  → native cross-check / Retester / Optimizer funnel
  → Backtest results
  → Proof
  → native Custom Project automation where desired
```

TraderCockpit owns the application experience and custody. SQX owns the producer algorithms.

### Fixed backbone navigation

Core research stages:

`Construct | Backtest | Proof`

Construct core tabs:

`Idea | Specification | Build | Candidates`

Backtest core tabs:

`Overview | Trades | Robustness | Configuration`

Explore, Automation, Operate and add-ons are auxiliary/capability-driven surfaces. Add-ons populate registered extension slots; they do not append arbitrary core-stage tabs.

## Global stop rule

**Do not expand unrelated backend capabilities until the Foundation Vertical below is green through the real application and real SQX runtime.**

Do not satisfy a missing native seam by adding a TraderCockpit-owned replacement producer.

## Authority required before implementation

For each stage inspect:

- [ ] matching section of `docs/product-backbone-spec-v1.md`;
- [ ] relevant original SQX screenshots;
- [ ] matching saved `.cfx`/task XML/preset/configuration;
- [ ] matching native runtime/output evidence;
- [ ] accepted TraderCockpit prototype mapping;
- [ ] current live branch/PR ownership.

The 35-shot UI set establishes the visible SQX configuration/workflow. Native configuration and execution establish producer truth. TraderCockpit prototype authority establishes presentation.

## Foundation Vertical — first release gate

### User story

A user starts from a simple indicator/trading concept and completes a real strategy-generation and validation path without seeing SQX’s full complexity.

### Required end-to-end proof

- [ ] Launch the actual TraderCockpit application/runtime.
- [ ] Verify exact SQX 144.2953 worker health before enabling compute.
- [ ] Create/open one simple indicator-based Idea from the UI.
- [ ] Apollo/Construct extracts stated rules and identifies only unresolved native SQX-required fields.
- [ ] User explicitly approves the structured Construct plan.
- [ ] Construct compiles to a complete exact native Builder configuration snapshot.
- [ ] The chosen first indicator/block is proved by retained native configuration evidence; it is not selected merely from a screenshot label.
- [ ] Persist the exact configuration bytes/identity that will execute.
- [ ] Start a bounded native Builder job.
- [ ] Display real native Builder progress/status.
- [ ] Produce at least one native `.sqx` survivor.
- [ ] Import that exact survivor idempotently into canonical TraderCockpit custody.
- [ ] Display the survivor in Candidate Lab with producer-backed result fields.
- [ ] Select the candidate and run one real downstream native validation/retest operation.
- [ ] Display its Backtest/validation/trade evidence.
- [ ] Display Proof linking Idea → Construct plan/config → SQX build/job → native candidate archive → result/validation evidence.
- [ ] Restart/reload and recover the same durable identities/artifacts.
- [ ] Negative proof: malformed/unavailable native configuration refuses visibly with no substitute strategy/result.
- [ ] Full product/browser acceptance green on exact head.
- [ ] Desktop launch/worker cleanup proof green where local environment permits it.

### Foundation browser path

The browser/product proof must traverse the actual backbone:

`Construct/Idea → Construct/Specification → approval → Construct/Build → native SQX Builder → Construct/Candidates → Backtest/Overview → Trades → Robustness → Configuration → Proof → restart/reopen`.

**Nothing counts as Foundation Complete until every applicable item above is executable.**

## Stage A — remove the wrong producer authority

- [ ] Identify all production imports/callers of `product/tradercockpit/builder/evolution.py`.
- [ ] Remove/quarantine it from production Builder execution.
- [ ] Do not merge PR #25’s `tradercockpit.builder-strategy.v1` producer/search engine.
- [ ] Salvage only PR #25 UI/custody/read-model code that still applies to native candidates/jobs.
- [ ] Do not merge PR #27’s TraderCockpit-owned robustness producer where native SQX owns the cross-check.
- [ ] Do not merge PR #28’s TraderCockpit-owned task/loop executor as Custom Project runtime.
- [ ] Preserve useful recovered semantics as tests/reference evidence when they improve adapter verification.
- [ ] Verify there is only one candidate identity/store, one application server authority and one run/result custody authority.
- [ ] Add producer-boundary regression checks specified in `docs/product-backbone-spec-v1.md`.

## Stage B — native runtime and desktop spine

- [x] Exact SQX build markers can be verified.
- [x] Native `sqcli.exe` Builder control exists on the PR #2 lineage.
- [x] Native Retester execution/custody exists on the PR #23 lineage.
- [ ] Consolidate runtime discovery/verification under one canonical native-worker adapter/gateway.
- [ ] Expose one runtime health/readiness contract to the UI.
- [ ] Distinguish unavailable runtime, invalid build, producer failure and malformed request.
- [ ] Ensure native process control is bounded and no worker is orphaned after application shutdown.
- [ ] Integrate the approved desktop shell once the canonical product server/native worker path is green.
- [ ] Keep frontend access behind the canonical TraderCockpit API; no direct SQX calls from browser code.

## Stage C — Ideas and deterministic Construct planning

TraderCockpit starts before Builder with an idea/source layer.

- [ ] Persist an immutable/revisioned Idea/source record independently of runs.
- [ ] Support plain-language concept, pasted text/file/source, existing strategy/template and catalog selection as registered capabilities permit.
- [ ] Build a deterministic gap planner from **native SQX configuration requirements**, not invented assistant questions.
- [ ] Separate resolved-source, proven-native-default, recommendation, ambiguity, unsupported and not-applicable states as specified in the backbone spec.
- [ ] Ask the user only when genuine ambiguity remains.
- [ ] No run/candidate identity exists before an executable Construct plan exists.
- [ ] Changing approved upstream semantics creates a new revision and makes current downstream eligibility stale without mutating historical evidence.

### Native Builder configuration coverage

Map the real SQX construction categories through the Specification tab and typed native-field registry:

- [ ] What to build / strategy type;
- [ ] Parts to improve when applicable;
- [ ] condition/period limits;
- [ ] stop loss / profit target behavior;
- [ ] data / symbol / timeframe / date ranges / IS-OOS / precision;
- [ ] trading/session/cost settings;
- [ ] building blocks / indicators / signals / weights / parameters;
- [ ] order and exit types;
- [ ] ATM where supported;
- [ ] money management/sizing;
- [ ] build mode;
- [ ] Genetic options when genetic evolution is chosen;
- [ ] ranking/basic filtering;
- [ ] cross-check selection/settings/filtering.

### Construct compiler acceptance

- [ ] Read one proven native project/task/config snapshot.
- [ ] Verify source artifact/build identity.
- [ ] Apply only registered typed user-approved changes.
- [ ] Frontend cannot supply arbitrary XML/XPath selectors.
- [ ] Preserve untouched native fields/semantics and record exact output bytes.
- [ ] Emit the exact executable native snapshot and content identity.
- [ ] Emit a human-readable diff/read model.
- [ ] Bind explicit approval to one exact plan/config revision.
- [ ] Refuse incomplete/unsupported required fields before producer launch.
- [ ] Reopen the same Construct plan and configuration after restart.
- [ ] Prove source/template mutation after compilation cannot alter the launched snapshot.

## Stage D — native Builder job

Builder is the producer; genetic evolution is one build mode.

- [ ] Start native Builder with the exact approved snapshot.
- [ ] Bind product job identity to exact config/SQX build/project identity.
- [ ] Read native progress without fabricating generation/result fields.
- [ ] Support bounded stop/cancel only through real native control.
- [ ] Detect new/updated native databank outputs deterministically.
- [ ] Import each valid native `.sqx` once.
- [ ] Preserve native archive/config/result provenance and exact artifact custody.
- [ ] Reopen job status and candidate set after product restart.
- [ ] Invalid/unavailable runtime fails closed with no fallback strategy generator.

## Stage E — Candidate Lab and native initial results

- [x] Canonical `StrategySpecV1` / `CandidateSpecV1` / content-addressed custody exists.
- [x] Native SQX archive import/custody exists on PR #23 lineage.
- [ ] Candidate Lab lists actual native Builder survivors from canonical custody.
- [ ] Candidate rows preserve exact native strategy/archive identity.
- [ ] Exact native artifact bytes are retained/reopenable, not represented only by loose hashes.
- [ ] Render only producer-backed metrics plus TraderCockpit custody metadata.
- [ ] Link candidate to originating Idea, Construct plan, config snapshot and Builder job.
- [ ] No `construction_fit` or TraderCockpit substitute fitness may masquerade as native Builder evaluation.
- [ ] Cross-idea/config/job candidate substitution fails closed.

## Stage F — Backtest and validation funnel

Backtest UI is fixed to `Overview | Trades | Robustness | Configuration`; individual validation methods are dynamic data inside those surfaces, not new tabs.

Cross-checks are a sequential strategy-testing funnel. They may run during Builder or when retesting existing strategies.

### Basic / fast native methods

- [ ] What If simulations;
- [ ] Monte Carlo trades manipulation;
- [ ] Higher backtest precision.

### Standard native methods

- [ ] Backtests on additional markets;
- [ ] Monte Carlo retest methods;
- [ ] Sequential Optimization.

### Extensive native methods

- [ ] Optimization Profile / System Parameter Permutation;
- [ ] Walk-Forward Optimization;
- [ ] Walk-Forward Matrix.

For every enabled method:

- [ ] exact native settings are preserved;
- [ ] filtering settings are preserved separately from method settings;
- [ ] native order/short-circuit behavior is respected;
- [ ] a failed check cannot be silently waived;
- [ ] nonexecuted later checks are shown as not executed/not evaluated, not passes;
- [ ] result fields are typed only after native meaning is proven;
- [ ] read/reopen survives restart.

### Backtest tab acceptance

- [ ] Overview consumes producer-backed summary/read model only.
- [ ] Trades uses actual native trade records and chart focus; no synthetic trades.
- [ ] Robustness renders backend/native profile methods dynamically.
- [ ] Configuration shows the immutable native executable configuration artifact; a hash alone cannot mark the tab ready.
- [ ] Compare is an action/split view, not a fifth permanent Backtest tab.

### Fast / Golden product profiles

- [ ] One backend `ValidationProfile` record defines the enabled native plan.
- [ ] `Fast` is a named inexpensive native-backed screening profile, not a fixed phase count.
- [ ] `Golden` is a named deeper native-backed validation/proof profile, not a fixed phase count.
- [ ] UI renders the backend plan dynamically.
- [ ] Add-on/user profiles use the same profile contract.

## Stage G — Retester and Optimizer

- [x] Native Retester task-1 evaluator exists on PR #23 lineage.
- [ ] Retester is invoked only for existing native strategies.
- [ ] Same-setting retest can bind the strategy’s exact source configuration where available.
- [ ] Deliberately changed settings are explicit and separately identified.
- [ ] Optimizer/Walk-Forward execution uses native SQX, not TraderCockpit optimization code.
- [ ] Results/trades bind back to exact candidate/config/producer identity.

## Stage H — Proof

Proof is one consolidated core stage with progressive sections rather than a second row of permanent feature tabs.

- [ ] Proof binds Idea/source revision identity.
- [ ] Proof binds approved Construct plan and exact native config snapshot.
- [ ] Proof binds SQX build/worker/job identity.
- [ ] Proof binds candidate `.sqx` archive identity and exact artifact custody.
- [ ] Proof binds data/market/execution settings actually used.
- [ ] Proof binds native result/trade artifacts.
- [ ] Proof binds validation profile/method outcomes.
- [ ] Proof distinguishes generated, tested, passed, promoted, exported and deployed states.
- [ ] Compare reads two exact compatible proof/result chains without inventing a winner.
- [ ] Delivery/export targets come from the backend capability manifest; they are not fixed tabs.

## Stage I — native Custom Projects / automation

Saved `.cfx` evidence already proves projects with Build, repeated Retest, ClearDatabanks, GoToTask, Optimize and other task types.

- [ ] Parse/import exact native project/task topology for presentation.
- [ ] Execute the native Custom Project through SQX.
- [ ] Observe real task/progress/databank state where native evidence supports it.
- [ ] Preserve source/target databank custody.
- [ ] Preserve native task order and loop behavior.
- [ ] Unknown native task kinds remain opaque until typed support exists.
- [ ] Simplify the Automation UI without recreating task execution in TraderCockpit.
- [ ] Restart/reopen the same automation state.

## Stage J — capability/add-on backbone

- [ ] Implement one backend `CapabilityManifestV1`/descriptor authority.
- [ ] Frontend and Apollo consume it rather than maintaining master capability arrays.
- [ ] Cover indicators/building blocks, data/providers, build modes, validation methods/profiles, delivery targets and installed add-ons.
- [ ] Implement typed stable extension slots from `docs/product-backbone-spec-v1.md`.
- [ ] Unknown renderer/capability versions fail closed/update-required.
- [ ] Backend descriptors cannot inject arbitrary frontend HTML/JavaScript.
- [ ] Add-ons do not rewrite the three core research stages.
- [ ] Indicator/chart previews require typed visual display descriptors; missing descriptors show `Preview not defined` rather than guessed visuals.

## Stage K — desktop product completion

- [ ] Approved TraderCockpit visual hierarchy is the production UI authority.
- [ ] Persistent high-level stages remain `Construct → Backtest → Proof`.
- [ ] Construct secondary tabs remain `Idea → Specification → Build → Candidates`.
- [ ] Backtest secondary tabs remain `Overview → Trades → Robustness → Configuration`.
- [ ] Ideas/source intake and Apollo are available before Build.
- [ ] Candidate Lab is native-data-backed.
- [ ] Real candlestick/chart workspace is primary when market context exists.
- [ ] Backtest tabs and robustness drilldowns use canonical reads.
- [ ] Explore is backend-capability-driven rather than a hard-coded feature wall.
- [ ] Automation uses native Custom Projects.
- [ ] All fixture/prototype values are removed from production paths.
- [ ] Desktop supervises application server and native worker cleanly.
- [ ] Clean-machine install/start/health/compute/stop/reopen acceptance passes.

## Anti-drift acceptance

Before the Foundation Vertical can be called complete, add and keep green the backbone guardrails specified in `docs/product-backbone-spec-v1.md`, including:

- [ ] exact three-stage navigation contract;
- [ ] exact four Backtest-tab contract;
- [ ] backend capability-manifest source of dynamic features;
- [ ] no production use of quarantined replacement producers;
- [ ] one server/store/candidate/result authority;
- [ ] exact approved-config-to-launch byte custody;
- [ ] no native-runtime fallback;
- [ ] cross-context identity substitution refusals;
- [ ] revision/staleness preservation;
- [ ] typed native-result truth;
- [ ] safe add-on renderer/version behavior;
- [ ] full browser/restart Foundation Vertical proof.

## Current branch/PR disposition

- PR #23 — **use native adapter/custody/readback pieces; Retester is downstream**.
- PR #2 — **use verified native Builder control direction**.
- PR #15 — **reuse native Custom Project topology custody; execution stays native**.
- PR #25 — **do not merge producer engine; salvage compatible UI/custody only**.
- PR #27 — **do not merge duplicate robustness producer**.
- PR #28 — **do not merge duplicate workflow executor**.
- PR #29 — retain only data/trading state that maps truthfully to native execution or is clearly product-only with a proven compiler.
- PR #30 — reassess Evidence UI as Proof-stage material.
- PR #31 — reassess Compare as a contextual action/split view.
- PR #32 — reassess exact overview/provenance read logic for Construct/Backtest.
- PR #33 — reassess persistent Apollo/autonomy guardrails for the new capability/gap-plan contracts.
- Isolated GA/robustness/workflow parity PRs — evidence/test donors only unless they contain non-producer application code needed by the native spine.

## Open-evidence discipline

The architecture is settled; native implementation details still requiring evidence are tracked in `docs/product-backbone-spec-v1.md`. Missing progress fields, XML selectors, metric semantics, advanced optimizer controls, Custom Project readback or add-on deployment details must remain constrained/unavailable until proved. They are not permission to alter the backbone or create replacement producers.

## Completion question

A slice is complete only when the answer is yes:

> Can a user perform the intended operation through the real TraderCockpit application and fixed product backbone, through the actual SQX producer that owns it, receive durable truthful native results, reload them, and see them through the intended TraderCockpit UI?

If not, continue the same vertical. Do not open a new feature lane to avoid the missing seam.