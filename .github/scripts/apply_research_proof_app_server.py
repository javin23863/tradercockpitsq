from pathlib import Path

path = Path("product/tradercockpit/app_server.py")
text = path.read_text(encoding="utf-8")

imports_anchor = '''from tradercockpit.research_native_jobs import (\n    ResearchNativeJobError,\n    launch_approved_builder_configuration,\n    list_current_native_jobs,\n    read_current_native_job,\n)\n'''
imports_insert = imports_anchor + '''from tradercockpit.research_proof_http import (\n    RESEARCH_PROOFS_API_PATH,\n    research_proof_write_response,\n    research_proofs_response,\n)\n'''
if imports_anchor not in text or "from tradercockpit.research_proof_http import" in text:
    raise SystemExit("app_server Proof import anchor mismatch")
text = text.replace(imports_anchor, imports_insert, 1)

get_anchor = '''            if parsed.path == SQX_PRESETS_API_PATH:\n'''
get_block = '''            if parsed.path == RESEARCH_PROOFS_API_PATH:\n                if not self._research_client_is_loopback():\n                    self._reject_non_loopback_research_request()\n                    return\n                query = parse_qs(parsed.query, keep_blank_values=True)\n                if set(query) - {"entityId"}:\n                    self._json(400, {"error": "invalid_request", "detail": "unsupported query parameter"})\n                    return\n                entity_ids = query.get("entityId", [])\n                if len(entity_ids) > 1 or (entity_ids and not entity_ids[0]):\n                    self._json(400, {"error": "invalid_request", "detail": "at most one non-empty entityId is allowed"})\n                    return\n                status, payload = research_proofs_response(\n                    research_store,\n                    entity_id=entity_ids[0] if entity_ids else None,\n                )\n                self._json(status, payload)\n                return\n\n''' + get_anchor
if get_anchor not in text or "if parsed.path == RESEARCH_PROOFS_API_PATH:" in text:
    raise SystemExit("app_server Proof GET anchor mismatch")
text = text.replace(get_anchor, get_block, 1)

post_anchor = '''            if parsed.path.startswith("/api/"):\n                self._json(\n                    405,\n'''
post_block = '''            if parsed.path == RESEARCH_PROOFS_API_PATH:\n                if not self._research_client_is_loopback():\n                    self._reject_non_loopback_research_request()\n                    return\n                if parsed.query:\n                    self._json(400, {"error": "invalid_request", "detail": "Proof writes accept no query parameters"})\n                    return\n                payload = self._request_json()\n                if payload is None:\n                    return\n                status, response = research_proof_write_response(research_store, payload)\n                self._json(status, response)\n                return\n\n''' + post_anchor
if post_anchor not in text:
    raise SystemExit("app_server Proof POST anchor mismatch")
text = text.replace(post_anchor, post_block, 1)

path.write_text(text, encoding="utf-8")
