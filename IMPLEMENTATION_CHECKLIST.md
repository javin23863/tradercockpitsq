# TraderCockpit Native SQX Integration Plan

This is the binding implementation/acceptance map for `tradercockpitsq`. Read it with:

- `AGENTS.md`;
- `docs/product-architecture-v1.md`;
- `docs/product-backbone-spec-v1.md`;
- `docs/sqx-authoring-authority-v1.md`;
- `docs/consumer-openrouter-account-authority-v1.md`.

## Product objective

Build one real consumer desktop product around the StrategyQuant X 144.2953 backend:

```text
consumer Google account + bounded LLM allowance
                |
                v
Idea / source
  → native SQX authoring capability when needed
  → guided Construct plan
  → exact approved native SQX Builder configuration
  → native Builder generation + initial backtest
  → real .sqx candidates
  → Candidate Lab
  → native cross-check / Retester / Optimizer funnel
  → Backtest
  → Proof
```

TraderCockpit owns account/auth, application experience, configuration/custody, approval, control, readback and presentation. SQX owns the strategy/quantitative producer algorithms.

## Fixed product backbone

Core research stages:

`Construct | Backtest | Proof`

Construct tabs:

`Idea | Specification | Build | Candidates`

Backtest tabs:

`Overview | Trades | Robustness | Configuration`

Explore, Automation, Operate, account/settings and add-ons are auxiliary/capability-driven surfaces. Do not append arbitrary permanent research tabs.

## Global stop rules

- Do not satisfy a missing native SQX seam by adding a TraderCockpit-owned replacement producer.
- Do not reintroduce Apollo as a persistent product spine.
- Do not promote `sqx-lab` into the universal strategy-authoring path; it is optional custom-artifact tooling.
- Do not use a shared uncapped OpenRouter key or a browser/local-only credit counter as the consumer spend authority.
- Do not invent starter-credit amounts, subscription allowances or commercial plan values in source code.
- Do not widen unrelated feature work ahead of the Foundation Vertical and the consumer account/LLM release gate.

## Authority required before implementation

For each SQX-backed stage inspect:

- [ ] matching section of `docs/product-backbone-spec-v1.md`;
- [ ] relevant original SQX screenshots;
- [ ] matching saved `.cfx`/task XML/preset/configuration;
- [ ] matching native runtime/output evidence;
- [ ] accepted TraderCockpit prototype mapping;
- [ ] current live branch/PR ownership.

For strategy authoring also inspect `docs/sqx-authoring-authority-v1.md`.

For Google/OpenRouter work also inspect `docs/consumer-openrouter-account-authority-v1.md` and the approved conceptual lineage from earlier TraderCockpit application work. Reuse the design concept only; do not copy personal credentials or customer records.

---

# Release Gate 1 — consumer account and bounded OpenRouter lane

This lane is application infrastructure, not a competing strategy engine.

## Account identity

- [ ] Consumer can start Google sign-in from TraderCockpit.
- [ ] Use minimum identity scopes required for sign-in; no unrelated Google-data access.
- [ ] Verify Google identity in the trusted backend/runtime.
- [ ] Map external identity to one stable internal TraderCockpit subject.
- [ ] Email remains presentation/support data, not the sole durable account key.
- [ ] First verified account can receive a configured starter entitlement/allowance.
- [ ] Repeated sign-in resolves the same account rather than creating duplicate credit grants.
- [ ] Sign-out clears local active-account/model-spend capability appropriately.

## OpenRouter spend authority

- [ ] Operator/application provisioning credential exists only in trusted operator/backend custody.
- [ ] Browser code never receives the provisioning/management credential.
- [ ] Provision or associate one bounded per-consumer OpenRouter spend authority.
- [ ] Provider-side spending limit is explicit.
- [ ] Reset policy is explicit when the product plan renews credits.
- [ ] Expiry is explicit when entitlement has a finite period.
- [ ] Disable/revoke path exists for lapse, account closure or abuse handling.
- [ ] Local account/credit ledger is for UX/reconciliation and is not the sole money ceiling.
- [ ] Usage/cost from OpenRouter is attributed to the stable consumer subject.
- [ ] Remaining allowance is backend-derived and visible to the UI.
- [ ] Spending refuses cleanly when the provider-enforced limit is reached.

