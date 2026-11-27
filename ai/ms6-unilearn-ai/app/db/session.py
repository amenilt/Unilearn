from collections.abc import Generator
from functools import lru_cache

from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.config import Settings, get_settings


def create_database_engine(settings: Settings) -> Engine:
    url = settings.database_url
    kwargs = {"pool_pre_ping": True}
    if url.drivername.startswith("sqlite"):
        kwargs["connect_args"] = {"check_same_thread": False}
        if url.database in {None, "", ":memory:"}:
            kwargs["poolclass"] = StaticPool
    return create_engine(url, **kwargs)


@lru_cache
def get_engine() -> Engine:
    return create_database_engine(get_settings())


@lru_cache
def get_session_factory() -> sessionmaker[Session]:
    return sessionmaker(
        bind=get_engine(),
        autoflush=False,
        expire_on_commit=False,
    )


def get_db() -> Generator[Session, None, None]:
    with get_session_factory()() as session:
        yield session


def dispose_database_engine() -> None:
    get_session_factory.cache_clear()
    if get_engine.cache_info().currsize:
        get_engine().dispose()
    get_engine.cache_clear()
