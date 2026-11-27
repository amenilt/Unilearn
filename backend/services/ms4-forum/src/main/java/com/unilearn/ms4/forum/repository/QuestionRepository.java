package com.unilearn.ms4.forum.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.unilearn.ms4.forum.entity.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {

    List<Question> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    List<Question> findAllByOrderByCreatedAtDesc();

    /** Retrouve la question qui contient la reponse donnee (sous-document). */
    Optional<Question> findByAnswersId(String answerId);
}
