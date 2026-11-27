from pydantic import BaseModel


class LearningPathStep(BaseModel):
    step: int
    course_id: int
    title: str
    score: float
    rationale: str


class LearningPathPreview(BaseModel):
    ordering: str
    steps: list[LearningPathStep]
