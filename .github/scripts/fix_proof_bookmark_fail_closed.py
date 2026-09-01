from pathlib import Path

web = Path("web/research-proof.mjs")
text = web.read_text(encoding="utf-8")
old = '''function proofEntityFromLocation() {\n  const value = new URLSearchParams(globalThis.location?.search || "").get("proofEntity");\n  return typeof value === "string" && /^tc-research:proof:v1:[0-9a-f-]{36}$/.test(value) ? value : "";\n}\n'''
new = '''export function proofEntityFromLocation(search = globalThis.location?.search || "") {\n  const params = new URLSearchParams(search);\n  if (!params.has("proofEntity")) return { present: false, entityId: "" };\n  const value = params.get("proofEntity");\n  return {\n    present: true,\n    entityId: typeof value === "string" && /^tc-research:proof:v1:[0-9a-f-]{36}$/.test(value) ? value : "",\n  };\n}\n'''
if old not in text:
    raise SystemExit("bookmark helper anchor mismatch")
text = text.replace(old, new, 1)
old_load = '''    const bookmarked = proofEntityFromLocation();\n    if (bookmarked) {\n      const proof = await fetchProof(bookmarked);\n'''
new_load = '''    const bookmarked = proofEntityFromLocation();\n    if (bookmarked.present) {\n      if (!bookmarked.entityId) throw new Error("Bookmarked Research Proof identity is invalid");\n      const proof = await fetchProof(bookmarked.entityId);\n'''
if old_load not in text:
    raise SystemExit("bookmark load anchor mismatch")
text = text.replace(old_load, new_load, 1)
web.write_text(text, encoding="utf-8")

tests = Path("tests/research-proof.test.mjs")
test_text = tests.read_text(encoding="utf-8")
old_import = '''  proofCatalogFromPayload,\n  proofFromPayload,\n  proofSelections,\n} from "../web/research-proof.mjs";\n'''
new_import = '''  proofCatalogFromPayload,\n  proofEntityFromLocation,\n  proofFromPayload,\n  proofSelections,\n} from "../web/research-proof.mjs";\n'''
if old_import not in test_text:
    raise SystemExit("test import anchor mismatch")
test_text = test_text.replace(old_import, new_import, 1)
anchor = '''test("Proof parser accepts one exact bound chain and keeps verdict unread", () => {\n'''
regression = '''test("Proof bookmark distinguishes absent from malformed identity", () => {\n  assert.deepEqual(proofEntityFromLocation("?stage=proof"), { present: false, entityId: "" });\n  assert.deepEqual(proofEntityFromLocation("?stage=proof&proofEntity=not-a-proof"), { present: true, entityId: "" });\n  const valid = entity("proof", "7");\n  assert.deepEqual(\n    proofEntityFromLocation(`?stage=proof&proofEntity=${encodeURIComponent(valid)}`),\n    { present: true, entityId: valid },\n  );\n});\n\n''' + anchor
if anchor not in test_text:
    raise SystemExit("bookmark regression anchor mismatch")
test_text = test_text.replace(anchor, regression, 1)
tests.write_text(test_text, encoding="utf-8")
