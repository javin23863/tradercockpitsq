# TraderCockpit Consolidated Implementation Checklist

## Status

This is the binding execution/acceptance checklist for `tradercockpitsq` after the repository-recovery reset.

Read with:

- `AGENTS.md`;
- `docs/product-architecture-v1.md`;
- `docs/product-backbone-spec-v1.md`;
- `docs/home-strategyquant-surface-authority-v1.md`;
- `docs/sqx-authoring-authority-v1.md`;
- `docs/consumer-openrouter-account-authority-v1.md`;
- `docs/repository-consolidation-v1.md`.

Where older navigation wording conflicts with `docs/home-strategyquant-surface-authority-v1.md`, the Home/StrategyQuant X clarification wins.

---

# 1. Product shape to protect

## Top-level desktop surfaces

- [ ] `Home`
- [ ] `StrategyQuant X`
- [ ] `Explore`
- [ ] `Automation`
- [ ] `Operate`
- [ ] `Settings`

## Cockpit Home

Home is the current/live orientation screen. Preserve exactly these eight zones:

- [ ] Market Overview
- [ ] System Status
- [ ] Alpha Stack
- [ ] Pipeline Overview
- [ ] Signals
- [ ] Risk
- [ ] Performance
- [ ] Quick Actions

Rules:

- [ ] Historical SQX values are never silently presented as live market/signal/risk/account/performance truth.
- [ ] Missing live producers render explicit unavailable/pending state.
- [ ] Quick Actions navigate to owning surfaces; they do not create hidden workflows.

## StrategyQuant X screen

StrategyQuant X is one top-level historical-research surface. Inside that screen only:

`Construct | Backtest | Proof`

Construct:

`Idea | Specification | Build | Candidates`

Backtest:

`Overview | Trades | Robustness | Configuration`

- [ ] Canonical screen path is `/strategyquant`.
- [ ] Research stage/tab state remains internal to that screen.
- [ ] Old `/construct/*`, `/backtest/*`, `/proof` routes may redirect for compatibility but are not navigation authority.
- [ ] No persistent Apollo dock or second assistant product spine.
- [ ] No permanent Optimizer, Monte Carlo, Walk-Forward, MCP, LLM, or add-on research tab.

---

# 2. Producer ownership

## StrategyQuant X owns historical strategy/research computation

Do not reproduce these in TraderCockpit:

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

## TraderCockpit owns application/product mechanics

- [ ] desktop lifecycle;
- [ ] Google consumer identity/account state;
- [ ] bounded OpenRouter allowance/routing policy;
- [ ] live/current Home presentation from correct producers;
- [ ] idea/source revisioning;
- [ ] exact native configuration mapping, review and approval;
- [ ] native runtime verification and process control;
- [ ] exact native artifact custody and product identities;
- [ ] Candidate Lab presentation;
- [ ] historical Backtest/Proof presentation;
- [ ] capability discovery/add-on registration;
- [ ] structured refusal when a producer is unavailable.

---

# 3. Repository consolidation gate — current priority

Feature expansion remains paused until this gate is complete.

## Canonical trunk

- [x] Architecture authority landed-equivalent on `main`.
- [x] Review-governance workflow landed-equivalent on `main`.
- [ ] GitHub repository default branch is `main`.
- [ ] Consolidation PR is green on its exact head.
- [ ] Remaining open PRs are intentionally small and non-overlapping.

## Remove/guard architectural leakage

- [x] Remove production `tradercockpit.builder` / duplicate evolution engine.
- [x] Remove its production test authority.
- [x] Reject runtime imports from `sources`, `references`, legacy `futures`.
- [x] Reject Phase01 product markers.
- [x] Reject copied Futures repository markers.
- [x] Reject persistent Apollo product-spine markers.
- [x] Reject reintroduction of duplicate TraderCockpit Builder package/schema.
- [ ] Re-audit full consolidated production tree before merge.

## Canonical application/runtime

- [ ] One `ThreadingHTTPServer` application authority.
- [ ] One state-root/custody family.
- [ ] One native-SQX gateway/runtime-verification family.
- [ ] One strategy/candidate/result/proof identity family.
- [ ] No second application server, strategy engine, account authority or UI product spine.

## Development desktop

- [x] Thin desktop host starts the canonical local server.
- [x] Desktop window consumes the canonical `web/` UI.
- [x] Desktop lifecycle has headless server/start/stop tests.
- [ ] Packaged/manual Windows desktop launch is verified on an environment with WebView2.
- [ ] Closing desktop cannot orphan the canonical local server/native worker.

## Consolidated browser acceptance

- [ ] `/home` is the default product route.
- [ ] all eight Home zones are present.
- [ ] `/strategyquant` is separate from Home.
- [ ] Construct/Backtest/Proof are internal to `/strategyquant`.
- [ ] all Construct/Backtest internal tabs resolve.
- [ ] history/back/forward preserve SQX internal state.
- [ ] legacy research routes redirect into `/strategyquant`.
- [ ] Apollo does not appear.

