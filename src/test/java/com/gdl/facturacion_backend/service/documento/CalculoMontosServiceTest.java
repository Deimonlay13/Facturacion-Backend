package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculoMontosService - cálculo de montos en backend")
class CalculoMontosServiceTest {

    private static final int CODIGO_SII = 33;
    private static final BigDecimal TASA_IVA = new BigDecimal("0.19");

    @Mock
    private ReglaTributariaResolver reglaResolver;

    @Mock
    private ReglaTributaria regla;

    @InjectMocks
    private CalculoMontosService service;

    private DocumentoTributarioEntity documento;
    private TipoDocumentoEntity tipoDocumento;

    @BeforeEach
    void setUp() {
        tipoDocumento = new TipoDocumentoEntity();
        tipoDocumento.setCodigoSii(CODIGO_SII);

        documento = new DocumentoTributarioEntity();
        documento.setTipoDocumento(tipoDocumento);
    }

    /** Helper para crear un detalle con cantidad y precio. */
    private DetalleDocumentoEntity detalle(String cantidad, String precio) {
        DetalleDocumentoEntity d = new DetalleDocumentoEntity();
        d.setCantidad(cantidad != null ? new BigDecimal(cantidad) : null);
        d.setPrecioUnitario(precio != null ? new BigDecimal(precio) : null);
        return d;
    }

    // ----------------------------------------------------------------------
    // calcularSubtotal
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("calcularSubtotal: cantidad * precio redondeado HALF_UP a entero")
    void calcularSubtotal_caminoFeliz() {
        DetalleDocumentoEntity d = detalle("3", "1000");

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("3000");
        assertThat(subtotal.scale()).isZero();
    }

    @Test
    @DisplayName("calcularSubtotal: redondea hacia arriba en .5 (HALF_UP)")
    void calcularSubtotal_redondeoHalfUpArriba() {
        // 1 * 1000.5 = 1000.5 -> 1001
        DetalleDocumentoEntity d = detalle("1", "1000.5");

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("1001");
    }