## Model routing

- [ ] One backend model-policy record/configuration is authoritative.
- [ ] Current default model slug is `z-ai/glm-5.3-flash`.
- [ ] Model slug/provider preference/fallback policy is not hard-coded in browser code.
- [ ] Routine consumer requests start on the configured efficient workhorse.
- [ ] Escalation to another model requires backend policy/capability reason.
- [ ] Every request remains attributable to account + allowance + selected model.
- [ ] Provider/model changes require configuration/policy update, not frontend redesign.

## Consumer-lane acceptance

Required proof:

```text
Google sign-in
  → stable internal subject
  → configured starter/plan allowance
  → bounded OpenRouter spend authority
  → request through configured GLM 5.3 Flash default
  → usage/cost attributed to subject
  → remaining allowance updated
  → configured limit refuses further spend
  → sign-out/lapse/revocation cannot continue spending
```

- [ ] No developer personal OpenRouter key is used for acceptance.
- [ ] No shared uncapped consumer key is used.
- [ ] No model secret appears in browser storage, logs, fixtures or source.
- [ ] Negative tests cover duplicate-account credit grant, malformed identity, missing provider credential, exhausted allowance and revoked entitlement.

---

# Release Gate 2 — native SQX Foundation Vertical

## User story

A user starts from one bounded indicator/trading concept and completes a real strategy-generation and validation path without a TraderCockpit substitute producer.

## Required end-to-end proof

- [ ] Launch the actual TraderCockpit application/runtime.
- [ ] Verify exact SQX 144.2953 worker health before enabling native compute.
- [ ] Create/open one bounded Idea from the UI.
- [ ] Use native SQX authoring capability when the idea requires authoring; do not route the case through `sqx-lab` unless its custom-artifact capability is actually needed.
- [ ] Identify only unresolved native SQX-required fields.
- [ ] User explicitly approves the structured Construct plan.
- [ ] Construct compiles to a complete exact native Builder configuration snapshot.
- [ ] The first indicator/block mapping is proved by retained native configuration/install evidence.
- [ ] Persist the exact configuration bytes/identity that will execute.
- [ ] Start a bounded native Builder job.
- [ ] Display real native Builder progress/status only where observable.
- [ ] Produce at least one native `.sqx` survivor.
- [ ] Import that exact survivor idempotently into canonical TraderCockpit custody.
- [ ] Display the survivor in Candidate Lab with producer-backed result fields.
- [ ] Run one real downstream native validation/retest operation.
- [ ] Display Backtest/validation/trade evidence from the native result.
- [ ] Display Proof linking Idea → plan/config → SQX build/job → native candidate archive → result/validation evidence.
- [ ] Restart/reload and recover the same durable identities/artifacts.
- [ ] Malformed/unavailable native configuration refuses visibly with no substitute strategy/result.
- [ ] Full product/browser acceptance is green on the exact head.
- [ ] Desktop launch/worker cleanup proof is green where the local environment permits it.

Foundation browser path:

`Construct/Idea → Construct/Specification → approval → Construct/Build → native SQX Builder → Construct/Candidates → Backtest/Overview → Trades → Robustness → Configuration → Proof → restart/reopen`.

---

# Stage A — remove duplicate producer authority

