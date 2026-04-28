package com.gdl.facturacion_backend.dto.documento;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DocumentoResponse {

    private Long id;
    private Integer codigoTipoDocumento;
    private String tipoDocumento;
    private Long clienteId;
    private String estado;
    private String estadoSii;
    private Integer folio;
    private LocalDate fechaEmision;
}