package com.unilearn.quiz.config;

import com.unilearn.quiz.entity.Option;
import com.unilearn.quiz.entity.Question;
import com.unilearn.quiz.entity.Quiz;
import com.unilearn.quiz.repository.QuizRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "h2"})
public class DemoDataLoader implements CommandLineRunner {
    private final QuizRepository quizRepository;

    public DemoDataLoader(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Override
    public void run(String... args) {
        if (quizRepository.count() > 0) {
            return;
        }
        quizRepository.save(createQuiz(1L, "Quiz de démonstration - Java", 20, new String[][]{
                {"Quel mot-clé crée une classe en Java ?", "class", "function", "define", "struct"},
                {"Quel type représente vrai ou faux ?", "boolean", "String", "int", "char"},
                {"Quel mot-clé hérite d'une classe ?", "extends", "implements", "inherits", "super"}
        }, new int[]{0, 0, 0}));
        quizRepository.save(createQuiz(2L, "Quiz de démonstration - Spring", 15, new String[][]{
                {"Quelle annotation déclare un contrôleur REST ?", "@RestController", "@Entity", "@Bean", "@Repository"},
                {"Quel outil gère les dépendances Maven ?", "pom.xml", "build.gradle", "settings.json", "package.json"},
                {"Quel composant expose des endpoints HTTP ?", "Controller", "Repository", "Entity", "Mapper"}
        }, new int[]{0, 0, 0}));
    }

    private Quiz createQuiz(Long courseId, String title, int duration, String[][] data, int[] correctIndexes) {
        Quiz quiz = new Quiz();
        quiz.setCourseId(courseId);
        quiz.setTitle(title);
        quiz.setDuration(duration);
        for (int i = 0; i < data.length; i++) {
            Question question = new Question();
            question.setText(data[i][0]);
            question.setPoints(1);
            for (int optionIndex = 0; optionIndex < 4; optionIndex++) {
                Option option = new Option();
                option.setText(data[i][optionIndex + 1]);
                option.setCorrect(optionIndex == correctIndexes[i]);
                question.addOption(option);
            }
            quiz.addQuestion(question);
        }
        return quiz;
    }
}
