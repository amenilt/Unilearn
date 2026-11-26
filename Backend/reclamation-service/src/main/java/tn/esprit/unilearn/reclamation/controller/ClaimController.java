package tn.esprit.unilearn.reclamation.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.unilearn.reclamation.dto.ClaimRequest;
import tn.esprit.unilearn.reclamation.dto.CommentRequest;
import tn.esprit.unilearn.reclamation.dto.StatusRequest;
import tn.esprit.unilearn.reclamation.entity.Claim;
import tn.esprit.unilearn.reclamation.entity.ClaimComment;
import tn.esprit.unilearn.reclamation.entity.ClaimStatus;
import tn.esprit.unilearn.reclamation.service.ClaimService;

import java.util.List;
import java.util.Map;

/** Contrat d'API de MS3 (cf. diapo "MS3 Reclamations" de la conception). */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService service;

    @Value("${server.port}")
    private String port;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    /** Pour voir quelle instance repond (8083 ou 8093). */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("service", "MS3-RECLAMATIONS", "port", port);
    }

    /** GET /api/claims?status=OPEN  ou  ?studentId=...  -> 200 */
    @GetMapping
    public List<Claim> findAll(@RequestParam(required = false) ClaimStatus status,
                               @RequestParam(required = false) String studentId) {
        return service.findAll(status, studentId);
    }

    /** -> 200, 404 */
    @GetMapping("/{id}")
    public Claim findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** -> 201, 400 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Claim create(@Valid @RequestBody ClaimRequest req) {
        return service.create(req);
    }

    /** -> 200, 400, 404, 409 (deja prise en charge) */
    @PutMapping("/{id}")
    public Claim update(@PathVariable Long id, @Valid @RequestBody ClaimRequest req) {
        return service.update(id, req);
    }

    /** -> 204, 404 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** -> 200, 404, 409 (transition interdite) */
    @PatchMapping("/{id}/status")
    public Claim changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.changeStatus(id, req.status());
    }

    /** -> 201, 404 */
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimComment addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return service.addComment(id, req);
    }

    /** -> 200, 404 */
    @GetMapping("/{id}/comments")
    public List<ClaimComment> comments(@PathVariable Long id) {
        return service.findById(id).getComments();
    }

    /** Re-verifie aupres de MS1 les cours non verifies (deposes pendant une panne). */
    @PostMapping("/reverify-courses")
    public Map<String, Integer> reverify() {
        return Map.of("verifiees", service.reverifyPendingCourses());
    }
}
