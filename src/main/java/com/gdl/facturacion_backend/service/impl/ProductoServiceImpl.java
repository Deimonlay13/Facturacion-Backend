package com.gdl.facturacion_backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.EmpresaEntity;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.service.ProductoService;
import com.gdl.facturacion_backend.service.TenantService;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;
    private final TenantService tenantService;

    @Autowired
    public ProductoServiceImpl(ProductoRepository repository, TenantService tenantService) {
        this.repository = repository;
        this.tenantService = tenantService;
    }

    @Override
    public ProductoEntity create(ProductoRequest request) {

        if (repository.existsByCodigoAndEmpresaId(request.getCodigo(), getEmpresaId())) {
            throw new ReglaNegocioException("El código ya existe");
        }

        if (request.getPrecio() <= 0) {
            throw new ReglaNegocioException("El precio debe ser mayor a 0");
        }

        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo(request.getCodigo());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setUnidadMedida(request.getUnidadMedida());
        producto.setPrecio(request.getPrecio());
        producto.setAfectaIva(request.getAfectaIva());
        producto.setActivo(true);

        producto.setEmpresa(empresaRef());
        return repository.save(producto);
    }

    @Override
    public ProductoEntity update(Long id, ProductoRequest request) {

        ProductoEntity producto = findById(id);

        if (!producto.getCodigo().equals(request.getCodigo()) &&
            repository.existsByCodigoAndEmpresaId(request.getCodigo(), getEmpresaId())) {
            throw new ReglaNegocioException("El código ya existe");
        }

        if (request.getPrecio() <= 0) {
            throw new ReglaNegocioException("El precio debe ser mayor a 0");
        }

        producto.setCodigo(request.getCodigo());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setUnidadMedida(request.getUnidadMedida());
        producto.setPrecio(request.getPrecio());
        producto.setAfectaIva(request.getAfectaIva());
        producto.setActivo(request.getActivo());

        return repository.save(producto);
    }

    @Override
    public ProductoEntity findById(Long id) {
        return repository.findByIdAndEmpresaId(id, getEmpresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    @Override
    public void delete(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(false);
        repository.save(producto);
    }

    private Long getEmpresaId() {
        return tenantService.getEmpresaId();
    }

    private EmpresaEntity empresaRef() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(getEmpresaId());
        return empresa;
    }
}
