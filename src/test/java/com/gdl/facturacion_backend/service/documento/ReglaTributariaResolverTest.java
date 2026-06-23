package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Test unitario del {@link ReglaTributariaResolver}.
 *
 * <p>El servicio no usa repositorios, ni TenantContext/TenantService: recibe en su
 * constructor un {@code List<ReglaTributaria>} (todas las reglas que Spring inyecta)
 * y construye un {@code Map} indexado por {@link ReglaTributaria#codigoSii()}. Por eso
 * los colaboradores se mockean como instancias de {@link ReglaTributaria} y el resolver
 * se instancia manualmente en cada test (no se usa @InjectMocks porque la dependencia
 * es una colección).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReglaTributariaResolver - test unitario")
class ReglaTributariaResolverTest {

    @Mock
    private ReglaTributaria reglaFactura;

    @Mock
    private ReglaTributaria reglaBoleta;

    @BeforeEach
    void setUp() {
        // Códigos SII: 33 = Factura Electrónica, 39 = Boleta Electrónica.
        // lenient() porque no todos los tests consultan ambas reglas.
        lenient().when(reglaFactura.codigoSii()).thenReturn(33);
        lenient().when(reglaBoleta.codigoSii()).thenReturn(39);
    }

    @Test
    @DisplayName("resolver devuelve la regla cuyo codigoSii coincide")
    void resolver_devuelveReglaCoincidente() {
        ReglaTributariaResolver resolver =
                new ReglaTributariaResolver(List.of(reglaFactura, reglaBoleta));

        ReglaTributaria resultado = resolver.resolver(33);

        assertThat(resultado).isSameAs(reglaFactura);
    }

    @Test
    @DisplayName("resolver distingue entre varias reglas registradas")
    void resolver_distingueEntreVariasReglas() {
        ReglaTributariaResolver resolver =
                new ReglaTributariaResolver(List.of(reglaFactura, reglaBoleta));

        assertThat(resolver.resolver(33)).isSameAs(reglaFactura);
        assertThat(resolver.resolver(39)).isSameAs(reglaBoleta);
    }

    @Test
    @DisplayName("resolver lanza ReglaNegocioException cuando el codigoSii no esta registrado")
    void resolver_codigoNoRegistrado_lanzaReglaNegocio() {
        ReglaTributariaResolver resolver =
                new ReglaTributariaResolver(List.of(reglaFactura, reglaBoleta));

        assertThatThrownBy(() -> resolver.resolver(56))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Tipo de documento no soportado: 56");
    }

    @Test
    @DisplayName("resolver con codigoSii null lanza ReglaNegocioException")
    void resolver_codigoNull_lanzaReglaNegocio() {
        ReglaTributariaResolver resolver =
                new ReglaTributariaResolver(List.of(reglaFactura, reglaBoleta));

        assertThatThrownBy(() -> resolver.resolver(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Tipo de documento no soportado: null");
    }

    @Test
    @DisplayName("resolver con lista vacia siempre lanza ReglaNegocioException")
    void resolver_listaVacia_lanzaReglaNegocio() {
        ReglaTributariaResolver resolver =
                new ReglaTributariaResolver(Collections.emptyList());

        assertThatThrownBy(() -> resolver.resolver(33))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Tipo de documento no soportado: 33");
    }

    @Test
    @DisplayName("el constructor consulta codigoSii() de cada regla para indexar el mapa")
    void constructor_consultaCodigoSiiDeCadaRegla() {
        new ReglaTributariaResolver(List.of(reglaFactura, reglaBoleta));

        verify(reglaFactura).codigoSii();
        verify(reglaBoleta).codigoSii();
    }

    @Test
    @DisplayName("constructor con codigoSii duplicado propaga IllegalStateException de Collectors.toMap")
    void constructor_codigoDuplicado_lanzaIllegalState() {
        // Dos reglas con el mismo codigoSii (33) provocan colision de clave en toMap.
        ReglaTributaria reglaFacturaDuplicada = org.mockito.Mockito.mock(ReglaTributaria.class);
        lenient().when(reglaFacturaDuplicada.codigoSii()).thenReturn(33);

        List<ReglaTributaria> reglasConColision = List.of(reglaFactura, reglaFacturaDuplicada);

        assertThatThrownBy(() -> new ReglaTributariaResolver(reglasConColision))
                .isInstanceOf(IllegalStateException.class);
    }
}
