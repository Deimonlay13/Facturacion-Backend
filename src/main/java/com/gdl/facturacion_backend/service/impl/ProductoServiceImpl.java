package com.gdl.facturacion_backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.repository.ProductoRepository;
import com.gdl.facturacion_backend.service.ProductoService;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepository repository;

    @Override
    public ProductoEntity create(ProductoRequest request) {

        if (repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El código ya existe");
        }

        if (request.getPrecio() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
        }

        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo(request.getCodigo());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setUnidadMedida(request.getUnidadMedida());
        producto.setPrecio(request.getPrecio());
        producto.setAfectaIva(request.getAfectaIva());
        producto.setActivo(true); // siempre activo al crear

        return repository.save(producto);
    }

    @Override
    public ProductoEntity update(Long id, ProductoRequest request) {

        ProductoEntity producto = findById(id);

        if (!producto.getCodigo().equals(request.getCodigo()) &&
            repository.existsByCodigo(request.getCodigo())) {
            throw new RuntimeException("El código ya existe");
        }

        if (request.getPrecio() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
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
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Override
    public void delete(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(false);
        repository.save(producto);
    }
}