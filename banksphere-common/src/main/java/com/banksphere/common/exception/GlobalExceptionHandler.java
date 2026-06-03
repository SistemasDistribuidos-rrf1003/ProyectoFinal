package com.banksphere.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor de excepciones global para todas las APIs REST de BankSphere.
 * Traduce excepciones de negocio y de validación en respuestas JSON con códigos HTTP semánticos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DTO de respuesta para estructurar la salida de errores JSON.
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ErrorDetails {
        private final LocalDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;
    }

    /**
     * Captura: Recursos no encontrados (Usuarios o Cuentas).
     * Retorna: 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleResourceNotFound(ResourceNotFoundException ex, org.springframework.web.context.request.WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(details, HttpStatus.NOT_FOUND);
    }

    /**
     * Captura: Operaciones sin fondos.
     * Retorna: 400 Bad Request
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorDetails> handleInsufficientFunds(InsufficientFundsException ex, org.springframework.web.context.request.WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Insufficient Funds",
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    /**
     * Captura: Bloqueos de seguridad (Fraude / AML).
     * Retorna: 403 Forbidden
     */
    @ExceptionHandler(FraudBlockException.class)
    public ResponseEntity<ErrorDetails> handleFraudBlock(FraudBlockException ex, org.springframework.web.context.request.WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied / Fraud Lock",
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(details, HttpStatus.FORBIDDEN);
    }

    /**
     * Captura: Errores en la validación de campos del DTO (@Valid en @RequestBody).
     * Retorna: 400 Bad Request (con desglose detallado de qué campos fallaron)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, org.springframework.web.context.request.WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        // Recolectamos la lista de todos los campos que violaron las reglas (@NotBlank, @Size, @Email, etc.)
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage); 
        });

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("status", HttpStatus.BAD_REQUEST.value());
        responseBody.put("error", "Validation Failed");
        responseBody.put("fields", fieldErrors);
        responseBody.put("path", request.getDescription(false));

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    /**
     * Captura: Excepciones lógicas genéricas (IllegalArgumentException / IllegalStateException).
     * Retorna: 400 Bad Request
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorDetails> handleBadRequest(RuntimeException ex, org.springframework.web.context.request.WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }

    /**
     * Captura: Cualquier otro error interno imprevisto.
     * Retorna: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, org.springframework.web.context.request.WebRequest request) {
        ErrorDetails details = new ErrorDetails(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage(),
                request.getDescription(false)
        );
        return new ResponseEntity<>(details, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}