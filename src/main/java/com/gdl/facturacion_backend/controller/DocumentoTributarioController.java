package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.documento.*;
import com.gdl.facturacion_backend.dto.documento.importacion.ImportTxtPreviewResponse;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import com.gdl.facturacion_backend.service.documento.DocumentoExportService;
import com.gdl.facturacion_backend.service.documento.ImportacionTxtService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Documentos", description = "Documentos tributarios (DTE): creación, detalle, emisión, PDF/XML e importación TXT")
public class DocumentoTributarioController {

    private final DocumentoTributarioService service;
    private final ImportacionTxtService importacionService;
    private final DocumentoExportService exportService;

    public DocumentoTributarioController(DocumentoTributarioService service,
                                         ImportacionTxtService importacionService,
                                         DocumentoExportService exportService) {
        this.service = service;
        this.importacionService = importacionService;
        this.exportService = exportService;
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
    @GetMapping("/estadisticas/emisores")
    public List<DocumentoEmisorStatsResponse> estadisticasPorEmisor(
            @RequestParam(required = false) Integer tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.estadisticasPorEmisor(tipo, desde, hasta);
    }

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

    // ------------------------------------------------------------- carga TXT

    // previsualiza el TXT sin persistir nada
    @PostMapping(value = "/importar-txt/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTxtPreviewResponse previsualizarTxt(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return importacionService.previsualizar(leerTxt(archivo));
    }

    // importa el TXT y crea el documento en BORRADOR
    @PostMapping(value = "/importar-txt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDetailResponse importarTxt(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return DocumentoDetailResponse.from(importacionService.importar(leerTxt(archivo)));
    }

    // ------------------------------------------------------- descarga PDF/XML

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        return descarga(exportService.generarPdf(id), MediaType.APPLICATION_PDF,
                "documento-" + id + ".pdf");
    }

    @GetMapping("/{id}/factura")
    public ResponseEntity<byte[]> descargarFactura(@PathVariable Long id) {
        return descarga(exportService.generarPdf(id), MediaType.APPLICATION_PDF,
                "factura-" + id + ".pdf");
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long id) {
        return descarga(exportService.generarXml(id), MediaType.APPLICATION_XML,
                "documento-" + id + ".xml");
    }

    @GetMapping("/{id}/txt")
    public ResponseEntity<byte[]> descargarTxt(@PathVariable Long id) {
        return descarga(exportService.generarTxt(id),
                new MediaType("text", "plain", StandardCharsets.UTF_8),
                "documento-" + id + ".txt");
    }

    private ResponseEntity<byte[]> descarga(byte[] contenido, MediaType tipo, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(tipo);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return ResponseEntity.ok().headers(headers).body(contenido);
    }

    private String leerTxt(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo TXT está vacío");
        }
        // El TXT puede traer bytes fuera de UTF-8; ISO-8859-1 mapea 1:1 y el parser limpia controles.
        return new String(archivo.getBytes(), StandardCharsets.ISO_8859_1);
    }
}
