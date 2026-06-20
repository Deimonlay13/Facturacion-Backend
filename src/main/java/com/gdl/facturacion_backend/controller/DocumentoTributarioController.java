package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.documento.*;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoTributarioController {

    private final DocumentoTributarioService service;

    public DocumentoTributarioController(DocumentoTributarioService service) {
        this.service = service;
    }

    // crear documento (BORRADOR)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoResponse crear(@Valid @RequestBody DocumentoCreateRequest request) {
        return DocumentoResponse.from(service.crear(request));
    }

    @PutMapping("/{id}")
    public DocumentoDetailResponse actualizarBorrador(@PathVariable Long id,
                                                      @Valid @RequestBody DocumentoUpdateRequest request) {
        return DocumentoDetailResponse.from(service.actualizarBorrador(id, request));
    }

    // consulta con filtros opcionales
    @GetMapping
    public List<DocumentoResponse> consultar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Integer tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.consultar(clienteId, tipo, estado, desde, hasta).stream()
                .map(DocumentoResponse::from)
                .toList();
    }

    // ver documento con su detalle, referencias y guía
    @GetMapping("/{id}")
    public DocumentoDetailResponse obtener(@PathVariable Long id) {
        return DocumentoDetailResponse.from(service.obtenerDetalle(id));
    }

    // agregar / eliminar líneas de detalle
    @PostMapping("/{id}/detalles")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDetailResponse agregarDetalle(@PathVariable Long id,
                                                  @Valid @RequestBody DetalleCreateRequest request) {
        return DocumentoDetailResponse.from(service.agregarDetalle(id, request));
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public DocumentoDetailResponse eliminarDetalle(@PathVariable Long id, @PathVariable Long detalleId) {
        return DocumentoDetailResponse.from(service.eliminarDetalle(id, detalleId));
    }

    // agregar referencia tributaria
    @PostMapping("/{id}/referencias")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDetailResponse agregarReferencia(@PathVariable Long id,
                                                     @Valid @RequestBody ReferenciaCreateRequest request) {
        return DocumentoDetailResponse.from(service.agregarReferencia(id, request));
    }

    // datos de guía de despacho (solo tipo 52)
    @PutMapping("/{id}/guia-despacho")
    public DocumentoDetailResponse guardarGuiaDespacho(@PathVariable Long id,
                                                       @Valid @RequestBody GuiaDespachoRequest request) {
        return DocumentoDetailResponse.from(service.guardarGuiaDespacho(id, request));
    }

    // emitir documento (asigna folio, pasa a EMITIDO)
    @PostMapping("/{id}/emitir")
    public DocumentoDetailResponse emitir(@PathVariable Long id) {
        return DocumentoDetailResponse.from(service.emitir(id));
    }
}
