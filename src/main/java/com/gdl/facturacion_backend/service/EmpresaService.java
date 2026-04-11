package com.gdl.facturacion_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gdl.facturacion_backend.dto.EmpresaCreateRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.repository.EmpresaRepository;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository repository;

    public EmpresaEntity create(EmpresaCreateRequest request) {
        repository.findByRutEmpresa(request.getRutEmpresa()).ifPresent(e -> {
            throw new RuntimeException("Ya existe una empresa con ese RUT");
        });

        EmpresaEntity e = new EmpresaEntity();
        e.setRutEmpresa(request.getRutEmpresa());
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

        return repository.save(e);
    }

    public EmpresaEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no existe"));
    }
}