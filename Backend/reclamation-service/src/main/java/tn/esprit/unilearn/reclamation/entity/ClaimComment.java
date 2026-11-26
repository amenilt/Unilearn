package tn.esprit.unilearn.reclamation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Commentaire d'un etudiant ou d'un enseignant sur une reclamation. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ClaimComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorId;

    @Column(name = "comment_text", length = 1000)
    private String text;

    @Column(name = "comment_date")
    private LocalDateTime date;

    // Association N..1 vers la reclamation
    @ManyToOne(optional = false)
    @JoinColumn(name = "claim_id")
    @JsonIgnore // evite la boucle Claim -> comments -> claim -> ... en JSON
    private Claim claim;

    public ClaimComment(String authorId, String text) {
        this.authorId = authorId;
        this.text = text;
    }

    @PrePersist
    void onCreate() {
        date = LocalDateTime.now();
    }
}
