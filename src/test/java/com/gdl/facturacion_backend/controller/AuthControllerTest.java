package com.gdl.facturacion_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.AuthResponse;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.dto.RefreshRequest;
import com.gdl.facturacion_backend.dto.RegisterRequest;
import com.gdl.facturacion_backend.exception.CredencialesInvalidasException;
import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.RefreshTokenService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------------
    // POST /auth/signup
    // ---------------------------------------------------------------------

    @Test
    void register_devuelveTokens_yStatus200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("nuevo");
        request.setPassword("secret123");
        request.setRol("ROLE_USER");

        when(usuarioService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(usuarioService).register(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("nuevo");
        assertThat(captor.getValue().getPassword()).isEqualTo("secret123");
    }

    @Test
    void register_usernameDuplicado_devuelve400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existente");
        request.setPassword("secret123");

        when(usuarioService.register(any(RegisterRequest.class)))
                .thenThrow(new ReglaNegocioException("El nombre de usuario ya está en uso"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("El nombre de usuario ya está en uso"));
    }

    @Test
    void register_errorInesperado_devuelve500() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("nuevo");
        request.setPassword("secret123");

        when(usuarioService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Boom"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno"))
                .andExpect(jsonPath("$.mensaje").value("Boom"));
    }

    // ---------------------------------------------------------------------
    // POST /auth/login
    // ---------------------------------------------------------------------

    @Test
    void login_credencialesValidas_devuelveTokens_yStatus200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(usuarioService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-access", "jwt-refresh"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-access"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(usuarioService).login(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getPassword()).isEqualTo("admin123");
    }

    @Test
    void login_credencialesInvalidas_devuelve401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("malo");

        when(usuarioService.login(any(LoginRequest.class)))
                .thenThrow(new CredencialesInvalidasException("Usuario o contraseña inválidos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña inválidos"));
    }

    // ---------------------------------------------------------------------
    // POST /auth/refresh
    // ---------------------------------------------------------------------

    @Test
    void refresh_tokenValido_devuelveNuevoAccess_yStatus200() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-valido");

        when(refreshTokenService.refrescar(eq("refresh-valido")))
                .thenReturn(new AuthResponse("nuevo-access", "refresh-valido"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("nuevo-access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-valido"));

        verify(refreshTokenService).refrescar("refresh-valido");
    }

    @Test
    void refresh_tokenInvalido_devuelve400() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token-malo");

        when(refreshTokenService.refrescar(eq("token-malo")))
                .thenThrow(new ReglaNegocioException("Refresh token inválido"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Regla de negocio"))
                .andExpect(jsonPath("$.mensaje").value("Refresh token inválido"));
    }

    @Test
    void refresh_sesionExpirada_devuelve400() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token-expirado");

        when(refreshTokenService.refrescar(eq("token-expirado")))
                .thenThrow(new ReglaNegocioException("Sesión expirada, vuelve a iniciar sesión"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Sesión expirada, vuelve a iniciar sesión"));
    }

    // ---------------------------------------------------------------------
    // POST /auth/logout
    // ---------------------------------------------------------------------

    @Test
    void logout_revocaToken_yDevuelve204() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-a-revocar");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(refreshTokenService).revocar("refresh-a-revocar");
        verify(usuarioService, never()).login(any());
    }

    @Test
    void logout_errorEnServicio_devuelve500() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-x");

        org.mockito.Mockito.doThrow(new RuntimeException("fallo al revocar"))
                .when(refreshTokenService).revocar(eq("refresh-x"));

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje").value("fallo al revocar"));
    }
}
