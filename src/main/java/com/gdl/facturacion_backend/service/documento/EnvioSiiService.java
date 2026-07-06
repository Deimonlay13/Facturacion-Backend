package com.gdl.facturacion_backend.service.documento;

import com.gdl.facturacion_backend.context.TenantContext;
import com.gdl.facturacion_backend.dto.sii.EnvioSiiResponse;
import com.gdl.facturacion_backend.dto.sii.EstadoSiiResponse;
import com.gdl.facturacion_backend.dto.sii.HistorialSiiAdminResponse;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EnvioSiiEntity;
import com.gdl.facturacion_backend.entity.HistorialEstadoSiiEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.enums.EstadoEnvioSii;
import com.gdl.facturacion_backend.enums.EstadoSii;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import com.gdl.facturacion_backend.repository.EnvioSiiRepository;
import com.gdl.facturacion_backend.repository.HistorialEstadoSiiRepository;
import com.gdl.facturacion_backend.security.SecurityUtils;
import com.gdl.facturacion_backend.service.DocumentoTributarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Simula el diálogo con el SII para un documento EMITIDO:
 *   enviar() -> ENVIADO (genera track id)   consultarEstado() -> ACEPTADO/RECHAZADO.
 * Persiste cada intento en EnvioSiiEntity y cada transición en HistorialEstadoSiiEntity.
 */
@Service
public class EnvioSiiService {

    private final DocumentoTributarioService documentoService;
    private final DocumentoTributarioRepository documentoRepository;
    private final EnvioSiiRepository envioRepository;
    private final HistorialEstadoSiiRepository historialRepository;
    private final DocumentoExportService exportService;

    public EnvioSiiService(DocumentoTributarioService documentoService,
                           DocumentoTributarioRepository documentoRepository,
                           EnvioSiiRepository envioRepository,
                           HistorialEstadoSiiRepository historialRepository,
                           DocumentoExportService exportService) {
        this.documentoService = documentoService;
        this.documentoRepository = documentoRepository;
        this.envioRepository = envioRepository;
        this.historialRepository = historialRepository;
        this.exportService = exportService;
    }

    /** Paso 1: simula el envío del DTE. Genera track id y deja el documento en ENVIADO. */
    @Transactional
    public EstadoSiiResponse enviar(Long documentoId) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(documentoId);

        if (doc.getEstado() != EstadoDocumento.EMITIDO) {
            throw new ReglaNegocioException("Solo se pueden enviar al SII documentos EMITIDOS");
        }
        if (doc.getEstadoSii() == EstadoDocumentoSii.ACEPTADO) {
            throw new ReglaNegocioException("El documento ya fue aceptado por el SII");
        }

        // El XML (DTE) es el "sobre" que se enviaría al SII. Aquí solo se genera para la simulación.
        byte[] xml = exportService.generarXml(documentoId);

        int intentoPrevio = (doc.getEnvio() != null && doc.getEnvio().getIntentos() != null)
                ? doc.getEnvio().getIntentos() : 0;

        EnvioSiiEntity envio = new EnvioSiiEntity();
        envio.setTrackId("SIM-" + doc.getFolio() + "-" + UUID.randomUUID().toString().substring(0, 8));
        envio.setFechaEnvio(LocalDateTime.now());
        envio.setEstado(EstadoEnvioSii.ENVIADO);
        envio.setIntentos(intentoPrevio + 1);
        envio.setRespuesta("DTE recibido por el SII (simulado). Tamaño XML: " + xml.length
                + " bytes. Track ID asignado; a la espera de validación.");
        envio = envioRepository.save(envio);

        doc.setEnvio(envio);
        doc.setEstadoSii(EstadoDocumentoSii.ENVIADO);
        doc.setXmlFirmado(true);
        documentoRepository.save(doc);

        registrarHistorial(doc, EstadoSii.ENVIADO,
                "Envío simulado al SII. Track ID: " + envio.getTrackId());

