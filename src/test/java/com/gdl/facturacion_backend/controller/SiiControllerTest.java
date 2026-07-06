package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.sii.EnvioSiiResponse;
import com.gdl.facturacion_backend.dto.sii.EstadoSiiResponse;
import com.gdl.facturacion_backend.dto.sii.HistorialSiiAdminResponse;
import com.gdl.facturacion_backend.dto.sii.HistorialSiiResponse;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.documento.EnvioSiiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SiiControllerTest {

    @Mock
    private EnvioSiiService envioSiiService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SiiController controller = new SiiController(envioSiiService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------- fixtures

    private EstadoSiiResponse enviado() {
        EnvioSiiResponse envio = new EnvioSiiResponse(
                9L, "SIM-55-abc12345", LocalDateTime.now(), "ENVIADO", "recibido", 1);
        HistorialSiiResponse hist = new HistorialSiiResponse(
                "ENVIADO", LocalDateTime.now(), "Envío simulado al SII.");
        return new EstadoSiiResponse(
                100L, 55, "ENVIADO", true, "SIM-55-abc12345", envio, List.of(hist));
    }

    private EstadoSiiResponse aceptado() {
        EnvioSiiResponse envio = new EnvioSiiResponse(
                9L, "SIM-55-abc12345", LocalDateTime.now(), "ACEPTADO", "aceptado", 1);
        HistorialSiiResponse h1 = new HistorialSiiResponse(
                "ENVIADO", LocalDateTime.now(), "Envío simulado al SII.");
        HistorialSiiResponse h2 = new HistorialSiiResponse(
                "ACEPTADO", LocalDateTime.now(), "El SII aceptó el documento (simulado).");
        return new EstadoSiiResponse(
                100L, 55, "ACEPTADO", true, "SIM-55-abc12345", envio, List.of(h1, h2));
    }

    // ---------------------------------------------------------------- tests

    @Test
    void enviar_devuelve200YEstadoEnviado() throws Exception {
        when(envioSiiService.enviar(100L)).thenReturn(enviado());

        mockMvc.perform(post("/api/documentos/100/enviar-sii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value(100))
                .andExpect(jsonPath("$.estadoSii").value("ENVIADO"))
                .andExpect(jsonPath("$.trackId").value("SIM-55-abc12345"))
                .andExpect(jsonPath("$.envio.intentos").value(1))
                .andExpect(jsonPath("$.historial[0].estado").value("ENVIADO"));

        verify(envioSiiService).enviar(100L);
    }

    @Test
    void consultar_devuelve200YEstadoAceptado() throws Exception {
        when(envioSiiService.consultarEstado(100L)).thenReturn(aceptado());

        mockMvc.perform(post("/api/documentos/100/consultar-sii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSii").value("ACEPTADO"))
                .andExpect(jsonPath("$.envio.estado").value("ACEPTADO"))
                .andExpect(jsonPath("$.historial[1].estado").value("ACEPTADO"));

        verify(envioSiiService).consultarEstado(100L);
    }

    @Test
    void estado_devuelve200ConEstadoEHistorial() throws Exception {
        when(envioSiiService.obtenerEstado(100L)).thenReturn(enviado());

        mockMvc.perform(get("/api/documentos/100/estado-sii"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSii").value("ENVIADO"))
                .andExpect(jsonPath("$.historial[0].mensaje").value("Envío simulado al SII."));

        verify(envioSiiService).obtenerEstado(100L);
    }

    @Test
    void enviar_documentoNoEmitido_devuelve400() throws Exception {
        when(envioSiiService.enviar(100L))
                .thenThrow(new ReglaNegocioException("Solo se pueden enviar al SII documentos EMITIDOS"));

        mockMvc.perform(post("/api/documentos/100/enviar-sii"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo se pueden enviar al SII documentos EMITIDOS"));
    }

    @Test
    void listarEnvios_devuelve200ConLista() throws Exception {
        EnvioSiiResponse e = new EnvioSiiResponse(
                9L, "SIM-55-abc12345", LocalDateTime.now(), "ENVIADO", "recibido", 1);
        when(envioSiiService.listarEnvios()).thenReturn(List.of(e));

        mockMvc.perform(get("/api/documentos/sii/envios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackId").value("SIM-55-abc12345"))
                .andExpect(jsonPath("$[0].estado").value("ENVIADO"));

        verify(envioSiiService).listarEnvios();
    }

    @Test
    void listarHistorial_devuelve200ConDocumentoYFolio() throws Exception {
        HistorialSiiAdminResponse h = new HistorialSiiAdminResponse(
                3L, 100L, 55, "ACEPTADO", LocalDateTime.now(), "El SII aceptó el documento (simulado).");
        when(envioSiiService.listarHistorial()).thenReturn(List.of(h));

        mockMvc.perform(get("/api/documentos/sii/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentoId").value(100))
                .andExpect(jsonPath("$[0].folio").value(55))
                .andExpect(jsonPath("$[0].estado").value("ACEPTADO"));

        verify(envioSiiService).listarHistorial();
    }
}
