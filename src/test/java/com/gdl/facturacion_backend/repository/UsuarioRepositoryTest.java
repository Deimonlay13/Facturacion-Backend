package com.gdl.facturacion_backend.repository;

import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UsuarioRepositoryTest {

    private static final String ROL_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EmpresaEntity empresa;
    private EmpresaEntity otraEmpresa;
    private RolEntity rolAdmin;
    private RolEntity rolUsuario;
    private RolEntity rolSuperAdmin;

    @BeforeEach
    void setUp() {
        empresa = persistEmpresa("76111111-1", "Empresa Uno SpA");
        otraEmpresa = persistEmpresa("76222222-2", "Empresa Dos SpA");

        rolAdmin = persistRol("ROLE_ADMIN", "Administrador");
        rolUsuario = persistRol("ROLE_USER", "Usuario");
        rolSuperAdmin = persistRol(ROL_SUPER_ADMIN, "Super Admin");
    }

    // ---------- Helpers ----------

    private EmpresaEntity persistEmpresa(String rut, String razonSocial) {
        EmpresaEntity e = new EmpresaEntity();
        e.setRutEmpresa(rut);
        e.setRazonSocial(razonSocial);
        e.setActivo(true);
        return entityManager.persistAndFlush(e);
    }

    private RolEntity persistRol(String nombre, String nombreMostrar) {
        RolEntity r = new RolEntity();
        r.setNombre(nombre);
        r.setNombreMostrar(nombreMostrar);
        r.setActivo(true);
        return entityManager.persistAndFlush(r);
    }

    private UsuarioEntity persistUsuario(EmpresaEntity emp, RolEntity rol, String username, Boolean activo) {
        UsuarioEntity u = new UsuarioEntity();
        u.setEmpresa(emp);
        u.setRol(rol);
        u.setUsername(username);
        u.setPasswordHash("$2a$10$hashFicticioParaTest");
        u.setActivo(activo);
        return entityManager.persistAndFlush(u);
    }

    // ---------- findByUsername ----------

    @Test
    void findByUsername_existente_devuelveUsuario() {
        UsuarioEntity guardado = persistUsuario(empresa, rolAdmin, "jperez", true);
        entityManager.clear();

        Optional<UsuarioEntity> resultado = usuarioRepository.findByUsername("jperez");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(guardado.getId());
        assertThat(resultado.get().getUsername()).isEqualTo("jperez");
        assertThat(resultado.get().getRol().getNombre()).isEqualTo("ROLE_ADMIN");
        assertThat(resultado.get().getEmpresa().getId()).isEqualTo(empresa.getId());
    }

    @Test
    void findByUsername_inexistente_devuelveVacio() {
        persistUsuario(empresa, rolAdmin, "jperez", true);
        entityManager.clear();

        Optional<UsuarioEntity> resultado = usuarioRepository.findByUsername("noexiste");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByUsername_esSensibleAMayusculas() {
        persistUsuario(empresa, rolAdmin, "jperez", true);
        entityManager.clear();

        assertThat(usuarioRepository.findByUsername("JPEREZ")).isEmpty();
        assertThat(usuarioRepository.findByUsername("jperez")).isPresent();
    }

    // ---------- existsByUsername ----------

    @Test
    void existsByUsername_existente_true() {
        persistUsuario(empresa, rolAdmin, "mlopez", true);
        entityManager.clear();

        assertThat(usuarioRepository.existsByUsername("mlopez")).isTrue();
    }

    @Test
    void existsByUsername_inexistente_false() {
        persistUsuario(empresa, rolAdmin, "mlopez", true);
        entityManager.clear();

        assertThat(usuarioRepository.existsByUsername("otro")).isFalse();
    }

    // ---------- findAllByRolNombre ----------

    @Test
    void findAllByRolNombre_devuelveSoloUsuariosDelRol() {
        persistUsuario(empresa, rolAdmin, "admin1", true);
        persistUsuario(empresa, rolAdmin, "admin2", true);
        persistUsuario(empresa, rolUsuario, "user1", true);
        // usuario de otra empresa pero mismo rol: igualmente debe contarse (no filtra por empresa)
        persistUsuario(otraEmpresa, rolAdmin, "admin3", true);
        entityManager.clear();

        List<UsuarioEntity> admins = usuarioRepository.findAllByRolNombre("ROLE_ADMIN");

        assertThat(admins).hasSize(3);
        assertThat(admins)
                .extracting(u -> u.getRol().getNombre())
                .containsOnly("ROLE_ADMIN");
        assertThat(admins)
                .extracting(UsuarioEntity::getUsername)
                .containsExactlyInAnyOrder("admin1", "admin2", "admin3");
    }

    @Test
    void findAllByRolNombre_rolSinUsuarios_devuelveListaVacia() {
        persistUsuario(empresa, rolAdmin, "admin1", true);
        entityManager.clear();

        List<UsuarioEntity> resultado = usuarioRepository.findAllByRolNombre("ROLE_USER");

        assertThat(resultado).isEmpty();
    }

    @Test
    void findAllByRolNombre_rolInexistente_devuelveListaVacia() {
        persistUsuario(empresa, rolAdmin, "admin1", true);
        entityManager.clear();

        List<UsuarioEntity> resultado = usuarioRepository.findAllByRolNombre("ROLE_QUE_NO_EXISTE");

        assertThat(resultado).isEmpty();
    }

    // ---------- findAllByEmpresaId (heredado de BaseTenantRepository) ----------

    @Test
    void findAllByEmpresaId_devuelveSoloUsuariosDeLaEmpresa() {
        persistUsuario(empresa, rolAdmin, "u1", true);
        persistUsuario(empresa, rolUsuario, "u2", true);
        persistUsuario(otraEmpresa, rolAdmin, "u3", true);
        entityManager.clear();

        assertThat(usuarioRepository.findAllByEmpresaId(empresa.getId())).hasSize(2);
        assertThat(usuarioRepository.findAllByEmpresaId(otraEmpresa.getId())).hasSize(1);
    }

    @Test
    void findAllByEmpresaId_empresaSinUsuarios_devuelveListaVacia() {
        persistUsuario(empresa, rolAdmin, "u1", true);
        entityManager.clear();

        assertThat(usuarioRepository.findAllByEmpresaId(otraEmpresa.getId())).isEmpty();
    }

    // ---------- findByIdAndEmpresaId (heredado de BaseTenantRepository) ----------

    @Test
    void findByIdAndEmpresaId_coincide_yNoCoincide() {
        UsuarioEntity usuario = persistUsuario(empresa, rolAdmin, "u1", true);
        entityManager.clear();

        assertThat(usuarioRepository.findByIdAndEmpresaId(usuario.getId(), empresa.getId())).isPresent();
        assertThat(usuarioRepository.findByIdAndEmpresaId(usuario.getId(), otraEmpresa.getId())).isEmpty();
    }

    // ---------- persistencia / PrePersist ----------

    @Test
    void guardarUsuario_persisteCamposYTimestamps() {
        UsuarioEntity usuario = persistUsuario(empresa, rolUsuario, "completo", true);
        entityManager.clear();

        UsuarioEntity recuperado = usuarioRepository.findById(usuario.getId()).orElseThrow();

        assertThat(recuperado.getUsername()).isEqualTo("completo");
        assertThat(recuperado.getPasswordHash()).isEqualTo("$2a$10$hashFicticioParaTest");
        assertThat(recuperado.getActivo()).isTrue();
        assertThat(recuperado.getRol().getNombre()).isEqualTo("ROLE_USER");
        assertThat(recuperado.getEmpresa().getId()).isEqualTo(empresa.getId());
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getUpdatedAt()).isNotNull();
        assertThat(recuperado.getCanDelete()).isTrue();
    }

    @Test
    void guardarUsuarioSuperAdmin_sinEmpresa_seGuardaPorqueNoRequiereEmpresa() {
        // requiereEmpresa() es false para ROLE_SUPER_ADMIN, por lo que el @PrePersist
        // no exige empresa ni consulta el TenantContext.
        UsuarioEntity superAdmin = new UsuarioEntity();
        superAdmin.setRol(rolSuperAdmin);
        superAdmin.setUsername("root");
        superAdmin.setPasswordHash("$2a$10$super");
        superAdmin.setActivo(true);

        UsuarioEntity guardado = entityManager.persistAndFlush(superAdmin);
        entityManager.clear();

        UsuarioEntity recuperado = usuarioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getEmpresa()).isNull();
        assertThat(recuperado.getRol().getNombre()).isEqualTo(ROL_SUPER_ADMIN);
        assertThat(recuperado.getCreatedAt()).isNotNull();
    }

    @Test
    void username_debeSerUnico() {
        persistUsuario(empresa, rolAdmin, "duplicado", true);
        entityManager.clear();

        UsuarioEntity otro = new UsuarioEntity();
        otro.setEmpresa(otraEmpresa);
        otro.setRol(rolAdmin);
        otro.setUsername("duplicado");
        otro.setPasswordHash("$2a$10$otro");
        otro.setActivo(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> entityManager.persistAndFlush(otro))
                .isInstanceOf(Exception.class);
    }
}
