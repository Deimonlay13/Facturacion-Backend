package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

final class DocumentoPdfGenerator {

    private static final Color ROJO_SII = new Color(190, 25, 45);
    private static final Color GRIS_CABECERA = new Color(226, 230, 234);
    private static final Color GRIS_SUAVE = new Color(247, 248, 249);
    private static final Color BORDE = new Color(95, 95, 95);

    private static final Font EMPRESA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.2f);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 7.2f);
    private static final Font NORMAL_GRANDE = FontFactory.getFont(FontFactory.HELVETICA, 8f);
    private static final Font TITULO_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f);
    private static final Font SII = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, ROJO_SII);
    private static final Font FOLIO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, ROJO_SII);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private DocumentoPdfGenerator() {
    }

    static byte[] generar(DocumentoTributarioEntity doc, EmpresaEntity emisor) {
        ClienteEntity receptor = doc.getCliente();
        List<ReferenciaDocumentoEntity> referencias =
                doc.getReferencias() != null ? doc.getReferencias() : List.of();

        Document pdf = new Document(PageSize.A4, 30, 30, 24, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(pdf, out);
        pdf.open();

        agregarEncabezado(pdf, doc, emisor);
        agregarReceptor(pdf, doc, receptor);
        agregarMotivo(pdf, referencias);
        agregarDetalle(pdf, doc);
        agregarObservaciones(pdf, doc);
        agregarReferencias(pdf, referencias);
        agregarPie(pdf, doc);

        pdf.close();
        return out.toByteArray();
    }

    private static void agregarEncabezado(
            Document pdf, DocumentoTributarioEntity doc, EmpresaEntity emisor) {
        String titulo = doc.getTipoDocumento().getDescripcion() != null
                ? doc.getTipoDocumento().getDescripcion()
                : "Documento " + doc.getTipoDocumento().getCodigoSii();
        String folio = doc.getFolio() != null ? String.valueOf(doc.getFolio()) : "BORRADOR";

        PdfPTable encabezado = new PdfPTable(new float[]{1.65f, 1f});
        encabezado.setWidthPercentage(100);
        encabezado.setSpacingAfter(2);

        PdfPCell datosEmisor = new PdfPCell();
        datosEmisor.setBorder(PdfPCell.NO_BORDER);
        datosEmisor.setPaddingRight(18);
        datosEmisor.setVerticalAlignment(Element.ALIGN_TOP);
        datosEmisor.addElement(new Paragraph(
                valor(doc.getNombreFantasiaEmisor(),
                        valor(doc.getRazonSocialEmisor(), emisor.getRazonSocial())), EMPRESA));
        datosEmisor.addElement(linea("GIRO", valor(doc.getGiroEmisor(), emisor.getGiro())));
        datosEmisor.addElement(new Paragraph(direccion(
                valor(doc.getDireccionEmisor(), emisor.getDireccion()),
                valor(doc.getComunaEmisor(), emisor.getComuna()),
                valor(doc.getCiudadEmisor(), emisor.getCiudad())), NORMAL));
        datosEmisor.addElement(linea(
                "TELÉFONO", valor(doc.getTelefonoEmisor(), emisor.getTelefono())));
        datosEmisor.addElement(linea(
                "CORREO", valor(doc.getEmailPrincipalEmisor(), emisor.getEmailPrincipal())));
        datosEmisor.addElement(linea("WEB", valor(emisor.getSitioWeb(), "")));
        encabezado.addCell(datosEmisor);

        PdfPCell sii = new PdfPCell();
        sii.setBorderColor(ROJO_SII);
        sii.setBorderWidth(2);
        sii.setPadding(7);
        sii.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sii.addElement(centrado("R.U.T.: " + valor(doc.getRutEmisor(), emisor.getRutEmpresa()), SII));
        sii.addElement(centrado(titulo.toUpperCase(), SII));
        sii.addElement(centrado("N° " + folio, FOLIO));
        encabezado.addCell(sii);
        pdf.add(encabezado);

        Paragraph oficina = new Paragraph("S.I.I. - DOCUMENTO TRIBUTARIO ELECTRÓNICO", LABEL);
        oficina.setAlignment(Element.ALIGN_RIGHT);
        oficina.setSpacingBefore(3);
        oficina.setSpacingAfter(10);
        pdf.add(oficina);
    }

    private static void agregarReceptor(
            Document pdf, DocumentoTributarioEntity doc, ClienteEntity receptor) {
        PdfPTable ficha = new PdfPTable(new float[]{0.92f, 2.45f, 1.05f, 1.42f});
        ficha.setWidthPercentage(100);
        ficha.setSpacingAfter(7);

        info(ficha, "SEÑORES", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getRazonSocial(), receptor != null ? receptor.getRazonSocial() : ""),
                NORMAL_GRANDE, Color.WHITE);
        info(ficha, "R.U.T.", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getRut(), receptor != null ? receptor.getRut() : ""),
                NORMAL_GRANDE, Color.WHITE);

        info(ficha, "GIRO", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getGiro(), receptor != null ? receptor.getGiro() : ""),
                NORMAL, Color.WHITE);
        info(ficha, "FECHA EMISIÓN", LABEL, GRIS_CABECERA);
        info(ficha, fecha(doc.getFechaEmision()), NORMAL, Color.WHITE);

        info(ficha, "DIRECCIÓN", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getDireccion(), receptor != null ? receptor.getDireccion() : ""),
                NORMAL, Color.WHITE);
        info(ficha, "FECHA VENCIMIENTO", LABEL, GRIS_CABECERA);
        info(ficha, fecha(doc.getFechaVencimiento()), NORMAL, Color.WHITE);

        info(ficha, "COMUNA", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getComuna(), receptor != null ? receptor.getComuna() : ""),
                NORMAL, Color.WHITE);
        info(ficha, "CIUDAD", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getCiudad(), receptor != null ? receptor.getCiudad() : ""),
                NORMAL, Color.WHITE);

        info(ficha, "CORREO", LABEL, GRIS_CABECERA);
        info(ficha, valor(doc.getCorreo(), receptor != null ? receptor.getEmail() : ""),
                NORMAL, Color.WHITE);
        info(ficha, "MONEDA", LABEL, GRIS_CABECERA);
        info(ficha, moneda(doc), NORMAL, Color.WHITE);

        pdf.add(ficha);
    }

    private static void agregarMotivo(Document pdf, List<ReferenciaDocumentoEntity> referencias) {
        String motivo = referencias.stream()
                .map(ReferenciaDocumentoEntity::getMotivo)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        if (motivo == null) {
            return;
        }

        PdfPTable tabla = new PdfPTable(new float[]{0.92f, 4.92f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(7);
        info(tabla, "MOTIVO", LABEL, GRIS_CABECERA);
        info(tabla, motivo, NORMAL, Color.WHITE);
        pdf.add(tabla);
    }

    private static void agregarDetalle(Document pdf, DocumentoTributarioEntity doc) {
        PdfPTable tabla = new PdfPTable(new float[]{1.05f, 4.45f, 0.75f, 0.65f, 1.25f, 0.65f, 1.25f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingAfter(8);

        cabecera(tabla, "CÓDIGO");
        cabecera(tabla, "DESCRIPCIÓN");
        cabecera(tabla, "CANT.");
        cabecera(tabla, "UNM");
        cabecera(tabla, "PRC. UNITARIO");
        cabecera(tabla, "%DTO");
        cabecera(tabla, "TOTAL");

        List<DetalleDocumentoEntity> detalles =
                doc.getDetalles() != null ? doc.getDetalles() : List.of();
        for (DetalleDocumentoEntity detalle : detalles) {
            String codigo = detalle.getProducto() != null
                    ? valor(detalle.getProducto().getCodigo(), "") : "";
            celda(tabla, codigo, Element.ALIGN_CENTER);
            celda(tabla, valor(detalle.getDescripcionItem(), ""), Element.ALIGN_LEFT);
            celda(tabla, numero(detalle.getCantidad()), Element.ALIGN_RIGHT);
            celda(tabla, valor(detalle.getUnidadMedida(), "UNI"), Element.ALIGN_CENTER);
            celda(tabla, monto(detalle.getPrecioUnitario()), Element.ALIGN_RIGHT);
            celda(tabla, "0", Element.ALIGN_RIGHT);
            celda(tabla, monto(detalle.getSubtotal()), Element.ALIGN_RIGHT);
        }

        if (detalles.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin ítems registrados", NORMAL));
            vacio.setColspan(7);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            vacio.setBorderColor(BORDE);
            vacio.setPadding(8);
            tabla.addCell(vacio);
        }
        pdf.add(tabla);
    }

    private static void agregarObservaciones(Document pdf, DocumentoTributarioEntity doc) {
        if (doc.getObservaciones() == null || doc.getObservaciones().isBlank()) {
            return;
        }

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(8);

        PdfPCell titulo = new PdfPCell(new Phrase("OBSERVACIONES", TITULO_SECCION));
        titulo.setBackgroundColor(GRIS_CABECERA);
        titulo.setBorderColor(BORDE);
        titulo.setPadding(4);
        tabla.addCell(titulo);

        PdfPCell contenido = new PdfPCell(new Phrase(doc.getObservaciones(), NORMAL));
        contenido.setBorderColor(BORDE);
        contenido.setBackgroundColor(GRIS_SUAVE);
        contenido.setPadding(6);
        contenido.setMinimumHeight(34);
        tabla.addCell(contenido);
        pdf.add(tabla);
    }

    private static void agregarReferencias(
            Document pdf, List<ReferenciaDocumentoEntity> referencias) {
        if (referencias.isEmpty()) {
            return;
        }

        Paragraph titulo = new Paragraph("Referencias a otros documentos", TITULO_SECCION);
        titulo.setSpacingAfter(3);
        pdf.add(titulo);

        PdfPTable tabla = new PdfPTable(new float[]{2f, 1f, 1.2f, 2.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(9);
        cabecera(tabla, "TIPO DOCUMENTO");
        cabecera(tabla, "FOLIO");
        cabecera(tabla, "FECHA");
        cabecera(tabla, "RAZÓN REFERENCIA");

        for (ReferenciaDocumentoEntity referencia : referencias) {
            DocumentoTributarioEntity referido = referencia.getDocumentoReferenciado();
            celda(tabla,
                    referido != null && referido.getTipoDocumento() != null
                            ? referido.getTipoDocumento().getDescripcion()
                            : valor(referencia.getTipoReferencia(), ""),
                    Element.ALIGN_LEFT);
            celda(tabla,
                    referido != null && referido.getFolio() != null
                            ? String.valueOf(referido.getFolio()) : "",
                    Element.ALIGN_CENTER);
            celda(tabla, referido != null ? fecha(referido.getFechaEmision()) : "",
                    Element.ALIGN_CENTER);
            celda(tabla, valor(referencia.getMotivo(), ""), Element.ALIGN_LEFT);
        }
        pdf.add(tabla);
    }

    private static void agregarPie(Document pdf, DocumentoTributarioEntity doc) {
        String moneda = doc.getMoneda() != null ? doc.getMoneda().name() : "CLP";
        Paragraph son = new Paragraph(
                "SON: " + montoEnPalabras(doc.getMontoTotal()) + " " + nombreMoneda(moneda),
                LABEL);
        son.setSpacingBefore(2);
        son.setSpacingAfter(5);
        pdf.add(son);

        PdfPTable pie = new PdfPTable(new float[]{1.65f, 1f});
        pie.setWidthPercentage(100);

        PdfPCell timbre = new PdfPCell();
        timbre.setBorderColor(BORDE);
        timbre.setPadding(7);
        timbre.setMinimumHeight(108);
        timbre.addElement(centrado("TIMBRE ELECTRÓNICO S.I.I.", TITULO_SECCION));
        timbre.addElement(centrado(
                "Documento tributario electrónico\n"
                        + "Verifique documento: www.sii.cl\n\n"
                        + "FECHA RECEPCIÓN: ____________________\n"
                        + "RECINTO: ____________________________\n"
                        + "NOMBRE: _____________________________\n"
                        + "RUT: ________________________________\n"
                        + "FIRMA: ______________________________", NORMAL));
        pie.addCell(timbre);

        PdfPTable totales = new PdfPTable(new float[]{1.2f, 1f});
        totales.setWidthPercentage(100);
        total(totales, "SUBTOTAL", doc.getMontoNeto(), NORMAL);
        total(totales, "NETO", doc.getMontoNeto(), NORMAL);
        total(totales, "IVA 19%", doc.getMontoIva(), NORMAL);
        BigDecimal exento = doc.getMontoIva() == null
                || doc.getMontoIva().compareTo(BigDecimal.ZERO) == 0
                ? doc.getMontoTotal() : BigDecimal.ZERO;
        total(totales, "EXENTO", exento, NORMAL);
        total(totales, "TOTAL", doc.getMontoTotal(), TOTAL);

        PdfPCell totalCell = new PdfPCell(totales);
        totalCell.setBorder(PdfPCell.NO_BORDER);
        totalCell.setPaddingLeft(8);
        pie.addCell(totalCell);
        pdf.add(pie);

        Paragraph estado = new Paragraph(
                "Estado: " + (doc.getEstado() != null ? doc.getEstado().name() : "SIN ESTADO")
                        + "  |  Documento generado electrónicamente",
                NORMAL);
        estado.setAlignment(Element.ALIGN_RIGHT);
        estado.setSpacingBefore(4);
        pdf.add(estado);
    }

    private static void cabecera(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, LABEL));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setBackgroundColor(GRIS_CABECERA);
        celda.setBorderColor(BORDE);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private static void celda(PdfPTable tabla, String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "", NORMAL));
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setBorderColor(BORDE);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private static void info(
            PdfPTable tabla, String texto, Font fuente, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "", fuente));
        celda.setBackgroundColor(fondo);
        celda.setBorderColor(BORDE);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private static void total(
            PdfPTable tabla, String etiqueta, BigDecimal valor, Font fuente) {
        PdfPCell label = new PdfPCell(new Phrase(etiqueta, fuente));
        label.setHorizontalAlignment(Element.ALIGN_RIGHT);
        label.setBorderColor(BORDE);
        label.setPadding(4);
        tabla.addCell(label);

        PdfPCell monto = new PdfPCell(new Phrase("$ " + monto(valor), fuente));
        monto.setHorizontalAlignment(Element.ALIGN_RIGHT);
        monto.setBorderColor(BORDE);
        monto.setPadding(4);
        tabla.addCell(monto);
    }

    private static Paragraph linea(String etiqueta, String valor) {
        return new Paragraph(etiqueta + " : " + valor, NORMAL);
    }

    private static Paragraph centrado(String texto, Font fuente) {
        Paragraph paragraph = new Paragraph(texto != null ? texto : "", fuente);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private static String moneda(DocumentoTributarioEntity doc) {
        String moneda = doc.getMoneda() != null ? doc.getMoneda().name() : "CLP";
        if (doc.getTipoCambio() == null || "CLP".equals(moneda)) {
            return moneda;
        }
        return moneda + "  |  T/C: " + numero(doc.getTipoCambio());
    }

    private static String direccion(String direccion, String comuna, String ciudad) {
        return String.join(", ", List.of(direccion, comuna, ciudad).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    private static String fecha(LocalDate value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
    }

    private static String numero(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "";
    }

    private static String monto(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "CL"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private static String valor(String preferido, String fallback) {
        return preferido != null && !preferido.isBlank()
                ? preferido
                : fallback != null ? fallback : "";
    }

    private static String nombreMoneda(String moneda) {
        return switch (moneda) {
            case "USD" -> "DÓLARES";
            case "EUR" -> "EUROS";
            default -> "PESOS";
        };
    }

    private static String montoEnPalabras(BigDecimal value) {
        if (value == null) {
            return "CERO";
        }
        long numero = value.setScale(0, RoundingMode.HALF_UP).longValue();
        return numeroEnPalabras(numero).toUpperCase();
    }

    private static String numeroEnPalabras(long numero) {
        if (numero == 0) return "cero";
        if (numero < 0) return "menos " + numeroEnPalabras(-numero);
        if (numero >= 1_000_000_000) {
            return numeroEnPalabras(numero / 1_000_000_000) + " mil millones"
                    + resto(numero % 1_000_000_000);
        }
        if (numero >= 1_000_000) {
            long millones = numero / 1_000_000;
            return (millones == 1 ? "un millón" : numeroEnPalabras(millones) + " millones")
                    + resto(numero % 1_000_000);
        }
        if (numero >= 1_000) {
            long miles = numero / 1_000;
            return (miles == 1 ? "mil" : numeroEnPalabras(miles) + " mil")
                    + resto(numero % 1_000);
        }
        if (numero >= 100) {
            if (numero == 100) return "cien";
            String[] centenas = {"", "ciento", "doscientos", "trescientos", "cuatrocientos",
                    "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};
            return centenas[(int) numero / 100] + resto(numero % 100);
        }
        if (numero < 30) {
            String[] menores = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis",
                    "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce",
                    "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
                    "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro",
                    "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"};
            return menores[(int) numero];
        }
        String[] decenas = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta",
                "setenta", "ochenta", "noventa"};
        long unidad = numero % 10;
        return decenas[(int) numero / 10]
                + (unidad > 0 ? " y " + numeroEnPalabras(unidad) : "");
    }

    private static String resto(long numero) {
        return numero > 0 ? " " + numeroEnPalabras(numero) : "";
    }
}
