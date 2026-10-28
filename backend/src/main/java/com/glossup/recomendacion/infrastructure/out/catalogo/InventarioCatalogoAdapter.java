package com.glossup.recomendacion.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Adaptador que implementa {@link InventarioPort} delegando en el agregado {@code Producto} del
 * módulo de catálogo. Es el único punto de acoplamiento recomendación → catálogo, y se mantiene
 * en infraestructura para no contaminar el dominio.
 */
@Component
public class InventarioCatalogoAdapter implements InventarioPort {

    private final ProductoRepositoryPort productoRepository;

    public InventarioCatalogoAdapter(ProductoRepositoryPort productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public Reserva reservar(Long productoId, int cantidad) {
        // shortcut: sin bloqueo pesimista ni columna de versión, dos checkouts concurrentes del
        // mismo producto podrían sobre-vender; subir a @Lock(PESSIMISTIC_WRITE) o versión optimista
        // cuando haya concurrencia real. @Transactional ya garantiza la atomicidad del pedido.
        Producto producto = cargarDisponible(productoId);
        producto.reducirStock(cantidad); // lanza ReglaNegocioException si el stock es insuficiente
        productoRepository.guardar(producto);
        return new Reserva(producto.getNombre(), producto.getPrecio());
    }

    @Override
    public void liberar(Long productoId, int cantidad) {
        // Sin exigir que siga activo: el stock reservado vuelve aunque el vendedor lo haya despublicado.
        productoRepository.buscarPorId(productoId).ifPresent(producto -> {
            producto.reponerStock(cantidad);
            productoRepository.guardar(producto);
        });
    }

    @Override
    public Dinero consultarPrecio(Long productoId) {
        return cargarDisponible(productoId).getPrecio();
    }

    private Producto cargarDisponible(Long productoId) {
        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));
        if (!producto.isActivo()) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' no está disponible");
        }
        return producto;
    }
}
