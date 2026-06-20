package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.dto.documento.importacion.FacturaTxt;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser del TXT de factura (pipe-delimitado). Cada línea empieza con '|' y su
 * segundo token identifica la sección: **CLIENTE**, **CABECERA**, **OBSERVACION**, **DETALLE**.
 */
@Component
public class TxtFacturaParser {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public FacturaTxt parse(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new ReglaNegocioException("El archivo TXT está vacío");
        }

        Integer codigoTipo = null;
        FacturaTxt.Cliente cliente = null;
        LocalDate fechaEmision = null;
        LocalDate fechaVencimiento = null;
        String condicionPago = null;
        BigDecimal tipoCambio = null;
        String observacion = null;
        String operacion = null;
        String vendedor = null;
        List<FacturaTxt.Detalle> detalles = new ArrayList<>();

        for (String linea : contenido.split("\\r?\\n")) {
            if (linea.isBlank()) {
                continue;
            }
            String[] t = linea.split("\\|", -1);
            String marcador = campo(t, 1);
            if (marcador == null) {
                continue;
            }
            switch (marcador.replace("*", "").trim().toUpperCase()) {
                case "CLIENTE" -> {
                    codigoTipo = parseEntero(campo(t, 2), "tipo de documento (CLIENTE)");
                    cliente = new FacturaTxt.Cliente(
                            campo(t, 3),  // código
                            campo(t, 5),  // razón social
                            campo(t, 6),  // dirección
                            campo(t, 7),  // país
                            campo(t, 9),  // ciudad
                            campo(t, 10)  // teléfono
                    );
                }
                case "CABECERA" -> {
                    fechaEmision = parseFecha(campo(t, 2), "fecha de emisión");
                    fechaVencimiento = parseFecha(campo(t, 3), "fecha de vencimiento");
                    condicionPago = campo(t, 4);
                    tipoCambio = parseDecimal(campo(t, 5));
                }
                case "OBSERVACION" -> {
                    observacion = limpiar(campo(t, 2));
                    vendedor = campo(t, 5);
                    operacion = campo(t, 6);
                }
                case "DETALLE" -> detalles.add(new FacturaTxt.Detalle(
                        parseEntero(campo(t, 2), "número de línea"),
                        campo(t, 5),  // código producto
                        campo(t, 6),  // descripción
                        parseDecimal(campo(t, 7)),  // cantidad
                        parseDecimal(campo(t, 8))   // precio unitario
                ));
                default -> { /* sección desconocida: se ignora */ }
            }
        }

        if (codigoTipo == null) {
            throw new ReglaNegocioException("El TXT no contiene una sección **CLIENTE** válida con el tipo de documento");
        }
        if (detalles.isEmpty()) {
            throw new ReglaNegocioException("El TXT no contiene líneas de **DETALLE**");
        }

        String moneda = detectarMoneda(detalles);

        return new FacturaTxt(codigoTipo, cliente, fechaEmision, fechaVencimiento,
                condicionPago, tipoCambio, moneda, observacion, operacion, vendedor, detalles);
    }

    private String campo(String[] tokens, int idx) {
        if (idx < 0 || idx >= tokens.length) {
            return null;
        }
        String v = tokens[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private Integer parseEntero(String valor, String campo) {
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Valor numérico inválido en " + campo + ": " + valor);
        }
    }

    private BigDecimal parseDecimal(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Monto inválido: " + valor);
        }
    }

    private LocalDate parseFecha(String valor, String campo) {
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim(), FECHA);
        } catch (DateTimeParseException e) {
            throw new ReglaNegocioException("Fecha inválida en " + campo + " (se espera dd-MM-yyyy): " + valor);
        }
    }

    /** Limpia caracteres de control / reemplazo que trae la observación del TXT. */
    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            sb.append((Character.isISOControl(c) || c == '�') ? ' ' : c);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String detectarMoneda(List<FacturaTxt.Detalle> detalles) {
        for (FacturaTxt.Detalle d : detalles) {
            String desc = d.descripcion() == null ? "" : d.descripcion().toUpperCase();
            if (desc.contains("USD")) return "USD";
            if (desc.contains("EUR")) return "EUR";
            if (desc.contains("UF")) return "UF";
        }
        return "CLP";
    }
}
