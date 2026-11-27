from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.config import Settings, get_settings
from app.core.security import UserContext, require_student
from app.db.session import get_db
from app.models.learning_profile import LearningProfile
from app.schemas.recommendation import RecommendationResponse
from app.services.course_catalog import CourseCatalogClient, CourseCatalogUnavailable
from app.services.demo_data import DemoCourseCatalogClient
from app.services.recommendations import recommend_courses

router = APIRouter(prefix="/api/v1/recommendations", tags=["recommendations"])


def get_course_catalog(settings: Settings = Depends(get_settings)) -> CourseCatalogClient:
    if settings.demo_mode:
        return DemoCourseCatalogClient()
    return CourseCatalogClient(settings.ms1_courses_url)


@router.get(
    "/courses",
    response_model=RecommendationResponse,
    summary="Recommend published courses",
    description="Ranks published MS1 courses against the student's saved interests and goals, excluding enrolled courses.",
    responses={404: {"description": "Create a learning profile first"}, 502: {"description": "MS1 catalog unavailable"}},
)
def get_course_recommendations(
    limit: int = Query(default=10, ge=1, le=50),
    user: UserContext = Depends(require_student),
    session: Session = Depends(get_db),
    catalog: CourseCatalogClient = Depends(get_course_catalog),
) -> RecommendationResponse:
    profile = session.get(LearningProfile, user.subject)
    if profile is None:
        raise HTTPException(status_code=404, detail="Create a learning profile first")
    try:
        courses = catalog.list_published_courses(user.access_token)
        enrolled_ids = catalog.list_enrolled_course_ids(user.access_token)
    except CourseCatalogUnavailable as error:
        raise HTTPException(status_code=502, detail="Course catalog is unavailable") from error
    courses = [course for course in courses if course.id not in enrolled_ids]
    return RecommendationResponse(items=recommend_courses(profile, courses, limit))
