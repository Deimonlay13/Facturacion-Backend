package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.dto.documento.importacion.FacturaTxt;
import com.gdl.facturacion_backend.dto.documento.importacion.ImportTxtPreviewResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DetalleDocumentoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.ClienteRepository;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.service.TenantService;
import com.gdl.facturacion_backend.service.TipoDocumentoService;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Importa una factura desde un TXT (formato pipe-delimitado). Puede previsualizar
 * sin persistir o crear el documento en estado BORRADOR.
 *
 * <p>El TXT no trae RUT del cliente, por lo que el cliente se resuelve por razón social
 * dentro de la empresa del solicitante.</p>
 */
@Service
public class ImportacionTxtService {

    private final TxtFacturaParser parser;
    private final ClienteRepository clienteRepository;
    private final TipoDocumentoService tipoDocumentoService;
    private final DocumentoTributarioRepository documentoRepository;
    private final CalculoMontosService calculoMontosService;
    private final ReglaTributariaResolver reglaResolver;
    private final TenantService tenantService;
    private final com.gdl.facturacion_backend.service.ArchivoService archivoService;

    public ImportacionTxtService(TxtFacturaParser parser,
                                 ClienteRepository clienteRepository,
                                 TipoDocumentoService tipoDocumentoService,
                                 DocumentoTributarioRepository documentoRepository,
                                 CalculoMontosService calculoMontosService,
                                 ReglaTributariaResolver reglaResolver,
                                 TenantService tenantService,
                                 com.gdl.facturacion_backend.service.ArchivoService archivoService) {
        this.parser = parser;
        this.clienteRepository = clienteRepository;
        this.tipoDocumentoService = tipoDocumentoService;
        this.documentoRepository = documentoRepository;
        this.calculoMontosService = calculoMontosService;
        this.reglaResolver = reglaResolver;
        this.tenantService = tenantService;
        this.archivoService = archivoService;
    }

    public ImportTxtPreviewResponse previsualizar(String contenido) {
        FacturaTxt factura = parser.parse(contenido);
        Long empresaId = tenantService.getEmpresaId();
        List<String> advertencias = new ArrayList<>();

        Optional<ClienteEntity> cliente = buscarCliente(factura, empresaId);
        String razonSocial = factura.cliente() != null ? factura.cliente().razonSocial() : null;
        if (cliente.isEmpty()) {
            advertencias.add("No se encontró un cliente con razón social '" + razonSocial
                    + "'. Debes crearlo antes de importar.");
        }

        BigDecimal tasaIva = BigDecimal.ZERO;
        try {
            tasaIva = reglaResolver.resolver(factura.codigoTipoDocumento()).tasaIva();
        } catch (RuntimeException e) {
            advertencias.add("El tipo de documento " + factura.codigoTipoDocumento()
                    + " no está soportado: " + e.getMessage());
        }
        // Documentos en moneda extranjera (USD/EUR) son exentos de IVA.
        if (!"CLP".equalsIgnoreCase(factura.moneda())) {
            tasaIva = BigDecimal.ZERO;
        }

        List<ImportTxtPreviewResponse.DetallePreview> detalles = new ArrayList<>();
        BigDecimal neto = BigDecimal.ZERO;
        for (FacturaTxt.Detalle d : factura.detalles()) {
            BigDecimal subtotal = subtotal(d.cantidad(), d.precioUnitario());
            neto = neto.add(subtotal);
            detalles.add(new ImportTxtPreviewResponse.DetallePreview(
                    d.numero(), d.codigoProducto(), descripcionItem(d),
                    d.cantidad(), d.precioUnitario(), subtotal));
        }
        neto = neto.setScale(0, RoundingMode.HALF_UP);
        BigDecimal iva = neto.multiply(tasaIva).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = neto.add(iva);

        return new ImportTxtPreviewResponse(
                factura.codigoTipoDocumento(),
                cliente.map(ClienteEntity::getId).orElse(null),
                razonSocial,
                cliente.isPresent(),
                factura.fechaEmision(),
                factura.fechaVencimiento(),
                factura.condicionPago(),
                factura.moneda(),
                factura.tipoCambio(),
                factura.observacion(),
                detalles,
                neto, iva, total,
                advertencias);
    }

