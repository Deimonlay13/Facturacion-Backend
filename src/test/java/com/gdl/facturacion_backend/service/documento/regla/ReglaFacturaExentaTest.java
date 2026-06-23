package com.gdl.facturacion_backend.service.documento.regla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;

/**
 * Test JUnit 5 puro (sin Spring, sin BD) para {@link ReglaFacturaExenta}.
 *
 * Código 34 — Factura Exenta: IVA = 0, total = neto.
 * Validación de emisión: debe existir al menos un detalle.
 */
@DisplayName("ReglaFacturaExenta")
class ReglaFacturaExentaTest {

    private ReglaFacturaExenta regla;

    @BeforeEach
    void setUp() {
        regla = new ReglaFacturaExenta();
    }

    // ------------------------------------------------------------------
    // codigoSii()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("codigoSii()")
    class CodigoSii {

        @Test
        @DisplayName("devuelve 34 (Factura Exenta)")
        void devuelve34() {
            assertThat(regla.codigoSii()).isEqualTo(34);
        }

        @Test
        @DisplayName("es estable entre invocaciones")
        void esEstable() {
            assertThat(regla.codigoSii()).isEqualTo(regla.codigoSii());
        }
    }

    // ------------------------------------------------------------------
    // tasaIva()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("tasaIva()")
    class TasaIva {

        @Test
        @DisplayName("es cero porque la factura exenta no afecta IVA")
        void esCero() {
            assertThat(regla.tasaIva()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("nunca es nula")
        void noEsNula() {
            assertThat(regla.tasaIva()).isNotNull();
        }

        @Test
        @DisplayName("no es la tasa afecta del 19%")
        void noEs19() {
            assertThat(regla.tasaIva()).isNotEqualByComparingTo(new BigDecimal("0.19"));
        }
    }

    // ------------------------------------------------------------------
    // validarParaEmitir(documento)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("validarParaEmitir()")
    class ValidarParaEmitir {

        @Test
        @DisplayName("lanza ReglaNegocioException cuando la lista de detalles es null")
        void detallesNull() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(null);

            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La factura exenta debe tener al menos un detalle");
        }

        @Test
        @DisplayName("lanza ReglaNegocioException cuando la lista de detalles está vacía")
        void detallesVacios() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(Collections.emptyList());

            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La factura exenta debe tener al menos un detalle");
        }

        @Test
        @DisplayName("lanza ReglaNegocioException cuando se usa una lista mutable vacía")
        void detallesListaMutableVacia() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(new ArrayList<>());

            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("al menos un detalle");
        }

        @Test
        @DisplayName("no lanza excepción cuando hay al menos un detalle")
        void unDetalle() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(List.of(nuevoDetalle("Servicio exento", new BigDecimal("1000"))));

            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("no lanza excepción cuando hay varios detalles")
        void variosDetalles() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(List.of(
                    nuevoDetalle("Item 1", new BigDecimal("500")),
                    nuevoDetalle("Item 2", new BigDecimal("750"))));

            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }
    }

    private static DetalleDocumentoEntity nuevoDetalle(String descripcion, BigDecimal subtotal) {
        DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
        detalle.setDescripcionItem(descripcion);
        detalle.setCantidad(BigDecimal.ONE);
        detalle.setPrecioUnitario(subtotal);
        detalle.setSubtotal(subtotal);
        return detalle;
    }
}
