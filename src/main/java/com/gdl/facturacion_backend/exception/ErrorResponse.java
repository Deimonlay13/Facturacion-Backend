package com.gdl.facturacion_backend.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String mensaje,
        LocalDateTime timestamp
) {}
