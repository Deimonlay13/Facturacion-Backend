package com.gdl.facturacion_backend.controller;

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
}