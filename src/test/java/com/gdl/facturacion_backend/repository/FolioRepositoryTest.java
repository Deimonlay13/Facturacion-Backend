package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.FolioEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoCaf;
import com.gdl.facturacion_backend.enums.EstadoFolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FolioRepositoryTest {

    @Autowired
    private FolioRepository folioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EmpresaEntity empresa;
    private EmpresaEntity otraEmpresa;
    private TipoDocumentoEntity tipoFactura;
    private TipoDocumentoEntity tipoBoleta;
    private CafEntity caf;
    private CafEntity otroCaf;

    @BeforeEach
    void setUp() {
        empresa = persistEmpresa("76111111-1", "Empresa Uno SpA");
        otraEmpresa = persistEmpresa("76222222-2", "Empresa Dos SpA");

        tipoFactura = persistTipoDocumento(33, "Factura Electrónica");
        tipoBoleta = persistTipoDocumento(39, "Boleta Electrónica");

        caf = persistCaf(empresa, tipoFactura, 1, 100);
        otroCaf = persistCaf(empresa, tipoBoleta, 1, 50);
    }

    // ---------- Helpers ----------

    private EmpresaEntity persistEmpresa(String rut, String razonSocial) {
        EmpresaEntity e = new EmpresaEntity();
        e.setRutEmpresa(rut);
        e.setRazonSocial(razonSocial);
        e.setActivo(true);
        return entityManager.persistAndFlush(e);
    }

    private TipoDocumentoEntity persistTipoDocumento(Integer codigoSii, String descripcion) {
        TipoDocumentoEntity t = new TipoDocumentoEntity();
        t.setCodigoSii(codigoSii);
        t.setDescripcion(descripcion);
        return entityManager.persistAndFlush(t);
    }

    private CafEntity persistCaf(EmpresaEntity emp, TipoDocumentoEntity tipo, Integer desde, Integer hasta) {
        CafEntity c = new CafEntity();
        c.setEmpresa(emp);
        c.setTipoDocumento(tipo);
        c.setRangoDesde(desde);
        c.setRangoHasta(hasta);
        c.setEstado(EstadoCaf.DISPONIBLE);
        return entityManager.persistAndFlush(c);
    }

    private FolioEntity persistFolio(EmpresaEntity emp, CafEntity cafEnt, TipoDocumentoEntity tipo,
                                     Integer numero, EstadoFolio estado) {
        FolioEntity f = new FolioEntity();
        f.setEmpresa(emp);
        f.setCaf(cafEnt);
        f.setTipoDocumento(tipo);
        f.setNumero(numero);
        f.setEstado(estado);
        return entityManager.persistAndFlush(f);
    }

    // ---------- findByEmpresaId(Pageable) ----------

    @Test
    void findByEmpresaId_devuelveSoloFoliosDeLaEmpresaPaginados() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 2, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 3, EstadoFolio.RESERVADO);
        // Folio de otra empresa (con su propio caf/tipo válido) que NO debe aparecer
        CafEntity cafOtra = persistCaf(otraEmpresa, tipoFactura, 1, 100);
        persistFolio(otraEmpresa, cafOtra, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        Page<FolioEntity> page = folioRepository.findByEmpresaId(empresa.getId(), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .allMatch(f -> f.getEmpresa().getId().equals(empresa.getId()));
    }

    @Test
    void findByEmpresaId_segundaPagina() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 2, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 3, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        Page<FolioEntity> page = folioRepository.findByEmpresaId(empresa.getId(), PageRequest.of(1, 2));

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void findByEmpresaId_empresaSinFolios_devuelvePaginaVacia() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        Page<FolioEntity> page = folioRepository.findByEmpresaId(otraEmpresa.getId(), Pageable.unpaged());

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    // ---------- existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween ----------

    @Test
    void existsByTipoCodigoSiiEmpresaNumeroBetween_dentroDelRango_true() {
        persistFolio(empresa, caf, tipoFactura, 50, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        boolean existe = folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(33, empresa.getId(), 40, 60);

        assertThat(existe).isTrue();
    }

    @Test
    void existsByTipoCodigoSiiEmpresaNumeroBetween_enLosLimites_true() {
        persistFolio(empresa, caf, tipoFactura, 40, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 60, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        // límite inferior inclusivo
        assertThat(folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(33, empresa.getId(), 40, 40))
                .isTrue();
        // límite superior inclusivo
        assertThat(folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(33, empresa.getId(), 60, 60))
                .isTrue();
    }

    @Test
    void existsByTipoCodigoSiiEmpresaNumeroBetween_fueraDelRango_false() {
        persistFolio(empresa, caf, tipoFactura, 50, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        boolean existe = folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(33, empresa.getId(), 60, 80);

        assertThat(existe).isFalse();
    }

    @Test
    void existsByTipoCodigoSiiEmpresaNumeroBetween_otroTipoDocumento_false() {
        persistFolio(empresa, caf, tipoFactura, 50, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        // codigoSii 39 (boleta) no tiene folios en ese rango
        boolean existe = folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(39, empresa.getId(), 40, 60);

        assertThat(existe).isFalse();
    }

    @Test
    void existsByTipoCodigoSiiEmpresaNumeroBetween_otraEmpresa_false() {
        persistFolio(empresa, caf, tipoFactura, 50, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        boolean existe = folioRepository
                .existsByTipoDocumentoCodigoSiiAndEmpresaIdAndNumeroBetween(33, otraEmpresa.getId(), 40, 60);

        assertThat(existe).isFalse();
    }

    // ---------- countByCafIdAndEmpresaId ----------

    @Test
    void countByCafIdAndEmpresaId_cuentaTodosLosEstados() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 2, EstadoFolio.RESERVADO);
        persistFolio(empresa, caf, tipoFactura, 3, EstadoFolio.UTILIZADO);
        // folio de otro caf que NO debe contarse
        persistFolio(empresa, otroCaf, tipoBoleta, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        long total = folioRepository.countByCafIdAndEmpresaId(caf.getId(), empresa.getId());

        assertThat(total).isEqualTo(3);
    }

    @Test
    void countByCafIdAndEmpresaId_sinCoincidencias_cero() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        long total = folioRepository.countByCafIdAndEmpresaId(caf.getId(), otraEmpresa.getId());

        assertThat(total).isZero();
    }

    // ---------- countByCafIdAndEmpresaIdAndEstado ----------

    @Test
    void countByCafIdAndEmpresaIdAndEstado_filtraPorEstado() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 2, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 3, EstadoFolio.RESERVADO);
        persistFolio(empresa, caf, tipoFactura, 4, EstadoFolio.UTILIZADO);
        entityManager.clear();

        assertThat(folioRepository
                .countByCafIdAndEmpresaIdAndEstado(caf.getId(), empresa.getId(), EstadoFolio.DISPONIBLE))
                .isEqualTo(2);
        assertThat(folioRepository
                .countByCafIdAndEmpresaIdAndEstado(caf.getId(), empresa.getId(), EstadoFolio.RESERVADO))
                .isEqualTo(1);
        assertThat(folioRepository
                .countByCafIdAndEmpresaIdAndEstado(caf.getId(), empresa.getId(), EstadoFolio.ANULADO))
                .isZero();
    }

    // ---------- findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc ----------

    @Test
    void findFirstByCafEmpresaEstadoOrderByNumeroAsc_devuelveElDeMenorNumero() {
        persistFolio(empresa, caf, tipoFactura, 30, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 10, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 20, EstadoFolio.DISPONIBLE);
        // uno reservado con número aún menor, no debe elegirse al pedir DISPONIBLE
        persistFolio(empresa, caf, tipoFactura, 5, EstadoFolio.RESERVADO);
        entityManager.clear();

        Optional<FolioEntity> first = folioRepository
                .findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        caf.getId(), empresa.getId(), EstadoFolio.DISPONIBLE);

        assertThat(first).isPresent();
        assertThat(first.get().getNumero()).isEqualTo(10);
        assertThat(first.get().getEstado()).isEqualTo(EstadoFolio.DISPONIBLE);
    }

    @Test
    void findFirstByCafEmpresaEstadoOrderByNumeroAsc_sinFoliosEnEstado_vacio() {
        persistFolio(empresa, caf, tipoFactura, 10, EstadoFolio.UTILIZADO);
        entityManager.clear();

        Optional<FolioEntity> first = folioRepository
                .findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        caf.getId(), empresa.getId(), EstadoFolio.DISPONIBLE);

        assertThat(first).isEmpty();
    }

    @Test
    void findFirstByCafEmpresaEstadoOrderByNumeroAsc_otraEmpresa_vacio() {
        persistFolio(empresa, caf, tipoFactura, 10, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        Optional<FolioEntity> first = folioRepository
                .findFirstByCafIdAndEmpresaIdAndEstadoOrderByNumeroAsc(
                        caf.getId(), otraEmpresa.getId(), EstadoFolio.DISPONIBLE);

        assertThat(first).isEmpty();
    }

    // ---------- métodos heredados de BaseTenantRepository ----------

    @Test
    void findAllByEmpresaId_devuelveSoloDeLaEmpresa() {
        persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        persistFolio(empresa, caf, tipoFactura, 2, EstadoFolio.DISPONIBLE);
        CafEntity cafOtra = persistCaf(otraEmpresa, tipoFactura, 1, 100);
        persistFolio(otraEmpresa, cafOtra, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        assertThat(folioRepository.findAllByEmpresaId(empresa.getId())).hasSize(2);
        assertThat(folioRepository.findAllByEmpresaId(otraEmpresa.getId())).hasSize(1);
    }

    @Test
    void findByIdAndEmpresaId_coincide_yNoCoincide() {
        FolioEntity folio = persistFolio(empresa, caf, tipoFactura, 1, EstadoFolio.DISPONIBLE);
        entityManager.clear();

        assertThat(folioRepository.findByIdAndEmpresaId(folio.getId(), empresa.getId())).isPresent();
        assertThat(folioRepository.findByIdAndEmpresaId(folio.getId(), otraEmpresa.getId())).isEmpty();
    }

    // ---------- persistencia básica / PrePersist ----------

    @Test
    void guardarFolio_persisteCamposYTimestamps() {
        FolioEntity folio = persistFolio(empresa, caf, tipoFactura, 7, EstadoFolio.RESERVADO);
        entityManager.clear();

        FolioEntity recuperado = folioRepository.findById(folio.getId()).orElseThrow();

        assertThat(recuperado.getNumero()).isEqualTo(7);
        assertThat(recuperado.getEstado()).isEqualTo(EstadoFolio.RESERVADO);
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(recuperado.getCaf().getId()).isEqualTo(caf.getId());
        assertThat(recuperado.getTipoDocumento().getCodigoSii()).isEqualTo(33);
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getUpdatedAt()).isNotNull();
    }
}
