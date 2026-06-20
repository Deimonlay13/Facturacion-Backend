package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Código 52 — Guía de Despacho (requiere datos de traslado y destino). */
@Component
public class ReglaGuiaDespacho implements ReglaTributaria {

    @Override
    public Integer codigoSii() {
        return 52;
    }

    @Override
    public BigDecimal tasaIva() {
        return new BigDecimal("0.19");
    }

    @Override
    public void validarParaEmitir(DocumentoTributarioEntity documento) {
        if (documento.getDetalles() == null || documento.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La guía de despacho debe tener al menos un detalle");
        }
        GuiaDespachoExtraEntity guia = documento.getGuiaDespacho();
        if (guia == null || guia.getTipoTraslado() == null) {
            throw new ReglaNegocioException("La guía de despacho requiere el tipo de traslado");
        }
        if (guia.getDireccionDestino() == null || guia.getDireccionDestino().isBlank()) {
            throw new ReglaNegocioException("La guía de despacho requiere la dirección de destino");
        }
    }
}
