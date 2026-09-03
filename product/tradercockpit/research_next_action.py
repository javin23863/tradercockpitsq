"""One legal next Research action derived from custody catalogs.

The product leads the owner through Idea → Specification → Build → Candidates →
Backtest → Proof. This read model names the current stage and the single next
action; it does not invent producer state or skip approval.
"""

from __future__ import annotations

from typing import Mapping, Sequence


RESEARCH_NEXT_ACTION_SCHEMA = "tc.research-next-action.v1"
RESEARCH_NEXT_ACTION_API_PATH = "/api/research/next-action"

_CHAIN = (
    "idea",
    "specification",
    "build",
    "candidates",
    "backtest",
    "proof",
)


def _action(
    action_id: str,
    label: str,
    path: str,
    *,
    current_stage: str,
    detail: str,
) -> dict[str, object]:
    locked = [stage for stage in _CHAIN if _CHAIN.index(stage) > _CHAIN.index(current_stage)] if current_stage in _CHAIN else list(_CHAIN)
    return {
        "schema": RESEARCH_NEXT_ACTION_SCHEMA,
        "current_stage": current_stage,
        "next_action": {
            "id": action_id,
            "label": label,
            "path": path,
        },
        "locked_stages": locked,
        "detail": detail,
    }


def next_action_from_catalogs(
    *,
    ideas: Sequence[Mapping[str, object]] = (),
    configurations: Sequence[Mapping[str, object]] = (),
    jobs: Sequence[Mapping[str, object]] = (),
    candidates: Sequence[Mapping[str, object]] = (),
    results: Sequence[Mapping[str, object]] = (),
    proofs: Sequence[Mapping[str, object]] = (),
    open_questions: int = 0,
) -> dict[str, object]:
    """Derive the one legal next action from custody catalogs already read."""

    if not ideas:
        return _action(
            "create_idea",
            "Create an Idea",
            "/research?workspace=signals&tab=overview",
            current_stage="idea",
            detail="Text entry mints Idea custody only. It does not create a candidate or launch native compute.",
        )
    if not configurations:
        if open_questions:
            return _action(
                "answer_clarifying_questions",
                "Answer clarifying questions",
                "/research?workspace=signals&tab=signals",
                current_stage="specification",
                detail="Unresolved Specification fields become typed Apollo questions. Build stays locked until required meaning is resolved.",
            )
        return _action(
            "specify_and_compile",
            "Specify and compile the plan",
            "/research?workspace=signals&tab=signals",
            current_stage="specification",
            detail="Resolve native or Models requirements, then compile the exact Builder task.",
        )
    if not any(item.get("state") == "approved" for item in configurations):
        return _action(
            "approve_configuration",
            "Review and approve the configuration",
            "/research?workspace=evolution",
            current_stage="build",
            detail="Approval binds the exact executable bytes. Launch stays locked until then.",
        )
    launched = any(item.get("state") in {"submitted", "completed"} for item in jobs)
    if not launched:
        return _action(
            "launch_builder",
            "Launch the approved Builder job",
            "/research?workspace=evolution",
            current_stage="build",
            detail="Native SQX owns generation. The gateway stages the approved Task-rooted configuration.",
        )
    if not candidates:
        return _action(
            "import_candidates",
            "Import native survivors",
            "/research?workspace=evolution",
            current_stage="candidates",
            detail="Candidates come from exact native Results archives, never from UI text.",
        )
    completed = any(item.get("state") == "completed" for item in results)
    if not completed:
        return _action(
            "run_historical_test",
            "Run the historical test",
            "/research?workspace=validate&tab=overview",
            current_stage="backtest",
            detail="Native Retester or an approved Custom Project produces the Historical Result.",
        )
    if not proofs:
        return _action(
            "create_proof",
            "Bind Proof",
            "/research?workspace=validate&tab=evidence",
            current_stage="proof",
            detail="Proof binds the exact Idea, configuration, job, candidate, and result chain.",
        )
    return {
        "schema": RESEARCH_NEXT_ACTION_SCHEMA,
        "current_stage": "proof",
        "next_action": {
            "id": "maintain",
            "label": "Maintain this revision",
            "path": "/research?workspace=catalog&tab=all",
        },
        "locked_stages": [],
        "detail": "The historical chain is bound. Operate stays empty until live producers exist.",
    }


def research_next_action_record(research_store: object | None, *, sqx_home: object | None = None) -> dict[str, object]:
    """Read current custody catalogs and return the next-action read model."""

    if research_store is None:
        return {
            "schema": RESEARCH_NEXT_ACTION_SCHEMA,
            "current_stage": None,
            "next_action": None,
            "locked_stages": list(_CHAIN),
            "detail": "Research custody is not connected.",
            "reason_code": "custody_unavailable",
        }

    from tradercockpit.research_candidates import list_current_candidates
    from tradercockpit.research_clarifying_questions import open_question_count
    from tradercockpit.research_configurations import list_current_configurations
    from tradercockpit.research_ideas import list_current_ideas
    from tradercockpit.research_native_jobs import list_current_native_jobs
    from tradercockpit.research_proof import list_current_research_proofs
    from tradercockpit.research_retester import list_current_historical_results

    ideas = list_current_ideas(research_store).get("ideas") or ()
    return next_action_from_catalogs(
        ideas=ideas,
        configurations=list_current_configurations(research_store).get("configurations") or (),
        jobs=list_current_native_jobs(research_store).get("jobs") or (),
        candidates=list_current_candidates(research_store).get("candidates") or (),
        results=list_current_historical_results(research_store).get("results") or (),
        proofs=list_current_research_proofs(research_store).get("proofs") or (),
        open_questions=open_question_count(research_store, sqx_home=sqx_home) if ideas else 0,
    )
