package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de capa de persistencia (@DataJpaTest sobre H2) para {@link ClienteRepository}.
 *
 * <p>Verifica cada método de consulta personalizado del repositorio, así como los
 * métodos heredados de {@link BaseTenantRepository}.</p>
 *
 * <p>Las entidades que extienden {@code BaseModelEntity} exigen una empresa por
 * {@code @PrePersist}; por eso se persiste una {@link EmpresaEntity} antes de
 * asociarla a cada cliente mediante {@code setEmpresa(...)}.</p>
 */
@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long empresaAId;
    private Long empresaBId;

    @BeforeEach
    void setUp() {
        EmpresaEntity empresaA = persistEmpresa("11.111.111-1", "Empresa A SpA");
        EmpresaEntity empresaB = persistEmpresa("22.222.222-2", "Empresa B SpA");
        empresaAId = empresaA.getId();
        empresaBId = empresaB.getId();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private EmpresaEntity persistEmpresa(String rutEmpresa, String razonSocial) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setRutEmpresa(rutEmpresa);
        empresa.setRazonSocial(razonSocial);
        empresa.setActivo(true);
        return entityManager.persistAndFlush(empresa);
    }

    private ClienteEntity persistCliente(EmpresaEntity empresa, String rut, String razonSocial) {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setEmpresa(empresa);
        cliente.setRut(rut);
        cliente.setRazonSocial(razonSocial);
        cliente.setActivo(true);
        return entityManager.persistAndFlush(cliente);
    }

    private EmpresaEntity empresaRef(Long id) {
        return entityManager.getEntityManager().getReference(EmpresaEntity.class, id);
    }

    // ---------------------------------------------------------------------
    // findByRutAndEmpresaId
    // ---------------------------------------------------------------------

    @Test
    void findByRutAndEmpresaId_devuelveClienteCuandoCoincideRutYEmpresa() {
        ClienteEntity guardado = persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");
        entityManager.clear();

        Optional<ClienteEntity> resultado =
                clienteRepository.findByRutAndEmpresaId("9.876.543-2", empresaAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(guardado.getId());
        assertThat(resultado.get().getRut()).isEqualTo("9.876.543-2");
        assertThat(resultado.get().getRazonSocial()).isEqualTo("Cliente Uno");
        assertThat(resultado.get().getEmpresa().getId()).isEqualTo(empresaAId);
    }

    @Test
    void findByRutAndEmpresaId_vacioCuandoRutNoExiste() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");

        Optional<ClienteEntity> resultado =
                clienteRepository.findByRutAndEmpresaId("0.000.000-0", empresaAId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByRutAndEmpresaId_vacioCuandoRutPerteneceAOtraEmpresa() {
        // El mismo RUT existe, pero asociado a empresa A; se consulta por empresa B.
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");

        Optional<ClienteEntity> resultado =
                clienteRepository.findByRutAndEmpresaId("9.876.543-2", empresaBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByRutAndEmpresaId_aislaPorEmpresaCuandoRutSeRepiteEntreEmpresas() {
        // Mismo RUT en dos empresas distintas: cada consulta debe traer el suyo.
        ClienteEntity enA = persistCliente(empresaRef(empresaAId), "5.555.555-5", "Cliente A");
        ClienteEntity enB = persistCliente(empresaRef(empresaBId), "5.555.555-5", "Cliente B");
        entityManager.clear();

        Optional<ClienteEntity> resA =
                clienteRepository.findByRutAndEmpresaId("5.555.555-5", empresaAId);
        Optional<ClienteEntity> resB =
                clienteRepository.findByRutAndEmpresaId("5.555.555-5", empresaBId);

        assertThat(resA).isPresent();
        assertThat(resA.get().getId()).isEqualTo(enA.getId());
        assertThat(resA.get().getRazonSocial()).isEqualTo("Cliente A");

        assertThat(resB).isPresent();
        assertThat(resB.get().getId()).isEqualTo(enB.getId());
        assertThat(resB.get().getRazonSocial()).isEqualTo("Cliente B");
    }

    // ---------------------------------------------------------------------
    // existsByRutAndEmpresaId
    // ---------------------------------------------------------------------

    @Test
    void existsByRutAndEmpresaId_trueCuandoExiste() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");

        boolean existe = clienteRepository.existsByRutAndEmpresaId("9.876.543-2", empresaAId);

        assertThat(existe).isTrue();
    }

    @Test
    void existsByRutAndEmpresaId_falseCuandoRutNoExiste() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");

        boolean existe = clienteRepository.existsByRutAndEmpresaId("0.000.000-0", empresaAId);

        assertThat(existe).isFalse();
    }

    @Test
    void existsByRutAndEmpresaId_falseCuandoRutPerteneceAOtraEmpresa() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Cliente Uno");

        boolean existe = clienteRepository.existsByRutAndEmpresaId("9.876.543-2", empresaBId);

        assertThat(existe).isFalse();
    }

    // ---------------------------------------------------------------------
    // findFirstByEmpresaIdAndRazonSocialIgnoreCase
    // ---------------------------------------------------------------------

    @Test
    void findFirstByEmpresaIdAndRazonSocialIgnoreCase_encuentraExactamente() {
        ClienteEntity guardado =
                persistCliente(empresaRef(empresaAId), "9.876.543-2", "Distribuidora Central");
        entityManager.clear();

        Optional<ClienteEntity> resultado =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaAId, "Distribuidora Central");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(guardado.getId());
    }

    @Test
    void findFirstByEmpresaIdAndRazonSocialIgnoreCase_ignoraMayusculas() {
        ClienteEntity guardado =
                persistCliente(empresaRef(empresaAId), "9.876.543-2", "Distribuidora Central");
        entityManager.clear();

        Optional<ClienteEntity> minusculas =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaAId, "distribuidora central");
        Optional<ClienteEntity> mayusculas =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaAId, "DISTRIBUIDORA CENTRAL");

        assertThat(minusculas).isPresent();
        assertThat(minusculas.get().getId()).isEqualTo(guardado.getId());
        assertThat(mayusculas).isPresent();
        assertThat(mayusculas.get().getId()).isEqualTo(guardado.getId());
    }

    @Test
    void findFirstByEmpresaIdAndRazonSocialIgnoreCase_vacioCuandoRazonSocialNoExiste() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Distribuidora Central");

        Optional<ClienteEntity> resultado =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaAId, "Comercial Inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findFirstByEmpresaIdAndRazonSocialIgnoreCase_vacioCuandoPerteneceAOtraEmpresa() {
        persistCliente(empresaRef(empresaAId), "9.876.543-2", "Distribuidora Central");

        Optional<ClienteEntity> resultado =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaBId, "Distribuidora Central");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findFirstByEmpresaIdAndRazonSocialIgnoreCase_devuelveSoloUnoCuandoHayVarios() {
        // "First" debe devolver un único resultado aun habiendo coincidencias múltiples.
        persistCliente(empresaRef(empresaAId), "1.111.111-1", "Razon Repetida");
        persistCliente(empresaRef(empresaAId), "2.222.222-2", "razon repetida");
        entityManager.clear();

        Optional<ClienteEntity> resultado =
                clienteRepository.findFirstByEmpresaIdAndRazonSocialIgnoreCase(
                        empresaAId, "RAZON REPETIDA");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getRazonSocial()).isEqualToIgnoringCase("razon repetida");
    }

    // ---------------------------------------------------------------------
    // Heredados de BaseTenantRepository: findAllByEmpresaId
    // ---------------------------------------------------------------------

    @Test
    void findAllByEmpresaId_devuelveSoloClientesDeLaEmpresa() {
        persistCliente(empresaRef(empresaAId), "1.111.111-1", "Cliente A1");
        persistCliente(empresaRef(empresaAId), "2.222.222-2", "Cliente A2");
        persistCliente(empresaRef(empresaBId), "3.333.333-3", "Cliente B1");
        entityManager.clear();

        List<ClienteEntity> deA = clienteRepository.findAllByEmpresaId(empresaAId);
        List<ClienteEntity> deB = clienteRepository.findAllByEmpresaId(empresaBId);

        assertThat(deA)
                .hasSize(2)
                .extracting(ClienteEntity::getRazonSocial)
                .containsExactlyInAnyOrder("Cliente A1", "Cliente A2");
        assertThat(deB)
                .hasSize(1)
                .extracting(ClienteEntity::getRazonSocial)
                .containsExactly("Cliente B1");
    }

    @Test
    void findAllByEmpresaId_vacioCuandoEmpresaSinClientes() {
        persistCliente(empresaRef(empresaAId), "1.111.111-1", "Cliente A1");

        List<ClienteEntity> resultado = clienteRepository.findAllByEmpresaId(empresaBId);

        assertThat(resultado).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Heredados de BaseTenantRepository: findByIdAndEmpresaId
    // ---------------------------------------------------------------------

    @Test
    void findByIdAndEmpresaId_devuelveClienteCuandoCoincideIdYEmpresa() {
        ClienteEntity guardado = persistCliente(empresaRef(empresaAId), "1.111.111-1", "Cliente A1");
        entityManager.clear();

        Optional<ClienteEntity> resultado =
                clienteRepository.findByIdAndEmpresaId(guardado.getId(), empresaAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getRut()).isEqualTo("1.111.111-1");
    }

    @Test
    void findByIdAndEmpresaId_vacioCuandoIdPerteneceAOtraEmpresa() {
        ClienteEntity guardado = persistCliente(empresaRef(empresaAId), "1.111.111-1", "Cliente A1");
        entityManager.clear();

        Optional<ClienteEntity> resultado =
                clienteRepository.findByIdAndEmpresaId(guardado.getId(), empresaBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndEmpresaId_vacioCuandoIdNoExiste() {
        Optional<ClienteEntity> resultado =
                clienteRepository.findByIdAndEmpresaId(999_999L, empresaAId);

        assertThat(resultado).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Persistencia / @PrePersist
    // ---------------------------------------------------------------------

    @Test
    void save_persisteCamposYTimestampsDeAuditoria() {
        ClienteEntity cliente = new ClienteEntity();
        cliente.setEmpresa(empresaRef(empresaAId));
        cliente.setRut("7.777.777-7");
        cliente.setRazonSocial("Cliente Auditado");
        cliente.setNombreFantasia("CA");
        cliente.setGiro("Servicios");
        cliente.setEmail("contacto@ca.cl");
        cliente.setActivo(true);

        ClienteEntity guardado = clienteRepository.saveAndFlush(cliente);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
        assertThat(guardado.getCanDelete()).isTrue();
        assertThat(guardado.getEmpresa().getId()).isEqualTo(empresaAId);
    }
}
