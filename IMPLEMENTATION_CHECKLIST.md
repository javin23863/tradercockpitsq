# TraderCockpit Consolidated Implementation Checklist

## Status

Binding execution/acceptance checklist after repository recovery.

Read with:

- `AGENTS.md`;
- `docs/product-architecture-v1.md`;
- `docs/product-backbone-spec-v1.md`;
- `docs/home-research-surface-authority-v1.md`;
- `docs/sqx-authoring-authority-v1.md`;
- `docs/consumer-openrouter-account-authority-v1.md`;
- `docs/repository-consolidation-v1.md`.

The platform-facing name is **Research**. StrategyQuant X / SQX is a backend producer identity, not a top-level workspace name.

---

# 1. Product shape to protect

## Top-level desktop surfaces

- [ ] `Home`
- [ ] `Research`
- [ ] `Explore`
- [ ] `Automation`
- [ ] `Operate`
- [ ] `Settings`

## Home

Preserve exactly:

- [ ] Market Overview
- [ ] System Status
- [ ] Alpha Stack
- [ ] Pipeline Overview
- [ ] Signals
- [ ] Risk
- [ ] Performance
- [ ] Quick Actions

Rules:

- [ ] Historical research values never masquerade as live market/signal/risk/account/performance truth.
- [ ] Missing live producers render unavailable/pending state.
- [ ] Quick Actions only navigate to owning surfaces.

## Research

One top-level historical-research workspace. Inside it only:

`Construct | Backtest | Proof`

Construct:

`Idea | Specification | Build | Candidates`

Backtest:

`Overview | Trades | Robustness | Configuration`

- [ ] Canonical path is `/research`.
- [ ] `/strategyquant` redirects to `/research` for compatibility only.
- [ ] Research stage/tab state remains internal to Research.
- [ ] Old `/construct/*`, `/backtest/*`, `/proof` routes may redirect but are not navigation authority.
- [ ] No persistent Apollo dock or second assistant product spine.
- [ ] No permanent Optimizer, Monte Carlo, Walk-Forward, MCP, LLM, or add-on research tab.

---

# 2. Producer ownership

## Native SQX backend owns proven historical quantitative computation

Do not reproduce:

- [ ] native strategy authoring / AlgoWizard semantics;
- [ ] Builder strategy search/generation;
- [ ] GA selection/crossover/mutation/island mechanics;
- [ ] historical backtesting;
- [ ] native ranking/filter calculations;
- [ ] robustness/cross-check algorithms;
- [ ] Retester execution;
- [ ] Optimizer / Walk-Forward execution;
- [ ] Custom Project task/databank execution;
- [ ] native `.sqx` strategy/result semantics.

## Platform owns application/product mechanics

- [ ] desktop lifecycle;
- [ ] Google consumer identity/account state;
- [ ] bounded OpenRouter allowance/routing policy;
- [ ] live/current Home presentation from correct producers;
- [ ] idea/source revisioning;
- [ ] exact native configuration mapping, review, and approval;
- [ ] native runtime verification/process control;
- [ ] exact native artifact custody/product identities;
- [ ] Candidate Lab presentation;
- [ ] historical Backtest/Proof presentation inside Research;
- [ ] capability discovery/add-on registration;
- [ ] structured refusal when a producer is unavailable.

---

# 3. Repository consolidation gate — current priority

Feature expansion remains paused until complete.

## Canonical trunk

- [x] Architecture authority landed-equivalent on `main`.
- [x] Review-governance workflow landed-equivalent on `main`.
- [ ] GitHub repository default branch is `main`.
- [ ] Consolidation PR is green on exact head.
- [ ] Remaining open PRs are intentionally small/non-overlapping.

## Remove/guard architectural leakage

- [x] Remove duplicate platform-owned Builder/evolution engine and production test authority.
- [x] Reject runtime imports from `sources`, `references`, legacy `futures`.
- [x] Reject Phase01, copied Futures, persistent Apollo, and duplicate Builder markers.
- [ ] Re-audit full consolidated production tree before merge.

## Canonical application/runtime

