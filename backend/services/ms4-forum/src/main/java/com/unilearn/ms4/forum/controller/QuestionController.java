package com.unilearn.ms4.forum.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.unilearn.ms4.forum.dto.ForumDtos.AnswerRequest;
import com.unilearn.ms4.forum.dto.ForumDtos.QuestionRequest;
import com.unilearn.ms4.forum.entity.Answer;
import com.unilearn.ms4.forum.entity.Question;
import com.unilearn.ms4.forum.service.ForumService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final ForumService service;

    @Value("${server.port}")
    private String port;

    public QuestionController(ForumService service) {
        this.service = service;
    }

    /** Permet de voir quelle instance repond (utile avec 2 instances). */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("service", "ms4-forum", "port", port, "message", "Hello from MS4 Forum");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Question create(@Valid @RequestBody QuestionRequest req) {
        return service.createQuestion(req);
    }

    /** GET /api/questions            -> toutes les questions
     *  GET /api/questions?course=1   -> questions d'un cours */
    @GetMapping
    public List<Question> list(@RequestParam(name = "course", required = false) Long courseId) {
        return service.listQuestions(courseId);
    }

    @GetMapping("/{id}")
    public Question get(@PathVariable String id) {
        return service.getQuestion(id);
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable String id, @Valid @RequestBody QuestionRequest req) {
        return service.updateQuestion(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deleteQuestion(id);
    }

    // ----- reponses d'une question -----

    @PostMapping("/{id}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public Answer addAnswer(@PathVariable String id, @Valid @RequestBody AnswerRequest req) {
        return service.addAnswer(id, req);
    }

    @GetMapping("/{id}/answers")
    public List<Answer> answers(@PathVariable String id) {
        return service.listAnswers(id);
    }
}
