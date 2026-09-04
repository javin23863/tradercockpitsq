"""Call the running StrategyQuant X local web server as the Electron UI does.

Electron injects header ``browserToken`` from ``user/settings/settings.xml``.
TraderCockpit never returns that token to the browser. This is native producer
invocation, not a substitute code generator.
"""

from __future__ import annotations

from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from xml.etree import ElementTree
import json

from .sqx_custom_project import SqxCustomProjectTopologyError, _verified_home
from .sqx_presets import SQX_BUILD


SQX_SETTINGS_RELATIVE = Path("user") / "settings" / "settings.xml"
SQX_NATIVE_WEB_TIMEOUT_SECONDS = 60.0


class SqxNativeWebError(RuntimeError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(detail)
        self.code = code
        self.detail = detail


def read_sqx_local_web_session(sqx_home: Path | str | None) -> tuple[Path, int, str]:
    home = _verified_home(sqx_home)
    settings_path = home / SQX_SETTINGS_RELATIVE
    try:
        resolved = settings_path.resolve()
        resolved.relative_to(home.resolve())
    except (OSError, RuntimeError, ValueError) as exc:
        raise SqxCustomProjectTopologyError(
            "custom_project_path_escape",
            "SQX settings.xml resolves outside the verified runtime",
        ) from exc
    if resolved.is_symlink() or not resolved.is_file():
        raise SqxNativeWebError(
            "sqx_web_settings_missing",
            "StrategyQuant X user/settings/settings.xml is missing.",
        )
    try:
        root = ElementTree.parse(resolved).getroot()
    except ElementTree.ParseError as exc:
        raise SqxNativeWebError(
            "sqx_web_settings_invalid",
            "StrategyQuant X settings.xml could not be parsed.",
        ) from exc
    port_text = (root.findtext("WebServerPortUsed") or "").strip()
    token = (root.findtext("BrowserToken") or "").strip()
    try:
        port = int(port_text)
    except ValueError as exc:
        raise SqxNativeWebError(
            "sqx_web_port_invalid",
            "StrategyQuant X WebServerPortUsed must be a TCP port.",
        ) from exc
    if port < 1 or port > 65535:
        raise SqxNativeWebError(
            "sqx_web_port_invalid",
            "StrategyQuant X WebServerPortUsed must be a TCP port.",
        )
    if not token or any(ch.isspace() for ch in token) or len(token) > 64:
        raise SqxNativeWebError(
            "sqx_web_token_invalid",
            "StrategyQuant X BrowserToken is missing.",
        )
    return home, port, token


def sqx_local_json(
    sqx_home: Path | str | None,
    path: str,
    *,
    method: str = "GET",
    fields: dict[str, str] | None = None,
    timeout: float = SQX_NATIVE_WEB_TIMEOUT_SECONDS,
    opener=urlopen,
) -> dict[str, object]:
    if not isinstance(path, str) or not path.startswith("/") or ".." in path or "://" in path:
        raise SqxNativeWebError("sqx_web_path_invalid", "SQX local web path is not a single servlet path.")
    _home, port, token = read_sqx_local_web_session(sqx_home)
    headers = {"browserToken": token, "Accept": "application/json"}
    body = None
    verb = method.upper()
    url = f"http://127.0.0.1:{port}{path}"
    if verb == "POST":
        encoded = urlencode(fields or {}).encode("utf-8")
        body = encoded
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    elif fields:
        url = f"{url}?{urlencode(fields)}"
    request = Request(
        url,
        data=body,
        headers=headers,
        method=verb,
    )
    try:
        with opener(request, timeout=timeout) as response:
            raw = response.read()
            status = getattr(response, "status", 200)
    except HTTPError as exc:
        raw = exc.read()
        status = exc.code
        if status == 401:
            raise SqxNativeWebError(
                "sqx_web_unauthorized",
                "StrategyQuant X local web refused the desktop browserToken.",
            ) from exc
        try:
            payload = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError, AttributeError):
            payload = {}
        if isinstance(payload, dict) and payload.get("error"):
            raise SqxNativeWebError("sqx_web_refused", str(payload.get("error"))) from exc
        raise SqxNativeWebError(
            "sqx_web_refused",
            f"StrategyQuant X local web returned HTTP {status}.",
        ) from exc
    except URLError as exc:
        raise SqxNativeWebError(
            "sqx_web_unavailable",
            "StrategyQuant X local web is not reachable. Keep StrategyQuant X open.",
        ) from exc
    try:
        payload = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise SqxNativeWebError(
            "sqx_web_invalid_response",
            "StrategyQuant X local web did not return JSON.",
        ) from exc
    if not isinstance(payload, dict):
        raise SqxNativeWebError(
            "sqx_web_invalid_response",
            "StrategyQuant X local web did not return a JSON object.",
        )
    if status >= 400:
        raise SqxNativeWebError(
            "sqx_web_refused",
            str(payload.get("error") or f"StrategyQuant X local web returned HTTP {status}."),
        )
    payload.setdefault("source_build", SQX_BUILD)
    return payload