- [ ] One application-server authority.
- [ ] One state-root/custody family.
- [ ] One native research gateway/runtime-verification family.
- [ ] One strategy/candidate/result/proof identity family.
- [ ] No second server, strategy engine, account authority, or UI product spine.

## Development desktop

- [x] Thin desktop host starts canonical local server.
- [x] Desktop consumes canonical `web/` UI.
- [x] Desktop lifecycle has headless start/stop/security tests.
- [ ] Packaged/manual Windows WebView2 launch verified.
- [ ] Closing desktop cannot orphan canonical local server/native worker.

## Browser acceptance

- [ ] `/home` is default.
- [ ] all eight Home zones are present.
- [ ] `/research` is separate from Home.
- [ ] Construct/Backtest/Proof are internal to `/research`.
- [ ] all internal tabs resolve.
- [ ] history/back/forward preserve Research state.
- [ ] `/strategyquant` and older research routes redirect to `/research`.
- [ ] top-level navigation says `Research`, not `StrategyQuant X`.
- [ ] Apollo does not appear.

---

# 4. Native research gateway/runtime integrity

Before native compute:

- [x] native build markers can be checked.
- [ ] one canonical runtime descriptor reports exact installed build/readiness.
- [ ] launcher identity verified by trusted SHA-256 before execution.
- [ ] configuration/project paths remain inside authorized runtime.
- [ ] symlink/junction escape refused.
- [ ] hash/build mismatch explicit and fail-closed.
- [ ] missing launcher/runtime/config explicit and fail-closed.
- [ ] browser never invokes native producer directly; control passes through canonical API.
- [ ] native process lifecycle bounded/observable.

Donors:

- PR #15: read-only Custom Project topology/path custody.
- PR #23: native candidate/Retester/readback plus corrected launcher trust boundary.

Integrate selectively; do not merge obsolete product assumptions.

---

# 5. Research Foundation Vertical

First historical-research release proof after consolidation.

Required desktop path:

`Research -> Construct/Idea -> Specification -> Build -> Candidates -> Backtest -> Proof`

## Idea/source

- [ ] Create/open one bounded idea from Research.
- [ ] Persist immutable/revisioned idea/source record and provenance.
- [ ] No candidate/run identity exists merely because text was entered.

## Native authoring

- [x] Native SQX AI-assisted authoring exists as a backend capability.
- [ ] Determine supported programmatic invocation seam, if any.
- [ ] MCP only uses its proven inspection/control tools.
- [ ] `sqx-lab` only for explicit custom-native-artifact cases.
- [ ] Missing direct native-AI seam never authorizes a platform substitute strategy engine.

## Specification / Construct planning

- [ ] Determine unresolved requirements from real native configuration requirements.
- [ ] Distinguish proven defaults, explicit choices, ambiguity, unsupported, and not-applicable state.
- [ ] User reviews/approves one exact construct revision.

Coverage where applicable includes strategy shape, conditions, exits/risk, historical data, costs/session, building blocks, sizing, search mode, genetic options, ranking/filters, and selected cross-checks.

## Exact configuration/custody

- [ ] Read one proven native project/task/config snapshot.
- [ ] Verify source artifact/build identity.
- [ ] Apply only registered approved changes.
- [ ] Preserve untouched native fields.
- [ ] Persist exact executable bytes/content identity.
- [ ] Show diff before launch.
- [ ] Bind approval to exact revision.
- [ ] Refuse incomplete/unsupported required fields.
- [ ] Source mutation after approval cannot alter launched bytes.

## Native Builder

- [ ] Start native Builder from exact approved snapshot.
- [ ] Bind product job to exact build/project/config identity.
- [ ] Show only observable progress.
- [ ] Stop/cancel through native control only.
- [ ] Detect real databank output.
- [ ] Import valid survivors idempotently.
- [ ] No fallback platform generator exists.

## Candidate Lab

- [ ] List real native survivors from canonical custody.
- [ ] Preserve exact archive bytes/native identity.
- [ ] Bind each candidate to Idea -> config -> native job -> native artifact.
- [ ] Show only producer-backed fields plus provenance.
- [ ] Cross-job/config substitution fails closed.

## Backtest / validation

