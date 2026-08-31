# TraderCockpit Agent Execution Policy

This file is repository-level policy for every LLM or implementation agent working in `javin23863/tradercockpitsq`.

## 1. Non-negotiable product direction

- `main` is the canonical TraderCockpit production line.
- **StrategyQuant X 144.2953 is the strategy-research producer/backend authority.** TraderCockpit is the desktop/application/account/configuration/custody/control/readback/presentation layer around that backend.
- **StrategyQuant X is one dedicated historical-research screen inside TraderCockpit. It is not the Home screen and it is not the source of unrelated live market/account/signal/execution/risk/performance truth.**
- **Cockpit Home is the live/current orientation screen** and preserves the accepted eight zones: `Market Overview | System Status | Alpha Stack | Pipeline Overview | Signals | Risk | Performance | Quick Actions`.
- Native SQX AI Wizard / AI Assistant + AlgoWizard are the primary native AI-assisted strategy-authoring path. Native Builder owns automatic candidate search/generation. Native SQX owns backtest, robustness/cross-check, optimization, Retester and Custom Project execution.
- Do not implement a second TraderCockpit Builder, genetic algorithm, strategy-tree language, backtester, robustness engine, optimizer, or Custom Project executor when SQX owns the operation.
- The currently proven executable producer boundary is the verified local SQX 144.2953 runtime controlled through native configuration/projects/databanks/`sqcli.exe`. Production adapters may evolve, but producer ownership does not move into ad-hoc TraderCockpit algorithms.
- Recovered/source/reference trees are evidence and build-time research material. Production code must not import those trees as loose runtime dependencies.
- Do not invent Phase 0 / Phase 1 / `phase01_intake` product stages.
- The accepted TraderCockpit prototype defines presentation. Do not clone SQX's dense interface and do not discard the accepted Home zones merely because an older shell is being replaced.

Top-level desktop surfaces are:

`Home | StrategyQuant X | Explore | Automation | Operate | Settings`.

Inside the dedicated **StrategyQuant X** screen, the research backbone is intentionally stable:

- research stages: `Construct | Backtest | Proof`;
- Construct tabs: `Idea | Specification | Build | Candidates`;
- Backtest tabs: `Overview | Trades | Robustness | Configuration`.

Those research stages are not top-level TraderCockpit workspaces. Dynamic capabilities and add-ons populate registered extension slots rather than rewriting either the top-level application navigation or the internal SQX research navigation.

## 2. Binding authority documents

Read these together before implementation:

1. `docs/product-architecture-v1.md` — producer ownership and product lifecycle;
2. `docs/product-backbone-spec-v1.md` — detailed UI/application/API/add-on contract;
3. `docs/home-strategyquant-surface-authority-v1.md` — binding clarification separating live/current Home from the dedicated SQX historical-research screen;
4. `IMPLEMENTATION_CHECKLIST.md` — execution order and acceptance gates;
5. `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/`sqx-lab` authoring boundary;
6. `docs/consumer-openrouter-account-authority-v1.md` — consumer Google account, OpenRouter spend and model-routing boundary;
7. `docs/repository-consolidation-v1.md` — current cleanup and development-desktop delivery rule.

Where older navigation prose conflicts with `docs/home-strategyquant-surface-authority-v1.md`, the Home/SQX surface clarification wins. Where older implementation prose conflicts with the current consolidation authority, Issue #37 and the current consolidation document win until the checkpoint closes.

## 3. Strategy authoring hierarchy

Apollo is deferred. Do not import or merge a persistent Apollo product spine.

Use the proven hierarchy:

1. **Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder** — core strategy authoring and generation authority.
2. **Native SQX MCP (`ServletMCP`)** — first-party integration/control surface. In retained build 144.2953 its published tools are `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, and `stop_project`; do not invent authoring methods behind it.
3. **`sqx-lab`** — optional external-LLM/custom-artifact extension for install-derived blocks, groups, `.sqx` templates and `project.cfx` projects. It is not the universal idea path or core strategy intelligence.
4. **TraderCockpit** — orchestration, custody, approval, control, readback and presentation.

The existence of native SQX AI-assisted authoring is proven. The exact supported programmable seam for directly invoking native SQX AI remains an evidence question. A missing transport seam is not permission to replace native SQX strategy authority with Apollo, `sqx-lab`, or a TraderCockpit strategy engine.

## 4. Consumer Google/OpenRouter boundary

The approved consumer-LLM architecture reuses the earlier Futures/TraderCockpit **concept**, not personal credentials or quantitative engine architecture:

