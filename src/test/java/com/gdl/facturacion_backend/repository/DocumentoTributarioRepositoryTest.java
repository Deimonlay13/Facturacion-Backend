package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.DocumentoTributarioEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.enums.EstadoDocumento;
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
class DocumentoTributarioRepositoryTest {

    @Autowired
    private DocumentoTributarioRepository documentoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EmpresaEntity empresa;
    private EmpresaEntity otraEmpresa;

    private TipoDocumentoEntity tipoFactura;   // codigoSii 33
    private TipoDocumentoEntity tipoBoleta;    // codigoSii 39

    private ClienteEntity clienteA;
    private ClienteEntity clienteB;

    @BeforeEach
    void setUp() {
        empresa = persistEmpresa("76111111-1", "Empresa Uno SpA");
        otraEmpresa = persistEmpresa("76222222-2", "Empresa Dos SpA");

        tipoFactura = persistTipoDocumento(33, "Factura Electrónica");
        tipoBoleta = persistTipoDocumento(39, "Boleta Electrónica");

        clienteA = persistCliente(empresa, "11111111-1", "Cliente A SpA");
        clienteB = persistCliente(empresa, "22222222-2", "Cliente B SpA");
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

    private ClienteEntity persistCliente(EmpresaEntity emp, String rut, String razonSocial) {
        ClienteEntity c = new ClienteEntity();
        c.setEmpresa(emp);
        c.setRut(rut);
        c.setRazonSocial(razonSocial);
        c.setActivo(true);
        return entityManager.persistAndFlush(c);
    }

    private DocumentoTributarioEntity persistDocumento(EmpresaEntity emp,
                                                       TipoDocumentoEntity tipo,
                                                       ClienteEntity cliente,
                                                       Integer folio,
                                                       LocalDate fechaEmision,
                                                       EstadoDocumento estado) {
        DocumentoTributarioEntity d = new DocumentoTributarioEntity();
        d.setEmpresa(emp);
        d.setTipoDocumento(tipo);
        d.setCliente(cliente);
        d.setFolio(folio);
        d.setFechaEmision(fechaEmision);
        d.setEstado(estado);
        return entityManager.persistAndFlush(d);
    }

    // ---------- buscar: sin filtros (solo empresa) ----------

    @Test
    void buscar_sinFiltros_devuelveSoloDeLaEmpresaOrdenadosPorIdDesc() {
        DocumentoTributarioEntity d1 = persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        DocumentoTributarioEntity d2 = persistDocumento(empresa, tipoBoleta, clienteB, 2,
                LocalDate.of(2026, 2, 10), EstadoDocumento.BORRADOR);
        // documento de otra empresa que NO debe aparecer
        persistDocumento(otraEmpresa, tipoFactura, null, 99,
                LocalDate.of(2026, 3, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), null, null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result)
                .allMatch(d -> d.getEmpresa().getId().equals(empresa.getId()));
        // ORDER BY d.id DESC -> el último insertado primero
        assertThat(result).extracting(DocumentoTributarioEntity::getId)
                .containsExactly(d2.getId(), d1.getId());
    }

    @Test
    void buscar_empresaSinDocumentos_devuelveVacio() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                otraEmpresa.getId(), null, null, null, null, null);

