package com.gdl.facturacion_backend.dto.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
public class DocumentoResponse {
    Long id;
    Integer codigoTipoDocumento;
    String tipoDocumento;
    Long clienteId;
    String clienteRut;
    String clienteRazonSocial;
    Integer folio;
    LocalDate fechaEmision;
    LocalDate fechaVencimiento;
    String observaciones;
    String moneda;
    BigDecimal tipoCambio;
    String estado;
    String estadoSii;
    BigDecimal montoNeto;
    BigDecimal montoIva;
    BigDecimal montoTotal;
    String rutEmisor;
    String razonSocialEmisor;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    public static DocumentoResponse from(DocumentoTributarioEntity e) {
        ClienteEntity cliente = e.getCliente();
        return new DocumentoResponse(
                e.getId(),
                e.getTipoDocumento() != null ? e.getTipoDocumento().getCodigoSii() : null,
                e.getTipoDocumento() != null ? e.getTipoDocumento().getDescripcion() : null,
                cliente != null ? cliente.getId() : null,
                e.getRut() != null ? e.getRut() : cliente != null ? cliente.getRut() : null,
                e.getRazonSocial() != null ? e.getRazonSocial() : cliente != null ? cliente.getRazonSocial() : null,
                e.getFolio(),
                e.getFechaEmision(),
                e.getFechaVencimiento(),
                e.getObservaciones(),
                e.getMoneda() != null ? e.getMoneda().name() : null,
                e.getTipoCambio(),
                e.getEstado() != null ? e.getEstado().name() : null,
                e.getEstadoSii() != null ? e.getEstadoSii().name() : null,
                e.getMontoNeto(),
                e.getMontoIva(),
                e.getMontoTotal(),
                e.getRutEmisor(),
                e.getRazonSocialEmisor(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
