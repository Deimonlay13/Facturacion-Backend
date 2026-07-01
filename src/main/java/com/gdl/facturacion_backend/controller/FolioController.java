package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.folio.CafCargaRequest;
import com.gdl.facturacion_backend.dto.folio.CafResponse;
import com.gdl.facturacion_backend.dto.folio.ControlFolioResponse;
import com.gdl.facturacion_backend.dto.folio.FolioCargaSimpleRequest;
import com.gdl.facturacion_backend.dto.folio.FolioResponse;
import com.gdl.facturacion_backend.dto.folio.FolioResumenResponse;
import com.gdl.facturacion_backend.service.FolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folios")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Folios y CAF", description = "Carga de CAF, control y listado de folios")
public class FolioController {

    private final FolioService service;

    public FolioController(FolioService service) {
        this.service = service;
    }

    @PostMapping("/caf")
    @ResponseStatus(HttpStatus.CREATED)
    public CafResponse cargarCaf(@Valid @RequestBody CafCargaRequest request) {
        return service.cargarCaf(request);
    }

    @PostMapping("/simple")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResumenResponse cargarFoliosSimple(@Valid @RequestBody FolioCargaSimpleRequest request) {
        return service.cargarFoliosSimple(request);
    }

    @GetMapping("/caf")
    public List<CafResponse> listarCafs() {
        return service.listarCafs();
    }

    @GetMapping("/control")
    public List<ControlFolioResponse> listarControles() {
        return service.listar().stream().map(ControlFolioResponse::from).toList();
    }

    @GetMapping("/resumen")
    public List<FolioResumenResponse> resumenFolios() {
        return service.resumenFolios();
    }

    @GetMapping("/resumen/{codigoTipoDocumento}")
    public FolioResumenResponse resumenFolio(@PathVariable Integer codigoTipoDocumento) {
        return service.resumenFolio(codigoTipoDocumento);
    }

    @GetMapping
    public org.springframework.data.domain.Page<FolioResponse> listarFolios(
            org.springframework.data.domain.Pageable pageable) {
        return service.listarFoliosPaginado(pageable).map(FolioResponse::from);
    }
}
