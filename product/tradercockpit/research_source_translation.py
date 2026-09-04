"""Deliver native SQX strategies to platforms SQX cannot compile for (TradingView, Python).

StrategyQuant X natively prints Pseudo Code, Strategy XML, MQL4, MQL5 and EasyLanguage through
``sourcecode/print``; Pine Script and Python are not native outputs. The vendor's own route is
the Source Code Translator Results plugin, which sends the native source to an LLM. This module
does the equivalent server-side through the bounded assistant transport:

- the exact native Pseudo Code and Strategy XML are read from the producer first (fail closed
  when StrategyQuant X is not running);
- the translation is stored immutably, bound to the SHA-256 of the native source it came from;
- the record is always ``unverified_translation``. TraderCockpit never backtests it and never
  presents it as producer-verified; the owner validates it in the target platform.

No quantitative behaviour is invented here: the code is a derived artefact of the assistant.
"""

from __future__ import annotations

from datetime import datetime, timezone
from hashlib import sha256
import json
from pathlib import Path
import re

from .assistant import AssistantError, assistant_policy, request_completion
from .sqx_custom_project import SqxCustomProjectTopologyError
from .sqx_native_web import SqxNativeWebError
from .sqx_presets import SQX_BUILD
from .sqx_sourcecode import print_sourcecode


RESEARCH_SOURCE_TRANSLATION_SCHEMA = "tc.research-source-translation.v1"
RESEARCH_SOURCE_TRANSLATION_CATALOG_SCHEMA = "tc.research-source-translation-catalog.v1"
RESEARCH_SOURCE_TRANSLATION_API_PATH = "/api/research/source-translation"
TRANSLATION_STATUS = "unverified_translation"
NATIVE_SOURCE_FORMAT = "pseudo"
TRANSLATION_OUTPUT_TOKENS = 12000
_NATIVE_SOURCE_MAX_CHARS = 120_000
_NAME_MAX = 256
_NAME = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 ._()+-]*$")
_FENCE = re.compile(r"^\s*```[A-Za-z0-9_+-]*\s*\n(.*?)\n\s*```\s*$", re.DOTALL)

# Backend-owned delivery targets. Labels follow the vendor translator plugin's option names so
# the owner sees the same vocabulary inside StrategyQuant X and inside TraderCockpit.
DELIVERY_TARGETS: tuple[dict[str, str], ...] = (
    {
        "id": "pine_v6",
        "label": "Pine Script v6 (TradingView)",
        "platform": "TradingView",
        "language": "Pine Script v6",
        "extension": "pine",
        "verify_in": "TradingView Strategy Tester",
    },
    {
        "id": "python_backtrader",
        "label": "Python (backtrader)",
        "platform": "Python",
        "language": "Python 3 / backtrader",
        "extension": "py",
        "verify_in": "a backtrader run on the same history data",
    },
    {
        "id": "python_zipline",
        "label": "Python (Zipline)",
        "platform": "Python",
        "language": "Python 3 / Zipline",
        "extension": "py",
        "verify_in": "a Zipline run on the same history data",
    },
)
_TARGETS_BY_ID = {item["id"]: item for item in DELIVERY_TARGETS}


class ResearchSourceTranslationError(ValueError):
    def __init__(self, code: str, detail: str, *, status: int = 409) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail
        self.status = status


def _translation_dir(data_root: Path) -> Path:
    return Path(data_root) / "research" / "source-translations"


def _sha256_text(text: str) -> str:
    return sha256(text.encode("utf-8")).hexdigest()


def _now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def _name(value: object, field: str) -> str:
    if not isinstance(value, str) or not value or len(value) > _NAME_MAX or not _NAME.match(value):
        raise ResearchSourceTranslationError(
            "source_translation_identity_invalid",
            f"{field} must name one exact native project/databank/archive.",
            status=400,
        )
    return value


def _target(value: object) -> dict[str, str]:
    if not isinstance(value, str) or value not in _TARGETS_BY_ID:
        raise ResearchSourceTranslationError(
            "source_translation_target_invalid",
            "target must be one backend delivery target id.",
            status=400,
        )
    return _TARGETS_BY_ID[value]


def _strip_fence(text: str) -> str:
    match = _FENCE.match(text)
    return match.group(1).strip() if match else text.strip()


def _system_prompt(target: dict[str, str]) -> str:
    return (
        "You translate an algorithmic trading strategy that StrategyQuant X generated into "
        f"{target['language']} for {target['platform']}.\n"
        "Rules:\n"
        "- Translate the rules exactly as written in the native Pseudo Code and Strategy XML: same "
        "indicators, periods, shifts, comparisons, order types, stop loss, profit target, trailing, "
        "time exits, trading options and position sizing.\n"
        "- Do not add filters, parameters, indicators, risk controls or optimisations that are not in "
        "the source.\n"
        "- If a construct cannot be expressed faithfully in the target, keep the surrounding logic and "
        "mark the exact gap with a comment starting with 'TC-UNTRANSLATABLE:' explaining what is missing. "
        "Never approximate silently.\n"
        "- Indicator shift N means the value N completed bars ago; StrategyQuant evaluates conditions on "
        "bar open using values from the previous completed bar unless the shift says otherwise.\n"
        "- Begin the output with a header comment stating: translated from StrategyQuant X native source, "
        "unverified, must be backtested in the target platform before use.\n"
        "- Output only the complete source file. No prose, no markdown fences."
    )


