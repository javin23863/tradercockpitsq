"""Guard Cursor Cloud Agent environment.json against schema-invalid ports."""

from __future__ import annotations

import json
import unittest
from pathlib import Path


_REPO = Path(__file__).resolve().parents[2]
_ENV_PATH = _REPO / ".cursor" / "environment.json"


class CursorEnvironmentTests(unittest.TestCase):
    def _payload(self) -> dict:
        return json.loads(_ENV_PATH.read_text(encoding="utf-8"))

    def test_ports_are_named_objects(self) -> None:
        ports = self._payload().get("ports")
        self.assertIsInstance(ports, list)
        self.assertGreater(len(ports), 0)
        for item in ports:
            self.assertIsInstance(item, dict)
            self.assertIsInstance(item.get("port"), int)
            self.assertGreaterEqual(item["port"], 1)
            self.assertLessEqual(item["port"], 65535)

    def test_install_uses_venv_and_editable_package(self) -> None:
        install = self._payload().get("install", "")
        self.assertIn("python3 -m venv", install)
        self.assertIn("pip install --no-deps -e .", install)

    def test_no_boot_terminal(self) -> None:
        # A crashing app-server tmux session fails the Cloud Agent boot.
        self.assertNotIn("terminals", self._payload())

    def test_dockerfile_exists_when_build_is_set(self) -> None:
        build = self._payload().get("build") or {}
        dockerfile = build.get("dockerfile")
        self.assertTrue(dockerfile)
        self.assertTrue((_REPO / ".cursor" / dockerfile).is_file())


if __name__ == "__main__":
    unittest.main()
