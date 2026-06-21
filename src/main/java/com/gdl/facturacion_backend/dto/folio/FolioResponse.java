package com.gdl.facturacion_backend.dto.folio;

import com.gdl.facturacion_backend.entity.FolioEntity;
import lombok.Value;

@Value
public class FolioResponse {
    Long id;
    Integer numero;
    Integer tipo;
    String estado;

    public static FolioResponse from(FolioEntity f) {
        return new FolioResponse(
                f.getId(),
                f.getNumero(),
                f.getTipoDocumento() != null ? f.getTipoDocumento().getCodigoSii() : null,
                f.getEstado() != null ? f.getEstado().name() : null
        );
    }
}
