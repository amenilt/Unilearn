from fastapi import APIRouter, Depends, HTTPException

from app.core.config import Settings, get_settings
from app.core.security import UserContext, require_student
from app.schemas.assistant import AssistantAnswer, AssistantQuestion
from app.services.assistant import find_course_material
from app.services.course_content import CourseContentClient, CourseContentUnavailable
from app.services.demo_data import DemoCourseContentClient
from app.services.llm import OpenAICompatibleChatClient

router = APIRouter(prefix="/api/v1/assistant", tags=["pedagogical assistant"])


def get_course_content_client(
    settings: Settings = Depends(get_settings),
) -> CourseContentClient:
    if settings.demo_mode:
        return DemoCourseContentClient()
    return CourseContentClient(settings.ms1_courses_url)


def get_chat_client(settings: Settings = Depends(get_settings)) -> OpenAICompatibleChatClient:
    return OpenAICompatibleChatClient(
        base_url=settings.llm_base_url,
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        timeout=settings.llm_timeout_seconds,
    )


@router.post(
    "/chat",
    response_model=AssistantAnswer,
    summary="Ask a question about a course",
    description="Retrieves lesson passages, answers with a configured OpenAI-compatible model when available, and returns cited extracts otherwise. It abstains when course material has no relevant passage.",
    responses={502: {"description": "MS1 course content unavailable"}},
)
@router.post("/ask", response_model=AssistantAnswer, include_in_schema=False)
def ask_about_course(
    body: AssistantQuestion,
    user: UserContext = Depends(require_student),
    content_client: CourseContentClient = Depends(get_course_content_client),
    chat_client: OpenAICompatibleChatClient = Depends(get_chat_client),
) -> AssistantAnswer:
    try:
        lessons = content_client.list_lessons(body.course_id, user.access_token)
    except CourseContentUnavailable as error:
        raise HTTPException(status_code=502, detail="Course content is unavailable") from error
    extractive = find_course_material(body.question, lessons)
    if extractive.abstained:
        return extractive
    answer = chat_client.answer(body.question, body.history, extractive.citations)
    if answer is None:
        return extractive
    return extractive.model_copy(update={"answer": answer, "mode": "grounded_llm"})
