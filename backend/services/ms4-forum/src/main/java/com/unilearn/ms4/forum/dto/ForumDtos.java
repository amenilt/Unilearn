package com.unilearn.ms4.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Objets recus par l'API (corps des requetes). */
public final class ForumDtos {

    private ForumDtos() {}

    public record QuestionRequest(
            @NotNull Long courseId,
            @NotBlank String authorId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank String body,
            List<String> tags) {}

    public record AnswerRequest(
            @NotBlank String authorId,
            @NotBlank String body) {}

    public record VoteRequest(
            @NotBlank String userId) {}
}
