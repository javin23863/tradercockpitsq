# Indicator Zoo / feature-structure grounding

Cites Quant-Guild lectures 17 (PCA), 44 (time series), 63 (neural nets /
feature capacity).

## Claims the product must honor

1. **An indicator is a feature of one strategy, not a global zoo of
   invented values.** The Indicator Zoo for a selected Candidate /
   Historical Result is the set of native SQX blocks/indicators that the
   strategy actually used (from approved configuration / native artifact)
   plus the per-trade / per-bar features derived from that run.

2. **Importance is comparative, not causal.** Permutation importance
   (shuffle one feature, measure score drop) and impurity importance
   (trees) are the retail defaults. SHAP is optional and must be an extra
   (`shap`) that fails closed to `reason_code: shap_backend_not_installed`.

3. **PCA reports structure.** Lecture 17: given a centered matrix `X` of
   indicator/feature columns, `X = U Σ Vᵀ`. Report the first `k`
   explained-variance ratios `σ_i² / Σ σ_j²` and the loadings `V`. Do not
   fabricate a "market-fit score" from PCA.

4. **Do not invent indicator values.** If the native configuration does
   not name an indicator, the zoo must not display one. Catalog cards
   for unused families stay `unavailable` / `not_in_this_strategy`.

## Retail-runnable method

- Feature matrix from native trades + named configuration indicators.
- `sklearn.inspection.permutation_importance` on the fitted allowlisted
  model (same family as `docs/features/ml-models.md`).
- `sklearn.decomposition.PCA` with `n_components = min(5, n_features)`.
- Optional SHAP TreeExplainer for tree families only.
