package com.gdl.facturacion_backend.service.documento.regla;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.GuiaDespachoExtraEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.enums.TipoTraslado;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test JUnit 5 puro para las reglas tributarias por tipo de DTE.
 * Verifica {@code codigoSii()}, {@code tasaIva()} y {@code validarParaEmitir(...)}
 * construyendo entidades minimas (caminos felices y cada validacion de error).
 */
class ReglaTributariaTest {

    // ---------------------------------------------------------------- fixtures

    private DetalleDocumentoEntity detalle() {
        DetalleDocumentoEntity d = new DetalleDocumentoEntity();
        d.setDescripcionItem("Item");
        d.setCantidad(BigDecimal.ONE);
        d.setPrecioUnitario(new BigDecimal("100"));
        return d;
    }

    private DocumentoTributarioEntity docConDetalle() {
        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
        doc.setDetalles(List.of(detalle()));
        return doc;
    }

    private ReferenciaDocumentoEntity referenciaValida() {
        ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
        ref.setDocumentoReferenciado(new DocumentoTributarioEntity());
        ref.setMotivo("Anulacion por error de facturacion");
        return ref;
    }

    // ================================================================ 33 - Factura afecta

    @Nested
    @DisplayName("ReglaFactura (33)")
    class Factura {

        private final ReglaFactura regla = new ReglaFactura();

        @Test
        @DisplayName("codigo SII 33 y tasa de IVA 19%")
        void codigoYTasa() {
            assertThat(regla.codigoSii()).isEqualTo(33);
            assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
        }

        @Test
        @DisplayName("con al menos un detalle no lanza excepcion")
        void conDetalleOk() {
            assertThatCode(() -> regla.validarParaEmitir(docConDetalle()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin detalles lanza ReglaNegocioException")
        void sinDetallesFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(List.of());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("al menos un detalle");
        }

        @Test
        @DisplayName("detalles null lanza ReglaNegocioException")
        void detallesNullFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    // ================================================================ 34 - Factura exenta

    @Nested
    @DisplayName("ReglaFacturaExenta (34)")
    class FacturaExenta {

        private final ReglaFacturaExenta regla = new ReglaFacturaExenta();

        @Test
        @DisplayName("codigo SII 34 y tasa de IVA 0")
        void codigoYTasa() {
            assertThat(regla.codigoSii()).isEqualTo(34);
            assertThat(regla.tasaIva()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("con al menos un detalle no lanza excepcion")
        void conDetalleOk() {
            assertThatCode(() -> regla.validarParaEmitir(docConDetalle()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin detalles lanza ReglaNegocioException")
        void sinDetallesFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(List.of());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("al menos un detalle");
        }
    }

    // ================================================================ 61 - Nota de credito

    @Nested
    @DisplayName("ReglaNotaCredito (61)")
    class NotaCredito {

        private final ReglaNotaCredito regla = new ReglaNotaCredito();

        @Test
        @DisplayName("codigo SII 61 y tasa de IVA 19%")
        void codigoYTasa() {
            assertThat(regla.codigoSii()).isEqualTo(61);
            assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
        }

        @Test
        @DisplayName("con referencia valida no lanza excepcion")
        void referenciaValidaOk() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of(referenciaValida()));
            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin referencias lanza ReglaNegocioException")
        void sinReferenciasFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("referenciar un documento");
        }

        @Test
        @DisplayName("referencias null lanza ReglaNegocioException")
        void referenciasNullFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("referenciar un documento");
        }

        @Test
        @DisplayName("referencia sin documento referenciado lanza ReglaNegocioException")
        void referenciaSinDocumentoFalla() {
            ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
            ref.setMotivo("Motivo");
            // documentoReferenciado queda null
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of(ref));
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("el documento referenciado y el motivo");
        }

        @Test
        @DisplayName("referencia con motivo en blanco lanza ReglaNegocioException")
        void referenciaMotivoBlancoFalla() {
            ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
            ref.setDocumentoReferenciado(new DocumentoTributarioEntity());
            ref.setMotivo("   ");
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of(ref));
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("el documento referenciado y el motivo");
        }

        @Test
        @DisplayName("referencia con motivo null lanza ReglaNegocioException")
        void referenciaMotivoNullFalla() {
            ReferenciaDocumentoEntity ref = new ReferenciaDocumentoEntity();
            ref.setDocumentoReferenciado(new DocumentoTributarioEntity());
            // motivo queda null
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of(ref));
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("el documento referenciado y el motivo");
        }
    }

