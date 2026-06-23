package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test JUnit 5 puro para {@link ReglaNotaCredito} (código SII 61).
 * Ejercita además la lógica de {@link ReglaNotaBase}: tasa de IVA y validación de referencias.
 */
class ReglaNotaCreditoTest {

    private final ReglaNotaCredito regla = new ReglaNotaCredito();

    private static ReferenciaDocumentoEntity referenciaValida() {
        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(new DocumentoTributarioEntity());
        ref.setMotivo("Anulación de factura por error de monto");
        return ref;
    }

    private static DocumentoTributarioEntity documentoValido() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(referenciaValida())));
        return doc;
    }

    @Test
    @DisplayName("codigoSii() devuelve 61 (Nota de Crédito)")
    void codigoSii() {
        assertThat(regla.codigoSii()).isEqualTo(61);
    }

    @Test
    @DisplayName("tasaIva() es 0.19")
    void tasaIva() {
        assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
    }

    @Test
    @DisplayName("es una ReglaNotaBase / ReglaTributaria")
    void jerarquia() {
        assertThat(regla)
                .isInstanceOf(ReglaNotaBase.class)
                .isInstanceOf(ReglaTributaria.class);
    }

    @Test
    @DisplayName("validarParaEmitir() no lanza con una referencia válida")
    void caminoFeliz() {
        assertThatCode(() -> regla.validarParaEmitir(documentoValido()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("referencias null -> 'debe referenciar un documento existente'")
    void referenciasNull() {
        DocumentoTributarioEntity doc = documentoValido();
        doc.setReferencias(null);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nota debe referenciar un documento existente");
    }

    @Test
    @DisplayName("referencias vacías -> 'debe referenciar un documento existente'")
    void referenciasVacias() {
        DocumentoTributarioEntity doc = documentoValido();
        doc.setReferencias(Collections.emptyList());
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nota debe referenciar un documento existente");
    }

    @Test
    @DisplayName("referencia sin documento referenciado -> 'documento referenciado y el motivo'")
    void referenciaSinDocumento() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setDocumentoReferenciado(null);
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(ref)));
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("referencia con motivo null -> 'documento referenciado y el motivo'")
    void referenciaMotivoNull() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setMotivo(null);
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(ref)));
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("referencia con motivo en blanco -> 'documento referenciado y el motivo'")
    void referenciaMotivoEnBlanco() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setMotivo("   ");
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(ref)));
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("basta UNA referencia válida entre varias inválidas")
    void unaValidaEntreInvalidas() {
        ReferenciaDocumentoEntity invalida = referenciaValida();
        invalida.setMotivo(null);
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(invalida, referenciaValida())));
        assertThatCode(() -> regla.validarParaEmitir(doc))
                .doesNotThrowAnyException();
    }
}
