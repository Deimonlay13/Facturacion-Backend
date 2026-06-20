package com.gdl.facturacion_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.service.EmpresaService;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaEntity create(@RequestBody EmpresaCreateRequest body) {
        return service.create(body);
    }

    @GetMapping
    public List<EmpresaEntity> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EmpresaEntity findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public EmpresaEntity update(@PathVariable Long id, @RequestBody EmpresaCreateRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}/estado")
    public EmpresaEntity cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        return service.cambiarEstado(id, activo);
    }
}