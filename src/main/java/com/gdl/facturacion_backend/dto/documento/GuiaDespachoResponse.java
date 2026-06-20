package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import lombok.Value;

@Value
public class GuiaDespachoResponse {
    Long id;
    String tipoTraslado;
    String patente;
    String rutTransportista;
    String nombreTransportista;
    String direccionDestino;

    public static GuiaDespachoResponse from(GuiaDespachoExtraEntity e) {
        return new GuiaDespachoResponse(
                e.getId(),
                e.getTipoTraslado() != null ? e.getTipoTraslado().name() : null,
                e.getPatente(),
                e.getRutTransportista(),
                e.getNombreTransportista(),
                e.getDireccionDestino()
        );
    }
}
