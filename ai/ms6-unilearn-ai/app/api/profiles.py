from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.orm import Session

from app.core.security import UserContext, require_student
from app.db.session import get_db
from app.models.learning_profile import LearningProfile
from app.schemas.profile import LearningProfileInput, LearningProfileResponse

router = APIRouter(prefix="/api/v1/profile", tags=["learning profile"])


@router.get(
    "/me",
    response_model=LearningProfileResponse,
    summary="Get the current student's learning profile",
    description="Returns interests and goals for the authenticated student, or the synthetic demo student in demo mode.",
    responses={404: {"description": "Profile does not exist"}},
)
def get_my_profile(
    user: UserContext = Depends(require_student),
    session: Session = Depends(get_db),
) -> LearningProfile:
    profile = session.get(LearningProfile, user.subject)
    if profile is None:
        raise HTTPException(status_code=404, detail="Learning profile not found")
    return profile


@router.put(
    "/me",
    response_model=LearningProfileResponse,
    summary="Create or update the current student's learning profile",
    description="Stores up to 30 interests and 20 learning goals for the current student.",
)
def update_my_profile(
    body: LearningProfileInput,
    user: UserContext = Depends(require_student),
    session: Session = Depends(get_db),
) -> LearningProfile:
    profile = session.get(LearningProfile, user.subject)
    if profile is None:
        profile = LearningProfile(subject_id=user.subject, **body.model_dump())
        session.add(profile)
    else:
        profile.interests = body.interests
        profile.goals = body.goals
    session.commit()
    session.refresh(profile)
    return profile


@router.delete(
    "/me",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete the current student's learning profile",
    description="Idempotently removes the current student's saved interests and goals.",
)
def delete_my_profile(
    user: UserContext = Depends(require_student),
    session: Session = Depends(get_db),
) -> Response:
    profile = session.get(LearningProfile, user.subject)
    if profile is not None:
        session.delete(profile)
        session.commit()
    return Response(status_code=status.HTTP_204_NO_CONTENT)
