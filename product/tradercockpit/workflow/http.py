"""Strict HTTP adapter for workflow automation routes.

The canonical server may delegate matching requests here.  This module does not
create a second HTTP server and cleanly declines paths owned by other product
capabilities.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping
from urllib.parse import parse_qs, urlsplit

from .api import (
    WORKFLOW_IMPORT_SQX_API_PATH,
    WORKFLOW_LIST_API_PATH,
    WORKFLOW_READ_API_PATH,
    WORKFLOW_START_API_PATH,
    workflow_import_sqx_response,
    workflow_list_response,
    workflow_read_response,
    workflow_start_response,
)
from .service import WorkflowActionHandler


def dispatch_workflow_http(
    method: str,
    target: str,
    body: object | None,
    *,
    state_root: Path | str | None,
    sqx_home: Path | str | None = None,
    handlers: Mapping[str, WorkflowActionHandler] | None = None,
) -> tuple[bool, int, dict[str, Any]]:
    """Return ``(handled, status, payload)`` for one parsed product request."""

    if not isinstance(method, str) or not isinstance(target, str):
        return False, 0, {}
    verb = method.upper()
    parsed = urlsplit(target)
    path = parsed.path

    if verb == "POST" and path == WORKFLOW_START_API_PATH:
        return True, *workflow_start_response(state_root, body, handlers=handlers)

    if verb == "GET" and path == WORKFLOW_LIST_API_PATH:
        if parsed.query:
            return True, 400, {"error": "invalid_request", "detail": "workflow list does not accept query parameters"}
        return True, *workflow_list_response(state_root, handlers=handlers)

    if verb == "GET" and path == WORKFLOW_READ_API_PATH:
        query = parse_qs(parsed.query, keep_blank_values=True, strict_parsing=False)
        if set(query) != {"runRef"} or len(query["runRef"]) != 1 or not query["runRef"][0]:
            return True, 400, {"error": "invalid_request", "detail": "workflow read requires exactly one runRef"}
        return True, *workflow_read_response(
            state_root,
            query["runRef"][0],
            handlers=handlers,
        )

    if verb == "POST" and path == WORKFLOW_IMPORT_SQX_API_PATH:
        return True, *workflow_import_sqx_response(sqx_home, body)

    if path in {
        WORKFLOW_START_API_PATH,
        WORKFLOW_LIST_API_PATH,
        WORKFLOW_READ_API_PATH,
        WORKFLOW_IMPORT_SQX_API_PATH,
    }:
        return True, 405, {"error": "method_not_allowed", "detail": f"{verb} is not supported for {path}"}

    return False, 0, {}
