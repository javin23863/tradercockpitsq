"""Write Cursor MCP config that launches MT5 (and optional TradingView Desktop) MCP.

Does not copy secret values into the JSON. The MT5 wrapper loads the secrets file.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
WRAPPER = ROOT / "tools" / "run_operator_mt5_mcp.py"


def main() -> int:
    target = Path.home() / ".cursor" / "mcp.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    existing: dict = {}
    if target.is_file():
        try:
            loaded = json.loads(target.read_text(encoding="utf-8"))
            if isinstance(loaded, dict):
                existing = loaded
        except json.JSONDecodeError:
            existing = {}
    servers = existing.get("mcpServers")
    if not isinstance(servers, dict):
        servers = {}
    python = sys.executable
    servers["metatrader5"] = {
        "command": python,
        "args": [str(WRAPPER)],
    }
    tv_root = (os.environ.get("TRADINGVIEW_MCP_ROOT") or "").strip()
    if tv_root:
        server_js = Path(tv_root) / "src" / "server.js"
        if server_js.is_file():
            servers["tradingview-desktop"] = {
                "command": "node",
                "args": [str(server_js)],
            }
    existing["mcpServers"] = servers
    target.write_text(json.dumps(existing, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
