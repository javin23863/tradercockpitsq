# Machine Learning / Models modality

Subordinate implementation guide. Canonical ownership lives in
`docs/product-architecture-v1.md` §3 and the Research contract in
`docs/product-backbone-spec-v1.md` §3. Grounding:
`references/quant-guild/excerpts/supervised-ml.md`,
`backtest-discipline.md`, `performance-metrics.md`.

Authority screen: `references/ui-authority` → `indicators-models-catalog`.

## 1. Purpose

Give the operator a platform-owned way to fit allowlisted classifiers
on **native SQX trade evidence** bound to one completed Historical
Result, then show those models in the Indicators & Models catalog.
Outputs flow into the same Candidate → Backtest → Robustness → Proof
custody. This is **not** a substitute SQX Builder / GA / backtester /
robustness engine.

## 2. What already exists (baseline — extend, do not rebuild)

On `cursor/ml-models-e2e-5d85` (fit-error handling on
`cursor/ml-models-fit-error-handling-5d85`):

| Piece | Location |
|-------|----------|
| Backend | `product/tradercockpit/research_models.py` |
| HTTP | `GET`/`POST /api/research/models` in `app_server.py` |
| UI | `web/research-models.mjs` + catalog tab `models` |
| Tests | `tests/product/test_research_models.py`, `tests/research-models.test.mjs` |
| Extra | `pyproject.toml` optional `ml = ["scikit-learn>=1.3,<2"]` |

Current families:

- `sklearn.tree.DecisionTreeClassifier` (`max_depth=3`, `random_state=0`)
- `sklearn.ensemble.RandomForestClassifier` (`n_estimators=32`, `max_depth=3`, `random_state=0`)
- `sklearn.ensemble.GradientBoostingClassifier` (`n_estimators=32`, `max_depth=2`, `random_state=0`)

Current features: `Duration`, `MAE`, `MFE`, `PipsPL`.
Label rule: `producer_pl_positive` (`1` if `PL > 0` else `0`).
Current score: **in-sample** `estimator.score(rows, labels)` only.
GET never loads a pickled estimator. Artifacts are joblib + SHA-256
under `{data_root}/ml-models/`.

**Required extensions in this guide:** logistic-regression baseline;
purged/embargoed OOS score; deflated-Sharpe / multiple-testing note;
`scope` on every record; fail-closed `ml_fit_failed` already present
on the error-handling branch.

## 3. Academic / mathematical contract

### 3.1 Label and features

Let trade `i` have producer fields from `orders.bin`
(`product/tradercockpit/sqx_orders.py`):

```
x_i = (Duration_i, MAE_i, MFE_i, PipsPL_i)
y_i = 1[PL_i > 0]
```

`scope` of this matrix is `historical_explanatory`. These features
describe a *completed* trade. They must not be advertised as a live
predictive feature set.

Minimum trades: 8 for a reported OOS score (keep `_MIN_TRADES = 2` only
for the fail-closed "cannot fit" path; if `2 ≤ n < 8`, fit is refused
with `reason_code: ml_trades_insufficient_for_oos`).

### 3.2 Purged / embargoed CV (López de Prado, AFML Ch. 7)

Sort trades by `CloseTime`. For `k=3` folds (or `k=2` when
`8 ≤ n < 24`):

1. Hold out a contiguous time block as the test fold.
2. **Purge** any training trade whose open/close interval overlaps the
   test fold.
3. **Embargo** training trades whose close is within `τ` of the test
   fold start, where `τ = median(Duration)` (at least one bar).

Score = mean test-fold accuracy (and log-loss if both classes appear).
Store `cv.n_splits`, `cv.embargo`, `cv.mean_accuracy`,
`cv.fold_accuracies`. If a fold has a single class, skip that fold;
if every fold is skipped, `reason_code: ml_fit_failed` (single-class).

