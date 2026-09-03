"""Platform-owned Machine Learning / Models modality.

Fits allowlisted sklearn classifiers on native SQX trade records already bound to
one completed Historical Result. This is not a substitute Builder, backtester, or
robustness engine. GET never loads a pickled estimator.
"""

from __future__ import annotations

from hashlib import sha256
import importlib.util
import json
from pathlib import Path
from statistics import median

from tradercockpit.atomic_io import atomic_write_json
from tradercockpit.research_custody import FileResearchCustodyStore, ResearchCustodyError
from tradercockpit.research_retester import ResearchRetesterError
from tradercockpit.research_trades import ResearchTradesError, read_historical_trades
from tradercockpit.trade_metrics import expected_value_record, sharpe_record


RESEARCH_MODELS_SCHEMA = "tc.research-ml-model-catalog.v1"
RESEARCH_MODELS_API_PATH = "/api/research/models"
FEATURE_NAMES = ("Duration", "MAE", "MFE", "PipsPL")
LABEL_RULE = "producer_pl_positive"
CATALOG_SCOPE = "historical_research"
MODEL_SCOPE = "historical_explanatory"
_CATALOG_NAME = "ml-models.json"
_ARTIFACT_DIR = "ml-models"
_MIN_TRADES = 2
_MIN_TRADES_FOR_OOS = 8
_MIN_EMBARGO_SECONDS = 1

FAMILIES = (
    {
        "family_id": "sklearn.linear_model.LogisticRegression",
        "label": "Logistic regression",
        "estimator": "sklearn.linear_model.LogisticRegression",
        "params": {"max_iter": 200, "random_state": 0},
    },
    {
        "family_id": "sklearn.tree.DecisionTreeClassifier",
        "label": "Decision tree",
        "estimator": "sklearn.tree.DecisionTreeClassifier",
        "params": {"max_depth": 3, "random_state": 0},
    },
    {
        "family_id": "sklearn.ensemble.RandomForestClassifier",
        "label": "Random forest",
        "estimator": "sklearn.ensemble.RandomForestClassifier",
        "params": {"n_estimators": 32, "max_depth": 3, "random_state": 0},
    },
    {
        "family_id": "sklearn.ensemble.GradientBoostingClassifier",
        "label": "Gradient boosting",
        "estimator": "sklearn.ensemble.GradientBoostingClassifier",
        "params": {"n_estimators": 32, "max_depth": 2, "random_state": 0},
    },
)
_FAMILY_BY_ID = {item["family_id"]: item for item in FAMILIES}


class ResearchModelsError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def sklearn_available() -> bool:
    return importlib.util.find_spec("sklearn") is not None


def _catalog_path(data_root: Path) -> Path:
    return data_root / _CATALOG_NAME


def _artifact_dir(data_root: Path) -> Path:
    path = data_root / _ARTIFACT_DIR
    path.mkdir(parents=True, exist_ok=True)
    return path


def _public_family(item: dict[str, object]) -> dict[str, object]:
    return {
        "family_id": item["family_id"],
        "label": item["label"],
        "enabled": True,
    }


def _load_models(data_root: Path) -> list[dict[str, object]]:
    path = _catalog_path(data_root)
    if not path.is_file():
        return []
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return []
    models = payload.get("models") if isinstance(payload, dict) else None
    return [item for item in models if isinstance(item, dict)] if isinstance(models, list) else []


def _write_models(data_root: Path, models: list[dict[str, object]]) -> None:
    atomic_write_json(
        _catalog_path(data_root),
        {"schema": RESEARCH_MODELS_SCHEMA, "models": models},
    )


def models_catalog(data_root: Path | str | None) -> dict[str, object]:
    available = sklearn_available()
    models = _load_models(Path(data_root)) if data_root is not None else []
    return {
        "schema": RESEARCH_MODELS_SCHEMA,
        "scope": CATALOG_SCOPE,
        "backend": "sklearn",
        "backend_available": available,
        "reason_code": None if available else "ml_backend_not_installed",
        "feature_names": list(FEATURE_NAMES),
        "label_rule": LABEL_RULE,
        "families": [_public_family(item) for item in FAMILIES],
        "models": models,
        "detail": (
            "Allowlisted sklearn classifiers fit on native SQX trade records from one completed Historical Result. "
            "Scope is historical_explanatory. SQX still owns backtest and robustness. Bind a fitted catalog digest onto an existing native Candidate; this never creates a Candidate from a pickle."
            if available
            else "Install the ml extra (scikit-learn) to fit Models. The browser never chooses an estimator path."
        ),
    }


