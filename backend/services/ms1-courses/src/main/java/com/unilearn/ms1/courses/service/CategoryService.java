package com.unilearn.ms1.courses.service;

import com.unilearn.ms1.courses.api.dto.CategoryRequest;
import com.unilearn.ms1.courses.api.dto.CategoryResponse;
import com.unilearn.ms1.courses.domain.Category;
import com.unilearn.ms1.courses.repository.CategoryRepository;
import com.unilearn.ms1.courses.repository.SubjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;

    public CategoryService(CategoryRepository categoryRepository, SubjectRepository subjectRepository) {
        this.categoryRepository = categoryRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(getCategory(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        ensureNameAvailable(request.name(), null);
        return toResponse(categoryRepository.save(new Category(request.name().trim(), request.description())));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        ensureNameAvailable(request.name(), id);
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Category category = getCategory(id);
        if (subjectRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category cannot be deleted while it has subjects.");
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " was not found."));
    }

    private void ensureNameAvailable(String name, Long excludedId) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByNameIgnoreCase(name.trim())
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), excludedId);
        if (exists) {
            throw new ConflictException("A category with this name already exists.");
        }
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