The catalog row for a fitted model must also publish **Expected
value** and **Sharpe** of the bound Historical Result trades
(same formulas as
`references/quant-guild/excerpts/performance-metrics.md`). These
are mandatory trader-facing fields on the Models tab: the model
fit does not replace them. If trades cannot form Sharpe (`n < 2`)
or EV (`n < 1`), keep the keys with an explicit `reason_code`.

Do **not** replace the existing `train_accuracy` field — keep it and
add `oos_accuracy` so the UI can show both and the operator can see
overfit.

### 3.3 Multiple testing

When the operator fits `N > 1` families on the same Historical Result,
the catalog record of each fit must include:

```
selection: { trial_index, trial_count_on_result, deflated_sharpe_status }
```

`deflated_sharpe_status` is `computed` only when a return series can
be built from trade P&L and `trial_count_on_result` is known; otherwise
`selection_count_unknown` and no letter grade is derived from Sharpe.

### 3.4 Allowlisted families (retail default)

| `family_id` | Label | Params |
|-------------|-------|--------|
| `sklearn.linear_model.LogisticRegression` | Logistic regression | `max_iter=200`, `random_state=0` |
| `sklearn.tree.DecisionTreeClassifier` | Decision tree | `max_depth=3`, `random_state=0` |
| `sklearn.ensemble.RandomForestClassifier` | Random forest | `n_estimators=32`, `max_depth=3`, `random_state=0` |
| `sklearn.ensemble.GradientBoostingClassifier` | Gradient boosting | `n_estimators=32`, `max_depth=2`, `random_state=0` |

Optional later (not in first slice): `sklearn.neural_network.MLPClassifier`
with `hidden_layer_sizes=(8,)`, only when `n ≥ 200`. Neural-net catalog
*cards* may exist as `unavailable` / `ml_family_not_enabled` until then.

No XGBoost, TensorFlow, or PyTorch.

## 4. Native evidence source

```
read_historical_trades(store,
    historical_result_entity_id=...,
    expected_historical_result_revision=...)
```

Require: revision match, `state == "completed"`,
`execution_completed is True`, immutable `result_archive_sha256`.
Catch `ResearchTradesError`, `ResearchRetesterError`,
`ResearchCustodyError` and re-raise `ResearchModelsError` with the
upstream code preserved when it is a client identity error.

## 5. Read model and HTTP

Schema: `tc.research-ml-model-catalog.v1`
Path: `/api/research/models`
Loopback-only. No query parameters.

### GET

```json
{
  "schema": "tc.research-ml-model-catalog.v1",
  "scope": "historical_research",
  "backend": "sklearn",
  "backend_available": true,
  "reason_code": null,
  "feature_names": ["Duration", "MAE", "MFE", "PipsPL"],
  "label_rule": "producer_pl_positive",
  "families": [
    {
      "family_id": "sklearn.linear_model.LogisticRegression",
      "label": "Logistic regression",
      "enabled": true
    }
  ],
  "models": [],
  "detail": "..."
}
```

When sklearn is missing: `backend_available: false`,
`reason_code: "ml_backend_not_installed"`, `models: []`.

### POST

Exact keys only (reject extras, especially `path` / `sqx_home` /
`model_path`):

```json
{
  "action": "fit",
  "family_id": "sklearn.linear_model.LogisticRegression",
  "historical_result_entity_id": "tc-research:historical-result:v1:...",
  "expected_historical_result_revision": "tc-research-revision:..."
}
```

Success: 200 + updated catalog. Identity errors: 400.
Missing store: 503. State conflicts (missing result, incomplete,
single-class, insufficient trades): 409 with `reason_code`.

### Reason codes

`ml_backend_not_installed`, `ml_family_unknown`, `ml_family_not_enabled`,
`ml_features_invalid`, `ml_trades_insufficient`,
`ml_trades_insufficient_for_oos`, `ml_action_invalid`,
`ml_fit_identity_invalid`, `ml_fit_failed`, `research_store_not_bound`,
plus preserved custody codes (`current_pointer_missing`,
`research_proof_entity_invalid` is not applicable here).

