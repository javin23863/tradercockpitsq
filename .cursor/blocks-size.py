import json
import urllib.request

with urllib.request.urlopen("http://127.0.0.1:4320/api/sqx-project-topology?project=Builder", timeout=30) as response:
    payload = json.loads(response.read())
blocks = next(section for section in payload["tasks"][0]["settings"] if section.get("tag") == "Blocks")
for child in blocks.get("children") or []:
    blob = json.dumps(child)
    kids = child.get("children") or []
    attrs = list((child.get("attributes") or {}).keys())[:8]
    print(f"{child.get('tag'):20} kids={len(kids):5} json={len(blob):8} attrs={attrs}")
    for grandchild in kids[:8]:
        gblob = json.dumps(grandchild)
        gkids = grandchild.get("children") or []
        print(f"  {grandchild.get('tag'):18} kids={len(gkids):5} json={len(gblob):8}")
