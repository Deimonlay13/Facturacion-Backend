package com.gdl.facturacion_backend.config;

import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.entity.TipoDocumentoEntity;
import com.gdl.facturacion_backend.entity.UsuarioEntity;
import com.gdl.facturacion_backend.repository.RolRepository;
import com.gdl.facturacion_backend.repository.TipoDocumentoRepository;
import com.gdl.facturacion_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Carga de datos iniciales. Idempotente: solo crea lo que falte, por lo que es
 * seguro ejecutarlo en cada arranque.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String ROL_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private static final Map<Integer, String> TIPOS_DOCUMENTO = Map.of(
            33, "Factura Electrónica",
            34, "Factura No Afecta o Exenta Electrónica",
            52, "Guía de Despacho Electrónica",
            56, "Nota de Débito Electrónica",
            61, "Nota de Crédito Electrónica"
    );

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearRolSiNoExiste(ROL_SUPER_ADMIN, "Super Administrador",
                "Acceso total al panel de administración");
        crearRolSiNoExiste("ROLE_ADMIN", "Administrador",
                "Administra usuarios, empresa y configuración");
        crearRolSiNoExiste("ROLE_USER", "Usuario",
                "Acceso operativo estándar");
        quitarEmpresaDeSuperAdministradores();
        crearTiposDocumento();
        crearSuperUsuarioRoot();
    }

    private void crearTiposDocumento() {
        TIPOS_DOCUMENTO.forEach((codigo, descripcion) -> {
            if (!tipoDocumentoRepository.existsByCodigoSii(codigo)) {
                TipoDocumentoEntity tipo = new TipoDocumentoEntity();
                tipo.setCodigoSii(codigo);
                tipo.setDescripcion(descripcion);
                tipoDocumentoRepository.save(tipo);
            }
        });
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

    private void crearSuperUsuarioRoot() {
        if (usuarioRepository.existsByUsername("root")) {
            return;
        }
        RolEntity rol = rolRepository.findByNombre(ROL_SUPER_ADMIN).orElseThrow();

        UsuarioEntity root = new UsuarioEntity();
        root.setUsername("root");
        root.setPasswordHash(passwordEncoder.encode("1234"));
        root.setActivo(true);
        root.setRol(rol);
        usuarioRepository.save(root);
    }

    private void quitarEmpresaDeSuperAdministradores() {
        var superAdministradores = usuarioRepository.findAllByRolNombre(ROL_SUPER_ADMIN);
        superAdministradores.forEach(usuario -> usuario.setEmpresa(null));
        usuarioRepository.saveAll(superAdministradores);
    }
}
