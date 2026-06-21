package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.usuario.CambiarEstadoRequest;
import com.gdl.facturacion_backend.dto.usuario.CambiarRolRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioCreateRequest;
import com.gdl.facturacion_backend.dto.usuario.UsuarioResponse;
import com.gdl.facturacion_backend.dto.usuario.UsuarioUpdateRequest;
import com.gdl.facturacion_backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administración de usuarios de la empresa (tenant) del solicitante.
 * La protección por roles se aborda en otra rama; por ahora basta con un JWT válido.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Usuarios", description = "Gestión de usuarios de la empresa")
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return UsuarioResponse.from(service.crear(request));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar().stream().map(UsuarioResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return UsuarioResponse.from(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return UsuarioResponse.from(service.actualizar(id, request));
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id,
                                      @Valid @RequestBody CambiarRolRequest request) {
        return UsuarioResponse.from(service.cambiarRol(id, request.getRol()));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRequest request) {
        return UsuarioResponse.from(service.cambiarEstado(id, request.getActivo()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
