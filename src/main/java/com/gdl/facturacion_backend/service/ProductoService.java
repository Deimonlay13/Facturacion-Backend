package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.ProductoEntity;

import java.util.List;

public interface ProductoService {

    ProductoEntity create(ProductoRequest request);

    ProductoEntity update(Long id, ProductoRequest request);

    ProductoEntity findById(Long id);

    List<ProductoEntity> findAll();

    void delete(Long id);
}
