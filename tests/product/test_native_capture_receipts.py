"""Test the receipt reader used by installed-producer acceptance, not a producer fixture."""
from hashlib import sha256
import importlib.util
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from uuid import uuid4
from xml.etree import ElementTree as E

spec = importlib.util.spec_from_file_location("capture_receipts", Path(__file__).parents[1] / "native_stage_capture/receipts.py")
receipts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(receipts)


class NativeCaptureReceiptTests(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.binding = dict(project="P", run=str(uuid4()), checkpoint="capture", task_entry="CustomAnalysis-Task2.xml",
                            task="Capture", databank="Results", graph_sha256="a" * 64)
        self.visit = str(uuid4()); self.folder = self.root / "capture" / self.visit
        self.folder.mkdir(parents=True)
        self.started = dict(self.binding, schema=receipts.SCHEMA, visit=self.visit, count="1", started="2026-09-06T00:00:00Z")
        self.write("started.xml", self.started)

    def write(self, name, data):
        root = E.Element("properties")
        for key, value in data.items(): E.SubElement(root, "entry", key=key).text = value
        (self.folder / name).write_bytes(b'<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">' + E.tostring(root))

    def complete(self, count=1):
        self.started["count"] = str(count); self.write("started.xml", self.started)
        data = dict(self.started, completed="2026-09-06T00:00:01Z")
        for i in range(count):
            raw = f"opaque captured bytes {i}".encode(); (self.folder / f"{i}.sqx").write_bytes(raw)
            data.update({f"artifact.{i}.name": f"Native {i}", f"artifact.{i}.sha256": sha256(raw).hexdigest(), f"artifact.{i}.bytes": str(len(raw))})
        self.write("completed.xml", data)
        return data

    def read(self, binding=None):
        return receipts.read_visit(self.root, self.visit, binding or self.binding)

    def test_completed_retry_and_reopen_are_stable_without_inferring_a_verdict(self):
        self.complete()
        first = self.read(); self.assertEqual(first, self.read())
        self.assertEqual(first["state"], "completed"); self.assertEqual(first["native_count"], 1)
        self.assertEqual(len(first["artifacts"]), 1)
        self.assertNotIn("candidate", first); self.assertNotIn("verdict", first)

    def test_started_only_pending_write_and_explicit_failure_stay_distinct(self):
        (self.folder / "completed.xml.pending").write_bytes(b"interrupted write")
        (self.folder / "0.sqx").write_bytes(b"unconfirmed copy")
        result = self.read(); self.assertEqual(result["state"], "capture_incomplete")
        self.assertEqual(result["artifacts"], [])
        self.write("failed.xml", dict(self.started, failed="2026-09-06T00:00:01Z", error_type="java.io.IOException"))
        self.assertEqual(self.read()["state"], "capture_failed")
        self.complete()
        with self.assertRaises(ValueError): self.read()

    def test_empty_completed_checkpoint_is_an_observed_zero_not_missing_capture(self):
        self.complete(0)
        self.assertEqual((self.read()["state"], self.read()["native_count"]), ("completed", 0))

    def test_failed_completion_publication_retains_verified_artifacts_without_completion(self):
        data = self.complete()
        (self.folder / "completed.xml").unlink()
        (self.folder / "completed.xml.pending").mkdir()
        data.pop("completed")
        self.write("failed.xml", dict(data, failed="2026-09-06T00:00:01Z", error_type="java.io.FileNotFoundException"))
        result = self.read()
        self.assertEqual(result["state"], "capture_failed")
        self.assertIsNone(result["completed"])
        self.assertEqual(len(result["artifacts"]), 1)

    def test_every_expected_binding_field_is_checked(self):
        self.complete()
        for key in self.binding:
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.read({**self.binding, key: "wrong"})

    def test_missing_tampered_extra_and_linked_archives_refuse(self):
        self.complete(); archive = self.folder / "0.sqx"; raw = archive.read_bytes()
        archive.unlink()
        with self.assertRaises(ValueError): self.read()
        archive.write_bytes(b"changed")
        with self.assertRaises(ValueError): self.read()
        archive.write_bytes(raw); extra = self.folder / "1.sqx"; extra.write_bytes(raw)
        with self.assertRaises(ValueError): self.read()
        extra.unlink(); extra.hardlink_to(archive)
        with self.assertRaises(ValueError): self.read()

    def test_partial_fields_identity_changes_and_backward_timestamps_refuse(self):
        data = self.complete()
        for changed in (dict(data, count="2"), dict(data, completed="2026-09-05T00:00:00Z"), {k:v for k,v in data.items() if k != "artifact.0.sha256"}):
            self.write("completed.xml", changed)
            with self.assertRaises(ValueError): self.read()

    def test_duplicate_fields_and_entity_declarations_refuse(self):
        self.complete(); path = self.folder / "started.xml"; raw = path.read_bytes()
        for changed in (raw.replace(b"</properties>", b'<entry key="count">1</entry></properties>'),
                        b'<!DOCTYPE properties [<!ENTITY bad "expanded">]><properties/>',
                        '<!DOCTYPE properties [<!ENTITY bad "expanded">]><properties/>'.encode("utf-16")):
            path.write_bytes(changed)
            with self.assertRaises((ValueError, E.ParseError)): self.read()


if __name__ == "__main__": unittest.main()
