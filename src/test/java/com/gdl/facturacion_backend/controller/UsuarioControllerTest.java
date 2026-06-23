package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.usuario.CambiarEstadoRequest;
import com.gdl.facturacion_backend.dto.usuario.CambiarRolRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioCreateRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioUpdateRequest;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.UsuarioService;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

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

    private RolEntity rol(String nombre, String nombreMostrar) {
        RolEntity rol = new RolEntity();
        rol.setNombre(nombre);
        rol.setNombreMostrar(nombreMostrar);
        return rol;
    }

    private UsuarioEntity usuario(Long id, String username, boolean activo, RolEntity rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setUsername(username);
        u.setActivo(activo);
        u.setRol(rol);
        u.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        u.setUpdatedAt(OffsetDateTime.parse("2026-01-02T10:00:00Z"));
        return u;
    }

    // ---------------------------------------------------------------------
    // POST /usuarios
    // ---------------------------------------------------------------------

    @Test
    void crear_devuelveUsuario_yStatus201() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setUsername("nuevo");
        request.setPassword("secret123");
        request.setRol("ROLE_ADMIN");

        UsuarioEntity creado = usuario(7L, "nuevo", true, rol("ROLE_ADMIN", "Administrador"));
        when(service.crear(any(UsuarioCreateRequest.class))).thenReturn(creado);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("nuevo"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.rolMostrar").value("Administrador"));

        ArgumentCaptor<UsuarioCreateRequest> captor = ArgumentCaptor.forClass(UsuarioCreateRequest.class);
        verify(service).crear(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("nuevo");
        assertThat(captor.getValue().getPassword()).isEqualTo("secret123");
        assertThat(captor.getValue().getRol()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void crear_rolNulo_seSerializaRolNull() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setUsername("sinrol");
        request.setPassword("secret123");

        UsuarioEntity creado = usuario(8L, "sinrol", true, null);
        when(service.crear(any(UsuarioCreateRequest.class))).thenReturn(creado);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.rol").doesNotExist())
                .andExpect(jsonPath("$.rolMostrar").doesNotExist());
    }

    @Test
    void crear_usernameEnBlanco_devuelve400_validacion() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setUsername("");
        request.setPassword("secret123");

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Error de validación"));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_passwordFaltante_devuelve400_validacion() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setUsername("alguien");
        // password nulo -> @NotBlank

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_usernameDuplicado_devuelve400_reglaNegocio() throws Exception {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setUsername("existente");
        request.setPassword("secret123");

        when(service.crear(any(UsuarioCreateRequest.class)))
                .thenThrow(new ReglaNegocioException("El nombre de usuario ya está en uso"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El nombre de usuario ya está en uso"));
    }

    // ---------------------------------------------------------------------
    // GET /usuarios
    // ---------------------------------------------------------------------

    @Test
    void listar_devuelveLista_yStatus200() throws Exception {
        UsuarioEntity u1 = usuario(1L, "ana", true, rol("ROLE_ADMIN", "Administrador"));
        UsuarioEntity u2 = usuario(2L, "beto", false, rol("ROLE_USER", "Usuario"));
        when(service.listar()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("ana"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("beto"))
                .andExpect(jsonPath("$[1].activo").value(false))
                .andExpect(jsonPath("$[1].rol").value("ROLE_USER"));

        verify(service).listar();
    }

    @Test
    void listar_vacia_devuelveArrayVacio() throws Exception {
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_sinTenant_devuelve400_reglaNegocio() throws Exception {
        when(service.listar())
                .thenThrow(new ReglaNegocioException(
                        "Operación no permitida: falta identificación de empresa (X-Tenant-ID o JWT)"));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"));
    }

    // ---------------------------------------------------------------------
    // GET /usuarios/{id}
    // ---------------------------------------------------------------------

    @Test
    void obtener_existente_devuelveUsuario_yStatus200() throws Exception {
        UsuarioEntity u = usuario(5L, "carla", true, rol("ROLE_USER", "Usuario"));
        when(service.obtenerPorId(eq(5L))).thenReturn(u);

        mockMvc.perform(get("/usuarios/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("carla"))
                .andExpect(jsonPath("$.rol").value("ROLE_USER"))
                .andExpect(jsonPath("$.rolMostrar").value("Usuario"));

        verify(service).obtenerPorId(5L);
    }

    @Test
    void obtener_noEncontrado_devuelve404() throws Exception {
        when(service.obtenerPorId(eq(99L)))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id: 99"));

        mockMvc.perform(get("/usuarios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con id: 99"));
    }

    // ---------------------------------------------------------------------
    // PUT /usuarios/{id}
    // ---------------------------------------------------------------------

    @Test
    void actualizar_devuelveUsuarioActualizado_yStatus200() throws Exception {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setUsername("nuevo-nombre");
        request.setPassword("nuevaPass1");

        UsuarioEntity actualizado = usuario(3L, "nuevo-nombre", true, rol("ROLE_USER", "Usuario"));
        when(service.actualizar(eq(3L), any(UsuarioUpdateRequest.class))).thenReturn(actualizado);

        mockMvc.perform(put("/usuarios/{id}", 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("nuevo-nombre"));

        ArgumentCaptor<UsuarioUpdateRequest> captor = ArgumentCaptor.forClass(UsuarioUpdateRequest.class);
        verify(service).actualizar(eq(3L), captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("nuevo-nombre");
        assertThat(captor.getValue().getPassword()).isEqualTo("nuevaPass1");
    }

    @Test
    void actualizar_camposNulos_permitidos_status200() throws Exception {
        // UsuarioUpdateRequest no tiene validaciones: username/password nulos son válidos.
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();

        UsuarioEntity sinCambios = usuario(4L, "igual", true, rol("ROLE_USER", "Usuario"));
        when(service.actualizar(eq(4L), any(UsuarioUpdateRequest.class))).thenReturn(sinCambios);

        mockMvc.perform(put("/usuarios/{id}", 4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.username").value("igual"));
    }

    @Test
    void actualizar_usernameDuplicado_devuelve400() throws Exception {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setUsername("ocupado");

        when(service.actualizar(eq(3L), any(UsuarioUpdateRequest.class)))
                .thenThrow(new ReglaNegocioException("El nombre de usuario ya está en uso"));

        mockMvc.perform(put("/usuarios/{id}", 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El nombre de usuario ya está en uso"));
    }

    @Test
    void actualizar_noEncontrado_devuelve404() throws Exception {
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setUsername("x");

        when(service.actualizar(eq(99L), any(UsuarioUpdateRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id: 99"));

        mockMvc.perform(put("/usuarios/{id}", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    // ---------------------------------------------------------------------
    // PATCH /usuarios/{id}/rol
    // ---------------------------------------------------------------------

    @Test
    void cambiarRol_devuelveUsuario_yStatus200() throws Exception {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRol("ROLE_ADMIN");

        UsuarioEntity actualizado = usuario(6L, "promovido", true, rol("ROLE_ADMIN", "Administrador"));
        when(service.cambiarRol(eq(6L), eq("ROLE_ADMIN"))).thenReturn(actualizado);

        mockMvc.perform(patch("/usuarios/{id}/rol", 6)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.rol").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.rolMostrar").value("Administrador"));

        verify(service).cambiarRol(6L, "ROLE_ADMIN");
    }

    @Test
    void cambiarRol_rolEnBlanco_devuelve400_validacion() throws Exception {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRol("   ");

        mockMvc.perform(patch("/usuarios/{id}/rol", 6)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));

        verify(service, never()).cambiarRol(any(), any());
    }

    @Test
    void cambiarRol_rolInexistente_devuelve404() throws Exception {
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRol("ROLE_FANTASMA");

        when(service.cambiarRol(eq(6L), eq("ROLE_FANTASMA")))
                .thenThrow(new RecursoNoEncontradoException("Rol no encontrado: ROLE_FANTASMA"));

        mockMvc.perform(patch("/usuarios/{id}/rol", 6)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    // ---------------------------------------------------------------------
    // PATCH /usuarios/{id}/estado
    // ---------------------------------------------------------------------

    @Test
    void cambiarEstado_activar_devuelveUsuario_yStatus200() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setActivo(true);

        UsuarioEntity actualizado = usuario(2L, "reactivado", true, rol("ROLE_USER", "Usuario"));
        when(service.cambiarEstado(eq(2L), eq(true))).thenReturn(actualizado);

        mockMvc.perform(patch("/usuarios/{id}/estado", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).cambiarEstado(2L, true);
    }

    @Test
    void cambiarEstado_desactivar_devuelveUsuario_yStatus200() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setActivo(false);

        UsuarioEntity actualizado = usuario(2L, "desactivado", false, rol("ROLE_USER", "Usuario"));
        when(service.cambiarEstado(eq(2L), eq(false))).thenReturn(actualizado);

        mockMvc.perform(patch("/usuarios/{id}/estado", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.activo").value(false));

        verify(service).cambiarEstado(2L, false);
    }

    @Test
    void cambiarEstado_activoNulo_devuelve400_validacion() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        // activo nulo -> @NotNull

        mockMvc.perform(patch("/usuarios/{id}/estado", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));

        verify(service, never()).cambiarEstado(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void cambiarEstado_noEncontrado_devuelve404() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setActivo(true);

        when(service.cambiarEstado(eq(99L), eq(true)))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id: 99"));

        mockMvc.perform(patch("/usuarios/{id}/estado", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    // ---------------------------------------------------------------------
    // DELETE /usuarios/{id}
    // ---------------------------------------------------------------------

    @Test
    void eliminar_existente_devuelve204_sinCuerpo() throws Exception {
        mockMvc.perform(delete("/usuarios/{id}", 10))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).eliminar(10L);
    }

    @Test
    void eliminar_noEncontrado_devuelve404() throws Exception {
        org.mockito.Mockito.doThrow(new RecursoNoEncontradoException("Usuario no encontrado con id: 99"))
                .when(service).eliminar(eq(99L));

        mockMvc.perform(delete("/usuarios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"));
    }

    @Test
    void eliminar_rootProtegido_devuelve400_reglaNegocio() throws Exception {
        org.mockito.Mockito.doThrow(new ReglaNegocioException("No se puede eliminar el super-usuario root."))
                .when(service).eliminar(eq(1L));

        mockMvc.perform(delete("/usuarios/{id}", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el super-usuario root."));
    }
}