- [ ] Identify all production callers of `product/tradercockpit/builder/evolution.py`.
- [ ] Remove/quarantine it from production Builder execution.
- [ ] Do not merge PR #25's TraderCockpit Builder/search producer.
- [ ] Salvage only compatible PR #25 UI/custody/read-model code.
- [ ] Do not merge PR #27's duplicate robustness producer where native SQX owns the cross-check.
- [ ] Do not merge PR #28's duplicate Custom Project/task-loop executor.
- [ ] Preserve useful recovered semantics as evidence/tests only where they improve adapter verification.
- [ ] Verify one canonical candidate identity/store, application server and result custody authority.
- [ ] Add producer-boundary regression tests.

# Stage B — canonical native runtime/gateway

- [x] Exact SQX build markers can be verified.
- [x] Native `sqcli.exe` Builder control exists on PR #2 lineage.
- [x] Native Retester execution/custody exists on PR #23 lineage.
- [x] Retained SQX MCP tool registry has been inspected; build 144.2953 exposes six project/strategy inspection/control tools, not authoring.
- [ ] Consolidate runtime discovery/verification under one canonical SQX gateway.
- [ ] Expose one runtime health/readiness contract to UI.
- [ ] Distinguish unavailable runtime, invalid build, producer failure and malformed request.
- [ ] Fold proven MCP tools into the gateway only where useful.
- [ ] Keep browser access behind the canonical TraderCockpit API; no direct SQX calls from browser code.
- [ ] Ensure native processes are bounded and no worker is orphaned after shutdown.

# Stage C — native AI/authoring invocation seam

- [x] Native SQX AI-assisted strategy authoring exists as product capability.
- [ ] Trace the supported programmable invocation seam, if any, for native SQX AI Wizard / AI Assistant.
- [ ] Distinguish UI/product existence from supported programmatic invocation.
- [ ] If no direct callable seam exists, expose only truthfully invokable native capabilities; do not replace native SQX AI authority with a new TraderCockpit strategy engine.
- [ ] Keep `sqx-lab` optional for explicit custom block/group/template/project authoring cases.
- [ ] Any `sqx-lab` artifact must be install-derived, validated and accepted by the real target SQX installation.
- [ ] External LLM assistance used by approved extensions runs through the bounded consumer OpenRouter lane, not a personal provider setup.

# Stage D — Ideas and deterministic Construct planning

- [ ] Persist immutable/revisioned Idea/source records independently of runs.
- [ ] Support plain-language concept, pasted source, existing strategy/template and catalog selection as registered capabilities permit.
- [ ] Build deterministic gap detection from native SQX configuration requirements, not invented assistant questions.
- [ ] Separate resolved-source, proven-native-default, recommendation, ambiguity, unsupported and not-applicable states.
- [ ] Ask the user only when genuine ambiguity remains.
- [ ] No run/candidate identity exists before an executable Construct plan exists.
- [ ] Upstream semantic edits create a new revision and stale downstream eligibility without mutating history.

## Native Builder configuration coverage

- [ ] What to build / strategy type;
- [ ] Parts to improve where applicable;
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

## Construct compiler acceptance

- [ ] Read one proven native project/task/config snapshot.
- [ ] Verify source artifact/build identity.
- [ ] Apply only registered typed user-approved changes.
- [ ] Frontend cannot supply arbitrary XML/XPath selectors.
- [ ] Preserve untouched native fields/semantics and exact output bytes.
- [ ] Emit exact executable snapshot + content identity + human-readable diff.
- [ ] Bind approval to one exact plan/config revision.
- [ ] Refuse incomplete/unsupported required fields before producer launch.
- [ ] Reopen the same plan/config after restart.
- [ ] Prove source/template mutation after compilation cannot alter the launched snapshot.

# Stage E — native Builder job

- [ ] Start native Builder with the exact approved snapshot.
- [ ] Bind product job identity to exact config/SQX build/project identity.
- [ ] Read native progress without fabricated generation/result fields.
- [ ] Support stop/cancel only through real native control.
- [ ] Detect native databank outputs deterministically.
- [ ] Import each valid native `.sqx` once.
- [ ] Preserve native archive/config/result provenance and exact artifact custody.
- [ ] Reopen job status/candidate set after restart.
- [ ] Invalid/unavailable runtime fails closed with no fallback generator.

