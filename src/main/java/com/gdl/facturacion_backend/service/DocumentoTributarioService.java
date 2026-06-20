package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.documento.DetalleCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.GuiaDespachoRequest;
import com.gdl.facturacion_backend.dto.documento.ReferenciaCreateRequest;
import com.gdl.facturacion_backend.entity.*;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.repository.GuiaDespachoExtraRepository;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.repository.ReferenciaDocumentoRepository;
import com.gdl.facturacion_backend.service.documento.CalculoMontosService;
import com.gdl.facturacion_backend.service.documento.ReglaTributariaResolver;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio raíz del agregado Documento Tributario. Gestiona el ciclo BORRADOR → EMITIDO.
 * Multi-tenant vía {@link BaseTenantService}.
 */
@Service
public class DocumentoTributarioService extends BaseTenantService<DocumentoTributarioEntity> {

    private static final int CODIGO_GUIA_DESPACHO = 52;

    private final DocumentoTributarioRepository documentoRepository;
    private final TipoDocumentoService tipoDocumentoService;
    private final ClienteService clienteService;
    private final ProductoRepository productoRepository;
    private final ReferenciaDocumentoRepository referenciaRepository;
    private final GuiaDespachoExtraRepository guiaRepository;
    private final CalculoMontosService calculoMontosService;
    private final ReglaTributariaResolver reglaResolver;
    private final FolioService folioService;

    public DocumentoTributarioService(DocumentoTributarioRepository documentoRepository,
                                      TenantService tenantService,
                                      TipoDocumentoService tipoDocumentoService,
                                      ClienteService clienteService,
                                      ProductoRepository productoRepository,
                                      ReferenciaDocumentoRepository referenciaRepository,
                                      GuiaDespachoExtraRepository guiaRepository,
                                      CalculoMontosService calculoMontosService,
                                      ReglaTributariaResolver reglaResolver,
                                      FolioService folioService) {
        super(documentoRepository, tenantService);
        this.documentoRepository = documentoRepository;
        this.tipoDocumentoService = tipoDocumentoService;
        this.clienteService = clienteService;
        this.productoRepository = productoRepository;
        this.referenciaRepository = referenciaRepository;
        this.guiaRepository = guiaRepository;
        this.calculoMontosService = calculoMontosService;
        this.reglaResolver = reglaResolver;
        this.folioService = folioService;
    }

    /** Crea el documento base en estado BORRADOR. No asigna folio ni calcula montos. */
    public DocumentoTributarioEntity crear(DocumentoCreateRequest request) {
        TipoDocumentoEntity tipo = tipoDocumentoService.findByCodigoSii(request.getCodigoTipoDocumento());
        ClienteEntity cliente = clienteService.obtenerPorId(request.getClienteId());

        DocumentoTributarioEntity documento = new DocumentoTributarioEntity();
        documento.setTipoDocumento(tipo);
        documento.setCliente(cliente);
        documento.setFechaEmision(LocalDate.now());
        documento.setEstado(EstadoDocumento.BORRADOR);
        documento.setEstadoSii(EstadoDocumentoSii.PENDIENTE);
        documento.setXmlFirmado(false);
        documento.setFolio(null);

        return save(documento);
    }

    public List<DocumentoTributarioEntity> consultar(Long clienteId, Integer codigoTipo,
                                                     String estado, LocalDate desde, LocalDate hasta) {
        return documentoRepository.buscar(getEmpresaId(), clienteId, codigoTipo,
                parseEstado(estado), desde, hasta);
    }

    @Transactional(readOnly = true)
    public DocumentoTributarioEntity obtenerDetalle(Long id) {
        DocumentoTributarioEntity documento = cargar(id);
        precargar(documento);
        return documento;
    }

    public DocumentoTributarioEntity obtenerPorId(Long id) {
        return cargar(id);
    }

