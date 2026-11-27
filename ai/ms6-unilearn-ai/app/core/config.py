from functools import lru_cache
from pathlib import Path

from pydantic import Field, SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import URL, make_url

REPOSITORY_ROOT = Path(__file__).resolve().parents[4]


class Settings(BaseSettings):
    app_name: str = "UniLearn AI"
    host: str = "127.0.0.1"
    port: int = Field(default=8086, ge=1, le=65535)
    db_host: str = "localhost"
    db_port: int = Field(default=5433, ge=1, le=65535)
    db_name: str = "unilearn_ms6"
    db_user: str = "unilearn_ms6"
    db_password: SecretStr | None = None
    demo_mode: bool = False
    demo_database_url: SecretStr | None = None
    keycloak_issuer_uri: str = "http://localhost:8180/realms/unilearn"
    keycloak_jwk_set_uri: str | None = None
    ms1_courses_url: str = "http://localhost:8081/api/courses"
    llm_base_url: str | None = None
    llm_api_key: SecretStr | None = None
    llm_model: str | None = None
    llm_timeout_seconds: float = Field(default=20, gt=0, le=120)
    risk_low_score_threshold: float = Field(default=50, ge=0, le=100)
    risk_score_decline_threshold: float = Field(default=15, ge=0, le=100)
    risk_completion_threshold: float = Field(default=0.6, ge=0, le=1)
    risk_moderate_score: int = Field(default=30, ge=1, le=100)
    risk_high_score: int = Field(default=60, ge=1, le=100)

    model_config = SettingsConfigDict(
        env_prefix="MS6_",
        case_sensitive=False,
        env_file=REPOSITORY_ROOT / ".env",
        extra="ignore",
    )

    @model_validator(mode="after")
    def validate_risk_levels(self) -> "Settings":
        if self.risk_high_score <= self.risk_moderate_score:
            raise ValueError("MS6_RISK_HIGH_SCORE must exceed MS6_RISK_MODERATE_SCORE")
        if self.demo_mode and self.demo_database_url is None:
            raise ValueError("MS6_DEMO_DATABASE_URL is required when MS6_DEMO_MODE=true")
        if self.demo_mode and not make_url(
            self.demo_database_url.get_secret_value()
        ).drivername.startswith("sqlite"):
            raise ValueError("MS6_DEMO_DATABASE_URL must use SQLite")
        return self

    @property
    def jwks_url(self) -> str:
        if self.keycloak_jwk_set_uri:
            return self.keycloak_jwk_set_uri
        return f"{self.keycloak_issuer_uri.rstrip('/')}/protocol/openid-connect/certs"

    @property
    def database_url(self) -> URL:
        if self.demo_mode and self.demo_database_url is not None:
            return make_url(self.demo_database_url.get_secret_value())
        if self.db_password is None or not self.db_password.get_secret_value():
            raise ValueError("MS6_DB_PASSWORD must be set to connect to PostgreSQL")

        return URL.create(
            drivername="postgresql+psycopg",
            username=self.db_user,
            password=self.db_password.get_secret_value(),
            host=self.db_host,
            port=self.db_port,
            database=self.db_name,
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()
