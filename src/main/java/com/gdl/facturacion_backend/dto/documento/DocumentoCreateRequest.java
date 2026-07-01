package com.gdl.facturacion_backend.dto.documento;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentoCreateRequest {

    @NotNull(message = "El tipo de documento es obligatorio")
    private Integer codigoTipoDocumento;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;
}
