package com.gdl.facturacion_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.gdl.facturacion_backend.dto.ProductoRequest;
import com.gdl.facturacion_backend.entity.ProductoEntity;
import com.gdl.facturacion_backend.service.ProductoService;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoEntity create(@RequestBody ProductoRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductoEntity update(@PathVariable Long id,
                                 @RequestBody ProductoRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    public ProductoEntity getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
