from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

from tradercockpit.desktop import (
    DESKTOP_LOOPBACK_ADVERT_SCHEMA,
    DESKTOP_WINDOW_OBSERVATION_SCHEMA,
    _record_window_observation,
)


class DesktopWindowObservationTests(unittest.TestCase):
    def observation(self) -> dict[str, object]:
        return {
            "schema": DESKTOP_WINDOW_OBSERVATION_SCHEMA,
            "location_pathname": "/research",
            "location_search": "",
            "document_title": "TraderCockpit — Research / Construct / Idea",
            "product_shell": "tradercockpit-desktop",
            "surface_id": "research",
            "research_stage_id": "construct",
            "research_tab_id": "idea",
            "page_heading": "Research",
            "idea_workspace": True,
            "idea_save_action": True,
        }

    def test_actual_window_observation_is_atomically_attached_to_loopback_advert(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "desktop-loopback.json"
            path.write_text(
                json.dumps(
                    {
                        "schema": DESKTOP_LOOPBACK_ADVERT_SCHEMA,
                        "product": "tradercockpit",
                        "url": "http://127.0.0.1:4174/research",
                        "pid": 123,
                    }
                ),
                encoding="utf-8",
            )
            self.assertTrue(_record_window_observation(path, self.observation()))
            advert = json.loads(path.read_text(encoding="utf-8"))
            observed = advert["window_observation"]
            self.assertEqual(observed["schema"], DESKTOP_WINDOW_OBSERVATION_SCHEMA)
            self.assertEqual(observed["location_pathname"], "/research")
            self.assertEqual(observed["product_shell"], "tradercockpit-desktop")
            self.assertEqual(observed["surface_id"], "research")
            self.assertEqual(observed["research_stage_id"], "construct")
            self.assertEqual(observed["research_tab_id"], "idea")
            self.assertIs(observed["idea_workspace"], True)
            self.assertIs(observed["idea_save_action"], True)
            self.assertFalse(path.with_name(path.name + ".window-observation.tmp").exists())

    def test_invalid_or_noncanonical_observation_is_never_published(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "desktop-loopback.json"
            path.write_text(
                json.dumps(
                    {
                        "schema": DESKTOP_LOOPBACK_ADVERT_SCHEMA,
                        "product": "tradercockpit",
                        "url": "http://127.0.0.1:4174/research",
                        "pid": 123,
                    }
                ),
                encoding="utf-8",
            )
            invalid = self.observation()
            invalid["idea_workspace"] = "true"
            self.assertFalse(_record_window_observation(path, invalid))
            advert = json.loads(path.read_text(encoding="utf-8"))
            self.assertNotIn("window_observation", advert)


if __name__ == "__main__":
    unittest.main()
