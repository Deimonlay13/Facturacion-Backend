package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.entity.ArchivoEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.enums.TipoArchivo;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.repository.ArchivoRepository;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final DocumentoTributarioRepository documentoRepository;
    private final TenantService tenantService;

    public ArchivoService(ArchivoRepository archivoRepository,
                         DocumentoTributarioRepository documentoRepository,
                         TenantService tenantService) {
        this.archivoRepository = archivoRepository;
        this.documentoRepository = documentoRepository;
        this.tenantService = tenantService;
    }

    /**
     * Guarda un archivo TXT asociado a un documento.
     */
    @Transactional
    public ArchivoEntity guardarTxt(Long documentoId, String contenidoTxt) {
        DocumentoTributarioEntity documento = obtenerDocumento(documentoId);
        
        ArchivoEntity archivo = new ArchivoEntity();
        archivo.setDocumento(documento);
        archivo.setTipo(TipoArchivo.XML_DTE); // Usamos este tipo como referencia al TXT original
        archivo.setNombreArchivo("documento_" + documentoId + "_original.txt");
        archivo.setRuta("txt"); // metadata para identificar que es TXT
        
        return archivoRepository.save(archivo);
    }

    /**
     * Guarda un archivo PDF generado para un documento.
     */
    @Transactional
    public ArchivoEntity guardarPdf(Long documentoId, byte[] pdfBytes) {
        DocumentoTributarioEntity documento = obtenerDocumento(documentoId);
        
        // Eliminar PDF anterior si existe
        archivoRepository.findLatestByDocumentoIdAndTipo(documentoId, TipoArchivo.PDF)
                .ifPresent(archivoRepository::delete);
        
        ArchivoEntity archivo = new ArchivoEntity();
        archivo.setDocumento(documento);
        archivo.setTipo(TipoArchivo.PDF);
        archivo.setNombreArchivo("documento_" + documentoId + ".pdf");
        archivo.setRuta("pdf"); // metadata: ruta virtual
        
        return archivoRepository.save(archivo);
    }

    /**
     * Guarda un archivo XML generado para un documento.
     */
    @Transactional
    public ArchivoEntity guardarXml(Long documentoId, byte[] xmlBytes) {
        DocumentoTributarioEntity documento = obtenerDocumento(documentoId);
        
        ArchivoEntity archivo = new ArchivoEntity();
        archivo.setDocumento(documento);
        archivo.setTipo(TipoArchivo.XML_DTE);
        archivo.setNombreArchivo("documento_" + documentoId + "_dte.xml");
        archivo.setRuta("xml"); // metadata: ruta virtual
        
        return archivoRepository.save(archivo);
    }

    /**
     * Obtiene el PDF más reciente de un documento.
     */
    public Optional<ArchivoEntity> obtenerPdfLatest(Long documentoId) {
        return archivoRepository.findLatestByDocumentoIdAndTipo(documentoId, TipoArchivo.PDF);
    }

    /**
     * Obtiene el XML más reciente de un documento.
     */
    public Optional<ArchivoEntity> obtenerXmlLatest(Long documentoId) {
        return archivoRepository.findLatestByDocumentoIdAndTipo(documentoId, TipoArchivo.XML_DTE);
    }

    /**
     * Obtiene todos los archivos de un documento.
     */
    public List<ArchivoEntity> obtenerArchivos(Long documentoId) {
        return archivoRepository.findByDocumentoId(documentoId);
    }

    /**
     * Obtiene archivos de un tipo específico para un documento.
     */
    public List<ArchivoEntity> obtenerArchivosPorTipo(Long documentoId, TipoArchivo tipo) {
        return archivoRepository.findByDocumentoIdAndTipo(documentoId, tipo);
    }

    /**
     * Elimina un archivo específico.
     */
    @Transactional
    public void eliminarArchivo(Long archivoId) {
        archivoRepository.findById(archivoId)
                .ifPresentOrElse(
                        archivoRepository::delete,
                        () -> {
                            throw new RecursoNoEncontradoException("Archivo no encontrado con id: " + archivoId);
                        });
    }

    /**
     * Elimina todos los archivos de un documento.
     */
    @Transactional
    public void eliminarArchivosPorDocumento(Long documentoId) {
        List<ArchivoEntity> archivos = obtenerArchivos(documentoId);
        archivoRepository.deleteAll(archivos);
    }

    // ============ Helper ============

    private DocumentoTributarioEntity obtenerDocumento(Long documentoId) {
        Long empresaId = tenantService.getEmpresaId();
        return documentoRepository.findByIdAndEmpresaId(documentoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con id: " + documentoId));
    }
}
