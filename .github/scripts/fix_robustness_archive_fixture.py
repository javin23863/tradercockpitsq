from pathlib import Path

path = Path("tests/product/test_research_robustness.py")
text = path.read_text(encoding="utf-8")
old_import = "from zipfile import ZipFile\n"
new_import = "from zipfile import ZipFile, ZipInfo\n"
if old_import not in text:
    raise SystemExit("zipfile import anchor mismatch")
text = text.replace(old_import, new_import, 1)
old_helper = '''    @staticmethod\n    def _archive_bytes(marker: str) -> bytes:\n        stream = BytesIO()\n        with ZipFile(stream, "w") as archive:\n            archive.writestr("settings.xml", f"<Settings>{marker}</Settings>".encode())\n            archive.writestr("strategy_Portfolio.xml", f"<Strategy>{marker}</Strategy>".encode())\n            archive.writestr("version.txt", b"144.2953")\n            archive.writestr("orders.bin", marker.encode())\n        return stream.getvalue()\n'''
new_helper = '''    @staticmethod\n    def _archive_bytes(marker: str) -> bytes:\n        stream = BytesIO()\n        entries = (\n            ("settings.xml", f"<Settings>{marker}</Settings>".encode()),\n            ("strategy_Portfolio.xml", f"<Strategy>{marker}</Strategy>".encode()),\n            ("version.txt", b"144.2953"),\n            ("orders.bin", marker.encode()),\n        )\n        with ZipFile(stream, "w") as archive:\n            for name, payload in entries:\n                # Fix the ZIP member timestamp so repeated construction of the same\n                # producer fixture is byte-identical even across wall-clock seconds.\n                info = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))\n                archive.writestr(info, payload)\n        return stream.getvalue()\n'''
if old_helper not in text:
    raise SystemExit("archive fixture helper anchor mismatch")
text = text.replace(old_helper, new_helper, 1)
path.write_text(text, encoding="utf-8")
