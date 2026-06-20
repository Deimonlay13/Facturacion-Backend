package com.gdl.facturacion_backend.dto;

import com.gdl.facturacion_backend.entity.EmpresaEntity;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class EmpresaResponse {
    Long id;
    String rutEmpresa;
    String razonSocial;
    String nombreFantasia;
    String giro;
    String direccion;
    String ciudad;
    String comuna;
    String pais;
    String telefono;
    String sitioWeb;
    String emailPrincipal;
    String emailContabilidad;
    String rutRepresentante;
    String nombreRepresentante;
    String telefonoRepresentante;
    Boolean activo;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static EmpresaResponse from(EmpresaEntity e) {
        return new EmpresaResponse(
                e.getId(),
                e.getRutEmpresa(),
                e.getRazonSocial(),
                e.getNombreFantasia(),
                e.getGiro(),
                e.getDireccion(),
                e.getCiudad(),
                e.getComuna(),
                e.getPais(),
                e.getTelefono(),
                e.getSitioWeb(),
                e.getEmailPrincipal(),
                e.getEmailContabilidad(),
                e.getRutRepresentante(),
                e.getNombreRepresentante(),
                e.getTelefonoRepresentante(),
                e.getActivo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
