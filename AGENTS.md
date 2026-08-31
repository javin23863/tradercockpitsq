# TraderCockpit Agent Execution Policy

This file is repository-level policy for every LLM or implementation agent working in `javin23863/tradercockpitsq`.

## 1. Non-negotiable product direction

- `main` is the canonical production line.
- This is a **new platform**. Do not use StrategyQuant X / SQX as the platform name, product name, or user-facing workspace name.
- **Research** is the platform's historical strategy-research workspace.
- **StrategyQuant X 144.2953 is a native research producer/backend authority**, not the product identity. Use its name only where producer provenance, native configuration, runtime diagnostics, or technical integration details require it.
- **Home** is the live/current orientation screen and preserves: `Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`.
- Native SQX AI Wizard / AI Assistant + AlgoWizard are the primary native AI-assisted authoring path. Native Builder owns automatic candidate search/generation. Native SQX owns backtest, robustness/cross-check, optimization, Retester, and Custom Project execution where proven.
- Do not implement a second platform-owned Builder, GA, strategy-tree language, backtester, robustness engine, optimizer, or Custom Project executor when the native producer owns the operation.
- Recovered/source/reference trees are evidence and build-time research material. Production code must not import them as loose runtime dependencies.
- Do not invent Phase 0 / Phase 1 / `phase01_intake` product stages.
- Do not clone SQX's dense UI. Native backend ownership does not determine platform presentation or navigation.

Top-level desktop surfaces are:

`Home | Research | Explore | Automation | Operate | Settings`.

Inside **Research**, the stable workflow is:

- `Construct | Backtest | Proof`;
- Construct: `Idea | Specification | Build | Candidates`;
- Backtest: `Overview | Trades | Robustness | Configuration`.

Those are internal Research states, not top-level product workspaces.

## 2. Binding authority documents

Read these together before implementation:

1. `docs/product-architecture-v1.md` — producer ownership and lifecycle;
2. `docs/product-backbone-spec-v1.md` — application/API/add-on contract;
3. `docs/home-research-surface-authority-v1.md` — binding platform placement and naming for Home vs Research;
4. `IMPLEMENTATION_CHECKLIST.md` — execution order and acceptance gates;
5. `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/`sqx-lab` backend boundary;
6. `docs/consumer-openrouter-account-authority-v1.md` — consumer account, spend, and model-routing boundary;
7. `docs/repository-consolidation-v1.md` — cleanup and development-desktop delivery rule.

Where older navigation prose conflicts with `docs/home-research-surface-authority-v1.md`, the Home/Research authority wins. Issue #37 and the current consolidation document govern the cleanup checkpoint until it closes.

## 3. Strategy authoring hierarchy

Apollo is deferred. Do not import or merge a persistent Apollo product spine.

Use this hierarchy:

1. Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder — current native strategy authoring/generation authority.
2. Native SQX MCP (`ServletMCP`) — first-party integration/control surface. Retained build 144.2953 publishes `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, and `stop_project`; do not invent authoring methods behind it.
3. `sqx-lab` — optional external-LLM/custom-artifact extension, not universal intelligence.
4. The platform — orchestration, custody, approval, control, readback, presentation, accounts, and product UX.

A missing native transport seam is not permission to replace native quantitative authority with a platform-owned strategy engine.

## 4. Consumer Google/OpenRouter boundary

Approved account flow:

`consumer Google sign-in -> stable platform account -> configured starter/plan allowance -> bounded per-consumer OpenRouter spend authority -> backend-selected model`.

Rules:

- Google authenticates the consumer to the platform; it is not OpenRouter login.
- Operator/application provisioning credentials never enter browser code or consumer custody.
- Prefer provider-enforced per-consumer limits/reset/expiry plus internal usage/readback; a local counter is not the sole monetary ceiling.
- Starter-credit amounts, renewal cadence, and paid allowances are configuration. Do not invent commercial values.
- Current default workhorse policy is `z-ai/glm-5.3-flash`; model/provider/fallback policy is backend-configurable.
- OpenRouter may assist with intent, summaries, approved tools, and extensions; it does not take quantitative producer authority away from native research backends.

The earlier `javin23863/futures` repository remains quarantined as product/quant architecture. Only the explicitly approved consumer Google/OpenRouter account pattern may be inspected as design lineage.

## 5. Mandatory context before editing

Before changing a native-research-backed capability:

1. inspect relevant original runtime/screenshots;
2. inspect matching `.cfx`, task XML, presets/configuration, output archives, or runtime evidence;
3. inspect the accepted platform prototype mapping;
4. read the matching backbone section;
5. read `docs/home-research-surface-authority-v1.md` before changing global navigation, Home, Research placement, or live-vs-historical presentation;
6. for authoring, read `docs/sqx-authoring-authority-v1.md`;
7. for consumer LLM/account work, read `docs/consumer-openrouter-account-authority-v1.md`;
8. identify which native module owns the quantitative operation;
9. identify exactly what the platform must authenticate/configure/control/read/persist/present;
10. only then edit implementation files.

