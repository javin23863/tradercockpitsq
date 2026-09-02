# Indicator Zoo

Subordinate implementation guide. Canonical: architecture §3, backbone
§3 (Indicators & Models catalog). Grounding:
`references/quant-guild/excerpts/indicator-structure.md`.

Authority screen: `indicators-models-catalog`.

## 1. Purpose

For the **currently selected** Candidate / completed Historical Result,
show the indicators and blocks that strategy actually used, plus
structure (PCA) and importance (permutation / optional SHAP) of the
features attached to that run. This is the "Indicator Zoo" the
prototype catalog implies — not a global invented library of every
possible indicator.

The same zoo feeds Home lifecycle decay for *indicators of that
strategy* (`docs/features/model-strategy-lifecycle-edge-decay.md`).

## 2. Ownership

- SQX owns the native block/indicator semantics and the configuration
  that named them.
- The platform owns the catalog presentation, importance/PCA math, and
  custody of the derived zoo record.
- Do not reimplement SQX indicators. If the approved configuration
  does not name a block, the zoo must not display a value for it.

## 3. Evidence source

1. Selected Candidate → approved configuration revision (already in
   Research custody).
2. Native Builder / AlgoWizard block list already exposed by
   `fetchNativeBuilderView` / preset catalog on later Research UI
   branches — on `main`, read the approved configuration's typed
   block/indicator names from the configuration custody record.
3. Completed Historical Result trades via `read_historical_trades`
   (same as ML models).
4. Optional: a fitted ML model `artifact_sha256` from
   `/api/research/models` to compute permutation importance. If no
   model is fitted, importance is `unavailable` /
   `ml_model_not_fitted` and the zoo still lists named indicators.

## 4. Math

### 4.1 Zoo membership

```
zoo = unique indicator/block names on the approved configuration
      that are of kind indicator|signal|raw-indicator
```

Each member:

```
{ id, native_name, kind, used_by_strategy: true, status: "current" }
```

Unused catalog families (Trend, Momentum, …) may appear as discovery
rows with `used_by_strategy: false`, `status: "not_in_this_strategy"`.
They must not show fabricated parameters or values.

### 4.2 Feature matrix for importance

Use the ML feature matrix (`Duration`, `MAE`, `MFE`, `PipsPL`) plus
any per-trade native fields already on the trade record. Do not
synthesize indicator time series from prices unless a live or
historical bar producer exists (it does not on `main`).

### 4.3 Permutation importance

```
imp_j = E[score(X, y) - score(X_{π_j}, y)]
```

`sklearn.inspection.permutation_importance` on the fitted allowlisted
estimator, scoring with the same purged-CV mean accuracy when a model
exists. Report `importances_mean`, `importances_std`, `n_repeats=10`.

### 4.4 PCA

Center columns. `sklearn.decomposition.PCA(n_components=min(5, p))`.
Report `explained_variance_ratio` and `components_` loadings keyed by
feature name. Do not treat a PC as a tradable indicator.

### 4.5 Optional SHAP

Only for tree families. Extra: `shap`. If missing:
`reason_code: shap_backend_not_installed`. Never block the zoo on SHAP.

## 5. Read model and HTTP

Schema: `tc.research-indicator-zoo.v1`
Path: `GET /api/research/indicator-zoo`

Query (registered only): `historicalResultEntityId`,
`expectedHistoricalResultRevision`, optional `modelArtifactSha256`.
Unknown query keys → 400.

```json
{
  "schema": "tc.research-indicator-zoo.v1",
  "scope": "historical_research",
  "status": "current",
  "reason_code": null,
  "historical_result_entity_id": "...",
  "historical_result_revision": "...",
  "configuration_revision": "...",
  "indicators": [
    {
      "id": "native:RSI",
      "native_name": "RSI",
      "kind": "indicator",
      "used_by_strategy": true,
      "status": "current"
    }
  ],
  "features": ["Duration", "MAE", "MFE", "PipsPL"],
  "importance": {
    "status": "current",
    "method": "permutation",
    "model_artifact_sha256": "...",
    "values": [{"feature": "MFE", "mean": 0.12, "std": 0.03}]
  },
  "pca": {
    "status": "current",
    "explained_variance_ratio": [0.61, 0.22],
    "loadings": [{"component": 1, "feature": "MFE", "value": 0.71}]
  },
  "shap": { "status": "unavailable", "reason_code": "shap_backend_not_installed" },
  "detail": "..."
}
```

Reason codes: `historical_result_missing`, `configuration_missing`,
`ml_model_not_fitted`, `ml_backend_not_installed`,
`shap_backend_not_installed`, `indicator_names_unavailable`,
`pca_insufficient_features`.

GET is read-only. No POST in the first slice (the zoo is derived).

## 6. UI

Catalog workspace tab `indicators` (and `all` when a result is
selected). Mount `[data-indicator-zoo]`.

- Table of `indicators` with `used_by_strategy` chip.
- Importance bar list only when `importance.status === "current"`.
- PCA variance chips only when `pca.status === "current"`.
- Discovery families without membership stay "not in this strategy".
- No hard-coded RSI/MACD values.

When no Historical Result is selected: empty state
"Select a completed Historical Result to load this strategy's Indicator Zoo."

If the zoo header shows strategy-level performance, it must reuse
`trade_metrics` **Expected value** and **Sharpe** (mandatory). Do
not invent a per-indicator EV unless a producer field isolates it.

## 7. Tests

- No result → `historical_result_missing` / empty indicators.
- Configuration without named indicators → `indicator_names_unavailable`.
- With trades + fitted model → permutation values length equals
  features; SHA matches.
- PCA refused when `p < 2` → `pca_insufficient_features`.
- Extra query key → 400.
- UI test: no fabricated indicator names in the empty state.

## 8. Coding instructions

1. New module `product/tradercockpit/indicator_zoo.py` with
   `indicator_zoo_record(store, ...)`.
2. Resolve configuration from the Historical Result's ancestry
   (same chain Backtest Configuration already reconstructs). If that
   chain is missing on `main`, fail closed — do not parse SQX XML
   ad-hoc beyond existing configuration custody.
3. Wire `GET /api/research/indicator-zoo` in `app_server.py`
   (loopback-only).
4. Frontend: `web/indicator-zoo.mjs` mounted from the catalog
   indicators tab. Fetch only after a result identity is selected.
5. Reuse ML feature builder; do not duplicate trade parsing.
6. Persist nothing except optional cache keyed by
   `(result_revision, model_sha)` if needed; default is derive-on-read.
7. Run boundary + unit + node tests for the new files.
