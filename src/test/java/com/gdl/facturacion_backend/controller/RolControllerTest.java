package com.gdl.facturacion_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.RolRequest;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.RolService;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock
    private RolService rolService;

    @InjectMocks
    private RolController rolController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RolEntity buildRol(Long id, String nombre, String nombreMostrar, String descripcion) {
        RolEntity rol = new RolEntity();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setNombreMostrar(nombreMostrar);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        return rol;
    }

    // ----------------------- POST /roles -----------------------

    @Test
    void crear_devuelve200ConElRolCreado() throws Exception {
        RolRequest request = new RolRequest();
        request.setNombre("supervisor");
        request.setNombreMostrar("Supervisor");
        request.setDescripcion("Rol supervisor");

        RolEntity creado = buildRol(5L, "ROLE_SUPERVISOR", "Supervisor", "Rol supervisor");
        when(rolService.save(any(RolRequest.class))).thenReturn(creado);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("ROLE_SUPERVISOR"))
                .andExpect(jsonPath("$.nombreMostrar").value("Supervisor"))
                .andExpect(jsonPath("$.descripcion").value("Rol supervisor"))
                .andExpect(jsonPath("$.activo").value(true));

        ArgumentCaptor<RolRequest> captor = ArgumentCaptor.forClass(RolRequest.class);
        verify(rolService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getNombre()).isEqualTo("supervisor");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getNombreMostrar()).isEqualTo("Supervisor");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getDescripcion()).isEqualTo("Rol supervisor");
    }

    @Test
    void crear_cuandoRolDuplicado_devuelve500() throws Exception {
        RolRequest request = new RolRequest();
        request.setNombre("admin");

        when(rolService.save(any(RolRequest.class)))
                .thenThrow(new RuntimeException("El rol ROLE_ADMIN ya existe"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("El rol ROLE_ADMIN ya existe"));

        verify(rolService).save(any(RolRequest.class));
    }

    // ----------------------- GET /roles -----------------------

    @Test
    void listar_devuelve200ConListaDeRoles() throws Exception {
        List<RolEntity> roles = List.of(
                buildRol(1L, "ROLE_ADMIN", "Administrador", "Acceso total"),
                buildRol(2L, "ROLE_USER", "Usuario", "Acceso basico"));
        when(rolService.findAll()).thenReturn(roles);

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nombre").value("ROLE_USER"));

        verify(rolService).findAll();
    }

    @Test
    void listar_cuandoNoHayRoles_devuelve200ConListaVacia() throws Exception {
        when(rolService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(rolService).findAll();
    }

    // ----------------------- DELETE /roles/{id} -----------------------

    @Test
    void eliminar_devuelve204() throws Exception {
        doNothing().when(rolService).eliminar(7L);

        mockMvc.perform(delete("/roles/{id}", 7L))
                .andExpect(status().isNoContent());

        verify(rolService).eliminar(7L);
    }

    @Test
    void eliminar_cuandoNoExiste_devuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Rol no encontrado con id: 99"))
                .when(rolService).eliminar(99L);

        mockMvc.perform(delete("/roles/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No encontrado"))
                .andExpect(jsonPath("$.mensaje").value("Rol no encontrado con id: 99"));

        verify(rolService).eliminar(99L);
    }

    @Test
    void eliminar_cuandoRolDeSistema_devuelve400() throws Exception {
        doThrow(new ReglaNegocioException("Este rol del sistema no se puede eliminar."))
                .when(rolService).eliminar(1L);

        mockMvc.perform(delete("/roles/{id}", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Este rol del sistema no se puede eliminar."));

        verify(rolService).eliminar(1L);
    }

    @Test
    void eliminar_cuandoRolAsignadoAUsuarios_devuelve400() throws Exception {
        doThrow(new ReglaNegocioException("No se puede eliminar el rol: está asignado a uno o más usuarios."))
                .when(rolService).eliminar(3L);

        mockMvc.perform(delete("/roles/{id}", 3L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede eliminar el rol: está asignado a uno o más usuarios."));

        verify(rolService).eliminar(3L);
    }

    @Test
    void eliminar_idInvalidoNoNumerico_noLlamaAlServicio() throws Exception {
        // El id "abc" no puede convertirse a Long: MethodArgumentTypeMismatchException
        // es subtipo de RuntimeException y el GlobalExceptionHandler la mapea a 500.
        // Lo relevante es que el servicio NUNCA se invoca.
        mockMvc.perform(delete("/roles/{id}", "abc"))
                .andExpect(status().isInternalServerError());

        verify(rolService, never()).eliminar(any());
        verify(rolService, never()).eliminar(eq(0L));
    }
}
