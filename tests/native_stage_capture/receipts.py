"""Read bounded native probe observations, never infer execution or filter verdicts."""
from datetime import datetime
from hashlib import sha256
from pathlib import Path
from uuid import UUID
from xml.etree import ElementTree as ET

SCHEMA = "tc.native-capture-probe.v2"
BINDINGS = {"project", "run", "checkpoint", "task_entry", "task", "databank", "graph_sha256"}


def _file(root, path):
    if root.resolve() != root.absolute():
        raise ValueError("Linked capture root")
    current = root
    for part in path.relative_to(root).parts:
        current /= part
        if current.is_symlink() or getattr(current, "is_junction", lambda: False)():
            raise ValueError("Linked capture path")
    if path.resolve() != path.absolute() or not path.is_file() or path.stat().st_nlink != 1:
        raise ValueError("Capture file is missing or linked")
    return path.read_bytes()


def _properties(raw):
    # Java Properties emits this fixed public DTD. ElementTree never fetches it;
    # reject internal subsets/entities and every other declaration before parsing.
    raw = raw.decode("utf-8").replace('<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">', '')
    if '<!DOCTYPE' in raw.upper() or '<!ENTITY' in raw.upper():
        raise ValueError("Unsupported manifest declaration")
    root = ET.fromstring(raw)
    if root.tag != "properties" or any(n.tag not in {"entry", "comment"} for n in root):
        raise ValueError("Invalid Properties manifest")
    entries = root.findall("entry")
    result = {n.attrib["key"]: n.text or "" for n in entries}
    if len(result) != len(entries) or any(set(n.attrib) != {"key"} or len(n) for n in entries):
        raise ValueError("Duplicate or malformed manifest field")
    return result


def _instant(value):
    result = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if result.tzinfo is None:
        raise ValueError("Capture timestamp lacks timezone")
    return result


def read_visit(root, visit, binding):
    """Reopen an exact visit using the independently retained test-run binding.

    A completed checkpoint means files were saved. It says nothing about native
    task success, Candidate identity, filter outcome or approved-run coverage.
    """
    root = Path(root).absolute()
    if str(UUID(visit)) != visit or set(binding) != BINDINGS:
        raise ValueError("Invalid visit or expected binding")
    folder = root / binding["checkpoint"] / visit
    # Checkpoint is one directory, not a caller-supplied relative path.
    if not binding["checkpoint"].isascii() or not binding["checkpoint"].isalpha() or not binding["checkpoint"].islower():
        raise ValueError("Invalid checkpoint")
    started = _properties(_file(root, folder / "started.xml"))
    if (set(started) != BINDINGS | {"schema", "visit", "started", "count"}
            or any(started[k] != v for k, v in binding.items())
            or started["visit"] != visit or started["schema"] != SCHEMA):
        raise ValueError("Capture does not match the retained binding")
    count = int(started["count"])
    if count < 0 or str(count) != started["count"]:
        raise ValueError("Invalid native count")
    begin = _instant(started["started"])
    completed, failed = folder / "completed.xml", folder / "failed.xml"
    for terminal in (completed, failed):
        if terminal.is_symlink() or getattr(terminal, "is_junction", lambda: False)():
            raise ValueError("Linked terminal capture state")
    if completed.exists() and failed.exists():
        raise ValueError("Conflicting terminal capture states")
    state = "completed" if completed.exists() else "capture_failed" if failed.exists() else "capture_incomplete"
    data = started if state == "capture_incomplete" else _properties(_file(root, completed if state == "completed" else failed))
    if any(data.get(k) != v for k, v in started.items()):
        raise ValueError("Terminal manifest changed the visit identity")
    if state != "capture_incomplete":
        end = "completed" if state == "completed" else "failed"
        if _instant(data[end]) < begin:
            raise ValueError("Capture completion precedes start")
    artifacts = []
    for i in range(count):
        keys = {f"artifact.{i}.{k}" for k in ("name", "sha256", "bytes")}
        if not keys <= data.keys():
            if state == "completed" or keys & data.keys():
                raise ValueError("Incomplete artifact manifest")
            continue
        raw = _file(root, folder / f"{i}.sqx")
        if sha256(raw).hexdigest() != data[f"artifact.{i}.sha256"] or str(len(raw)) != data[f"artifact.{i}.bytes"]:
            raise ValueError("Captured archive hash or byte count changed")
        artifacts.append({"index": i, "name": data[f"artifact.{i}.name"], "sha256": sha256(raw).hexdigest(), "bytes": len(raw)})
    expected = set(started)
    if state == "completed": expected.add("completed")
    if state == "capture_failed": expected.update({"failed", "error_type"})
    expected.update(f"artifact.{row['index']}.{key}" for row in artifacts for key in ("name", "sha256", "bytes"))
    if set(data) != expected:
        raise ValueError("Unexpected capture fields")
    if state == "completed" and {p.name for p in folder.glob("*.sqx")} != {f"{i}.sqx" for i in range(count)}:
        raise ValueError("Capture archive inventory differs from completion")
    return {**binding, "visit": visit, "state": state, "started": started["started"],
            "completed": data.get("completed"), "failed": data.get("failed"),
            "error_type": data.get("error_type"), "native_count": count, "artifacts": artifacts}
