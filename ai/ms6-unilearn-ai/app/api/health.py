from fastapi import APIRouter
from fastapi.responses import JSONResponse, Response
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.db.session import get_engine
from app.schemas.health import HealthResponse, ReadinessResponse

router = APIRouter(tags=["health"])


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Check that MS6 is running",
    description="Liveness check. Does not require PostgreSQL or Keycloak.",
)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@router.get(
    "/health/ready",
    response_model=ReadinessResponse,
    responses={503: {"model": ReadinessResponse, "description": "Database unavailable"}},
    summary="Check MS6 database readiness",
    description="Runs a lightweight PostgreSQL/SQLite SELECT 1 and returns 503 if unavailable.",
)
def readiness() -> ReadinessResponse | Response:
    """Report whether the service can reach the database it depends on."""
    try:
        with get_engine().connect() as connection:
            connection.execute(text("SELECT 1"))
    except (SQLAlchemyError, ValueError):
        return JSONResponse(
            status_code=503,
            content={"status": "not_ready", "checks": {"database": "unavailable"}},
        )

    return ReadinessResponse(status="ready", checks={"database": "ok"})
