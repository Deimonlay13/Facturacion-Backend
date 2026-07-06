package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.documento.DetalleCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoEmisorStatsResponse;
import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoUpdateRequest;
import com.gdl.facturacion_backend.dto.documento.GuiaDespachoRequest;
import com.gdl.facturacion_backend.dto.documento.ReferenciaCreateRequest;
import com.gdl.facturacion_backend.entity.*;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.Moneda;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.repository.GuiaDespachoExtraRepository;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.repository.ReferenciaDocumentoRepository;
import com.gdl.facturacion_backend.repository.AuditoriaRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import com.gdl.facturacion_backend.service.documento.CalculoMontosService;
import com.gdl.facturacion_backend.service.documento.ReglaTributariaResolver;
import com.gdl.facturacion_backend.service.documento.regla.ReglaTributaria;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
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
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public DocumentoTributarioService(DocumentoTributarioRepository documentoRepository,
                                      TenantService tenantService,
                                      TipoDocumentoService tipoDocumentoService,
                                      ClienteService clienteService,
                                      ProductoRepository productoRepository,
                                      ReferenciaDocumentoRepository referenciaRepository,
                                      GuiaDespachoExtraRepository guiaRepository,
                                      CalculoMontosService calculoMontosService,
                                      ReglaTributariaResolver reglaResolver,
                                      FolioService folioService,
                                      EmpresaRepository empresaRepository,
                                      UsuarioRepository usuarioRepository,
                                      AuditoriaRepository auditoriaRepository) {
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
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
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
        documento.setMoneda(Moneda.CLP);
        documento.setTipoCambio(BigDecimal.ONE);

        return save(documento);
    }

    @Transactional
    public DocumentoTributarioEntity actualizarBorrador(Long documentoId, DocumentoUpdateRequest request) {
        DocumentoTributarioEntity documento = cargarEditable(documentoId);

        if (request.getClienteId() != null) {
            documento.setCliente(clienteService.obtenerPorId(request.getClienteId()));
        }
        if (request.getFechaVencimiento() != null) {
            documento.setFechaVencimiento(request.getFechaVencimiento());
        }
        if (request.getObservaciones() != null) {
            documento.setObservaciones(request.getObservaciones());
        }
        if (request.getMoneda() != null) {
            documento.setMoneda(request.getMoneda());
        }
        if (request.getTipoCambio() != null) {
            documento.setTipoCambio(request.getTipoCambio());
        }

        DocumentoTributarioEntity guardado = save(documento);
        precargar(guardado);
        return guardado;
    }

    public List<DocumentoTributarioEntity> consultar(Long clienteId, Integer codigoTipo,
                                                     String estado, LocalDate desde, LocalDate hasta) {
        List<DocumentoTributarioEntity> documentos = documentoRepository.buscar(empresaIdConsulta(), clienteId, codigoTipo,
                parseEstado(estado), desde, hasta);
        completarUsuariosEmisoresDesdeAuditoria(documentos);
        return documentos;
    }

    /** empresaId a usar en consultas: null solo para un SUPER_ADMIN sin empresa seleccionada
     *  (ve documentos de todas las empresas); si elige una, queda acotado a ella. */
    private Long empresaIdConsulta() {
        return vistaGlobal() ? null : getEmpresaId();
    }

    public List<DocumentoEmisorStatsResponse> estadisticasPorEmisor(Integer codigoTipo,
                                                                    LocalDate desde,
                                                                    LocalDate hasta) {
        return documentoRepository.estadisticasPorEmisor(empresaIdConsulta(), codigoTipo, desde, hasta).stream()
                .map(row -> new DocumentoEmisorStatsResponse(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] instanceof BigDecimal total ? total : BigDecimal.ZERO
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoTributarioEntity obtenerDetalle(Long id) {
        DocumentoTributarioEntity documento = cargar(id);
        precargar(documento);
        completarUsuarioEmisorDesdeAuditoria(documento);
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
        copiarSnapshotCliente(documento);
        copiarSnapshotEmpresa(documento);

        Integer folio = folioService.asignarSiguienteFolio(documento.getTipoDocumento().getCodigoSii());
        documento.setFolio(folio);
        documento.setEstado(EstadoDocumento.EMITIDO);
        documento.setFechaEmision(LocalDate.now());
        documento.setNombreUsuarioEmisor(nombreUsuarioActual());
        usuarioActual().ifPresent(usuario -> {
            documento.setUsuarioEmisor(usuario);
            documento.setNombreUsuarioEmisor(usuario.getUsername());
        });

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

    private void copiarSnapshotCliente(DocumentoTributarioEntity documento) {
        ClienteEntity cliente = documento.getCliente();
        documento.setRut(cliente.getRut());
        documento.setRazonSocial(cliente.getRazonSocial());
        documento.setGiro(cliente.getGiro());
        documento.setDireccion(cliente.getDireccion());
        documento.setCiudad(cliente.getCiudad());
        documento.setComuna(cliente.getComuna());
        documento.setPais(cliente.getPais());
        documento.setCorreo(cliente.getEmail());
    }

    private void copiarSnapshotEmpresa(DocumentoTributarioEntity documento) {
        EmpresaEntity empresa = empresaRepository.findById(getEmpresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));

        documento.setRutEmisor(empresa.getRutEmpresa());
        documento.setRazonSocialEmisor(empresa.getRazonSocial());
        documento.setNombreFantasiaEmisor(empresa.getNombreFantasia());
        documento.setGiroEmisor(empresa.getGiro());
        documento.setDireccionEmisor(empresa.getDireccion());
        documento.setCiudadEmisor(empresa.getCiudad());
        documento.setComunaEmisor(empresa.getComuna());
        documento.setPaisEmisor(empresa.getPais());
        documento.setTelefonoEmisor(empresa.getTelefono());
        documento.setEmailPrincipalEmisor(empresa.getEmailPrincipal());
        documento.setEmailContabilidadEmisor(empresa.getEmailContabilidad());
        documento.setRutRepresentanteEmisor(empresa.getRutRepresentante());
        documento.setNombreRepresentanteEmisor(empresa.getNombreRepresentante());
    }

    private void completarUsuariosEmisoresDesdeAuditoria(List<DocumentoTributarioEntity> documentos) {
        documentos.stream()
                .filter(documento -> documento.getUsuarioEmisor() == null)
                .filter(documento -> documento.getId() != null)
                .filter(documento -> documento.getEstado() == EstadoDocumento.EMITIDO)
                .forEach(this::completarUsuarioEmisorDesdeAuditoria);
    }

    private void completarUsuarioEmisorDesdeAuditoria(DocumentoTributarioEntity documento) {
        if (documento.getUsuarioEmisor() != null
                || documento.getId() == null
                || documento.getEstado() != EstadoDocumento.EMITIDO) {
            return;
        }

        auditoriaRepository
                .findFirstByTablaAndAccionAndDetalleOrderByFechaDesc(
                        "DocumentoTributario",
                        "emitir",
                        "id=" + documento.getId())
                .filter(auditoria -> auditoria.getUsuarioId() != null)
                .flatMap(auditoria -> usuarioRepository.findById(auditoria.getUsuarioId()))
                .ifPresent(usuario -> {
                    documento.setUsuarioEmisor(usuario);
                    documento.setNombreUsuarioEmisor(usuario.getUsername());
                });
    }

    private java.util.Optional<UsuarioEntity> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equals(auth.getName())) {
            return java.util.Optional.empty();
        }
        return usuarioRepository.findByUsername(auth.getName());
    }

    private String nombreUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()
                || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return auth.getName();
    }
}
