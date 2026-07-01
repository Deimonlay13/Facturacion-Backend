package com.gdl.facturacion_backend.dto.folio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FolioCargaSimpleRequest {

    @NotNull(message = "El tipo de documento es obligatorio")
    private Integer codigoTipoDocumento;

    @NotNull(message = "La cantidad de folios es obligatoria")
    @Positive(message = "La cantidad de folios debe ser mayor a 0")
    private Integer cantidad;
}
