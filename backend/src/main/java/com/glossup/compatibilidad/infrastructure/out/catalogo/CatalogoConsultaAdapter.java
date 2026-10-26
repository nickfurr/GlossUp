package com.glossup.compatibilidad.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Único punto de acoplamiento compatibilidad → catálogo. Expone la ficha INCI del producto como
 * lista de nombres normalizados.
 */
@Component
public class CatalogoConsultaAdapter implements CatalogoConsultaPort {

    private final ProductoRepositoryPort productoRepository;

    public CatalogoConsultaAdapter(ProductoRepositoryPort productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public ProductoConsulta obtener(Long productoId) {
        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + productoId));
        List<String> ingredientes = producto.getComposicion().stream()
                .map(c -> c.ingrediente().getInci().nombre())
                .toList();
        return new ProductoConsulta(producto.getNombre(), ingredientes);
    }
}
