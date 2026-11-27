package com.unilearn.ms1.courses.service;

import com.unilearn.ms1.courses.api.dto.CourseCreateRequest;
import com.unilearn.ms1.courses.domain.Category;
import com.unilearn.ms1.courses.domain.Course;
import com.unilearn.ms1.courses.domain.CourseStatus;
import com.unilearn.ms1.courses.domain.Subject;
import com.unilearn.ms1.courses.repository.CourseRepository;
import com.unilearn.ms1.courses.repository.EnrollmentRepository;
import com.unilearn.ms1.courses.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, subjectRepository, enrollmentRepository);
    }

    @Test
    void createsCoursesAsDraftsOwnedByTheAuthenticatedTeacher() {
        Category category = new Category("Computer Science", null);
        Subject subject = new Subject(category, "Distributed Systems", null);
        when(subjectRepository.findById(12L)).thenReturn(Optional.of(subject));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseService.create(new CourseCreateRequest("  Cloud Basics  ", "Introduction", 12L), "teacher-7");

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Cloud Basics");
        assertThat(captor.getValue().getTeacherId()).isEqualTo("teacher-7");
        assertThat(captor.getValue().getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void hidesDraftCoursesFromStudents() {
        Category category = new Category("Computer Science", null);
        Subject subject = new Subject(category, "Distributed Systems", null);
        Course draft = new Course(subject, "teacher-7", "Draft", null, CourseStatus.DRAFT);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> courseService.findById(3L, "student-4", false, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsDuplicateCourseEnrollments() {
        Course course = new Course(
                new Subject(new Category("Computer Science", null), "Distributed Systems", null),
                "teacher-7",
                "Published",
                null,
                CourseStatus.PUBLISHED
        );
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByCourseIdAndStudentId(3L, "student-4")).thenReturn(true);

        assertThatThrownBy(() -> courseService.enroll(3L, "student-4"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already enrolled");
    }
}
