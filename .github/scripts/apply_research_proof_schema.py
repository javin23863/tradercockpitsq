from pathlib import Path

path = Path("product/tradercockpit/research_proof.py")
text = path.read_text(encoding="utf-8")

old_import = "from tradercockpit.research_ideas import ResearchIdeaContent, ResearchIdeaError\n"
new_import = "from tradercockpit.research_ideas import IDEA_READ_SCHEMA, ResearchIdeaContent, ResearchIdeaError\n"
if old_import not in text or "IDEA_READ_SCHEMA" in text:
    raise SystemExit("Research Proof Idea import anchor mismatch")
text = text.replace(old_import, new_import, 1)

old_record = '''    return {\n        "entity_id": str(entity),\n        "revision": str(revision),\n        "content_ref": str(stored.content),\n        "text": content.text,\n        "source": content.source,\n    }\n'''
new_record = '''    return {\n        "schema": IDEA_READ_SCHEMA,\n        "entity_id": str(entity),\n        "revision": str(revision),\n        "content_ref": str(stored.content),\n        "text": content.text,\n        "source": content.source,\n    }\n'''
if old_record not in text:
    raise SystemExit("Research Proof Idea readback anchor mismatch")
text = text.replace(old_record, new_record, 1)

path.write_text(text, encoding="utf-8")
