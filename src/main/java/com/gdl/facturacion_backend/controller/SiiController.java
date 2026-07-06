package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.sii.EnvioSiiResponse;
import com.gdl.facturacion_backend.dto.sii.EstadoSiiResponse;
import com.gdl.facturacion_backend.dto.sii.HistorialSiiAdminResponse;
import com.gdl.facturacion_backend.service.documento.EnvioSiiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@io.swagger.v3.oas.annotations.tags.Tag(name = "SII", description = "Simulación de envío de documentos al Servicio de Impuestos Internos")
public class SiiController {

    private final EnvioSiiService envioSiiService;

    public SiiController(EnvioSiiService envioSiiService) {
        this.envioSiiService = envioSiiService;
    }

    // Paso 1: enviar el DTE al SII (genera track id)
    @PostMapping("/{id}/enviar-sii")
    public EstadoSiiResponse enviar(@PathVariable Long id) {
        return envioSiiService.enviar(id);
    }

    // Paso 2: consultar el estado del envío (acepta o rechaza)
    @PostMapping("/{id}/consultar-sii")
    public EstadoSiiResponse consultar(@PathVariable Long id) {
        return envioSiiService.consultarEstado(id);
    }

    // Estado SII actual + historial (solo lectura)
    @GetMapping("/{id}/estado-sii")
    public EstadoSiiResponse estado(@PathVariable Long id) {
        return envioSiiService.obtenerEstado(id);
    }

    // --- Administración: tablas completas para el panel ---

    // Todos los envíos al SII (super admin: todas las empresas; resto: la suya)
    @GetMapping("/sii/envios")
    public List<EnvioSiiResponse> listarEnvios() {
        return envioSiiService.listarEnvios();
    }

    // Todo el historial de estados SII
    @GetMapping("/sii/historial")
    public List<HistorialSiiAdminResponse> listarHistorial() {
        return envioSiiService.listarHistorial();
    }
}
