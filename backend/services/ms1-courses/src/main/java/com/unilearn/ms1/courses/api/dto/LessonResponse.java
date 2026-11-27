package com.unilearn.ms1.courses.api.dto;

public record LessonResponse(
        Long id,
        Long chapterId,
        String title,
        int position,
        String content
) {
}
