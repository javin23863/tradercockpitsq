"""Native Candidate carrier and conservative archive reserialization checks.

Tokens are custody references, never evidence of backtest execution or validity.
The caller retains both immutable archives and authorizes any membership update.
"""
from base64 import b64decode
from copy import copy
from hashlib import sha256
from io import BytesIO
import json
import math
import re
import struct
from xml.etree import ElementTree as ET
from zipfile import ZipFile

from . import sqx_databank_grid as grid

TOKEN_KEY = "TraderCockpitCandidateTokenV1"
_TOKEN = re.compile(r"[0-9a-f]{64}")
_MAPS = {"ResultsMap", "Results", "SettingsMap", "ValuesMap", "SymbolsMap", "SpecialValuesMap"}
_CACHES = {"MEC_FULL_Main", "MEC_FULL_Portfolio", "MEC_IS_Portfolio", "MEC_OOS_Portfolio"}
_CACHE_PREFIX = "{{sparklinesWidget data='"


class SqxCandidateIdentityError(ValueError):
    def __init__(self, code, detail):
        super().__init__(detail)
        self.code, self.detail = code, detail


def _refuse(detail, code="candidate_reserialization_unverified"):
    raise SqxCandidateIdentityError(code, detail)


def _token(value):
    if not isinstance(value, str) or not _TOKEN.fullmatch(value):
        _refuse("Candidate token must be exactly 64 lowercase hexadecimal characters.", "candidate_token_invalid")
    return value


def _archive(raw):
    # Lazy import lets the native action adapter reuse this helper without a cycle.
    from .sqx_databank_actions import inspect_databank_upload, SqxDatabankActionError
    from .sqx_outputs import SqxOutputError
    try:
        inspect_databank_upload(raw, "candidate.sqx")
        with ZipFile(BytesIO(raw)) as zipped:
            return {entry.filename: zipped.read(entry) for entry in zipped.infolist()}
    except (SqxDatabankActionError, SqxOutputError) as exc:
        raise SqxCandidateIdentityError("candidate_archive_invalid", "Candidate archive failed bounded native ZIP/XML validation.") from exc


def _xml(raw):
    try:
        root = ET.fromstring(raw)
        pending, count = [(root, 0)], 0
        while pending:
            node, depth = pending.pop()
            count += 1
            if depth > 128 or count > 100000:
                _refuse("Candidate XML exceeds bounded comparison depth or size.", "candidate_archive_invalid")
            pending.extend((child, depth + 1) for child in node)
        return root
    except (ET.ParseError, LookupError, ValueError) as exc:
        raise SqxCandidateIdentityError("candidate_archive_invalid", "Candidate XML is unreadable.") from exc


def _carrier(root, *, allow_absent=False):
    maps = root.findall("./SpecialValuesMap/SettingsMap")
    nodes = [node for node in root.iter() if node.tag.rsplit("}", 1)[-1].casefold().startswith("tradercockpitcandidatetoken")]
    if allow_absent and root.tag == "ResultsGroup" and not root.findall("SpecialValuesMap") and not nodes:
        return None, None
    if root.tag != "ResultsGroup" or len(root.findall("SpecialValuesMap")) != 1 or len(maps) != 1:
        _refuse("Native Candidate SpecialValuesMap is missing or ambiguous.", "candidate_token_invalid")
    if not nodes:
        return maps[0], None
    if len(nodes) != 1 or nodes[0] not in list(maps[0]):
        _refuse("Candidate token is duplicated or misplaced.", "candidate_token_invalid")
    node = nodes[0]
    if node.tag != TOKEN_KEY or node.attrib != {"type": "String"} or len(node):
        _refuse("Reserved Candidate token metadata has an unsupported shape.", "candidate_token_invalid")
    return maps[0], _token(node.text)


def read_candidate_token(raw):
    """Return the exact native carrier, or None for an unstamped legacy archive."""
    return _carrier(_xml(_archive(raw)["settings.xml"]), allow_absent=True)[1]


def stamp_candidate_token(raw, token):
    """Return a metadata derivative; never alter the supplied archive bytes."""
    return _stamp(raw, token, replace_existing=False)


def stamp_import_candidate_token(raw, token):
    """Give a NEW import its own carrier; callers retain the original separately."""
    return _stamp(raw, token, replace_existing=True)


