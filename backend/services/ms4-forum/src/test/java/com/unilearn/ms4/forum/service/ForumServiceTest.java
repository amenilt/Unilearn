package com.unilearn.ms4.forum.service;

import com.unilearn.ms4.forum.dto.ForumDtos.AnswerRequest;
import com.unilearn.ms4.forum.dto.ForumDtos.QuestionRequest;
import com.unilearn.ms4.forum.entity.Answer;
import com.unilearn.ms4.forum.entity.Question;
import com.unilearn.ms4.forum.exception.ConflictException;
import com.unilearn.ms4.forum.exception.ResourceNotFoundException;
import com.unilearn.ms4.forum.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForumServiceTest {

    private QuestionRepository repository;
    private ForumService service;

    @BeforeEach
    void setUp() {
        repository = mock(QuestionRepository.class);
        when(repository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ForumService(repository);
    }

    @Test
    void createQuestionIsNotSolved() {
        Question q = service.createQuestion(new QuestionRequest(1L, "student-1", "Titre", "Corps", List.of("java")));

        assertThat(q.isSolved()).isFalse();
        assertThat(q.getCourseId()).isEqualTo(1L);
        assertThat(q.getAnswers()).isEmpty();
    }

    @Test
    void addAnswerToQuestion() {
        Question q = new Question();
        q.setId("q1");
        when(repository.findById("q1")).thenReturn(Optional.of(q));

        Answer a = service.addAnswer("q1", new AnswerRequest("teacher-1", "Reponse"));

        assertThat(q.getAnswers()).containsExactly(a);
        assertThat(a.getVotes()).isZero();
    }

    @Test
    void unknownQuestionGives404() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQuestion("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void secondVoteOfSameUserGives409() {
        Question q = questionWithAnswer();

        service.vote(q.getAnswers().get(0).getId(), "student-2");

        assertThat(q.getAnswers().get(0).getVotes()).isEqualTo(1);
        assertThatThrownBy(() -> service.vote(q.getAnswers().get(0).getId(), "student-2"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptAnswerSolvesQuestion() {
        Question q = questionWithAnswer();
        String answerId = q.getAnswers().get(0).getId();

        Question result = service.accept(answerId);

        assertThat(result.isSolved()).isTrue();
        assertThat(result.getAnswers().get(0).isAccepted()).isTrue();
    }

    private Question questionWithAnswer() {
        Question q = new Question();
        q.setId("q1");
        Answer a = new Answer();
        q.getAnswers().add(a);
        when(repository.findByAnswersId(a.getId())).thenReturn(Optional.of(q));
        return q;
    }
}
