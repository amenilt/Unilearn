package tn.esprit.unilearn.reclamation.dto;

import jakarta.validation.constraints.NotNull;
import tn.esprit.unilearn.reclamation.entity.ClaimStatus;

/** Corps de PATCH /api/claims/{id}/status, ex. {"status":"IN_PROGRESS"}. */
public record StatusRequest(@NotNull ClaimStatus status) {
}
