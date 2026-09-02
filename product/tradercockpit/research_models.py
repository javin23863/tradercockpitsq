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

from tradercockpit.research_custody import FileResearchCustodyStore
from tradercockpit.research_trades import ResearchTradesError, read_historical_trades


RESEARCH_MODELS_SCHEMA = "tc.research-ml-model-catalog.v1"
RESEARCH_MODELS_API_PATH = "/api/research/models"
FEATURE_NAMES = ("Duration", "MAE", "MFE", "PipsPL")
LABEL_RULE = "producer_pl_positive"
_CATALOG_NAME = "ml-models.json"
_ARTIFACT_DIR = "ml-models"
_MIN_TRADES = 2

FAMILIES = (
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
    path = _catalog_path(data_root)
    path.write_text(
        json.dumps({"schema": RESEARCH_MODELS_SCHEMA, "models": models}, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def models_catalog(data_root: Path | str | None) -> dict[str, object]:
    available = sklearn_available()
    models = _load_models(Path(data_root)) if data_root is not None else []
    return {
        "schema": RESEARCH_MODELS_SCHEMA,
        "backend": "sklearn",
        "backend_available": available,
        "reason_code": None if available else "ml_backend_not_installed",
        "feature_names": list(FEATURE_NAMES),
        "label_rule": LABEL_RULE,
        "families": [dict(item) for item in FAMILIES],
        "models": models,
        "detail": (
            "Allowlisted sklearn classifiers fit on native SQX trade records from one completed Historical Result. "
            "SQX still owns backtest and robustness. Reopen Candidates only after a later custody bind."
            if available
            else "Install the ml extra (scikit-learn) to fit Models. The browser never chooses an estimator path."
        ),
    }


def _feature_matrix(trades: list[dict[str, object]]) -> tuple[list[list[float]], list[int]]:
    rows: list[list[float]] = []
    labels: list[int] = []
    for trade in trades:
        try:
            rows.append([float(trade[name]) for name in FEATURE_NAMES])
            labels.append(1 if float(trade["PL"]) > 0 else 0)
        except (KeyError, TypeError, ValueError) as exc:
            raise ResearchModelsError("ml_features_invalid", "native trade is missing a required producer field") from exc
    if len(rows) < _MIN_TRADES:
        raise ResearchModelsError("ml_trades_insufficient", f"fitting requires at least {_MIN_TRADES} native trades")
    return rows, labels


def _estimator(family: dict[str, object]):
    module_name, _, class_name = str(family["estimator"]).rpartition(".")
    try:
        module = importlib.import_module(module_name)
        cls = getattr(module, class_name)
    except (ImportError, AttributeError) as exc:
        raise ResearchModelsError("ml_backend_not_installed", "allowlisted sklearn estimator is unavailable") from exc
    return cls(**family["params"])  # type: ignore[arg-type]


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
    except ResearchTradesError as exc:
        raise ResearchModelsError(exc.code, exc.detail) from exc
    trades = trades_payload.get("trades")
    if not isinstance(trades, list):
        raise ResearchModelsError("ml_features_invalid", "Historical Result trades read model is invalid")
    rows, labels = _feature_matrix(trades)
    estimator = _estimator(family)
    estimator.fit(rows, labels)
    from io import BytesIO

    import joblib  # imported with sklearn

    buffer = BytesIO()
    joblib.dump(estimator, buffer)
    raw = buffer.getvalue()
    digest = sha256(raw).hexdigest()
    artifact = _artifact_dir(store.root) / f"{digest}.joblib"
    artifact.write_bytes(raw)
    record = {
        "family_id": family["family_id"],
        "label": family["label"],
        "estimator": family["estimator"],
        "historical_result_entity_id": trades_payload["historical_result_entity_id"],
        "historical_result_revision": trades_payload["historical_result_revision"],
        "trade_count": trades_payload["trade_count"],
        "feature_names": list(FEATURE_NAMES),
        "label_rule": LABEL_RULE,
        "artifact_sha256": digest,
        "train_accuracy": float(estimator.score(rows, labels)),
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
