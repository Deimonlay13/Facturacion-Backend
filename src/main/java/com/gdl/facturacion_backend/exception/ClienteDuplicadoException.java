package com.gdl.facturacion_backend.exception;

public class ClienteDuplicadoException extends RuntimeException {
    public ClienteDuplicadoException(String rut) {
        super("Ya existe un cliente con el RUT: " + rut);
    }
}
