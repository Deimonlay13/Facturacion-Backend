package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test JUnit 5 PURO (sin Spring, sin BD) para {@link ReglaNotaDebito} (código SII 56).
 *
 * <p>{@code ReglaNotaDebito} sólo redefine {@link ReglaNotaDebito#codigoSii()}; el resto del
 * comportamiento (tasa de IVA y validación de referencias) lo hereda de {@link ReglaNotaBase}.
 * Por eso aquí se ejercitan también esos caminos heredados: tasaIva() y CADA validación /
 * excepción de {@code validarParaEmitir}.</p>
 */
class ReglaNotaDebitoTest {

    private final ReglaNotaDebito regla = new ReglaNotaDebito();

    // ---------- helpers de construcción de entidades mínimas ----------

    private static ReferenciaDocumentoEntity referenciaValida() {
        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(new DocumentoTributarioEntity());
        ref.setMotivo("Recargo por intereses de mora");
        return ref;
    }

    private static DocumentoTributarioEntity documentoConReferencias(ReferenciaDocumentoEntity... refs) {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(new ArrayList<>(List.of(refs)));
        return doc;
    }

    private static DocumentoTributarioEntity documentoValido() {
        return documentoConReferencias(referenciaValida());
    }

    // ---------- codigoSii / tasaIva / jerarquía ----------

    @Test
    @DisplayName("codigoSii() devuelve 56 (Nota de Débito)")
    void codigoSiiEs56() {
        assertThat(regla.codigoSii()).isEqualTo(56);
    }

    @Test
    @DisplayName("codigoSii() NO es el de la Nota de Crédito (61)")
    void codigoSiiNoEs61() {
        assertThat(regla.codigoSii())
                .isNotEqualTo(new ReglaNotaCredito().codigoSii())
                .isNotEqualTo(61);
    }

    @Test
    @DisplayName("tasaIva() heredada es 0.19")
    void tasaIvaEs019() {
        assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
        // valor heredado de ReglaNotaBase, idéntico al de la nota de crédito
        assertThat(regla.tasaIva()).isEqualByComparingTo(new ReglaNotaCredito().tasaIva());
    }

    @Test
    @DisplayName("tasaIva() nunca es nula")
    void tasaIvaNoNula() {
        assertThat(regla.tasaIva()).isNotNull();
    }

    @Test
    @DisplayName("es una ReglaNotaBase / ReglaTributaria")
    void jerarquia() {
        assertThat(regla)
                .isInstanceOf(ReglaNotaBase.class)
                .isInstanceOf(ReglaTributaria.class);
    }

    // ---------- validarParaEmitir: camino feliz ----------

    @Test
    @DisplayName("validarParaEmitir() no lanza con una referencia válida")
    void caminoFeliz() {
        assertThatCode(() -> regla.validarParaEmitir(documentoValido()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("basta UNA referencia válida entre varias inválidas")
    void unaValidaEntreInvalidas() {
        ReferenciaDocumentoEntity sinDocumento = referenciaValida();
        sinDocumento.setDocumentoReferenciado(null);
        ReferenciaDocumentoEntity sinMotivo = referenciaValida();
        sinMotivo.setMotivo(null);

        DocumentoTributarioEntity doc =
                documentoConReferencias(sinDocumento, sinMotivo, referenciaValida());

        assertThatCode(() -> regla.validarParaEmitir(doc))
                .doesNotThrowAnyException();
    }

    // ---------- validarParaEmitir: falta de referencias ----------

    @Test
    @DisplayName("referencias null -> 'debe referenciar un documento existente'")
    void referenciasNull() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(null);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nota debe referenciar un documento existente");
    }

    @Test
    @DisplayName("referencias vacías -> 'debe referenciar un documento existente'")
    void referenciasVacias() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setReferencias(Collections.emptyList());
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nota debe referenciar un documento existente");
    }

    // ---------- validarParaEmitir: referencias inválidas ----------

    @Test
    @DisplayName("referencia sin documento referenciado -> 'documento referenciado y el motivo'")
    void referenciaSinDocumento() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setDocumentoReferenciado(null);
        DocumentoTributarioEntity doc = documentoConReferencias(ref);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("referencia con motivo null -> 'documento referenciado y el motivo'")
    void referenciaMotivoNull() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setMotivo(null);
        DocumentoTributarioEntity doc = documentoConReferencias(ref);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("referencia con motivo en blanco (espacios) -> 'documento referenciado y el motivo'")
    void referenciaMotivoEnBlanco() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setMotivo("   ");
        DocumentoTributarioEntity doc = documentoConReferencias(ref);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("referencia con motivo vacío ('') -> 'documento referenciado y el motivo'")
    void referenciaMotivoVacio() {
        ReferenciaDocumentoEntity ref = referenciaValida();
        ref.setMotivo("");
        DocumentoTributarioEntity doc = documentoConReferencias(ref);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    @Test
    @DisplayName("todas las referencias inválidas -> 'documento referenciado y el motivo'")
    void todasInvalidas() {
        ReferenciaDocumentoEntity sinDocumento = referenciaValida();
        sinDocumento.setDocumentoReferenciado(null);
        ReferenciaDocumentoEntity sinMotivo = referenciaValida();
        sinMotivo.setMotivo(null);
        DocumentoTributarioEntity doc = documentoConReferencias(sinDocumento, sinMotivo);
        assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La referencia debe indicar el documento referenciado y el motivo");
    }

    /**
     * {@code ReglaNotaDebito} (56) y {@code ReglaNotaCredito} (61) comparten {@link ReglaNotaBase};
     * la validación de referencias se comporta idénticamente: misma excepción y mismo mensaje.
     */
    @Nested
    @DisplayName("Paridad de validación con la Nota de Crédito (misma base)")
    class ParidadConNotaCredito {

        private final ReglaNotaCredito notaCredito = new ReglaNotaCredito();

        @Test
        @DisplayName("ambas aceptan el mismo documento válido")
        void mismoDocumentoValido() {
            assertThatCode(() -> regla.validarParaEmitir(documentoValido()))
                    .doesNotThrowAnyException();
            assertThatCode(() -> notaCredito.validarParaEmitir(documentoValido()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ambas rechazan referencias vacías con el mismo mensaje")
        void mismoRechazoReferenciasVacias() {
            String esperado = "La nota debe referenciar un documento existente";

            DocumentoTributarioEntity docA = new DocumentoTributarioEntity();
            docA.setReferencias(Collections.emptyList());
            DocumentoTributarioEntity docB = new DocumentoTributarioEntity();
            docB.setReferencias(Collections.emptyList());

            assertThatThrownBy(() -> regla.validarParaEmitir(docA))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage(esperado);
            assertThatThrownBy(() -> notaCredito.validarParaEmitir(docB))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage(esperado);
        }
    }
}
