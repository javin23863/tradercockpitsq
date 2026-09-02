from __future__ import annotations

import json
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Barrier, Thread
import unittest

from tradercockpit.atomic_io import atomic_write_json, atomic_write_text


class AtomicIoTests(unittest.TestCase):
    def test_write_json_is_valid_and_leaves_no_temp_files(self) -> None:
        with TemporaryDirectory() as tmp:
            path = Path(tmp) / "store.json"
            atomic_write_json(path, {"b": 2, "a": 1})
            self.assertEqual(json.loads(path.read_text(encoding="utf-8")), {"a": 1, "b": 2})
            # Sorted keys, trailing newline, and no stray temp files remain.
            self.assertTrue(path.read_text(encoding="utf-8").endswith("}\n"))
            self.assertEqual([p.name for p in Path(tmp).iterdir()], ["store.json"])

    def test_write_text_creates_parent_directories(self) -> None:
        with TemporaryDirectory() as tmp:
            path = Path(tmp) / "nested" / "deep" / "advert.json"
            atomic_write_text(path, "{}\n")
            self.assertEqual(path.read_text(encoding="utf-8"), "{}\n")

    def test_concurrent_writers_never_corrupt_the_file(self) -> None:
        with TemporaryDirectory() as tmp:
            path = Path(tmp) / "store.json"
            atomic_write_json(path, {"n": -1})
            writers = 16
            barrier = Barrier(writers)

            def write(n: int) -> None:
                barrier.wait()
                for _ in range(50):
                    atomic_write_json(path, {"n": n, "payload": list(range(n, n + 40))})

            threads = [Thread(target=write, args=(i,)) for i in range(writers)]
            for thread in threads:
                thread.start()
            for thread in threads:
                thread.join()

            # Every read during the storm would have been a complete, valid document;
            # the final file parses and no temp files were left behind.
            parsed = json.loads(path.read_text(encoding="utf-8"))
            self.assertIn(parsed["n"], range(writers))
            self.assertEqual(sorted(p.name for p in Path(tmp).iterdir()), ["store.json"])


if __name__ == "__main__":
    unittest.main()
