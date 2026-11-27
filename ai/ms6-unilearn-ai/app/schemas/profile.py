from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


class LearningProfileInput(BaseModel):
    model_config = ConfigDict(
        str_strip_whitespace=True,
        json_schema_extra={
            "examples": [
                {
                    "interests": ["statistics", "Python", "data analysis"],
                    "goals": ["data visualization", "interpret study results"],
                }
            ]
        },
    )

    interests: list[str] = Field(default_factory=list, max_length=30)
    goals: list[str] = Field(default_factory=list, max_length=20)

    @field_validator("interests", "goals")
    @classmethod
    def clean_terms(cls, values: list[str]) -> list[str]:
        cleaned: list[str] = []
        seen: set[str] = set()
        for value in values:
            term = " ".join(value.split())
            if not term or len(term) > 80:
                raise ValueError("Each interest or goal must contain 1 to 80 characters")
            key = term.casefold()
            if key not in seen:
                cleaned.append(term)
                seen.add(key)
        return cleaned


class LearningProfileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    interests: list[str]
    goals: list[str]
    created_at: datetime
    updated_at: datetime
