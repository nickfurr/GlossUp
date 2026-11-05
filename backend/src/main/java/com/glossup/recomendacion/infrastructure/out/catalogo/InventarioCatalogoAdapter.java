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
        Producto producto = cargarDisponible(productoId);
        producto.reducirStock(cantidad); // solo valida (cantidad positiva, stock leído); no se guarda el agregado
        // El descuento real es atómico: si otra compra se llevó las unidades entre la lectura y aquí, no alcanza.
        if (!productoRepository.ajustarStock(productoId, -cantidad)) {
            throw new ReglaNegocioException("Stock insuficiente para '" + producto.getNombre()
                    + "': otro cliente acaba de comprar las últimas unidades");
        }
        return new Reserva(producto.getNombre(), producto.getPrecio());
    }

    @Override
    public void liberar(Long productoId, int cantidad) {
        // Sin exigir que siga activo: el stock reservado vuelve aunque el vendedor lo haya despublicado.
        if (cantidad > 0) {
            productoRepository.ajustarStock(productoId, cantidad);
        }
    }

    @Override
    public Dinero consultarPrecio(Long productoId) {
        return cargarDisponible(productoId).getPrecio();
    }

    private Producto cargarDisponible(Long productoId) {
        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));
        if (!producto.isActivo() || producto.isBaneado()) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' no está disponible");
        }
        return producto;
    }
}
