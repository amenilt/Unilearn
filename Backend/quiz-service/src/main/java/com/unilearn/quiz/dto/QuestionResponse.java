package com.unilearn.quiz.dto;

import java.util.List;

public record QuestionResponse(Long id, String text, int points, List<OptionResponse> options) {
}
