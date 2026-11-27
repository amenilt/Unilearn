package com.unilearn.ms4.forum.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.unilearn.ms4.forum.dto.ForumDtos.AnswerRequest;
import com.unilearn.ms4.forum.dto.ForumDtos.VoteRequest;
import com.unilearn.ms4.forum.entity.Answer;
import com.unilearn.ms4.forum.entity.Question;
import com.unilearn.ms4.forum.service.ForumService;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {

    private final ForumService service;

    public AnswerController(ForumService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    public Answer update(@PathVariable String id, @Valid @RequestBody AnswerRequest req) {
        return service.updateAnswer(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deleteAnswer(id);
    }

    /** 200 si le vote est pris en compte, 409 si l'utilisateur a deja vote. */
    @PostMapping("/{id}/vote")
    public Answer vote(@PathVariable String id, @Valid @RequestBody VoteRequest req) {
        return service.vote(id, req.userId());
    }

    /** Marque la reponse comme acceptee et la question comme resolue. */
    @PatchMapping("/{id}/accept")
    public Question accept(@PathVariable String id) {
        return service.accept(id);
    }
}
