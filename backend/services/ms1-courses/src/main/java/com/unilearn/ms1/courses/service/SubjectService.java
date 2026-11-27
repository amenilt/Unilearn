package com.unilearn.ms1.courses.service;

import com.unilearn.ms1.courses.api.dto.SubjectRequest;
import com.unilearn.ms1.courses.api.dto.SubjectResponse;
import com.unilearn.ms1.courses.domain.Category;
import com.unilearn.ms1.courses.domain.Subject;
import com.unilearn.ms1.courses.repository.CategoryRepository;
import com.unilearn.ms1.courses.repository.CourseRepository;
import com.unilearn.ms1.courses.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            CategoryRepository categoryRepository,
            CourseRepository courseRepository
    ) {
        this.subjectRepository = subjectRepository;
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll(Long categoryId) {
        List<Subject> subjects = categoryId == null
                ? subjectRepository.findAll()
                : subjectRepository.findAllByCategoryIdOrderByName(categoryId);
        return subjects.stream().map(SubjectService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Long id) {
        return toResponse(getSubject(id));
    }

    public SubjectResponse create(SubjectRequest request) {
        Category category = getCategory(request.categoryId());
        ensureNameAvailable(category.getId(), request.name(), null);
        return toResponse(subjectRepository.save(
                new Subject(category, request.name().trim(), request.description())
        ));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = getSubject(id);
        Category category = getCategory(request.categoryId());
        ensureNameAvailable(category.getId(), request.name(), id);
        subject.setCategory(category);
        subject.setName(request.name().trim());
        subject.setDescription(request.description());
        return toResponse(subjectRepository.save(subject));
    }

    public void delete(Long id) {
        Subject subject = getSubject(id);
        if (courseRepository.existsBySubjectId(id)) {
            throw new ConflictException("Subject cannot be deleted while it has courses.");
        }
        subjectRepository.delete(subject);
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject " + id + " was not found."));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " was not found."));
    }

    private void ensureNameAvailable(Long categoryId, String name, Long excludedId) {
        boolean exists = excludedId == null
                ? subjectRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name.trim())
                : subjectRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, name.trim(), excludedId);
        if (exists) {
            throw new ConflictException("A subject with this name already exists in the category.");
        }
    }

    private static SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getCategory().getId(),
                subject.getCategory().getName(),
                subject.getName(),
                subject.getDescription(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}
