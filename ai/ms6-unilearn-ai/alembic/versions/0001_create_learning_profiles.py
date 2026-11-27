"""Create learner profiles.

Revision ID: 0001_learning_profiles
Revises:
Create Date: 2026-11-27
"""
from typing import Sequence

from alembic import op
import sqlalchemy as sa

revision: str = "0001_learning_profiles"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "learning_profiles",
        sa.Column("subject_id", sa.String(length=255), primary_key=True),
        sa.Column("interests", sa.JSON(), nullable=False),
        sa.Column("goals", sa.JSON(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
    )


def downgrade() -> None:
    op.drop_table("learning_profiles")