def _feature_matrix(trades: list[dict[str, object]]) -> tuple[list[list[float]], list[int], list[float]]:
    rows: list[list[float]] = []
    labels: list[int] = []
    pnl: list[float] = []
    for trade in trades:
        try:
            rows.append([float(trade[name]) for name in FEATURE_NAMES])
            pl = float(trade["PL"])
            labels.append(1 if pl > 0 else 0)
            pnl.append(pl)
        except (KeyError, TypeError, ValueError) as exc:
            raise ResearchModelsError("ml_features_invalid", "native trade is missing a required producer field") from exc
    if len(rows) < _MIN_TRADES:
        raise ResearchModelsError("ml_trades_insufficient", f"fitting requires at least {_MIN_TRADES} native trades")
    if len(rows) < _MIN_TRADES_FOR_OOS:
        raise ResearchModelsError(
            "ml_trades_insufficient_for_oos",
            f"purged/embargoed OOS requires at least {_MIN_TRADES_FOR_OOS} native trades",
        )
    return rows, labels, pnl


def _trade_clock(trades: list[dict[str, object]]) -> tuple[list[int], list[int], list[int]]:
    opens: list[int] = []
    closes: list[int] = []
    durations: list[int] = []
    for trade in trades:
        try:
            open_ms = int(trade["OpenTime"])
            close_ms = int(trade["CloseTime"])
            duration_s = int(trade["Duration"])
        except (KeyError, TypeError, ValueError) as exc:
            raise ResearchModelsError("ml_features_invalid", "native trade is missing OpenTime/CloseTime/Duration") from exc
        opens.append(open_ms)
        closes.append(close_ms)
        durations.append(duration_s)
    return opens, closes, durations


def _estimator(family: dict[str, object]):
    module_name, _, class_name = str(family["estimator"]).rpartition(".")
    try:
        module = importlib.import_module(module_name)
        cls = getattr(module, class_name)
    except (ImportError, AttributeError) as exc:
        raise ResearchModelsError("ml_backend_not_installed", "allowlisted sklearn estimator is unavailable") from exc
    return cls(**family["params"])  # type: ignore[arg-type]


def _cv_split_count(n: int) -> int:
    return 2 if n < 24 else 3


def _purged_embargoed_cv(
    estimator,
    rows: list[list[float]],
    labels: list[int],
    opens: list[int],
    closes: list[int],
    durations: list[int],
) -> tuple[float, dict[str, object]]:
    from sklearn.base import clone

    n = len(rows)
    order = sorted(range(n), key=lambda index: (closes[index], index))
    X = [rows[index] for index in order]
    y = [labels[index] for index in order]
    open_ms = [opens[index] for index in order]
    close_ms = [closes[index] for index in order]
    duration_s = [durations[index] for index in order]
    k = _cv_split_count(n)
    fold_size = n // k
    tau_seconds = max(int(median(duration_s)) if duration_s else 0, _MIN_EMBARGO_SECONDS)
    tau_ms = tau_seconds * 1000
    fold_accuracies: list[float] = []
    folds: list[dict[str, object]] = []

    for fold in range(k):
        test_start = fold * fold_size
        test_end = n if fold == k - 1 else (fold + 1) * fold_size
        test_idx = list(range(test_start, test_end))
        if not test_idx:
            continue
        test_min_open = min(open_ms[index] for index in test_idx)
        test_max_close = max(close_ms[index] for index in test_idx)
        embargo_end = test_max_close + tau_ms
        train_idx: list[int] = []
        for index in range(n):
            if test_start <= index < test_end:
                continue
            if close_ms[index] > test_min_open and open_ms[index] < test_max_close:
                continue
            if test_min_open - tau_ms < close_ms[index] <= test_min_open:
                continue
            if test_max_close <= open_ms[index] < embargo_end:
                continue
            train_idx.append(index)
        y_train = [y[index] for index in train_idx]
        y_test = [y[index] for index in test_idx]
        if len(train_idx) < 2 or len(set(y_train)) < 2 or len(set(y_test)) < 2:
            folds.append(
                {
                    "fold": fold,
                    "train_n": len(train_idx),
                    "test_n": len(test_idx),
                    "skipped": True,
                }
            )
            continue
        clone_estimator = clone(estimator)
        clone_estimator.fit([X[index] for index in train_idx], y_train)
        accuracy = float(clone_estimator.score([X[index] for index in test_idx], y_test))
        fold_accuracies.append(accuracy)
        folds.append(
            {
                "fold": fold,
                "train_n": len(train_idx),
                "test_n": len(test_idx),
                "skipped": False,
                "accuracy": accuracy,
            }
        )

    if not fold_accuracies:
        raise ResearchModelsError(
            "ml_fit_failed",
            "purged/embargoed CV could not score any fold on these native trades",
        )
    mean_accuracy = sum(fold_accuracies) / len(fold_accuracies)
    return mean_accuracy, {
        "n_splits": k,
        "embargo": {
            "tau_seconds": tau_seconds,
            "unit": "median_duration",
        },
        "mean_accuracy": mean_accuracy,
        "fold_accuracies": fold_accuracies,
        "folds": folds,
    }


