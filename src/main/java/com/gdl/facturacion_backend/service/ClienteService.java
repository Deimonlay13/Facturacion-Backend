package com.gdl.facturacion_backend.service;

import com.gdl.facturacion_backend.client.SreClient;
import com.gdl.facturacion_backend.dto.ClienteCreateRequest;
import com.gdl.facturacion_backend.dto.SreCompanyResponse;
import com.gdl.facturacion_backend.entity.ClienteEntity;
import com.gdl.facturacion_backend.exception.ClienteDuplicadoException;
import com.gdl.facturacion_backend.exception.RecursoNoEncontradoException;
import com.gdl.facturacion_backend.exception.ReglaNegocioException;
import com.gdl.facturacion_backend.exception.RutInvalidoException;
import com.gdl.facturacion_backend.repository.ClienteRepository;
import com.gdl.facturacion_backend.util.RutUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService extends BaseTenantService<ClienteEntity> {

    private final ClienteRepository clienteRepository;
    private final SreClient sreClient;

    public ClienteService(ClienteRepository clienteRepository,
                          TenantService tenantService,
                          SreClient sreClient) {
        super(clienteRepository, tenantService);
        this.clienteRepository = clienteRepository;
        this.sreClient = sreClient;
    }

    public ClienteEntity crear(ClienteCreateRequest request) {
        String rutClean = RutUtils.clean(request.getRut());

        if (!RutUtils.isValid(rutClean)) {
            throw new RutInvalidoException(request.getRut());
        }

        if (clienteRepository.existsByRutAndEmpresaId(rutClean, getEmpresaId())) {
            throw new ClienteDuplicadoException(rutClean);
        }

        return save(mapToEntity(new ClienteEntity(), request, rutClean));
    }

    public ClienteEntity actualizar(Long id, ClienteCreateRequest request) {
        ClienteEntity cliente = findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));

        String rutClean = RutUtils.clean(request.getRut());

        if (!RutUtils.isValid(rutClean)) {
            throw new RutInvalidoException(request.getRut());
        }

        // Allow same RUT only if it belongs to this same client record
        if (!cliente.getRut().equals(rutClean) &&
                clienteRepository.existsByRutAndEmpresaId(rutClean, getEmpresaId())) {
            throw new ClienteDuplicadoException(rutClean);
        }

        return save(mapToEntity(cliente, request, rutClean));
    }

    public List<ClienteEntity> listarActivos() {
        return findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .toList();
    }

    public ClienteEntity obtenerPorId(Long id) {
        return findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id: " + id));
    }

    public void desactivar(Long id) {
        ClienteEntity cliente = obtenerPorId(id);
        cliente.setActivo(false);
        save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        ClienteEntity cliente = obtenerPorId(id);
        try {
            clienteRepository.delete(cliente);
            clienteRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("No se puede eliminar el cliente: tiene documentos asociados.");
        }
    }

    /**
     * Validates the RUT format/DV then queries SRE.
     * Returns empty if RUT is not found in SRE's registry (non-blocking for the form).
     */
    public Optional<SreCompanyResponse> consultarSre(String rut) {
        String rutClean = RutUtils.clean(rut);
        if (!RutUtils.isValid(rutClean)) {
            throw new RutInvalidoException(rut);
        }
        return sreClient.buscarPorRut(rutClean);
    }

    private ClienteEntity mapToEntity(ClienteEntity cliente, ClienteCreateRequest request, String rutClean) {
        cliente.setRut(rutClean);
        cliente.setRazonSocial(request.getRazonSocial());
        cliente.setNombreFantasia(request.getNombreFantasia());
        cliente.setGiro(request.getGiro());
        cliente.setDireccion(request.getDireccion());
        cliente.setCiudad(request.getCiudad());
        cliente.setComuna(request.getComuna());
        cliente.setRegion(request.getRegion());
        cliente.setPais(request.getPais());
        cliente.setTelefono(request.getTelefono());
        cliente.setEmail(request.getEmail());
        return cliente;
    }
}
