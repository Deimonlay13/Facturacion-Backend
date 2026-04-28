package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.tipoDocumento.TipoDocumentoResponseDto;
import com.gdl.facturacion_backend.service.TipoDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-documento")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoService service;

    @GetMapping
    public List<TipoDocumentoResponseDto> findAll() {
        return service.findAll();
    }
}