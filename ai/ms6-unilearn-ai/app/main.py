from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from app.api.health import router as health_router
from app.api.assistant import router as assistant_router
from app.api.analysis import router as analysis_router
from app.api.learning_paths import router as learning_paths_router
from app.api.profiles import router as profile_router
from app.api.recommendations import router as recommendations_router
from app.core.config import get_settings
from app.db.base import Base
from app.db.session import dispose_database_engine, get_engine, get_session_factory
from app.models.learning_profile import LearningProfile
from app.services.demo_data import DEMO_GOALS, DEMO_INTERESTS, DEMO_STUDENT_ID

settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    try:
        if settings.demo_mode:
            Base.metadata.create_all(bind=get_engine())
            with get_session_factory()() as session:
                if session.get(LearningProfile, DEMO_STUDENT_ID) is None:
                    session.add(
                        LearningProfile(
                            subject_id=DEMO_STUDENT_ID,
                            interests=DEMO_INTERESTS,
                            goals=DEMO_GOALS,
                        )
                    )
                    session.commit()
        yield
    finally:
        dispose_database_engine()


app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description=(
        "UniLearn MS6 provides grounded course assistance, explainable recommendations, "
        "student performance summaries, and advisory risk indicators. In demo mode, all "
        "student and course records are synthetic."
    ),
    openapi_tags=[
        {"name": "health", "description": "Liveness and database readiness."},
        {"name": "learning profile", "description": "Manage the authenticated student's interests and goals."},
        {"name": "recommendations", "description": "Rank MS1 published courses against a learning profile."},
        {"name": "pedagogical assistant", "description": "Answer course questions using cited lesson material."},
        {"name": "performance analysis", "description": "Summarize supplied results and identify advisory risk signals."},
        {"name": "learning paths", "description": "Preview courses ordered by profile relevance."},
    ],
    lifespan=lifespan,
)
app.include_router(health_router)
app.include_router(assistant_router)
app.include_router(profile_router)
app.include_router(recommendations_router)
app.include_router(analysis_router)
app.include_router(learning_paths_router)


if __name__ == "__main__":
    uvicorn.run(app, host=settings.host, port=settings.port)
