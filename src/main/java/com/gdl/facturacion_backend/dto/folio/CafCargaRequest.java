package com.gdl.facturacion_backend.dto.folio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CafCargaRequest {

    @NotNull(message = "El tipo de documento es obligatorio")
    private Integer codigoTipoDocumento;

    @NotNull(message = "El rango desde es obligatorio")
    @Positive(message = "El rango desde debe ser mayor a 0")
    private Integer rangoDesde;

    @NotNull(message = "El rango hasta es obligatorio")
    @Positive(message = "El rango hasta debe ser mayor a 0")
    private Integer rangoHasta;

    @NotNull(message = "La fecha de autorización es obligatoria")
    private LocalDate fechaAutorizacion;

    private LocalDate fechaVencimiento;

    private String cafXml;
}
