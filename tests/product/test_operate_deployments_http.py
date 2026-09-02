from __future__ import annotations

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch

from tradercockpit.operate_deployments import OperateDeploymentError
from tradercockpit.operate_deployments_http import (
    operate_deployment_write_response,
    operate_deployments_response,
)
from tradercockpit.research_custody import FileResearchCustodyStore


class OperateDeploymentsHttpTests(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.store = FileResearchCustodyStore(Path(self.tmp.name) / "data")

    def test_unbound_store_is_503(self) -> None:
        status, payload = operate_deployments_response(None)
        self.assertEqual(status, 503)
        self.assertEqual(payload["reason_code"], "research_store_not_bound")

    def test_catalog_and_exact_read_delegate_to_deployment_custody(self) -> None:
        with patch(
            "tradercockpit.operate_deployments_http.list_current_deployments",
            return_value={"schema": "tc.operate-deployment-catalog.v1", "deployments": []},
        ):
            status, payload = operate_deployments_response(self.store)
        self.assertEqual(status, 200)
        self.assertEqual(payload["deployments"], [])

        with patch(
            "tradercockpit.operate_deployments_http.read_current_deployment",
            return_value={"schema": "tc.operate-deployment.v1", "entity_id": "deployment"},
        ):
            status, payload = operate_deployments_response(self.store, entity_id="deployment")
        self.assertEqual(status, 200)
        self.assertEqual(payload["schema"], "tc.operate-deployment.v1")

    def test_deploy_write_requires_exact_action_and_identity(self) -> None:
        status, payload = operate_deployment_write_response(self.store, {"action": "export"})
        self.assertEqual(status, 400)
        self.assertEqual(payload["reason_code"], "operate_deployment_action_invalid")

        request = {"action": "deploy", "export_entity_id": "tc-research:export:v1:eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"}
        with patch(
            "tradercockpit.operate_deployments_http.create_deployment",
            return_value={"schema": "tc.operate-deployment.v1", "reused": False},
        ):
            status, _ = operate_deployment_write_response(self.store, request)
        self.assertEqual(status, 201)

        with patch(
            "tradercockpit.operate_deployments_http.create_deployment",
            return_value={"schema": "tc.operate-deployment.v1", "reused": True},
        ):
            status, _ = operate_deployment_write_response(self.store, request)
        self.assertEqual(status, 200)

    def test_deployment_failures_are_typed_http_state(self) -> None:
        with patch(
            "tradercockpit.operate_deployments_http.create_deployment",
            side_effect=OperateDeploymentError("current_pointer_missing", "no export"),
        ):
            status, payload = operate_deployment_write_response(
                self.store,
                {"action": "deploy", "export_entity_id": "tc-research:export:v1:eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"},
            )
        self.assertEqual(status, 404)
        self.assertEqual(payload["reason_code"], "current_pointer_missing")


if __name__ == "__main__":
    unittest.main()
