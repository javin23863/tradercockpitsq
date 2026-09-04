"""Native SQX Results overview HTML via overview/getOverviewContent.

SQ Default and the other snippet templates are filled by StrategyQuant X from the
live databank ResultsGroup. TraderCockpit does not invent those SQStats columns.
If the databank file is on disk but not loaded, this asks SQX to load that exact
archive (native project/loadFilesToDatabank) and then requests the HTML.
"""

from __future__ import annotations

from time import sleep

from .sqx_custom_project import SqxCustomProjectTopologyError
from .sqx_custom_project_strategy import _resolved_strategy_archive
from .sqx_native_web import SqxNativeWebError, sqx_local_json
from .sqx_presets import SQX_BUILD


SQX_OVERVIEW_SCHEMA = "tc.sqx-overview.v1"
SQX_OVERVIEW_API_PATH = "/api/sqx-overview"
OVERVIEW_CONTENT_PATH = "/overview/getOverviewContent"
DATA_ITEMS_PATH = "/project/getDataItems"
LOAD_FILES_PATH = "/project/loadFilesToDatabank"

OVERVIEW_TEMPLATES = frozenset(
    {
        "DefaultOpenDD",
        "SQDefault",
        "SQDefaultPct",
        "SQDefaultWithPortfolio",
        "SQDefaultWithPortfolioPct",
        "TSOverview",
    }
)
SAMPLE_TO_NATIVE = {"full": "127", "is": "10", "oos": "20"}
DIRECTION_TO_NATIVE = {"both": "0", "long": "1", "short": "-1"}
OVERVIEW_QUERY = frozenset({"project", "databank", "archive", "template", "sample", "direction"})
_LOAD_ATTEMPTS = 20
_LOAD_WAIT_SECONDS = 0.4


def _strategy_name(archive: str) -> str:
    return archive[:-4] if archive.lower().endswith(".sqx") else archive


def _result_listed(payload: dict[str, object]) -> bool:
    if payload.get("strDoesntExist") is True:
        return False
    if payload.get("error"):
        return False
    return bool(payload.get("success") or payload.get("dataItems"))


def ensure_databank_result(
    sqx_home: object,
    project: str,
    databank: str,
    archive: str,
    *,
    sleeper=sleep,
) -> None:
    name = _strategy_name(archive)
    listed = sqx_local_json(
        sqx_home,
        DATA_ITEMS_PATH,
        fields={"projectName": project, "databankName": databank, "reportName": name},
    )
    if _result_listed(listed):
        return
    path = _resolved_strategy_archive(sqx_home, project, databank, archive)
    loaded = sqx_local_json(
        sqx_home,
        LOAD_FILES_PATH,
        method="POST",
        fields={
            "projectName": project,
            "databankName": databank,
            "clear": "false",
            "filePaths[]": str(path),
        },
    )
    if loaded.get("error"):
        raise SqxNativeWebError("sqx_web_refused", str(loaded.get("error")))
    for _ in range(_LOAD_ATTEMPTS):
        listed = sqx_local_json(
            sqx_home,
            DATA_ITEMS_PATH,
            fields={"projectName": project, "databankName": databank, "reportName": name},
        )
        if _result_listed(listed):
            return
        sleeper(_LOAD_WAIT_SECONDS)
    raise SqxNativeWebError(
        "sqx_web_refused",
        f"StrategyQuant X did not load '{name}' into databank '{databank}'.",
    )


def overview_html(
    sqx_home: object,
    *,
    project: str,
    databank: str,
    archive: str,
    template: str = "SQDefault",
    sample: str = "full",
    direction: str = "both",
    sleeper=sleep,
) -> dict[str, object]:
    if template not in OVERVIEW_TEMPLATES:
        raise SqxCustomProjectTopologyError(
            "overview_fields_invalid",
            "Overview template must be a native Results overview snippet id.",
        )
    native_sample = SAMPLE_TO_NATIVE.get(sample)
    native_direction = DIRECTION_TO_NATIVE.get(direction)
    if native_sample is None or native_direction is None:
        raise SqxCustomProjectTopologyError(
            "overview_fields_invalid",
            "Overview sample must be full, is, or oos and direction both, long, or short.",
        )
    name = _strategy_name(archive)
    try:
        ensure_databank_result(sqx_home, project, databank, archive, sleeper=sleeper)
        payload = sqx_local_json(
            sqx_home,
            OVERVIEW_CONTENT_PATH,
            fields={
                "projectName": project,
                "databankName": databank,
                "strategyName": name,
                "reportName": name,
                "resultKey": "Portfolio",
                "direction": native_direction,
                "sampleType": native_sample,
                "template": template,
            },
        )
    except SqxNativeWebError as exc:
        return {
            "schema": SQX_OVERVIEW_SCHEMA,
            "source_build": SQX_BUILD,
            "producer": "unavailable",
            "template": template,
            "overviewHtml": "",
            "reason_code": exc.code,
            "detail": exc.detail,
        }
    html = payload.get("overviewHtml")
    error = payload.get("error")
    if not isinstance(html, str) or not html:
        return {
            "schema": SQX_OVERVIEW_SCHEMA,
            "source_build": SQX_BUILD,
            "producer": "sqx_local_web",
            "template": template,
            "overviewHtml": "",
            "reason_code": "overview_unavailable",
            "detail": str(error) if error else "StrategyQuant X returned no overview HTML.",
        }
    return {
        "schema": SQX_OVERVIEW_SCHEMA,
        "source_build": SQX_BUILD,
        "producer": "sqx_local_web",
        "template": template,
        "overviewHtml": html,
        "reason_code": None,
        "detail": None,
    }
