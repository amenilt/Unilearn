import re

from app.models.learning_profile import LearningProfile
from app.schemas.recommendation import CourseCandidate, CourseRecommendation


def _normalize(value: str) -> str:
    return " ".join(re.findall(r"\w+", value.casefold(), flags=re.UNICODE))


def recommend_courses(
    profile: LearningProfile,
    courses: list[CourseCandidate],
    limit: int = 10,
) -> list[CourseRecommendation]:
    normalized_terms: list[tuple[str, str]] = []
    seen_terms: set[str] = set()
    for term in [*profile.interests, *profile.goals]:
        normalized = _normalize(term)
        if normalized and normalized not in seen_terms:
            normalized_terms.append((term, normalized))
            seen_terms.add(normalized)
    if not normalized_terms:
        return []

    recommendations: list[CourseRecommendation] = []
    for course in courses:
        course_text = _normalize(
            " ".join(filter(None, [course.title, course.description or "", course.categoryName or "", course.subjectName or ""]))
        )
        padded_text = f" {course_text} "
        matched = [
            term for term, normalized in normalized_terms if f" {normalized} " in padded_text
        ]
        if not matched:
            continue
        score = round(len(matched) / len(normalized_terms), 3)
        recommendations.append(
            CourseRecommendation(
                course_id=course.id,
                title=course.title,
                subject=course.subjectName,
                category=course.categoryName,
                score=score,
                matched_terms=matched,
                explanation=f"Matches your interests or goals: {', '.join(matched)}.",
            )
        )

    return sorted(recommendations, key=lambda item: (-item.score, item.course_id))[:limit]
