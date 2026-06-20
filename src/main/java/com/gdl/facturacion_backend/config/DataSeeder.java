package com.gdl.facturacion_backend.config;

import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga de datos iniciales. Idempotente: solo crea los roles base del sistema
 * si todavía no existen, por lo que es seguro ejecutarlo en cada arranque.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        crearRolSiNoExiste("ROLE_ADMIN", "Administrador",
                "Administra usuarios, empresa y configuración");
        crearRolSiNoExiste("ROLE_USER", "Usuario",
                "Acceso operativo estándar");
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
}