        assertThat(result).isEmpty();
    }

    // ---------- buscar: filtro por cliente ----------

    @Test
    void buscar_filtraPorCliente() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteB, 2,
                LocalDate.of(2026, 1, 11), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), clienteA.getId(), null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCliente().getId()).isEqualTo(clienteA.getId());
    }

    @Test
    void buscar_filtroClienteSinCoincidencias_devuelveVacio() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), clienteB.getId(), null, null, null, null);

        assertThat(result).isEmpty();
    }

    // ---------- buscar: filtro por codigoTipo (codigoSii) ----------

    @Test
    void buscar_filtraPorCodigoTipo() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoBoleta, clienteA, 2,
                LocalDate.of(2026, 1, 11), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> facturas = documentoRepository.buscar(
                empresa.getId(), null, 33, null, null, null);
        List<DocumentoTributarioEntity> boletas = documentoRepository.buscar(
                empresa.getId(), null, 39, null, null, null);

        assertThat(facturas).hasSize(1);
        assertThat(facturas.get(0).getTipoDocumento().getCodigoSii()).isEqualTo(33);
        assertThat(boletas).hasSize(1);
        assertThat(boletas.get(0).getTipoDocumento().getCodigoSii()).isEqualTo(39);
    }

    @Test
    void buscar_filtroCodigoTipoInexistente_devuelveVacio() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), null, 61, null, null, null);

        assertThat(result).isEmpty();
    }

    // ---------- buscar: filtro por estado ----------

    @Test
    void buscar_filtraPorEstado() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.BORRADOR);
        persistDocumento(empresa, tipoFactura, clienteA, 2,
                LocalDate.of(2026, 1, 11), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteA, 3,
                LocalDate.of(2026, 1, 12), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> borradores = documentoRepository.buscar(
                empresa.getId(), null, null, EstadoDocumento.BORRADOR, null, null);
        List<DocumentoTributarioEntity> emitidos = documentoRepository.buscar(
                empresa.getId(), null, null, EstadoDocumento.EMITIDO, null, null);

        assertThat(borradores).hasSize(1);
        assertThat(borradores).allMatch(d -> d.getEstado() == EstadoDocumento.BORRADOR);
        assertThat(emitidos).hasSize(2);
        assertThat(emitidos).allMatch(d -> d.getEstado() == EstadoDocumento.EMITIDO);
    }

    // ---------- buscar: filtro por rango de fechas (desde/hasta - Between) ----------

    @Test
    void buscar_filtraPorRangoFechas_inclusivoEnLimites() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 1), EstadoDocumento.EMITIDO);   // antes del rango
        persistDocumento(empresa, tipoFactura, clienteA, 2,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);  // límite inferior
        persistDocumento(empresa, tipoFactura, clienteA, 3,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);  // dentro
        persistDocumento(empresa, tipoFactura, clienteA, 4,
                LocalDate.of(2026, 1, 20), EstadoDocumento.EMITIDO);  // límite superior
        persistDocumento(empresa, tipoFactura, clienteA, 5,
                LocalDate.of(2026, 1, 31), EstadoDocumento.EMITIDO);  // después del rango
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), null, null, null,
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(DocumentoTributarioEntity::getFolio)
                .containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    void buscar_soloDesde_devuelveDesdeFechaEnAdelante() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 5), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteA, 2,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteA, 3,
                LocalDate.of(2026, 1, 25), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), null, null, null,
                LocalDate.of(2026, 1, 15), null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DocumentoTributarioEntity::getFolio)
                .containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void buscar_soloHasta_devuelveHastaFecha() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 5), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteA, 2,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoFactura, clienteA, 3,
                LocalDate.of(2026, 1, 25), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), null, null, null,
                null, LocalDate.of(2026, 1, 15));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DocumentoTributarioEntity::getFolio)
                .containsExactlyInAnyOrder(1, 2);
    }

    // ---------- buscar: combinación de filtros ----------

    @Test
    void buscar_combinaTodosLosFiltros() {
        // El que cumple todo
        DocumentoTributarioEntity esperado = persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        // mismo todo pero otro cliente
        persistDocumento(empresa, tipoFactura, clienteB, 2,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        // mismo todo pero otro tipo
        persistDocumento(empresa, tipoBoleta, clienteA, 3,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        // mismo todo pero otro estado
        persistDocumento(empresa, tipoFactura, clienteA, 4,
                LocalDate.of(2026, 1, 15), EstadoDocumento.BORRADOR);
        // mismo todo pero fuera de rango
        persistDocumento(empresa, tipoFactura, clienteA, 5,
                LocalDate.of(2026, 2, 15), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                empresa.getId(), clienteA.getId(), 33, EstadoDocumento.EMITIDO,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(esperado.getId());
    }

    @Test
    void buscar_aislaPorEmpresaAunConFiltros() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        // otra empresa, mismo tipo y estado: no debe filtrarse hacia la primera empresa
        persistDocumento(otraEmpresa, tipoFactura, null, 1,
                LocalDate.of(2026, 1, 15), EstadoDocumento.EMITIDO);
        entityManager.clear();

        List<DocumentoTributarioEntity> result = documentoRepository.buscar(
                otraEmpresa.getId(), null, 33, EstadoDocumento.EMITIDO, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmpresa().getId()).isEqualTo(otraEmpresa.getId());
    }

    // ---------- métodos heredados de BaseTenantRepository ----------

    @Test
    void findAllByEmpresaId_devuelveSoloDeLaEmpresa() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        persistDocumento(empresa, tipoBoleta, clienteB, 2,
                LocalDate.of(2026, 1, 11), EstadoDocumento.BORRADOR);
        persistDocumento(otraEmpresa, tipoFactura, null, 1,
                LocalDate.of(2026, 1, 12), EstadoDocumento.EMITIDO);
        entityManager.clear();

        assertThat(documentoRepository.findAllByEmpresaId(empresa.getId())).hasSize(2);
        assertThat(documentoRepository.findAllByEmpresaId(otraEmpresa.getId())).hasSize(1);
    }

    @Test
    void findAllByEmpresaId_empresaSinDocumentos_vacio() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        assertThat(documentoRepository.findAllByEmpresaId(otraEmpresa.getId())).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_coincide_yNoCoincide() {
        DocumentoTributarioEntity doc = persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        Optional<DocumentoTributarioEntity> encontrado =
                documentoRepository.findByIdAndEmpresaId(doc.getId(), empresa.getId());
        Optional<DocumentoTributarioEntity> noEncontrado =
                documentoRepository.findByIdAndEmpresaId(doc.getId(), otraEmpresa.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getFolio()).isEqualTo(1);
        assertThat(noEncontrado).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_idInexistente_vacio() {
        persistDocumento(empresa, tipoFactura, clienteA, 1,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        assertThat(documentoRepository.findByIdAndEmpresaId(999999L, empresa.getId())).isEmpty();
    }

    // ---------- persistencia básica / PrePersist (BaseModelEntity) ----------

    @Test
    void guardarDocumento_persisteCamposYTimestamps() {
        DocumentoTributarioEntity doc = persistDocumento(empresa, tipoFactura, clienteA, 7,
                LocalDate.of(2026, 1, 10), EstadoDocumento.EMITIDO);
        entityManager.clear();

        DocumentoTributarioEntity recuperado =
                documentoRepository.findById(doc.getId()).orElseThrow();

        assertThat(recuperado.getFolio()).isEqualTo(7);
        assertThat(recuperado.getEstado()).isEqualTo(EstadoDocumento.EMITIDO);
        assertThat(recuperado.getFechaEmision()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(recuperado.getCliente().getId()).isEqualTo(clienteA.getId());
        assertThat(recuperado.getTipoDocumento().getCodigoSii()).isEqualTo(33);
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getUpdatedAt()).isNotNull();
    }
}