For live/current Home work, identify the actual market, signal, account, execution, risk, performance, or pipeline producer. Never substitute historical research data because it is convenient.

## 6. Producer boundary

When native producer behavior is not wired:

- inspect more native configuration/source/runtime evidence;
- expose unresolved/unavailable state if necessary;
- extend the adapter to the real producer;
- fail closed rather than substitute a new quantitative producer.

The platform may implement authentication, account/credit state, API routing, desktop supervision, configuration snapshots, process control, content-addressed custody, lifecycle state, read models, UI state, provenance, and proof.

The platform must not manufacture strategy generation, native fitness, historical backtest results, robustness outcomes, optimization outcomes, or native workflow semantics. Home likewise must not manufacture live/current market, signal, risk, execution, account, or performance truth.

## 7. Required product gates

Research lifecycle:

```text
Idea / source
  -> native authoring capability when needed
  -> exact native Construct configuration + approval
  -> native generation + initial evaluation
  -> Candidate Lab
  -> native Backtest / cross-check / Retester / Optimizer funnel
  -> Proof
```

First native proof:

`bounded idea -> native authoring/configuration -> approved native Builder run -> real native survivor -> Candidate Lab -> one downstream native validation/retest -> Backtest -> Proof -> restart/reopen same identities`.

That proof must be visible through the platform's **Research** workspace. The vendor/backend name is provenance, not the workspace title.

Consumer account/LLM proof:

`Google sign-in -> stable internal subject -> configured allowance -> bounded OpenRouter spend -> configured workhorse request -> usage attribution -> limit refusal -> sign-out/lapse/revocation cannot keep spending`.

Mocks, synthetic quantitative results, shared uncapped provider keys, browser-stored secrets, or local-only money ceilings do not satisfy the relevant gate.

## 8. Duplicate and donor work

- PR #23: retain native candidate/Retester/custody/readback pieces; Retester remains downstream.
- PR #15: retain native Custom Project topology custody; execution remains native.
- PR #25: do not merge its platform-owned Builder/search producer; salvage only valid UI/custody/application pieces.
- old `product/tradercockpit/builder/evolution.py`: removed/quarantined.
- PR #27: do not merge platform-owned robustness algorithms where the native producer owns the cross-check.
- PR #28: do not merge a platform-owned executor as a replacement for native Custom Projects.
- PR #33: Apollo is superseded/deferred.
- retained `ServletMCP`: use actual published tools only.
- `codex/sqx-lab-plugin`: optional custom native-artifact extension material.
- earlier app Google/OpenRouter work: account-infrastructure lineage only.
- algorithm-parity ingredient PRs: evidence/test donors, not production engine modules.

## 9. Concurrency

- Each concurrent lane gets its own branch/worktree.
- Never switch/reset/clean another lane's checkout.
- Treat unknown local changes as protected concurrent work.
- Re-check live PR heads before touching shared files.
- Acceptance runs on a clean checkout pinned to the exact tested commit.

## 10. Implementation behavior

- Prefer one end-to-end vertical over isolated fragments.
- Do not create a second server, store, candidate identity, run pipeline, account authority, credit authority, result authority, live-data authority, or duplicate UI spine to avoid integration conflict.
- Preserve exact native configuration, archive, and producer identities.
- Native producer errors must be structured and visible; never silently fall back to substitute logic.
- UI data comes from backend read models; do not hard-code producer state, result metrics, candidate IDs, validation truth, balances, live prices/signals/risk, or model pricing.
- Frontend code does not maintain master lists of native indicators/capabilities or model/provider policy.
- Validation profiles compile to inspectable native-backed plans rather than frontend constants.
- Add-ons contribute through registered typed extension slots.
- Documentation is not an implementation substitute.

## 11. Review and acceptance

For merge-intended work:

1. inspect actual diff/state;
2. compare with producer authority and binding docs;
3. fix concrete defects;
4. run focused acceptance;
5. run full product/browser/runtime/desktop acceptance on the exact head;
6. inspect substantive review findings for that exact head;
7. rerun after corrective commits.

Green unit tests alone do not establish product completion.

## 12. Binding acceptance questions

For historical research:

> Can a user perform the intended operation through the platform's Research workspace, through the canonical application/native gateway, through the actual producer that owns the operation, and receive durable truthful results back in the platform?

For Home/live capability work:

> Is the Home zone reading current state from the correct producer, with historical research kept explicitly scoped rather than substituted for live truth?

For authoring:

> Did the request remain on a native authoring/execution path rather than becoming a platform-only strategy representation?

For consumer LLM/account work:

> Is the request attributable to a stable consumer account, bounded by provider-enforced spend authority, routed by backend policy, and unable to continue spending after the relevant limit or entitlement ends?

If no, the capability is not product-complete.
