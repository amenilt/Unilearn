package com.unilearn.ms1.courses.repository;

import com.unilearn.ms1.courses.domain.Course;
import com.unilearn.ms1.courses.domain.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
            select c from Course c
            where (:teacherId is null or c.teacherId = :teacherId)
              and (:categoryId is null or c.subject.category.id = :categoryId)
              and (:subjectId is null or c.subject.id = :subjectId)
              and (:status is null or c.status = :status)
            order by c.createdAt desc
            """)
    List<Course> search(
            @Param("teacherId") String teacherId,
            @Param("categoryId") Long categoryId,
            @Param("subjectId") Long subjectId,
            @Param("status") CourseStatus status
    );

    Optional<Course> findByIdAndTeacherId(Long id, String teacherId);

    boolean existsBySubjectId(Long subjectId);
}
