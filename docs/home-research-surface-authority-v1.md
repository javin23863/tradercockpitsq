# Home and Research Surface Authority v1

## Status

This is a binding consolidation clarification for the new platform desktop application.

**Research is a platform workspace. It is not named StrategyQuant X.** StrategyQuant X / SQX may be identified in backend provenance, native-runtime configuration, adapter code, logs, or technical diagnostics because it is one current historical-research producer. That producer identity must not become the platform's workspace name or product identity.

Where older product documents describe `Construct | Backtest | Proof` as global application navigation or call the research surface `StrategyQuant X`, this document supersedes that placement/naming.

## Top-level desktop surfaces

The platform has these top-level surfaces:

`Home | Research | Explore | Automation | Operate | Settings`

- **Home** — live/current cockpit orientation and operational awareness.
- **Research** — historical strategy research, construction, backtesting, validation, candidates, and proof.
- **Explore** — capability/catalog discovery.
- **Automation** — workflow visibility and control where supported.
- **Operate** — live/deployed execution, performance, and risk where supported.
- **Settings** — account, allowance, model policy, runtime/provider, and add-on configuration.

The canonical research route is `/research`.

`/strategyquant` is compatibility-only and redirects into `/research`; it is not a product-navigation authority.

## Home authority

Home preserves these eight zones:

1. Market Overview
2. System Status
3. Alpha Stack
4. Pipeline Overview
5. Signals
6. Risk
7. Performance
8. Quick Actions

Home must never fabricate live data to populate them. Historical research may be summarized on Home only with explicit scope.

## Research authority

Inside the single **Research** workspace, the research workflow is:

`Construct -> Backtest -> Proof`

Construct tabs:

`Idea | Specification | Build | Candidates`

Backtest tabs:

`Overview | Trades | Robustness | Configuration`

Proof is the exact evidence/provenance chain.

These are internal Research states, not top-level application workspaces.

## Producer boundary

The platform owns product identity, desktop experience, accounts/auth, approval, configuration/custody, job control/readback, Candidate Lab presentation, live-product surfaces, and proof presentation.

The current native SQX backend continues to own the historical quantitative operations already proven to belong to it, including native authoring/AlgoWizard, Builder generation/GA behavior, backtesting, robustness/cross-checks, Retester, optimization/Walk-Forward, Custom Project execution, and native `.sqx` artifacts.

That technical backend ownership does **not** rename the platform's Research workspace.

## Acceptance rules

Desktop/browser acceptance must prove:

- `/home` is the default route and renders all eight Home zones;
- `/research` is the separate top-level research workspace;
- top-level navigation says **Research**, not StrategyQuant X;
- `Construct | Backtest | Proof` remain internal to Research;
- Construct and Backtest tab sets remain exact;
- `/strategyquant` redirects to `/research` for compatibility;
- vendor/backend identity may appear only where producer provenance or native configuration is technically relevant;
- no persistent Apollo product spine returns;
- unavailable producer state remains truthful rather than fabricated.
