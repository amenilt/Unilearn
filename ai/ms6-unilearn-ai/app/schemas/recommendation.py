from pydantic import BaseModel, Field


class CourseCandidate(BaseModel):
    id: int
    title: str
    description: str | None = None
    categoryName: str | None = None
    subjectName: str | None = None


class CourseRecommendation(BaseModel):
    course_id: int
    title: str
    subject: str | None
    category: str | None
    score: float = Field(ge=0, le=1)
    matched_terms: list[str]
    explanation: str


class RecommendationResponse(BaseModel):
    items: list[CourseRecommendation]
    method: str = "transparent_interest_match"

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "items": [
                        {
                            "course_id": 103,
                            "title": "Data Visualization with Python",
                            "subject": "Computer Science",
                            "category": "Data Science",
                            "score": 0.4,
                            "matched_terms": ["Python", "data visualization"],
                            "explanation": "Matches your interests or goals: Python, data visualization.",
                        }
                    ],
                    "method": "transparent_interest_match",
                }
            ]
        }
    }
