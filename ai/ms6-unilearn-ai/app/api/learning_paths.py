from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.api.recommendations import get_course_catalog
from app.core.security import UserContext, require_student
from app.db.session import get_db
from app.models.learning_profile import LearningProfile
from app.schemas.learning_path import LearningPathPreview, LearningPathStep
from app.services.course_catalog import CourseCatalogClient, CourseCatalogUnavailable
from app.services.recommendations import recommend_courses

router = APIRouter(prefix="/api/v1/learning-paths", tags=["learning paths"])


@router.get(
    "/preview",
    response_model=LearningPathPreview,
    summary="Preview a personalized course path",
    description="Orders recommended published courses by interest relevance. MS1 does not provide prerequisite data, so this is not prerequisite sequencing.",
    responses={404: {"description": "Create a learning profile first"}, 502: {"description": "MS1 catalog unavailable"}},
)
def preview_learning_path(
    limit: int = Query(default=10, ge=1, le=50),
    user: UserContext = Depends(require_student),
    session: Session = Depends(get_db),
    catalog: CourseCatalogClient = Depends(get_course_catalog),
) -> LearningPathPreview:
    profile = session.get(LearningProfile, user.subject)
    if profile is None:
        raise HTTPException(status_code=404, detail="Create a learning profile first")
    try:
        courses = catalog.list_published_courses(user.access_token)
        enrolled_ids = catalog.list_enrolled_course_ids(user.access_token)
    except CourseCatalogUnavailable as error:
        raise HTTPException(status_code=502, detail="Course catalog is unavailable") from error

    recommendations = recommend_courses(
        profile,
        [course for course in courses if course.id not in enrolled_ids],
        limit,
    )
    return LearningPathPreview(
        ordering="interest_relevance; prerequisite order is not available from MS1",
        steps=[
            LearningPathStep(
                step=index,
                course_id=item.course_id,
                title=item.title,
                score=item.score,
                rationale=item.explanation,
            )
            for index, item in enumerate(recommendations, start=1)
        ],
    )
