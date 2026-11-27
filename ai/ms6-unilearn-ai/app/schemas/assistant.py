from typing import Literal

from pydantic import BaseModel, Field


class ChatTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=2000)


class AssistantQuestion(BaseModel):
    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "course_id": 101,
                    "question": "What does the arithmetic mean represent?",
                    "history": [],
                }
            ]
        }
    }

    course_id: int = Field(gt=0)
    question: str = Field(min_length=3, max_length=500)
    history: list[ChatTurn] = Field(default_factory=list, max_length=20)


class LessonSource(BaseModel):
    chapter_id: int
    chapter_title: str
    lesson_id: int
    lesson_title: str
    content: str


class SourceCitation(BaseModel):
    chapter_id: int
    chapter_title: str
    lesson_id: int
    lesson_title: str
    excerpt: str
    relevance: float = Field(ge=0, le=1)


class AssistantAnswer(BaseModel):
    answer: str
    abstained: bool
    citations: list[SourceCitation]
    mode: Literal["grounded_llm", "extractive"] = "extractive"

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "answer": "The arithmetic mean is the sum of observations divided by their count. [lesson:11011]",
                    "abstained": False,
                    "mode": "extractive",
                    "citations": [
                        {
                            "chapter_id": 1101,
                            "chapter_title": "Describing Data",
                            "lesson_id": 11011,
                            "lesson_title": "The Arithmetic Mean",
                            "excerpt": "The arithmetic mean is the sum of all observations divided by the number of observations.",
                            "relevance": 0.5,
                        }
                    ],
                }
            ]
        }
    }
