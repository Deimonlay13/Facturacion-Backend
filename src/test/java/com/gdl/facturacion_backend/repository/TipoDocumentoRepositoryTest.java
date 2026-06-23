package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TipoDocumentoRepositoryTest {

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Integer CODIGO_FACTURA = 33;
    private static final Integer CODIGO_BOLETA = 39;
    private static final Integer CODIGO_NOTA_CREDITO = 61;

    @BeforeEach
    void setUp() {
        // TipoDocumentoEntity extiende BaseGlobalEntity (entidad global, sin empresa/tenant),
        // por lo que NO requiere persistir una EmpresaEntity ni setear empresa.
    }

    // ---------- Helper ----------

    private TipoDocumentoEntity persistTipoDocumento(Integer codigoSii, String descripcion) {
        TipoDocumentoEntity td = new TipoDocumentoEntity();
        td.setCodigoSii(codigoSii);
        td.setDescripcion(descripcion);
        return entityManager.persistAndFlush(td);
    }

    // ---------- findByCodigoSii ----------

    @Test
    void findByCodigoSii_codigoExistente_devuelveTipoDocumento() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        Optional<TipoDocumentoEntity> encontrado = tipoDocumentoRepository.findByCodigoSii(CODIGO_FACTURA);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigoSii()).isEqualTo(CODIGO_FACTURA);
        assertThat(encontrado.get().getDescripcion()).isEqualTo("Factura Electrónica");
        assertThat(encontrado.get().getId()).isNotNull();
    }

    @Test
    void findByCodigoSii_codigoInexistente_vacio() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        Optional<TipoDocumentoEntity> encontrado = tipoDocumentoRepository.findByCodigoSii(999);

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByCodigoSii_sinDatos_vacio() {
        Optional<TipoDocumentoEntity> encontrado = tipoDocumentoRepository.findByCodigoSii(CODIGO_FACTURA);

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByCodigoSii_argumentoNull_vacio() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        Optional<TipoDocumentoEntity> encontrado = tipoDocumentoRepository.findByCodigoSii(null);

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByCodigoSii_seleccionaElCodigoCorrectoEntreVarios() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        persistTipoDocumento(CODIGO_BOLETA, "Boleta Electrónica");
        persistTipoDocumento(CODIGO_NOTA_CREDITO, "Nota de Crédito Electrónica");
        entityManager.clear();

        Optional<TipoDocumentoEntity> encontrado = tipoDocumentoRepository.findByCodigoSii(CODIGO_BOLETA);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigoSii()).isEqualTo(CODIGO_BOLETA);
        assertThat(encontrado.get().getDescripcion()).isEqualTo("Boleta Electrónica");
    }

    // ---------- existsByCodigoSii ----------

    @Test
    void existsByCodigoSii_codigoExistente_true() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        boolean existe = tipoDocumentoRepository.existsByCodigoSii(CODIGO_FACTURA);

        assertThat(existe).isTrue();
    }

    @Test
    void existsByCodigoSii_codigoInexistente_false() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        boolean existe = tipoDocumentoRepository.existsByCodigoSii(999);

        assertThat(existe).isFalse();
    }

    @Test
    void existsByCodigoSii_sinDatos_false() {
        boolean existe = tipoDocumentoRepository.existsByCodigoSii(CODIGO_FACTURA);

        assertThat(existe).isFalse();
    }

    @Test
    void existsByCodigoSii_argumentoNull_false() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        entityManager.clear();

        boolean existe = tipoDocumentoRepository.existsByCodigoSii(null);

        assertThat(existe).isFalse();
    }

    // ---------- restricción de unicidad codigo_sii ----------

    @Test
    void guardar_codigoSiiDuplicado_lanzaConstraintViolation() {
        // La columna codigo_sii es unique=true: no debe permitir duplicados.
        // Al persistir vía TestEntityManager (sin traducción de excepciones de Spring),
        // la violación de unicidad emerge como ConstraintViolationException de Hibernate.
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");

        assertThatThrownBy(() -> persistTipoDocumento(CODIGO_FACTURA, "Otra Factura"))
                .isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    @Test
    void guardar_codigoSiiNull_lanzaExcepcion() {
        // La columna codigo_sii es nullable=false.
        assertThatThrownBy(() -> persistTipoDocumento(null, "Sin código"))
                .isInstanceOf(Exception.class);
    }

    // ---------- persistencia básica / @PrePersist (BaseGlobalEntity) ----------

    @Test
    void guardarTipoDocumento_persisteCamposYTimestamps() {
        TipoDocumentoEntity guardado = persistTipoDocumento(CODIGO_NOTA_CREDITO, "Nota de Crédito Electrónica");
        entityManager.clear();

        TipoDocumentoEntity recuperado = tipoDocumentoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getCodigoSii()).isEqualTo(CODIGO_NOTA_CREDITO);
        assertThat(recuperado.getDescripcion()).isEqualTo("Nota de Crédito Electrónica");
        assertThat(recuperado.getCreatedAt()).isNotNull();
        // canDelete tiene valor por defecto true en BaseGlobalEntity.
        assertThat(recuperado.getCanDelete()).isTrue();
        // deletedAt no se setea al crear.
        assertThat(recuperado.getDeletedAt()).isNull();
    }

    @Test
    void guardarTipoDocumento_descripcionNull_permitido() {
        // descripcion no tiene restricción NOT NULL.
        TipoDocumentoEntity guardado = persistTipoDocumento(CODIGO_BOLETA, null);
        entityManager.clear();

        TipoDocumentoEntity recuperado = tipoDocumentoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getCodigoSii()).isEqualTo(CODIGO_BOLETA);
        assertThat(recuperado.getDescripcion()).isNull();
    }

    @Test
    void findAll_devuelveTodosLosTiposPersistidos() {
        persistTipoDocumento(CODIGO_FACTURA, "Factura Electrónica");
        persistTipoDocumento(CODIGO_BOLETA, "Boleta Electrónica");
        persistTipoDocumento(CODIGO_NOTA_CREDITO, "Nota de Crédito Electrónica");
        entityManager.clear();

        assertThat(tipoDocumentoRepository.findAll())
                .hasSize(3)
                .extracting(TipoDocumentoEntity::getCodigoSii)
                .containsExactlyInAnyOrder(CODIGO_FACTURA, CODIGO_BOLETA, CODIGO_NOTA_CREDITO);
    }
}
