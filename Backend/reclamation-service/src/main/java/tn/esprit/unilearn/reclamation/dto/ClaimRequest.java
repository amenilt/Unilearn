package tn.esprit.unilearn.reclamation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corps de POST /api/claims et PUT /api/claims/{id}. Un champ invalide => 400. */
public record ClaimRequest(
        @NotBlank String studentId,
        @NotNull Long courseId,
        @NotBlank @Size(max = 150) String subject,
        @NotBlank @Size(max = 2000) String description) {
}
