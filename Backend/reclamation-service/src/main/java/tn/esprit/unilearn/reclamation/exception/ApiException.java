package tn.esprit.unilearn.reclamation.exception;

import org.springframework.http.HttpStatus;

/** Erreur metier portant directement son code HTTP (404, 409, 400...). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
