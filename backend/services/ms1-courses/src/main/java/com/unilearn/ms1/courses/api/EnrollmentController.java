package com.unilearn.ms1.courses.api;

import com.unilearn.ms1.courses.api.dto.EnrollmentResponse;
import com.unilearn.ms1.courses.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Enrollments")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final CourseService courseService;

    public EnrollmentController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/api/courses/{courseId}/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Enroll the authenticated student in a published course")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long courseId, Authentication authentication) {
        EnrollmentResponse response = courseService.enroll(courseId, userId(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/api/courses/{courseId}/enrollments/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw from a course")
    public ResponseEntity<Void> withdraw(@PathVariable Long courseId, Authentication authentication) {
        courseService.withdraw(courseId, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/enrollments/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List the authenticated student's enrollments")
    public List<EnrollmentResponse> findMyEnrollments(Authentication authentication) {
        return courseService.findMyEnrollments(userId(authentication));
    }

    @GetMapping("/api/courses/{courseId}/enrollments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "List enrollments for an owned course")
    public List<EnrollmentResponse> findCourseEnrollments(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return courseService.findCourseEnrollments(
                courseId,
                userId(authentication),
                hasRole(authentication, "ADMIN")
        );
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
