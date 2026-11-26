package com.unilearn.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
        @NotBlank(message = "Le texte de l'option est obligatoire")
        String text,
        boolean correct
) {
}
