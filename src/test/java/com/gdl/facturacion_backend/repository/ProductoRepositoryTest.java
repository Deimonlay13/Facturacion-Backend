package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EmpresaEntity empresa;
    private EmpresaEntity otraEmpresa;

    @BeforeEach
    void setUp() {
        empresa = persistEmpresa("76111111-1", "Empresa Uno SpA");
        otraEmpresa = persistEmpresa("76222222-2", "Empresa Dos SpA");
    }

    // ---------- Helpers ----------

    private EmpresaEntity persistEmpresa(String rut, String razonSocial) {
        EmpresaEntity e = new EmpresaEntity();
        e.setRutEmpresa(rut);
        e.setRazonSocial(razonSocial);
        e.setActivo(true);
        return entityManager.persistAndFlush(e);
    }

    private ProductoEntity persistProducto(EmpresaEntity emp, String codigo, String nombre) {
        ProductoEntity p = new ProductoEntity();
        // Se asigna la empresa explícitamente para no depender del TenantContext (@PrePersist).
        p.setEmpresa(emp);
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion("Descripción de " + nombre);
        p.setUnidadMedida("UN");
        p.setPrecio(1000.0);
        p.setAfectaIva(true);
        p.setActivo(true);
        return entityManager.persistAndFlush(p);
    }

    // ---------- existsByCodigoAndEmpresaId ----------

    @Test
    void existsByCodigoAndEmpresaId_codigoExistenteEnLaEmpresa_true() {
        persistProducto(empresa, "PROD-001", "Producto Uno");
        entityManager.clear();

        boolean existe = productoRepository.existsByCodigoAndEmpresaId("PROD-001", empresa.getId());

        assertThat(existe).isTrue();
    }

    @Test
    void existsByCodigoAndEmpresaId_codigoInexistente_false() {
        persistProducto(empresa, "PROD-001", "Producto Uno");
        entityManager.clear();

        boolean existe = productoRepository.existsByCodigoAndEmpresaId("NO-EXISTE", empresa.getId());

        assertThat(existe).isFalse();
    }

    @Test
    void existsByCodigoAndEmpresaId_mismoCodigoOtraEmpresa_false() {
        // El código existe pero pertenece a "empresa", no a "otraEmpresa".
        persistProducto(empresa, "PROD-001", "Producto Uno");
        entityManager.clear();

        boolean existe = productoRepository.existsByCodigoAndEmpresaId("PROD-001", otraEmpresa.getId());

        assertThat(existe).isFalse();
    }

    @Test
    void existsByCodigoAndEmpresaId_mismoCodigoEnDistintasEmpresas_aislamientoPorTenant() {
        // El mismo código puede coexistir en empresas distintas (multi-tenant).
        persistProducto(empresa, "PROD-DUP", "Producto Empresa Uno");
        persistProducto(otraEmpresa, "PROD-DUP", "Producto Empresa Dos");
        entityManager.clear();

        assertThat(productoRepository.existsByCodigoAndEmpresaId("PROD-DUP", empresa.getId())).isTrue();
        assertThat(productoRepository.existsByCodigoAndEmpresaId("PROD-DUP", otraEmpresa.getId())).isTrue();
    }

    @Test
    void existsByCodigoAndEmpresaId_empresaSinProductos_false() {
        boolean existe = productoRepository.existsByCodigoAndEmpresaId("PROD-001", empresa.getId());

        assertThat(existe).isFalse();
    }

    // ---------- findAllByEmpresaId (heredado de BaseTenantRepository) ----------

    @Test
    void findAllByEmpresaId_devuelveSoloProductosDeLaEmpresa() {
        persistProducto(empresa, "P-1", "Producto 1");
        persistProducto(empresa, "P-2", "Producto 2");
        persistProducto(otraEmpresa, "P-3", "Producto 3");
        entityManager.clear();

        List<ProductoEntity> deEmpresa = productoRepository.findAllByEmpresaId(empresa.getId());
        List<ProductoEntity> deOtra = productoRepository.findAllByEmpresaId(otraEmpresa.getId());

        assertThat(deEmpresa).hasSize(2);
        assertThat(deEmpresa).allMatch(p -> p.getEmpresa().getId().equals(empresa.getId()));
        assertThat(deEmpresa).extracting(ProductoEntity::getCodigo)
                .containsExactlyInAnyOrder("P-1", "P-2");
        assertThat(deOtra).hasSize(1);
        assertThat(deOtra.get(0).getCodigo()).isEqualTo("P-3");
    }

    @Test
    void findAllByEmpresaId_empresaSinProductos_listaVacia() {
        persistProducto(empresa, "P-1", "Producto 1");
        entityManager.clear();

        List<ProductoEntity> resultado = productoRepository.findAllByEmpresaId(otraEmpresa.getId());

        assertThat(resultado).isEmpty();
    }

    // ---------- findByIdAndEmpresaId (heredado de BaseTenantRepository) ----------

    @Test
    void findByIdAndEmpresaId_coincideEmpresa_devuelveProducto() {
        ProductoEntity producto = persistProducto(empresa, "P-1", "Producto 1");
        entityManager.clear();

        Optional<ProductoEntity> encontrado =
                productoRepository.findByIdAndEmpresaId(producto.getId(), empresa.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCodigo()).isEqualTo("P-1");
        assertThat(encontrado.get().getEmpresa().getId()).isEqualTo(empresa.getId());
    }

    @Test
    void findByIdAndEmpresaId_empresaDistinta_vacio() {
        ProductoEntity producto = persistProducto(empresa, "P-1", "Producto 1");
        entityManager.clear();

        Optional<ProductoEntity> encontrado =
                productoRepository.findByIdAndEmpresaId(producto.getId(), otraEmpresa.getId());

        assertThat(encontrado).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_idInexistente_vacio() {
        persistProducto(empresa, "P-1", "Producto 1");
        entityManager.clear();

        Optional<ProductoEntity> encontrado =
                productoRepository.findByIdAndEmpresaId(999999L, empresa.getId());

        assertThat(encontrado).isEmpty();
    }

    // ---------- persistencia básica / PrePersist ----------

    @Test
    void guardarProducto_persisteCamposYTimestamps() {
        ProductoEntity producto = persistProducto(empresa, "P-1", "Producto 1");
        entityManager.clear();

        ProductoEntity recuperado = productoRepository.findById(producto.getId()).orElseThrow();

        assertThat(recuperado.getCodigo()).isEqualTo("P-1");
        assertThat(recuperado.getNombre()).isEqualTo("Producto 1");
        assertThat(recuperado.getDescripcion()).isEqualTo("Descripción de Producto 1");
        assertThat(recuperado.getUnidadMedida()).isEqualTo("UN");
        assertThat(recuperado.getPrecio()).isEqualTo(1000.0);
        assertThat(recuperado.getAfectaIva()).isTrue();
        assertThat(recuperado.getActivo()).isTrue();
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getUpdatedAt()).isNotNull();
        assertThat(recuperado.getCanDelete()).isTrue();
    }
}
