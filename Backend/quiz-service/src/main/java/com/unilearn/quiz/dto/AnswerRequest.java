package com.unilearn.quiz.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotNull(message = "questionId est obligatoire") Long questionId,
        @NotNull(message = "optionId est obligatoire") Long optionId
) {
}
