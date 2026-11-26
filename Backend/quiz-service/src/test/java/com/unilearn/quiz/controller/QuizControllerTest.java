package com.unilearn.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unilearn.quiz.dto.QuizResponse;
import com.unilearn.quiz.dto.QuestionResponse;
import com.unilearn.quiz.dto.OptionResponse;
import com.unilearn.quiz.exception.GlobalExceptionHandler;
import com.unilearn.quiz.exception.ResourceNotFoundException;
import com.unilearn.quiz.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
@Import(GlobalExceptionHandler.class)
class QuizControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private QuizService quizService;

    @Test
    void createReturns201() throws Exception {
        when(quizService.create(any())).thenReturn(new QuizResponse(1L, 2L, "Java", 20,
                List.of(new QuestionResponse(3L, "Question?", 1, List.of(new OptionResponse(4L, "Oui"))))));
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQuizJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());
    }

    @Test
    void createReturns400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":null,\"title\":\"\",\"duration\":0,\"questions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/quizzes"));
    }

    @Test
    void getReturns404ForMissingQuiz() throws Exception {
        when(quizService.findById(eq(404L))).thenThrow(new ResourceNotFoundException("Quiz 404 introuvable"));
        mockMvc.perform(get("/api/quizzes/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Quiz 404 introuvable"));
    }

    private String validQuizJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "courseId", 2,
                "title", "Java",
                "duration", 20,
                "questions", List.of(java.util.Map.of(
                        "text", "Question?",
                        "points", 1,
                        "options", List.of(
                                java.util.Map.of("text", "Oui", "correct", true),
                                java.util.Map.of("text", "Non", "correct", false)
                        )
                ))
        ));
    }
}
