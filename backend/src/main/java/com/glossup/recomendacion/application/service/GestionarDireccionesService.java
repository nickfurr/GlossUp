package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.TarifaEnvio;
import com.glossup.recomendacion.domain.port.in.GestionarDireccionesUseCase;
import com.glossup.recomendacion.domain.port.out.DireccionRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Libreta de direcciones (HU14). Si el cliente tiene direcciones, siempre hay exactamente una
 * principal: la primera lo es, marcar otra la reemplaza y borrarla pasa la marca a la siguiente.
 */
@Service
@Transactional
public class GestionarDireccionesService implements GestionarDireccionesUseCase {

    private static final int MAX_DIRECCIONES = 10;

    private final DireccionRepositoryPort direcciones;

    public GestionarDireccionesService(DireccionRepositoryPort direcciones) {
        this.direcciones = direcciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Direccion> listar(Long usuarioId) {
        return direcciones.listar(usuarioId);
    }

    @Override
    public Direccion crear(Direccion datos) {
        validarDepartamento(datos);
        List<Direccion> actuales = direcciones.listar(datos.usuarioId());
        if (actuales.size() >= MAX_DIRECCIONES) {
            throw new ReglaNegocioException("Puedes guardar hasta " + MAX_DIRECCIONES + " direcciones. Borra una para agregar otra.");
        }
        boolean principal = datos.principal() || actuales.isEmpty();
        if (principal) {
            direcciones.quitarPrincipal(datos.usuarioId());
        }
        return direcciones.guardar(datos.conId(null, principal));
    }

    @Override
    public Direccion actualizar(Long id, Direccion datos) {
        validarDepartamento(datos);
        Direccion actual = propia(datos.usuarioId(), id);
        boolean principal = datos.principal() || actual.principal();
        if (principal) {
            direcciones.quitarPrincipal(datos.usuarioId());
        }
        return direcciones.guardar(datos.conId(id, principal));
    }

    @Override
    public void eliminar(Long usuarioId, Long id) {
        Direccion actual = propia(usuarioId, id);
        direcciones.eliminar(id);
        if (actual.principal()) {
            direcciones.listar(usuarioId).stream().findFirst()
                    .ifPresent(siguiente -> direcciones.guardar(siguiente.conId(siguiente.id(), true)));
        }
    }

    private Direccion propia(Long usuarioId, Long id) {
        return direcciones.buscar(id)
                .filter(d -> d.usuarioId().equals(usuarioId))
                .orElseThrow(() -> new NotFoundException("Dirección no encontrada con ID: " + id));
    }

    private static void validarDepartamento(Direccion datos) {
        if (!TarifaEnvio.DEPARTAMENTOS.contains(datos.departamento())) {
            throw new ReglaNegocioException("Departamento no válido: " + datos.departamento());
        }
    }
}
