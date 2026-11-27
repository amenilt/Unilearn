package com.unilearn.ms1.courses.api;

import com.unilearn.ms1.courses.api.dto.CourseCreateRequest;
import com.unilearn.ms1.courses.api.dto.CourseResponse;
import com.unilearn.ms1.courses.api.dto.CourseUpdateRequest;
import com.unilearn.ms1.courses.domain.CourseStatus;
import com.unilearn.ms1.courses.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses")
@SecurityRequirement(name = "bearerAuth")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @Operation(summary = "List courses visible to the authenticated user")
    public List<CourseResponse> findAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) CourseStatus status,
            Authentication authentication
    ) {
        return courseService.findAll(
                userId(authentication),
                hasRole(authentication, "TEACHER"),
                hasRole(authentication, "ADMIN"),
                categoryId,
                subjectId,
                status
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a course visible to the authenticated user")
    public CourseResponse findById(@PathVariable Long id, Authentication authentication) {
        return courseService.findById(
                id,
                userId(authentication),
                hasRole(authentication, "TEACHER"),
                hasRole(authentication, "ADMIN")
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create a draft course")
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseCreateRequest request,
            Authentication authentication
    ) {
        CourseResponse response = courseService.create(request, userId(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update a course owned by the teacher")
    public CourseResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request,
            Authentication authentication
    ) {
        return courseService.update(id, request, userId(authentication), hasRole(authentication, "ADMIN"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete a course owned by the teacher")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        courseService.delete(id, userId(authentication), hasRole(authentication, "ADMIN"));
        return ResponseEntity.noContent().build();
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
