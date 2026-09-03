# Desktop Agent: TraderCockpit UI Authority

Start here before changing the frontend or the engine framing.

The product UI authority is the neon TraderCockpit prototype: the five screens under
`references/ui-authority/screenshots/`. They supersede the earlier dark-blue
`Chart / Backtest / Proof` shell and the earlier "ESQ" multicolor mockups.

## Repository-native visual previews

Open the five files in `references/ui-authority/previews/` (720-px derivatives of the canonical
PNGs; open the PNG when you need detail):

1. `cockpit-home.webp` — Home chrome and density. Product Home is the eight live/current zones (Market Overview · System Status · Alpha Stack · Pipeline Overview · Signals · Risk · Performance · Quick Actions) plus persistent Apollo. PNG card titles are illustrative framing.
2. `order-flow-signals-models.webp` — Research → Signals & Models (nine tabs, chart, Strategy Panel, Signal Pulse, Active Models, bottom row).
3. `evolutionary_search_trading_dashboard.webp` — Research → Evolutionary Search (state strip, configuration/population/generations/Pareto/operators/fitness/islands/objectives/candidates/seed).
4. `test-validate-dashboard.webp` — Research → Test & Validate (KPI strip, funnel, performance, distribution, seven stage cards, run table, conclusions, next actions).
5. `indicators-models-catalog.webp` — Research → Indicators & Models (pills, filters, categories rail, component table, detail panel).

The exact canonical PNG identities, dimensions, byte counts, and SHA-256 digests are pinned in
`references/ui-authority/manifest.json`; verify before trusting a copied asset.

## Rules for UI-impacting work

- Keep the six surfaces and the four Research workspaces with their exact tab rows; do not condense tabs.
- Render real values only where a backend read model exists; every other value is an explicit "not connected / no data yet" state. Charts render axes and frames; series appear only from a read model.
- Native StrategyQuant X depth (blocks, GA parameters, rankings, cross-checks, presets, topology) is shown from the exact native task with native tag names visible; TraderCockpit assigns no quantitative semantics to them.
- Validation verdicts come only from the backend `cockpit_verdict` read model (SQX produces the trades, the cockpit computes the verdict over them); the UI never infers pass/fail client-side.
- The Assistant is a bounded, functional card (Apollo identity) under the account/model boundary, backed by `/api/assistant`; it is never disabled and never a product spine.
- Vanilla ESM in one `web/` tree; no framework or build step.

## Real-runtime acceptance

When the installed StrategyQuant X 144.2953 runtime is available, work from current
`main`, follow `docs/windows-desktop-acceptance-runbook.md`, and bring back the assessment
package it lists. The running program is the executable specification; fixtures and tests
are corrected to it, not the other way round.

## Consumer contract

```text
idea / specification → native Builder search (Random Discovery | Genetic Evolution)
→ candidate import → Initial Test (native Retester) → cross-checks → Evidence (Proof)
→ prop simulation / delivery
```

Do not infer that every stage has a producer. Connect existing native seams first; search scores
are discovery outputs, not validation evidence.
