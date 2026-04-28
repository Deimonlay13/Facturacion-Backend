package com.gdl.facturacion_backend.dto.tipoDocumento;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TipoDocumentoResponseDto {

    private Long id;
    private Integer codigoSii;
    private String descripcion;
}