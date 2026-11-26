package com.unilearn.ms1.courses.api;

import com.unilearn.ms1.courses.api.dto.ChapterCreateRequest;
import com.unilearn.ms1.courses.api.dto.ChapterResponse;
import com.unilearn.ms1.courses.api.dto.LessonCreateRequest;
import com.unilearn.ms1.courses.api.dto.LessonResponse;
import com.unilearn.ms1.courses.service.CourseContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Course content")
@SecurityRequirement(name = "bearerAuth")
public class CourseContentController {

    private final CourseContentService courseContentService;

    public CourseContentController(CourseContentService courseContentService) {
        this.courseContentService = courseContentService;
    }

    @GetMapping("/api/courses/{courseId}/chapters")
    @Operation(summary = "List chapters and lessons for a visible course")
    public List<ChapterResponse> findChapters(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return courseContentService.findChapters(
                courseId,
                userId(authentication),
                hasRole(authentication, "TEACHER"),
                hasRole(authentication, "ADMIN")
        );
    }

    @PostMapping("/api/courses/{courseId}/chapters")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Add a chapter to an owned course")
    public ResponseEntity<ChapterResponse> createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody ChapterCreateRequest request,
            Authentication authentication
    ) {
        ChapterResponse response = courseContentService.createChapter(
                courseId,
                request,
                userId(authentication),
                hasRole(authentication, "ADMIN")
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{chapterId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/api/chapters/{chapterId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Add a text lesson to an owned course chapter")
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable Long chapterId,
            @Valid @RequestBody LessonCreateRequest request,
            Authentication authentication
    ) {
        LessonResponse response = courseContentService.createLesson(
                chapterId,
                request,
                userId(authentication),
                hasRole(authentication, "ADMIN")
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{lessonId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    private static String userId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }
        return authentication.getName();
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + role));
    }
}
