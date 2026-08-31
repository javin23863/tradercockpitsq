# TraderCockpit Native-SQX Product Architecture

## Product decision

`main` remains the canonical TraderCockpit product line.

**StrategyQuant X 144.2953 is the strategy-research producer/backend authority. TraderCockpit is the consumer application, account/auth, guidance, configuration, custody, control/readback and presentation layer around that backend.**

TraderCockpit must not implement a second Builder, genetic algorithm, strategy language, backtester, robustness engine, optimizer or Custom Project execution engine where SQX owns the operation.

The production boundary is:

```text
Consumer
  |
  | Google identity + bounded product allowance
  v
TraderCockpit desktop/UI
  |
  | idea, user choices, review/approval, account state
  v
TraderCockpit application/runtime
  |                         \
  | native config/control    \ bounded external-LLM transport
  v                           v
StrategyQuant X 144.2953    OpenRouter workhorse policy
Builder / AlgoWizard /      account-attributed assistance/tools
Retester / Optimizer /
Cross checks / Projects
  |
  | native databanks, .sqx strategies, native results/trades
  v
TraderCockpit custody + Candidate Lab + Backtest + Proof
```

The currently proven executable SQX adapter uses the verified local 144.2953 runtime and `sqcli.exe`. That is the first production spine. The adapter boundary may later change transport or packaging without changing producer ownership.

Recovered/source/reference trees remain evidence and build-time research material; production code must not import them as ad-hoc runtime dependencies.

The older Futures quantitative architecture remains quarantined. The user's explicit exception is narrow: earlier Futures/TraderCockpit consumer Google/OpenRouter concepts may be inspected as account/LLM design lineage. They do not become this product's strategy backend.

There is no generic Phase 0/Phase 1 intake model in this product.

## Binding companion specifications

- `docs/product-backbone-spec-v1.md` — detailed application/UI/API/add-on contract;
- `docs/sqx-authoring-authority-v1.md` — native SQX AI/MCP/`sqx-lab` authoring hierarchy;
- `docs/consumer-openrouter-account-authority-v1.md` — consumer Google/OpenRouter account, spend and model-routing boundary;
- `IMPLEMENTATION_CHECKLIST.md` — execution order and acceptance gates;
- `AGENTS.md` — repository implementation policy.

## Fixed research navigation

Core stages:

- `Construct | Backtest | Proof`.

Construct:

- `Idea | Specification | Build | Candidates`.

Backtest:

- `Overview | Trades | Robustness | Configuration`.

Explore, Automation, Operate, account/settings and installed add-ons are auxiliary/capability-driven surfaces. They do not expand the core research stage bar by default.

## Authorities used to define the product

Implementation reconciles these authorities before code changes:

1. **Observed SQX behavior** — retained SQX UI, Builder settings, progress/results, Retester, cross-check and Custom Project surfaces.
2. **Executable SQX evidence** — saved `.cfx`/task XML, preset/configuration files, native runtime traces, native Builder outputs, `.sqx` archives, Retester results and bounded native runs.
3. **Official SQX semantics** — Builder creates/improves strategies; AlgoWizard edits strategies; Retester retests existing strategies; Optimizer optimizes existing strategies; Custom Projects automate ordered task/databank workflows; current SQX AI assists strategy authoring.
4. **TraderCockpit presentation authority** — accepted product direction for Idea/Specification/Build/Candidates, Candidate Lab, Backtest, Proof and auxiliary surfaces.
5. **Consumer account/LLM authority** — Google identifies the consumer to TraderCockpit; OpenRouter is a bounded external-LLM transport/billing layer with backend model policy.

Screenshots prove visible workflow/configuration surfaces, not hidden implementation. Native execution/result evidence proves producer behavior. TraderCockpit simplifies presentation without changing producer ownership.

## Native SQX authoring hierarchy

Apollo is deferred and is not part of the repaired product spine.

The strategy-authoring hierarchy is:

