package com.unilearn.ms1.courses.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChapterCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Positive Integer position
) {
}
