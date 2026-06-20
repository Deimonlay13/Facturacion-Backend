package com.gdl.facturacion_backend.config;

import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.repository.RolRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga de datos iniciales. Idempotente: solo crea lo que falte, por lo que es
 * seguro ejecutarlo en cada arranque.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String ROL_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearRolSiNoExiste(ROL_SUPER_ADMIN, "Super Administrador",
                "Acceso total al panel de administración");
        crearRolSiNoExiste("ROLE_ADMIN", "Administrador",
                "Administra usuarios, empresa y configuración");
        crearRolSiNoExiste("ROLE_USER", "Usuario",
                "Acceso operativo estándar");
        crearSuperUsuarioRoot();
    }

    private void crearRolSiNoExiste(String nombre, String nombreMostrar, String descripcion) {
        if (rolRepository.findByNombre(nombre).isPresent()) {
            return;
        }
        RolEntity rol = new RolEntity();
        rol.setNombre(nombre);
        rol.setNombreMostrar(nombreMostrar);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        rol.setCanDelete(false);
        rolRepository.save(rol);
    }

    /** Super usuario root / 1234 — único que puede entrar al panel de administración. */
    private void crearSuperUsuarioRoot() {
        if (usuarioRepository.existsByUsername("root")) {
            return;
        }
        // La entidad Usuario requiere empresa (id_empresa). Se asocia a la primera empresa.
        EmpresaEntity empresa = empresaRepository.findAll().stream().findFirst().orElse(null);
        if (empresa == null) {
            return; // sin empresas todavía; se creará en el próximo arranque
        }
        RolEntity rol = rolRepository.findByNombre(ROL_SUPER_ADMIN).orElseThrow();

        UsuarioEntity root = new UsuarioEntity();
        root.setUsername("root");
        root.setPasswordHash(passwordEncoder.encode("1234"));
        root.setActivo(true);
        root.setRol(rol);
        root.setEmpresa(empresa);
        usuarioRepository.save(root);
    }
}
