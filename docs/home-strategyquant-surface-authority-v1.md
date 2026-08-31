# Home and StrategyQuant X Surface Authority v1

## Status

This is a binding consolidation clarification for the desktop application. Where older product documents describe `Construct | Backtest | Proof` as the global application navigation, this document narrows that wording: those stages are the internal historical-research workflow of the dedicated **StrategyQuant X** surface, not the TraderCockpit Home screen and not the whole application's top-level navigation.

This clarification preserves the native-SQX producer boundary. It changes only product placement/navigation authority.

## Top-level desktop surfaces

TraderCockpit is one desktop application with distinct product surfaces:

- **Home** — live/current cockpit orientation and operational awareness;
- **StrategyQuant X** — historical strategy research, construction, backtesting, validation and proof through the native SQX backend;
- **Explore** — capability/catalog discovery;
- **Automation** — native SQX Custom Project/workflow visibility and control where supported;
- **Operate** — live/deployed execution, performance and risk surfaces where supported;
- **Settings** — account, allowance, model policy, runtime/provider/add-on configuration.

StrategyQuant X is one top-level screen/surface. It is not the Home screen.

## Home authority

The accepted Cockpit Home prototype defines eight distinct zones. Consolidation must preserve these zones and their separation of concerns:

1. **Market Overview** — current market context/live-data orientation;
2. **System Status** — application/runtime/worker/system readiness and alerts;
3. **Alpha Stack** — current strategy/candidate/deployed-strategy context from authoritative producers;
4. **Pipeline Overview** — current pipeline/validation/deployment progress and attention state;
5. **Signals** — current signal/confluence state when live strategy and market producers exist;
6. **Risk** — current portfolio/exposure/loss/deployment risk state;
7. **Performance** — current account/strategy/live performance summaries with explicit scope;
8. **Quick Actions** — navigation into the owning surfaces without creating hidden workflows.

Home must never fabricate live data merely to populate these zones. Until their producers are connected, each zone remains visibly pending/unavailable while retaining its intended place in the desktop.

Historical SQX research results may be summarized on Home only when explicitly labeled and useful; they do not turn Home into the SQX workspace.

## StrategyQuant X surface authority

The StrategyQuant X screen is the product-facing interface to the native SQX historical research backend.

Inside that one surface, the research workflow remains:

`Construct -> Backtest -> Proof`

Construct tabs remain:

`Idea | Specification | Build | Candidates`

Backtest tabs remain:

`Overview | Trades | Robustness | Configuration`

Proof remains the exact evidence/provenance chain.

These are internal SQX research states, not independent top-level TraderCockpit workspaces.

The canonical browser route is one top-level path, `/strategyquant`, with internal research stage/tab state carried within that surface. Compatibility redirects from older research routes may exist, but they are not product-navigation authority.

## Producer boundary

StrategyQuant X continues to own historical strategy/research computation, including native authoring/AlgoWizard, Builder generation/GA behavior, backtesting, robustness/cross-checks, Retester, optimization/Walk-Forward, Custom Project execution and native `.sqx` artifacts.

TraderCockpit owns the desktop experience, account/auth, exact native configuration/custody, approval, job control/readback, live-product surfaces, Candidate Lab presentation and proof.

Home/live-product functionality must not be implemented by copying the SQX historical backend into the Home screen, and SQX must not be treated as the source of unrelated live market/account/execution truth.

## Acceptance rules

The consolidated desktop/browser acceptance must prove:

- `/home` is the default application route;
- Home renders all eight accepted cockpit zones;
- Home does not present `Construct | Backtest | Proof` as its primary content;
- `/strategyquant` is a separate top-level product surface;
- the StrategyQuant X surface exposes `Construct | Backtest | Proof` internally;
- Construct and Backtest internal tab sets remain exact;
- no persistent Apollo product spine returns;
- unavailable live/SQX producer state remains truthful rather than fabricated.
