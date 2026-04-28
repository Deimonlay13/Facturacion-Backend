package com.gdl.facturacion_backend.dto.documento;

import lombok.Getter;

@Getter
public class DocumentoCreateRequest {

    private Integer codigoTipoDocumento;
    private Long clienteId;
}