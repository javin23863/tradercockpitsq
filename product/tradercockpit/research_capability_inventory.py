"""Backend-owned inventory of the current user-facing Research capability families."""

from __future__ import annotations

from typing import Final


RESEARCH_CAPABILITY_INVENTORY_SCHEMA: Final = "tc.research-capability-inventory.v1"

_CAPABILITIES: Final = (
    ("idea_revision_custody", "canonical_read_model", ("/api/research/ideas",)),
    ("builder_native_specification", "canonical_read_model", ("/api/sqx-builder-config",)),
    ("native_preset_inspection", "canonical_read_model", ("/api/sqx-presets",)),
    ("builder_configuration_custody", "canonical_read_model", ("/api/research/configurations",)),
    ("native_builder_execution", "canonical_read_model", ("/api/research/native-jobs",)),
    ("native_output_candidate_import", "canonical_read_model", ("/api/sqx-outputs", "/api/research/candidates")),
    ("native_historical_retester", "canonical_read_model", ("/api/research/historical-results",)),
    ("native_trade_rows", "canonical_read_model", ("/api/research/historical-results",)),
    (
        "executed_chain_inspection",
        "canonical_read_model",
        (
            "/api/research/configurations",
            "/api/research/native-jobs",
            "/api/research/candidates",
            "/api/research/historical-results",
        ),
    ),
    ("native_higher_precision_robustness", "canonical_read_model", ("/api/research/historical-results",)),
    ("native_custom_project_topology", "canonical_read_model", ("/api/sqx-project-topology",)),
    ("research_proof", "canonical_read_model", ("/api/research/proofs", "/api/status")),
    ("typed_rule_block_authoring", "not_exposed", ()),
    ("typed_search_parameter_authoring", "not_exposed", ()),
    ("typed_data_trading_input_authoring", "not_exposed", ()),
    ("typed_money_management_atm_authoring", "not_exposed", ()),
    ("robustness_method_family_depth", "not_exposed", ()),
    ("robustness_producer_outcome_readback", "not_exposed", ()),
    ("custom_project_task_parameter_control", "not_exposed", ()),
    ("historical_performance_metrics_readback", "not_exposed", ()),
)


def research_capability_inventory_record() -> dict[str, object]:
    """Return an independent backend inventory for frontend coverage reconciliation."""

    return {
        "schema": RESEARCH_CAPABILITY_INVENTORY_SCHEMA,
        "authority": "tradercockpit_backend_canonical_research_seams",
        "capabilities": [
            {
                "id": capability_id,
                "producer_exposure": exposure,
                "api_paths": list(api_paths),
            }
            for capability_id, exposure, api_paths in _CAPABILITIES
        ],
    }
