package com.gdl.facturacion_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RutInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleRutInvalido(RutInvalidoException ex) {
        return build(HttpStatus.BAD_REQUEST, "RUT inválido", ex.getMessage());
    }

    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(ClienteDuplicadoException ex) {
        return build(HttpStatus.CONFLICT, "Cliente duplicado", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex) {
        return build(HttpStatus.BAD_REQUEST, "Regla de negocio", ex.getMessage());
    }

    @ExceptionHandler(SreApiException.class)
    public ResponseEntity<ErrorResponse> handleSreApi(SreApiException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Error al consultar SRE", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "Error de validación", mensaje);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, mensaje, LocalDateTime.now()));
    }
}
