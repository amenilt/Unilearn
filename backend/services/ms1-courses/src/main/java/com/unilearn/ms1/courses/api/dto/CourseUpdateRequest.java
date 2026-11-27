package com.unilearn.ms1.courses.api.dto;

import com.unilearn.ms1.courses.domain.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull Long subjectId,
        @NotNull CourseStatus status
) {
}
