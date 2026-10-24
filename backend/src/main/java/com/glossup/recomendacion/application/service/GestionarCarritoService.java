package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Carrito;
import com.glossup.recomendacion.domain.port.in.GestionarCarritoUseCase;
import com.glossup.recomendacion.domain.port.out.CarritoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.shared.domain.Dinero;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación que implementa {@link GestionarCarritoUseCase} (HU9 - CRUD de carrito).
 * Mantiene el carrito activo del cliente; el precio de cada ítem se captura al agregarlo
 * (consultado al catálogo, sin reservar stock).
 */
@Service
@Transactional
public class GestionarCarritoService implements GestionarCarritoUseCase {

    private final CarritoRepositoryPort carritoRepository;
    private final InventarioPort inventario;

    public GestionarCarritoService(CarritoRepositoryPort carritoRepository, InventarioPort inventario) {
        this.carritoRepository = carritoRepository;
        this.inventario = inventario;
    }

    @Override
    @Transactional(readOnly = true)
    public Carrito verCarrito(Long usuarioId) {
        return carritoRepository.buscarPorUsuario(usuarioId)
                .orElseGet(() -> Carrito.vacio(usuarioId));
    }

    @Override
    public Carrito agregarItem(Long usuarioId, Long productoId, int cantidad) {
        Carrito carrito = cargarOCrear(usuarioId);
        Dinero precioUnitario = inventario.consultarPrecio(productoId);
        carrito.agregar(productoId, precioUnitario, cantidad);
        return carritoRepository.guardar(carrito);
    }

    @Override
    public Carrito cambiarCantidad(Long usuarioId, Long productoId, int cantidad) {
        Carrito carrito = cargarOCrear(usuarioId);
        carrito.cambiarCantidad(productoId, cantidad); // lanza ReglaNegocioException si no está en el carrito
        return carritoRepository.guardar(carrito);
    }

    @Override
    public Carrito quitarItem(Long usuarioId, Long productoId) {
        Carrito carrito = cargarOCrear(usuarioId);
        carrito.quitar(productoId);
        return carritoRepository.guardar(carrito);
    }

    @Override
    public Carrito vaciar(Long usuarioId) {
        Carrito carrito = cargarOCrear(usuarioId);
        carrito.vaciar();
        return carritoRepository.guardar(carrito);
    }

    private Carrito cargarOCrear(Long usuarioId) {
        return carritoRepository.buscarPorUsuario(usuarioId)
                .orElseGet(() -> Carrito.vacio(usuarioId));
    }
}