        return construirRespuesta(doc);
    }

    /** Paso 2: simula la consulta del estado del envío (acepta o rechaza). */
    @Transactional
    public EstadoSiiResponse consultarEstado(Long documentoId) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(documentoId);
        EnvioSiiEntity envio = doc.getEnvio();

        if (envio == null || doc.getEstadoSii() == EstadoDocumentoSii.PENDIENTE) {
            throw new ReglaNegocioException("El documento aún no ha sido enviado al SII");
        }
        // Ya resuelto: devuelve el estado actual sin cambios.
        if (doc.getEstadoSii() == EstadoDocumentoSii.ACEPTADO
                || doc.getEstadoSii() == EstadoDocumentoSii.RECHAZADO) {
            return construirRespuesta(doc);
        }

        List<String> reparos = validarParaAceptacion(doc);
        if (reparos.isEmpty()) {
            envio.setEstado(EstadoEnvioSii.ACEPTADO);
            envio.setRespuesta("DTE ACEPTADO por el SII (simulado). Track ID: " + envio.getTrackId());
            doc.setEstadoSii(EstadoDocumentoSii.ACEPTADO);
            registrarHistorial(doc, EstadoSii.ACEPTADO, "El SII aceptó el documento (simulado).");
        } else {
            String motivo = String.join("; ", reparos);
            envio.setEstado(EstadoEnvioSii.RECHAZADO);
            envio.setRespuesta("DTE RECHAZADO por el SII (simulado): " + motivo);
            doc.setEstadoSii(EstadoDocumentoSii.RECHAZADO);
            registrarHistorial(doc, EstadoSii.RECHAZADO, "El SII rechazó el documento (simulado): " + motivo);
        }
        envioRepository.save(envio);
        documentoRepository.save(doc);

        return construirRespuesta(doc);
    }

    /** Estado SII actual + historial (solo lectura). */
    @Transactional(readOnly = true)
    public EstadoSiiResponse obtenerEstado(Long documentoId) {
        DocumentoTributarioEntity doc = documentoService.obtenerDetalle(documentoId);
        return construirRespuesta(doc);
    }

    // ------------------------------------------------- administración (panel)

    /** Todos los envíos al SII. Un SUPER_ADMIN ve los de todas las empresas; el resto, los suyos. */
    @Transactional(readOnly = true)
    public List<EnvioSiiResponse> listarEnvios() {
        List<EnvioSiiEntity> envios = SecurityUtils.esSuperAdminSinEmpresa()
                ? envioRepository.findAll()
                : envioRepository.findAllByEmpresaId(TenantContext.getEmpresaId());
        return envios.stream()
                .sorted(Comparator.comparing(EnvioSiiEntity::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(EnvioSiiResponse::from)
                .toList();
    }

    /** Todo el historial de estados SII. SUPER_ADMIN ve todas las empresas; el resto, la suya. */
    @Transactional(readOnly = true)
    public List<HistorialSiiAdminResponse> listarHistorial() {
        List<HistorialEstadoSiiEntity> historial = SecurityUtils.esSuperAdminSinEmpresa()
                ? historialRepository.findAll()
                : historialRepository.findAllByEmpresaId(TenantContext.getEmpresaId());
        return historial.stream()
                .sorted(Comparator.comparing(HistorialEstadoSiiEntity::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(HistorialSiiAdminResponse::from)
                .toList();
    }

    // -------------------------------------------------------------- helpers

    private List<String> validarParaAceptacion(DocumentoTributarioEntity doc) {
        List<String> reparos = new ArrayList<>();
        if (doc.getFolio() == null) reparos.add("folio no asignado");
        if (doc.getRutEmisor() == null || doc.getRutEmisor().isBlank()) reparos.add("emisor sin RUT");
        if (doc.getRut() == null || doc.getRut().isBlank()) reparos.add("receptor sin RUT");
        if (doc.getMontoTotal() == null || doc.getMontoTotal().compareTo(BigDecimal.ZERO) <= 0)
            reparos.add("monto total inválido");
        return reparos;
    }

    private void registrarHistorial(DocumentoTributarioEntity doc, EstadoSii estado, String mensaje) {
        HistorialEstadoSiiEntity h = new HistorialEstadoSiiEntity();
        h.setDocumento(doc);
        h.setEstado(estado);
        h.setFechaEstado(LocalDateTime.now());
        h.setMensaje(mensaje);
        historialRepository.save(h);
    }

    private EstadoSiiResponse construirRespuesta(DocumentoTributarioEntity doc) {
        List<HistorialEstadoSiiEntity> historial =
                historialRepository.findByDocumentoIdOrderByFechaEstadoAsc(doc.getId());
        return EstadoSiiResponse.from(doc, doc.getEnvio(), historial);
    }
}