def _stamp(raw, token, *, replace_existing):
    token = _token(token)
    members = _archive(raw)
    root = _xml(members["settings.xml"])
    values, existing = _carrier(root, allow_absent=True)
    if existing is not None and not replace_existing:
        _refuse("An existing Candidate token cannot be overwritten.", "candidate_token_exists")
    if values is None:
        values = ET.SubElement(ET.SubElement(root, "SpecialValuesMap"), "SettingsMap")
    if existing is None:
        ET.SubElement(values, TOKEN_KEY, {"type": "String"}).text = token
    else:
        values.find(TOKEN_KEY).text = token
    settings = ET.tostring(root, encoding="utf-8", xml_declaration=True)
    output = BytesIO()
    with ZipFile(BytesIO(raw)) as source, ZipFile(output, "w") as target:
        target.comment = source.comment
        for member in source.infolist():
            target.writestr(copy(member), settings if member.filename == "settings.xml" else members[member.filename])
    stamped = output.getvalue()
    if read_candidate_token(stamped) != token:
        _refuse("Candidate metadata derivative could not be verified.", "candidate_token_invalid")
    return stamped


def _stats(node):
    """Keep every typed record, including duplicates; the display decoder alone loses them."""
    if node.attrib != {"version": "2", "e": "b64"} or len(node):
        _refuse("Unsupported native statistics encoding.")
    text = node.text or ""
    if len(text) > 1024 * 1024:
        _refuse("Native statistics exceed the bounded comparison size.")
    try:
        raw = b64decode(text.strip(), validate=True)
        decoded = grid.decode_sqstats_v2(text)
        indexed = {1: grid._INT_BY_INDEX, 2: grid._LONG_BY_INDEX, 3: grid._DOUBLE_BY_INDEX}
        formats = {1: ">i", 2: ">q", 3: ">f"}
        position, records, named = 0, [], {}
        while position < len(raw):
            kind = raw[position]
            position += 1
            if kind in indexed:
                index = raw[position]
                position += 1
                name, basic = indexed[kind][index], kind
            elif kind in (101, 102, 103):
                name, position = grid._read_modified_utf(raw, position)
                basic = kind - 100
                if not name or len(name) > 1024:
                    _refuse("Native statistic name is invalid.")
            else:
                _refuse("Unsupported native statistic record type.")
            width = struct.calcsize(formats[basic])
            value = struct.unpack_from(formats[basic], raw, position)[0]
            bits = raw[position:position + width].hex()
            position += width
            # Native sentinels may be Infinity/NaN. Preserve their bits without
            # interpreting them as available financial measurements.
            if name not in decoded or not (decoded[name] == value or (math.isnan(decoded[name]) and math.isnan(value))):
                _refuse("Native statistics contain conflicting or unreadable records.")
            if name in named and not (named[name] == value or (math.isnan(named[name]) and math.isnan(value))):
                _refuse("Native statistics contain conflicting duplicate values.")
            named[name] = value
            records.append((name, basic, bits))
        if set(named) != set(decoded):
            _refuse("Native statistics were not completely decoded.")
        return tuple(sorted(records))
    except (ValueError, KeyError, IndexError, struct.error, UnicodeError) as exc:
        if isinstance(exc, SqxCandidateIdentityError):
            raise
        raise SqxCandidateIdentityError("candidate_reserialization_unverified", "Native statistics could not be completely verified.") from exc


def _key(parent, child):
    if parent == "Results":
        if child.tag != "Result" or not child.get("resultKey"):
            _refuse("Native result map has an unsupported key.")
        return child.get("resultKey")
    if parent == "SymbolsMap":
        if child.tag != "SymbolInfo" or not child.get("symbolName"):
            _refuse("Native symbol map has an unsupported key.")
        return child.get("symbolName")
    return child.tag


def _canonical(node, *, maps):
    text = node.text or ""
    tail = node.tail or ""
    children = [_canonical(child, maps=maps) for child in node]
    if maps and node.tag in _MAPS:
        keys = [_key(node.tag, child) for child in node]
        if len(set(keys)) != len(keys):
            _refuse("Native XML map contains duplicate keys.")
        children = [value for _, value in sorted(zip(keys, children))]
    if node.tag == "SQStats":
        text = _stats(node)
    elif not text.strip():
        text = ""
    return node.tag, tuple(sorted(node.attrib.items())), text, tail if tail.strip() else "", tuple(children)


def _json_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            _refuse("Native display cache contains duplicate JSON fields.")
        result[key] = value
    return result


def _finite(value):
    try:
        return type(value) in (int, float) and math.isfinite(value)
    except OverflowError:
        return False


def _cache(node):
    text = node.text or ""
    if node.attrib != {"type": "String"} or len(node) or len(text) > 16384 or (node.tail or "").strip():
        _refuse("Native display cache has an unsupported shape.")
    if not text.startswith(_CACHE_PREFIX) or not text.endswith("'}}"):
        _refuse("Native display cache is not the supported sparkline widget.")
    try:
        value = json.loads(text[len(_CACHE_PREFIX):-3], object_pairs_hook=_json_object)
    except (ValueError, TypeError) as exc:
        raise SqxCandidateIdentityError("candidate_reserialization_unverified", "Native display cache JSON is unreadable.") from exc
    if (not isinstance(value, dict) or set(value) != {"values", "zeroPoint"}
            or not isinstance(value["values"], list) or len(value["values"]) not in {0, 50}
            or any(not _finite(item) for item in value["values"]) or not _finite(value["zeroPoint"])):
        _refuse("Native display cache values have an unsupported shape.")


