package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.folio.ControlFolioRequest;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.ControlFolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Control de folios por empresa y tipo de documento.
 * Cada empresa lleva su propio rango y nunca se reutilizan folios.
 */
@Service
public class FolioService extends BaseTenantService<ControlFolioEntity> {

    private final ControlFolioRepository controlFolioRepository;
    private final TipoDocumentoService tipoDocumentoService;

    public FolioService(ControlFolioRepository controlFolioRepository,
                        TenantService tenantService,
                        TipoDocumentoService tipoDocumentoService) {
        super(controlFolioRepository, tenantService);
        this.controlFolioRepository = controlFolioRepository;
        this.tipoDocumentoService = tipoDocumentoService;
    }

    public ControlFolioEntity registrarControl(ControlFolioRequest request) {
        if (request.getRangoDesde() > request.getRangoHasta()) {
            throw new ReglaNegocioException("El rango desde no puede ser mayor que el rango hasta");
        }

        TipoDocumentoEntity tipo = tipoDocumentoService.findByCodigoSii(request.getCodigoTipoDocumento());

        controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(request.getCodigoTipoDocumento(), getEmpresaId())
                .ifPresent(existente -> {
                    throw new ReglaNegocioException(
                            "Ya existe un control de folios para el tipo " + request.getCodigoTipoDocumento());
                });

        ControlFolioEntity control = new ControlFolioEntity();
        control.setTipoDocumento(tipo);
        control.setRangoDesde(request.getRangoDesde());
        control.setRangoHasta(request.getRangoHasta());
        control.setUltimoFolioUtilizado(null);
        return save(control);
    }

    public List<ControlFolioEntity> listar() {
        return findAll();
    }

    /**
     * Asigna y reserva el siguiente folio disponible para la empresa autenticada y el tipo dado.
     */
    @Transactional
    public Integer asignarSiguienteFolio(Integer codigoTipoDocumento) {
        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(codigoTipoDocumento, getEmpresaId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay folios configurados para el tipo " + codigoTipoDocumento));

        int siguiente = (control.getUltimoFolioUtilizado() == null)
                ? control.getRangoDesde()
                : control.getUltimoFolioUtilizado() + 1;

        if (siguiente > control.getRangoHasta()) {
            throw new ReglaNegocioException(
                    "No hay folios disponibles en el rango para el tipo " + codigoTipoDocumento);
        }

        control.setUltimoFolioUtilizado(siguiente);
        controlFolioRepository.save(control);
        return siguiente;
    }
}
