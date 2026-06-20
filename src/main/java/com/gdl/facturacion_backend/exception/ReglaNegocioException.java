package com.gdl.facturacion_backend.exception;

/**
 * Error de regla de negocio / validación tributaria (responde 400).
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
