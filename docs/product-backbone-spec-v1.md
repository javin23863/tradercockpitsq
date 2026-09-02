# Product Backbone Specification v1

This document is the detailed implementation contract beneath `docs/product-architecture-v1.md`.

Current sequencing/status lives only in `LIVING_IMPLEMENTATION_PLAN.md`.

## 1. Global desktop frame

Top-level navigation is exactly:

`Home | Research | Explore | Automation | Operate | Settings`

Rules:

- `/home` is the default route;
- `/research` is the canonical historical-research route;
- no top-level Construct/Backtest/Proof items;
- the persistent Apollo assistant surface is canonical (bounded LLM under the account/model contract); it is not a product/result authority and never mutates native state directly;
- no frontend-owned master list of providers/models/native capabilities;
- no fabricated runtime, market, account, candidate, result, or deployment identity in global chrome.

The top bar may show only backend-owned current context, application/runtime status, account/allowance state, and actionable alerts.

## 2. Home contract

Home answers: **what matters now and where should the user go next?**

Home is the multicolor Cockpit Home authority (`references/ui-authority`, `cockpit-home`). It
presents eight semantic zones (below), elaborated with Engine & System Status, System Alerts,
Resource Usage, and Signal Feed as shown in the authority, plus the persistent Apollo assistant.
The zones are the accepted content; the visual execution must match the authority.

### Market Overview

Current market context only. Read model may include instrument, timeframe/context, source/provider, session/market state, producer timestamp/freshness, and typed producer-owned descriptors.

Missing/stale feed is explicit. Historical research bars/results are not a live substitute.

The market ticker and Market Overview watchlist are served by the live-quotes read model
`GET /api/market/quotes` (`tc.market-quotes.v1`). Watchlist symbols are operator configuration
(`TRADERCOCKPIT_WATCHLIST`), never hard-coded in the frontend. Quote values (`last`,
`change_percent`, `observed_at`, `currency`) exist only for symbols a connected provider resolves;
every other symbol is carried as an explicit `unavailable` placeholder. With no provider the record
is `status:"unavailable"`, `reason_code:"provider_not_configured"` and lists the configured symbols
as placeholders. A provider read failure fails closed to `reason_code:"provider_read_failed"` with
no partial values.

Live-market provider seam: implement `tradercockpit.market_data.MarketDataProvider.fetch_quotes`
against any real feed (broker, vendor, or websocket poller) and pass it to the server. This is the
single hookup point for live data; the record itself is secret-free and self-describes the hookup
via `provider_hookup`. TraderCockpit never invents symbols, prices, or timestamps.

### System Status

May include application server, desktop lifecycle, native research runtime, data/provider, model/account, and registered extension health.

Each component has its own readiness; one healthy component never implies another is healthy.

### Alpha Stack

Shows canonical research/promotion/deployment identities only. Historical candidate, promoted research strategy, exported strategy, and deployed/live strategy remain distinct.

### Pipeline Overview

Derived from canonical lifecycle/read models. Pending/not-run/failed/pass remain distinct. Do not invent generic numbered phases.

### Signals

Requires current market context plus a live strategy/deployment signal producer. Historical entries/exits are not live signals.

### Risk

Requires current account/execution/exposure producer. Historical drawdown or robustness output is not current account risk.

### Performance

Every metric declares scope such as live account, deployed strategy, paper/simulated operation, or historical research. Scopes are never silently merged.

### Quick Actions

Navigation into owning surfaces only. No hidden local workflow or producer.

### Home state vocabulary

At minimum: `current`, `stale`, `pending`, `unavailable`, `error` where applicable.

## 3. Research contract

Research answers: **what historical strategy is being constructed/tested, what did the native producer execute, and what evidence supports the result?**

Accepted workflow:

`Idea → Construct → Build → Candidates → Backtest → Robustness → Proof → Delivery / Simulation`

Internal stages:

`Construct | Backtest | Proof | Delivery/Simulation`

Construct tabs:

`Idea | Specification | Build | Candidates`

Construct exposes distinct problem-solving modalities feeding the same downstream custody:
Random Discovery and Genetic/Evolutionary search (both native SQX), and Machine Learning /
Models (platform-owned; standard ML libraries producing signals/features/models, never a
substitute for native Builder/GA/backtest/robustness). The Indicators & Models catalog is the
capability-discovery surface for both technical indicators and model families.

Backtest tabs:

`Overview | Trades | Robustness | Configuration`

Delivery/Simulation covers prop-firm/paper simulation and post-Proof hand-off; it never
converts historical evidence into live account/execution truth.

Route/query state may select only registered states. Arbitrary query text never creates new product states or durable identities.

### Construct / Idea

- capture idea/source/provenance;
- open existing native strategy/template when applicable;
- preserve revision identity;
- identify unresolved native requirements;
- allow bounded language assistance without inventing trading meaning.

Text entry alone does not create candidate or run identity.

### Construct / Specification

Resolve the smallest complete set of native requirements for one exact executable plan.

Native requirement families may include strategy shape, parts to improve, conditions/periods, exits/stops/targets, historical data/symbol/timeframe/date/IS-OOS/precision, trading/session/cost assumptions, building blocks/parameter ranges, ATM, sizing/money management, search/build mode, genetic options where selected, ranking/basic filters, cross-checks/filters, and notes/provenance.

Each field/group has explicit state such as proven default, user selected, unresolved, unsupported, or not applicable. Missing required native meaning locks Build.

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
- only explicitly proven typed fields such as selected databank names or GoToTask target label.

Unknown canonical task kinds remain opaque. Read-only topology does not imply execution support.

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
- provider/data/model/extension readiness.

### Home/live

- market overview;
- Alpha Stack;
- pipeline/attention;
- signals;
- risk;
- scoped performance.

These remain unavailable until the actual producers exist.

### Research

- native preset/configuration discovery;
- exact configuration/approval custody;
- native job control/readback;
- native output discovery/import;
- candidates;
- exact historical run/result reads;
- native validation/retest/optimization plan/results;
- proof/evidence;
- native project topology/control/readback.

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

- immutable evidence is never rewritten as current state;
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

One backend registry is authoritative.

A descriptor includes stable capability identity/version, owning producer, availability, supported product placement, configuration/read/action schema versions, and optional typed presentation descriptors.

Rules:

- no arbitrary script/HTML injection;
- no competing frontend capability catalog;
- no add-on-created top-level navigation without architecture change;
- no replacement for Research core stages;
- unknown descriptor versions fail closed.

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
