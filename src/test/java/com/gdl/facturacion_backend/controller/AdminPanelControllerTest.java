package com.gdl.facturacion_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gdl.facturacion_backend.exception.GlobalExceptionHandler;

/**
 * Pruebas unitarias del {@link AdminPanelController} usando MockMvc en modo
 * standalone (sin levantar el contexto de Spring ni los filtros de seguridad).
 *
 * El controller no tiene dependencias de servicios: su única responsabilidad
 * es hacer un forward (no redirect) hacia la página estática del panel.
 */
@ExtendWith(MockitoExtension.class)
class AdminPanelControllerTest {

    private MockMvc mockMvc;

    private AdminPanelController adminPanelController;

    @BeforeEach
    void setUp() {
        adminPanelController = new AdminPanelController();
        mockMvc = MockMvcBuilders.standaloneSetup(adminPanelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------------------------------------------------------------------
    // Camino feliz: ruta /admin
    // ---------------------------------------------------------------------

    @Test
    void panel_conRutaAdmin_deberiaForwardearAIndexHtml() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/index.html"));
    }

    // ---------------------------------------------------------------------
    // Camino feliz: ruta /admin/ (con slash final, mapeada igual)
    // ---------------------------------------------------------------------

    @Test
    void panel_conRutaAdminConSlash_deberiaForwardearAIndexHtml() throws Exception {
        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/index.html"));
    }

    // ---------------------------------------------------------------------
    // El forward NO debe comportarse como redirect (3xx)
    // ---------------------------------------------------------------------

    @Test
    void panel_noDeberiaRedireccionar() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(status().is(200))
                .andExpect(forwardedUrl("/admin/index.html"));
    }

    // ---------------------------------------------------------------------
    // Caso límite: método HTTP no soportado (solo GET está mapeado).
    // En standalone MockMvc un POST a la ruta produce 405 Method Not Allowed.
    // ---------------------------------------------------------------------

    @Test
    void panel_conMetodoPost_deberiaResponderMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/admin"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------------------------------------------------------------------
    // Caso límite: ruta no mapeada -> 404 Not Found.
    // ---------------------------------------------------------------------

    @Test
    void getRutaNoMapeada_deberiaResponderNotFound() throws Exception {
        mockMvc.perform(get("/admin/inexistente"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------------
    // Prueba unitaria directa del método: verifica el nombre de la vista.
    // ---------------------------------------------------------------------

    @Test
    void panel_deberiaRetornarNombreDeVistaForward() {
        String vista = adminPanelController.panel();

        assertThat(vista).isEqualTo("forward:/admin/index.html");
        assertThat(vista).startsWith("forward:");
        assertThat(vista).doesNotStartWith("redirect:");
    }
}
