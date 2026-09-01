from __future__ import annotations

import importlib.util
from pathlib import Path
import sys
import unittest


MODULE_PATH = Path(__file__).resolve().parents[2] / "tools" / "run_installed_sqx_research_acceptance.py"
SPEC = importlib.util.spec_from_file_location("installed_sqx_acceptance", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
acceptance = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = acceptance
SPEC.loader.exec_module(acceptance)


class FakeClient:
    def __init__(self) -> None:
        self.base_url = "http://127.0.0.1:4173"
        self.output_reads = 0
        self.source = "1" * 64
        self.xml = "2" * 64
        self.compiled_rev = "tc-research-revision:configuration:sha256:" + "3" * 64
        self.config_rev = "tc-research-revision:configuration:sha256:" + "4" * 64
        self.config_id = "tc-research:configuration:v1:11111111-1111-4111-8111-111111111111"
        self.job_id = "tc-research:native-job:v1:22222222-2222-4222-8222-222222222222"
        self.job_rev = "tc-research-revision:native-job:sha256:" + "5" * 64
        self.candidate_id = "tc-research:candidate:v1:33333333-3333-4333-8333-333333333333"
        self.candidate_rev = "tc-research-revision:candidate:sha256:" + "6" * 64
        self.candidate_sha = "7" * 64
        self.result_id = "tc-research:historical-result:v1:44444444-4444-4444-8444-444444444444"
        self.result_rev = "tc-research-revision:historical-result:sha256:" + "8" * 64
        self.result_sha = "9" * 64
        self.engine = "a" * 64
        self.orders = "b" * 64

    def _config(self):
        return {"entity_id": self.config_id, "revision": self.config_rev, "state": "approved",
                "source_project_sha256": self.source, "executable_xml_sha256": self.xml}

    def _job(self):
        return {"entity_id": self.job_id, "revision": self.job_rev, "state": "submitted",
                "configuration_revision": self.config_rev}

    def _candidate(self):
        return {"entity_id": self.candidate_id, "revision": self.candidate_rev,
                "native_job_revision": self.job_rev, "configuration_revision": self.config_rev,
                "association_mode": acceptance.ASSOCIATION, "archive_sha256": self.candidate_sha}

    def _result(self):
        return {"entity_id": self.result_id, "revision": self.result_rev, "state": "completed",
                "execution_completed": True, "candidate_revision": self.candidate_rev,
                "result_archive_sha256": self.result_sha, "engine_sha256": self.engine,
                "retester_task": 1, "trades_readback": {"state": "available", "payload": {
                    "schema": "tc.research-historical-trades.v1",
                    "historical_result_revision": self.result_rev,
                    "orders_entry_sha256": self.orders,
                    "orders_format": "SQOrderFileFormat:11", "orders_format_version": 11,
                    "native_order_count": 12, "trade_count": 7,
                    "selection": {"result_key": "Portfolio"},
                }}}

    def request(self, method, path, *, payload=None, query=None):
        if method == "GET" and path == acceptance.STATUS:
            return {"schema": "tc.runtime-status.v1"}
        if method == "GET" and path == acceptance.BUILDER:
            return {"archive_sha256": self.source}
        if method == "GET" and path == acceptance.OUTPUTS:
            self.output_reads += 1
            outputs = [{"archive": "Existing.sqx", "archive_sha256": "c" * 64, "inspectable": True}]
            if self.output_reads > 1:
                outputs.append({"archive": "Survivor.sqx", "archive_sha256": self.candidate_sha, "inspectable": True})
            return {"outputs": outputs}
        if method == "POST" and path == acceptance.CONFIGS and payload == {"action": "compile"}:
            return {"entity_id": self.config_id, "revision": self.compiled_rev, "state": "compiled",
                    "source_project_sha256": self.source, "executable_xml_sha256": self.xml}
        if method == "POST" and path == acceptance.CONFIGS:
            return self._config()
        if method == "POST" and path == acceptance.JOBS:
            return self._job()
        if method == "POST" and path == acceptance.CANDIDATES:
            return self._candidate()
        if method == "POST" and path == acceptance.RESULTS:
            return self._result()
        if method == "GET" and path == acceptance.CONFIGS:
            return self._config()
        if method == "GET" and path == acceptance.JOBS:
            return self._job()
        if method == "GET" and path == acceptance.CANDIDATES:
            return self._candidate()
        if method == "GET" and path == acceptance.RESULTS:
            return self._result()
        raise AssertionError((method, path, payload, query))


class InstalledSqxResearchAcceptanceTests(unittest.TestCase):
    def test_full_chain_and_reopen_verification(self) -> None:
        client = FakeClient()
        started = acceptance.start(client)
        self.assertEqual(started["candidate_archive_options"], [{
            "archive": "Survivor.sqx", "archive_sha256": client.candidate_sha, "change": "new",
        }])

        completed = acceptance.finish(client, started, "Survivor.sqx")
        self.assertEqual(completed["stage"], "completed")
        self.assertEqual(completed["identities"]["retester_engine_sha256"], client.engine)
        self.assertEqual(completed["trades"]["trade_count"], 7)

        reopened = acceptance.verify(client, completed)
        self.assertEqual(reopened["stage"], "reopen_verified")
        self.assertEqual(reopened["identities"], completed["identities"])

    def test_finish_refuses_preexisting_unchanged_archive(self) -> None:
        client = FakeClient()
        started = acceptance.start(client)
        with self.assertRaises(acceptance.AcceptanceError) as caught:
            acceptance.finish(client, started, "Existing.sqx")
        self.assertEqual(caught.exception.code, "archive_not_observed")

    def test_base_url_is_literal_loopback_only(self) -> None:
        self.assertEqual(acceptance._base_url("http://127.0.0.1:4173"), "http://127.0.0.1:4173")
        self.assertEqual(acceptance._base_url("http://[::1]:4173"), "http://[::1]:4173")
        for value in ("http://example.com:4173", "https://127.0.0.1:4173", "http://127.0.0.1"):
            with self.assertRaises(acceptance.AcceptanceError):
                acceptance._base_url(value)


if __name__ == "__main__":
    unittest.main()
