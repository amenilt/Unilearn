package com.unilearn.ms1.courses.repository;

import com.unilearn.ms1.courses.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findAllByStudentIdOrderByEnrolledAtDesc(String studentId);

    List<Enrollment> findAllByCourseIdOrderByEnrolledAtAsc(Long courseId);

    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, String studentId);

    boolean existsByCourseIdAndStudentId(Long courseId, String studentId);
}
