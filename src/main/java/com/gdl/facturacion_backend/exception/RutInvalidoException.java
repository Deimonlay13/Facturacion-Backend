package com.gdl.facturacion_backend.exception;

public class RutInvalidoException extends RuntimeException {
    public RutInvalidoException(String rut) {
        super("El RUT ingresado no es válido: " + rut);
    }
}