1. **Native SQX AI Wizard / AI Assistant + AlgoWizard / Builder** — primary strategy-authoring and generation authority.
2. **Native `ServletMCP`** — first-party AI-tool integration/control surface. Retained 144.2953 source publishes exactly `list_projects`, `list_databanks`, `list_strategies`, `get_strategy_stats`, `run_project`, and `stop_project`; those are control/readback tools, not a general authoring API.
3. **`sqx-lab`** — optional external-LLM/custom-artifact extension for install-derived custom blocks, groups, `.sqx` templates and `project.cfx` projects.
4. **TraderCockpit** — orchestration, custody, approval, control/readback and UI.

The existence of native SQX AI-assisted authoring is proven. The exact supported programmable seam for direct invocation from TraderCockpit remains open evidence. A transport gap is not permission to replace native SQX intelligence with a TraderCockpit strategy engine.

## Consumer account and external-LLM architecture

Google OAuth authenticates the consumer **to TraderCockpit**. It is not an OpenRouter login.

The intended account path is:

```text
first verified Google sign-in
  → stable TraderCockpit internal subject
  → configured entitlement / starter or plan allowance
  → bounded per-consumer OpenRouter spend authority
  → backend-selected efficient workhorse
  → usage/cost attributed to the account
```

Rules:

- the operator/application keeps the OpenRouter provisioning/management credential;
- consumers/browser code never receive that management credential;
- prefer provider-enforced per-consumer spending limits/reset/expiry plus internal accounting/readback;
- local credit display is not the only hard money ceiling;
- starter allowance and paid-plan values are product configuration, not source-code guesses;
- the current default workhorse policy is `z-ai/glm-5.3-flash`;
- model/provider/fallback policy is backend-configurable so the market can change without rewriting the frontend;
- OpenRouter may support intent interpretation, approved extensions, summaries and tool operation, but it never becomes the native trading/backtest/robustness/optimization authority.

## Actual SQX strategy-development lifecycle

### 1. Idea or trading concept

A user begins with an idea, source, indicator, existing strategy, template, paper, notes or explicit rules.

TraderCockpit owns the human-facing intake record and provenance. Bounded LLM assistance may summarize supplied material or identify unresolved requirements, but trading meaning that remains ambiguous requires explicit resolution. Factual product state always comes from backend records.

Questions derive from the selected native SQX construction path: strategy type, direction/style, conditions/periods, stop/profit behavior, data, trading/session rules, building blocks, sizing, search mode, ranking/filtering and optional cross-checks.

### 2. Native authoring when needed

If the request requires strategy authoring, use the smallest proven native SQX authoring capability.

- Prefer native SQX AI/AlgoWizard where a supported invocation path exists.
- Use MCP only for its published inspection/control tools.
- Use `sqx-lab` only when the case explicitly needs custom block/group/template/project artifact tooling.
- Any `sqx-lab` artifact must be install-derived, validated and accepted by the target SQX installation.

### 3. Construct the native strategy-search space

SQX Builder Full settings establish the search space:

`What to build → Parts to improve → Genetic options → Data → Trading options → Building blocks → ATM → Money management → Cross checks → Ranking → Notes`.

TraderCockpit maps approved intent to an **exact native SQX configuration snapshot**. It does not translate the idea into a competing executable strategy language.

The mapping preserves, where applicable:

- construction mode;
- direction/style/structural complexity;
- conditions, periods, stops and targets;
- symbol, timeframe, date ranges, IS/OOS, precision and trading costs;
- session/trading constraints;
- allowed indicators/signals/building blocks, weights and ranges;
- order/exit types;
- money management/sizing;
- build/search mode including genetic evolution when selected;
- ranking/basic filters and cross-check filters.

If a required native field cannot be mapped truthfully, Construct exposes the gap and the run remains locked.

### 4. Native Builder generation and initial evaluation

Builder is the strategy-generation producer. Genetic evolution is one Builder mode, not a separate TraderCockpit backend.

SQX owns:

