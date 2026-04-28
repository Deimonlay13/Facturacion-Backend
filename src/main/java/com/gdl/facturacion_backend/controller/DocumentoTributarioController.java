package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoResponse;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoTributarioController {

    private final DocumentoTributarioService service;

    @PostMapping
    public DocumentoResponse create(@RequestBody DocumentoCreateRequest request) {
        return service.create(request);
    }
}