- [ ] Run at least one real downstream native validation/retest operation.
- [ ] Overview shows producer-backed historical summary only.
- [ ] Trades uses actual native trade records.
- [ ] Robustness renders selected native methods dynamically.
- [ ] Configuration shows exact executed native configuration.
- [ ] Native failed checks cannot be waived by frontend.
- [ ] Not-executed checks remain not executed.
- [ ] Result/readback survives restart.

## Proof

- [ ] Bind idea/source revision.
- [ ] Bind approved plan/config bytes.
- [ ] Bind native producer build/worker/job identity.
- [ ] Bind native strategy archive.
- [ ] Bind historical data/settings actually used.
- [ ] Bind native result/trade artifacts.
- [ ] Bind validation identities/outcomes.
- [ ] Keep generated/tested/passed/promoted/exported/deployed states distinct.
- [ ] Restart/reopen resolves same chain.

Mocks/fixtures do not satisfy Foundation acceptance.

---

# 6. Home/live-product track

For each Home zone:

- [ ] identify authoritative live/current producer;
- [ ] define one backend read model;
- [ ] expose timestamp/scope where meaningful;
- [ ] distinguish unavailable, stale, and current;
- [ ] label historical research if summarized;
- [ ] never fabricate values.

Market Overview requires current market data. Signals require live strategy + market context. Risk requires execution/account/exposure state. Performance must explicitly scope live account, deployed strategy, or historical research. Quick Actions only route to owning surfaces.

---

# 7. Consumer account/OpenRouter gate

Rebuild only after consolidation lands.

Required proof:

`Google sign-in -> stable internal subject -> configured allowance -> provider-bounded OpenRouter spend -> configured workhorse -> usage attribution -> remaining allowance -> clean limit refusal -> no spend after lapse/revocation`

- [ ] minimum Google scopes and trusted backend verification;
- [ ] stable internal subject;
- [ ] duplicate starter grant prevented, including multi-writer admission;
- [ ] explicit grant-policy identity;
- [ ] operator provisioning credential never reaches browser/customer;
- [ ] provider limit/reset/expiry/revocation explicit;
- [ ] local display is not sole hard money boundary;
- [ ] usage attributed to stable subject;
- [ ] exhausted/revoked refuses before further spend;
- [ ] backend model policy separate from durable account history;
- [ ] current default is `z-ai/glm-5.3-flash` and is replaceable by backend config.

Commercial allowance values are configuration, not source guesses.

---

# 8. Automation / native projects

- [ ] import/read exact native project/task topology for presentation;
- [ ] resolved paths cannot escape verified runtime;
- [ ] task order/databank topology preserved;
- [ ] unknown native task kinds remain opaque until typed support exists;
- [ ] execution remains native;
- [ ] no replacement platform task-loop engine;
- [ ] restart/reopen same automation identity/state.

---

# 9. Capability/add-on backbone

- [ ] one backend capability-manifest/descriptor authority;
- [ ] frontend and LLM/tool surfaces consume it rather than separate master lists;
- [ ] typed stable extension slots only;
- [ ] unknown descriptor/renderer versions fail closed;
- [ ] backend descriptors cannot inject arbitrary frontend JS/HTML;
- [ ] add-ons do not rewrite top-level navigation or Research core stages;
- [ ] chart/indicator previews require typed display descriptors.

---

# 10. Definition of done

Before merge:

- [ ] exact head recorded;
- [ ] scope does not overlap another active branch;
- [ ] production-boundary checks pass;
- [ ] focused tests pass;
- [ ] full applicable Product Runtime Acceptance passes;
- [ ] browser acceptance passes when UI/routing is touched;
- [ ] desktop lifecycle/manual acceptance passes when desktop behavior is touched;
- [ ] user-facing behavior is visible/inspectable in the one development desktop;
- [ ] substantive exact-head review findings are resolved/dispositioned;
- [ ] corrective commits trigger a fresh exact-head cycle.

Product completion question:

> Can the user perform the intended operation in the one platform desktop, through the canonical application/read-model/native-producer boundary, and receive durable truthful state back in the correct product surface?

If no, the slice is not product-complete.
