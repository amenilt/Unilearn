package tn.esprit.unilearn.reclamation.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Appel synchrone MS3 -> MS1 : verifier que le cours concerne existe.
 * On appelle MS1 par son NOM Eureka (MS1-COURS), jamais par IP:port.
 */
@Component
public class CourseClient {

    private static final Logger log = LoggerFactory.getLogger(CourseClient.class);
    private static final String COURSE_URL = "http://MS1-COURS/api/courses/{id}";

    public enum Result { EXISTS, NOT_FOUND, UNAVAILABLE }

    private final RestTemplate restTemplate;

    public CourseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Result checkCourse(Long courseId) {
        try {
            restTemplate.getForObject(COURSE_URL, Object.class, courseId);
            return Result.EXISTS;
        } catch (HttpClientErrorException.NotFound e) {
            return Result.NOT_FOUND;
        } catch (RestClientException | IllegalStateException e) {
            // MS1 arrete ou absent d'Eureka : on ne bloque pas le depot (choix de conception)
            log.warn("MS1-COURS indisponible, verification du cours {} differee : {}", courseId, e.getMessage());
            return Result.UNAVAILABLE;
        }
    }
}
