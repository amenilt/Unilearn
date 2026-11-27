from statistics import mean, median
from collections import defaultdict

from app.core.config import Settings
from app.schemas.analysis import (
    CoursePerformanceSummary,
    FailureRiskRequest,
    FailureRiskResponse,
    PerformanceAnalysisRequest,
    PerformanceAnalysisResponse,
    RiskSignal,
)


def analyze_results(request: PerformanceAnalysisRequest) -> PerformanceAnalysisResponse:
    ordered = sorted(request.results, key=lambda result: result.completed_at)
    scores = [result.score_percent for result in ordered]
    first_score = scores[0]
    latest_score = scores[-1]
    grouped = defaultdict(list)
    titles: dict[int, str] = {}
    for result in ordered:
        grouped[result.course_id].append(result.score_percent)
        titles[result.course_id] = result.course_title
    course_summaries = [
        CoursePerformanceSummary(
            course_id=course_id,
            course_title=titles[course_id],
            assessment_count=len(course_scores),
            average_score=round(mean(course_scores), 2),
            latest_score=next(
                result.score_percent
                for result in reversed(ordered)
                if result.course_id == course_id
            ),
        )
        for course_id, course_scores in sorted(grouped.items())
    ]
    return PerformanceAnalysisResponse(
        assessment_count=len(scores),
        average_score=round(mean(scores), 2),
        median_score=round(median(scores), 2),
        earliest_score=first_score,
        latest_score=latest_score,
        score_change=round(latest_score - first_score, 2) if len(scores) > 1 else None,
        courses=course_summaries,
        data_note="Descriptive summary of submitted results; MS6 does not verify or persist these scores.",
    )


def predict_failure_risk(
    request: FailureRiskRequest,
    settings: Settings,
) -> FailureRiskResponse:
    ordered = sorted(request.results, key=lambda result: result.completed_at)
    scores = [result.score_percent for result in ordered]
    signals: list[RiskSignal] = []

    average_score = mean(scores)
    if average_score < settings.risk_low_score_threshold:
        signals.append(
            RiskSignal(
                code="low_average",
                description=f"Average submitted score is {average_score:.1f}%.",
                points=40,
            )
        )

    split = max(1, len(scores) // 2)
    decline = mean(scores[:split]) - mean(scores[split:])
    if decline >= settings.risk_score_decline_threshold:
        signals.append(
            RiskSignal(
                code="declining_scores",
                description=f"Recent scores average {decline:.1f} points below earlier scores.",
                points=35,
            )
        )

    if request.completion_ratio < settings.risk_completion_threshold:
        signals.append(
            RiskSignal(
                code="low_completion",
                description=f"Reported completion is {request.completion_ratio:.0%}.",
                points=15,
            )
        )
    if request.missed_assessments:
        points = min(request.missed_assessments * 5, 10)
        signals.append(
            RiskSignal(
                code="missed_assessments",
                description=f"{request.missed_assessments} assessment(s) were reported missed.",
                points=points,
            )
        )

    score = min(sum(signal.points for signal in signals), 100)
    level = (
        "high"
        if score >= settings.risk_high_score
        else "moderate"
        if score >= settings.risk_moderate_score
        else "low"
    )
    return FailureRiskResponse(
        risk_score=score,
        risk_level=level,
        signals=signals,
        note=(
            "Configurable heuristic indicator from submitted data, not a calibrated probability. "
            "Use it to offer human support; do not use it for academic decisions."
        ),
    )