def _literal(node):
    """Unknown metadata stays exact, including text within its child structure."""
    if node is None:
        return None
    return node.tag, tuple(sorted(node.attrib.items())), node.text, tuple((_literal(child), child.tail) for child in node)


def _settings(previous, current, *, import_name=None):
    roots = [_xml(previous), _xml(current)]
    name_change = None
    old_name, new_name = (root.get("ResultName") for root in roots)
    if import_name is not None and old_name != new_name:
        if new_name != import_name:
            _refuse("Native import label does not match the requested archive filename.")
        name_change = {"path": "ResultsGroup/@ResultName", "previous": old_name, "current": new_name}
        if old_name is None:
            roots[1].attrib.pop("ResultName")
        else:
            roots[1].set("ResultName", old_name)
    values = []
    for root in roots:
        if any(len(root.findall(path)) != 1 for path in ("ResultsMap", "ResultsMap/Results", "SymbolsMap", "SpecialValuesMap")):
            _refuse("Native results/settings maps are missing or ambiguous.")
        _canonical(root, maps=True)  # Validate duplicates and complete stats before projection.
        values.append(_carrier(root)[0])
    left, right = ({node.tag: node for node in value} for value in values)
    changes = []
    for name in (set(left) | set(right)) - _CACHES:
        if name.startswith("MEC_") and _literal(left.get(name)) != _literal(right.get(name)):
            _refuse("Unsupported native cache metadata changed.")
    note = left.get("Note")
    if note is not None and "Note" not in right and note.attrib == {"type": "String"} and not len(note) and not (note.text or "") and not (note.tail or "").strip():
        values[0].remove(note)
        changes.append("SpecialValuesMap/SettingsMap/Note:empty_removed")
    for name in sorted(_CACHES):
        old, new = left.get(name), right.get(name)
        for parent, node in zip(values, (old, new)):
            if node is not None:
                _cache(node)
                parent.remove(node)
        if (ET.tostring(old) if old is not None else None) != (ET.tostring(new) if new is not None else None):
            changes.append("SpecialValuesMap/SettingsMap/" + name)
    if _canonical(roots[0], maps=True) != _canonical(roots[1], maps=True):
        _refuse("Native result, settings, identity, or unsupported metadata changed.")
    return changes, name_change


def verify_native_reserialization(previous, current, expected_token):
    """Verify a known carrier across a narrowly supported native serialization change."""
    return _verify(previous, current, expected_token)


def verify_native_import(previous, current, expected_token, archive_name):
    """Verify an explicit import, including the producer's filename-derived label."""
    from .sqx_databank_actions import _name, SqxDatabankActionError
    try:
        _name(archive_name)
        if not archive_name.lower().endswith(".sqx"):
            _refuse("Native import requires the exact .sqx filename.", "candidate_import_name_invalid")
        _name(archive_name[:-4])
    except SqxDatabankActionError as exc:
        raise SqxCandidateIdentityError("candidate_import_name_invalid", "Native import requires an ordinary .sqx filename without paths.") from exc
    return _verify(previous, current, expected_token, archive_name=archive_name)


def _verify(previous, current, expected_token, *, archive_name=None):
    expected_token = _token(expected_token)
    before, after = _archive(previous), _archive(current)
    if set(before) != set(after):
        _refuse("Native archive members changed.")
    for members in (before, after):
        if _carrier(_xml(members["settings.xml"]))[1] != expected_token:
            _refuse("Native Candidate token is missing or does not match custody.", "candidate_token_mismatch")
    display_changes, name_change = _settings(before["settings.xml"], after["settings.xml"],
        import_name=archive_name[:-4] if archive_name is not None else None)
    for name in before:
        if name == "settings.xml":
            continue
        if name == "lastSettings.xml":
            if _canonical(_xml(before[name]), maps=False) != _canonical(_xml(after[name]), maps=False):
                _refuse("Native task configuration changed.")
        elif before[name] != after[name]:
            _refuse("Native strategy, trades, equity, or another archive member changed.")
    proof = {"schema": "tc.sqx-native-import.v1" if archive_name is not None else "tc.sqx-native-reserialization.v1", "previous_sha256": sha256(previous).hexdigest(),
        "current_sha256": sha256(current).hexdigest(), "changed_members": sorted(name for name in before if before[name] != after[name]),
        "allowed_display_changes": display_changes}
    if archive_name is not None:
        proof.update(archive_name=archive_name, allowed_name_change=name_change)
    return proof
