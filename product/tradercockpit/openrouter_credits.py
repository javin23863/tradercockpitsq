"""Per-consumer OpenRouter LLM usage allowance, funded from the single membership.

There is no separate LLM charge. The one membership subscription funds a
provider-enforced OpenRouter key limit that both bounds consumer spend and lets
the product report usage. The limit amount is an internal allocation carved from
the membership (backend-configurable), never presented to the consumer as a price.
"""

from __future__ import annotations

from datetime import datetime, timezone
import json
import os
from pathlib import Path
from typing import Callable, Mapping
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from tradercockpit.consumer_account import signed_in_account_id
from tradercockpit.stripe_membership import membership_status_record


OPENROUTER_MANAGEMENT_KEY_ENV = "OPENROUTER_MANAGEMENT_KEY"
OPENROUTER_CREDIT_LIMIT_USD_ENV = "OPENROUTER_CREDIT_LIMIT_USD"

OPENROUTER_KEYS_API_BASE = "https://openrouter.ai/api/v1/keys"
OPENROUTER_CREDITS_NAME = "openrouter-credits.json"
OPENROUTER_CREDITS_SCHEMA = "tc.openrouter-credits.v1"
CREDITS_STATUS_SCHEMA = "tc.openrouter-credits-status.v1"

DEFAULT_CREDIT_LIMIT_USD = 30.0
DEFAULT_LIMIT_RESET = "monthly"

OpenRouterTransport = Callable[[str, str, dict[str, str], bytes | None], tuple[int, bytes]]


def openrouter_credits_path(data_root: Path | str) -> Path:
    return Path(data_root) / OPENROUTER_CREDITS_NAME


def _management_key(environ: Mapping[str, str] | None = None) -> str:
    source = environ if environ is not None else os.environ
    return (source.get(OPENROUTER_MANAGEMENT_KEY_ENV) or "").strip()


def openrouter_credits_configured(environ: Mapping[str, str] | None = None) -> bool:
    return bool(_management_key(environ))


def configured_credit_limit_usd(environ: Mapping[str, str] | None = None) -> float:
    source = environ if environ is not None else os.environ
    raw = (source.get(OPENROUTER_CREDIT_LIMIT_USD_ENV) or "").strip()
    if not raw:
        return DEFAULT_CREDIT_LIMIT_USD
    try:
        limit = float(raw)
    except ValueError as exc:
        raise ValueError(f"{OPENROUTER_CREDIT_LIMIT_USD_ENV} must be a USD amount") from exc
    if limit <= 0:
        raise ValueError(f"{OPENROUTER_CREDIT_LIMIT_USD_ENV} must be positive")
    return limit


def _load_credits(data_root: Path | str | None) -> dict[str, object]:
    if data_root is None:
        return {}
    path = openrouter_credits_path(data_root)
    if not path.is_file():
        return {}
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}
    if not isinstance(payload, dict) or payload.get("schema") != OPENROUTER_CREDITS_SCHEMA:
        return {}
    account_id = payload.get("account_id")
    key_hash = payload.get("key_hash")
    api_key = payload.get("api_key")
    if not isinstance(account_id, str) or not isinstance(key_hash, str) or not isinstance(api_key, str):
        return {}
    if not account_id.strip() or not key_hash.strip() or not api_key.strip():
        return {}
    return payload


def _write_credits(data_root: Path | str, payload: dict[str, object]) -> None:
    path = openrouter_credits_path(data_root)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _membership_active(data_root: Path | str | None, environ: Mapping[str, str] | None = None) -> bool:
    record = membership_status_record(data_root, environ)
    return record.get("membership_status") == "active"


def _urllib_openrouter_request(
    method: str,
    path: str,
    management_key: str,
    headers: dict[str, str],
    body: bytes | None,
) -> tuple[int, bytes]:
    request = Request(
        f"{OPENROUTER_KEYS_API_BASE}{path}",
        data=body,
        headers=headers,
        method=method,
    )
    try:
        with urlopen(request, timeout=15) as response:  # noqa: S310 - fixed OpenRouter host
            return int(response.status), response.read()
    except HTTPError as extra:
        return int(extra.code), extra.read()
    except URLError as extra:
        raise RuntimeError(f"OpenRouter keys API unreachable: {extra.reason}") from extra
    except TimeoutError as extra:
        raise RuntimeError("OpenRouter keys API timed out") from extra


