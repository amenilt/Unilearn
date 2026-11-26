package tn.esprit.unilearn.reclamation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.unilearn.reclamation.client.CourseClient;
import tn.esprit.unilearn.reclamation.dto.ClaimRequest;
import tn.esprit.unilearn.reclamation.dto.CommentRequest;
import tn.esprit.unilearn.reclamation.entity.Claim;
import tn.esprit.unilearn.reclamation.entity.ClaimComment;
import tn.esprit.unilearn.reclamation.entity.ClaimStatus;
import tn.esprit.unilearn.reclamation.exception.ApiException;
import tn.esprit.unilearn.reclamation.repository.ClaimCommentRepository;
import tn.esprit.unilearn.reclamation.repository.ClaimRepository;

import java.util.List;

@Service
@Transactional
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository repo;
    private final ClaimCommentRepository commentRepo;
    private final CourseClient courseClient;

    public ClaimService(ClaimRepository repo, ClaimCommentRepository commentRepo, CourseClient courseClient) {
        this.repo = repo;
        this.commentRepo = commentRepo;
        this.courseClient = courseClient;
    }

    @Transactional(readOnly = true)
    public List<Claim> findAll(ClaimStatus status, String studentId) {
        if (status != null) {
            return repo.findByStatus(status);
        }
        if (studentId != null) {
            return repo.findByStudentId(studentId);
        }
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Claim findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reclamation " + id + " introuvable"));
    }

    /** Depot : ne depend jamais de la disponibilite d'un autre service. */
    public Claim create(ClaimRequest req) {
        Claim claim = new Claim();
        apply(claim, req);
        claim.setCourseVerified(verifyCourse(req.courseId()));
        Claim saved = repo.save(claim);
        log.info("Reclamation {} deposee par {} (cours {}, verifie={})",
                saved.getId(), saved.getStudentId(), saved.getCourseId(), saved.isCourseVerified());
        return saved;
    }

    public Claim update(Long id, ClaimRequest req) {
        Claim claim = findById(id);
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "Seule une reclamation OPEN peut etre modifiee");
        }
        boolean courseChanged = !claim.getCourseId().equals(req.courseId());
        apply(claim, req);
        if (courseChanged) {
            claim.setCourseVerified(verifyCourse(req.courseId()));
        }
        return repo.save(claim);
    }

    public void delete(Long id) {
        repo.delete(findById(id));
    }

    /** PATCH status : 409 si la transition n'est pas permise par le diagramme d'etats. */
    public Claim changeStatus(Long id, ClaimStatus target) {
        Claim claim = findById(id);
        if (!claim.getStatus().canMoveTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Transition interdite : " + claim.getStatus() + " -> " + target
                            + " (autorisees : " + claim.getStatus().nextAllowed() + ")");
        }
        log.info("Reclamation {} : {} -> {}", id, claim.getStatus(), target);
        claim.setStatus(target);
        // Evolution prevue (conception) : publier claim.status.changed vers MS5 via RabbitMQ
        return repo.save(claim);
    }

    public ClaimComment addComment(Long id, CommentRequest req) {
        Claim claim = findById(id);
        ClaimComment comment = new ClaimComment(req.authorId(), req.text());
        claim.addComment(comment);       // maintient les deux cotes de l'association
        return commentRepo.save(comment); // persist : l'id genere est renvoye au client
    }

    /** Re-verifie les cours des reclamations deposees pendant une panne de MS1. */
    public int reverifyPendingCourses() {
        int verified = 0;
        for (Claim c : repo.findByCourseVerifiedFalse()) {
            if (courseClient.checkCourse(c.getCourseId()) == CourseClient.Result.EXISTS) {
                c.setCourseVerified(true);
                verified++;
            }
        }
        return verified;
    }

    private boolean verifyCourse(Long courseId) {
        return switch (courseClient.checkCourse(courseId)) {
            case EXISTS -> true;
            case NOT_FOUND -> throw new ApiException(HttpStatus.BAD_REQUEST, "Le cours " + courseId + " n'existe pas");
            case UNAVAILABLE -> false; // MS1 en panne : enregistree quand meme, verifiee plus tard
        };
    }

    private void apply(Claim claim, ClaimRequest req) {
        claim.setStudentId(req.studentId());
        claim.setCourseId(req.courseId());
        claim.setSubject(req.subject());
        claim.setDescription(req.description());
    }
}
