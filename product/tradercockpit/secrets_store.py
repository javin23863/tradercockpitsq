"""Operator/provider credential store outside the application data root.

Secrets load from a configured file path or the process environment. Browser code
and JSON read models receive readiness only — never secret values or prefixes.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path
from typing import Mapping

TRADERCOCKPIT_SECRETS_PATH_ENV = "TRADERCOCKPIT_SECRETS_PATH"
SECRETS_STATUS_SCHEMA = "tc.secrets-status.v1"

SECRET_GROUPS: tuple[dict[str, object], ...] = (
    {
        "id": "openrouter",
        "label": "OpenRouter",
        "credential_scope": "operator",
        "any_of": ("OPENROUTER_API_KEY", "OPENROUTER_MANAGEMENT_KEY"),
    },
    {
        "id": "google",
        "label": "Google OAuth",
        "credential_scope": "operator",
        "all_of": ("GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"),
    },
    {
        "id": "stripe",
        "label": "Stripe membership",
        "credential_scope": "operator",
        "all_of": ("STRIPE_SECRET_KEY", "STRIPE_PRICE_ID"),
    },
    {
        "id": "schwab",
        "label": "Schwab Market Data",
        "credential_scope": "operator",
        "all_of": ("SCHWAB_CLIENT_ID", "SCHWAB_CLIENT_SECRET"),
    },
    {
        "id": "metatrader",
        "label": "MetaTrader 5",
        "credential_scope": "consumer",
        "all_of": ("MT5_LOGIN", "MT5_PASSWORD", "MT5_SERVER"),
    },
    {
        "id": "tradingview",
        "label": "TradingView market data",
        "credential_scope": "consumer",
        "any_of": ("TRADINGVIEW_MARKET_DATA",),
    },
    {
        "id": "fred",
        "label": "FRED macro series",
        "credential_scope": "operator",
        "all_of": ("FRED_API_KEY",),
    },
    {
        "id": "market",
        "label": "Finnhub market quotes",
        "credential_scope": "operator",
        "all_of": ("TRADERCOCKPIT_MARKET_API_KEY",),
    },
)


class SecretsStoreError(ValueError):
    def __init__(self, code: str, detail: str) -> None:
        super().__init__(f"{code}: {detail}")
        self.code = code
        self.detail = detail


def default_secrets_path() -> Path:
    try:
        home = Path.home()
    except RuntimeError:
        return Path("keys.env")
    if sys.platform == "win32":
        return home / "Desktop" / "keys.env"
    return home / ".config" / "tradercockpit" / "secrets.env"


def resolve_secrets_path(environ: Mapping[str, str] | None = None) -> Path:
    source = environ if environ is not None else os.environ
    raw = (source.get(TRADERCOCKPIT_SECRETS_PATH_ENV) or "").strip()
    if raw:
        return Path(raw).expanduser()
    return default_secrets_path()


def parse_dotenv(text: str) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].strip()
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        if key:
            values[key] = value
    return values


def load_secrets_file(path: Path | str) -> dict[str, str]:
    target = Path(path).expanduser()
    if not target.is_file():
        raise SecretsStoreError("secrets_file_missing", f"Secrets file does not exist: {target}")
    return parse_dotenv(target.read_text(encoding="utf-8"))


def apply_operator_secrets(
    environ: dict[str, str] | None = None,
    *,
    path: Path | str | None = None,
) -> dict[str, object]:
    """Load operator secrets into the process environment.

    Existing non-empty process environment values win over the secrets file.
    """

    source = environ if environ is not None else os.environ
    target = Path(path).expanduser() if path is not None else resolve_secrets_path(source)
    explicit = bool((source.get(TRADERCOCKPIT_SECRETS_PATH_ENV) or "").strip())
    if not target.is_file():
        if explicit or path is not None:
            raise SecretsStoreError("secrets_file_missing", f"Secrets file does not exist: {target}")
        return {"loaded": 0, "path_exists": False, "explicit_path": explicit}

    parsed = load_secrets_file(target)
    loaded = 0
    for key, value in parsed.items():
        if not (source.get(key) or "").strip():
            source[key] = value
            loaded += 1
    return {"loaded": loaded, "path_exists": True, "explicit_path": explicit or path is not None}


def _group_configured(source: Mapping[str, str], group: dict[str, object]) -> bool:
    any_of = group.get("any_of")
    if isinstance(any_of, tuple):
        return any(bool((source.get(name) or "").strip()) for name in any_of)
    all_of = group.get("all_of")
    if isinstance(all_of, tuple):
        return all(bool((source.get(name) or "").strip()) for name in all_of)
    return False


def secrets_status_record(environ: Mapping[str, str] | None = None) -> dict[str, object]:
    source = environ if environ is not None else os.environ
    path = resolve_secrets_path(source)
    explicit = bool((source.get(TRADERCOCKPIT_SECRETS_PATH_ENV) or "").strip())
    file_exists = path.is_file()

    if explicit and not file_exists:
        store_status = "unavailable"
        reason_code = "secrets_file_missing"
        detail = (
            f"Set {TRADERCOCKPIT_SECRETS_PATH_ENV} to an existing operator secrets file "
            "outside the application data root."
        )
    elif file_exists:
        store_status = "ready"
        reason_code = None
        detail = "Operator secrets file is configured and readable by the backend."
    else:
        store_status = "not_configured"
        reason_code = "secrets_store_not_configured"
        detail = (
            f"Set {TRADERCOCKPIT_SECRETS_PATH_ENV} or provide operator credentials through "
            "the process environment."
        )

    groups = [
        {
            "id": group["id"],
            "label": group["label"],
            "credential_scope": group["credential_scope"],
            "status": "configured" if _group_configured(source, group) else "not_configured",
        }
        for group in SECRET_GROUPS
    ]

    return {
        "schema": SECRETS_STATUS_SCHEMA,
        "store": {
            "status": store_status,
            "reason_code": reason_code,
            "detail": detail,
            "source": "file" if file_exists else "environment",
        },
        "groups": groups,
    }