- strategy generation;
- genetic selection/crossover/mutation/island mechanics;
- strategy-tree/block semantics;
- initial backtest/evaluation;
- native fitness/ranking/filter decisions;
- native Builder databank output.

TraderCockpit owns job identity, exact config custody, process/error handling, progress projection and durable import of exact native output.

A surviving strategy enters product custody as its exact native `.sqx` artifact plus canonical identity/provenance.

### 5. Candidate Lab

Candidate Lab is the user-facing view of real native Builder survivors.

It preserves:

`idea/source revision → native Construct configuration → native Builder job → native .sqx strategy → candidate identity`.

Candidate Lab may show only producer-backed fields plus TraderCockpit custody/provenance. It is not another generator.

### 6. Backtest and validation funnel

SQX Cross checks, Retester and Optimizer form the native strategy-testing funnel.

TraderCockpit exposes the selected native plan progressively rather than reproducing all SQX settings at once. Current evidenced method families include What If, Monte Carlo trade manipulation, higher precision, additional markets, Monte Carlo retest, Sequential Optimization, Optimization Profile/System Parameter Permutation, Walk-Forward Optimization and Walk-Forward Matrix.

Exact methods, order, settings and filters come from native-backed configuration/profile data. Product names such as `Fast` or `Golden` are allowed only as profiles that compile to inspectable native plans; they are not hard-coded phase counts.

### 7. Retester and Optimizer

Retester is downstream of strategy creation. It retests existing strategies with the same or deliberately changed settings. Optimizer operates on existing strategies for parameter optimization/Walk-Forward work.

TraderCockpit invokes these native modules when required. It never presents Retester as the start of the lifecycle or substitutes its own optimizer.

### 8. Custom Projects / Automation

Custom Projects automate the native workflow. Saved evidence includes ordered native combinations such as Build, Retest, ClearDatabanks, GoToTask, Optimize and other tasks.

TraderCockpit drives/observes the native Custom Project and its databank/task state. It must not translate `.cfx` topology into a second product-owned workflow engine and claim that is SQX execution.

### 9. Results and Proof

Backtest shows useful producer-backed strategy performance/trade analysis. Proof binds the complete chain of identities/evidence.

Proof answers:

- what idea/source revision was approved;
- what native configuration executed;
- which SQX build/worker/job executed it;
- what data/market/settings were used;
- which native strategy archive survived;
- what native results/trades were produced;
- what validation plan ran;
- what passed, failed, refused or did not execute;
- what exact artifact is retained/exported/promoted.

TraderCockpit may add custody/evidence receipts. It may not manufacture producer results or waive failed native gates.

## TraderCockpit user experience

```text
CONSTRUCT
  Idea → Specification → Build → Candidates
      ↓
BACKTEST
  Overview → Trades → Robustness → Configuration
      ↓
PROOF
  exact native artifacts + configuration + results + validation evidence
```

Home, Explore, Automation and Operate sit around this lifecycle.

There is **no binding persistent Apollo dock**. A bounded language-assistance surface may appear where useful, but it is a consumer tool surface backed by account/credit/capability policy, not a new strategy producer or separate product spine.

Language assistance may:

- summarize user-supplied sources;
- identify unresolved native configuration requirements;
- explain choices/evidence;
- prepare explicit configuration changes for review;
- explain native results/failures;
- navigate/propose valid next actions.

It may not:

- invent ambiguous trading meaning;
- silently approve or mutate an approved plan;
- launch/cancel consequential compute without the required product authorization;
- waive native gates;
- promote/certify a candidate;
- fabricate producer state or result truth.

## Runtime/application ownership

### TraderCockpit owns

- consumer Google identity verification and internal account subject;
- account entitlement/credit read model;
- bounded OpenRouter provisioning/credential custody and model policy;
- desktop shell and worker supervision;
- runtime discovery/health and exact SQX build verification;
- secure local application API;
- idea/source records and approved intent;
- deterministic mapping to exact native SQX configuration;
- configuration snapshots/diffs and approval;
- native job lifecycle/control;
- content-addressed custody of native strategy/result artifacts;
- product identities/provenance;
- progress/read models/frontend state;
- Candidate Lab, Backtest, Proof and Automation presentation;
- explicit refusal when a native mapping/capability is unavailable.