# Stage F — Candidate Lab

- [x] Canonical strategy/candidate/content-addressed custody exists.
- [x] Native SQX archive import/custody exists on PR #23 lineage.
- [ ] List actual native Builder survivors from canonical custody.
- [ ] Preserve exact native strategy/archive identity.
- [ ] Retain reopenable artifact bytes, not hashes alone.
- [ ] Render only producer-backed metrics plus custody metadata.
- [ ] Link candidate to Idea, Construct plan, config snapshot and Builder job.
- [ ] Never present TraderCockpit substitute fitness as native Builder evaluation.
- [ ] Cross-idea/config/job candidate substitution fails closed.

# Stage G — Backtest and validation funnel

Backtest remains `Overview | Trades | Robustness | Configuration`; individual validation methods are dynamic data, not permanent tabs.

Native method families currently evidenced include:

- [ ] What If simulations;
- [ ] Monte Carlo trades manipulation;
- [ ] Higher backtest precision;
- [ ] Additional markets;
- [ ] Monte Carlo retest methods;
- [ ] Sequential Optimization;
- [ ] Optimization Profile / System Parameter Permutation;
- [ ] Walk-Forward Optimization;
- [ ] Walk-Forward Matrix.

For every method:

- [ ] exact native settings are preserved;
- [ ] filters are preserved separately from method settings;
- [ ] native order/short-circuit behavior is respected;
- [ ] failed checks cannot be silently waived;
- [ ] later nonexecuted checks are `not executed/not evaluated`, not passes;
- [ ] result fields are typed only after native meaning is proven;
- [ ] result/readback survives restart.

Backtest tabs:

- [ ] Overview consumes producer-backed summary/read model only.
- [ ] Trades uses actual native trade records; no synthetic trades.
- [ ] Robustness renders backend/native profile methods dynamically.
- [ ] Configuration shows immutable executable native configuration; a hash alone is insufficient.
- [ ] Compare is an action/split view, not a fifth permanent tab.

# Stage H — Retester and Optimizer

- [x] Native Retester evaluator exists on PR #23 lineage.
- [ ] Retester is invoked only for existing native strategies.
- [ ] Same-setting retest binds exact source configuration where available.
- [ ] Deliberately changed settings are explicit and separately identified.
- [ ] Optimizer/Walk-Forward uses native SQX, not TraderCockpit optimization code.
- [ ] Results/trades bind to exact candidate/config/producer identity.

# Stage I — Proof

- [ ] Bind Idea/source revision.
- [ ] Bind approved Construct plan and exact native config.
- [ ] Bind SQX build/worker/job identity.
- [ ] Bind candidate `.sqx` archive and exact artifact custody.
- [ ] Bind data/market/execution settings actually used.
- [ ] Bind native result/trade artifacts.
- [ ] Bind validation profile/method outcomes.
- [ ] Keep generated/tested/passed/promoted/exported/deployed states distinct.
- [ ] Compare reads exact compatible chains without inventing a winner.
- [ ] Delivery/export targets come from capability manifest rather than fixed tabs.

# Stage J — native Custom Projects / Automation

- [ ] Parse/import exact native project/task topology for presentation.
- [ ] Execute Custom Projects through SQX.
- [ ] Observe real task/progress/databank state only where supported.
- [ ] Preserve source/target databank custody.
- [ ] Preserve native task order and loop behavior.
- [ ] Unknown native task kinds remain opaque until typed support exists.
- [ ] Simplify UI without recreating execution in TraderCockpit.
- [ ] Restart/reopen same automation state.

# Stage K — capability/add-on backbone

