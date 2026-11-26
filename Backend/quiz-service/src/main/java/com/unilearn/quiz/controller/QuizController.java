package com.unilearn.quiz.controller;

import com.unilearn.quiz.dto.AttemptResponse;
import com.unilearn.quiz.dto.QuestionRequest;
import com.unilearn.quiz.dto.QuizRequest;
import com.unilearn.quiz.dto.QuizResponse;
import com.unilearn.quiz.dto.SubmitAttemptRequest;
import com.unilearn.quiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizService;
    private final String applicationName;
    private final String port;

    public QuizController(QuizService quizService,
                          @Value("${spring.application.name:quiz-service}") String applicationName,
                          @Value("${server.port:8082}") String port) {
        this.quizService = quizService;
        this.applicationName = applicationName;
        this.port = port;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResponse create(@Valid @RequestBody QuizRequest request) {
        // TODO MS1 : vérifier que le cours existe
        return quizService.create(request);
    }

    @GetMapping
    public List<QuizResponse> findAll(@RequestParam(required = false) Long courseId) {
        return quizService.findAll(courseId);
    }

    @GetMapping("/{id}")
    public QuizResponse findById(@PathVariable Long id) {
        return quizService.findById(id);
    }

    @PutMapping("/{id}")
    public QuizResponse update(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return quizService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizService.delete(id);
    }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResponse addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return quizService.addQuestion(id, request);
    }

    @PostMapping("/{id}/submit")
    public AttemptResponse submit(@PathVariable Long id, @Valid @RequestBody SubmitAttemptRequest request) {
        return quizService.submit(id, request);
    }

    @GetMapping("/{id}/results")
    public List<AttemptResponse> results(@PathVariable Long id) {
        return quizService.findResults(id);
    }

    @GetMapping("/instance-info")
    public Map<String, String> instanceInfo() {
        return Map.of(
                "applicationName", applicationName,
                "port", port,
                "instanceId", applicationName + ":" + port
        );
    }
}
