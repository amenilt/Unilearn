package tn.esprit.unilearn.reclamation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de POST /api/claims/{id}/comments. */
public record CommentRequest(@NotBlank String authorId, @NotBlank @Size(max = 1000) String text) {
}
