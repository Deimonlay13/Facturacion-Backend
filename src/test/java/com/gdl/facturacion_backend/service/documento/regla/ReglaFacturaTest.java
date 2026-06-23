package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
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
 * Test JUnit 5 puro (sin Spring, sin BD) para {@link ReglaFactura}
 * (código 33 — Factura Electrónica afecta a IVA).
 *
 * <p>Verifica el contrato de {@link ReglaTributaria}: {@code codigoSii()},
 * {@code tasaIva()} y {@code validarParaEmitir(...)} (camino feliz y cada validación
 * de error) instanciando la clase directamente.</p>
 */
class ReglaFacturaTest {

    private ReglaFactura regla;

    @BeforeEach
    void setUp() {
        regla = new ReglaFactura();
    }

    // ---------------------------------------------------------------- fixtures

    private DetalleDocumentoEntity detalle() {
        DetalleDocumentoEntity d = new DetalleDocumentoEntity();
        d.setDescripcionItem("Item afecto");
        d.setCantidad(new BigDecimal("1"));
        d.setPrecioUnitario(new BigDecimal("1000"));
        d.setSubtotal(new BigDecimal("1000"));
        return d;
    }

    private DocumentoTributarioEntity documentoConDetalles() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        List<DetalleDocumentoEntity> detalles = new ArrayList<>();
        detalles.add(detalle());
        doc.setDetalles(detalles);
        return doc;
    }

    // ---------------------------------------------------------------- codigoSii

    @Nested
    @DisplayName("codigoSii()")
    class CodigoSii {

        @Test
        @DisplayName("devuelve 33 (Factura Electrónica afecta)")
        void devuelve33() {
            assertThat(regla.codigoSii()).isEqualTo(33);
        }

        @Test
        @DisplayName("es estable entre invocaciones")
        void estable() {
            assertThat(regla.codigoSii()).isEqualTo(regla.codigoSii());
        }
    }

    // ---------------------------------------------------------------- tasaIva

    @Nested
    @DisplayName("tasaIva()")
    class TasaIva {

        @Test
        @DisplayName("devuelve 0.19 (19% IVA)")
        void devuelve019() {
            assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
        }

        @Test
        @DisplayName("conserva la escala exacta '0.19'")
        void escalaExacta() {
            assertThat(regla.tasaIva().toPlainString()).isEqualTo("0.19");
        }

        @Test
        @DisplayName("no es cero (la factura es afecta a IVA)")
        void noEsCero() {
            assertThat(regla.tasaIva().signum()).isEqualTo(1);
        }
    }

    // ------------------------------------------------------------ validarParaEmitir

    @Nested
    @DisplayName("validarParaEmitir()")
    class ValidarParaEmitir {

        @Test
        @DisplayName("camino feliz: documento con al menos un detalle no lanza excepción")
        void caminoFeliz() {
            DocumentoTributarioEntity doc = documentoConDetalles();
            assertThatCode(() -> regla.validarParaEmitir(doc)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta múltiples detalles")
        void multiplesDetalles() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            List<DetalleDocumentoEntity> detalles = new ArrayList<>();
            detalles.add(detalle());
            detalles.add(detalle());
            detalles.add(detalle());
            doc.setDetalles(detalles);

            assertThatCode(() -> regla.validarParaEmitir(doc)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("lanza ReglaNegocioException cuando los detalles son null")
        void detallesNull() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(null);

            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La factura debe tener al menos un detalle");
        }

        @Test
        @DisplayName("lanza ReglaNegocioException cuando la lista de detalles está vacía")
        void detallesVacios() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(Collections.emptyList());

            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La factura debe tener al menos un detalle");
        }
    }

    // ---------------------------------------------------------------- contrato

    @Test
    @DisplayName("ReglaFactura implementa el contrato ReglaTributaria")
    void implementaContrato() {
        assertThat(regla).isInstanceOf(ReglaTributaria.class);
    }
}
