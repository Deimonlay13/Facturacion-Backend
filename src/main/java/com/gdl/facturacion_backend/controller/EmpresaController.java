package com.gdl.facturacion_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.dto.EmpresaResponse;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.service.EmpresaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/empresas")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Empresas", description = "Gestión de empresas (emisores)")
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

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmpresaResponse subirLogo(@PathVariable Long id,
                                     @RequestParam(value = "file", required = false) MultipartFile file,
                                     @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        MultipartFile logo = file != null ? file : archivo;
        return EmpresaResponse.from(service.guardarLogo(id, logo));
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> verLogo(@PathVariable Long id) {
        EmpresaEntity empresa = service.findById(id);
        if (empresa.getLogo() == null || empresa.getLogo().length == 0) {
            throw new RecursoNoEncontradoException("La empresa no tiene logo cargado");
        }
        MediaType mediaType = empresa.getLogoContentType() != null
                ? MediaType.parseMediaType(empresa.getLogoContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(empresa.getLogo());
    }

    @DeleteMapping("/{id}/logo")
    public EmpresaResponse eliminarLogo(@PathVariable Long id) {
        return EmpresaResponse.from(service.eliminarLogo(id));
    }
}
