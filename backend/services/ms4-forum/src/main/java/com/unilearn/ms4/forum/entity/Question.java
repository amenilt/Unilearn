package com.unilearn.ms4.forum.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Question du forum (document MongoDB "questions").
 * Association : Question 1 --- * Answer
 * Les reponses sont des sous-documents imbriques dans la question.
 */
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    /** Identifiant du cours dans MS1 (Cours). */
    @Indexed
    private Long courseId;

    /** Identifiant de l'utilisateur (Keycloak). */
    private String authorId;

    private String title;
    private String body;
    private List<String> tags = new ArrayList<>();
    private boolean solved;
    private Instant createdAt;

    private List<Answer> answers = new ArrayList<>();

    public Optional<Answer> findAnswer(String answerId) {
        return answers.stream().filter(a -> a.getId().equals(answerId)).findFirst();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers != null ? answers : new ArrayList<>(); }
}
