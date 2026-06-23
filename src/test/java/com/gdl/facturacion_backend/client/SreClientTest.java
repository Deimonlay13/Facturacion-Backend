package com.gdl.facturacion_backend.client;

import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.exception.SreApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SreClientTest {

    private static final String SRE_URL = "https://sre.example.com/api/empresa";
    private static final String SRE_TOKEN = "test-token-123";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SreClient sreClient;

    @BeforeEach
    void setUp() {
        // sreUrl y sreToken vienen de @Value, hay que inyectarlos manualmente en el test unitario.
        ReflectionTestUtils.setField(sreClient, "sreUrl", SRE_URL);
        ReflectionTestUtils.setField(sreClient, "sreToken", SRE_TOKEN);
    }

    private SreCompanyResponse companyResponse() {
        SreCompanyResponse response = new SreCompanyResponse();
        response.setRut("76.123.456-7");
        response.setRazonSocial("Empresa de Prueba SpA");
        response.setNombreFantasia("Prueba");
        response.setGiro("Servicios");
        response.setDireccion("Calle Falsa 123");
        response.setCiudad("Santiago");
        response.setComuna("Providencia");
        response.setRegion("Metropolitana");
        response.setCodigoRegion("13");
        response.setTelefono("+56912345678");
        response.setEmail("contacto@prueba.cl");
        response.setActivo(Boolean.TRUE);
        return response;
    }

    @Test
    void buscarPorRut_respuestaOk_devuelveOptionalConLaEmpresa() {
        SreCompanyResponse esperado = companyResponse();
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenReturn(esperado);

        Optional<SreCompanyResponse> resultado = sreClient.buscarPorRut("76.123.456-7");

        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isSameAs(esperado);
        assertThat(resultado.get().getRut()).isEqualTo("76.123.456-7");
        assertThat(resultado.get().getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
        assertThat(resultado.get().hasError()).isFalse();
    }

    @Test
    void buscarPorRut_construyeUrlConTokenRutYActualizado() {
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenReturn(companyResponse());

        sreClient.buscarPorRut("76.123.456-7");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(restTemplate)
                .getForObject(urlCaptor.capture(), eq(SreCompanyResponse.class));

        String url = urlCaptor.getValue();
        assertThat(url).startsWith(SRE_URL);
        assertThat(url).contains("token=" + SRE_TOKEN);
        // El RUT se codifica en la query string (76.123.456-7).
        assertThat(url).contains("rut=76.123.456-7");
        assertThat(url).contains("actualizado=False");
    }

    @Test
    void buscarPorRut_respuestaNula_devuelveOptionalVacio() {
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenReturn(null);

        Optional<SreCompanyResponse> resultado = sreClient.buscarPorRut("11.111.111-1");

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarPorRut_respuestaConCampoError_devuelveOptionalVacio() {
        SreCompanyResponse conError = new SreCompanyResponse();
        conError.setError("RUT no encontrado");

        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenReturn(conError);

        Optional<SreCompanyResponse> resultado = sreClient.buscarPorRut("99.999.999-9");

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarPorRut_respuestaConCampoMessage_devuelveOptionalVacio() {
        SreCompanyResponse conMensaje = new SreCompanyResponse();
        conMensaje.setMessage("No se encontraron resultados");

        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenReturn(conMensaje);

        Optional<SreCompanyResponse> resultado = sreClient.buscarPorRut("88.888.888-8");

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarPorRut_cuandoRestTemplateLanza404_devuelveOptionalVacio() {
        // HttpClientErrorException es subtipo de RestClientException -> se mapea a SreApiException.
        HttpClientErrorException notFound =
                HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found", null, null, null);
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenThrow(notFound);

        assertThatThrownBy(() -> sreClient.buscarPorRut("77.777.777-7"))
                .isInstanceOf(SreApiException.class)
                .hasMessageContaining("No se pudo conectar con la API de SRE")
                .hasCause(notFound);
    }

    @Test
    void buscarPorRut_cuandoRestTemplateLanzaRestClientException_lanzaSreApiException() {
        RestClientException error = new RestClientException("conexion rechazada");
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenThrow(error);

        assertThatThrownBy(() -> sreClient.buscarPorRut("76.123.456-7"))
                .isInstanceOf(SreApiException.class)
                .hasMessageContaining("No se pudo conectar con la API de SRE")
                .hasMessageContaining("conexion rechazada")
                .hasCause(error);
    }

    @Test
    void buscarPorRut_cuandoHayProblemaDeAcceso_lanzaSreApiException() {
        // ResourceAccessException (timeout / IO) tambien es RestClientException.
        ResourceAccessException error = new ResourceAccessException("timeout de lectura");
        when(restTemplate.getForObject(anyUrl(), eq(SreCompanyResponse.class)))
                .thenThrow(error);

        assertThatThrownBy(() -> sreClient.buscarPorRut("76.123.456-7"))
                .isInstanceOf(SreApiException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    private static String anyUrl() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