---

# 4. Native SQX gateway and runtime integrity

Before enabling any native compute:

- [x] SQX build markers can be checked.
- [ ] one canonical runtime descriptor reports exact installed build and readiness.
- [ ] launcher executable identity is verified by trusted SHA-256 before execution.
- [ ] configuration/project paths are resolved and proven to remain inside the authorized SQX runtime.
- [ ] symlink/junction escape is refused.
- [ ] runtime hash/build mismatch is explicit and fail-closed.
- [ ] missing launcher/runtime/config is explicit and fail-closed.
- [ ] browser never invokes SQX directly; all control passes through canonical TraderCockpit API.
- [ ] native process lifecycle is bounded and observable.

Vetted donor material:

- PR #15: read-only Custom Project topology/path custody.
- PR #23: native candidate/Retester/readback plus corrected launcher trust boundary.

Integrate donor files selectively; do not merge obsolete shared product assumptions with them.

---

# 5. StrategyQuant X Foundation Vertical

This is the first historical-research release proof after consolidation.

Required desktop path:

`StrategyQuant X → Construct/Idea → Specification → Build → Candidates → Backtest → Proof`

## Idea/source

- [ ] Create/open one bounded idea from the StrategyQuant X screen.
- [ ] Persist immutable/revisioned Idea/source record.
- [ ] Preserve source/provenance.
- [ ] No candidate/run identity exists merely because text was entered.

## Native authoring

- [x] Native SQX AI-assisted authoring exists as a product capability.
- [ ] Determine the supported programmatic invocation seam, if any.
- [ ] Use MCP only for its proven six inspection/control tools.
- [ ] `sqx-lab` is used only for explicit custom native-artifact cases.
- [ ] Missing direct native-AI seam never authorizes a TraderCockpit substitute strategy engine.

## Specification / Construct planning

- [ ] Determine unresolved requirements from real native SQX configuration requirements.
- [ ] Distinguish proven defaults, explicit choices, ambiguity, unsupported and not-applicable state.
- [ ] User reviews/approves one exact construct revision.

Native configuration coverage where applicable:

- [ ] What to build / strategy shape;
- [ ] Parts to improve;
- [ ] entry/condition/period constraints;
- [ ] exits, stop loss, profit target;
- [ ] historical data/symbol/timeframe/date/IS-OOS/precision;
- [ ] trading/session/cost settings;
- [ ] building blocks/indicators/signals and parameter ranges;
- [ ] ATM;
- [ ] money management/sizing;
- [ ] search/build mode;
- [ ] Genetic options when native genetic Builder mode is selected;
- [ ] ranking/basic filters;
- [ ] selected cross-checks and their filters.

## Exact configuration compiler/custody

- [ ] Read one proven native project/task/config snapshot.
- [ ] Verify source artifact/build identity.
- [ ] Apply only registered typed approved changes.
- [ ] Preserve untouched native fields.
- [ ] Persist exact executable bytes and content identity.
- [ ] Show human-readable diff before launch.
- [ ] Bind approval to exact configuration revision.
- [ ] Refuse incomplete/unsupported required fields.
- [ ] Source/template mutation after approval cannot change launched bytes.

## Native Builder

- [ ] Start native Builder using the exact approved snapshot.
- [ ] Bind product job to exact SQX build/project/config identity.
- [ ] Show only real observable progress.
- [ ] Stop/cancel only through native control.
- [ ] Detect actual native Builder databank output.
- [ ] Import each valid `.sqx` survivor idempotently.
- [ ] No fallback TraderCockpit generator exists.

## Candidate Lab

- [ ] List real native survivors from canonical custody.
- [ ] Preserve exact archive bytes and native identity.
- [ ] Bind each candidate to Idea → config → native job → `.sqx` artifact.
- [ ] Show only producer-backed fields plus custody/provenance.
- [ ] Cross-job/config substitution fails closed.

## Backtest / validation

- [ ] Run at least one real downstream native validation/retest operation.
- [ ] Overview shows producer-backed historical summary only.
- [ ] Trades uses actual native trade records.
- [ ] Robustness renders the selected native methods dynamically.
- [ ] Configuration shows exact executed native configuration.
- [ ] Native failed checks cannot be waived by the frontend.
- [ ] Not-executed checks remain not executed, not passes.
- [ ] Result/readback survives restart.

## Proof

- [ ] Bind Idea/source revision.
- [ ] Bind approved Construct plan/config bytes.
- [ ] Bind SQX build/worker/job identity.
- [ ] Bind native `.sqx` archive.
- [ ] Bind historical data/settings actually used.
- [ ] Bind native result/trade artifacts.
- [ ] Bind validation method/outcome identities.
- [ ] Keep generated/tested/passed/promoted/exported/deployed states distinct.
- [ ] Restart/reopen and resolve the same chain.