def _selection_record(
    store: FileResearchCustodyStore,
    *,
    historical_result_entity_id: str,
    sharpe: dict[str, object],
) -> dict[str, object]:
    prior = [
        item
        for item in _load_models(store.root)
        if item.get("historical_result_entity_id") == historical_result_entity_id
    ]
    trial_count = len(prior) + 1
    return {
        "trial_index": trial_count,
        "trial_count_on_result": trial_count,
        "deflated_sharpe_status": "computed" if sharpe.get("status") == "available" else "selection_count_unknown",
    }


def fit_model(
    store: FileResearchCustodyStore,
    *,
    family_id: str,
    historical_result_entity_id: str,
    expected_historical_result_revision: str,
) -> dict[str, object]:
    if not sklearn_available():
        raise ResearchModelsError("ml_backend_not_installed", "scikit-learn is not installed")
    family = _FAMILY_BY_ID.get(family_id)
    if family is None:
        raise ResearchModelsError("ml_family_unknown", "fit requires an allowlisted model family")
    try:
        trades_payload = read_historical_trades(
            store,
            historical_result_entity_id=historical_result_entity_id,
            expected_historical_result_revision=expected_historical_result_revision,
        )
    except (ResearchTradesError, ResearchRetesterError, ResearchCustodyError) as exc:
        raise ResearchModelsError(exc.code, exc.detail) from exc
    trades = trades_payload.get("trades")
    if not isinstance(trades, list):
        raise ResearchModelsError("ml_features_invalid", "Historical Result trades read model is invalid")
    rows, labels, pnl = _feature_matrix(trades)
    opens, closes, durations = _trade_clock(trades)
    estimator = _estimator(family)
    try:
        estimator.fit(rows, labels)
        train_accuracy = float(estimator.score(rows, labels))
        oos_accuracy, cv = _purged_embargoed_cv(estimator, rows, labels, opens, closes, durations)
    except ResearchModelsError:
        raise
    except ValueError as exc:
        raise ResearchModelsError(
            "ml_fit_failed",
            "the allowlisted estimator could not fit these native trades",
        ) from exc
    from io import BytesIO

    import joblib  # imported with sklearn

    buffer = BytesIO()
    joblib.dump(estimator, buffer)
    raw = buffer.getvalue()
    digest = sha256(raw).hexdigest()
    artifact = _artifact_dir(store.root) / f"{digest}.joblib"
    artifact.write_bytes(raw)
    expected_value = expected_value_record(pnl, window="full")
    sharpe = sharpe_record(pnl, window="full")
    record = {
        "family_id": family["family_id"],
        "label": family["label"],
        "estimator": family["estimator"],
        "scope": MODEL_SCOPE,
        "historical_result_entity_id": trades_payload["historical_result_entity_id"],
        "historical_result_revision": trades_payload["historical_result_revision"],
        "trade_count": trades_payload["trade_count"],
        "feature_names": list(FEATURE_NAMES),
        "label_rule": LABEL_RULE,
        "artifact_sha256": digest,
        "train_accuracy": train_accuracy,
        "oos_accuracy": oos_accuracy,
        "cv": cv,
        "expected_value": expected_value,
        "sharpe": sharpe,
        "selection": _selection_record(
            store,
            historical_result_entity_id=str(trades_payload["historical_result_entity_id"]),
            sharpe=sharpe,
        ),
    }
    models = [item for item in _load_models(store.root) if item.get("artifact_sha256") != digest]
    models.append(record)
    _write_models(store.root, models)
    return models_catalog(store.root)


def models_write(
    store: FileResearchCustodyStore | None,
    payload: dict[str, object],
) -> tuple[int, dict[str, object]]:
    if store is None:
        return 503, {
            "error": "unavailable",
            "reason_code": "research_store_not_bound",
            "detail": "Models require the application data root.",
        }
    if not isinstance(payload, dict) or payload.get("action") != "fit":
        return 400, {
            "error": "invalid_request",
            "reason_code": "ml_action_invalid",
            "detail": "models writes accept action=fit",
        }
    expected = {"action", "family_id", "historical_result_entity_id", "expected_historical_result_revision"}
    if set(payload) != expected or not all(isinstance(payload[key], str) and payload[key] for key in expected if key != "action"):
        return 400, {
            "error": "invalid_request",
            "reason_code": "ml_fit_identity_invalid",
            "detail": "fit requires family_id and one exact Historical Result identity",
        }
    try:
        return 200, fit_model(
            store,
            family_id=str(payload["family_id"]),
            historical_result_entity_id=str(payload["historical_result_entity_id"]),
            expected_historical_result_revision=str(payload["expected_historical_result_revision"]),
        )
    except ResearchModelsError as exc:
        return 409, {"error": "invalid_state", "reason_code": exc.code, "detail": exc.detail}
