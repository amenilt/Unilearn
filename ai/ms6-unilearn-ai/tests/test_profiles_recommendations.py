import json
from urllib.parse import parse_qs, urlsplit

from app.api.recommendations import get_course_catalog
from app.api.assistant import get_course_content_client
from app.api.assistant import get_chat_client
from app.core.config import Settings, get_settings
from app.core.security import UserContext, get_current_user
from app.schemas.assistant import LessonSource
from app.schemas.recommendation import CourseCandidate
from app.services.course_content import CourseContentClient
from app.services.course_catalog import CourseCatalogClient


class FakeCatalog:
    def list_published_courses(self, access_token: str) -> list[CourseCandidate]:
        assert access_token == "test-token"
        return [
            CourseCandidate(
                id=1,
                title="Applied Statistics",
                description="Probability and data analysis",
                subjectName="Mathematics",
            ),
            CourseCandidate(id=2, title="Intro to Literature", description="Poetry and prose"),
        ]

    def list_enrolled_course_ids(self, access_token: str) -> set[int]:
        assert access_token == "test-token"
        return set()


class FakeCourseContent:
    def list_lessons(self, course_id: int, access_token: str) -> list[LessonSource]:
        assert course_id == 9
        assert access_token == "test-token"
        return [
            LessonSource(
                chapter_id=2,
                chapter_title="Probability",
                lesson_id=7,
                lesson_title="Events",
                content="Probability measures the likelihood that an event will occur. Events are subsets of a sample space.",
            )
        ]


class FakeChatClient:
    def answer(self, question, history, citations):
        return "Probability measures likelihood. [lesson:7]"


def test_profile_can_be_created_and_read_for_authenticated_student(client) -> None:
    response = client.put(
        "/api/v1/profile/me",
        json={"interests": [" statistics ", "Statistics", "probability"], "goals": ["Data analysis"]},
    )

    assert response.status_code == 200
    assert response.json()["interests"] == ["statistics", "probability"]
    assert client.get("/api/v1/profile/me").json()["goals"] == ["Data analysis"]


def test_course_recommendations_use_profile_and_published_catalog(client) -> None:
    client.put(
        "/api/v1/profile/me",
        json={"interests": ["statistics", "poetry"], "goals": []},
    )
    client.app.dependency_overrides[get_course_catalog] = lambda: FakeCatalog()

    response = client.get("/api/v1/recommendations/courses")

    assert response.status_code == 200
    items = response.json()["items"]
    assert [item["course_id"] for item in items] == [1, 2]
    assert items[0]["matched_terms"] == ["statistics"]
    assert items[0]["score"] == 0.5


def test_recommendations_require_a_saved_profile(client) -> None:
    client.app.dependency_overrides[get_course_catalog] = lambda: FakeCatalog()

    response = client.get("/api/v1/recommendations/courses")

    assert response.status_code == 404


def test_learning_path_preview_orders_matching_courses(client) -> None:
    client.put("/api/v1/profile/me", json={"interests": ["statistics"], "goals": []})
    client.app.dependency_overrides[get_course_catalog] = lambda: FakeCatalog()

    response = client.get("/api/v1/learning-paths/preview")

    assert response.status_code == 200
    body = response.json()
    assert body["steps"][0]["course_id"] == 1
    assert "prerequisite order is not available" in body["ordering"]


def test_recommendations_exclude_courses_the_student_is_enrolled_in(client) -> None:
    class EnrolledCatalog(FakeCatalog):
        def list_enrolled_course_ids(self, access_token: str) -> set[int]:
            return {1}

    client.put("/api/v1/profile/me", json={"interests": ["statistics"], "goals": []})
    client.app.dependency_overrides[get_course_catalog] = lambda: EnrolledCatalog()

    response = client.get("/api/v1/recommendations/courses")

    assert response.status_code == 200
    assert response.json()["items"] == []


def test_profile_is_not_visible_to_another_authenticated_user(client) -> None:
    client.put("/api/v1/profile/me", json={"interests": ["biology"], "goals": []})
    client.app.dependency_overrides[get_current_user] = lambda: UserContext(
        subject="student-2", roles=frozenset({"STUDENT"}), access_token="other-token"
    )

    response = client.get("/api/v1/profile/me")

    assert response.status_code == 404


def test_profile_can_be_deleted_by_its_owner(client) -> None:
    client.put("/api/v1/profile/me", json={"interests": ["biology"], "goals": []})

    response = client.delete("/api/v1/profile/me")

    assert response.status_code == 204
    assert client.get("/api/v1/profile/me").status_code == 404


def test_course_catalog_url_uses_the_configured_ms1_endpoint() -> None:
    client = get_course_catalog(Settings(ms1_courses_url="http://ms1.test/api/courses"))

    assert isinstance(client, CourseCatalogClient)
    assert client.courses_url == "http://ms1.test/api/courses"


