package com.unilearn.ms4.forum.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.unilearn.ms4.forum.dto.ForumDtos.AnswerRequest;
import com.unilearn.ms4.forum.dto.ForumDtos.QuestionRequest;
import com.unilearn.ms4.forum.repository.QuestionRepository;
import com.unilearn.ms4.forum.service.ForumService;

import java.util.List;

/** Ajoute quelques donnees de demo si la base est vide. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository repository;
    private final ForumService service;

    public DataInitializer(QuestionRepository repository, ForumService service) {
        this.repository = repository;
        this.service = service;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        var q1 = service.createQuestion(new QuestionRequest(1L, "student-1",
                "Difference entre @Component et @Service ?",
                "Dans Spring Boot, quand utiliser @Service plutot que @Component ?",
                List.of("spring", "java")));
        service.addAnswer(q1.getId(), new AnswerRequest("teacher-1",
                "@Service est une specialisation de @Component pour la couche metier."));

        service.createQuestion(new QuestionRequest(2L, "student-2",
                "Comment fonctionne Eureka ?",
                "Comment un microservice retrouve-t-il un autre service ?",
                List.of("eureka", "microservices")));
    }
}
