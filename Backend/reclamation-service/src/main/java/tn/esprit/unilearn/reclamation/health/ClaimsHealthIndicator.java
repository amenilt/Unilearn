package tn.esprit.unilearn.reclamation.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import tn.esprit.unilearn.reclamation.entity.ClaimStatus;
import tn.esprit.unilearn.reclamation.repository.ClaimRepository;

/**
 * HealthIndicator personnalise : apparait dans /actuator/health sous le nom "claims".
 * UP si la base H2 repond (avec le nombre de reclamations par statut), DOWN sinon.
 */
@Component("claims")
public class ClaimsHealthIndicator implements HealthIndicator {

    private final ClaimRepository repo;

    public ClaimsHealthIndicator(ClaimRepository repo) {
        this.repo = repo;
    }

    @Override
    public Health health() {
        try {
            return Health.up()
                    .withDetail("total", repo.count())
                    .withDetail("open", repo.countByStatus(ClaimStatus.OPEN))
                    .withDetail("inProgress", repo.countByStatus(ClaimStatus.IN_PROGRESS))
                    .withDetail("coursNonVerifies", repo.findByCourseVerifiedFalse().size())
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
