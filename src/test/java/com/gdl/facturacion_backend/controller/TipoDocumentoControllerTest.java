package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.tipoDocumento.TipoDocumentoResponseDto;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.service.TipoDocumentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoControllerTest {

    @Mock
    private TipoDocumentoService service;

    @InjectMocks
    private TipoDocumentoController controller;

    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/tipos-documento";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_returns200WithListOfTipos() throws Exception {
        // given
        List<TipoDocumentoResponseDto> tipos = List.of(
                new TipoDocumentoResponseDto(1L, 33, "Factura Electrónica"),
                new TipoDocumentoResponseDto(2L, 34, "Factura No Afecta o Exenta Electrónica"),
                new TipoDocumentoResponseDto(3L, 61, "Nota de Crédito Electrónica")
        );
        given(service.findAll()).willReturn(tipos);

        // when / then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigoSii").value(33))
                .andExpect(jsonPath("$[0].descripcion").value("Factura Electrónica"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].codigoSii").value(34))
                .andExpect(jsonPath("$[2].codigoSii").value(61))
                .andExpect(jsonPath("$[2].descripcion").value("Nota de Crédito Electrónica"));

        verify(service).findAll();
    }

    @Test
    void findAll_returns200WithSingleTipo() throws Exception {
        // given
        given(service.findAll()).willReturn(
                List.of(new TipoDocumentoResponseDto(10L, 39, "Boleta Electrónica")));

        // when / then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].codigoSii").value(39))
                .andExpect(jsonPath("$[0].descripcion").value("Boleta Electrónica"));

        verify(service).findAll();
    }

    @Test
    void findAll_returns200WithEmptyListWhenNoTipos() throws Exception {
        // given
        given(service.findAll()).willReturn(List.of());

        // when / then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().json("[]"));

        verify(service).findAll();
    }

    @Test
    void findAll_returns404WhenServiceThrowsRecursoNoEncontrado() throws Exception {
        // given
        given(service.findAll())
                .willThrow(new RecursoNoEncontradoException("Tipo de documento no existe: 99"));

        // when / then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Tipo de documento no existe: 99"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(service).findAll();
    }

    @Test
    void findAll_returns500WhenServiceThrowsRuntimeException() throws Exception {
        // given
        given(service.findAll())
                .willThrow(new RuntimeException("Fallo inesperado de base de datos"));

        // when / then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Fallo inesperado de base de datos"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(service).findAll();
    }
}
