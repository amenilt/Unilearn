from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.core.security import UserContext, require_student
from app.schemas.analysis import (
    FailureRiskRequest,
    FailureRiskResponse,
    PerformanceAnalysisRequest,
    PerformanceAnalysisResponse,
)
from app.services.analysis import analyze_results, predict_failure_risk

router = APIRouter(prefix="/api/v1/analysis", tags=["performance analysis"])


@router.post(
    "/results",
    response_model=PerformanceAnalysisResponse,
    summary="Summarize student assessment results",
    description="Returns overall and per-course averages, median, and chronological score change for the submitted data. Results are not persisted or verified against MS2.",
)
def analyze_my_results(
    body: PerformanceAnalysisRequest,
    _user: UserContext = Depends(require_student),
) -> PerformanceAnalysisResponse:
    return analyze_results(body)


@router.post(
    "/failure-risk",
    response_model=FailureRiskResponse,
    summary="Estimate an explainable failure-risk indicator",
    description="Applies configurable heuristic thresholds to submitted scores, completion, and missed assessments. This is not a calibrated probability or an academic decision.",
)
def estimate_my_failure_risk(
    body: FailureRiskRequest,
    _user: UserContext = Depends(require_student),
    settings: Settings = Depends(get_settings),
) -> FailureRiskResponse:
    return predict_failure_risk(body, settings)
