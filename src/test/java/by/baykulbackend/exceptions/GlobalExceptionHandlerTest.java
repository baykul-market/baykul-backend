package by.baykulbackend.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleAccessDeniedExceptionReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().get("error"));
    }

    @Test
    void handleDataIntegrityViolationExceptionReturnsConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation", new RuntimeException("duplicate key"));
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Database operation failed due to a constraint violation", response.getBody().get("error"));
    }

    @Test
    void handleMailExceptionReturnsInternalServerError() {
        MailSendException ex = new MailSendException("SMTP server connection refused");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMailException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to dispatch email notification", response.getBody().get("error"));
    }

    @Test
    void handleMethodNotSupportedReturnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("HTTP method 'DELETE' is not supported for this endpoint", response.getBody().get("error"));
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An internal server error occurred", response.getBody().get("error"));
    }
}