    // ================================================================ 56 - Nota de debito

    @Nested
    @DisplayName("ReglaNotaDebito (56)")
    class NotaDebito {

        private final ReglaNotaDebito regla = new ReglaNotaDebito();

        @Test
        @DisplayName("codigo SII 56 y tasa de IVA 19%")
        void codigoYTasa() {
            assertThat(regla.codigoSii()).isEqualTo(56);
            assertThat(regla.tasaIva()).isEqualByComparingTo(new BigDecimal("0.19"));
        }

        @Test
        @DisplayName("con referencia valida no lanza excepcion")
        void referenciaValidaOk() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of(referenciaValida()));
            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin referencias lanza ReglaNegocioException")
        void sinReferenciasFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setReferencias(List.of());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("referenciar un documento");
        }
    }

    // ================================================================ 52 - Guia de despacho

    @Nested
    @DisplayName("ReglaGuiaDespacho (52)")
    class GuiaDespacho {

        private final ReglaGuiaDespacho regla = new ReglaGuiaDespacho();

        private GuiaDespachoExtraEntity guiaValida() {
            GuiaDespachoExtraEntity g = new GuiaDespachoExtraEntity();
            g.setTipoTraslado(TipoTraslado.VENTA);
            g.setDireccionDestino("Bodega Central 456");
            return g;
        }

        @Test
        @DisplayName("codigo SII 52 y tasa de IVA 0")
        void codigoYTasa() {
            assertThat(regla.codigoSii()).isEqualTo(52);
            assertThat(regla.tasaIva()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("con detalle y guia completa no lanza excepcion")
        void guiaCompletaOk() {
            DocumentoTributarioEntity doc = docConDetalle();
            doc.setGuiaDespacho(guiaValida());
            assertThatCode(() -> regla.validarParaEmitir(doc))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sin detalles lanza ReglaNegocioException")
        void sinDetallesFalla() {
            DocumentoTributarioEntity doc = new DocumentoTributarioEntity();
            doc.setDetalles(List.of());
            doc.setGuiaDespacho(guiaValida());
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("al menos un detalle");
        }

        @Test
        @DisplayName("sin datos de guia lanza ReglaNegocioException (tipo de traslado)")
        void sinGuiaFalla() {
            DocumentoTributarioEntity doc = docConDetalle();
            // guiaDespacho queda null
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("tipo de traslado");
        }

        @Test
        @DisplayName("guia sin tipo de traslado lanza ReglaNegocioException")
        void guiaSinTipoTrasladoFalla() {
            DocumentoTributarioEntity doc = docConDetalle();
            GuiaDespachoExtraEntity g = new GuiaDespachoExtraEntity();
            g.setDireccionDestino("Bodega");
            doc.setGuiaDespacho(g);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("tipo de traslado");
        }

        @Test
        @DisplayName("guia sin direccion de destino lanza ReglaNegocioException")
        void guiaSinDireccionFalla() {
            DocumentoTributarioEntity doc = docConDetalle();
            GuiaDespachoExtraEntity g = new GuiaDespachoExtraEntity();
            g.setTipoTraslado(TipoTraslado.TRASLADO_INTERNO);
            // direccionDestino queda null
            doc.setGuiaDespacho(g);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("de destino");
        }

        @Test
        @DisplayName("guia con direccion en blanco lanza ReglaNegocioException")
        void guiaDireccionBlancaFalla() {
            DocumentoTributarioEntity doc = docConDetalle();
            GuiaDespachoExtraEntity g = new GuiaDespachoExtraEntity();
            g.setTipoTraslado(TipoTraslado.VENTA);
            g.setDireccionDestino("   ");
            doc.setGuiaDespacho(g);
            assertThatThrownBy(() -> regla.validarParaEmitir(doc))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("de destino");
        }
    }
}