    @Test
    @DisplayName("calcularSubtotal: redondea hacia abajo bajo .5")
    void calcularSubtotal_redondeoHalfUpAbajo() {
        // 1 * 1000.4 = 1000.4 -> 1000
        DetalleDocumentoEntity d = detalle("1", "1000.49");

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("calcularSubtotal: cantidad null se trata como cero")
    void calcularSubtotal_cantidadNull() {
        DetalleDocumentoEntity d = detalle(null, "1000");

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calcularSubtotal: precio null se trata como cero")
    void calcularSubtotal_precioNull() {
        DetalleDocumentoEntity d = detalle("5", null);

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calcularSubtotal: cantidad y precio null se tratan como cero")
    void calcularSubtotal_ambosNull() {
        DetalleDocumentoEntity d = detalle(null, null);

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calcularSubtotal: cantidad decimal se aplica antes de redondear")
    void calcularSubtotal_cantidadDecimal() {
        // 2.5 * 1000 = 2500
        DetalleDocumentoEntity d = detalle("2.5", "1000");

        BigDecimal subtotal = service.calcularSubtotal(d);

        assertThat(subtotal).isEqualByComparingTo("2500");
    }

    // ----------------------------------------------------------------------
    // recalcular - camino feliz
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("recalcular: setea subtotales por línea, neto, iva y total")
    void recalcular_caminoFeliz() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        DetalleDocumentoEntity d1 = detalle("2", "1000"); // 2000
        DetalleDocumentoEntity d2 = detalle("1", "500");  // 500
        documento.setDetalles(new ArrayList<>(List.of(d1, d2)));

        service.recalcular(documento);

        // subtotales por línea
        assertThat(d1.getSubtotal()).isEqualByComparingTo("2000");
        assertThat(d2.getSubtotal()).isEqualByComparingTo("500");

        // neto = 2500, iva = 2500 * 0.19 = 475, total = 2975
        assertThat(documento.getMontoNeto()).isEqualByComparingTo("2500");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("475");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("2975");

        verify(reglaResolver).resolver(CODIGO_SII);
        verify(regla).tasaIva();
    }

    @Test
    @DisplayName("recalcular: el IVA se redondea HALF_UP a entero")
    void recalcular_ivaRedondeado() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        // neto = 100, iva = 100 * 0.19 = 19.00 -> 19
        DetalleDocumentoEntity d1 = detalle("1", "100");
        documento.setDetalles(new ArrayList<>(List.of(d1)));

        service.recalcular(documento);

        assertThat(documento.getMontoNeto()).isEqualByComparingTo("100");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("19");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("119");
    }

    @Test
    @DisplayName("recalcular: IVA con fracción .5 redondea hacia arriba")
    void recalcular_ivaFraccionHalfUp() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        // neto = 50, iva = 50 * 0.19 = 9.5 -> 10 (HALF_UP)
        DetalleDocumentoEntity d1 = detalle("1", "50");
        documento.setDetalles(new ArrayList<>(List.of(d1)));

        service.recalcular(documento);

        assertThat(documento.getMontoNeto()).isEqualByComparingTo("50");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("10");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("recalcular: tasa de IVA cero deja total igual al neto")
    void recalcular_tasaIvaCero() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(BigDecimal.ZERO);

        DetalleDocumentoEntity d1 = detalle("3", "1000"); // 3000
        documento.setDetalles(new ArrayList<>(List.of(d1)));

        service.recalcular(documento);

        assertThat(documento.getMontoNeto()).isEqualByComparingTo("3000");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("0");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("recalcular: detalles null -> neto 0, iva 0, total 0")
    void recalcular_detallesNull() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        documento.setDetalles(null);

        service.recalcular(documento);

        assertThat(documento.getMontoNeto()).isEqualByComparingTo("0");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("0");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("recalcular: lista de detalles vacía -> montos en cero")
    void recalcular_detallesVacios() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        documento.setDetalles(new ArrayList<>(Collections.emptyList()));

        service.recalcular(documento);

        assertThat(documento.getMontoNeto()).isEqualByComparingTo("0");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("0");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("recalcular: detalle con cantidad/precio null aporta 0 al neto")
    void recalcular_detalleConNulls() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        DetalleDocumentoEntity d1 = detalle("2", "1000"); // 2000
        DetalleDocumentoEntity d2 = detalle(null, null);  // 0
        documento.setDetalles(new ArrayList<>(List.of(d1, d2)));

        service.recalcular(documento);

        assertThat(d1.getSubtotal()).isEqualByComparingTo("2000");
        assertThat(d2.getSubtotal()).isEqualByComparingTo("0");
        assertThat(documento.getMontoNeto()).isEqualByComparingTo("2000");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("380");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("2380");
    }

    @Test
    @DisplayName("recalcular: el neto se redondea HALF_UP antes de calcular el IVA")
    void recalcular_netoRedondeado() {
        when(reglaResolver.resolver(CODIGO_SII)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        // cada subtotal ya se redondea a entero por línea: 1 * 1000.5 = 1001
        DetalleDocumentoEntity d1 = detalle("1", "1000.5"); // -> 1001
        DetalleDocumentoEntity d2 = detalle("1", "999.4");  // -> 999
        documento.setDetalles(new ArrayList<>(List.of(d1, d2)));

        service.recalcular(documento);

        assertThat(d1.getSubtotal()).isEqualByComparingTo("1001");
        assertThat(d2.getSubtotal()).isEqualByComparingTo("999");
        // neto = 1001 + 999 = 2000
        assertThat(documento.getMontoNeto()).isEqualByComparingTo("2000");
        assertThat(documento.getMontoIva()).isEqualByComparingTo("380");
        assertThat(documento.getMontoTotal()).isEqualByComparingTo("2380");
    }

    @Test
    @DisplayName("recalcular: usa el codigoSii del tipo de documento para resolver la regla")
    void recalcular_resuelvePorCodigoSiiDelTipo() {
        tipoDocumento.setCodigoSii(39); // boleta electrónica
        when(reglaResolver.resolver(39)).thenReturn(regla);
        when(regla.tasaIva()).thenReturn(TASA_IVA);

        documento.setDetalles(new ArrayList<>(List.of(detalle("1", "1000"))));

        service.recalcular(documento);

        verify(reglaResolver).resolver(39);
    }

    // ----------------------------------------------------------------------
    // recalcular - excepciones
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("recalcular: propaga ReglaNegocioException si el tipo no está soportado")
    void recalcular_tipoNoSoportado_lanzaReglaNegocioException() {
        when(reglaResolver.resolver(CODIGO_SII))
                .thenThrow(new ReglaNegocioException("Tipo de documento no soportado: " + CODIGO_SII));

        documento.setDetalles(new ArrayList<>(List.of(detalle("1", "1000"))));

        assertThatThrownBy(() -> service.recalcular(documento))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Tipo de documento no soportado");

        // si la regla no se resuelve, no se calculan montos
        assertThat(documento.getMontoNeto()).isNull();
        assertThat(documento.getMontoIva()).isNull();
        assertThat(documento.getMontoTotal()).isNull();
        verify(regla, never()).tasaIva();
    }

    @Test
    @DisplayName("recalcular: NPE si el documento no tiene tipo de documento")
    void recalcular_sinTipoDocumento_lanzaNpe() {
        // resolver no llega a invocarse de forma significativa; el NPE ocurre al obtener el codigoSii
        lenient().when(reglaResolver.resolver(null)).thenReturn(regla);
        documento.setTipoDocumento(null);

        assertThatThrownBy(() -> service.recalcular(documento))
                .isInstanceOf(NullPointerException.class);
    }
}
