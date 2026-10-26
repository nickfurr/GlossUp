package com.glossup.compatibilidad.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
    public Optional<ProductoConsulta> buscar(Long productoId) {
        return productoRepository.buscarPorId(productoId).map(producto -> new ProductoConsulta(
                producto.getNombre(),
                producto.getComposicion().stream().map(c -> c.ingrediente().getInci().nombre()).toList()));
    }
}
