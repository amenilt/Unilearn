package com.unilearn.ms1.courses.repository;

import com.unilearn.ms1.courses.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllByChapterIdOrderByPositionAsc(Long chapterId);

    boolean existsByChapterIdAndPosition(Long chapterId, int position);
}
