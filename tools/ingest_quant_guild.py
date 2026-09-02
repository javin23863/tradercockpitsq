#!/usr/bin/env python3
"""Ingest Quant-Guild lecture markdown as Assistant reference data.

Downloads notebook JSON from GitHub (not a git clone) and writes
``product/tradercockpit/knowledge/quant_guild.json``. Notebook Python is discarded.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen


REPO = "romanmichaelpaolucci/Quant-Guild-Library"
TREE_URL = f"https://api.github.com/repos/{REPO}/git/trees/HEAD?recursive=1"
RAW_ROOT = f"https://raw.githubusercontent.com/{REPO}"
BLOB_ROOT = f"https://github.com/{REPO}/blob"
LECTURE_RE = re.compile(r"^(20\d\d Video Lectures)/(\d+)\.\s+(.+)$")
MAX_DOC_CHARS = 1200
TIMEOUT = 45


def _get(url: str) -> bytes:
    request = Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "tradercockpit-quant-guild-ingest"})
    with urlopen(request, timeout=TIMEOUT) as response:  # noqa: S310 - fixed GitHub hosts
        return response.read()


def _cell_text(cell: object) -> str:
    if not isinstance(cell, dict) or cell.get("cell_type") != "markdown":
        return ""
    source = cell.get("source")
    if isinstance(source, list):
        return "".join(part for part in source if isinstance(part, str))
    return source if isinstance(source, str) else ""


def _extract_markdown(notebook: object) -> str:
    if not isinstance(notebook, dict):
        return ""
    parts = [_cell_text(cell).strip() for cell in notebook.get("cells") or []]
    text = "\n\n".join(part for part in parts if part)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    return text[:MAX_DOC_CHARS]


def _lecture_key(path: str) -> tuple[str, str, str] | None:
    folder = path.rsplit("/", 1)[0] if "/" in path else path
    match = LECTURE_RE.match(folder)
    if match is None:
        return None
    year_folder, number, title = match.groups()
    return year_folder, number, title


def _is_primary_notebook(path: str) -> bool:
    if not path.endswith(".ipynb"):
        return False
    relative = path.rsplit("/", 1)[0]
    return LECTURE_RE.match(relative) is not None


def ingest(destination: Path) -> dict[str, object]:
    tree = json.loads(_get(TREE_URL))
    revision = tree.get("sha") if isinstance(tree.get("sha"), str) else "HEAD"
    paths = [item["path"] for item in tree.get("tree") or [] if isinstance(item, dict) and isinstance(item.get("path"), str)]
    lectures: dict[tuple[str, str], dict[str, object]] = {}
    for path in paths:
        key = _lecture_key(path)
        if key is None:
            continue
        year_folder, number, title = key
        record = lectures.setdefault(
            (year_folder, number),
            {"id": f"{year_folder[:4]}-{int(number):03d}", "title": title, "path": f"{year_folder}/{number}. {title}", "notebook": None},
        )
        if _is_primary_notebook(path) and record["notebook"] is None:
            record["notebook"] = path
            record["path"] = path.rsplit("/", 1)[0]

    documents = []
    for (_year, number), record in sorted(lectures.items(), key=lambda item: (item[0][0], int(item[0][1]))):
        notebook_path = record["notebook"]
        text = ""
        url = f"{BLOB_ROOT}/{revision}/{quote(str(record['path']), safe='/')}"
        if isinstance(notebook_path, str):
            raw_url = f"{RAW_ROOT}/{revision}/{quote(notebook_path, safe='/')}"
            try:
                text = _extract_markdown(json.loads(_get(raw_url)))
            except Exception as exc:  # noqa: BLE001 - ingest continues; missing lecture stays title-only
                print(f"skip {notebook_path}: {exc}", file=sys.stderr)
            url = f"{BLOB_ROOT}/{revision}/{quote(notebook_path, safe='/')}"
        if not text:
            text = (
                f"{record['title']}. No lecture markdown was present in this Quant-Guild snapshot. "
                "Do not invent the lecture content."
            )
        documents.append(
            {
                "id": record["id"],
                "title": record["title"],
                "path": record["path"],
                "url": url,
                "text": text,
            }
        )

    payload = {
        "schema": "tc.quant-guild-corpus.v1",
        "library": "quant-guild",
        "source": f"https://github.com/{REPO}",
        "source_revision": revision,
        "attribution": "Roman Paolucci / Quant Guild. Markdown excerpts only; notebooks and code are not imported.",
        "documents": documents,
    }
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return payload


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    destination = root / "product" / "tradercockpit" / "knowledge" / "quant_guild.json"
    payload = ingest(destination)
    print(f"wrote {len(payload['documents'])} documents to {destination} @ {payload['source_revision']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
