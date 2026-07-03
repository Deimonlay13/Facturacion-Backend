package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ReferenciaDocumentoEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import com.gdl.facturacion_backend.service.TenantService;
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
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;

/**
 * Genera representaciones descargables (XML tipo DTE simulado y PDF) de un documento.
 * Para documentos en BORRADOR (sin snapshot) usa los datos vivos de cliente y empresa.
 */
@Service
public class DocumentoExportService {

    private final DocumentoTributarioService documentoService;
    private final EmpresaRepository empresaRepository;
    private final TenantService tenantService;
    private final com.gdl.facturacion_backend.service.ArchivoService archivoService;

    public DocumentoExportService(DocumentoTributarioService documentoService,
                                  EmpresaRepository empresaRepository,
                                  TenantService tenantService,
                                  com.gdl.facturacion_backend.service.ArchivoService archivoService) {
        this.documentoService = documentoService;
        this.empresaRepository = empresaRepository;
        this.tenantService = tenantService;
        this.archivoService = archivoService;
    }

    // ============================================================== XML
    public byte[] generarXml(Long id) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(id);
        EmpresaEntity emisor = emisor();
        ClienteEntity receptor = doc.getCliente();

        try {
            org.w3c.dom.Document xml = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();

            org.w3c.dom.Element dte = xml.createElement("DTE");
            dte.setAttribute("version", "1.0");
            xml.appendChild(dte);

            org.w3c.dom.Element documento = xml.createElement("Documento");
            dte.appendChild(documento);

            org.w3c.dom.Element encabezado = xml.createElement("Encabezado");
            documento.appendChild(encabezado);

            org.w3c.dom.Element idDoc = child(xml, encabezado, "IdDoc");
            text(xml, idDoc, "TipoDTE", String.valueOf(doc.getTipoDocumento().getCodigoSii()));
            text(xml, idDoc, "Folio", doc.getFolio() != null ? String.valueOf(doc.getFolio()) : "0");
            text(xml, idDoc, "FchEmis", str(doc.getFechaEmision()));
            text(xml, idDoc, "FchVenc", str(doc.getFechaVencimiento()));
            text(xml, idDoc, "Estado", doc.getEstado() != null ? doc.getEstado().name() : "");
            text(xml, idDoc, "MntMoneda", doc.getMoneda() != null ? doc.getMoneda().name() : "CLP");
            text(xml, idDoc, "TpoCambio", str(doc.getTipoCambio()));

            org.w3c.dom.Element emisorEl = child(xml, encabezado, "Emisor");
            text(xml, emisorEl, "RUTEmisor", val(doc.getRutEmisor(), emisor.getRutEmpresa()));
            text(xml, emisorEl, "RznSoc", val(doc.getRazonSocialEmisor(), emisor.getRazonSocial()));
            text(xml, emisorEl, "GiroEmis", val(doc.getGiroEmisor(), emisor.getGiro()));
            text(xml, emisorEl, "DirOrigen", val(doc.getDireccionEmisor(), emisor.getDireccion()));
            text(xml, emisorEl, "CmnaOrigen", val(doc.getComunaEmisor(), emisor.getComuna()));
            text(xml, emisorEl, "CiudadOrigen", val(doc.getCiudadEmisor(), emisor.getCiudad()));

            org.w3c.dom.Element receptorEl = child(xml, encabezado, "Receptor");
            text(xml, receptorEl, "RUTRecep", val(doc.getRut(), receptor != null ? receptor.getRut() : null));
            text(xml, receptorEl, "RznSocRecep", val(doc.getRazonSocial(), receptor != null ? receptor.getRazonSocial() : null));
            text(xml, receptorEl, "GiroRecep", val(doc.getGiro(), receptor != null ? receptor.getGiro() : null));
            text(xml, receptorEl, "DirRecep", val(doc.getDireccion(), receptor != null ? receptor.getDireccion() : null));
            text(xml, receptorEl, "CmnaRecep", val(doc.getComuna(), receptor != null ? receptor.getComuna() : null));
            text(xml, receptorEl, "CiudadRecep", val(doc.getCiudad(), receptor != null ? receptor.getCiudad() : null));

            org.w3c.dom.Element totales = child(xml, encabezado, "Totales");
            text(xml, totales, "MntNeto", str(doc.getMontoNeto()));
            text(xml, totales, "IVA", str(doc.getMontoIva()));
            text(xml, totales, "MntTotal", str(doc.getMontoTotal()));

            List<DetalleDocumentoEntity> detalles = doc.getDetalles() != null ? doc.getDetalles() : List.of();
            int nro = 1;
            for (DetalleDocumentoEntity d : detalles) {
                org.w3c.dom.Element detalle = child(xml, documento, "Detalle");
                text(xml, detalle, "NroLinDet", String.valueOf(nro++));
                text(xml, detalle, "NmbItem", val(d.getDescripcionItem(), ""));
                text(xml, detalle, "QtyItem", str(d.getCantidad()));
                text(xml, detalle, "PrcItem", str(d.getPrecioUnitario()));
                text(xml, detalle, "MontoItem", str(d.getSubtotal()));
            }

            if (doc.getObservaciones() != null) {
                text(xml, documento, "Observaciones", doc.getObservaciones());
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(xml), new StreamResult(out));
            byte[] xmlBytes = out.toByteArray();
            
            // Guardar XML en tabla archivos
            try {
                archivoService.guardarXml(id, xmlBytes);
            } catch (Exception e) {
                System.err.println("Advertencia: No se pudo guardar el XML: " + e.getMessage());
            }
            
            return xmlBytes;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el XML del documento: " + e.getMessage(), e);
        }
    }

    // ============================================================== PDF
    public byte[] generarPdf(Long id) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(id);
        byte[] pdfBytes = DocumentoPdfGenerator.generar(doc, emisor());
        
        // Guardar PDF en tabla archivos
        try {
            archivoService.guardarPdf(id, pdfBytes);
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo guardar el PDF: " + e.getMessage());
        }
        
        return pdfBytes;
    }

    // ============================================================== TXT
    public byte[] generarTxt(Long id) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(id);
        StringBuilder txt = new StringBuilder();

        Integer codigoTipo = doc.getTipoDocumento() != null ? doc.getTipoDocumento().getCodigoSii() : null;
        ClienteEntity cliente = doc.getCliente();
        String numeroDocumento = prefijoDocumento(codigoTipo)
                + (doc.getFolio() != null ? doc.getFolio() : doc.getId());

        txt.append("|CLIENTE|")
                .append(valor(codigoTipo))
                .append('|').append(campo(numeroDocumento))
                .append('|').append(campo(valor(doc.getRut(), cliente != null ? cliente.getRut() : null)))
                .append('|').append(campo(valor(doc.getRazonSocial(), cliente != null ? cliente.getRazonSocial() : null)))
                .append('|').append(campo(valor(doc.getDireccion(), cliente != null ? cliente.getDireccion() : null)))
                .append('|').append(campo(valor(doc.getPais(), cliente != null ? cliente.getPais() : null)))
                .append('|')
                .append('|').append(campo(valor(doc.getCiudad(), cliente != null ? cliente.getCiudad() : null)))
                .append('|').append(campo(cliente != null ? cliente.getTelefono() : null))
                .append('|').append('\n');

        txt.append("|CABECERA|")
                .append(fechaTxt(doc.getFechaEmision()))
                .append('|').append(fechaTxt(doc.getFechaVencimiento()))
                .append('|')
                .append('|').append(numeroTxt(doc.getTipoCambio()))
                .append('|').append('\n');

        txt.append("|OBSERVACION|")
                .append(campo(doc.getObservaciones()))
                .append("|||||").append('\n');

        List<DetalleDocumentoEntity> detalles = doc.getDetalles() != null ? doc.getDetalles() : List.of();
        int linea = 1;
        for (DetalleDocumentoEntity detalle : detalles) {
            String codigoProducto = detalle.getProducto() != null ? detalle.getProducto().getCodigo() : "";
            txt.append("|DETALLE|")
                    .append(linea++)
                    .append("|||")
                    .append(campo(codigoProducto))
                    .append('|').append(campo(detalle.getDescripcionItem()))
                    .append('|').append(numeroTxt(detalle.getCantidad()))
                    .append('|').append(numeroTxt(detalle.getPrecioUnitario()))
                    .append('|').append('\n');
        }

        return txt.toString().getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private byte[] generarPdfLegacy(Long id) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(id);
        EmpresaEntity emisor = emisor();
        ClienteEntity receptor = doc.getCliente();

        Color rojoSii = new Color(190, 30, 45);
        Color gris = new Color(235, 238, 241);
        Color borde = new Color(125, 125, 125);
        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
        Font siiFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, rojoSii);
        Font folioFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, rojoSii);

        Document pdf = new Document(PageSize.A4, 28, 28, 25, 25);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(pdf, out);
        pdf.open();

        String titulo = doc.getTipoDocumento().getDescripcion() != null
                ? doc.getTipoDocumento().getDescripcion()
                : "Documento " + doc.getTipoDocumento().getCodigoSii();
        String folio = doc.getFolio() != null ? String.valueOf(doc.getFolio()) : "BORRADOR";

        PdfPTable encabezado = new PdfPTable(new float[]{1.7f, 1f});
        encabezado.setWidthPercentage(100);
        PdfPCell datosEmisor = new PdfPCell();
        datosEmisor.setBorder(PdfPCell.NO_BORDER);
        datosEmisor.setPaddingRight(16);
        datosEmisor.addElement(new Paragraph(
                val(doc.getNombreFantasiaEmisor(),
                        val(doc.getRazonSocialEmisor(), emisor.getRazonSocial())), h1));
        datosEmisor.addElement(new Paragraph(val(doc.getGiroEmisor(), emisor.getGiro()), normal));
        datosEmisor.addElement(new Paragraph(
                val(doc.getDireccionEmisor(), emisor.getDireccion()) + ", "
                        + val(doc.getComunaEmisor(), emisor.getComuna()) + ", "
                        + val(doc.getCiudadEmisor(), emisor.getCiudad()), normal));
        datosEmisor.addElement(new Paragraph(
                "TELÉFONO: " + val(doc.getTelefonoEmisor(), emisor.getTelefono()), normal));
        datosEmisor.addElement(new Paragraph(
                "CORREO: " + val(doc.getEmailPrincipalEmisor(), emisor.getEmailPrincipal()), normal));
        datosEmisor.addElement(new Paragraph("WEB: " + val(emisor.getSitioWeb(), ""), normal));
        encabezado.addCell(datosEmisor);

        PdfPCell sii = new PdfPCell();
        sii.setBorderColor(rojoSii);
        sii.setBorderWidth(2);
        sii.setPadding(8);
        sii.addElement(centrado("R.U.T.: " + val(doc.getRutEmisor(), emisor.getRutEmpresa()), siiFont));
        sii.addElement(centrado(titulo.toUpperCase(), siiFont));
        sii.addElement(centrado("N° " + folio, folioFont));
        encabezado.addCell(sii);
        pdf.add(encabezado);

        Paragraph oficina = new Paragraph("S.I.I. - DOCUMENTO TRIBUTARIO ELECTRÓNICO", label);
        oficina.setAlignment(Element.ALIGN_RIGHT);
        oficina.setSpacingBefore(3);
        oficina.setSpacingAfter(8);
        pdf.add(oficina);

        PdfPTable ficha = new PdfPTable(new float[]{1f, 2.3f, 1f, 1.35f});
        ficha.setWidthPercentage(100);
        ficha.setSpacingAfter(8);
        infoCell(ficha, "SEÑORES", label, gris, borde);
        infoCell(ficha, val(doc.getRazonSocial(),
                receptor != null ? receptor.getRazonSocial() : ""), normal, Color.WHITE, borde);
        infoCell(ficha, "R.U.T.", label, gris, borde);
        infoCell(ficha, val(doc.getRut(), receptor != null ? receptor.getRut() : ""),
                normal, Color.WHITE, borde);
        infoCell(ficha, "GIRO", label, gris, borde);
        infoCell(ficha, val(doc.getGiro(), receptor != null ? receptor.getGiro() : ""),
                normal, Color.WHITE, borde);
        infoCell(ficha, "FECHA EMISIÓN", label, gris, borde);
        infoCell(ficha, fecha(doc.getFechaEmision()), normal, Color.WHITE, borde);
        infoCell(ficha, "DIRECCIÓN", label, gris, borde);
        infoCell(ficha, val(doc.getDireccion(), receptor != null ? receptor.getDireccion() : ""),
                normal, Color.WHITE, borde);
        infoCell(ficha, "VENCIMIENTO", label, gris, borde);
        infoCell(ficha, fecha(doc.getFechaVencimiento()), normal, Color.WHITE, borde);
        infoCell(ficha, "COMUNA / CIUDAD", label, gris, borde);
        infoCell(ficha,
                val(doc.getComuna(), receptor != null ? receptor.getComuna() : "") + " / "
                        + val(doc.getCiudad(), receptor != null ? receptor.getCiudad() : ""),
                normal, Color.WHITE, borde);
        infoCell(ficha, "MONEDA", label, gris, borde);
        infoCell(ficha, doc.getMoneda() != null ? doc.getMoneda().name() : "CLP",
                normal, Color.WHITE, borde);
        pdf.add(ficha);

        PdfPTable table = new PdfPTable(new float[]{1.15f, 4.8f, 1f, 0.8f, 1.35f, 1.35f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(7);
        headerCell(table, "CÓDIGO", label, gris, borde);
        headerCell(table, "DESCRIPCIÓN", label, gris, borde);
        headerCell(table, "CANTIDAD", label, gris, borde);
        headerCell(table, "UNM", label, gris, borde);
        headerCell(table, "PRC. UNITARIO", label, gris, borde);
        headerCell(table, "TOTAL", label, gris, borde);

        List<DetalleDocumentoEntity> detalles = doc.getDetalles() != null ? doc.getDetalles() : List.of();
        for (DetalleDocumentoEntity d : detalles) {
            String codigo = d.getProducto() != null ? val(d.getProducto().getCodigo(), "") : "";
            cell(table, codigo, normal, Element.ALIGN_CENTER, borde);
            cell(table, val(d.getDescripcionItem(), ""), normal, Element.ALIGN_LEFT, borde);
            cell(table, numero(d.getCantidad()), normal, Element.ALIGN_RIGHT, borde);
            cell(table, val(d.getUnidadMedida(), "UNI"), normal, Element.ALIGN_CENTER, borde);
            cell(table, monto(d.getPrecioUnitario()), normal, Element.ALIGN_RIGHT, borde);
            cell(table, monto(d.getSubtotal()), normal, Element.ALIGN_RIGHT, borde);
        }
        pdf.add(table);

        if (doc.getObservaciones() != null && !doc.getObservaciones().isBlank()) {
            pdf.add(new Paragraph("Observaciones:", label));
            Paragraph observaciones = new Paragraph(doc.getObservaciones(), normal);
            observaciones.setSpacingAfter(6);
            pdf.add(observaciones);
        }

        List<ReferenciaDocumentoEntity> referencias =
                doc.getReferencias() != null ? doc.getReferencias() : List.of();
        if (!referencias.isEmpty()) {
            pdf.add(new Paragraph("Referencias a otros documentos", label));
            PdfPTable refs = new PdfPTable(new float[]{2f, 1f, 1.2f, 2.5f});
            refs.setWidthPercentage(100);
            refs.setSpacingBefore(3);
            refs.setSpacingAfter(7);
            headerCell(refs, "TIPO DOCUMENTO", label, gris, borde);
            headerCell(refs, "FOLIO", label, gris, borde);
            headerCell(refs, "FECHA", label, gris, borde);
            headerCell(refs, "RAZÓN REFERENCIA", label, gris, borde);
            for (ReferenciaDocumentoEntity referencia : referencias) {
                DocumentoTributarioEntity referido = referencia.getDocumentoReferenciado();
                cell(refs,
                        referido != null && referido.getTipoDocumento() != null
                                ? referido.getTipoDocumento().getDescripcion()
                                : val(referencia.getTipoReferencia(), ""),
                        normal, Element.ALIGN_LEFT, borde);
                cell(refs, referido != null && referido.getFolio() != null
                                ? String.valueOf(referido.getFolio()) : "",
                        normal, Element.ALIGN_CENTER, borde);
                cell(refs, referido != null ? fecha(referido.getFechaEmision()) : "",
                        normal, Element.ALIGN_CENTER, borde);
                cell(refs, val(referencia.getMotivo(), ""), normal, Element.ALIGN_LEFT, borde);
            }
            pdf.add(refs);
        }

        PdfPTable pie = new PdfPTable(new float[]{1.65f, 1f});
        pie.setWidthPercentage(100);
        PdfPCell timbre = new PdfPCell();
        timbre.setBorderColor(borde);
        timbre.setPadding(8);
        timbre.addElement(centrado("TIMBRE ELECTRÓNICO S.I.I.", label));
        timbre.addElement(centrado(
                "Documento tributario electrónico\nVerifique documento: www.sii.cl\n\n"
                        + "FECHA RECEPCIÓN: ____________________\n"
                        + "NOMBRE: _____________________________\n"
                        + "RUT: __________________  FIRMA: __________________", normal));
        pie.addCell(timbre);

        PdfPTable totales = new PdfPTable(new float[]{1.2f, 1f});
        totales.setWidthPercentage(100);
        totalRow(totales, "SUBTOTAL", doc.getMontoNeto(), normal, borde);
        totalRow(totales, "NETO", doc.getMontoNeto(), normal, borde);
        totalRow(totales, "IVA", doc.getMontoIva(), normal, borde);
        BigDecimal exento = doc.getMontoIva() == null || doc.getMontoIva().compareTo(BigDecimal.ZERO) == 0
                ? doc.getMontoTotal() : BigDecimal.ZERO;
        totalRow(totales, "EXENTO", exento, normal, borde);
        totalRow(totales, "TOTAL", doc.getMontoTotal(), label, borde);
        PdfPCell totalCell = new PdfPCell(totales);
        totalCell.setBorder(PdfPCell.NO_BORDER);
        totalCell.setPaddingLeft(8);
        pie.addCell(totalCell);
        pdf.add(pie);

        Paragraph son = new Paragraph(
                "SON: " + montoEnPalabras(doc.getMontoTotal()) + " "
                        + (doc.getMoneda() != null ? doc.getMoneda().name() : "CLP"),
                label);
        son.setSpacingBefore(5);
        pdf.add(son);

        pdf.close();
        return out.toByteArray();
    }

    // ----------------------------------------------------------- helpers
    private EmpresaEntity emisor() {
        return empresaRepository.findById(tenantService.getEmpresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa emisora no encontrada"));
    }

    private org.w3c.dom.Element child(org.w3c.dom.Document xml, org.w3c.dom.Element parent, String name) {
        org.w3c.dom.Element el = xml.createElement(name);
        parent.appendChild(el);
        return el;
    }

    private void text(org.w3c.dom.Document xml, org.w3c.dom.Element parent, String name, String value) {
        org.w3c.dom.Element el = xml.createElement(name);
        el.setTextContent(value != null ? value : "");
        parent.appendChild(el);
    }

    private void headerCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void headerCell(PdfPTable table, String text, Font font, Color background, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void cell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void cell(PdfPTable table, String text, Font font, int align, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(border);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void infoCell(PdfPTable table, String text, Font font, Color background, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(background);
        cell.setBorderColor(border);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void totalRow(PdfPTable table, String labelText, BigDecimal value, Font font, Color border) {
        cell(table, labelText, font, Element.ALIGN_RIGHT, border);
        cell(table, "$ " + monto(value), font, Element.ALIGN_RIGHT, border);
    }

    private Paragraph centrado(String text, Font font) {
        Paragraph paragraph = new Paragraph(text != null ? text : "", font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private String fecha(LocalDate value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
    }

    private String fechaTxt(LocalDate value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "";
    }

    private String numero(BigDecimal value) {
        if (value == null) return "";
        return value.stripTrailingZeros().toPlainString();
    }

    private String numeroTxt(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "";
    }

    private String monto(BigDecimal value) {
        if (value == null) return "0";
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "CL"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private String montoEnPalabras(BigDecimal value) {
        if (value == null) return "CERO";
        long numero = value.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        return numeroEnPalabras(numero).toUpperCase();
    }

    private String numeroEnPalabras(long numero) {
        if (numero == 0) return "cero";
        if (numero < 0) return "menos " + numeroEnPalabras(-numero);
        if (numero >= 1_000_000_000) {
            long milesMillones = numero / 1_000_000_000;
            return numeroEnPalabras(milesMillones) + " mil millones"
                    + resto(numero % 1_000_000_000);
        }
        if (numero >= 1_000_000) {
            long millones = numero / 1_000_000;
            String prefijo = millones == 1 ? "un millón" : numeroEnPalabras(millones) + " millones";
            return prefijo + resto(numero % 1_000_000);
        }
        if (numero >= 1_000) {
            long miles = numero / 1_000;
            String prefijo = miles == 1 ? "mil" : numeroEnPalabras(miles) + " mil";
            return prefijo + resto(numero % 1_000);
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

    private String resto(long numero) {
        return numero > 0 ? " " + numeroEnPalabras(numero) : "";
    }

    private Paragraph alineadoDerecha(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private String val(String preferido, String fallback) {
        if (preferido != null && !preferido.isBlank()) return preferido;
        return fallback != null ? fallback : "";
    }

    private String valor(String value) {
        return value != null ? value : "";
    }

    private String valor(Integer value) {
        return value != null ? value.toString() : "";
    }

    private String valor(String preferido, String fallback) {
        return preferido != null && !preferido.isBlank()
                ? preferido
                : fallback != null ? fallback : "";
    }

    private String campo(String value) {
        return value != null
                ? value.replace('|', ' ').replace('\r', ' ').replace('\n', ' ').trim()
                : "";
    }

    private String prefijoDocumento(Integer codigoTipo) {
        if (codigoTipo == null) {
            return "D";
        }
        return switch (codigoTipo) {
            case 33 -> "FA";
            case 34 -> "FE";
            case 56 -> "ND";
            case 61 -> "NC";
            default -> "D" + codigoTipo + "-";
        };
    }

    private String str(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal bd) return bd.toPlainString();
        return value.toString();
    }
}
