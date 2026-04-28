package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.documento.DocumentoCreateRequest;
import com.gdl.facturacion_backend.dto.documento.DocumentoResponse;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
import com.gdl.facturacion_backend.enums.EstadoDocumentoSii;
import com.gdl.facturacion_backend.repository.DocumentoTributarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DocumentoTributarioService {

    private final DocumentoTributarioRepository repository;
    private final TipoDocumentoService tipoDocumentoService;

    // integrar ClienteService cuando esté implementado
    // private final ClienteService clienteService;

    public DocumentoResponse create(DocumentoCreateRequest request) {

    
        TipoDocumentoEntity tipo = tipoDocumentoService
                .findByCodigoSii(request.getCodigoTipoDocumento());

        DocumentoTributarioEntity doc = new DocumentoTributarioEntity();

        doc.setTipoDocumento(tipo);

        // integrar cliente cuando exista ClienteService
        // var cliente = clienteService.findById(request.getClienteId());
        // doc.setCliente(cliente);

        doc.setFechaEmision(LocalDate.now());
        doc.setEstado(EstadoDocumento.BORRADOR);
        doc.setEstadoSii(EstadoDocumentoSii.PENDIENTE);
        doc.setXmlFirmado(false);
        doc.setFolio(null);

       
        DocumentoTributarioEntity saved = repository.save(doc);

        return new DocumentoResponse(
                saved.getId(),
                saved.getTipoDocumento().getCodigoSii(),
                saved.getTipoDocumento().getDescripcion(),
                null, // cliente pendiente
                saved.getEstado().name(),
                saved.getEstadoSii().name(),
                saved.getFolio(),
                saved.getFechaEmision()
        );
    }
}