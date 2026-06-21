package com.gdl.facturacion_backend.controller;

import com.gdl.facturacion_backend.dto.ClienteCreateRequest;
import com.gdl.facturacion_backend.dto.ClienteResponse;
import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.service.ClienteService;
import com.gdl.facturacion_backend.util.RutUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private static final String[] PLANTILLA_HEADERS = {
            "rut*", "razonSocial*", "email*", "nombreFantasia",
            "giro", "direccion", "ciudad", "comuna", "region", "pais", "telefono"
    };

    private final ClienteService service;
    private final Validator validator;
    private final DataFormatter dataFormatter = new DataFormatter();

    public ClienteController(ClienteService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteCreateRequest request) {
        return ClienteResponse.from(service.crear(request));
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return service.listarActivos().stream().map(ClienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return ClienteResponse.from(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody ClienteCreateRequest request) {
        return ClienteResponse.from(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /**
     * Validates RUT format/DV and queries the SRE API for company data.
     * Returns 200 with company data, 404 when not found in SRE, or 400 for invalid RUT.
     */
    @GetMapping("/sre")
    public ResponseEntity<SreCompanyResponse> consultarSre(@RequestParam String rut) {
        Optional<SreCompanyResponse> resultado = service.consultarSre(rut);
        return resultado.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Downloads an XLSX template (data sheet has only headers; example lives in Instrucciones sheet). */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantilla() throws IOException {
        byte[] excel = generarPlantillaExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("plantilla_clientes.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(excel);
    }

    @PostMapping("/cargar-plantilla")
    public ResponseEntity<CargaPlantillaResult> cargarPlantilla(
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        List<ClienteResponse> creados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        Set<String> rutsEnBatch = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                return ResponseEntity.badRequest()
                        .body(new CargaPlantillaResult(creados, List.of("El archivo está vacío")));
            }
            rows.next(); // skip header row

            int rowNum = 2;
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) {
                    rowNum++;
                    continue;
                }
                try {
                    ClienteCreateRequest req = leerFila(row);

                    // Reuse @NotBlank / @Email constraints from the DTO
                    Set<ConstraintViolation<ClienteCreateRequest>> violations = validator.validate(req);
                    if (!violations.isEmpty()) {
                        String msg = violations.stream()
                                .map(ConstraintViolation::getMessage)
                                .collect(Collectors.joining(", "));
                        errores.add("Fila " + rowNum + ": " + msg);
                        rowNum++;
                        continue;
                    }

                    // RUT format + DV
                    String rutClean = RutUtils.clean(req.getRut());
                    if (!RutUtils.isValid(rutClean)) {
                        errores.add("Fila " + rowNum + ": RUT inválido: " + req.getRut());
                        rowNum++;
                        continue;
                    }

                    // Intra-batch duplicate check (DB-level duplicate is caught below)
                    if (!rutsEnBatch.add(rutClean)) {
                        errores.add("Fila " + rowNum + ": RUT duplicado en el archivo: " + rutClean);
                        rowNum++;
                        continue;
                    }

                    // Autocomplete blank fields from SRE (non-blocking)
                    autocompletarDesdeSre(req, rutClean);

                    creados.add(ClienteResponse.from(service.crear(req)));
                } catch (Exception ex) {
                    errores.add("Fila " + rowNum + ": " + ex.getMessage());
                }
                rowNum++;
            }
        }

        return ResponseEntity.ok(new CargaPlantillaResult(creados, errores));
    }

    private void autocompletarDesdeSre(ClienteCreateRequest req, String rutClean) {
        try {
            service.consultarSre(rutClean).ifPresent(sre -> {
                if (isBlank(req.getRazonSocial()) && sre.getRazonSocial() != null)
                    req.setRazonSocial(sre.getRazonSocial());
                if (isBlank(req.getNombreFantasia()) && sre.getNombreFantasia() != null)
                    req.setNombreFantasia(sre.getNombreFantasia());
                if (isBlank(req.getGiro()) && sre.getGiro() != null)
                    req.setGiro(sre.getGiro());
                if (isBlank(req.getDireccion()) && sre.getDireccion() != null)
                    req.setDireccion(sre.getDireccion());
                if (isBlank(req.getCiudad()) && sre.getCiudad() != null)
                    req.setCiudad(sre.getCiudad());
                if (isBlank(req.getComuna()) && sre.getComuna() != null)
                    req.setComuna(sre.getComuna());
                if (isBlank(req.getRegion()) && sre.getRegion() != null)
                    req.setRegion(sre.getRegion());
                if (isBlank(req.getTelefono()) && sre.getTelefono() != null)
                    req.setTelefono(sre.getTelefono());
                if (isBlank(req.getEmail()) && sre.getEmail() != null)
                    req.setEmail(sre.getEmail());
            });
        } catch (Exception ignored) {
            // SRE unreachable → proceed with file data
        }
    }

    private byte[] generarPlantillaExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = buildHeaderStyle(workbook);

            // ---- "Clientes" sheet: headers only, no example data ----
            Sheet dataSheet = workbook.createSheet("Clientes");
            Row headerRow = dataSheet.createRow(0);
            for (int i = 0; i < PLANTILLA_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(PLANTILLA_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                dataSheet.setColumnWidth(i, 6500);
            }

            // ---- "Instrucciones" sheet: guidance + example (never imported) ----
            Sheet instrSheet = workbook.createSheet("Instrucciones");
            instrSheet.setColumnWidth(0, 9000);
            instrSheet.createRow(0).createCell(0)
                    .setCellValue("Complete la hoja 'Clientes'. Los campos marcados con * son obligatorios.");
            instrSheet.createRow(1).createCell(0)
                    .setCellValue("El sistema completará automáticamente los campos vacíos consultando el RUT en SRE.");
            instrSheet.createRow(2).createCell(0).setCellValue("Ejemplo:");

            Row exampleHeader = instrSheet.createRow(3);
            for (int i = 0; i < PLANTILLA_HEADERS.length; i++) {
                Cell cell = exampleHeader.createCell(i);
                cell.setCellValue(PLANTILLA_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                instrSheet.setColumnWidth(i, 6500);
            }

            Row example = instrSheet.createRow(4);
            example.createCell(0).setCellValue("76883241-2");
            example.createCell(1).setCellValue("Empresa Ejemplo SpA");
            example.createCell(2).setCellValue("contacto@empresa.cl");
            example.createCell(3).setCellValue("Empresa Ejemplo");
            example.createCell(4).setCellValue("Actividades de Software");
            example.createCell(5).setCellValue("Av. Las Leones 1234");
            example.createCell(6).setCellValue("Santiago");
            example.createCell(7).setCellValue("Providencia");
            example.createCell(8).setCellValue("Región Metropolitana");
            example.createCell(9).setCellValue("Chile");
            example.createCell(10).setCellValue("+56912345678");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private ClienteCreateRequest leerFila(Row row) {
        ClienteCreateRequest req = new ClienteCreateRequest();
        req.setRut(cellText(row, 0));
        req.setRazonSocial(cellText(row, 1));
        req.setEmail(cellText(row, 2));
        req.setNombreFantasia(cellText(row, 3));
        req.setGiro(cellText(row, 4));
        req.setDireccion(cellText(row, 5));
        req.setCiudad(cellText(row, 6));
        req.setComuna(cellText(row, 7));
        req.setRegion(cellText(row, 8));
        req.setPais(cellText(row, 9));
        req.setTelefono(cellText(row, 10));
        return req;
    }

    /** Uses DataFormatter to preserve RUT hyphens and any cell formatting regardless of cell type. */
    private String cellText(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String value = dataFormatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < PLANTILLA_HEADERS.length; i++) {
            if (cellText(row, i) != null) return false;
        }
        return true;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record CargaPlantillaResult(List<ClienteResponse> creados, List<String> errores) {}
}
