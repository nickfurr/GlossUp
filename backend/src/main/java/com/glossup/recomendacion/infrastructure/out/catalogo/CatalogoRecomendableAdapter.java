package com.glossup.recomendacion.infrastructure.out.catalogo;

import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.recomendacion.domain.model.MotorRecomendacion;
import com.glossup.recomendacion.domain.port.out.CatalogoRecomendablePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/** Traduce el catálogo publicado a candidatos del recomendador. */
@Component
public class CatalogoRecomendableAdapter implements CatalogoRecomendablePort {

    private final ProductoRepositoryPort productos;

    public CatalogoRecomendableAdapter(ProductoRepositoryPort productos) {
        this.productos = productos;
    }

    @Override
    public List<MotorRecomendacion.Candidato> disponibles() {
        return productos.listarTodos().stream()
                .filter(Producto::estaEnCatalogo)
                .map(p -> new MotorRecomendacion.Candidato(p.getId(), p.getNombre(), p.getCategoria(),
                        p.getComposicion().stream().map(c -> c.ingrediente().getInci().nombre()).collect(Collectors.toSet()),
                        p.getCreadoEn()))
                .toList();
    }
}
