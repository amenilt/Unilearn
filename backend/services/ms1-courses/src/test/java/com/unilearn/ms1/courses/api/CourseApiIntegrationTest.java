package com.unilearn.ms1.courses.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unilearn.ms1.courses.repository.CourseRepository;
import com.unilearn.ms1.courses.repository.EnrollmentRepository;
import com.unilearn.ms1.courses.repository.SubjectRepository;
import com.unilearn.ms1.courses.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void startsHttpServerAndExposesHealthAndOpenApiEndpoints() {
        var health = restTemplate.getForEntity("/actuator/health", String.class);
        var openApi = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
        assertThat(openApi.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(openApi.getBody()).contains("\"openapi\"", "/api/courses");
    }

    @BeforeEach
    void clearDatabase() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        subjectRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void teacherCanCreateAndPublishACourseAndStudentCanEnrollAndWithdraw() throws Exception {
        long categoryId = createCategory("Sciences");
        long subjectId = createSubject(categoryId, "Mathematics");

        MvcResult createdCourse = mockMvc.perform(post("/api/courses")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Linear Algebra","description":"Matrices and vectors","subjectId":%d}
                                """.formatted(subjectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        long courseId = idFrom(createdCourse);

        mockMvc.perform(get("/api/courses/{id}", courseId)
                        .with(jwtFor("teacher-1", "TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value("teacher-1"))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Linear Algebra","description":"Matrices and vectors","subjectId":%d,"status":"PUBLISHED"}
                                """.formatted(subjectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/courses/{id}/enrollments", courseId)
                        .with(jwtFor("student-1", "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value("student-1"));

        mockMvc.perform(post("/api/courses/{id}/enrollments", courseId)
                        .with(jwtFor("student-1", "STUDENT")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/enrollments/me")
                        .with(jwtFor("student-1", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(courseId));

        mockMvc.perform(delete("/api/courses/{id}/enrollments/me", courseId)
                        .with(jwtFor("student-1", "STUDENT")))
                .andExpect(status().isNoContent());
    }

    @Test
    void validatesRequestsAndEnforcesOwnershipAndRoles() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","description":"invalid","subjectId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());

        mockMvc.perform(post("/api/courses")
                        .with(jwtFor("student-1", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Not allowed","subjectId":1}
                                """))
                .andExpect(status().isForbidden());

        long categoryId = createCategory("Arts");
        long subjectId = createSubject(categoryId, "Music");
        long courseId = idFrom(mockMvc.perform(post("/api/courses")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Music Theory","subjectId":%d}
                                """.formatted(subjectId)))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(delete("/api/courses/{id}", courseId)
                        .with(jwtFor("teacher-2", "TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void preventsDeletingCategoriesAndSubjectsThatAreInUse() throws Exception {
        long categoryId = createCategory("Engineering");
        long subjectId = createSubject(categoryId, "Robotics");
        mockMvc.perform(post("/api/courses")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Robotics 101","subjectId":%d}
                                """.formatted(subjectId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/subjects/{id}", subjectId)
                        .with(jwtFor("teacher-1", "TEACHER")))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(jwtFor("teacher-1", "TEACHER")))
                .andExpect(status().isConflict());
    }

    @Test
    void teacherCanManageOrderedCourseContentAndStudentsCanReadPublishedContent() throws Exception {
        long categoryId = createCategory("Humanities");
        long subjectId = createSubject(categoryId, "History");
        long courseId = idFrom(mockMvc.perform(post("/api/courses")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"World History","subjectId":%d}
                                """.formatted(subjectId)))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"World History","subjectId":%d,"status":"PUBLISHED"}
                                """.formatted(subjectId)))
                .andExpect(status().isOk());

        MvcResult chapter = mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ancient Civilizations","position":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessons").isEmpty())
                .andReturn();
        long chapterId = idFrom(chapter);

        mockMvc.perform(post("/api/chapters/{chapterId}/lessons", chapterId)
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Mesopotamia","position":1,"content":"An introduction to early civilizations."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("An introduction to early civilizations."));

        mockMvc.perform(post("/api/chapters/{chapterId}/lessons", chapterId)
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Duplicate position","position":1,"content":"This must not replace the first lesson."}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/courses/{courseId}/chapters", courseId)
                        .with(jwtFor("student-1", "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Ancient Civilizations"))
                .andExpect(jsonPath("$[0].lessons[0].title").value("Mesopotamia"));

        mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                        .with(jwtFor("teacher-2", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unauthorized chapter","position":2}
                                """))
                .andExpect(status().isForbidden());
    }

    private long createCategory(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("name", name, "description", "Test category")
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private long createSubject(long categoryId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/subjects")
                        .with(jwtFor("teacher-1", "TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("categoryId", categoryId, "name", name)
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(result);
    }

    private long idFrom(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return json.get("id").asLong();
    }

    private static JwtRequestPostProcessor jwtFor(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
