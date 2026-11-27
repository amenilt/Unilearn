from dataclasses import dataclass, field
from functools import lru_cache

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import PyJWKClient
from jwt.exceptions import PyJWTError

from app.core.config import Settings, get_settings

bearer_scheme = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class UserContext:
    subject: str
    roles: frozenset[str]
    access_token: str = field(repr=False)


@lru_cache(maxsize=4)
def _jwk_client(jwks_url: str) -> PyJWKClient:
    return PyJWKClient(jwks_url, cache_keys=True)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    settings: Settings = Depends(get_settings),
) -> UserContext:
    if settings.demo_mode and credentials is None:
        return UserContext(
            subject="demo-student-001",
            roles=frozenset({"STUDENT"}),
            access_token="demo-only-token",
        )
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Bearer token required",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        signing_key = _jwk_client(settings.jwks_url).get_signing_key_from_jwt(
            credentials.credentials
        )
        claims = jwt.decode(
            credentials.credentials,
            signing_key.key,
            algorithms=["RS256"],
            issuer=settings.keycloak_issuer_uri,
            options={"require": ["exp", "sub"], "verify_aud": False},
        )
    except PyJWTError as error:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        ) from error

    subject = claims.get("sub")
    if not isinstance(subject, str) or not subject.strip():
        raise HTTPException(status_code=401, detail="Token has no subject")

    realm_access = claims.get("realm_access")
    role_values = realm_access.get("roles", []) if isinstance(realm_access, dict) else []
    roles = frozenset(role.upper() for role in role_values if isinstance(role, str))
    return UserContext(subject=subject, roles=roles, access_token=credentials.credentials)


def require_student(user: UserContext = Depends(get_current_user)) -> UserContext:
    if not user.roles.intersection({"STUDENT", "ADMIN"}):
        raise HTTPException(status_code=403, detail="Student role required")
    return user
