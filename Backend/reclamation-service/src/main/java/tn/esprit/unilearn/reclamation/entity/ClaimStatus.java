package tn.esprit.unilearn.reclamation.entity;

import java.util.Set;

/**
 * Cycle de vie d'une reclamation (voir le diagramme d'etats de la conception).
 * OPEN -> IN_PROGRESS -> RESOLVED
 *   \          \
 *    -> REJECTED <-
 */
public enum ClaimStatus {
    OPEN, IN_PROGRESS, RESOLVED, REJECTED;

    /** Transitions autorisees depuis l'etat courant. */
    public Set<ClaimStatus> nextAllowed() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> Set.of(RESOLVED, REJECTED);
            case RESOLVED, REJECTED -> Set.of(); // etats finaux
        };
    }

    public boolean canMoveTo(ClaimStatus target) {
        return nextAllowed().contains(target);
    }
}
