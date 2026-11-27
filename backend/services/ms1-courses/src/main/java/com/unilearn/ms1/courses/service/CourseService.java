package com.unilearn.ms1.courses.service;

import com.unilearn.ms1.courses.api.dto.CourseCreateRequest;
import com.unilearn.ms1.courses.api.dto.CourseResponse;
import com.unilearn.ms1.courses.api.dto.CourseUpdateRequest;
import com.unilearn.ms1.courses.api.dto.EnrollmentResponse;
import com.unilearn.ms1.courses.domain.Course;
import com.unilearn.ms1.courses.domain.CourseStatus;
import com.unilearn.ms1.courses.domain.Enrollment;
import com.unilearn.ms1.courses.domain.Subject;
import com.unilearn.ms1.courses.repository.CourseRepository;
import com.unilearn.ms1.courses.repository.EnrollmentRepository;
import com.unilearn.ms1.courses.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll(
            String userId,
            boolean teacher,
            boolean administrator,
            Long categoryId,
            Long subjectId,
            CourseStatus status
    ) {
        String ownerFilter = teacher && !administrator ? userId : null;
        CourseStatus visibilityFilter = !teacher && !administrator ? CourseStatus.PUBLISHED : status;
        return courseRepository.search(ownerFilter, categoryId, subjectId, visibilityFilter)
                .stream()
                .map(CourseService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id, String userId, boolean teacher, boolean administrator) {
        Course course = getCourseForViewing(id, userId, teacher, administrator);
        return toResponse(course);
    }

    public CourseResponse create(CourseCreateRequest request, String teacherId) {
        Subject subject = getSubject(request.subjectId());
        Course course = new Course(
                subject,
                teacherId,
                request.title().trim(),
                request.description(),
                CourseStatus.DRAFT
        );
        return toResponse(courseRepository.save(course));
    }

    public CourseResponse update(Long id, CourseUpdateRequest request, String userId, boolean administrator) {
        Course course = getCourse(id);
        ensureOwnerOrAdministrator(course, userId, administrator);
        course.setSubject(getSubject(request.subjectId()));
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setStatus(request.status());
        return toResponse(courseRepository.save(course));
    }

    public void delete(Long id, String userId, boolean administrator) {
        Course course = getCourseForManagement(id, userId, administrator);
        courseRepository.delete(course);
    }

    public EnrollmentResponse enroll(Long courseId, String studentId) {
        Course course = getCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ConflictException("Only published courses can be joined.");
        }
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ConflictException("The student is already enrolled in this course.");
        }
        return toResponse(enrollmentRepository.save(new Enrollment(course, studentId)));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findMyEnrollments(String studentId) {
        return enrollmentRepository.findAllByStudentIdOrderByEnrolledAtDesc(studentId)
                .stream()
                .map(CourseService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findCourseEnrollments(Long courseId, String userId, boolean administrator) {
        Course course = getCourse(courseId);
        ensureOwnerOrAdministrator(course, userId, administrator);
        return enrollmentRepository.findAllByCourseIdOrderByEnrolledAtAsc(courseId)
                .stream()
                .map(CourseService::toResponse)
                .toList();
    }

    public void withdraw(Long courseId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment was not found."));
        enrollmentRepository.delete(enrollment);
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course " + id + " was not found."));
    }

    Course getCourseForViewing(Long id, String userId, boolean teacher, boolean administrator) {
        Course course = getCourse(id);
        ensureVisible(course, userId, teacher, administrator);
        return course;
    }

    Course getCourseForManagement(Long id, String userId, boolean administrator) {
        Course course = getCourse(id);
        ensureOwnerOrAdministrator(course, userId, administrator);
        return course;
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject " + id + " was not found."));
    }

    private static void ensureVisible(Course course, String userId, boolean teacher, boolean administrator) {
        if (administrator) {
            return;
        }
        if (teacher) {
            if (!course.getTeacherId().equals(userId)) {
                throw new ResourceNotFoundException("Course " + course.getId() + " was not found.");
            }
            return;
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Course " + course.getId() + " was not found.");
        }
    }

    private static void ensureOwnerOrAdministrator(Course course, String userId, boolean administrator) {
        if (!administrator && !course.getTeacherId().equals(userId)) {
            throw new ForbiddenOperationException("Only the course owner or an administrator can manage this course.");
        }
    }

    private static CourseResponse toResponse(Course course) {
        Subject subject = course.getSubject();
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getTeacherId(),
                course.getStatus(),
                subject.getCategory().getId(),
                subject.getCategory().getName(),
                subject.getId(),
                subject.getName(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    private static EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getStudentId(),
                enrollment.getEnrolledAt()
        );
    }
}
