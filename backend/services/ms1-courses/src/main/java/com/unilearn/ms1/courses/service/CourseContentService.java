package com.unilearn.ms1.courses.service;

import com.unilearn.ms1.courses.api.dto.ChapterCreateRequest;
import com.unilearn.ms1.courses.api.dto.ChapterResponse;
import com.unilearn.ms1.courses.api.dto.LessonCreateRequest;
import com.unilearn.ms1.courses.api.dto.LessonResponse;
import com.unilearn.ms1.courses.domain.Chapter;
import com.unilearn.ms1.courses.domain.Course;
import com.unilearn.ms1.courses.domain.Lesson;
import com.unilearn.ms1.courses.repository.ChapterRepository;
import com.unilearn.ms1.courses.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourseContentService {

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;

    public CourseContentService(
            CourseService courseService,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository
    ) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> findChapters(
            Long courseId,
            String userId,
            boolean teacher,
            boolean administrator
    ) {
        courseService.findById(courseId, userId, teacher, administrator);
        return chapterRepository.findAllByCourseIdOrderByPositionAsc(courseId)
                .stream()
                .map(CourseContentService::toResponse)
                .toList();
    }

    public ChapterResponse createChapter(
            Long courseId,
            ChapterCreateRequest request,
            String userId,
            boolean administrator
    ) {
        Course course = courseService.getCourseForManagement(courseId, userId, administrator);
        if (chapterRepository.existsByCourseIdAndPosition(courseId, request.position())) {
            throw new ConflictException("The course already has a chapter at this position.");
        }
        Chapter chapter = chapterRepository.save(
                new Chapter(course, request.title().trim(), request.position())
        );
        return toResponse(chapter);
    }

    public LessonResponse createLesson(
            Long chapterId,
            LessonCreateRequest request,
            String userId,
            boolean administrator
    ) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter " + chapterId + " was not found."));
        courseService.getCourseForManagement(chapter.getCourse().getId(), userId, administrator);
        if (lessonRepository.existsByChapterIdAndPosition(chapterId, request.position())) {
            throw new ConflictException("The chapter already has a lesson at this position.");
        }
        Lesson lesson = lessonRepository.save(new Lesson(
                chapter,
                request.title().trim(),
                request.position(),
                request.content().trim()
        ));
        return toResponse(lesson);
    }

    private static ChapterResponse toResponse(Chapter chapter) {
        List<LessonResponse> lessons = chapter.getLessons().stream()
                .sorted(java.util.Comparator.comparingInt(Lesson::getPosition))
                .map(CourseContentService::toResponse)
                .toList();
        return new ChapterResponse(
                chapter.getId(),
                chapter.getCourse().getId(),
                chapter.getTitle(),
                chapter.getPosition(),
                lessons
        );
    }

    private static LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getChapter().getId(),
                lesson.getTitle(),
                lesson.getPosition(),
                lesson.getContent()
        );
    }
}
