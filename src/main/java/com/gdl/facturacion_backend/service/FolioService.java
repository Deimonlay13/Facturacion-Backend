package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.folio.CafCargaRequest;
import com.gdl.facturacion_backend.dto.folio.CafResponse;
import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoCaf;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.CafRepository;
import com.gdl.facturacion_backend.repository.ControlFolioRepository;
import com.gdl.facturacion_backend.repository.FolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FolioService extends BaseTenantService<ControlFolioEntity> {

    private final ControlFolioRepository controlFolioRepository;
    private final CafRepository cafRepository;
    private final FolioRepository folioRepository;
    private final TipoDocumentoService tipoDocumentoService;

    public FolioService(ControlFolioRepository controlFolioRepository,
                        CafRepository cafRepository,
                        FolioRepository folioRepository,
                        TenantService tenantService,
                        TipoDocumentoService tipoDocumentoService) {
        super(controlFolioRepository, tenantService);
        this.controlFolioRepository = controlFolioRepository;
        this.cafRepository = cafRepository;
        this.folioRepository = folioRepository;
        this.tipoDocumentoService = tipoDocumentoService;
    }

    @Transactional
    public CafResponse cargarCaf(CafCargaRequest request) {
        validarRango(request.getRangoDesde(), request.getRangoHasta());

        Long empresaId = getEmpresaId();
        TipoDocumentoEntity tipo = tipoDocumentoService.findByCodigoSii(request.getCodigoTipoDocumento());

        if (folioRepository.existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(
                request.getCodigoTipoDocumento(), empresaId, request.getRangoDesde(), request.getRangoHasta())) {
            throw new ReglaNegocioException("El rango del CAF se superpone con folios ya existentes");
        }

        CafEntity caf = new CafEntity();
        caf.setEmpresa(empresaRef(empresaId));
        caf.setTipoDocumento(tipo);
        caf.setRangoDesde(request.getRangoDesde());
        caf.setRangoHasta(request.getRangoHasta());
        caf.setFechaAutorizacion(request.getFechaAutorizacion());
        caf.setFechaVencimiento(request.getFechaVencimiento());
        caf.setCafXml(request.getCafXml());
        caf.setEstado(EstadoCaf.DISPONIBLE);
        CafEntity guardado = cafRepository.save(caf);

        folioRepository.saveAll(generarFolios(guardado, tipo, empresaId));

        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(request.getCodigoTipoDocumento(), empresaId)
                .orElseGet(() -> {
                    ControlFolioEntity nuevo = new ControlFolioEntity();
                    nuevo.setTipoDocumento(tipo);
                    nuevo.setUltimoFolioUtilizado(null);
                    return nuevo;
                });
        control.setCafActivo(guardado);
        save(control);

        return toCafResponse(guardado);
    }

    public List<ControlFolioEntity> listar() {
        return findAll();
    }

    public List<FolioEntity> listarFolios() {
        return folioRepository.findAllByEmpresaId(getEmpresaId());
    }

    public org.springframework.data.domain.Page<FolioEntity> listarFoliosPaginado(
            org.springframework.data.domain.Pageable pageable) {
        return folioRepository.findByEmpresaId(getEmpresaId(), pageable);
    }

    public List<CafResponse> listarCafs() {
        return cafRepository.findAllByEmpresaId(getEmpresaId()).stream()
                .map(this::toCafResponse)
                .toList();
    }

    @Transactional
    public Integer asignarSiguienteFolio(Integer codigoTipoDocumento) {
        Long empresaId = getEmpresaId();
        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(codigoTipoDocumento, empresaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay CAF cargado para el tipo " + codigoTipoDocumento));
        if (control.getCafActivo() == null) {
            throw new ReglaNegocioException("No hay CAF activo para el tipo " + codigoTipoDocumento);
        }

        FolioEntity folio = folioRepository
                .findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        control.getCafActivo().getId(), empresaId, EstadoFolio.DISPONIBLE)
                .orElseThrow(() -> {
                    marcarCafAgotado(control.getCafActivo(), empresaId);
                    return new ReglaNegocioException(
                            "No hay folios disponibles para el tipo " + codigoTipoDocumento);
                });

        folio.setEstado(EstadoFolio.RESERVADO);
        folioRepository.save(folio);

        folio.setEstado(EstadoFolio.UTILIZADO);
        folioRepository.save(folio);

        control.setUltimoFolioUtilizado(folio.getNumero());
        control.setUltimaFechaEmision(LocalDate.now());
        controlFolioRepository.save(control);
        marcarCafAgotado(folio.getCaf(), empresaId);

        return folio.getNumero();
    }

    private void validarRango(Integer desde, Integer hasta) {
        if (desde > hasta) {
            throw new ReglaNegocioException("El rango desde no puede ser mayor que el rango hasta");
        }
    }

    private List<FolioEntity> generarFolios(CafEntity caf, TipoDocumentoEntity tipo, Long empresaId) {
        List<FolioEntity> folios = new ArrayList<>();
        for (int numero = caf.getRangoDesde(); numero <= caf.getRangoHasta(); numero++) {
            FolioEntity folio = new FolioEntity();
            folio.setEmpresa(empresaRef(empresaId));
            folio.setCaf(caf);
            folio.setTipoDocumento(tipo);
            folio.setNumero(numero);
            folio.setEstado(EstadoFolio.DISPONIBLE);
            folios.add(folio);
        }
        return folios;
    }

    private CafResponse toCafResponse(CafEntity caf) {
        Long empresaId = getEmpresaId();
        long generados = folioRepository.countByCafIdAndEmpresaId(caf.getId(), empresaId);
        long disponibles = folioRepository.countByCafIdAndEmpresaIdAndEstado(
                caf.getId(), empresaId, EstadoFolio.DISPONIBLE);
        return CafResponse.from(caf, generados, disponibles);
    }

    private void marcarCafAgotado(CafEntity caf, Long empresaId) {
        if (caf == null || caf.getEstado() == EstadoCaf.AGOTADO) {
            return;
        }
        long disponibles = folioRepository.countByCafIdAndEmpresaIdAndEstado(
                caf.getId(), empresaId, EstadoFolio.DISPONIBLE);
        if (disponibles == 0) {
            caf.setEstado(EstadoCaf.AGOTADO);
            cafRepository.save(caf);
        }
    }

    private EmpresaEntity empresaRef(Long empresaId) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(empresaId);
        return empresa;
    }
}
