package com.unilearn.ms4.forum.service;

import org.springframework.stereotype.Service;
import com.unilearn.ms4.forum.dto.ForumDtos.AnswerRequest;
import com.unilearn.ms4.forum.dto.ForumDtos.QuestionRequest;
import com.unilearn.ms4.forum.entity.Answer;
import com.unilearn.ms4.forum.entity.Question;
import com.unilearn.ms4.forum.exception.ConflictException;
import com.unilearn.ms4.forum.exception.ResourceNotFoundException;
import com.unilearn.ms4.forum.repository.QuestionRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ForumService {

    private final QuestionRepository repository;

    public ForumService(QuestionRepository repository) {
        this.repository = repository;
    }

    // ---------- Questions (CRUD) ----------

    public Question createQuestion(QuestionRequest req) {
        Question q = new Question();
        q.setCourseId(req.courseId());
        q.setAuthorId(req.authorId());
        q.setTitle(req.title());
        q.setBody(req.body());
        q.setTags(req.tags());
        q.setSolved(false);
        q.setCreatedAt(Instant.now());
        return repository.save(q);
    }

    public List<Question> listQuestions(Long courseId) {
        return courseId == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    public Question getQuestion(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question introuvable : " + id));
    }

    public Question updateQuestion(String id, QuestionRequest req) {
        Question q = getQuestion(id);
        q.setCourseId(req.courseId());
        q.setTitle(req.title());
        q.setBody(req.body());
        q.setTags(req.tags());
        return repository.save(q);
    }

    public void deleteQuestion(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Question introuvable : " + id);
        }
        repository.deleteById(id);
    }

    // ---------- Reponses ----------

    public Answer addAnswer(String questionId, AnswerRequest req) {
        Question q = getQuestion(questionId);
        Answer a = new Answer();
        a.setAuthorId(req.authorId());
        a.setBody(req.body());
        a.setCreatedAt(Instant.now());
        q.getAnswers().add(a);
        repository.save(q);
        return a;
    }

    public List<Answer> listAnswers(String questionId) {
        return getQuestion(questionId).getAnswers();
    }

    public Answer updateAnswer(String answerId, AnswerRequest req) {
        Question q = questionOfAnswer(answerId);
        Answer a = q.findAnswer(answerId).orElseThrow();
        a.setBody(req.body());
        repository.save(q);
        return a;
    }

    public void deleteAnswer(String answerId) {
        Question q = questionOfAnswer(answerId);
        Answer a = q.findAnswer(answerId).orElseThrow();
        q.getAnswers().remove(a);
        if (a.isAccepted()) {
            q.setSolved(false);
        }
        repository.save(q);
    }

    /** Un utilisateur ne peut voter qu'une seule fois pour une reponse (409 sinon). */
    public Answer vote(String answerId, String userId) {
        Question q = questionOfAnswer(answerId);
        Answer a = q.findAnswer(answerId).orElseThrow();
        if (!a.getVoterIds().add(userId)) {
            throw new ConflictException("L'utilisateur " + userId + " a deja vote pour cette reponse");
        }
        a.setVotes(a.getVotes() + 1);
        repository.save(q);
        return a;
    }

    /** Accepte une reponse : la question passe a "solved", une seule reponse acceptee. */
    public Question accept(String answerId) {
        Question q = questionOfAnswer(answerId);
        q.getAnswers().forEach(a -> a.setAccepted(a.getId().equals(answerId)));
        q.setSolved(true);
        return repository.save(q);
    }

    private Question questionOfAnswer(String answerId) {
        return repository.findByAnswersId(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reponse introuvable : " + answerId));
    }
}