`consumer Google sign-in → stable TraderCockpit account → configured starter/plan allowance → bounded per-consumer OpenRouter spend authority → centrally selected efficient model`.

Rules:

- Google authenticates the consumer to TraderCockpit; it is not an OpenRouter login.
- The operator/application owns the OpenRouter provisioning/management credential. Never expose it to browser code or consumers.
- Prefer provider-enforced per-consumer OpenRouter limits/reset/expiry plus internal usage/readback; a local credit counter is not the sole money ceiling.
- Starter-credit amounts, renewal cadence and paid-plan allowances are product configuration. Do not invent commercial values in implementation code.
- The current default workhorse policy is `z-ai/glm-5.3-flash`; model/provider/fallback policy is backend-configurable and may change as the model market changes.
- OpenRouter is an external-LLM transport/billing fabric. It may assist with user intent, approved extensions, summaries and tool operation, but it does not take Builder/backtest/robustness/optimization authority away from SQX.

The earlier `javin23863/futures` repository remains quarantined as TraderCockpit strategy/product architecture. The user's explicit exception is narrow: its and `tradercockpit-app`'s consumer Google/OpenRouter account pattern may be inspected as design lineage. Do not import the Futures quantitative backend into this product.

## 5. Mandatory context before planning or editing

Before changing an SQX-backed capability:

1. inspect the relevant original SQX screenshots, not only prose summaries;
2. inspect matching `.cfx`, task XML, preset/configuration, output archives, or runtime evidence;
3. inspect the accepted TraderCockpit prototype mapping;
4. read the matching section of `docs/product-backbone-spec-v1.md`;
5. read `docs/home-strategyquant-surface-authority-v1.md` before changing global navigation, Home, SQX placement, or live-vs-historical presentation;
6. for strategy authoring, read `docs/sqx-authoring-authority-v1.md`;
7. for consumer LLM/account work, read `docs/consumer-openrouter-account-authority-v1.md`;
8. identify which native SQX module owns the quantitative operation;
9. identify exactly what TraderCockpit must authenticate/configure/control/read/persist/present;
10. only then edit implementation files.

The observed Builder configuration surfaces are:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks (robustness) → Ranking → Notes`.

These define native construction/search configuration. Genetic evolution is one Builder mode. Retester operates on existing strategies. Custom Projects automate ordered native tasks/databanks.

For live/current Home work, identify the actual producer for market, signal, account, execution, risk, performance, or pipeline state. Never substitute historical SQX data merely because it is available.

## 6. Producer boundary — do not reconstruct a replacement engine

Previous policy requiring TraderCockpit-owned reconstruction when SQX internals were incomplete is superseded.

When native producer behavior is not yet wired:

- inspect more native configuration/source/runtime evidence;
- expose the native field/capability as unresolved or unavailable if necessary;
- extend the adapter to the actual SQX producer;
- fail closed rather than substitute a new TraderCockpit producer.

TraderCockpit may implement application mechanics SQX does not own: authentication, account/credit state, API routing, desktop supervision, intent/configuration records, exact configuration snapshots, process control, content-addressed custody, lifecycle state, read models, UI state, provenance and proof.

TraderCockpit must not manufacture strategy generation, native fitness, historical backtest results, robustness outcomes, optimization outcomes, or Custom Project task semantics.

Likewise, Home must not manufacture live/current market, signal, risk, execution, account, or performance truth.

## 7. Required user lifecycle and foundation gates

The StrategyQuant X historical-research lifecycle is:

```text
Idea / source
  → native SQX authoring capability when needed
  → exact native SQX Construct configuration + approval
  → native Builder generation + initial evaluation
  → Candidate Lab
  → native Backtest / cross-check / Retester / Optimizer funnel
  → Proof
