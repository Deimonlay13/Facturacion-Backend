package com.gdl.facturacion_backend.dto.sii;

import com.gdl.facturacion_backend.entity.EnvioSiiEntity;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class EnvioSiiResponse {
    Long id;
    String trackId;
    LocalDateTime fechaEnvio;
    String estado;
    String respuesta;
    Integer intentos;

    public static EnvioSiiResponse from(EnvioSiiEntity e) {
        if (e == null) return null;
        return new EnvioSiiResponse(
                e.getId(),
                e.getTrackId(),
                e.getFechaEnvio(),
                e.getEstado() != null ? e.getEstado().name() : null,
                e.getRespuesta(),
                e.getIntentos());
    }
}
