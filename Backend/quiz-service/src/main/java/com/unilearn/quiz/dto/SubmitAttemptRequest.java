package com.unilearn.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAttemptRequest(
        @NotBlank(message = "studentId est obligatoire") String studentId,
        @NotNull(message = "La liste des réponses est obligatoire")
        List<@NotNull @Valid AnswerRequest> answers
) {
}