Foundation acceptance succeeds only when the real desktop path is executable; mocks/fixtures do not satisfy it.

---

# 6. Home/live-product track

This track is separate from the historical SQX Foundation Vertical.

For each Home zone:

- [ ] identify its authoritative live/current producer;
- [ ] define one backend read model;
- [ ] expose explicit timestamp/scope where meaningful;
- [ ] distinguish unavailable, stale and current state;
- [ ] ensure historical SQX data is labeled if summarized;
- [ ] never fabricate values to fill the dashboard.

## Market Overview

- [ ] current market-data producer identified;
- [ ] symbol/timeframe/session/source timestamped;
- [ ] stale feed visible as stale.

## System Status

- [ ] TraderCockpit server health;
- [ ] native SQX worker/runtime health;
- [ ] relevant provider/add-on status;
- [ ] actionable alerts without inferred success.

## Alpha Stack

- [ ] current strategy/candidate/deployed identities from canonical custody/execution;
- [ ] historical candidate state clearly separated from live deployment state.

## Pipeline Overview

- [ ] current research/validation/deployment attention from canonical lifecycle state;
- [ ] no invented generic phase model.

## Signals

- [ ] live strategy + live market context required;
- [ ] historical backtest entries are not live signals.

## Risk

- [ ] current execution/account/exposure producer required;
- [ ] historical drawdown is not current account risk.

## Performance

- [ ] scope explicitly says live account, deployed strategy, or historical research;
- [ ] mixed scopes are never silently combined.

## Quick Actions

- [ ] route into owning product surface;
- [ ] no hidden local workflow/producer.

---

# 7. Consumer account and bounded OpenRouter release gate

Rebuild this lane only after repository consolidation lands. Do not revive stale PR #36 in place.

Required proof:

`Google sign-in → stable internal subject → configured allowance → provider-bounded OpenRouter spend → configured workhorse → usage attribution → remaining allowance → clean limit refusal → no spend after lapse/revocation`

## Identity/account

- [ ] minimum Google sign-in scopes;
- [ ] trusted backend verification;
- [ ] one stable internal subject;
- [ ] repeated sign-in cannot create duplicate starter grant;
- [ ] first-grant admission is safe across multiple writer processes;
- [ ] grant-policy identity is explicit and durable;
- [ ] sign-out clears local active capability appropriately.

## Spend authority

- [ ] operator provisioning credential never reaches browser/customer;
- [ ] per-consumer provider limit is explicit;
- [ ] reset/expiry policy is explicit when applicable;
- [ ] revoke/disable path exists;
- [ ] local credit display is not sole hard money boundary;
- [ ] usage/cost is attributed to stable subject;
- [ ] exhausted/revoked state refuses before further spend.

## Model policy

- [ ] backend model policy is separate from durable account identity/history;
- [ ] current default policy is `z-ai/glm-5.3-flash`;
- [ ] provider/model/fallback policy is not hard-coded in browser;
- [ ] model changes do not require rewriting account state.

Commercial allowance values remain configuration, not source-code guesses.

---

# 8. Automation / native Custom Projects

- [ ] import/read exact native project/task topology for presentation;
- [ ] resolved project path cannot escape verified SQX runtime;
- [ ] task order/databank topology is preserved;
- [ ] unknown native task kinds remain opaque until typed support exists;
- [ ] execution remains native SQX;
- [ ] TraderCockpit does not create a replacement task-loop engine;
- [ ] restart/reopen same automation identity/state.

---

# 9. Capability/add-on backbone

- [ ] one backend capability-manifest/descriptor authority;
- [ ] frontend and LLM/tool surfaces consume it rather than separate master lists;
- [ ] typed stable extension slots only;
- [ ] unknown descriptor/renderer versions fail closed;
- [ ] backend descriptors cannot inject arbitrary frontend JavaScript/HTML;
- [ ] add-ons do not rewrite top-level app navigation or internal SQX core research stages;
- [ ] chart/indicator previews require typed display descriptors.

---

# 10. Definition of done for every future slice

A slice is not complete because unit tests pass.

Before merge:

- [ ] exact current head recorded;
- [ ] scope does not overlap another active branch;
- [ ] production-boundary checks pass;
- [ ] focused tests pass;
- [ ] full applicable Product Runtime Acceptance passes;
- [ ] browser acceptance passes when UI/routing is touched;
- [ ] desktop lifecycle/manual acceptance passes when desktop behavior is touched;
- [ ] user-facing behavior is visible/inspectable in the development desktop;
- [ ] substantive review findings on the exact head are resolved or explicitly dispositioned;
- [ ] corrective commits trigger a fresh exact-head acceptance/review cycle.

Product completion question:

> Can the user perform the intended operation in the one TraderCockpit desktop, through the canonical application/read-model/native-producer boundary, and receive durable truthful state back in the correct product surface?

If no, the slice is not product-complete.