    @Transactional
    public DocumentoTributarioEntity importar(String contenido) {
        FacturaTxt factura = parser.parse(contenido);
        Long empresaId = tenantService.getEmpresaId();

        String razonSocial = factura.cliente() != null ? factura.cliente().razonSocial() : null;
        ClienteEntity cliente = buscarCliente(factura, empresaId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró un cliente con razón social '"
                        + razonSocial + "'. Créalo antes de importar el TXT."));

        TipoDocumentoEntity tipo = tipoDocumentoService.findByCodigoSii(factura.codigoTipoDocumento());

        DocumentoTributarioEntity documento = new DocumentoTributarioEntity();
        documento.setTipoDocumento(tipo);
        documento.setCliente(cliente);
        documento.setFechaEmision(factura.fechaEmision() != null ? factura.fechaEmision() : LocalDate.now());
        documento.setFechaVencimiento(factura.fechaVencimiento());
        documento.setObservaciones(factura.observacion());
        documento.setMoneda(parseMoneda(factura.moneda()));
        documento.setTipoCambio(factura.tipoCambio() != null ? factura.tipoCambio() : BigDecimal.ONE);
        documento.setEstado(EstadoDocumento.BORRADOR);
        documento.setEstadoSii(EstadoDocumentoSii.PENDIENTE);
        documento.setXmlFirmado(false);
        documento.setFolio(null);

        List<DetalleDocumentoEntity> detalles = new ArrayList<>();
        for (FacturaTxt.Detalle d : factura.detalles()) {
            DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
            detalle.setDocumento(documento);
            detalle.setDescripcionItem(descripcionItem(d));
            detalle.setCantidad(d.cantidad());
            detalle.setPrecioUnitario(d.precioUnitario());
            detalles.add(detalle);
        }
        documento.setDetalles(detalles);

        calculoMontosService.recalcular(documento);
        // Documentos en moneda extranjera (USD/EUR) son exentos de IVA.
        if (documento.getMoneda() != Moneda.CLP) {
            documento.setMontoIva(java.math.BigDecimal.ZERO);
            documento.setMontoTotal(documento.getMontoNeto());
        }
        DocumentoTributarioEntity documentoGuardado = documentoRepository.save(documento);
        
        // Guardar el TXT original como archivo
        try {
            archivoService.guardarTxt(documentoGuardado.getId(), contenido);
        } catch (Exception e) {
            // Log pero no falla la importación si no se puede guardar el archivo
            System.err.println("Advertencia: No se pudo guardar el TXT: " + e.getMessage());
        }
        
        return documentoGuardado;
    }

    // ------------------------------------------------------------- helpers
    private Optional<ClienteEntity> buscarCliente(FacturaTxt factura, Long empresaId) {
        if (factura.cliente() == null || factura.cliente().razonSocial() == null) {
            return Optional.empty();
        }
        return clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                empresaId, factura.cliente().razonSocial().trim());
    }

    private String descripcionItem(FacturaTxt.Detalle d) {
        if (d.codigoProducto() != null && d.descripcion() != null) {
            return d.codigoProducto() + " - " + d.descripcion();
        }
        return d.descripcion() != null ? d.descripcion() : d.codigoProducto();
    }

    private BigDecimal subtotal(BigDecimal cantidad, BigDecimal precio) {
        BigDecimal c = cantidad != null ? cantidad : BigDecimal.ZERO;
        BigDecimal p = precio != null ? precio : BigDecimal.ZERO;
        return c.multiply(p).setScale(0, RoundingMode.HALF_UP);
    }

    private Moneda parseMoneda(String moneda) {
        try {
            return moneda != null ? Moneda.valueOf(moneda) : Moneda.CLP;
        } catch (IllegalArgumentException e) {
            return Moneda.CLP;
        }
    }
}
