from datetime import datetime

from typing import Literal

from pydantic import BaseModel, Field


class PerformanceEntry(BaseModel):
    course_id: int = Field(gt=0)
    course_title: str = Field(min_length=1, max_length=200)
    assessment_id: str = Field(min_length=1, max_length=100)
    score_percent: float = Field(ge=0, le=100)
    completed_at: datetime


class PerformanceAnalysisRequest(BaseModel):
    results: list[PerformanceEntry] = Field(min_length=1, max_length=200)

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "results": [
                        {
                            "course_id": 101,
                            "course_title": "Introduction to Statistics",
                            "assessment_id": "quiz-1",
                            "score_percent": 68,
                            "completed_at": "2026-10-05T10:00:00Z",
                        },
                        {
                            "course_id": 101,
                            "course_title": "Introduction to Statistics",
                            "assessment_id": "quiz-2",
                            "score_percent": 82,
                            "completed_at": "2026-11-05T10:00:00Z",
                        },
                        {
                            "course_id": 103,
                            "course_title": "Data Visualization with Python",
                            "assessment_id": "project-1",
                            "score_percent": 91,
                            "completed_at": "2026-11-10T14:00:00Z",
                        },
                    ]
                }
            ]
        }
    }


class PerformanceAnalysisResponse(BaseModel):
    assessment_count: int
    average_score: float
    median_score: float
    earliest_score: float
    latest_score: float
    score_change: float | None
    courses: list["CoursePerformanceSummary"]
    data_note: str

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "assessment_count": 3,
                    "average_score": 80.33,
                    "median_score": 82.0,
                    "earliest_score": 68.0,
                    "latest_score": 91.0,
                    "score_change": 23.0,
                    "courses": [
                        {"course_id": 101, "course_title": "Introduction to Statistics", "assessment_count": 2, "average_score": 75.0, "latest_score": 82.0},
                        {"course_id": 103, "course_title": "Data Visualization with Python", "assessment_count": 1, "average_score": 91.0, "latest_score": 91.0},
                    ],
                    "data_note": "Descriptive summary of submitted results; MS6 does not verify or persist these scores.",
                }
            ]
        }
    }


class CoursePerformanceSummary(BaseModel):
    course_id: int
    course_title: str
    assessment_count: int
    average_score: float
    latest_score: float


class FailureRiskRequest(BaseModel):
    results: list[PerformanceEntry] = Field(min_length=2, max_length=200)
    completion_ratio: float = Field(ge=0, le=1)
    missed_assessments: int = Field(default=0, ge=0, le=100)

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "results": [
                        {"course_id": 101, "course_title": "Introduction to Statistics", "assessment_id": "quiz-1", "score_percent": 61, "completed_at": "2026-10-05T10:00:00Z"},
                        {"course_id": 101, "course_title": "Introduction to Statistics", "assessment_id": "quiz-2", "score_percent": 43, "completed_at": "2026-10-20T10:00:00Z"},
                        {"course_id": 101, "course_title": "Introduction to Statistics", "assessment_id": "quiz-3", "score_percent": 35, "completed_at": "2026-11-05T10:00:00Z"},
                        {"course_id": 101, "course_title": "Introduction to Statistics", "assessment_id": "quiz-4", "score_percent": 28, "completed_at": "2026-11-20T10:00:00Z"}
                    ],
                    "completion_ratio": 0.45,
                    "missed_assessments": 1
                }
            ]
        }
    }


class RiskSignal(BaseModel):
    code: str
    description: str
    points: int = Field(ge=0, le=100)


class FailureRiskResponse(BaseModel):
    risk_score: int = Field(ge=0, le=100)
    risk_level: Literal["low", "moderate", "high"]
    signals: list[RiskSignal]
    note: str

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "risk_score": 95,
                    "risk_level": "high",
                    "signals": [
                        {"code": "low_average", "description": "Average submitted score is 41.8%.", "points": 40},
                        {"code": "declining_scores", "description": "Recent scores average 20.5 points below earlier scores.", "points": 35},
                        {"code": "low_completion", "description": "Reported completion is 45%.", "points": 15},
                        {"code": "missed_assessments", "description": "1 assessment(s) were reported missed.", "points": 5},
                    ],
                    "note": "Configurable heuristic indicator from submitted data, not a calibrated probability. Use it to offer human support; do not use it for academic decisions.",
                }
            ]
        }
    }
