import json
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from pydantic import ValidationError

from app.schemas.recommendation import CourseCandidate


class CourseCatalogUnavailable(Exception):
    pass


class CourseCatalogClient:
    def __init__(self, courses_url: str, timeout: float = 5.0) -> None:
        self.courses_url = courses_url
        self.timeout = timeout

    def list_published_courses(self, access_token: str) -> list[CourseCandidate]:
        url = f"{self.courses_url}?{urlencode({'status': 'PUBLISHED'})}"
        payload = self._get_json(url, access_token)
        if not isinstance(payload, list):
            raise CourseCatalogUnavailable
        try:
            return [CourseCandidate.model_validate(course) for course in payload]
        except (ValidationError, TypeError) as error:
            raise CourseCatalogUnavailable from error

    def list_enrolled_course_ids(self, access_token: str) -> set[int]:
        courses_path = self.courses_url.rstrip("/")
        api_prefix = courses_path.rsplit("/", 1)[0]
        payload = self._get_json(f"{api_prefix}/enrollments/me", access_token)
        if not isinstance(payload, list):
            raise CourseCatalogUnavailable
        try:
            return {int(enrollment["courseId"]) for enrollment in payload}
        except (KeyError, TypeError, ValueError) as error:
            raise CourseCatalogUnavailable from error

    def _get_json(self, url: str, access_token: str) -> object:
        request = Request(
            url,
            headers={"Authorization": f"Bearer {access_token}", "Accept": "application/json"},
        )
        try:
            with urlopen(request, timeout=self.timeout) as response:
                payload = json.load(response)
        except (
            HTTPError,
            URLError,
            TimeoutError,
            OSError,
            UnicodeDecodeError,
            json.JSONDecodeError,
        ) as error:
            raise CourseCatalogUnavailable from error
        return payload
