import json
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from pydantic import ValidationError

from app.schemas.assistant import LessonSource


class CourseContentUnavailable(Exception):
    pass


class CourseContentClient:
    def __init__(self, courses_url: str, timeout: float = 5.0) -> None:
        self.courses_url = courses_url
        self.timeout = timeout

    def list_lessons(self, course_id: int, access_token: str) -> list[LessonSource]:
        url = f"{self.courses_url.rstrip('/')}/{course_id}/chapters"
        request = Request(
            url,
            headers={"Authorization": f"Bearer {access_token}", "Accept": "application/json"},
        )
        try:
            with urlopen(request, timeout=self.timeout) as response:
                chapters = json.load(response)
        except (HTTPError, URLError, TimeoutError, OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
            raise CourseContentUnavailable from error
        if not isinstance(chapters, list):
            raise CourseContentUnavailable

        lessons: list[LessonSource] = []
        try:
            for chapter in chapters:
                for lesson in chapter.get("lessons", []):
                    if lesson.get("content", "").strip():
                        lessons.append(
                            LessonSource(
                                chapter_id=chapter["id"],
                                chapter_title=chapter["title"],
                                lesson_id=lesson["id"],
                                lesson_title=lesson["title"],
                                content=lesson["content"],
                            )
                        )
        except (AttributeError, KeyError, TypeError, ValidationError) as error:
            raise CourseContentUnavailable from error
        return lessons
