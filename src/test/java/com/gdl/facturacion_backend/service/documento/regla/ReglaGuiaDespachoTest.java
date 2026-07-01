package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import com.gdl.facturacion_backend.enums.TipoTraslado;
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
 * Test JUnit 5 puro (sin Spring, sin BD) para {@link ReglaGuiaDespacho} (código SII 52).
 * Cubre: identidad del DTE, tasa de IVA y CADA validación de emisión.
 */
class ReglaGuiaDespachoTest {

    private final ReglaGuiaDespacho regla = new ReglaGuiaDespacho();

    // ---------- Helpers ----------

    private static DetalleDocumentoEntity detalle() {
        DetalleDocumentoEntity d = new DetalleDocumentoEntity();
        d.setDescripcionItem("Mercadería");
        d.setCantidad(BigDecimal.ONE);
        d.setPrecioUnitario(new BigDecimal("1000"));
        d.setSubtotal(new BigDecimal("1000"));
        return d;
    }

    private static GuiaDespachoExtraEntity guiaValida() {
        GuiaDespachoExtraEntity g = new GuiaDespachoExtraEntity();
        g.setTipoTraslado(TipoTraslado.VENTA);
        g.setDireccionDestino("Av. Siempre Viva 742, Springfield");
        return g;
    }

    private static DocumentoTributarioEntity documentoValido() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setDetalles(new ArrayList<>(List.of(detalle())));
        doc.setGuiaDespacho(guiaValida());
        return doc;
    }

    // ---------- Identidad del DTE ----------

    @Test
    @DisplayName("codigoSii() devuelve 52 (Guía de Despacho)")
    void codigoSii_devuelve52() {
        assertThat(regla.codigoSii()).isEqualTo(52);
    }

    @Test
    @DisplayName("tasaIva() es CERO (la guía de despacho no aplica IVA)")
    void tasaIva_esCero() {
        assertThat(regla.tasaIva()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(regla.tasaIva()).isZero();
    }

    @Test
    @DisplayName("La regla implementa el contrato ReglaTributaria")
    void implementaContrato() {
        assertThat(regla).isInstanceOf(ReglaTributaria.class);
    }

    // ---------- Camino feliz ----------

    @Test
    @DisplayName("validarParaEmitir() no lanza con detalle, tipo de traslado y dirección de destino")
    void validar_caminoFeliz() {
        assertThatCode(() -> regla.validarParaEmitir(documentoValido()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validarParaEmitir() acepta cualquier valor del enum TipoTraslado")
    void validar_aceptaCadaTipoTraslado() {
        for (TipoTraslado tt : TipoTraslado.values()) {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setTipoTraslado(tt);
            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .as("TipoTraslado=%s", tt)
                    .doesNotThrowAnyException();
        }
    }

    // ---------- Validación: detalles ----------

    @Nested
    @DisplayName("Validación de detalles")
    class Detalles {

        @Test
        @DisplayName("detalles null -> 'al menos un detalle'")
        void detallesNull() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setDetalles(null);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho debe tener al menos un detalle");
        }

        @Test
        @DisplayName("detalles vacíos -> 'al menos un detalle'")
        void detallesVacios() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setDetalles(Collections.emptyList());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho debe tener al menos un detalle");
        }

        @Test
        @DisplayName("la falta de detalles se evalúa antes que la guía (guía null pero sin detalles)")
        void detallesTienePrioridadSobreGuia() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(Collections.emptyList());
            doc.setGuiaDespacho(null);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho debe tener al menos un detalle");
        }
    }

    // ---------- Validación: guía / tipo de traslado ----------

    @Nested
    @DisplayName("Validación de guía y tipo de traslado")
    class GuiaYTraslado {

        @Test
        @DisplayName("guiaDespacho null -> 'requiere el tipo de traslado'")
        void guiaNull() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.setGuiaDespacho(null);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho requiere el tipo de traslado");
        }

        @Test
        @DisplayName("tipoTraslado null -> 'requiere el tipo de traslado'")
        void tipoTrasladoNull() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setTipoTraslado(null);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho requiere el tipo de traslado");
        }
    }

    // ---------- Validación: dirección de destino ----------

    @Nested
    @DisplayName("Validación de dirección de destino")
    class DireccionDestino {

        @Test
        @DisplayName("direccionDestino null -> 'requiere la dirección de destino'")
        void direccionNull() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setDireccionDestino(null);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho requiere la dirección de destino");
        }

        @Test
        @DisplayName("direccionDestino vacía -> 'requiere la dirección de destino'")
        void direccionVacia() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setDireccionDestino("");
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho requiere la dirección de destino");
        }

        @Test
        @DisplayName("direccionDestino solo espacios en blanco -> 'requiere la dirección de destino'")
        void direccionEnBlanco() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setDireccionDestino("   \t  ");
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La guía de despacho requiere la dirección de destino");
        }

        @Test
        @DisplayName("direccionDestino con contenido (rodeado de espacios) es válida")
        void direccionConContenidoEsValida() {
            DocumentoTributarioEntity doc = documentoValido();
            doc.getGuiaDespacho().setDireccionDestino("  Calle Real 123  ");
            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }
    }
}
