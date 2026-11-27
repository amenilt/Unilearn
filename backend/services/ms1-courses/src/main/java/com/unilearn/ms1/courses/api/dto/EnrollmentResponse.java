package com.unilearn.ms1.courses.api.dto;

import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        String courseTitle,
        String studentId,
        Instant enrolledAt
) {
}