def _openrouter_request(
    method: str,
    path: str,
    management_key: str,
    *,
    payload: dict[str, object] | None = None,
    transport: OpenRouterTransport | None = None,
) -> dict[str, object]:
    headers = {
        "Authorization": f"Bearer {management_key}",
        "Accept": "application/json",
    }
    body: bytes | None = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(payload).encode("utf-8")
    if transport is None:
        status, raw = _urllib_openrouter_request(method, path, management_key, headers, body)
    else:
        status, raw = transport(method, path, headers, body)
    if status >= 400:
        raise RuntimeError(f"OpenRouter keys API request failed ({status})")
    try:
        decoded = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as extra:
        raise RuntimeError("OpenRouter keys API returned non-JSON") from extra
    if not isinstance(decoded, dict):
        raise RuntimeError("OpenRouter keys API returned a malformed body")
    return decoded


def _key_record_from_response(
    account_id: str,
    response: dict[str, object],
    *,
    limit_usd: float,
) -> dict[str, object]:
    data = response.get("data") if isinstance(response.get("data"), dict) else response
    if not isinstance(data, dict):
        raise RuntimeError("OpenRouter key response missing data")
    key_hash = data.get("hash")
    api_key = response.get("key") if isinstance(response.get("key"), str) else data.get("key")
    if not isinstance(key_hash, str) or not key_hash.strip():
        raise RuntimeError("OpenRouter key response missing hash")
    if not isinstance(api_key, str) or not api_key.strip():
        raise RuntimeError("OpenRouter key response missing plaintext key")
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    record: dict[str, object] = {
        "schema": OPENROUTER_CREDITS_SCHEMA,
        "account_id": account_id,
        "key_hash": key_hash.strip(),
        "api_key": api_key.strip(),
        "limit_usd": limit_usd,
        "limit_reset": DEFAULT_LIMIT_RESET,
        "provisioned_at": now,
        "updated_at": now,
    }
    for field in ("limit_remaining", "usage"):
        value = data.get(field)
        if isinstance(value, (int, float)):
            record[field] = float(value)
    return record


def provision_consumer_key(
    data_root: Path | str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: OpenRouterTransport | None = None,
) -> dict[str, object]:
    management_key = _management_key(environ)
    if not management_key:
        raise ValueError(f"OpenRouter credit provisioning requires {OPENROUTER_MANAGEMENT_KEY_ENV}")
    account_id = signed_in_account_id(data_root)
    if not account_id:
        raise ValueError("OpenRouter credit provisioning requires a verified Google session")
    if not _membership_active(data_root, environ):
        raise ValueError("OpenRouter credit provisioning requires an active membership")
    limit_usd = configured_credit_limit_usd(environ)
    stored = _load_credits(data_root)
    # Reuse an existing key only when it is still active. A key OpenRouter disabled
    # (e.g. after a monthly allowance was reached) must be re-provisioned rather than
    # returned as a dead credential.
    if stored and stored.get("account_id") == account_id and stored.get("disabled") is not True:
        return stored
    response = _openrouter_request(
        "POST",
        "",
        management_key,
        payload={
            "name": f"TraderCockpit {account_id[:24]}",
            "limit": limit_usd,
            "limit_reset": DEFAULT_LIMIT_RESET,
        },
        transport=transport,
    )
    record = _key_record_from_response(account_id, response, limit_usd=limit_usd)
    _write_credits(data_root, record)
    return record


def refresh_consumer_key_usage(
    data_root: Path | str,
    environ: Mapping[str, str] | None = None,
    *,
    transport: OpenRouterTransport | None = None,
) -> dict[str, object] | None:
    management_key = _management_key(environ)
    if not management_key:
        return _load_credits(data_root) or None
    stored = _load_credits(data_root)
    if not stored:
        return None
    account_id = signed_in_account_id(data_root)
    key_hash = stored.get("key_hash")
    if not isinstance(account_id, str) or stored.get("account_id") != account_id:
        return stored
    if not isinstance(key_hash, str) or not key_hash.strip():
        return stored
    response = _openrouter_request(
        "GET",
        f"/{key_hash.strip()}",
        management_key,
        transport=transport,
    )
    data = response.get("data") if isinstance(response.get("data"), dict) else response
    if not isinstance(data, dict):
        return stored
    updated = dict(stored)
    updated["updated_at"] = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    for field in ("limit_remaining", "usage", "usage_monthly", "limit"):
        value = data.get(field)
        if isinstance(value, (int, float)):
            updated[field] = float(value)
    # Track disabled state bidirectionally so a key re-enabled after a monthly reset
    # is no longer treated as dead.
    if "disabled" in data:
        if bool(data.get("disabled")):
            updated["disabled"] = True
        else:
            updated.pop("disabled", None)
    _write_credits(data_root, updated)
    return updated


