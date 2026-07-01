package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.folio.CafCargaRequest;
import com.gdl.facturacion_backend.dto.folio.CafResponse;
import com.gdl.facturacion_backend.dto.folio.FolioCargaSimpleRequest;
import com.gdl.facturacion_backend.dto.folio.FolioResumenResponse;
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

    private static final List<Integer> TIPOS_FOLIO_SIMPLE = List.of(33, 34, 56, 61);

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
    public FolioResumenResponse cargarFoliosSimple(FolioCargaSimpleRequest request) {
        validarTipoSimple(request.getCodigoTipoDocumento());

        Long empresaId = getEmpresaId();
        TipoDocumentoEntity tipo = tipoDocumentoService.findByCodigoSii(request.getCodigoTipoDocumento());
        int ultimo = folioRepository
                .findFirstByTipoDocumentoCodigoSiiAndEmpresaIdOrderByNumeroDesc(
                        request.getCodigoTipoDocumento(), empresaId)
                .map(FolioEntity::getNumero)
                .orElse(0);
        int desde = ultimo + 1;
        int hasta = ultimo + request.getCantidad();

        CafEntity caf = new CafEntity();
        caf.setEmpresa(empresaRef(empresaId));
        caf.setTipoDocumento(tipo);
        caf.setRangoDesde(desde);
        caf.setRangoHasta(hasta);
        caf.setFechaAutorizacion(LocalDate.now());
        caf.setCafXml("FOLIOS_SIMPLES_GENERADOS_INTERNAMENTE");
        caf.setEstado(EstadoCaf.DISPONIBLE);
        CafEntity guardado = cafRepository.save(caf);

        folioRepository.saveAll(generarFolios(guardado, tipo, empresaId));

        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(request.getCodigoTipoDocumento(), empresaId)
                .orElseGet(() -> {
                    ControlFolioEntity nuevo = new ControlFolioEntity();
                    nuevo.setTipoDocumento(tipo);
                    nuevo.setUltimoFolioUtilizado(ultimo > 0 ? ultimo : null);
                    return nuevo;
                });
        control.setCafActivo(guardado);
        controlFolioRepository.save(control);

        return toResumen(control, empresaId);
    }

    public List<FolioResumenResponse> resumenFolios() {
        Long empresaId = getEmpresaId();
        return controlFolioRepository.findAllByEmpresaId(empresaId).stream()
                .map(control -> toResumen(control, empresaId))
                .toList();
    }

    public FolioResumenResponse resumenFolio(Integer codigoTipoDocumento) {
        Long empresaId = getEmpresaId();
        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(codigoTipoDocumento, empresaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay CAF cargado para el tipo " + codigoTipoDocumento));
        return toResumen(control, empresaId);
    }

    @Transactional
    public Integer asignarSiguienteFolio(Integer codigoTipoDocumento) {
        Long empresaId = getEmpresaId();
        ControlFolioEntity control = controlFolioRepository
                .findByTipoDocumentoCodigoSiiAndEmpresaId(codigoTipoDocumento, empresaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No hay folios cargados para el tipo " + codigoTipoDocumento));
        if (control.getCafActivo() == null) {
            throw new ReglaNegocioException("No hay folios disponibles para el tipo " + codigoTipoDocumento);
        }

        FolioEntity folio = folioRepository
                .findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        control.getCafActivo().getId(), empresaId, EstadoFolio.DISPONIBLE)
                .orElseThrow(() -> {
                    marcarCafAgotado(control.getCafActivo(), empresaId);
                    return new ReglaNegocioException(
                            "No hay folios disponibles para el tipo " + codigoTipoDocumento
                                    + ". Cargue nuevos folios antes de facturar.");
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

    private FolioResumenResponse toResumen(ControlFolioEntity control, Long empresaId) {
        Integer codigoSii = control.getTipoDocumento() != null
                ? control.getTipoDocumento().getCodigoSii()
                : null;
        if (codigoSii == null) {
            return FolioResumenResponse.from(control, null, 0);
        }

        long disponibles = folioRepository.countByTipoDocumentoCodigoSiiAndEmpresaIdAndEstado(
                codigoSii, empresaId, EstadoFolio.DISPONIBLE);
        FolioEntity siguiente = folioRepository
                .findFirstByTipoDocumentoCodigoSiiAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        codigoSii, empresaId, EstadoFolio.DISPONIBLE)
                .orElse(null);
        return FolioResumenResponse.from(control, siguiente, disponibles);
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

    private void validarTipoSimple(Integer codigoTipoDocumento) {
        if (!TIPOS_FOLIO_SIMPLE.contains(codigoTipoDocumento)) {
            throw new ReglaNegocioException(
                    "La carga simple solo está permitida para factura afecta (33), "
                            + "factura exenta (34), nota de débito (56) y nota de crédito (61)");
        }
    }

    private EmpresaEntity empresaRef(Long empresaId) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(empresaId);
        return empresa;
    }
}
