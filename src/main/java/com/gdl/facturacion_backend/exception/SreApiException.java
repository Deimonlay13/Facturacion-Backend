package com.gdl.facturacion_backend.exception;

public class SreApiException extends RuntimeException {
    public SreApiException(String mensaje) {
        super(mensaje);
    }

    public SreApiException(String mensaje, Throwable cause) {
        super(mensaje, cause);
    }
}