def _user_prompt(strategy_name: str, pseudo: str, strategy_xml: str) -> str:
    return (
        f"Strategy name: {strategy_name}\n\n"
        "=== Native Pseudo Code (StrategyQuant X sourcecode/print) ===\n"
        f"{pseudo}\n\n"
        "=== Native Strategy XML (strategy_Portfolio.xml) ===\n"
        f"{strategy_xml}\n"
    )


def _native_source(sqx_home: object, project: str, databank: str, archive: str) -> dict[str, object]:
    identity = {"project": project, "databank": databank, "archive": archive}
    try:
        pseudo = print_sourcecode(sqx_home, {**identity, "format": NATIVE_SOURCE_FORMAT})
        xml = print_sourcecode(sqx_home, {**identity, "format": "xml"})
    except SqxNativeWebError as exc:
        raise ResearchSourceTranslationError(
            "source_translation_native_unavailable",
            f"StrategyQuant X must be running to print the native Pseudo Code: {exc.detail}",
            status=503,
        ) from exc
    except SqxCustomProjectTopologyError as exc:
        raise ResearchSourceTranslationError(exc.code, exc.detail, status=404 if "missing" in exc.code else 400) from exc
    pseudo_code = pseudo.get("code") if isinstance(pseudo.get("code"), str) else ""
    xml_code = xml.get("code") if isinstance(xml.get("code"), str) else ""
    if not pseudo_code.strip():
        raise ResearchSourceTranslationError(
            "source_translation_native_empty",
            "StrategyQuant X returned no Pseudo Code for this strategy.",
            status=502,
        )
    if len(pseudo_code) + len(xml_code) > _NATIVE_SOURCE_MAX_CHARS:
        raise ResearchSourceTranslationError(
            "source_translation_native_too_large",
            "The native source exceeds the bounded translation input.",
            status=413,
        )
    return {
        "format": NATIVE_SOURCE_FORMAT,
        "type": pseudo.get("type"),
        "producer": pseudo.get("producer"),
        "source_build": SQX_BUILD,
        "pseudo_code": pseudo_code,
        "pseudo_sha256": _sha256_text(pseudo_code),
        "strategy_xml": xml_code,
        "strategy_xml_sha256": _sha256_text(xml_code),
        "strategy_name": Path(archive).stem,
        **identity,
    }


def _record_id(target_id: str, native_sha: str, xml_sha: str, code: str, created: str) -> str:
    return _sha256_text("|".join((target_id, native_sha, xml_sha, code, created)))[:32]


def translate_native_source(
    sqx_home: object,
    data_root: Path | str | None,
    payload: dict[str, object],
    *,
    environ: dict[str, str] | None = None,
    transport=None,
) -> dict[str, object]:
    if data_root is None:
        raise ResearchSourceTranslationError(
            "research_store_not_bound",
            "Source translations require the application data root.",
            status=503,
        )
    if not isinstance(payload, dict) or set(payload) != {"project", "databank", "archive", "target"}:
        raise ResearchSourceTranslationError(
            "source_translation_fields_invalid",
            "translation requires exactly project, databank, archive, and target.",
            status=400,
        )
    project = _name(payload.get("project"), "project")
    databank = _name(payload.get("databank"), "databank")
    archive = _name(payload.get("archive"), "archive")
    target = _target(payload.get("target"))
    native = _native_source(sqx_home, project, databank, archive)
    messages = [
        {"role": "system", "content": _system_prompt(target)},
        {"role": "user", "content": _user_prompt(str(native["strategy_name"]), str(native["pseudo_code"]), str(native["strategy_xml"]))},
    ]
    try:
        completion = request_completion(
            messages,
            environ=environ,
            transport=transport,
            tools_enabled=False,
            temperature=0.0,
            max_output_tokens=TRANSLATION_OUTPUT_TOKENS,
        )
    except AssistantError as exc:
        raise ResearchSourceTranslationError(exc.code, exc.detail, status=exc.status) from exc
    reply = completion.get("reply")
    if not isinstance(reply, str) or not reply.strip():
        raise ResearchSourceTranslationError(
            "source_translation_empty",
            "The assistant returned no translation.",
            status=502,
        )
    code = _strip_fence(reply)
    created = _now()
    record = {
        "schema": RESEARCH_SOURCE_TRANSLATION_SCHEMA,
        "id": _record_id(target["id"], str(native["pseudo_sha256"]), str(native["strategy_xml_sha256"]), code, created),
        "status": TRANSLATION_STATUS,
        "created_at": created,
        "target": dict(target),
        "native": {
            "project": project,
            "databank": databank,
            "archive": archive,
            "strategy_name": native["strategy_name"],
            "format": native["format"],
            "type": native["type"],
            "producer": native["producer"],
            "source_build": SQX_BUILD,
            "pseudo_sha256": native["pseudo_sha256"],
            "strategy_xml_sha256": native["strategy_xml_sha256"],
        },
        "model": {
            "requested": completion.get("requested_model"),
            "used": completion.get("model"),
            "fallback_used": bool(completion.get("fallback_used")),
            "usage": completion.get("usage"),
            "provider_request_id": completion.get("provider_request_id"),
        },
        "code_sha256": _sha256_text(code),
        "code": code,
        "untranslatable_markers": code.count("TC-UNTRANSLATABLE:"),
        "verification": {
            "state": "not_verified",
            "detail": (
                f"Derived by the assistant from native Pseudo Code {str(native['pseudo_sha256'])[:12]}…. "
                f"Backtest it in {target['verify_in']} before use; TraderCockpit does not run it and "
                "does not claim it reproduces the StrategyQuant X result."
            ),
        },
    }
    directory = _translation_dir(Path(data_root))
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{record['id']}.json"
    if path.exists():
        raise ResearchSourceTranslationError(
            "source_translation_exists",
            "This exact translation is already stored.",
            status=409,
        )
    tmp = path.with_suffix(".json.tmp")
    tmp.write_text(json.dumps(record, indent=2, sort_keys=True), encoding="utf-8")
    tmp.replace(path)
    return record


