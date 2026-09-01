# TraderCockpit Prototype UI Authority

This directory records the canonical frontend prototype lineage that the SQX engine extraction must serve.

## Authority ruling

The multicolor ESQ TraderCockpit/Cursor-era prototype is the frontend product authority. It supersedes the earlier dark-blue `Chart / Backtest / Proof` shell as a product baseline.

The authority set is exactly these five recovered screens:

1. `cockpit-home.png` — Cockpit Home: market overview, engine/system status, Alpha Stack, pipeline overview, signals, risk, performance, quick actions, and Apollo.
2. `order-flow-signals-models.png` — strategy workspace: chart, Signals & Models, confluence, signal history, market state, and persistent Apollo composer.
3. `test-validate-dashboard.png` — validation workspace: Initial Test, Fast Pipeline, Golden Pipeline, scenario tests, OOS, stability/stress tests, costs, and validation funnel.
4. `evolutionary_search_trading_dashboard.png` — evolutionary search: population, generations, mutation, Pareto selection, deterministic seed, search budget, fitness evolution, Pareto frontier, MAP-Elites archive, islands, objectives, and candidate table.
5. `indicators-models-catalog.png` — Research / Indicators & Models catalog with capability/data requirements and strategy integration.

The exact bytes, dimensions, and SHA-256 digests are pinned in [`manifest.json`](manifest.json). The expected repository location for the binary files is `references/ui-authority/screenshots/`.

## Engine-facing consumer chain

These screens establish concrete product consumers for the backend flow:

```text
strategy construction / signals
        ↓
candidate generation
        ↓
evolutionary search
        ↓
initial backtest
        ↓
Fast robustness
        ↓
Golden robustness
        ↓
scenario / stress / OOS
        ↓
prop simulation
        ↓
evidence / monitoring
```

This is a consumer contract, not a claim that every backend stage is already complete. Engine work should recover and connect existing implementation first, then add only missing seams required by these consumers. Search/evolution scores are discovery signals, not validation results; validation and evidence remain distinct governed stages.

## Custody and verification

The recovered source set was consolidated under the persistent Library path `/TraderCockpit/UI Baselines/2026-08-26/` on 2026-08-30. Repository consumers must not depend on that Library path at runtime; it is provenance only.

Use [`../../tools/sync-ui-authority.ps1`](../../tools/sync-ui-authority.ps1) to import the five PNGs from a local recovered source directory or to verify an already committed repository copy. The tool checks byte count and SHA-256 before accepting any asset and fails closed on missing, renamed, or altered files.

Do not substitute screenshots from the earlier blue shell, regenerate lookalikes, or silently edit these baseline files. Improvements belong in a new, explicitly versioned prototype lineage while preserving this set as the historical product authority that informed the engine framing.
