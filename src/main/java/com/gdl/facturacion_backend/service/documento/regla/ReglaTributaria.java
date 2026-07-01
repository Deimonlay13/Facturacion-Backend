package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;

import java.math.BigDecimal;

/**
 * Estrategia por tipo de DTE. Cada implementación encapsula la tasa de IVA
 * y las validaciones tributarias propias del tipo, sin tocar el núcleo del módulo.
 */
public interface ReglaTributaria {

    Integer codigoSii();

    BigDecimal tasaIva();

    void validarParaEmitir(DocumentoTributarioEntity documento);
}
