"""FRED macro series read model. Not a live ticker and not SQX Dukascopy history."""

from __future__ import annotations

from math import isfinite
import json
import os
from typing import Callable, Mapping, Protocol, Sequence, runtime_checkable
from urllib.parse import urlencode

from tradercockpit.market_data import _urllib_get


MACRO_SERIES_SCHEMA = "tc.macro-series.v1"
FRED_API_KEY_ENV = "FRED_API_KEY"
FRED_SERIES_ENV = "TRADERCOCKPIT_FRED_SERIES"
FRED_PROVIDER_ID = "fred"
FRED_OBSERVATIONS_URL = "https://api.stlouisfed.org/fred/series/observations"

SeriesTransport = Callable[[str, dict[str, str]], tuple[int, bytes]]


@runtime_checkable
class MacroSeriesProvider(Protocol):
    def fetch_series(self, series_ids: Sequence[str]) -> Sequence[dict[str, object]]:
        ...


def series_ids_from_env(environ: Mapping[str, str] | None = None) -> tuple[str, ...]:
    source = environ if environ is not None else os.environ
    raw = source.get(FRED_SERIES_ENV, "") or ""
    ids: list[str] = []
    seen: set[str] = set()
    for token in raw.split(","):
        candidate = token.strip().upper()
        if candidate and candidate not in seen:
            seen.add(candidate)
            ids.append(candidate)
    return tuple(ids)


def _hookup() -> dict[str, object]:
    return {
        "interface": "tradercockpit.macro_series.MacroSeriesProvider.fetch_series",
        "series_env": FRED_SERIES_ENV,
        "credential_env": FRED_API_KEY_ENV,
        "credential_scope": "operator",
        "detail": (
            "FRED observations for operator-configured series ids (TRADERCOCKPIT_FRED_SERIES). "
            "The same FRED key can be product-provisioned for consumers later. "
            "This is not a live ticker and not SQX Dukascopy history."
        ),
    }


def unavailable_macro_series_record(
    series_ids: Sequence[str] = (),
    *,
    reason_code: str = "provider_not_configured",
    detail: str = "No FRED provider is connected.",
) -> dict[str, object]:
    return {
        "schema": MACRO_SERIES_SCHEMA,
        "status": "unavailable",
        "reason_code": reason_code,
        "detail": detail,
        "provider": None,
        "provider_hookup": _hookup(),
        "series": [{"id": item, "status": "unavailable", "date": None, "value": None} for item in series_ids],
    }


class FredSeriesProvider:
    """Operator FRED REST observations. The key never enters a read model."""

    provider_id = FRED_PROVIDER_ID

    def __init__(self, api_key: str, *, transport: SeriesTransport | None = None) -> None:
        key = api_key.strip() if isinstance(api_key, str) else ""
        if not key:
            raise ValueError("FRED API key must be a non-empty string")
        self._key = key
        self._send = transport or _urllib_get

    def fetch_series(self, series_ids: Sequence[str]) -> Sequence[dict[str, object]]:
        rows: list[dict[str, object]] = []
        for series_id in series_ids:
            cleaned = series_id.strip().upper()
            if not cleaned:
                continue
            status, body = self._send(
                f"{FRED_OBSERVATIONS_URL}?{urlencode({'series_id': cleaned, 'api_key': self._key, 'file_type': 'json', 'sort_order': 'desc', 'limit': '1'})}",
                {"Accept": "application/json", "User-Agent": "TraderCockpit/1.0"},
            )
            if status in {401, 403}:
                raise RuntimeError("macro series provider rejected the credential")
            if status >= 400:
                raise RuntimeError(f"macro series provider failed ({status})")
            try:
                payload = json.loads(body.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as extra:
                raise RuntimeError("macro series provider returned non-JSON") from extra
            if not isinstance(payload, dict):
                raise RuntimeError("macro series provider returned a malformed body")
            observations = payload.get("observations")
            if not isinstance(observations, list) or not observations:
                continue
            latest = observations[0]
            if not isinstance(latest, dict):
                continue
            date = latest.get("date")
            raw_value = latest.get("value")
            if not isinstance(date, str) or not date.strip():
                continue
            try:
                value = float(raw_value) if isinstance(raw_value, str) else raw_value
            except (TypeError, ValueError):
                continue
            if not isinstance(value, (int, float)) or isinstance(value, bool) or not isfinite(float(value)):
                continue
            rows.append({"id": cleaned, "status": "current", "date": date.strip(), "value": float(value)})
        return rows


def macro_provider_from_env(
    environ: Mapping[str, str] | None = None,
    *,
    transport: SeriesTransport | None = None,
) -> FredSeriesProvider | None:
    source = environ if environ is not None else os.environ
    key = (source.get(FRED_API_KEY_ENV) or "").strip()
    if not key:
        return None
    return FredSeriesProvider(key, transport=transport)


def macro_series_record(
    provider: MacroSeriesProvider | None = None,
    series_ids: Sequence[str] = (),
    *,
    provider_id: str | None = None,
    environ: Mapping[str, str] | None = None,
) -> dict[str, object]:
    resolved = tuple(item.strip().upper() for item in series_ids if isinstance(item, str) and item.strip())
    if not resolved:
        resolved = series_ids_from_env(environ)
    if provider is None:
        return unavailable_macro_series_record(resolved)

    try:
        raw = provider.fetch_series(resolved)
        rows = [row for row in raw if isinstance(row, dict) and isinstance(row.get("id"), str)]
    except Exception as exc:  # provider errors must fail closed, never fabricate
        return unavailable_macro_series_record(
            resolved,
            reason_code="provider_read_failed",
            detail=f"The connected macro series provider failed: {exc}",
        )

    by_id = {str(row["id"]).upper(): row for row in rows}
    series: list[dict[str, object]] = []
    for series_id in resolved:
        row = by_id.get(series_id)
        if row is None:
            series.append({"id": series_id, "status": "unavailable", "date": None, "value": None})
            continue
        series.append(
            {
                "id": series_id,
                "status": "current",
                "date": row.get("date") if isinstance(row.get("date"), str) else None,
                "value": float(row["value"]) if isinstance(row.get("value"), (int, float)) and not isinstance(row.get("value"), bool) else None,
            }
        )

    return {
        "schema": MACRO_SERIES_SCHEMA,
        "status": "current",
        "reason_code": None,
        "detail": "FRED observations provided by the connected macro series provider.",
        "provider": {"id": provider_id or getattr(provider, "provider_id", "connected"), "credential_scope": "operator"},
        "provider_hookup": _hookup(),
        "series": series,
    }
