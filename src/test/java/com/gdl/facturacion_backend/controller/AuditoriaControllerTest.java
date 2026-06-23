package com.gdl.facturacion_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gdl.facturacion_backend.entity.AuditoriaEntity;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.repository.AuditoriaRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del controller {@link AuditoriaController} usando MockMvc en modo standalone
 * (sin filtro de seguridad) y el {@link GlobalExceptionHandler} real para el mapeo de errores.
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @InjectMocks
    private AuditoriaController auditoriaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditoriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuditoriaEntity buildAuditoria(Long id, String tabla, String accion, Long usuarioId,
                                           String detalle) {
        AuditoriaEntity a = new AuditoriaEntity();
        a.setId(id);
        a.setTabla(tabla);
        a.setAccion(accion);
        a.setUsuarioId(usuarioId);
        a.setDetalle(detalle);
        // fecha se deja en null para no requerir el modulo JavaTime en el converter de MockMvc.
        return a;
    }

    // ----------------------- GET /auditoria -----------------------

    @Test
    void listar_devuelve200ConListaDeAuditorias() throws Exception {
        List<AuditoriaEntity> registros = List.of(
                buildAuditoria(2L, "cliente", "CREATE", 10L, "Se creo cliente"),
                buildAuditoria(1L, "producto", "DELETE", 11L, "Se elimino producto"));
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(registros);

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].tabla").value("cliente"))
                .andExpect(jsonPath("$[0].accion").value("CREATE"))
                .andExpect(jsonPath("$[0].usuarioId").value(10))
                .andExpect(jsonPath("$[0].detalle").value("Se creo cliente"))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].tabla").value("producto"))
                .andExpect(jsonPath("$[1].accion").value("DELETE"))
                .andExpect(jsonPath("$[1].usuarioId").value(11))
                .andExpect(jsonPath("$[1].detalle").value("Se elimino producto"));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void listar_cuandoNoHayRegistros_devuelve200ConListaVacia() throws Exception {
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void listar_ordenaPorIdDescendente() throws Exception {
        // Verifica que el controller solicita el orden DESC por "id" al repositorio.
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk());

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(auditoriaRepository).findAll(sortCaptor.capture());

        Sort.Order order = sortCaptor.getValue().getOrderFor("id");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(order.getProperty()).isEqualTo("id");
    }

    @Test
    void listar_limitaResultadosA200() throws Exception {
        // El repositorio devuelve 250 registros pero el controller debe limitar a 200.
        List<AuditoriaEntity> registros = new ArrayList<>(
                IntStream.rangeClosed(1, 250)
                        .mapToObj(i -> buildAuditoria((long) i, "tabla", "ACCION",
                                (long) i, "detalle " + i))
                        .toList());
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(registros);

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(200))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[199].id").value(200));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void listar_cuandoExactamente200_devuelveTodos() throws Exception {
        List<AuditoriaEntity> registros = new ArrayList<>(
                IntStream.rangeClosed(1, 200)
                        .mapToObj(i -> buildAuditoria((long) i, "tabla", "ACCION",
                                (long) i, "detalle " + i))
                        .toList());
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(registros);

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(200));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void listar_serializaCampoUsuarioIdNuloComoNull() throws Exception {
        AuditoriaEntity sinUsuario = buildAuditoria(1L, "sistema", "LOGIN", null, "Acceso anonimo");
        when(auditoriaRepository.findAll(any(Sort.class))).thenReturn(List.of(sinUsuario));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist())
                .andExpect(jsonPath("$[0].tabla").value("sistema"))
                .andExpect(jsonPath("$[0].accion").value("LOGIN"));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void listar_cuandoRepositorioFalla_devuelve500() throws Exception {
        when(auditoriaRepository.findAll(any(Sort.class)))
                .thenThrow(new RuntimeException("Fallo de base de datos"));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Fallo de base de datos"));

        verify(auditoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void post_noSoportado_devuelve405YNoLlamaAlRepositorio() throws Exception {
        // El controller solo expone GET; un POST debe dar 405 Method Not Allowed.
        mockMvc.perform(post("/auditoria"))
                .andExpect(status().isMethodNotAllowed());

        verify(auditoriaRepository, never()).findAll(any(Sort.class));
    }
}
