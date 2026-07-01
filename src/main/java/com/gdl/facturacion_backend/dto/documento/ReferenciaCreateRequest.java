package com.gdl.facturacion_backend.dto.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReferenciaCreateRequest {

    @NotNull(message = "El documento referenciado es obligatorio")
    private Long documentoReferenciadoId;

    private String tipoReferencia;

    @NotBlank(message = "El motivo de la referencia es obligatorio")
    private String motivo;
}
