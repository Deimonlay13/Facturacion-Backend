package com.gdl.facturacion_backend.dto.sii;

import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class HistorialSiiResponse {
    String estado;
    LocalDateTime fechaEstado;
    String mensaje;

    public static HistorialSiiResponse from(HistorialEstadoSiiEntity h) {
        return new HistorialSiiResponse(
                h.getEstado() != null ? h.getEstado().name() : null,
                h.getFechaEstado(),
                h.getMensaje());
    }
}
