package com.gdl.facturacion_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.dto.EmpresaResponse;
import com.gdl.facturacion_backend.service.EmpresaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse create(@Valid @RequestBody EmpresaCreateRequest body) {
        return EmpresaResponse.from(service.create(body));
    }

    @GetMapping
    public List<EmpresaResponse> findAll() {
        return service.findAll().stream().map(EmpresaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EmpresaResponse findById(@PathVariable Long id) {
        return EmpresaResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    public EmpresaResponse update(@PathVariable Long id, @Valid @RequestBody EmpresaCreateRequest body) {
        return EmpresaResponse.from(service.update(id, body));
    }

    @PatchMapping("/{id}/estado")
    public EmpresaResponse cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return EmpresaResponse.from(service.cambiarEstado(id, activo));
    }
}
