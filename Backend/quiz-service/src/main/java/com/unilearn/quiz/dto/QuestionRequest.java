package com.unilearn.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Le texte de la question est obligatoire")
        String text,
        @Min(value = 1, message = "Les points doivent être supérieurs ou égaux à 1")
        int points,
        @NotNull(message = "La liste des options est obligatoire")
        @Size(min = 2, message = "Une question doit avoir au moins deux options")
        List<@NotNull @Valid OptionRequest> options
) {
}