def _load_records(data_root: Path) -> list[dict[str, object]]:
    directory = _translation_dir(data_root)
    if not directory.is_dir():
        return []
    records: list[dict[str, object]] = []
    for path in sorted(directory.glob("*.json")):
        try:
            item = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(item, dict) and item.get("schema") == RESEARCH_SOURCE_TRANSLATION_SCHEMA:
            records.append(item)
    records.sort(key=lambda item: str(item.get("created_at") or ""), reverse=True)
    return records


def source_translation_catalog(
    data_root: Path | str | None,
    *,
    project: str | None = None,
    databank: str | None = None,
    archive: str | None = None,
    environ: dict[str, str] | None = None,
) -> dict[str, object]:
    policy = assistant_policy(environ)
    translations: list[dict[str, object]] = []
    if data_root is not None:
        for item in _load_records(Path(data_root)):
            native = item.get("native") if isinstance(item.get("native"), dict) else {}
            if project is not None and native.get("project") != project:
                continue
            if databank is not None and native.get("databank") != databank:
                continue
            if archive is not None and native.get("archive") != archive:
                continue
            translations.append(item)
    return {
        "schema": RESEARCH_SOURCE_TRANSLATION_CATALOG_SCHEMA,
        "status": TRANSLATION_STATUS,
        "native_source_format": NATIVE_SOURCE_FORMAT,
        "native_targets": [
            {"id": "mq4", "label": "Expert Advisor for MetaTrader4 (*.MQ4)", "producer": "sqx_local_web"},
            {"id": "mq5", "label": "Expert Advisor for MetaTrader5 (*.MQ5)", "producer": "sqx_local_web"},
        ],
        "translation_targets": [dict(item) for item in DELIVERY_TARGETS],
        "assistant": {
            "configured": policy["configured"],
            "model": policy["model"],
            "provider": policy["provider"],
        },
        "data_root_bound": data_root is not None,
        "translations": translations,
    }


def source_translation_read_response(
    data_root: Path | str | None,
    query: dict[str, list[str]],
    *,
    environ: dict[str, str] | None = None,
) -> tuple[int, dict[str, object]]:
    allowed = {"project", "databank", "archive"}
    if set(query) - allowed:
        return 400, {"error": "invalid_request", "detail": "source translation catalog accepts project, databank, archive."}
    picked: dict[str, str | None] = {}
    for key in allowed:
        values = query.get(key)
        if values is None:
            picked[key] = None
            continue
        if len(values) != 1:
            return 400, {"error": "invalid_request", "detail": f"{key} must be given once."}
        try:
            picked[key] = _name(values[0], key)
        except ResearchSourceTranslationError as exc:
            return exc.status, {"error": "invalid_request", "reason_code": exc.code, "detail": exc.detail}
    return 200, source_translation_catalog(data_root, environ=environ, **picked)


def source_translation_write_response(
    sqx_home: object,
    data_root: Path | str | None,
    payload: object,
    *,
    environ: dict[str, str] | None = None,
    transport=None,
) -> tuple[int, dict[str, object]]:
    if not isinstance(payload, dict):
        return 400, {"error": "invalid_request", "detail": "translation body must be a JSON object."}
    try:
        return 200, translate_native_source(sqx_home, data_root, payload, environ=environ, transport=transport)
    except ResearchSourceTranslationError as exc:
        error = "invalid_request" if exc.status == 400 else "not_found" if exc.status == 404 else "unavailable" if exc.status == 503 else "invalid_state"
        return exc.status, {"error": error, "reason_code": exc.code, "detail": exc.detail}
