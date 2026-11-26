package com.unilearn.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizRequest(
        @NotNull(message = "courseId est obligatoire")
        @Min(value = 1, message = "courseId doit être supérieur ou égal à 1")
        Long courseId,
        @NotBlank(message = "Le titre est obligatoire")
        String title,
        @Min(value = 1, message = "La durée doit être supérieure ou égale à 1 minute")
        int duration,
        @NotNull(message = "La liste des questions est obligatoire")
        @Size(min = 1, message = "Un quiz doit contenir au moins une question")
        List<@NotNull @Valid QuestionRequest> questions
) {
}
