import re

from app.schemas.assistant import AssistantAnswer, LessonSource, SourceCitation

_STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "for", "from",
    "how", "i", "in", "is", "it", "of", "on", "or", "the", "this", "to", "what",
    "when", "where", "which", "who", "why", "with",
}


def _tokens(text: str) -> set[str]:
    return {
        token
        for token in re.findall(r"\w+", text.casefold(), flags=re.UNICODE)
        if len(token) > 1 and token not in _STOP_WORDS
    }


def find_course_material(
    question: str,
    lessons: list[LessonSource],
    limit: int = 3,
) -> AssistantAnswer:
    question_terms = _tokens(question)
    passages: list[tuple[float, int, LessonSource, str]] = []
    for lesson in lessons:
        sentences = re.split(r"(?<=[.!?])\s+|\n+", lesson.content.strip())
        for index, sentence in enumerate(sentences):
            sentence = sentence.strip()
            if not sentence:
                continue
            overlap = question_terms.intersection(_tokens(sentence))
            if overlap:
                passages.append(
                    (len(overlap) / max(len(question_terms), 1), index, lesson, sentence)
                )

    passages.sort(key=lambda item: (-item[0], item[2].chapter_id, item[2].lesson_id, item[1]))
    citations = [
        SourceCitation(
            chapter_id=lesson.chapter_id,
            chapter_title=lesson.chapter_title,
            lesson_id=lesson.lesson_id,
            lesson_title=lesson.lesson_title,
            excerpt=sentence,
            relevance=round(score, 3),
        )
        for score, _, lesson, sentence in passages[:limit]
    ]
    if not citations:
        return AssistantAnswer(
            answer="I could not find a relevant passage in this course's available lessons.",
            abstained=True,
            citations=[],
        )

    excerpts = " ".join(f"[{index}] {citation.excerpt}" for index, citation in enumerate(citations, 1))
    return AssistantAnswer(
        answer=f"Relevant course material: {excerpts}",
        abstained=False,
        citations=citations,
    )
