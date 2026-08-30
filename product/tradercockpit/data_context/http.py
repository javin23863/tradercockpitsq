"""Shared-server adapter for Data & Trading Context routes.

This module owns no HTTP server. It accepts already-parsed request components
and delegates only the paths belonging to this capability, returning ``None``
for every other product authority.
"""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from .api import (
    DATA_CONTEXTS_API_PATH,
    DATA_CONTEXT_READ_API_PATH,
    data_context_create_response,
    data_context_list_response,
    data_context_read_response,
)

DataContextHttpResponse = tuple[int, dict[str, Any]]


def _single_query_value(
    query: Mapping[str, Sequence[str]],
    *,
    key: str,
    path: str,
) -> tuple[str | None, DataContextHttpResponse | None]:
    unknown = sorted(set(query) - {key})
    if unknown:
        return None, (
            400,
            {
                "error": "invalid_request",
                "detail": f"unknown query fields for {path}: " + ", ".join(unknown),
            },
        )
    values = query.get(key, ())
    if len(values) != 1 or not values[0]:
        return None, (
            400,
            {
                "error": "invalid_request",
                "detail": f"exactly one non-empty {key} query value is required",
            },
        )
    return values[0], None


def data_context_http_get_response(
    state_root: Path | str | None,
    path: str,
    query: Mapping[str, Sequence[str]],
) -> DataContextHttpResponse | None:
    if path == DATA_CONTEXTS_API_PATH:
        if query:
            return 400, {
                "error": "invalid_request",
                "detail": f"{DATA_CONTEXTS_API_PATH} does not accept query fields",
            }
        return data_context_list_response(state_root)

    if path == DATA_CONTEXT_READ_API_PATH:
        context_ref, error = _single_query_value(
            query,
            key="contextRef",
            path=path,
        )
        if error is not None:
            return error
        return data_context_read_response(state_root, context_ref)

    return None


def data_context_http_post_response(
    state_root: Path | str | None,
    path: str,
    request: object,
) -> DataContextHttpResponse | None:
    if path == DATA_CONTEXTS_API_PATH:
        return data_context_create_response(state_root, request)
    return None
