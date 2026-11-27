package com.unilearn.ms1.courses.api.dto;

import com.unilearn.ms1.courses.domain.CourseStatus;

import java.time.Instant;

public record CourseResponse(
        Long id,
        String title,
        String description,
        String teacherId,
        CourseStatus status,
        Long categoryId,
        String categoryName,
        Long subjectId,
        String subjectName,
        Instant createdAt,
        Instant updatedAt
) {
}
