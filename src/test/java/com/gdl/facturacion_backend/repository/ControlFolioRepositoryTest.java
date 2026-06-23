package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.CafEntity;
import com.gdl.facturacion_backend.entity.ControlFolioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoCaf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ControlFolioRepositoryTest {

    @Autowired
    private ControlFolioRepository controlFolioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EmpresaEntity empresa;
    private EmpresaEntity otraEmpresa;
    private TipoDocumentoEntity tipoFactura;
    private TipoDocumentoEntity tipoBoleta;
    private CafEntity cafFactura;
    private CafEntity cafBoleta;

    @BeforeEach
    void setUp() {
        empresa = persistEmpresa("76111111-1", "Empresa Uno SpA");
        otraEmpresa = persistEmpresa("76222222-2", "Empresa Dos SpA");

        tipoFactura = persistTipoDocumento(33, "Factura Electrónica");
        tipoBoleta = persistTipoDocumento(39, "Boleta Electrónica");

        cafFactura = persistCaf(empresa, tipoFactura, 1, 100);
        cafBoleta = persistCaf(empresa, tipoBoleta, 1, 50);
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

    private ControlFolioEntity persistControlFolio(EmpresaEntity emp, TipoDocumentoEntity tipo,
                                                   CafEntity caf, Integer ultimoFolio, LocalDate fecha) {
        ControlFolioEntity cf = new ControlFolioEntity();
        cf.setEmpresa(emp);
        cf.setTipoDocumento(tipo);
        cf.setCafActivo(caf);
        cf.setUltimoFolioUtilizado(ultimoFolio);
        cf.setUltimaFechaEmision(fecha);
        return entityManager.persistAndFlush(cf);
    }

    // ---------- findByTipoDocumentoCodigoSiiAndEmpresaId ----------

    @Test
    void findByTipoCodigoSiiAndEmpresaId_coincide_devuelveControlFolio() {
        ControlFolioEntity guardado = persistControlFolio(
                empresa, tipoFactura, cafFactura, 42, LocalDate.of(2026, 1, 15));
        entityManager.clear();

        Optional<ControlFolioEntity> encontrado =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(33, empresa.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(guardado.getId());
        assertThat(encontrado.get().getUltimoFolioUtilizado()).isEqualTo(42);
        assertThat(encontrado.get().getUltimaFechaEmision()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(encontrado.get().getTipoDocumento().getCodigoSii()).isEqualTo(33);
        assertThat(encontrado.get().getCafActivo().getId()).isEqualTo(cafFactura.getId());
        assertThat(encontrado.get().getEmpresa().getId()).isEqualTo(empresa.getId());
    }

    @Test
    void findByTipoCodigoSiiAndEmpresaId_codigoSiiInexistente_vacio() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 10, LocalDate.now());
        entityManager.clear();

        // 56 = Nota de Débito: no existe ningún control de folio con ese tipo
        Optional<ControlFolioEntity> encontrado =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(56, empresa.getId());

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByTipoCodigoSiiAndEmpresaId_otraEmpresa_vacio() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 10, LocalDate.now());
        entityManager.clear();

        // Mismo codigoSii pero buscando por otra empresa que no tiene control de folio
        Optional<ControlFolioEntity> encontrado =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(33, otraEmpresa.getId());

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByTipoCodigoSiiAndEmpresaId_aislaPorEmpresa() {
        // Misma empresa para factura
        persistControlFolio(empresa, tipoFactura, cafFactura, 5, LocalDate.now());
        // Otra empresa con su propio control de folio para el mismo tipo de documento
        CafEntity cafFacturaOtra = persistCaf(otraEmpresa, tipoFactura, 1, 100);
        persistControlFolio(otraEmpresa, tipoFactura, cafFacturaOtra, 99, LocalDate.now());
        entityManager.clear();

        Optional<ControlFolioEntity> deEmpresa =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(33, empresa.getId());
        Optional<ControlFolioEntity> deOtraEmpresa =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(33, otraEmpresa.getId());

        assertThat(deEmpresa).isPresent();
        assertThat(deEmpresa.get().getUltimoFolioUtilizado()).isEqualTo(5);
        assertThat(deEmpresa.get().getEmpresa().getId()).isEqualTo(empresa.getId());

        assertThat(deOtraEmpresa).isPresent();
        assertThat(deOtraEmpresa.get().getUltimoFolioUtilizado()).isEqualTo(99);
        assertThat(deOtraEmpresa.get().getEmpresa().getId()).isEqualTo(otraEmpresa.getId());
    }

    @Test
    void findByTipoCodigoSiiAndEmpresaId_distintosTiposEnMismaEmpresa() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 5, LocalDate.now());
        persistControlFolio(empresa, tipoBoleta, cafBoleta, 17, LocalDate.now());
        entityManager.clear();

        Optional<ControlFolioEntity> factura =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(33, empresa.getId());
        Optional<ControlFolioEntity> boleta =
                controlFolioRepository.findByTipoDocumentoCodigoSiiAndEmpresaId(39, empresa.getId());

        assertThat(factura).isPresent();
        assertThat(factura.get().getUltimoFolioUtilizado()).isEqualTo(5);
        assertThat(factura.get().getTipoDocumento().getCodigoSii()).isEqualTo(33);

        assertThat(boleta).isPresent();
        assertThat(boleta.get().getUltimoFolioUtilizado()).isEqualTo(17);
        assertThat(boleta.get().getTipoDocumento().getCodigoSii()).isEqualTo(39);
    }

    // ---------- métodos heredados de BaseTenantRepository ----------

    @Test
    void findAllByEmpresaId_devuelveSoloDeLaEmpresa() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 1, LocalDate.now());
        persistControlFolio(empresa, tipoBoleta, cafBoleta, 2, LocalDate.now());
        CafEntity cafFacturaOtra = persistCaf(otraEmpresa, tipoFactura, 1, 100);
        persistControlFolio(otraEmpresa, tipoFactura, cafFacturaOtra, 3, LocalDate.now());
        entityManager.clear();

        List<ControlFolioEntity> deEmpresa = controlFolioRepository.findAllByEmpresaId(empresa.getId());
        List<ControlFolioEntity> deOtraEmpresa = controlFolioRepository.findAllByEmpresaId(otraEmpresa.getId());

        assertThat(deEmpresa).hasSize(2);
        assertThat(deEmpresa).allMatch(cf -> cf.getEmpresa().getId().equals(empresa.getId()));
        assertThat(deOtraEmpresa).hasSize(1);
        assertThat(deOtraEmpresa).allMatch(cf -> cf.getEmpresa().getId().equals(otraEmpresa.getId()));
    }

    @Test
    void findAllByEmpresaId_empresaSinControlFolios_listaVacia() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 1, LocalDate.now());
        entityManager.clear();

        assertThat(controlFolioRepository.findAllByEmpresaId(otraEmpresa.getId())).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_coincide_yNoCoincide() {
        ControlFolioEntity cf = persistControlFolio(empresa, tipoFactura, cafFactura, 8, LocalDate.now());
        entityManager.clear();

        assertThat(controlFolioRepository.findByIdAndEmpresaId(cf.getId(), empresa.getId())).isPresent();
        assertThat(controlFolioRepository.findByIdAndEmpresaId(cf.getId(), otraEmpresa.getId())).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_idInexistente_vacio() {
        persistControlFolio(empresa, tipoFactura, cafFactura, 8, LocalDate.now());
        entityManager.clear();

        assertThat(controlFolioRepository.findByIdAndEmpresaId(999999L, empresa.getId())).isEmpty();
    }

    // ---------- persistencia básica / PrePersist ----------

    @Test
    void guardarControlFolio_persisteCamposYTimestamps() {
        ControlFolioEntity cf = persistControlFolio(
                empresa, tipoFactura, cafFactura, 12, LocalDate.of(2026, 6, 22));
        entityManager.clear();

        ControlFolioEntity recuperado = controlFolioRepository.findById(cf.getId()).orElseThrow();

        assertThat(recuperado.getUltimoFolioUtilizado()).isEqualTo(12);
        assertThat(recuperado.getUltimaFechaEmision()).isEqualTo(LocalDate.of(2026, 6, 22));
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(recuperado.getTipoDocumento().getCodigoSii()).isEqualTo(33);
        assertThat(recuperado.getCafActivo().getId()).isEqualTo(cafFactura.getId());
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getUpdatedAt()).isNotNull();
        assertThat(recuperado.getCanDelete()).isTrue();
    }

    @Test
    void guardarControlFolio_camposOpcionalesNulos() {
        ControlFolioEntity cf = persistControlFolio(empresa, tipoFactura, cafFactura, null, null);
        entityManager.clear();

        ControlFolioEntity recuperado = controlFolioRepository.findById(cf.getId()).orElseThrow();

        assertThat(recuperado.getUltimoFolioUtilizado()).isNull();
        assertThat(recuperado.getUltimaFechaEmision()).isNull();
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
    }
}
