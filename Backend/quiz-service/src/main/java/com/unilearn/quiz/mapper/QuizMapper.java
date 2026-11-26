package com.unilearn.quiz.mapper;

import com.unilearn.quiz.dto.OptionRequest;
import com.unilearn.quiz.dto.OptionResponse;
import com.unilearn.quiz.dto.QuestionRequest;
import com.unilearn.quiz.dto.QuestionResponse;
import com.unilearn.quiz.dto.QuizRequest;
import com.unilearn.quiz.dto.QuizResponse;
import com.unilearn.quiz.entity.Option;
import com.unilearn.quiz.entity.Question;
import com.unilearn.quiz.entity.Quiz;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizMapper {

    public Quiz toEntity(QuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setCourseId(request.courseId());
        quiz.setTitle(request.title());
        quiz.setDuration(request.duration());
        request.questions().forEach(questionRequest -> quiz.addQuestion(toEntity(questionRequest)));
        return quiz;
    }

    public Question toEntity(QuestionRequest request) {
        Question question = new Question();
        question.setText(request.text());
        question.setPoints(request.points());
        request.options().forEach(optionRequest -> question.addOption(toEntity(optionRequest)));
        return question;
    }

    public QuizResponse toResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(this::toResponse)
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getCourseId(), quiz.getTitle(), quiz.getDuration(), questions);
    }

    private QuestionResponse toResponse(Question question) {
        List<OptionResponse> options = question.getOptions().stream()
                .map(option -> new OptionResponse(option.getId(), option.getText()))
                .toList();
        return new QuestionResponse(question.getId(), question.getText(), question.getPoints(), options);
    }

    private Option toEntity(OptionRequest request) {
        Option option = new Option();
        option.setText(request.text());
        option.setCorrect(request.correct());
        return option;
    }
}
