package com.unilearn.quiz.repository;

import com.unilearn.quiz.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    boolean existsByQuizIdAndStudentId(Long quizId, String studentId);
    List<Attempt> findByQuizIdOrderByDateDesc(Long quizId);
}