- [ ] Implement one backend `CapabilityManifestV1`/descriptor authority.
- [ ] Frontend and bounded LLM/tool surfaces consume it rather than keeping independent master arrays.
- [ ] Cover indicators/building blocks, data/providers, build modes, validation profiles/methods, delivery targets and installed add-ons.
- [ ] Implement typed stable extension slots.
- [ ] Unknown renderer/capability versions fail closed/update-required.
- [ ] Backend descriptors cannot inject arbitrary frontend HTML/JavaScript.
- [ ] Add-ons do not rewrite the three core stages.
- [ ] Indicator/chart previews require typed display descriptors; missing descriptor means `Preview not defined`.

# Stage L — desktop product completion

- [ ] Approved TraderCockpit visual hierarchy is production UI authority.
- [ ] Core stage/tab contracts remain exact.
- [ ] Idea/source intake is available before Build.
- [ ] Consumer account/remaining-credit state comes from backend read models.
- [ ] No persistent Apollo dock is required by the backbone.
- [ ] Candidate Lab is native-data-backed.
- [ ] Real candlestick/chart workspace is primary when market context exists.
- [ ] Backtest/robustness views use canonical reads.
- [ ] Explore is backend-capability-driven.
- [ ] Automation uses native Custom Projects.
- [ ] Fixture/prototype values are removed from production paths.
- [ ] Desktop supervises application server/native worker cleanly.
- [ ] Clean-machine sign-in/start/health/compute/stop/reopen acceptance passes.

# Anti-drift acceptance

Before release keep green guards for:

- [ ] exact three-stage navigation;
- [ ] exact Construct/Backtest tab contracts;
- [ ] no persistent-Apollo requirement;
- [ ] backend capability manifest as feature authority;
- [ ] no production use of quarantined replacement producers;
- [ ] native SQX AI authority not replaced by `sqx-lab` or OpenRouter;
- [ ] one server/store/candidate/result authority;
- [ ] one consumer-account/credit authority;
- [ ] provider-enforced OpenRouter spend ceiling;
- [ ] backend-configured model routing;
- [ ] exact approved-config-to-launch byte custody;
- [ ] no native-runtime fallback;
- [ ] cross-context identity substitution refusals;
- [ ] revision/staleness preservation;
- [ ] typed native-result truth;
- [ ] safe add-on renderer/version behavior;
- [ ] browser/restart Foundation proof.

# Current branch/PR disposition

- PR #23 — use native adapter/custody/readback pieces; Retester is downstream.
- PR #2 — use verified native Builder control direction.
- PR #15 — reuse native Custom Project topology custody; execution stays native.
- PR #25 — do not merge producer engine; salvage compatible UI/custody only.
- PR #27 — do not merge duplicate robustness producer.
- PR #28 — do not merge duplicate workflow executor.
- PR #29 — retain only data/trading state that maps truthfully to native execution or clearly product-only state with a proven compiler.
- PR #30 — reassess Evidence UI as Proof-stage material.
- PR #31 — reassess Compare as contextual action/split view.
- PR #32 — reassess exact overview/provenance read logic for Construct/Backtest.
- PR #33 — closed/deferred; do not revive persistent Apollo. Reuse only narrow generic refusal/safety concepts if useful.
- retained `ServletMCP` — use actual published tools only.
- `codex/sqx-lab-plugin` — optional custom native-artifact extension material.
- earlier TraderCockpit/Futures Google/OpenRouter work — approved conceptual lineage for the consumer account/LLM lane only.
- isolated GA/robustness/workflow parity PRs — evidence/test donors only unless they contain non-producer application code needed by the native spine.

# Completion question

A slice is complete only when the relevant answer is yes:

> Can a user perform the operation through the real TraderCockpit application and fixed product backbone, through the actual SQX producer that owns quantitative work, receive durable truthful results, reload them, and see them through the intended UI?

For consumer LLM/account slices:

> Can a consumer authenticate with Google, receive only the configured allowance, spend through a provider-enforced bounded OpenRouter lane, see attributable usage, and be unable to continue spending after limit/lapse/revocation?

If not, continue the same vertical. Do not open a new feature lane to avoid the missing seam.
