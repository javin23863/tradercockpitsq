"""Guard Cursor Cloud Agent environment.json against schema-invalid ports."""

from __future__ import annotations

import json
import unittest
from pathlib import Path


_ENV_PATH = Path(__file__).resolve().parents[2] / ".cursor" / "environment.json"


class CursorEnvironmentTests(unittest.TestCase):
    def test_ports_are_named_objects(self) -> None:
        payload = json.loads(_ENV_PATH.read_text(encoding="utf-8"))
        self.assertIsInstance(payload.get("ports"), list)
        self.assertGreater(len(payload["ports"]), 0)
        for item in payload["ports"]:
            self.assertIsInstance(item, dict)
            self.assertIsInstance(item.get("port"), int)
            self.assertGreaterEqual(item["port"], 1)
            self.assertLessEqual(item["port"], 65535)

    def test_install_and_app_server_terminal_exist(self) -> None:
        payload = json.loads(_ENV_PATH.read_text(encoding="utf-8"))
        self.assertIn("pip install --no-deps -e .", payload.get("install", ""))
        terminals = payload.get("terminals") or []
        self.assertTrue(terminals)
        commands = " ".join(str(item.get("command", "")) for item in terminals)
        self.assertIn("tradercockpit.app_server", commands)
        self.assertIn("--port 4173", commands)


if __name__ == "__main__":
    unittest.main()
