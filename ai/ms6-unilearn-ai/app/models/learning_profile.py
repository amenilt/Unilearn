from datetime import datetime

from sqlalchemy import DateTime, JSON, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class LearningProfile(Base):
    __tablename__ = "learning_profiles"

    subject_id: Mapped[str] = mapped_column(String(255), primary_key=True)
    interests: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    goals: Mapped[list[str]] = mapped_column(JSON, nullable=False, default=list)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )
