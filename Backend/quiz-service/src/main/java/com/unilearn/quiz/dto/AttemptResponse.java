package com.unilearn.quiz.dto;

import java.time.LocalDateTime;

public record AttemptResponse(Long id, String studentId, int score, LocalDateTime date) {
}