def test_recommendations_request_published_ms1_catalog_and_forward_bearer(
    client, monkeypatch
) -> None:
    client.put(
        "/api/v1/profile/me",
        json={"interests": ["statistics"], "goals": []},
    )
    settings = Settings(ms1_courses_url="http://ms1.test/api/courses")
    client.app.dependency_overrides[get_settings] = lambda: settings
    requests = []
    published = {
        "id": 7,
        "title": "Published Statistics",
        "description": "Probability and data analysis",
        "status": "PUBLISHED",
        "subjectName": "Mathematics",
    }
    draft = {
        "id": 8,
        "title": "Draft Statistics",
        "description": "Not visible to students",
        "status": "DRAFT",
        "subjectName": "Mathematics",
    }

    class FakeResponse:
        def __init__(self, payload):
            self.payload = json.dumps(payload).encode()

        def __enter__(self):
            return self

        def __exit__(self, *_args):
            return False

        def read(self):
            return self.payload

    def fake_urlopen(request, timeout):
        requests.append(request)
        parsed = urlsplit(request.full_url)
        if parsed.path == "/api/courses":
            courses = [published] if parse_qs(parsed.query) == {"status": ["PUBLISHED"]} else [published, draft]
            return FakeResponse(courses)
        assert parsed.path == "/api/enrollments/me"
        return FakeResponse([])

    monkeypatch.setattr("app.services.course_catalog.urlopen", fake_urlopen)
    response = client.get("/api/v1/recommendations/courses")

    assert response.status_code == 200
    assert [item["course_id"] for item in response.json()["items"]] == [7]
    assert len(requests) == 2
    assert all(request.headers["Authorization"] == "Bearer test-token" for request in requests)
    assert parse_qs(urlsplit(requests[0].full_url).query) == {"status": ["PUBLISHED"]}
    assert urlsplit(requests[1].full_url).path == "/api/enrollments/me"


def test_assistant_returns_relevant_lesson_excerpt_and_citation(client) -> None:
    client.app.dependency_overrides[get_course_content_client] = lambda: FakeCourseContent()

    response = client.post(
        "/api/v1/assistant/ask",
        json={"course_id": 9, "question": "What is probability?"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["abstained"] is False
    assert body["mode"] == "extractive"
    assert body["citations"][0]["lesson_id"] == 7
    assert body["citations"][0]["excerpt"] == "Probability measures the likelihood that an event will occur."


def test_assistant_abstains_when_course_has_no_matching_passage(client) -> None:
    client.app.dependency_overrides[get_course_content_client] = lambda: FakeCourseContent()

    response = client.post(
        "/api/v1/assistant/ask",
        json={"course_id": 9, "question": "Explain quantum mechanics"},
    )

    assert response.status_code == 200
    assert response.json()["abstained"] is True
    assert response.json()["citations"] == []


def test_chat_endpoint_uses_grounded_llm_and_keeps_citations(client) -> None:
    client.app.dependency_overrides[get_course_content_client] = lambda: FakeCourseContent()
    client.app.dependency_overrides[get_chat_client] = lambda: FakeChatClient()

    response = client.post(
        "/api/v1/assistant/chat",
        json={"course_id": 9, "question": "What is probability?", "history": []},
    )

    assert response.status_code == 200
    assert response.json()["mode"] == "grounded_llm"
    assert response.json()["citations"][0]["lesson_id"] == 7


def test_result_analysis_is_descriptive_and_returns_score_change(client) -> None:
    response = client.post(
        "/api/v1/analysis/results",
        json={
            "results": [
                {
                    "course_id": 4,
                    "course_title": "Statistics",
                    "assessment_id": "quiz-1",
                    "score_percent": 60,
                    "completed_at": "2026-10-01T10:00:00Z",
                },
                {
                    "course_id": 4,
                    "course_title": "Statistics",
                    "assessment_id": "quiz-2",
                    "score_percent": 100,
                    "completed_at": "2026-11-01T10:00:00Z",
                },
            ]
        },
    )

    assert response.status_code == 200
    assert response.json()["average_score"] == 80
    assert response.json()["median_score"] == 80
    assert response.json()["score_change"] == 40
    assert response.json()["courses"][0]["average_score"] == 80


def test_failure_risk_reports_explainable_high_risk_signals(client) -> None:
    response = client.post(
        "/api/v1/analysis/failure-risk",
        json={
            "results": [
                {"course_id": 4, "course_title": "Statistics", "assessment_id": "q1", "score_percent": 60, "completed_at": "2026-10-01T10:00:00Z"},
                {"course_id": 4, "course_title": "Statistics", "assessment_id": "q2", "score_percent": 20, "completed_at": "2026-11-01T10:00:00Z"},
            ],
            "completion_ratio": 0.4,
            "missed_assessments": 2,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["risk_score"] == 100
    assert body["risk_level"] == "high"
    assert {signal["code"] for signal in body["signals"]} == {
        "low_average", "declining_scores", "low_completion", "missed_assessments"
    }