def consumer_inference_credential(
    data_root: Path | str | None = None,
    environ: Mapping[str, str] | None = None,
    *,
    transport: OpenRouterTransport | None = None,
    provision: bool = True,
) -> dict[str, object] | None:
    """Return consumer OpenRouter inference credential when provider-enforced credits apply."""

    if data_root is None or not openrouter_credits_configured(environ):
        return None
    account_id = signed_in_account_id(data_root)
    if not account_id or not _membership_active(data_root, environ):
        return None
    stored = _load_credits(data_root)
    if stored and stored.get("account_id") == account_id and stored.get("disabled") is not True:
        # Refresh usage only on the inference path (provision=True), never on the
        # non-blocking status read path, so /api/status performs no network I/O.
        if provision:
            try:
                refreshed = refresh_consumer_key_usage(data_root, environ, transport=transport)
                if refreshed and refreshed.get("account_id") == account_id:
                    stored = refreshed
            except RuntimeError:
                pass
        api_key = stored.get("api_key")
        if isinstance(api_key, str) and api_key.strip():
            return {
                "api_key": api_key.strip(),
                "credential_scope": "consumer",
                "provider_enforced": True,
                "limit_usd": stored.get("limit_usd", configured_credit_limit_usd(environ)),
                "limit_remaining_usd": stored.get("limit_remaining"),
                "usage_usd": stored.get("usage"),
            }
    if not provision:
        return None
    try:
        stored = provision_consumer_key(data_root, environ, transport=transport)
    except (ValueError, RuntimeError):
        return None
    api_key = stored.get("api_key")
    if not isinstance(api_key, str) or not api_key.strip():
        return None
    return {
        "api_key": api_key.strip(),
        "credential_scope": "consumer",
        "provider_enforced": True,
        "limit_usd": stored.get("limit_usd", configured_credit_limit_usd(environ)),
        "limit_remaining_usd": stored.get("limit_remaining"),
        "usage_usd": stored.get("usage"),
    }


def credits_status_record(
    data_root: Path | str | None = None,
    environ: Mapping[str, str] | None = None,
    *,
    transport: OpenRouterTransport | None = None,
) -> dict[str, object]:
    """Secret-free OpenRouter credits readiness for /api/status and Settings."""

    limit_usd = configured_credit_limit_usd(environ)
    base: dict[str, object] = {
        "schema": CREDITS_STATUS_SCHEMA,
        "limit_usd": limit_usd,
        "limit_reset": DEFAULT_LIMIT_RESET,
        "provider_enforced": False,
    }
    if not openrouter_credits_configured(environ):
        return {
            **base,
            "status": "unavailable",
            "reason_code": "provision_not_configured",
            "detail": (
                f"Set {OPENROUTER_MANAGEMENT_KEY_ENV} so member LLM usage is funded "
                "from the membership with a provider-enforced limit."
            ),
        }
    account_id = signed_in_account_id(data_root)
    if not account_id:
        return {
            **base,
            "status": "unavailable",
            "reason_code": "not_signed_in",
            "detail": "Sign in with Google before OpenRouter credits can be provisioned.",
        }
    if not _membership_active(data_root, environ):
        return {
            **base,
            "status": "unavailable",
            "reason_code": "membership_inactive",
            "detail": "An active membership funds the included LLM usage allowance.",
        }
    stored = _load_credits(data_root)
    if stored and stored.get("account_id") != account_id:
        stored = {}
    # The status read model never performs network I/O. It reports the persisted
    # usage (kept current by consumer_inference_credential on assistant requests)
    # so /api/status stays fast even when OpenRouter is slow or unreachable.
    if not stored or stored.get("account_id") != account_id:
        return {
            **base,
            "status": "unavailable",
            "reason_code": "not_provisioned",
            "detail": "Your LLM usage allowance provisions on the next assistant request.",
        }
    if stored.get("disabled") is True:
        return {
            **base,
            "status": "unavailable",
            "reason_code": "credit_limit_reached",
            "detail": "OpenRouter paused this consumer key after the monthly usage allowance was reached.",
            "provider_enforced": True,
        }
    record: dict[str, object] = {
        **base,
        "status": "ready",
        "reason_code": None,
        "detail": "LLM usage is funded by your membership with a provider-enforced limit.",
        "provider_enforced": True,
    }
    for src, dst in (("limit_remaining", "limit_remaining_usd"), ("usage", "usage_usd"), ("usage_monthly", "usage_monthly_usd")):
        value = stored.get(src)
        if isinstance(value, (int, float)):
            record[dst] = float(value)
    # Usage tracking is exposed as a non-price percentage so the consumer can see
    # how much of the membership-funded allowance is used without a dollar figure.
    usage = record.get("usage_usd")
    if isinstance(limit_usd, (int, float)) and limit_usd > 0 and isinstance(usage, (int, float)):
        used_ratio = max(0.0, min(1.0, usage / limit_usd))
        record["percent_used"] = round(used_ratio * 100)
        record["percent_remaining"] = round((1.0 - used_ratio) * 100)
    return record
