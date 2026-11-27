import pytest

from app.core.config import Settings


def test_settings_are_loaded_from_environment(monkeypatch) -> None:
    monkeypatch.setenv("MS6_APP_NAME", "Test UniLearn AI")
    monkeypatch.setenv("MS6_HOST", "0.0.0.0")
    monkeypatch.setenv("MS6_PORT", "8090")

    settings = Settings()

    assert settings.app_name == "Test UniLearn AI"
    assert settings.host == "0.0.0.0"
    assert settings.port == 8090


def test_database_url_uses_psycopg_and_environment(monkeypatch) -> None:
    monkeypatch.setenv("MS6_DB_HOST", "db.example.test")
    monkeypatch.setenv("MS6_DB_PORT", "5433")
    monkeypatch.setenv("MS6_DB_NAME", "ms6_test")
    monkeypatch.setenv("MS6_DB_USER", "ms6_user")
    monkeypatch.setenv("MS6_DB_PASSWORD", "test:password")

    url = Settings().database_url

    assert url.drivername == "postgresql+psycopg"
    assert url.host == "db.example.test"
    assert url.port == 5433
    assert url.database == "ms6_test"
    assert url.username == "ms6_user"
    assert url.password == "test:password"
    assert "test:password" not in url.render_as_string(hide_password=True)


def test_database_url_requires_a_password() -> None:
    settings = Settings(db_password=None)

    try:
        settings.database_url
    except ValueError as error:
        assert str(error) == "MS6_DB_PASSWORD must be set to connect to PostgreSQL"
    else:
        raise AssertionError("A database URL must not be built without a password")


def test_risk_thresholds_require_high_above_moderate() -> None:
    with pytest.raises(ValueError, match="MS6_RISK_HIGH_SCORE"):
        Settings(risk_moderate_score=70, risk_high_score=60)