### SQX owns

- native AI-assisted strategy authoring and AlgoWizard semantics;
- Builder strategy generation;
- genetic/evolutionary producer algorithms;
- indicator/building-block strategy semantics;
- backtest engine behavior;
- native ranking/filter calculations;
- cross-check/robustness producer algorithms;
- Retester execution;
- optimization/Walk-Forward producer behavior;
- Custom Project task/databank execution;
- native strategy/result artifacts.

### OpenRouter owns externally enforced model spend/inference transport

OpenRouter is used as configured external-LLM infrastructure. Provider-side limits form a hard money boundary for the consumer model lane. OpenRouter does not own product account truth or SQX quantitative truth.

## Release gates

### Consumer account/LLM gate

Prove:

`Google sign-in → stable subject → configured allowance → bounded OpenRouter authority → configured GLM 5.3 Flash request → account-attributed usage → remaining allowance → limit refusal → no spend after lapse/revocation`.

### Native SQX Foundation Vertical

Prove one bounded idea end to end:

1. start TraderCockpit and verify exact SQX health;
2. create/open the idea;
3. use native authoring capability when needed and resolve only genuine native gaps;
4. explicitly approve the Construct plan;
5. produce/persist exact native Builder configuration;
6. start bounded native Builder;
7. show real native progress and produce a real `.sqx` survivor;
8. import exact archive to Candidate Lab;
9. run one downstream native validation/retest;
10. display Backtest/trade/result evidence;
11. display Proof linking the full chain;
12. restart/reopen the same durable identities/artifacts;
13. malformed/unavailable native config fails visibly with no substitute result.

No broader backend feature lane outranks this foundation path.

## Implementation order

1. Remove/quarantine duplicate TraderCockpit producer authority.
2. Consolidate consumer Google/OpenRouter account contracts and bounded spend/model policy.
3. Consolidate one canonical native SQX runtime/gateway, incorporating proven MCP tools only where useful.
4. Trace the native SQX AI/AlgoWizard invocation boundary.
5. Implement deterministic Construct planning/configuration custody.
6. Prove native Builder → Candidate Lab.
7. Prove native Backtest/validation → Proof.
8. Add native Custom Project automation.
9. Integrate `sqx-lab` only as optional custom-artifact extension.
10. Harden/package desktop and run clean-machine acceptance.

## Current-work disposition

- PR #23 native candidate/Retester/custody: retain useful native adapter/custody/readback work.
- PR #2 native Builder control: retain verified native launch/control direction.
- PR #15 native Custom Project topology: retain custody/parser direction; execution stays native.
- PR #25 TraderCockpit Builder engine: do not merge producer/search engine.
- `product/tradercockpit/builder/evolution.py`: quarantine from production producer authority.
- PR #27 duplicate robustness producer: do not merge as production authority.
- PR #28 duplicate workflow engine: do not merge as native Custom Project replacement.
- PR #33 Apollo: closed/deferred; do not revive persistent assistant architecture.
- retained `ServletMCP`: first-party control/readback integration material for published tools only.
- `codex/sqx-lab-plugin`: optional custom native-artifact extension.
- earlier TraderCockpit/Futures Google/OpenRouter work: account/LLM concept lineage only.

Ingredient PRs that reconstructed isolated SQX algorithms remain evidence/test donors, not production producer modules.

## Acceptance rule

A green unit suite is not product completion.

For any SQX producer claim, acceptance traverses the real TraderCockpit application and real SQX backend. For consumer LLM/account claims, acceptance uses a real stable account identity and provider-enforced bounded model spend rather than a developer personal key or local-only counter.

No test may substitute a TraderCockpit-generated quantitative result for a claim that SQX executed it.
