import json
from io import BytesIO

from pydantic import SecretStr

import app.services.llm as llm_module
from app.schemas.assistant import SourceCitation
from app.services.llm import OpenAICompatibleChatClient


def test_llm_client_accepts_only_answers_citing_retrieved_lessons(monkeypatch) -> None:
    citation = SourceCitation(
        chapter_id=2,
        chapter_title="Probability",
        lesson_id=7,
        lesson_title="Events",
        excerpt="Probability measures likelihood.",
        relevance=1.0,
    )
    response_body = json.dumps(
        {"choices": [{"message": {"content": "Probability measures likelihood. [lesson:7]"}}]}
    ).encode()

    class Response(BytesIO):
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            self.close()

    monkeypatch.setattr(llm_module, "urlopen", lambda *_args, **_kwargs: Response(response_body))
    client = OpenAICompatibleChatClient(
        "https://llm.example/v1", "test-model", SecretStr("test-key")
    )

    assert client.answer("What is probability?", [], [citation]) == (
        "Probability measures likelihood. [lesson:7]"
    )


def test_llm_client_rejects_unknown_citation(monkeypatch) -> None:
    citation = SourceCitation(
        chapter_id=2,
        chapter_title="Probability",
        lesson_id=7,
        lesson_title="Events",
        excerpt="Probability measures likelihood.",
        relevance=1.0,
    )
    body = json.dumps({"choices": [{"message": {"content": "Answer [lesson:999]"}}]}).encode()

    class Response(BytesIO):
        def __enter__(self):
            return self

        def __exit__(self, *_args):
            self.close()

    monkeypatch.setattr(llm_module, "urlopen", lambda *_args, **_kwargs: Response(body))
    client = OpenAICompatibleChatClient("https://llm.example/v1", "test-model", None)

    assert client.answer("question", [], [citation]) is None
