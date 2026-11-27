from fastapi.testclient import TestClient

import app.api.health as health_module
from app.main import app


def test_app_starts_and_health_endpoint_responds() -> None:
    with TestClient(app) as client:
        response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_readiness_reports_database_failure_without_details(monkeypatch) -> None:
    monkeypatch.setattr(
        health_module,
        "get_engine",
        lambda: (_ for _ in ()).throw(ValueError("secret connection detail")),
    )

    with TestClient(app) as client:
        response = client.get("/health/ready")

    assert response.status_code == 503
    assert response.json() == {
        "status": "not_ready",
        "checks": {"database": "unavailable"},
    }


def test_readiness_reports_database_success(monkeypatch) -> None:
    class Connection:
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return None

        def execute(self, _statement):
            return 1

    class Engine:
        def connect(self):
            return Connection()

    monkeypatch.setattr(health_module, "get_engine", lambda: Engine())
    with TestClient(app) as client:
        response = client.get("/health/ready")

    assert response.status_code == 200
    assert response.json() == {"status": "ready", "checks": {"database": "ok"}}
