package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.folio.CafCargaRequest;
import com.gdl.facturacion_backend.dto.folio.CafResponse;
import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoCaf;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.CafRepository;
import com.gdl.facturacion_backend.repository.ControlFolioRepository;
import com.gdl.facturacion_backend.repository.FolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolioServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Integer CODIGO_TIPO = 33;

    @Mock
    private ControlFolioRepository controlFolioRepository;

    @Mock
    private CafRepository cafRepository;

    @Mock
    private FolioRepository folioRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private TipoDocumentoService tipoDocumentoService;

    @InjectMocks
    private FolioService folioService;

    private TipoDocumentoEntity tipo;

    @BeforeEach
    void setUp() {
        // El servicio resuelve la empresa a través de tenantService.getEmpresaId().
        // Se usa lenient porque algunos tests (validaciones tempranas) no llegan a usarlo.
        lenient().when(tenantService.getEmpresaId()).thenReturn(EMPRESA_ID);

        tipo = new TipoDocumentoEntity();
        tipo.setId(7L);
        tipo.setCodigoSii(CODIGO_TIPO);
        tipo.setDescripcion("Factura Electrónica");
    }

    private CafCargaRequest nuevaRequest(int desde, int hasta) {
        CafCargaRequest request = new CafCargaRequest();
        request.setCodigoTipoDocumento(CODIGO_TIPO);
        request.setRangoDesde(desde);
        request.setRangoHasta(hasta);
        request.setFechaAutorizacion(LocalDate.of(2026, 1, 1));
        request.setFechaVencimiento(LocalDate.of(2026, 12, 31));
        request.setCafXml("<CAF>xml</CAF>");
        return request;
    }

    // ----------------------------------------------------------------------
    // cargarCaf
    // ----------------------------------------------------------------------

    @Test
    void cargarCaf_creaCafFoliosYControlNuevo() {
        CafCargaRequest request = nuevaRequest(1, 3);

        when(tipoDocumentoService.findByCodigoSii(CODIGO_TIPO)).thenReturn(tipo);
        when(folioRepository.existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(
                CODIGO_TIPO, EMPRESA_ID, 1, 3)).thenReturn(false);

        CafEntity guardado = new CafEntity();
        guardado.setId(50L);
        guardado.setTipoDocumento(tipo);
        guardado.setRangoDesde(1);
        guardado.setRangoHasta(3);
        guardado.setFechaAutorizacion(request.getFechaAutorizacion());
        guardado.setFechaVencimiento(request.getFechaVencimiento());
        guardado.setEstado(EstadoCaf.DISPONIBLE);
        when(cafRepository.save(any(CafEntity.class))).thenReturn(guardado);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.empty());
        when(controlFolioRepository.save(any(ControlFolioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // toCafResponse
        when(folioRepository.countByCafIdAndEmpresaId(50L, EMPRESA_ID)).thenReturn(3L);
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(50L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(3L);

        CafResponse response = folioService.cargarCaf(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getCodigoTipoDocumento()).isEqualTo(CODIGO_TIPO);
        assertThat(response.getTipoDocumento()).isEqualTo("Factura Electrónica");
        assertThat(response.getRangoDesde()).isEqualTo(1);
        assertThat(response.getRangoHasta()).isEqualTo(3);
        assertThat(response.getEstado()).isEqualTo("DISPONIBLE");
        assertThat(response.getFoliosGenerados()).isEqualTo(3L);
        assertThat(response.getFoliosDisponibles()).isEqualTo(3L);

        // Se guarda el CAF con estado DISPONIBLE y datos del request
        ArgumentCaptor<CafEntity> cafCaptor = ArgumentCaptor.forClass(CafEntity.class);
        verify(cafRepository).save(cafCaptor.capture());
        CafEntity cafGuardado = cafCaptor.getValue();
        assertThat(cafGuardado.getEstado()).isEqualTo(EstadoCaf.DISPONIBLE);
        assertThat(cafGuardado.getRangoDesde()).isEqualTo(1);
        assertThat(cafGuardado.getRangoHasta()).isEqualTo(3);
        assertThat(cafGuardado.getCafXml()).isEqualTo("<CAF>xml</CAF>");
        assertThat(cafGuardado.getTipoDocumento()).isSameAs(tipo);
        assertThat(cafGuardado.getEmpresa()).isNotNull();
        assertThat(cafGuardado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);

        // Se generan tantos folios como el tamaño del rango (1..3 => 3)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FolioEntity>> foliosCaptor = ArgumentCaptor.forClass(List.class);
        verify(folioRepository).saveAll(foliosCaptor.capture());
        List<FolioEntity> folios = foliosCaptor.getValue();
        assertThat(folios).hasSize(3);
        assertThat(folios).extracting(FolioEntity::getNumero).containsExactly(1, 2, 3);
        assertThat(folios).allMatch(f -> f.getEstado() == EstadoFolio.DISPONIBLE);
        assertThat(folios).allMatch(f -> f.getCaf() == guardado);
        assertThat(folios).allMatch(f -> f.getTipoDocumento() == tipo);

        // El control nuevo apunta al CAF guardado como activo
        ArgumentCaptor<ControlFolioEntity> controlCaptor = ArgumentCaptor.forClass(ControlFolioEntity.class);
        verify(controlFolioRepository).save(controlCaptor.capture());
        ControlFolioEntity control = controlCaptor.getValue();
        assertThat(control.getCafActivo()).isSameAs(guardado);
        assertThat(control.getTipoDocumento()).isSameAs(tipo);
        assertThat(control.getUltimoFolioUtilizado()).isNull();
    }

    @Test
    void cargarCaf_rangoDeUnSoloFolio_generaUnFolio() {
        CafCargaRequest request = nuevaRequest(5, 5);

        when(tipoDocumentoService.findByCodigoSii(CODIGO_TIPO)).thenReturn(tipo);
        when(folioRepository.existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(
                CODIGO_TIPO, EMPRESA_ID, 5, 5)).thenReturn(false);

        CafEntity guardado = new CafEntity();
        guardado.setId(60L);
        guardado.setTipoDocumento(tipo);
        guardado.setRangoDesde(5);
        guardado.setRangoHasta(5);
        guardado.setEstado(EstadoCaf.DISPONIBLE);
        when(cafRepository.save(any(CafEntity.class))).thenReturn(guardado);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.empty());
        when(controlFolioRepository.save(any(ControlFolioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(folioRepository.countByCafIdAndEmpresaId(60L, EMPRESA_ID)).thenReturn(1L);
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(60L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(1L);

        folioService.cargarCaf(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FolioEntity>> foliosCaptor = ArgumentCaptor.forClass(List.class);
        verify(folioRepository).saveAll(foliosCaptor.capture());
        assertThat(foliosCaptor.getValue()).hasSize(1);
        assertThat(foliosCaptor.getValue().get(0).getNumero()).isEqualTo(5);
    }

    @Test
    void cargarCaf_reutilizaControlExistente() {
        CafCargaRequest request = nuevaRequest(10, 12);

        when(tipoDocumentoService.findByCodigoSii(CODIGO_TIPO)).thenReturn(tipo);
        when(folioRepository.existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(
                CODIGO_TIPO, EMPRESA_ID, 10, 12)).thenReturn(false);

        CafEntity guardado = new CafEntity();
        guardado.setId(70L);
        guardado.setTipoDocumento(tipo);
        guardado.setRangoDesde(10);
        guardado.setRangoHasta(12);
        guardado.setEstado(EstadoCaf.DISPONIBLE);
        when(cafRepository.save(any(CafEntity.class))).thenReturn(guardado);

        // Control existente con id (ya persistido) -> BaseTenantService.save validará findById
        ControlFolioEntity existente = new ControlFolioEntity();
        existente.setId(99L);
        existente.setTipoDocumento(tipo);
        existente.setUltimoFolioUtilizado(8);
        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(existente));
        // save() en BaseTenantService verifica permiso vía findByIdAndEmpresaId
        when(controlFolioRepository.findByIdAndEmpresaId(99L, EMPRESA_ID))
                .thenReturn(Optional.of(existente));
        when(controlFolioRepository.save(any(ControlFolioEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(folioRepository.countByCafIdAndEmpresaId(70L, EMPRESA_ID)).thenReturn(3L);
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(70L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(3L);

        folioService.cargarCaf(request);

        ArgumentCaptor<ControlFolioEntity> controlCaptor = ArgumentCaptor.forClass(ControlFolioEntity.class);
        verify(controlFolioRepository).save(controlCaptor.capture());
        ControlFolioEntity control = controlCaptor.getValue();
        assertThat(control).isSameAs(existente);
        assertThat(control.getCafActivo()).isSameAs(guardado);
        // Conserva el último folio previo
        assertThat(control.getUltimoFolioUtilizado()).isEqualTo(8);
        // Se verificó permiso sobre el registro existente
        verify(controlFolioRepository).findByIdAndEmpresaId(99L, EMPRESA_ID);
    }

    @Test
    void cargarCaf_rangoInvertido_lanzaReglaNegocio() {
        CafCargaRequest request = nuevaRequest(10, 5);

        assertThatThrownBy(() -> folioService.cargarCaf(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rango desde no puede ser mayor que el rango hasta");

        verifyNoInteractions(cafRepository);
        verify(folioRepository, never()).saveAll(any());
        verify(controlFolioRepository, never()).save(any());
    }

    @Test
    void cargarCaf_rangoSuperpuesto_lanzaReglaNegocio() {
        CafCargaRequest request = nuevaRequest(1, 3);

        when(tipoDocumentoService.findByCodigoSii(CODIGO_TIPO)).thenReturn(tipo);
        when(folioRepository.existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(
                CODIGO_TIPO, EMPRESA_ID, 1, 3)).thenReturn(true);

        assertThatThrownBy(() -> folioService.cargarCaf(request))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rango del CAF se superpone con folios ya existentes");

        verifyNoInteractions(cafRepository);
        verify(folioRepository, never()).saveAll(any());
        verify(controlFolioRepository, never()).save(any());
    }

    @Test
    void cargarCaf_tipoDocumentoInexistente_propagaRecursoNoEncontrado() {
        CafCargaRequest request = nuevaRequest(1, 3);

        when(tipoDocumentoService.findByCodigoSii(CODIGO_TIPO))
                .thenThrow(new RecursoNoEncontradoException("Tipo de documento no existe: " + CODIGO_TIPO));

        assertThatThrownBy(() -> folioService.cargarCaf(request))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Tipo de documento no existe");

        verifyNoInteractions(cafRepository);
        verify(folioRepository, never()).saveAll(any());
    }

    // ----------------------------------------------------------------------
    // listar / listarFolios / listarFoliosPaginado / listarCafs
    // ----------------------------------------------------------------------

    @Test
    void listar_delegaEnFindAllByEmpresa() {
        ControlFolioEntity c1 = new ControlFolioEntity();
        ControlFolioEntity c2 = new ControlFolioEntity();
        when(controlFolioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of(c1, c2));

        List<ControlFolioEntity> resultado = folioService.listar();

        assertThat(resultado).containsExactly(c1, c2);
        verify(controlFolioRepository).findAllByEmpresaId(EMPRESA_ID);
    }

    @Test
    void listarFolios_delegaEnRepositorioPorEmpresa() {
        FolioEntity f = new FolioEntity();
        when(folioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of(f));

        List<FolioEntity> resultado = folioService.listarFolios();

        assertThat(resultado).containsExactly(f);
        verify(folioRepository).findAllByEmpresaId(EMPRESA_ID);
    }

    @Test
    void listarFoliosPaginado_delegaEnRepositorioConPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        FolioEntity f = new FolioEntity();
        Page<FolioEntity> page = new PageImpl<>(List.of(f));
        when(folioRepository.findByEmpresaId(EMPRESA_ID, pageable)).thenReturn(page);

        Page<FolioEntity> resultado = folioService.listarFoliosPaginado(pageable);

        assertThat(resultado.getContent()).containsExactly(f);
        verify(folioRepository).findByEmpresaId(EMPRESA_ID, pageable);
    }

    @Test
    void listarCafs_mapeaCadaCafAResponse() {
        CafEntity caf = new CafEntity();
        caf.setId(80L);
        caf.setTipoDocumento(tipo);
        caf.setRangoDesde(1);
        caf.setRangoHasta(2);
        caf.setEstado(EstadoCaf.DISPONIBLE);

        when(cafRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of(caf));
        when(folioRepository.countByCafIdAndEmpresaId(80L, EMPRESA_ID)).thenReturn(2L);
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(80L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(1L);

        List<CafResponse> resultado = folioService.listarCafs();

        assertThat(resultado).hasSize(1);
        CafResponse r = resultado.get(0);
        assertThat(r.getId()).isEqualTo(80L);
        assertThat(r.getFoliosGenerados()).isEqualTo(2L);
        assertThat(r.getFoliosDisponibles()).isEqualTo(1L);
        assertThat(r.getEstado()).isEqualTo("DISPONIBLE");
    }

    @Test
    void listarCafs_sinCafs_retornaListaVacia() {
        when(cafRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        assertThat(folioService.listarCafs()).isEmpty();
        verify(folioRepository, never()).countByCafIdAndEmpresaId(anyLong(), anyLong());
    }

    // ----------------------------------------------------------------------
    // asignarSiguienteFolio
    // ----------------------------------------------------------------------

    @Test
    void asignarSiguienteFolio_caminoFeliz_cafConFoliosRestantes() {
        CafEntity caf = new CafEntity();
        caf.setId(100L);
        caf.setEstado(EstadoCaf.DISPONIBLE);

        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(5L);
        control.setTipoDocumento(tipo);
        control.setCafActivo(caf);

        FolioEntity folio = new FolioEntity();
        folio.setId(200L);
        folio.setNumero(1);
        folio.setCaf(caf);
        folio.setEstado(EstadoFolio.DISPONIBLE);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(control));
        when(folioRepository.findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                100L, EMPRESA_ID, EstadoFolio.DISPONIBLE)).thenReturn(Optional.of(folio));
        // Todavía quedan folios disponibles -> el CAF no se marca agotado
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(100L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(4L);

        Integer numero = folioService.asignarSiguienteFolio(CODIGO_TIPO);

        assertThat(numero).isEqualTo(1);
        // El folio termina en estado UTILIZADO
        assertThat(folio.getEstado()).isEqualTo(EstadoFolio.UTILIZADO);
        // Se guarda el folio dos veces (RESERVADO y luego UTILIZADO)
        verify(folioRepository, times(2)).save(folio);
        // Se actualiza el control con último folio y fecha de hoy
        assertThat(control.getUltimoFolioUtilizado()).isEqualTo(1);
        assertThat(control.getUltimaFechaEmision()).isEqualTo(LocalDate.now());
        verify(controlFolioRepository).save(control);
        // El CAF no se marca agotado porque quedan folios
        verify(cafRepository, never()).save(any(CafEntity.class));
        assertThat(caf.getEstado()).isEqualTo(EstadoCaf.DISPONIBLE);
    }

    @Test
    void asignarSiguienteFolio_ultimoFolio_marcaCafAgotado() {
        CafEntity caf = new CafEntity();
        caf.setId(110L);
        caf.setEstado(EstadoCaf.DISPONIBLE);

        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(6L);
        control.setTipoDocumento(tipo);
        control.setCafActivo(caf);

        FolioEntity folio = new FolioEntity();
        folio.setId(210L);
        folio.setNumero(99);
        folio.setCaf(caf);
        folio.setEstado(EstadoFolio.DISPONIBLE);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(control));
        when(folioRepository.findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                110L, EMPRESA_ID, EstadoFolio.DISPONIBLE)).thenReturn(Optional.of(folio));
        // Ya no quedan folios disponibles -> el CAF se marca agotado
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(110L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(0L);

        Integer numero = folioService.asignarSiguienteFolio(CODIGO_TIPO);

        assertThat(numero).isEqualTo(99);
        assertThat(caf.getEstado()).isEqualTo(EstadoCaf.AGOTADO);
        verify(cafRepository).save(caf);
    }

    @Test
    void asignarSiguienteFolio_cafYaAgotado_noVuelveAGuardar() {
        CafEntity caf = new CafEntity();
        caf.setId(120L);
        caf.setEstado(EstadoCaf.AGOTADO);

        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(7L);
        control.setTipoDocumento(tipo);
        control.setCafActivo(caf);

        FolioEntity folio = new FolioEntity();
        folio.setId(220L);
        folio.setNumero(50);
        folio.setCaf(caf);
        folio.setEstado(EstadoFolio.DISPONIBLE);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(control));
        when(folioRepository.findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                120L, EMPRESA_ID, EstadoFolio.DISPONIBLE)).thenReturn(Optional.of(folio));

        Integer numero = folioService.asignarSiguienteFolio(CODIGO_TIPO);

        assertThat(numero).isEqualTo(50);
        // El CAF ya estaba agotado: marcarCafAgotado retorna sin consultar disponibilidad ni guardar
        assertThat(caf.getEstado()).isEqualTo(EstadoCaf.AGOTADO);
        verify(cafRepository, never()).save(any(CafEntity.class));
        verify(folioRepository, never()).countByCafIdAndEmpresaIdAndEstado(anyLong(), anyLong(), any());
    }

    @Test
    void asignarSiguienteFolio_sinControl_lanzaReglaNegocio() {
        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> folioService.asignarSiguienteFolio(CODIGO_TIPO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No hay CAF cargado para el tipo " + CODIGO_TIPO);

        verify(folioRepository, never()).save(any());
    }

    @Test
    void asignarSiguienteFolio_sinCafActivo_lanzaReglaNegocio() {
        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(8L);
        control.setTipoDocumento(tipo);
        control.setCafActivo(null);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(control));

        assertThatThrownBy(() -> folioService.asignarSiguienteFolio(CODIGO_TIPO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No hay CAF activo para el tipo " + CODIGO_TIPO);

        verify(folioRepository, never()).findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                anyLong(), anyLong(), any());
        verify(folioRepository, never()).save(any());
    }

    @Test
    void asignarSiguienteFolio_sinFoliosDisponibles_lanzaReglaNegocioYMarcaCafAgotado() {
        CafEntity caf = new CafEntity();
        caf.setId(130L);
        caf.setEstado(EstadoCaf.DISPONIBLE);

        ControlFolioEntity control = new ControlFolioEntity();
        control.setId(9L);
        control.setTipoDocumento(tipo);
        control.setCafActivo(caf);

        when(controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(CODIGO_TIPO, EMPRESA_ID))
                .thenReturn(Optional.of(control));
        when(folioRepository.findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                130L, EMPRESA_ID, EstadoFolio.DISPONIBLE)).thenReturn(Optional.empty());
        // marcarCafAgotado se invoca dentro del orElseThrow: no quedan folios
        when(folioRepository.countByCafIdAndEmpresaIdAndEstado(130L, EMPRESA_ID, EstadoFolio.DISPONIBLE))
                .thenReturn(0L);

        assertThatThrownBy(() -> folioService.asignarSiguienteFolio(CODIGO_TIPO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No hay folios disponibles para el tipo " + CODIGO_TIPO);

        // Al no haber folios, el CAF activo se marca agotado
        assertThat(caf.getEstado()).isEqualTo(EstadoCaf.AGOTADO);
        verify(cafRepository).save(caf);
        verify(folioRepository, never()).save(any());
        verify(controlFolioRepository, never()).save(any());
    }

    @Test
    void getEmpresaId_seResuelveDesdeTenantContext() {
        // Verifica explícitamente que el servicio obtiene la empresa del tenantService
        when(folioRepository.findAllByEmpresaId(EMPRESA_ID)).thenReturn(List.of());

        folioService.listarFolios();

        verify(tenantService).getEmpresaId();
        verify(folioRepository).findAllByEmpresaId(eq(EMPRESA_ID));
    }
}