```

Custom Projects automate this lifecycle using native SQX tasks/databanks.

The first required native product proof is:

`bounded idea → native SQX authoring/configuration → approved native Builder run → real .sqx survivor → Candidate Lab → one downstream native validation/retest → Backtest → Proof → restart/reopen same identities`.

That proof must be visible through the dedicated StrategyQuant X screen in the development desktop. It does not replace the separate Home/live product track.

MCP may contribute its published project/strategy inspection and control tools. `sqx-lab` participates only if the selected acceptance case actually needs its custom-artifact capability.

The consumer account/LLM lane has a separate release proof:

`Google sign-in → stable internal subject → configured allowance → bounded OpenRouter spend → configured workhorse request → usage attribution → limit refusal → sign-out/lapse/revocation cannot keep spending`.

A mock, fixture, synthetic result, TraderCockpit-only strategy schema, shared uncapped provider key, browser-stored secret, or local-only credit counter does not satisfy the relevant gate.

## 8. Disposition of duplicate and reusable work

- PR #23: retain native SQX candidate/Retester/custody/readback pieces; Retester remains downstream.
- PR #15: retain native Custom Project topology custody; execution remains native SQX.
- PR #25: do not merge its TraderCockpit-owned Builder/search producer; salvage only valid UI/custody/application pieces.
- old `product/tradercockpit/builder/evolution.py`: removed/quarantined from production producer wiring.
- PR #27: do not merge TraderCockpit-owned robustness producer algorithms where SQX owns the cross-check.
- PR #28: do not merge a TraderCockpit-owned task/loop executor as a replacement for native Custom Projects.
- PR #33: Apollo is superseded/deferred; do not import its persistent assistant implementation. Reuse only narrow generic safety/refusal ideas if they remain useful.
- retained `ServletMCP`: use its actual published tools where useful; do not expand authority by inference.
- `codex/sqx-lab-plugin`: optional custom native-artifact extension material, not core intelligence.
- earlier TraderCockpit app Google/OpenRouter work: conceptual/account-infrastructure lineage only; do not copy secrets, personal state or customer records.
- algorithm-parity ingredient PRs are evidence/test donors, not production engine modules.

## 9. Assistant-first execution and concurrency

The primary assistant must do every operation available through connected repository/runtime tools before delegating. A desktop agent is an implementation/runtime executor, not the product architect unless the user explicitly says otherwise.

For concurrent work:

- each lane gets its own branch/worktree;
- never switch/reset/clean another lane's checkout;
- treat unknown local changes as protected concurrent work;
- re-check live PR heads before touching shared files;
- acceptance runs on a clean checkout pinned to the exact tested commit.

## 10. Implementation behavior

- Prefer one end-to-end vertical over isolated capability fragments.
- Do not create a second server, store, candidate identity, run pipeline, account authority, credit authority, result authority, live-data authority, or duplicate UI product spine to avoid an integration conflict.
- Preserve exact native configuration, archive and producer identities.
- Native producer errors must be structured and visible; never silently fall back to a substitute implementation.
- UI data comes from backend read models; do not hard-code producer state, result metrics, phase counts, candidate IDs, validation truth, account balances, live prices/signals/risk, or model pricing.
- Frontend code does not maintain master lists of SQX indicators/capabilities or model/provider policy. Those come from backend capability/routing policy.
- The exact OpenRouter model slug, provider preference, fallback list and account limit policy stay in backend configuration.
- Fast/Golden or other validation profiles compile to inspectable native SQX-backed plans rather than frontend constants.
- Add-ons contribute only through registered typed extension slots and compatible presentation primitives.
- Documentation is not an implementation substitute. After an architecture decision is recorded, default to executable product work.

## 11. Review and acceptance ownership

After any implementation report:

1. inspect the actual diff/state;
2. compare it with native SQX authority and the binding documents above;
3. fix concrete defects directly when possible;
4. run focused acceptance;
5. run full applicable product/browser/runtime/desktop acceptance on the exact head;
6. report product completion only when the real user path is executable.

Green unit tests do not establish product completion.

For a PR intended to merge:

- record the exact current head;
- run required executable acceptance on that head;
- inspect substantive review findings for that head;
- correct valid findings and rerun acceptance;
- a corrective commit requires review of the new head;
- do not describe mechanical workflow success as substantive review closure.

## 12. Binding acceptance questions

For every SQX-backed capability ask:

> Can a user perform the intended historical-research operation through the dedicated StrategyQuant X screen, through the canonical application/native gateway, through the actual SQX producer that owns the operation, and receive durable truthful results back in TraderCockpit?

For Home/live capability work ask:

> Is the Home zone reading current state from the correct market/account/execution/signal/risk/performance producer, with historical SQX results kept explicitly scoped rather than substituted for live truth?

For strategy authoring additionally ask:

> Did the user's request remain on a native SQX authoring/execution path rather than becoming a TraderCockpit-only strategy representation?

For consumer LLM/account work additionally ask:

> Is the request attributable to a stable consumer account, bounded by provider-enforced spend authority, routed by backend policy, and unable to continue spending after the relevant limit or entitlement ends?

If no, the capability is not product-complete.
