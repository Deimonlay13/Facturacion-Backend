package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.RolRequest;
import com.gdl.facturacion_backend.entity.RolEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.RolRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository roleRepository;

    public RolEntity save(RolRequest request) {
        String nombreTecnico = request.getNombre().toUpperCase();
        if (!nombreTecnico.startsWith("ROLE_")) {
            nombreTecnico = "ROLE_" + nombreTecnico;
        }

        if (roleRepository.findByNombre(nombreTecnico).isPresent()) {
            throw new RuntimeException("El rol " + nombreTecnico + " ya existe");
        }

        RolEntity rol = new RolEntity();
        rol.setNombre(nombreTecnico);
        rol.setNombreMostrar(request.getNombreMostrar());
        rol.setDescripcion(request.getDescripcion());
        rol.setActivo(true);

        return roleRepository.save(rol);
    }


    public RolEntity findByNombre(String nombre) {
        return roleRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("El rol " + nombre + " no existe en el sistema"));
    }

    public List<RolEntity> findAll() {
        return roleRepository.findAll();
    }

    @Transactional
    public void eliminar(Long id) {
        RolEntity rol = roleRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado con id: " + id));
        if (Boolean.FALSE.equals(rol.getCanDelete())) {
            throw new ReglaNegocioException("Este rol del sistema no se puede eliminar.");
        }
        try {
            roleRepository.delete(rol);
            roleRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("No se puede eliminar el rol: está asignado a uno o más usuarios.");
        }
    }
}