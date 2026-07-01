package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.service.EmpresaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmpresaControllerTest {

    @Mock
    private EmpresaService service;

    @InjectMocks
    private EmpresaController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private EmpresaEntity empresaEjemplo(Long id) {
        EmpresaEntity e = new EmpresaEntity();
        e.setId(id);
        e.setRutEmpresa("76086428-5");
        e.setRazonSocial("Empresa de Prueba SpA");
        e.setNombreFantasia("PruebaSpA");
        e.setGiro("Servicios informáticos");
        e.setDireccion("Av. Siempre Viva 742");
        e.setCiudad("Santiago");
        e.setComuna("Providencia");
        e.setPais("Chile");
        e.setTelefono("+56222222222");
        e.setSitioWeb("https://prueba.cl");
        e.setEmailPrincipal("contacto@prueba.cl");
        e.setEmailContabilidad("conta@prueba.cl");
        e.setRutRepresentante("12345678-5");
        e.setNombreRepresentante("Juan Pérez");
        e.setTelefonoRepresentante("+56999999999");
        e.setActivo(true);
        e.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        e.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 11, 0));
        return e;
    }

    private EmpresaCreateRequest requestValido() {
        EmpresaCreateRequest req = new EmpresaCreateRequest();
        req.setRutEmpresa("76086428-5");
        req.setRazonSocial("Empresa de Prueba SpA");
        req.setNombreFantasia("PruebaSpA");
        req.setGiro("Servicios informáticos");
        req.setDireccion("Av. Siempre Viva 742");
        req.setCiudad("Santiago");
        req.setComuna("Providencia");
        req.setPais("Chile");
        req.setTelefono("+56222222222");
        req.setSitioWeb("https://prueba.cl");
        req.setEmailPrincipal("contacto@prueba.cl");
        req.setEmailContabilidad("conta@prueba.cl");
        req.setRutRepresentante("12345678-5");
        req.setNombreRepresentante("Juan Pérez");
        req.setTelefonoRepresentante("+56999999999");
        return req;
    }

    // ---------------------------------------------------------------------
    // POST /empresas
    // ---------------------------------------------------------------------

    @Test
    void create_empresaValida_devuelve201_yCuerpoMapeado() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.create(any(EmpresaCreateRequest.class))).thenReturn(empresaEjemplo(7L));

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.rutEmpresa").value("76086428-5"))
                .andExpect(jsonPath("$.razonSocial").value("Empresa de Prueba SpA"))
                .andExpect(jsonPath("$.nombreFantasia").value("PruebaSpA"))
                .andExpect(jsonPath("$.emailPrincipal").value("contacto@prueba.cl"))
                .andExpect(jsonPath("$.activo").value(true));

        ArgumentCaptor<EmpresaCreateRequest> captor = ArgumentCaptor.forClass(EmpresaCreateRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getRutEmpresa()).isEqualTo("76086428-5");
        assertThat(captor.getValue().getRazonSocial()).isEqualTo("Empresa de Prueba SpA");
    }

    @Test
    void create_rutSinRazonSocial_devuelve400_validacion() throws Exception {
        EmpresaCreateRequest req = requestValido();
        req.setRazonSocial("   "); // @NotBlank

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.mensaje").value("La razón social es obligatoria"));

        verify(service, never()).create(any());
    }

    @Test
    void create_rutEnBlanco_devuelve400_validacion() throws Exception {
        EmpresaCreateRequest req = requestValido();
        req.setRutEmpresa(""); // @NotBlank

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.mensaje").value("El RUT de la empresa es obligatorio"));

        verify(service, never()).create(any());
    }

    @Test
    void create_emailPrincipalInvalido_devuelve400_validacion() throws Exception {
        EmpresaCreateRequest req = requestValido();
        req.setEmailPrincipal("no-es-un-email"); // @Email

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.mensaje").value("El email principal no tiene un formato válido"));

        verify(service, never()).create(any());
    }

    @Test
    void create_rutInvalido_devuelve400() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.create(any(EmpresaCreateRequest.class)))
                .thenThrow(new RutInvalidoException("11111111-1"));

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("RUT inválido"))
                .andExpect(jsonPath("$.mensaje").value("El RUT ingresado no es válido: 11111111-1"));
    }

    @Test
    void create_rutDuplicado_devuelve400_reglaNegocio() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.create(any(EmpresaCreateRequest.class)))
                .thenThrow(new ReglaNegocioException("Ya existe una empresa con el RUT: 76086428-5"));

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Ya existe una empresa con el RUT: 76086428-5"));
    }

    @Test
    void create_errorInesperado_devuelve500() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.create(any(EmpresaCreateRequest.class)))
                .thenThrow(new RuntimeException("Boom"));

        mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Boom"));
    }

    // ---------------------------------------------------------------------
    // GET /empresas
    // ---------------------------------------------------------------------

    @Test
    void findAll_devuelveLista_yStatus200() throws Exception {
        EmpresaEntity e1 = empresaEjemplo(1L);
        EmpresaEntity e2 = empresaEjemplo(2L);
        e2.setRazonSocial("Otra Empresa Ltda");
        when(service.findAll()).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].razonSocial").value("Empresa de Prueba SpA"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].razonSocial").value("Otra Empresa Ltda"));

        verify(service).findAll();
    }

    @Test
    void findAll_sinEmpresas_devuelveListaVacia() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------------
    // GET /empresas/{id}
    // ---------------------------------------------------------------------

    @Test
    void findById_existente_devuelve200_yCuerpo() throws Exception {
        when(service.findById(5L)).thenReturn(empresaEjemplo(5L));

        mockMvc.perform(get("/empresas/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.rutEmpresa").value("76086428-5"))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).findById(5L);
    }

    @Test
    void findById_noExiste_devuelve404() throws Exception {
        when(service.findById(99L))
                .thenThrow(new RecursoNoEncontradoException("Empresa no encontrada con id: 99"));

        mockMvc.perform(get("/empresas/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Empresa no encontrada con id: 99"));
    }

    // ---------------------------------------------------------------------
    // PUT /empresas/{id}
    // ---------------------------------------------------------------------

    @Test
    void update_existente_devuelve200_yCuerpoActualizado() throws Exception {
        EmpresaCreateRequest req = requestValido();
        req.setRazonSocial("Razón Social Modificada");

        EmpresaEntity actualizada = empresaEjemplo(3L);
        actualizada.setRazonSocial("Razón Social Modificada");
        when(service.update(eq(3L), any(EmpresaCreateRequest.class))).thenReturn(actualizada);

        mockMvc.perform(put("/empresas/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.razonSocial").value("Razón Social Modificada"));

        ArgumentCaptor<EmpresaCreateRequest> captor = ArgumentCaptor.forClass(EmpresaCreateRequest.class);
        verify(service).update(eq(3L), captor.capture());
        assertThat(captor.getValue().getRazonSocial()).isEqualTo("Razón Social Modificada");
    }

    @Test
    void update_noExiste_devuelve404() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.update(eq(99L), any(EmpresaCreateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Empresa no encontrada con id: 99"));

        mockMvc.perform(put("/empresas/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Empresa no encontrada con id: 99"));
    }

    @Test
    void update_validacionFalla_devuelve400_yNoLlamaServicio() throws Exception {
        EmpresaCreateRequest req = requestValido();
        req.setRazonSocial(""); // @NotBlank

        mockMvc.perform(put("/empresas/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.mensaje").value("La razón social es obligatoria"));

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    void update_rutDuplicadoDeOtraEmpresa_devuelve400_reglaNegocio() throws Exception {
        EmpresaCreateRequest req = requestValido();
        when(service.update(eq(3L), any(EmpresaCreateRequest.class)))
                .thenThrow(new ReglaNegocioException("Ya existe otra empresa con el RUT: 76086428-5"));

        mockMvc.perform(put("/empresas/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Ya existe otra empresa con el RUT: 76086428-5"));
    }

    // ---------------------------------------------------------------------
    // PATCH /empresas/{id}/estado
    // ---------------------------------------------------------------------

    @Test
    void cambiarEstado_desactivar_devuelve200_yActivoFalse() throws Exception {
        EmpresaEntity desactivada = empresaEjemplo(4L);
        desactivada.setActivo(false);
        when(service.cambiarEstado(4L, false)).thenReturn(desactivada);

        mockMvc.perform(patch("/empresas/{id}/estado", 4L)
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.activo").value(false));

        verify(service).cambiarEstado(4L, false);
    }

    @Test
    void cambiarEstado_activar_devuelve200_yActivoTrue() throws Exception {
        EmpresaEntity activada = empresaEjemplo(4L);
        activada.setActivo(true);
        when(service.cambiarEstado(4L, true)).thenReturn(activada);

        mockMvc.perform(patch("/empresas/{id}/estado", 4L)
                        .param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).cambiarEstado(4L, true);
    }

    @Test
    void cambiarEstado_empresaNoExiste_devuelve404() throws Exception {
        when(service.cambiarEstado(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Empresa no encontrada con id: 99"));

        mockMvc.perform(patch("/empresas/{id}/estado", 99L)
                        .param("activo", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Empresa no encontrada con id: 99"));
    }
}
