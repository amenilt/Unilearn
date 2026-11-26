package com.unilearn.quiz.service;

import com.unilearn.quiz.dto.AnswerRequest;
import com.unilearn.quiz.dto.OptionRequest;
import com.unilearn.quiz.dto.QuestionRequest;
import com.unilearn.quiz.dto.QuizRequest;
import com.unilearn.quiz.dto.SubmitAttemptRequest;
import com.unilearn.quiz.entity.Attempt;
import com.unilearn.quiz.entity.Option;
import com.unilearn.quiz.entity.Question;
import com.unilearn.quiz.entity.Quiz;
import com.unilearn.quiz.exception.DuplicateAttemptException;
import com.unilearn.quiz.mapper.QuizMapper;
import com.unilearn.quiz.repository.AttemptRepository;
import com.unilearn.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private AttemptRepository attemptRepository;
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepository, attemptRepository, new QuizMapper());
    }

    @Test
    void submitCalculatesScoreAndPersistsAttempt() {
        Quiz quiz = sampleQuiz();
        when(quizRepository.findById(7L)).thenReturn(Optional.of(quiz));
        when(attemptRepository.existsByQuizIdAndStudentId(7L, "student-1")).thenReturn(false);
        when(attemptRepository.save(any(Attempt.class))).thenAnswer(invocation -> {
            Attempt attempt = invocation.getArgument(0);
            attempt.setId(11L);
            return attempt;
        });

        var result = quizService.submit(7L, new SubmitAttemptRequest("student-1",
                List.of(new AnswerRequest(21L, 31L), new AnswerRequest(22L, 33L))));

        assertEquals(5, result.score());
        verify(attemptRepository).save(any(Attempt.class));
    }

    @Test
    void submitRejectsSecondAttemptWithConflictException() {
        when(quizRepository.findById(7L)).thenReturn(Optional.of(sampleQuiz()));
        when(attemptRepository.existsByQuizIdAndStudentId(7L, "student-1")).thenReturn(true);

        assertThrows(DuplicateAttemptException.class, () -> quizService.submit(7L,
                new SubmitAttemptRequest("student-1", List.of())));
        verify(attemptRepository, never()).save(any(Attempt.class));
    }

    @Test
    void createRejectsQuestionWithoutExactlyOneCorrectOption() {
        QuizRequest request = new QuizRequest(1L, "Quiz", 10,
                List.of(new QuestionRequest("Q", 1, List.of(
                        new OptionRequest("A", false), new OptionRequest("B", false)))));
        assertThrows(IllegalArgumentException.class, () -> quizService.create(request));
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    private Quiz sampleQuiz() {
        Quiz quiz = new Quiz();
        quiz.setId(7L);
        Question first = question(21L, 3, 31L, true);
        Question second = question(22L, 2, 33L, true);
        quiz.addQuestion(first);
        quiz.addQuestion(second);
        return quiz;
    }

    private Question question(Long id, int points, Long correctOptionId, boolean correct) {
        Question question = new Question();
        question.setId(id);
        question.setPoints(points);
        Option expected = new Option();
        expected.setId(correctOptionId);
        expected.setCorrect(correct);
        question.addOption(expected);
        Option wrong = new Option();
        wrong.setId(correctOptionId + 1);
        wrong.setCorrect(false);
        question.addOption(wrong);
        return question;
    }
}
