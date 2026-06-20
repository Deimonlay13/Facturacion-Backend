package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.enums.Moneda;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DocumentoUpdateRequest {

    private Long clienteId;
    private LocalDate fechaVencimiento;
    private String observaciones;
    private Moneda moneda;

    @Positive(message = "El tipo de cambio debe ser mayor a 0")
    private BigDecimal tipoCambio;
}
