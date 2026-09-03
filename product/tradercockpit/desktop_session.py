"""Last registered desktop route. Launch restore only; not a second product spine."""

from __future__ import annotations

from json import JSONDecodeError, dumps, loads
import os
from pathlib import Path
from urllib.parse import parse_qsl, urlencode, urlsplit

from tradercockpit.research_custody import EvidenceRef, ResearchCustodyError, ResearchEntityId, ResearchKind

DESKTOP_SESSION_SCHEMA = "tc.desktop-session.v1"
DESKTOP_SESSION_API_PATH = "/api/desktop/session"
DESKTOP_SESSION_FILE = "desktop-session.json"
DEFAULT_SESSION_PATH = "/home"

_PRODUCT_PATHS = frozenset({
    "/home",
    "/builder",
    "/retester",
    "/optimizer",
    "/data-manager",
    "/custom-projects",
    "/algowizard",
    "/operate",
    "/settings",
    "/research",
})
_LEGACY_PATHS = {
    "/explore": "/home",
    "/automation": "/custom-projects",
}
_RESEARCH_TABS = {
    "signals": (
        "overview",
        "signals",
        "order-flow",
        "footprint",
        "volume-profile",
        "liquidity-map",
        "replays",
        "alerts",
        "reports",
    ),
    "evolution": (),
    "validate": ("overview", "initial-test", "trades", "robustness", "configuration", "evidence"),
    "catalog": ("all", "indicators", "models", "strategies", "utilities", "mine"),
}
_IDENTITY_KINDS = {
    "configuration": ResearchKind.CONFIGURATION,
    "proofEntity": ResearchKind.PROOF,
    "historicalResult": ResearchKind.HISTORICAL_RESULT,
}


class DesktopSessionError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def canonicalize_desktop_path(value: str) -> str:
    """Return one registered product path, keeping only registered Research identity keys."""

    if not isinstance(value, str) or not value.startswith("/") or "\\" in value or value.startswith("//"):
        raise DesktopSessionError("desktop_path_invalid", "desktop path must be a registered product route")
    parsed = urlsplit(value)
    if parsed.scheme or parsed.netloc or parsed.fragment:
        raise DesktopSessionError("desktop_path_invalid", "desktop path must not contain a scheme, host, or fragment")
    pathname = parsed.path.rstrip("/") or "/home"
    if pathname in _LEGACY_PATHS:
        return _LEGACY_PATHS[pathname]
    if pathname == "/research" and not parsed.query:
        return "/builder"
    if pathname not in _PRODUCT_PATHS:
        raise DesktopSessionError("desktop_path_invalid", "desktop path is not a registered product surface")

    pairs = parse_qsl(parsed.query, keep_blank_values=True)
    keys = [key for key, _ in pairs]
    if len(keys) != len(set(keys)):
        raise DesktopSessionError("desktop_path_invalid", "desktop path cannot repeat query keys")

    allowed = {"workspace", "tab", "configuration", "proofEntity", "validationRef", "historicalResult"}
    if set(keys) - allowed:
        raise DesktopSessionError("desktop_path_invalid", "desktop path has an unsupported query key")
    if pathname != "/research" and keys:
        raise DesktopSessionError("desktop_path_invalid", "only Research may carry selection query keys")

    params: dict[str, str] = {}
    if pathname == "/research":
        workspace = _single(pairs, "workspace") or "signals"
        tabs = _RESEARCH_TABS.get(workspace)
        if tabs is None:
            raise DesktopSessionError("desktop_path_invalid", "Research workspace is not registered")
        params["workspace"] = workspace
        if tabs:
            tab = _single(pairs, "tab") or next(iter(tabs))
            if tab not in tabs:
                raise DesktopSessionError("desktop_path_invalid", "Research tab is not registered")
            params["tab"] = tab
        elif _single(pairs, "tab"):
            raise DesktopSessionError("desktop_path_invalid", "Evolutionary Search has no tab row")
        _copy_identity(pairs, params)

    query = urlencode(params)
    return f"{pathname}?{query}" if query else pathname


def session_record(path: str) -> dict[str, object]:
    return {"schema": DESKTOP_SESSION_SCHEMA, "path": canonicalize_desktop_path(path)}


def read_desktop_session(data_root: Path | str | None) -> dict[str, object]:
    default = session_record(DEFAULT_SESSION_PATH)
    if data_root is None:
        return default
    path = Path(data_root) / DESKTOP_SESSION_FILE
    try:
        payload = loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return default
    except (OSError, UnicodeDecodeError, JSONDecodeError):
        return default
    if not isinstance(payload, dict) or payload.get("schema") != DESKTOP_SESSION_SCHEMA:
        return default
    try:
        return session_record(str(payload.get("path") or ""))
    except DesktopSessionError:
        return default


def write_desktop_session(data_root: Path | str, path: str) -> dict[str, object]:
    record = session_record(path)
    root = Path(data_root)
    root.mkdir(parents=True, exist_ok=True)
    target = root / DESKTOP_SESSION_FILE
    payload = dumps(record, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    temporary = target.with_name(f".{target.name}.tmp-{os.getpid()}")
    try:
        temporary.write_text(payload, encoding="utf-8")
        os.replace(temporary, target)
    finally:
        try:
            temporary.unlink()
        except FileNotFoundError:
            pass
    return record


def _single(pairs: list[tuple[str, str]], key: str) -> str:
    return next((value for item, value in pairs if item == key), "")


def _copy_identity(pairs: list[tuple[str, str]], params: dict[str, str]) -> None:
    for key, kind in _IDENTITY_KINDS.items():
        value = _single(pairs, key)
        if not value:
            continue
        try:
            entity = ResearchEntityId.parse(value)
        except ResearchCustodyError as exc:
            raise DesktopSessionError("desktop_path_invalid", exc.detail) from exc
        if entity.kind != kind:
            raise DesktopSessionError("desktop_path_invalid", f"{key} is not a {kind.value} identity")
        params[key] = str(entity)
    validation = _single(pairs, "validationRef")
    if not validation:
        return
    try:
        params["validationRef"] = str(EvidenceRef.parse(validation))
    except ResearchCustodyError as exc:
        raise DesktopSessionError("desktop_path_invalid", exc.detail) from exc
