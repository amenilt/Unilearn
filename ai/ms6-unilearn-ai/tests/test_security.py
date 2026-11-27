from datetime import datetime, timedelta, timezone

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials
from fastapi.testclient import TestClient

from app.core.config import Settings
from app.core.config import get_settings
from app.core.security import UserContext, get_current_user, require_student
from app.main import app


def test_keycloak_access_token_is_verified_and_roles_are_extracted(monkeypatch) -> None:
    issuer = "https://identity.example.test/realms/unilearn"
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)

    class SigningKey:
        key = private_key.public_key()

    class JwkClient:
        def get_signing_key_from_jwt(self, token: str) -> SigningKey:
            return SigningKey()

    monkeypatch.setattr("app.core.security._jwk_client", lambda _: JwkClient())
    now = datetime.now(timezone.utc)
    token = jwt.encode(
        {
            "sub": "student-1",
            "iss": issuer,
            "iat": now,
            "exp": now + timedelta(minutes=5),
            "realm_access": {"roles": ["student", "offline_access"]},
        },
        private_key,
        algorithm="RS256",
        headers={"kid": "test-key"},
    )

    user = get_current_user(
        HTTPAuthorizationCredentials(scheme="Bearer", credentials=token),
        Settings(keycloak_issuer_uri=issuer),
    )

    assert user.subject == "student-1"
    assert user.roles == frozenset({"STUDENT", "OFFLINE_ACCESS"})


def test_missing_bearer_token_is_rejected() -> None:
    with pytest.raises(HTTPException) as error:
        get_current_user(None, Settings())

    assert error.value.status_code == 401


def test_personal_profile_route_requires_authentication() -> None:
    app.dependency_overrides.clear()
    with TestClient(app) as client:
        response = client.get("/api/v1/profile/me")

    assert response.status_code == 401


def test_teacher_cannot_use_student_profile_routes() -> None:
    with pytest.raises(HTTPException) as error:
        require_student(UserContext("teacher-1", frozenset({"TEACHER"}), "token"))

    assert error.value.status_code == 403


def _signed_token(private_key, issuer: str, subject: str, roles: list[str]) -> str:
    now = datetime.now(timezone.utc)
    return jwt.encode(
        {
            "sub": subject,
            "iss": issuer,
            "iat": now,
            "exp": now + timedelta(minutes=5),
            "realm_access": {"roles": roles},
        },
        private_key,
        algorithm="RS256",
        headers={"kid": "test-key"},
    )


def _use_test_signing_key(monkeypatch, private_key) -> None:
    class SigningKey:
        key = private_key.public_key()

    class JwkClient:
        def get_signing_key_from_jwt(self, token: str) -> SigningKey:
            return SigningKey()

    monkeypatch.setattr("app.core.security._jwk_client", lambda _: JwkClient())


def test_signed_keycloak_tokens_isolate_profiles_by_subject(client, monkeypatch) -> None:
    issuer = "https://identity.example.test/realms/unilearn"
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    _use_test_signing_key(monkeypatch, private_key)
    client.app.dependency_overrides.pop(get_current_user)
    client.app.dependency_overrides[get_settings] = lambda: Settings(
        keycloak_issuer_uri=issuer
    )
    student_one = _signed_token(private_key, issuer, "student-1", ["STUDENT"])
    student_two = _signed_token(private_key, issuer, "student-2", ["STUDENT"])

    created = client.put(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {student_one}"},
        json={"interests": ["statistics"], "goals": ["pass the course"]},
    )
    assert created.status_code == 200

    hidden_from_second_user = client.get(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {student_two}"},
    )
    assert hidden_from_second_user.status_code == 404

    second_profile = client.put(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {student_two}"},
        json={"interests": ["biology"], "goals": ["learn genetics"]},
    )
    assert second_profile.status_code == 200

    first_profile = client.get(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {student_one}"},
    )
    assert first_profile.status_code == 200
    assert first_profile.json()["interests"] == ["statistics"]


def test_signed_token_requires_valid_signature_issuer_and_student_role(
    client, monkeypatch
) -> None:
    issuer = "https://identity.example.test/realms/unilearn"
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    attacker_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    _use_test_signing_key(monkeypatch, private_key)
    client.app.dependency_overrides.pop(get_current_user)
    client.app.dependency_overrides[get_settings] = lambda: Settings(
        keycloak_issuer_uri=issuer
    )

    assert client.get("/api/v1/profile/me").status_code == 401
    wrong_signature = _signed_token(attacker_key, issuer, "student-1", ["STUDENT"])
    assert client.get(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {wrong_signature}"},
    ).status_code == 401
    wrong_issuer = _signed_token(
        private_key, "https://other.example.test/realms/unilearn", "student-1", ["STUDENT"]
    )
    assert client.get(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {wrong_issuer}"},
    ).status_code == 401
    teacher = _signed_token(private_key, issuer, "teacher-1", ["TEACHER"])
    assert client.get(
        "/api/v1/profile/me",
        headers={"Authorization": f"Bearer {teacher}"},
    ).status_code == 403
