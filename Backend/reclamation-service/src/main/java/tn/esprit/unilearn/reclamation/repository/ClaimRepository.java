package tn.esprit.unilearn.reclamation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.unilearn.reclamation.entity.Claim;
import tn.esprit.unilearn.reclamation.entity.ClaimStatus;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findByStudentId(String studentId);

    List<Claim> findByCourseVerifiedFalse();

    long countByStatus(ClaimStatus status);
}
