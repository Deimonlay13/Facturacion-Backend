package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.ClienteCreateRequest;
import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.exception.ClienteDuplicadoException;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.service.ClienteService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService service;

    @Mock
    private Validator validator;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ClienteController controller = new ClienteController(service, validator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ClienteEntity sampleEntity() {
        ClienteEntity c = new ClienteEntity();
        c.setId(5L);
        c.setRut("76883241-2");
        c.setRazonSocial("Empresa Ejemplo SpA");
        c.setNombreFantasia("Ejemplo");
        c.setGiro("Software");
        c.setDireccion("Av. Siempre Viva 123");
        c.setCiudad("Santiago");
        c.setComuna("Providencia");
        c.setRegion("RM");
        c.setPais("Chile");
        c.setTelefono("+56911111111");
        c.setEmail("contacto@ejemplo.cl");
        c.setActivo(true);
        c.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        c.setUpdatedAt(OffsetDateTime.parse("2026-01-02T11:00:00Z"));
        return c;
    }

    private ClienteCreateRequest sampleRequest() {
        ClienteCreateRequest r = new ClienteCreateRequest();
        r.setRut("76883241-2");
        r.setRazonSocial("Empresa Ejemplo SpA");
        r.setEmail("contacto@ejemplo.cl");
        r.setNombreFantasia("Ejemplo");
        r.setGiro("Software");
        r.setDireccion("Av. Siempre Viva 123");
        r.setCiudad("Santiago");
        r.setComuna("Providencia");
        r.setRegion("RM");
        r.setPais("Chile");
        r.setTelefono("+56911111111");
        return r;
    }

    // ---------------------------------------------------------------- POST /clientes
    @Test
    void crear_devuelve201YClienteCreado() throws Exception {
        when(service.crear(any(ClienteCreateRequest.class))).thenReturn(sampleEntity());

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.rut").value("76883241-2"))
                .andExpect(jsonPath("$.razonSocial").value("Empresa Ejemplo SpA"))
                .andExpect(jsonPath("$.email").value("contacto@ejemplo.cl"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).crear(any(ClienteCreateRequest.class));
    }

    @Test
    void crear_pasaLosDatosDelBodyAlServicio() throws Exception {
        when(service.crear(any(ClienteCreateRequest.class))).thenReturn(sampleEntity());

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());

        ArgumentCaptor<ClienteCreateRequest> captor = ArgumentCaptor.forClass(ClienteCreateRequest.class);
        verify(service).crear(captor.capture());
        assertThat(captor.getValue().getRut()).isEqualTo("76883241-2");
        assertThat(captor.getValue().getRazonSocial()).isEqualTo("Empresa Ejemplo SpA");
        assertThat(captor.getValue().getEmail()).isEqualTo("contacto@ejemplo.cl");
    }

    @Test
    void crear_cuandoFaltanCamposObligatorios_devuelve400() throws Exception {
        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    void crear_cuandoRutInvalido_devuelve400() throws Exception {
        when(service.crear(any(ClienteCreateRequest.class)))
                .thenThrow(new RutInvalidoException("12345"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("RUT inválido"));
    }

    @Test
    void crear_cuandoClienteDuplicado_devuelve409() throws Exception {
        when(service.crear(any(ClienteCreateRequest.class)))
                .thenThrow(new ClienteDuplicadoException("76883241-2"));

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Cliente duplicado"));
    }

    // ---------------------------------------------------------------- GET /clientes
    @Test
    void listar_devuelve200YListaDeClientes() throws Exception {
        ClienteEntity otro = sampleEntity();
        otro.setId(6L);
        otro.setRazonSocial("Otra Empresa Ltda");

        when(service.listarActivos()).thenReturn(List.of(sampleEntity(), otro));

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[1].id").value(6))
                .andExpect(jsonPath("$[1].razonSocial").value("Otra Empresa Ltda"));

        verify(service).listarActivos();
    }

    @Test
    void listar_cuandoNoHay_devuelveListaVacia() throws Exception {
        when(service.listarActivos()).thenReturn(List.of());

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- GET /clientes/{id}
    @Test
    void obtener_devuelve200() throws Exception {
        when(service.obtenerPorId(5L)).thenReturn(sampleEntity());

        mockMvc.perform(get("/clientes/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.rut").value("76883241-2"));

        verify(service).obtenerPorId(5L);
    }

    @Test
    void obtener_cuandoNoExiste_devuelve404() throws Exception {
        when(service.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado con id: 99"));

        mockMvc.perform(get("/clientes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    // ---------------------------------------------------------------- PUT /clientes/{id}
    @Test
    void actualizar_devuelve200() throws Exception {
        ClienteEntity actualizado = sampleEntity();
        actualizado.setRazonSocial("Razón Actualizada SpA");
        when(service.actualizar(eq(5L), any(ClienteCreateRequest.class))).thenReturn(actualizado);

        ClienteCreateRequest req = sampleRequest();
        req.setRazonSocial("Razón Actualizada SpA");

        mockMvc.perform(put("/clientes/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.razonSocial").value("Razón Actualizada SpA"));

        verify(service).actualizar(eq(5L), any(ClienteCreateRequest.class));
    }

    @Test
    void actualizar_cuandoNoExiste_devuelve404() throws Exception {
        when(service.actualizar(eq(99L), any(ClienteCreateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado con id: 99"));

        mockMvc.perform(put("/clientes/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------------------------------------------------------------- DELETE /clientes/{id}
    @Test
    void eliminar_devuelve204() throws Exception {
        doNothing().when(service).eliminar(5L);

        mockMvc.perform(delete("/clientes/{id}", 5L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).eliminar(5L);
    }

    @Test
    void eliminar_cuandoTieneDocumentos_devuelve400() throws Exception {
        doThrow(new ReglaNegocioException("No se puede eliminar el cliente: tiene documentos asociados."))
                .when(service).eliminar(5L);

        mockMvc.perform(delete("/clientes/{id}", 5L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"));

        verify(service).eliminar(5L);
    }

    // ---------------------------------------------------------------- GET /clientes/sre
    @Test
    void consultarSre_cuandoExiste_devuelve200() throws Exception {
        SreCompanyResponse sre = new SreCompanyResponse();
        sre.setRut("76883241-2");
        sre.setRazonSocial("Empresa SRE SpA");
        when(service.consultarSre("76883241-2")).thenReturn(Optional.of(sre));

        mockMvc.perform(get("/clientes/sre").param("rut", "76883241-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razon_social").value("Empresa SRE SpA"));

        verify(service).consultarSre("76883241-2");
    }

    @Test
    void consultarSre_cuandoNoExiste_devuelve404() throws Exception {
        when(service.consultarSre("76883241-2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/clientes/sre").param("rut", "76883241-2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarSre_cuandoRutInvalido_devuelve400() throws Exception {
        when(service.consultarSre("xxx")).thenThrow(new RutInvalidoException("xxx"));

        mockMvc.perform(get("/clientes/sre").param("rut", "xxx"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RUT inválido"));
    }

    // ---------------------------------------------------------------- GET /clientes/plantilla
    @Test
    void descargarPlantilla_devuelveXlsx() throws Exception {
        mockMvc.perform(get("/clientes/plantilla"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("plantilla_clientes.xlsx")))
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
