package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
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

    public DocumentoExportService(DocumentoTributarioService documentoService,
                                  EmpresaRepository empresaRepository,
                                  TenantService tenantService) {
        this.documentoService = documentoService;
        this.empresaRepository = empresaRepository;
        this.tenantService = tenantService;
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
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el XML del documento: " + e.getMessage(), e);
        }
    }

    // ============================================================== PDF
    public byte[] generarPdf(Long id) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(id);
        EmpresaEntity emisor = emisor();
        ClienteEntity receptor = doc.getCliente();

        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        Document pdf = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(pdf, out);
        pdf.open();

        String titulo = doc.getTipoDocumento().getDescripcion() != null
                ? doc.getTipoDocumento().getDescripcion()
                : "Documento " + doc.getTipoDocumento().getCodigoSii();
        String folio = doc.getFolio() != null ? "N° " + doc.getFolio() : "(BORRADOR sin folio)";
        Paragraph tituloPar = new Paragraph(titulo + "  " + folio, h1);
        tituloPar.setAlignment(Element.ALIGN_CENTER);
        pdf.add(tituloPar);
        pdf.add(new Paragraph(" ", normal));

        pdf.add(new Paragraph("EMISOR", label));
        pdf.add(new Paragraph(val(doc.getRazonSocialEmisor(), emisor.getRazonSocial()), normal));
        pdf.add(new Paragraph("RUT: " + val(doc.getRutEmisor(), emisor.getRutEmpresa()), normal));
        pdf.add(new Paragraph("Giro: " + val(doc.getGiroEmisor(), emisor.getGiro()), normal));
        pdf.add(new Paragraph("Direccion: " + val(doc.getDireccionEmisor(), emisor.getDireccion()), normal));
        pdf.add(new Paragraph(" ", normal));

        pdf.add(new Paragraph("RECEPTOR", label));
        pdf.add(new Paragraph(val(doc.getRazonSocial(), receptor != null ? receptor.getRazonSocial() : ""), normal));
        pdf.add(new Paragraph("RUT: " + val(doc.getRut(), receptor != null ? receptor.getRut() : ""), normal));
        pdf.add(new Paragraph("Direccion: " + val(doc.getDireccion(), receptor != null ? receptor.getDireccion() : ""), normal));
        pdf.add(new Paragraph("Fecha emision: " + str(doc.getFechaEmision())
                + "    Vencimiento: " + str(doc.getFechaVencimiento()), normal));
        pdf.add(new Paragraph("Moneda: " + (doc.getMoneda() != null ? doc.getMoneda().name() : "CLP"), normal));
        pdf.add(new Paragraph(" ", normal));

        PdfPTable table = new PdfPTable(new float[]{0.8f, 5f, 1.5f, 2f, 2f});
        table.setWidthPercentage(100);
        headerCell(table, "#", label);
        headerCell(table, "Descripcion", label);
        headerCell(table, "Cant.", label);
        headerCell(table, "P. Unit.", label);
        headerCell(table, "Subtotal", label);

        List<DetalleDocumentoEntity> detalles = doc.getDetalles() != null ? doc.getDetalles() : List.of();
        int nro = 1;
        for (DetalleDocumentoEntity d : detalles) {
            cell(table, String.valueOf(nro++), normal, Element.ALIGN_CENTER);
            cell(table, val(d.getDescripcionItem(), ""), normal, Element.ALIGN_LEFT);
            cell(table, str(d.getCantidad()), normal, Element.ALIGN_RIGHT);
            cell(table, str(d.getPrecioUnitario()), normal, Element.ALIGN_RIGHT);
            cell(table, str(d.getSubtotal()), normal, Element.ALIGN_RIGHT);
        }
        pdf.add(table);
        pdf.add(new Paragraph(" ", normal));

        pdf.add(alineadoDerecha("Neto: " + str(doc.getMontoNeto()), normal));
        pdf.add(alineadoDerecha("IVA: " + str(doc.getMontoIva()), normal));
        pdf.add(alineadoDerecha("TOTAL: " + str(doc.getMontoTotal()), label));

        if (doc.getObservaciones() != null && !doc.getObservaciones().isBlank()) {
            pdf.add(new Paragraph(" ", normal));
            pdf.add(new Paragraph("Observaciones:", label));
            pdf.add(new Paragraph(doc.getObservaciones(), normal));
        }

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

    private void cell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
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

    private String str(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal bd) return bd.toPlainString();
        return value.toString();
    }
}
