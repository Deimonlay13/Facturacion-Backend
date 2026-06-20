package com.gdl.facturacion_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Atajo para abrir el panel de administración estático en /admin/index.html.
 */
@Controller
public class AdminPanelController {

    @GetMapping({"/admin", "/admin/"})
    public String panel() {
        // forward (no redirect) para que la URL quede en /admin sin mostrar index.html
        return "forward:/admin/index.html";
    }
}
