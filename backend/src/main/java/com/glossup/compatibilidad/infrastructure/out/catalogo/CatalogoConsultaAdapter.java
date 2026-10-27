package com.glossup.compatibilidad.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.compatibilidad.domain.model.Zona;
import com.glossup.compatibilidad.domain.port.out.CatalogoConsultaPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return productoRepository.buscarPorId(productoId).map(CatalogoConsultaAdapter::aConsulta);
    }

    @Override
    public Map<Long, ProductoConsulta> buscarVarios(Collection<Long> productoIds) {
        return productoRepository.buscarPorIds(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, CatalogoConsultaAdapter::aConsulta));
    }

    private static ProductoConsulta aConsulta(Producto producto) {
        return new ProductoConsulta(
                producto.getNombre(),
                producto.getComposicion().stream().map(c -> c.ingrediente().getInci().nombre()).toList(),
                producto.getPh(), Zona.deCategoria(producto.getCategoria()));
    }
}
