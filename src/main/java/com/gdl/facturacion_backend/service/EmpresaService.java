package com.gdl.facturacion_backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.repository.EmpresaRepository;
import com.gdl.facturacion_backend.util.RutUtils;

@Service
public class EmpresaService {

    private static final long MAX_LOGO_BYTES = 1_000_000;
    private static final Set<String> LOGO_CONTENT_TYPES = Set.of("image/png", "image/jpeg");

    @Autowired
    private EmpresaRepository repository;

    public EmpresaEntity create(EmpresaCreateRequest request) {
        String rut = validarRut(request.getRutEmpresa());

        repository.findByRutEmpresa(rut).ifPresent(e -> {
            throw new ReglaNegocioException("Ya existe una empresa con el RUT: " + rut);
        });

        EmpresaEntity e = new EmpresaEntity();
        e.setActivo(true);
        cargarDatosEmpresa(e, request, rut);

        return repository.save(e);
    }

    public List<EmpresaEntity> findAll() {
        return repository.findAll();
    }

    public EmpresaEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada con id: " + id));
    }

    public EmpresaEntity update(Long id, EmpresaCreateRequest request) {
        EmpresaEntity e = findById(id);
        String rut = validarRut(request.getRutEmpresa());

        // Permitir el mismo RUT solo si pertenece a esta misma empresa
        repository.findByRutEmpresa(rut)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException("Ya existe otra empresa con el RUT: " + rut);
                });

        cargarDatosEmpresa(e, request, rut);
        return repository.save(e);
    }

    public EmpresaEntity cambiarEstado(Long id, Boolean activo) {
        EmpresaEntity e = findById(id);
        e.setActivo(activo);
        return repository.save(e);
    }

    public EmpresaEntity guardarLogo(Long id, MultipartFile file) {
        EmpresaEntity e = findById(id);
        validarLogo(file);
        try {
            e.setLogo(file.getBytes());
            e.setLogoContentType(file.getContentType());
            e.setLogoFilename(file.getOriginalFilename());
            return repository.save(e);
        } catch (java.io.IOException ex) {
            throw new ReglaNegocioException("No se pudo leer el archivo del logo");
        }
    }

    public EmpresaEntity eliminarLogo(Long id) {
        EmpresaEntity e = findById(id);
        e.setLogo(null);
        e.setLogoContentType(null);
        e.setLogoFilename(null);
        return repository.save(e);
    }

    private String validarRut(String rut) {
        String limpio = rut != null ? RutUtils.clean(rut) : null;
        if (!RutUtils.isValid(limpio)) {
            throw new RutInvalidoException(rut);
        }
        return limpio;
    }

    private void cargarDatosEmpresa(EmpresaEntity e, EmpresaCreateRequest request, String rutLimpio) {
        e.setRutEmpresa(rutLimpio);
        e.setRazonSocial(request.getRazonSocial());
        e.setNombreFantasia(request.getNombreFantasia());
        e.setGiro(request.getGiro());
        e.setDireccion(request.getDireccion());
        e.setCiudad(request.getCiudad());
        e.setComuna(request.getComuna());
        e.setPais(request.getPais());
        e.setTelefono(request.getTelefono());
        e.setSitioWeb(request.getSitioWeb());
        e.setEmailPrincipal(request.getEmailPrincipal());
        e.setEmailContabilidad(request.getEmailContabilidad());
        e.setRutRepresentante(request.getRutRepresentante());
        e.setNombreRepresentante(request.getNombreRepresentante());
        e.setTelefonoRepresentante(request.getTelefonoRepresentante());
    }

    private void validarLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ReglaNegocioException("Debe adjuntar un archivo de logo");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new ReglaNegocioException("El logo no puede superar 1 MB");
        }
        if (!LOGO_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ReglaNegocioException("El logo debe ser PNG o JPG/JPEG");
        }
    }
}