## 6. Persistence and security

- Catalog: `{data_root}/ml-models.json` via `atomic_write_json`.
- Artifacts: `{data_root}/ml-models/{sha256}.joblib`.
- GET never `joblib.load`s. There is no inference endpoint in this slice.
- Production-boundary already rejects `pickle.load` / `joblib.load` in
  production code — keep GET metadata-only so the checker stays green.
  Fit-time `joblib.dump` is the only serialization, inside
  `research_models.py`, and must stay allowlisted if the checker is
  extended.
- Browser never chooses a filesystem path.

## 7. UI

Surface: Research → Indicators & Models catalog → tab `models`
(`web/research-models.mjs`, mount `[data-ml-models]`).

Must show:

- backend availability from the catalog (`data-backend-available`);
- Historical Result `<select data-ml-result>` of *completed* results;
- one Fit button per **enabled** family (`data-ml-fit="{family_id}"`);
- fitted rows with family label, result id, trade count, sha256 prefix,
  `train_accuracy` **and** `oos_accuracy` (or "OOS unavailable");
- truthful empty/unavailable copy — never a fabricated accuracy.

Forbidden in the DOM: file inputs, `path`, `sqx_home`, `model_path`,
hard-coded accuracies, Neural Network as a working Fit target until
enabled.

Authority visual grammar: catalog cards / table from
`indicators-models-catalog`. Do not invent a second Models workspace.

## 8. Tests to add or extend

`tests/product/test_research_models.py`:

- catalog truthful when sklearn missing;
- extra `path` field → 400 `ml_fit_identity_invalid`;
- fit writes artifact digest from native trades;
- missing Historical Result → 409 `current_pointer_missing`;
- single-class → `ml_fit_failed`;
- **new:** `n < 8` → `ml_trades_insufficient_for_oos`;
- **new:** logistic regression is in `FAMILIES` and can fit;
- **new:** `oos_accuracy` is present and differs from a forced
  overfit case (or equals train only when folds collapse — then
  reason code);
- HTTP GET schema + POST query-param refused.

`tests/research-models.test.mjs`:

- parser accepts only sklearn `family_id`s;
- panel has `data-ml-fit` for the four enabled families;
- no file / path fields;
- rendered copy includes `OOS` or `oos_accuracy`.

## 9. Step-by-step coding instructions

1. Start a branch from current `main`. Cherry-pick or rebase
   `cursor/ml-models-e2e-5d85` + the fit-error-handling commit so
   `research_models.py` exists.
2. Add `sklearn.linear_model.LogisticRegression` to `FAMILIES` with the
   params in §3.4.
3. In `fit_model`:
   - after building `rows, labels`, if `len(rows) < 8` raise
     `ResearchModelsError("ml_trades_insufficient_for_oos")`;
   - fit as today;
   - compute purged/embargoed CV (§3.2) into `oos_accuracy` /
     `cv`;
   - keep `train_accuracy`;
   - write both into the catalog record plus `scope:
     "historical_explanatory"` and `selection`.
4. Catch sklearn `ValueError` → `ml_fit_failed` (already on the
   error-handling branch).
5. Persist with `atomic_write_json` (hardening branch utility; copy the
   module if `main` does not yet have it — do not invent a second
   writer).
6. Wire GET/POST in `app_server.py` if the cherry-pick did not.
7. Update `web/research-models.mjs` to list four families and show
   OOS. Keep MutationObserver mount if that is the current catalog
   pattern; do not add a second Models route.
8. Add the new unit tests. Run
   `python tools/check_production_boundary.py`,
   `python -m unittest tests.product.test_research_models`,
   `node --test tests/research-models.test.mjs`.
9. Do **not** load artifacts on GET. Do **not** add live inference.

## 10. Out of scope (later guides)

Indicator Zoo importance, Wave Intelligence regimes, Home lifecycle,
and add-on Results-plugin analytics are separate guides. A fitted
model record is an input to those guides via `artifact_sha256` +
Historical Result identity only.
