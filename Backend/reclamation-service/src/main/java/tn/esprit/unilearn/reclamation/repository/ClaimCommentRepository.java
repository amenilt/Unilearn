package tn.esprit.unilearn.reclamation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.unilearn.reclamation.entity.ClaimComment;

public interface ClaimCommentRepository extends JpaRepository<ClaimComment, Long> {
}
