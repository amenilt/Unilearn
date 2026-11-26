package com.unilearn.quiz.service;

import com.unilearn.quiz.dto.AttemptResponse;
import com.unilearn.quiz.dto.QuestionRequest;
import com.unilearn.quiz.dto.QuizRequest;
import com.unilearn.quiz.dto.QuizResponse;
import com.unilearn.quiz.dto.SubmitAttemptRequest;
import com.unilearn.quiz.entity.Attempt;
import com.unilearn.quiz.entity.Option;
import com.unilearn.quiz.entity.Question;
import com.unilearn.quiz.entity.Quiz;
import com.unilearn.quiz.exception.DuplicateAttemptException;
import com.unilearn.quiz.exception.ResourceNotFoundException;
import com.unilearn.quiz.mapper.QuizMapper;
import com.unilearn.quiz.repository.AttemptRepository;
import com.unilearn.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {
    private final QuizRepository quizRepository;
    private final AttemptRepository attemptRepository;
    private final QuizMapper quizMapper;

    public QuizService(QuizRepository quizRepository, AttemptRepository attemptRepository, QuizMapper quizMapper) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.quizMapper = quizMapper;
    }

    public QuizResponse create(QuizRequest request) {
        validateCorrectOptions(request.questions());
        return quizMapper.toResponse(quizRepository.save(quizMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> findAll(Long courseId) {
        List<Quiz> quizzes = courseId == null ? quizRepository.findAll() : quizRepository.findByCourseId(courseId);
        return quizzes.stream().map(quizMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public QuizResponse findById(Long id) {
        return quizMapper.toResponse(getQuiz(id));
    }

    public QuizResponse update(Long id, QuizRequest request) {
        validateCorrectOptions(request.questions());
        Quiz quiz = getQuiz(id);
        quiz.setCourseId(request.courseId());
        quiz.setTitle(request.title());
        quiz.setDuration(request.duration());
        Quiz replacement = quizMapper.toEntity(request);
        quiz.replaceQuestions(replacement.getQuestions());
        return quizMapper.toResponse(quizRepository.save(quiz));
    }

    public void delete(Long id) {
        quizRepository.delete(getQuiz(id));
    }

    public QuizResponse addQuestion(Long quizId, QuestionRequest request) {
        validateCorrectOptions(List.of(request));
        Quiz quiz = getQuiz(quizId);
        quiz.addQuestion(quizMapper.toEntity(request));
        return quizMapper.toResponse(quizRepository.save(quiz));
    }

    public AttemptResponse submit(Long quizId, SubmitAttemptRequest request) {
        Quiz quiz = getQuiz(quizId);
        if (attemptRepository.existsByQuizIdAndStudentId(quizId, request.studentId())) {
            throw new DuplicateAttemptException("Cet étudiant a déjà passé ce quiz");
        }

        Map<Long, Question> questions = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Long> answers = request.answers().stream().collect(Collectors.toMap(
                answer -> answer.questionId(),
                answer -> answer.optionId(),
                (first, duplicate) -> {
                    throw new IllegalArgumentException("Une seule réponse est autorisée par question");
                }));

        int score = 0;
        for (Map.Entry<Long, Long> answer : answers.entrySet()) {
            Question question = questions.get(answer.getKey());
            if (question == null) {
                throw new ResourceNotFoundException("Question " + answer.getKey() + " introuvable dans ce quiz");
            }
            Option option = question.getOptions().stream()
                    .filter(candidate -> candidate.getId().equals(answer.getValue()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Option " + answer.getValue() + " introuvable pour cette question"));
            if (option.isCorrect()) {
                score += question.getPoints();
            }
        }

        Attempt attempt = new Attempt();
        attempt.setStudentId(request.studentId());
        attempt.setScore(score);
        attempt.setDate(LocalDateTime.now());
        attempt.setQuiz(quiz);
        Attempt saved = attemptRepository.save(attempt);
        return new AttemptResponse(saved.getId(), saved.getStudentId(), saved.getScore(), saved.getDate());
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> findResults(Long quizId) {
        getQuiz(quizId);
        return attemptRepository.findByQuizIdOrderByDateDesc(quizId).stream()
                .map(attempt -> new AttemptResponse(attempt.getId(), attempt.getStudentId(),
                        attempt.getScore(), attempt.getDate()))
                .toList();
    }

    private Quiz getQuiz(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz " + id + " introuvable"));
    }

    private void validateCorrectOptions(List<QuestionRequest> questions) {
        for (QuestionRequest question : questions) {
            long correctCount = question.options().stream().filter(option -> option.correct()).count();
            if (correctCount != 1) {
                throw new IllegalArgumentException("Chaque question doit avoir exactement une option correcte");
            }
        }
    }
}
