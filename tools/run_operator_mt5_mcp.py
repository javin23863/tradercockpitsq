"""Start the operator MetaTrader MCP after loading the secrets file.

Credentials stay in the secrets file. This wrapper does not print them.
"""

from __future__ import annotations

import os
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "product"))

from tradercockpit.secrets_store import SecretsStoreError, apply_operator_secrets  # noqa: E402


def main() -> int:
    try:
        apply_operator_secrets()
    except SecretsStoreError as exc:
        print(f"TraderCockpit secrets: {exc.detail}", file=sys.stderr)
        return 1
    uvx = shutil.which("uvx")
    if not uvx:
        print("uvx is required to run mcp-metatrader5-server", file=sys.stderr)
        return 1
    os.execv(uvx, [uvx, "--from", "mcp-metatrader5-server", "mt5mcp"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
