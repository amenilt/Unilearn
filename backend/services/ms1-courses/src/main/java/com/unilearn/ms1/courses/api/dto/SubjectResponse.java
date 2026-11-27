package com.unilearn.ms1.courses.api.dto;

import java.time.Instant;

public record SubjectResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
