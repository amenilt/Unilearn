package com.unilearn.ms1.courses.api.dto;

import java.util.List;

public record ChapterResponse(
        Long id,
        Long courseId,
        String title,
        int position,
        List<LessonResponse> lessons
) {
}
