package com.gdl.facturacion_backend.exception;

/** Usuario o contraseña inválidos (responde 401). */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
