package com.unilearn.ms1.courses.repository;

import com.unilearn.ms1.courses.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByCourseIdOrderByPositionAsc(Long courseId);

    boolean existsByCourseIdAndPosition(Long courseId, int position);
}
