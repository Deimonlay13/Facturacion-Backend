package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RefreshTokenEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.RefreshTokenRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long REFRESH_EXPIRATION_MS = 604_800_000L; // 7 días

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        // El campo @Value no se inyecta en un test unitario; lo seteamos por reflexión.
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", REFRESH_EXPIRATION_MS);
    }

    // ---------- helpers ----------

    private UsuarioEntity buildUsuario(Long id, String username, EmpresaEntity empresa, RolEntity rol) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setEmpresa(empresa);
        usuario.setRol(rol);
        return usuario;
    }

    private RolEntity buildRol(String nombre) {
        RolEntity rol = new RolEntity();
        rol.setNombre(nombre);
        return rol;
    }

    private EmpresaEntity buildEmpresa(Long id) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        return empresa;
    }

    // ---------- crear ----------

    @Test
    void crear_persisteRefreshTokenConDatosCorrectos_yRetornaElToken() {
        UsuarioEntity usuario = buildUsuario(42L, "jdoe", buildEmpresa(1L), buildRol("ROLE_ADMIN"));
        LocalDateTime antes = LocalDateTime.now();

        String token = refreshTokenService.crear(usuario);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshTokenEntity guardado = captor.getValue();

        // El token retornado es el mismo que se persistió.
        assertThat(token).isNotNull();
        assertThat(token).isEqualTo(guardado.getToken());
        // Debe ser un UUID válido.
        assertThat(token).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        assertThat(guardado.getUsuarioId()).isEqualTo(42L);
        assertThat(guardado.isRevocado()).isFalse();
        // Expira aproximadamente 7 días en el futuro.
        assertThat(guardado.getExpiraEn())
                .isAfter(antes.plusDays(6))
                .isBefore(LocalDateTime.now().plusDays(8));
    }

    @Test
    void crear_generaTokensUnicos_enLlamadasSucesivas() {
        UsuarioEntity usuario = buildUsuario(1L, "user", buildEmpresa(1L), buildRol("ROLE_USER"));

        String token1 = refreshTokenService.crear(usuario);
        String token2 = refreshTokenService.crear(usuario);

        assertThat(token1).isNotEqualTo(token2);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshTokenEntity.class));
    }

    // ---------- refrescar ----------

    @Test
    void refrescar_lanzaReglaNegocio_cuandoTokenNoExiste() {
        when(refreshTokenRepository.findByToken("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refrescar("desconocido"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Refresh token inválido");

        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void refrescar_lanzaReglaNegocio_cuandoTokenEstaRevocado() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok");
        rt.setUsuarioId(7L);
        rt.setRevocado(true);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1)); // aún no expira, pero está revocado
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> refreshTokenService.refrescar("tok"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Sesión expirada, vuelve a iniciar sesión");

        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void refrescar_lanzaReglaNegocio_cuandoTokenEstaExpirado() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok");
        rt.setUsuarioId(7L);
        rt.setRevocado(false);
        rt.setExpiraEn(LocalDateTime.now().minusSeconds(1)); // expirado
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> refreshTokenService.refrescar("tok"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Sesión expirada, vuelve a iniciar sesión");

        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void refrescar_lanzaReglaNegocio_cuandoUsuarioNoExiste() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok");
        rt.setUsuarioId(99L);
        rt.setRevocado(false);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(rt));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refrescar("tok"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Usuario no encontrado");

        verify(usuarioRepository).findById(99L);
        verifyNoInteractions(jwtService);
    }

    @Test
    void refrescar_emiteNuevoAccessToken_conEmpresaId_cuandoUsuarioTieneEmpresa() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok-valido");
        rt.setUsuarioId(5L);
        rt.setRevocado(false);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));

        UsuarioEntity usuario = buildUsuario(5L, "jdoe", buildEmpresa(3L), buildRol("ROLE_ADMIN"));

        when(refreshTokenRepository.findByToken("tok-valido")).thenReturn(Optional.of(rt));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken("jdoe", 3L, "ROLE_ADMIN")).thenReturn("nuevo-access-token");

        AuthResponse response = refreshTokenService.refrescar("tok-valido");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("nuevo-access-token");
        // El refresh token retornado se mantiene igual al recibido.
        assertThat(response.getRefreshToken()).isEqualTo("tok-valido");
        verify(jwtService).generateToken("jdoe", 3L, "ROLE_ADMIN");
    }

    @Test
    void refrescar_emiteNuevoAccessToken_conEmpresaIdNull_cuandoUsuarioNoTieneEmpresa() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok-valido");
        rt.setUsuarioId(10L);
        rt.setRevocado(false);
        rt.setExpiraEn(LocalDateTime.now().plusDays(1));

        // Super admin: sin empresa.
        UsuarioEntity usuario = buildUsuario(10L, "superadmin", null, buildRol("ROLE_SUPER_ADMIN"));

        when(refreshTokenRepository.findByToken("tok-valido")).thenReturn(Optional.of(rt));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken("superadmin", null, "ROLE_SUPER_ADMIN")).thenReturn("access-sa");

        AuthResponse response = refreshTokenService.refrescar("tok-valido");

        assertThat(response.getToken()).isEqualTo("access-sa");
        assertThat(response.getRefreshToken()).isEqualTo("tok-valido");
        verify(jwtService).generateToken("superadmin", null, "ROLE_SUPER_ADMIN");
    }

    // ---------- revocar ----------

    @Test
    void revocar_marcaTokenComoRevocadoYGuarda_cuandoExiste() {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setToken("tok");
        rt.setRevocado(false);
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(rt));

        refreshTokenService.revocar("tok");

        assertThat(rt.isRevocado()).isTrue();
        verify(refreshTokenRepository).findByToken("tok");
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    void revocar_noHaceNada_cuandoTokenNoExiste() {
        when(refreshTokenRepository.findByToken("inexistente")).thenReturn(Optional.empty());

        refreshTokenService.revocar("inexistente");

        verify(refreshTokenRepository).findByToken("inexistente");
        verify(refreshTokenRepository, never()).save(any());
    }
}
