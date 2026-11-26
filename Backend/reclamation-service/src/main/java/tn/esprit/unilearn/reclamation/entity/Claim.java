package tn.esprit.unilearn.reclamation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reclamation deposee par un etudiant sur un cours.
 * studentId et courseId sont de simples identifiants : l'utilisateur vit dans Keycloak
 * et le cours dans MS1 (pas de base partagee entre microservices).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;
    private Long courseId;

    private String subject;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.OPEN;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** false si MS1 etait indisponible au depot : le cours sera verifie plus tard. */
    private boolean courseVerified;

    // Association 1..N : une reclamation possede plusieurs commentaires
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("date ASC")
    private List<ClaimComment> comments = new ArrayList<>();

    public void addComment(ClaimComment comment) {
        comment.setClaim(this);
        comments.add(comment);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
