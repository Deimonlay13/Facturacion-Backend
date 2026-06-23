package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.entity.AuditoriaEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.repository.AuditoriaRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Coloca una autenticación con el username indicado en el contexto de seguridad.
     */
    private void autenticarComo(String username) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                username, "credenciales", Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private UsuarioEntity usuarioConId(Long id, String username) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void registrar_conUsuarioAutenticadoExistente_guardaAuditoriaConUsuarioId() {
        autenticarComo("juan");
        when(usuarioRepository.findByUsername("juan"))
                .thenReturn(Optional.of(usuarioConId(7L, "juan")));

        LocalDateTime antes = LocalDateTime.now();
        auditoriaService.registrar("clientes", "CREATE", "alta de cliente");
        LocalDateTime despues = LocalDateTime.now();

        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());

        AuditoriaEntity guardada = captor.getValue();
        assertThat(guardada.getTabla()).isEqualTo("clientes");
        assertThat(guardada.getAccion()).isEqualTo("CREATE");
        assertThat(guardada.getDetalle()).isEqualTo("alta de cliente");
        assertThat(guardada.getUsuarioId()).isEqualTo(7L);
        assertThat(guardada.getFecha())
                .isNotNull()
                .isBetween(antes, despues);

        verify(usuarioRepository).findByUsername("juan");
    }

    @Test
    void registrar_sinAutenticacion_guardaAuditoriaConUsuarioIdNull() {
        // No se coloca autenticación en el contexto: auth == null

        auditoriaService.registrar("facturas", "DELETE", "borrado");

        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());

        AuditoriaEntity guardada = captor.getValue();
        assertThat(guardada.getTabla()).isEqualTo("facturas");
        assertThat(guardada.getAccion()).isEqualTo("DELETE");
        assertThat(guardada.getDetalle()).isEqualTo("borrado");
        assertThat(guardada.getUsuarioId()).isNull();

        // Sin auth no debe consultarse el repositorio de usuarios
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void registrar_usuarioAutenticadoNoEncontrado_guardaAuditoriaConUsuarioIdNull() {
        autenticarComo("desconocido");
        when(usuarioRepository.findByUsername("desconocido"))
                .thenReturn(Optional.empty());

        auditoriaService.registrar("roles", "UPDATE", "cambio de rol");

        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());

        assertThat(captor.getValue().getUsuarioId()).isNull();
        verify(usuarioRepository).findByUsername("desconocido");
    }

    @Test
    void registrar_cuandoSaveLanzaExcepcion_noPropagaYNoRompeFlujo() {
        autenticarComo("juan");
        when(usuarioRepository.findByUsername("juan"))
                .thenReturn(Optional.of(usuarioConId(3L, "juan")));
        doThrow(new RuntimeException("fallo de BD"))
                .when(auditoriaRepository).save(any(AuditoriaEntity.class));

        // La auditoría nunca debe romper la operación de negocio
        assertThatCode(() -> auditoriaService.registrar("clientes", "CREATE", "detalle"))
                .doesNotThrowAnyException();

        verify(auditoriaRepository).save(any(AuditoriaEntity.class));
    }

    @Test
    void registrar_cuandoFindByUsernameLanzaExcepcion_usuarioIdNullYGuardaIgual() {
        autenticarComo("juan");
        when(usuarioRepository.findByUsername("juan"))
                .thenThrow(new RuntimeException("error al consultar usuario"));

        assertThatCode(() -> auditoriaService.registrar("clientes", "CREATE", "detalle"))
                .doesNotThrowAnyException();

        // La excepción al resolver el usuario se ignora -> usuarioId null,
        // pero la auditoría se guarda igual.
        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isNull();
    }

    @Test
    void registrar_conDetalleNull_guardaAuditoriaSinError() {
        autenticarComo("juan");
        when(usuarioRepository.findByUsername("juan"))
                .thenReturn(Optional.of(usuarioConId(1L, "juan")));

        assertThatCode(() -> auditoriaService.registrar("clientes", "CREATE", null))
                .doesNotThrowAnyException();

        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getDetalle()).isNull();
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void registrar_usuarioEncontradoConIdNull_guardaAuditoriaConUsuarioIdNull() {
        autenticarComo("juan");
        // Usuario existe pero su id es null -> el map produce null
        when(usuarioRepository.findByUsername("juan"))
                .thenReturn(Optional.of(usuarioConId(null, "juan")));

        auditoriaService.registrar("clientes", "CREATE", "detalle");

        ArgumentCaptor<AuditoriaEntity> captor = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isNull();
    }

    @Test
    void registrar_nuncaConsultaUsuariosCuandoNoHayAuth() {
        SecurityContextHolder.clearContext();

        auditoriaService.registrar("clientes", "CREATE", "detalle");

        verify(usuarioRepository, never()).findByUsername(any());
        verify(auditoriaRepository).save(any(AuditoriaEntity.class));
    }
}
