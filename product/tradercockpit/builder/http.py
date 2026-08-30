"""Shared-server adapter for the Builder/evolution HTTP contract.

This module deliberately does not own or instantiate TraderCockpit's HTTP server.
It translates already-parsed method/path/query/body inputs into the canonical
Builder API responses so ``app_server.py`` only needs a minimal routing hook after
Recovery Vertical 1 releases that shared file.
"""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from .api import (
    BUILDER_CANDIDATES_API_PATH,
    BUILDER_SEARCH_READ_API_PATH,
    BUILDER_SEARCH_START_API_PATH,
    builder_candidates_response,
    builder_search_read_response,
    builder_search_start_response,
)


BuilderHttpResponse = tuple[int, dict[str, Any]]


def _single_query_value(
    query: Mapping[str, Sequence[str]],
    *,
    key: str,
    path: str,
) -> tuple[str | None, BuilderHttpResponse | None]:
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


def builder_http_get_response(
    state_root: Path | str | None,
    path: str,
    query: Mapping[str, Sequence[str]],
) -> BuilderHttpResponse | None:
    """Handle Builder-owned GET paths or return ``None`` for another authority."""

    if path == BUILDER_SEARCH_READ_API_PATH:
        search_ref, error = _single_query_value(
            query,
            key="searchRef",
            path=path,
        )
        if error is not None:
            return error
        return builder_search_read_response(state_root, search_ref)

    if path == BUILDER_CANDIDATES_API_PATH:
        strategy_ref, error = _single_query_value(
            query,
            key="strategyRef",
            path=path,
        )
        if error is not None:
            return error
        return builder_candidates_response(state_root, strategy_ref)

    return None


def builder_http_post_response(
    state_root: Path | str | None,
    path: str,
    request: object,
) -> BuilderHttpResponse | None:
    """Handle Builder-owned POST paths or return ``None`` for another authority."""

    if path == BUILDER_SEARCH_START_API_PATH:
        return builder_search_start_response(state_root, request)
    return None
