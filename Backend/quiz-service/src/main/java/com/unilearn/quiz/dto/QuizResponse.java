package com.unilearn.quiz.dto;

import java.util.List;

public record QuizResponse(Long id, Long courseId, String title, int duration, List<QuestionResponse> questions) {
}
