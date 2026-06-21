package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.tipoDocumento.TipoDocumentoResponseDto;
import com.gdl.facturacion_backend.service.TipoDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-documento")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tipos de Documento", description = "Catálogo de tipos de DTE")
public class TipoDocumentoController {

    private final TipoDocumentoService service;

    @GetMapping
    public List<TipoDocumentoResponseDto> findAll() {
        return service.findAll();
    }
}