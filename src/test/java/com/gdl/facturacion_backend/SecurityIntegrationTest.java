package com.gdl.facturacion_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdl.facturacion_backend.dto.LoginRequest;
import com.gdl.facturacion_backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de INTEGRACIÓN de seguridad: levanta el contexto completo de Spring
 * (con H2 en perfil de test) y ejercita las reglas declaradas en
 * {@link com.gdl.facturacion_backend.config.SecurityConfig} a través de
 * {@link com.gdl.facturacion_backend.security.JwtFilter} y
 * {@link JwtService}.
 *
 * <p>Los JWT se generan con el {@code JwtService} real inyectado, usando el
 * mismo secreto que configura {@code src/test/resources/application.properties},
 * por lo que el filtro de seguridad los valida correctamente. El claim
 * {@code empresaId} es necesario para los endpoints que resuelven el tenant
 * (p. ej. {@code GET /usuarios} y {@code GET /api/folios}); sin él, esos
 * servicios fallarían al pedir la empresa del contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String EMPRESA_ID = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String tokenUser() {
        return jwtService.generateToken("usuario", 1L, "ROLE_USER");
    }

    private String tokenAdmin() {
        return jwtService.generateToken("admin", 1L, "ROLE_ADMIN");
    }

    private String tokenSuperAdmin() {
        return jwtService.generateToken("root", 1L, "ROLE_SUPER_ADMIN");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ------------------------------------------------------------------
    // Anónimo (sin token)
    // ------------------------------------------------------------------

    @Test
    void anonimo_getUsuarios_devuelve401() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonimo_getEmpresas_devuelve401() throws Exception {
        mockMvc.perform(get("/empresas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonimo_getRoles_devuelve401() throws Exception {
        mockMvc.perform(get("/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonimo_getFolios_devuelve401() throws Exception {
        mockMvc.perform(get("/api/folios"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // ROLE_USER -> 403 en endpoints de administración
    // ------------------------------------------------------------------

    @Test
    void user_getUsuarios_devuelve403() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header("Authorization", bearer(tokenUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_getEmpresas_devuelve403() throws Exception {
        mockMvc.perform(get("/empresas")
                        .header("Authorization", bearer(tokenUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_getRoles_devuelve403() throws Exception {
        mockMvc.perform(get("/roles")
                        .header("Authorization", bearer(tokenUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_getFolios_devuelve403() throws Exception {
        mockMvc.perform(get("/api/folios")
                        .header("Authorization", bearer(tokenUser())))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // ROLE_ADMIN -> 200 en endpoints de administración
    // ------------------------------------------------------------------

    @Test
    void admin_getUsuarios_devuelve200() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void admin_getEmpresas_devuelve200() throws Exception {
        mockMvc.perform(get("/empresas")
                        .header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void admin_getRoles_devuelve200() throws Exception {
        mockMvc.perform(get("/roles")
                        .header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void admin_getFolios_devuelve200() throws Exception {
        mockMvc.perform(get("/api/folios")
                        .header("Authorization", bearer(tokenAdmin())))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // ROLE_SUPER_ADMIN -> 200 en endpoints de administración
    // ------------------------------------------------------------------

    @Test
    void superAdmin_getUsuarios_devuelve200() throws Exception {
        mockMvc.perform(get("/usuarios")
                        .header("Authorization", bearer(tokenSuperAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void superAdmin_getEmpresas_devuelve200() throws Exception {
        mockMvc.perform(get("/empresas")
                        .header("Authorization", bearer(tokenSuperAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void superAdmin_getRoles_devuelve200() throws Exception {
        mockMvc.perform(get("/roles")
                        .header("Authorization", bearer(tokenSuperAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void superAdmin_getFolios_devuelve200() throws Exception {
        mockMvc.perform(get("/api/folios")
                        .header("Authorization", bearer(tokenSuperAdmin())))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // POST /empresas (bootstrap): abierto sin token.
    // No debe ser 401 ni 403; con cuerpo vacío da 400 por validación.
    // ------------------------------------------------------------------

    @Test
    void postEmpresas_sinToken_noEs401Ni403_yDa400PorValidacion() throws Exception {
        MvcResult result = mockMvc.perform(post("/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        assertThat(statusCode)
                .as("POST /empresas es público (bootstrap), no debe rechazarse por seguridad")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
        assertThat(statusCode)
                .as("Con cuerpo vacío debe fallar la validación (@NotBlank)")
                .isEqualTo(400);
    }

    // ------------------------------------------------------------------
    // /auth/login con contraseña inválida -> 401
    // ------------------------------------------------------------------

    @Test
    void login_passwordInvalida_devuelve401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("no-existe");
        request.setPassword("contrasena-incorrecta");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Endpoints públicos accesibles sin token
    // ------------------------------------------------------------------

    @Test
    void raiz_accesibleSinToken() throws Exception {
        int status = mockMvc.perform(get("/"))
                .andReturn().getResponse().getStatus();
        assertThat(status)
                .as("GET / debe ser accesible sin token (no 401/403)")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    @Test
    void swaggerUi_accesibleSinToken() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn().getResponse().getStatus();
        assertThat(status)
                .as("GET /swagger-ui.html debe ser accesible sin token (no 401/403); springdoc redirige con 3xx/2xx")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    @Test
    void adminIndex_accesibleSinToken() throws Exception {
        int status = mockMvc.perform(get("/admin/index.html"))
                .andReturn().getResponse().getStatus();
        assertThat(status)
                .as("GET /admin/index.html debe ser accesible sin token (no 401/403)")
                .isNotEqualTo(401)
                .isNotEqualTo(403);
    }

    @Test
    void tiposDocumento_accesibleSinToken() throws Exception {
        mockMvc.perform(get("/api/tipos-documento"))
                .andExpect(status().isOk());
    }
}