    @Transactional
    public DocumentoTributarioEntity agregarDetalle(Long documentoId, DetalleCreateRequest request) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);

        DetalleDocumentoEntity detalle = new DetalleDocumentoEntity();
        detalle.setDocumento(documento);
        if (request.getProductoId() != null) {
            ProductoEntity producto = productoRepository
                    .findByIdAndEmpresaId(request.getProductoId(), getEmpresaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto no encontrado con id: " + request.getProductoId()));
            detalle.setProducto(producto);
        }
        detalle.setDescripcionItem(request.getDescripcion());
        detalle.setCantidad(request.getCantidad());
        detalle.setUnidadMedida(request.getUnidadMedida());
        detalle.setPrecioUnitario(request.getPrecioUnitario());
        detalle.setSubtotal(calculoMontosService.calcularSubtotal(detalle));

        if (documento.getDetalles() == null) {
            documento.setDetalles(new ArrayList<>());
        }
        documento.getDetalles().add(detalle);

        calculoMontosService.recalcular(documento);
        DocumentoTributarioEntity guardado = save(documento);
        precargar(guardado);
        return guardado;
    }

    @Transactional
    public DocumentoTributarioEntity eliminarDetalle(Long documentoId, Long detalleId) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);

        boolean eliminado = documento.getDetalles() != null
                && documento.getDetalles().removeIf(d -> d.getId() != null && d.getId().equals(detalleId));
        if (!eliminado) {
            throw new RecursoNoEncontradoException("Detalle no encontrado con id: " + detalleId);
        }

        calculoMontosService.recalcular(documento);
        DocumentoTributarioEntity guardado = save(documento);
        precargar(guardado);
        return guardado;
    }

    @Transactional
    public DocumentoTributarioEntity agregarReferencia(Long documentoId, ReferenciaCreateRequest request) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);
        DocumentoTributarioEntity referenciado = cargar(request.getDocumentoReferenciadoId());
        if (referenciado.getId().equals(documento.getId())) {
            throw new ReglaNegocioException("Un documento no puede referenciarse a sí mismo");
        }

        ReferenciaDocumentoEntity referencia = new ReferenciaDocumentoEntity();
        referencia.setDocumento(documento);
        referencia.setDocumentoReferenciado(referenciado);
        referencia.setTipoReferencia(request.getTipoReferencia());
        referencia.setMotivo(request.getMotivo());
        ReferenciaDocumentoEntity guardada = referenciaRepository.save(referencia);

        if (documento.getReferencias() == null) {
            documento.setReferencias(new ArrayList<>());
        }
        documento.getReferencias().add(guardada);
        precargar(documento);
        return documento;
    }

    @Transactional
    public DocumentoTributarioEntity guardarGuiaDespacho(Long documentoId, GuiaDespachoRequest request) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);
        if (documento.getTipoDocumento() == null
                || !Integer.valueOf(CODIGO_GUIA_DESPACHO).equals(documento.getTipoDocumento().getCodigoSii())) {
            throw new ReglaNegocioException("Los datos de guía de despacho solo aplican a documentos tipo 52");
        }

        GuiaDespachoExtraEntity guia = guiaRepository
                .findByDocumentoIdAndEmpresaId(documentoId, getEmpresaId())
                .orElseGet(() -> {
                    GuiaDespachoExtraEntity nueva = new GuiaDespachoExtraEntity();
                    nueva.setDocumento(documento);
                    return nueva;
                });
        guia.setTipoTraslado(request.getTipoTraslado());
        guia.setPatente(request.getPatente());
        guia.setRutTransportista(request.getRutTransportista());
        guia.setNombreTransportista(request.getNombreTransportista());
        guia.setDireccionDestino(request.getDireccionDestino());

        documento.setGuiaDespacho(guiaRepository.save(guia));
        precargar(documento);
        return documento;
    }

    @Transactional
    public DocumentoTributarioEntity emitir(Long documentoId) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);
        precargar(documento);

        if (documento.getCliente() == null) {
            throw new ReglaNegocioException("El documento debe tener un cliente asociado");
        }
        if (documento.getTipoDocumento() == null) {
            throw new ReglaNegocioException("El documento debe tener un tipo de documento");
        }

        ReglaTributaria regla = reglaResolver.resolver(documento.getTipoDocumento().getCodigoSii());
        regla.validarParaEmitir(documento);
        calculoMontosService.recalcular(documento);

        Integer folio = folioService.asignarSiguienteFolio(documento.getTipoDocumento().getCodigoSii());
        documento.setFolio(folio);
        documento.setEstado(EstadoDocumento.EMITIDO);
        documento.setFechaEmision(LocalDate.now());

        DocumentoTributarioEntity guardado = save(documento);
        precargar(guardado);
        return guardado;
    }

    // -------------------------------------------------------------- helpers
    private DocumentoTributarioEntity cargar(Long id) {
        return findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con id: " + id));
    }

    private DocumentoTributarioEntity cargarEditable(Long id) {
        DocumentoTributarioEntity documento = cargar(id);
        if (documento.getEstado() == EstadoDocumento.EMITIDO) {
            throw new ReglaNegocioException("El documento está EMITIDO y no puede modificarse");
        }
        return documento;
    }

    /** Inicializa las colecciones lazy para que puedan mapearse al DTO fuera de la transacción. */
    private void precargar(DocumentoTributarioEntity documento) {
        if (documento.getDetalles() != null) {
            documento.getDetalles().size();
        }
        if (documento.getReferencias() != null) {
            documento.getReferencias().size();
        }
    }

    private EstadoDocumento parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoDocumento.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Estado inválido: " + estado);
        }
    }
}
