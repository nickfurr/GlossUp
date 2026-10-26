package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.out.IngredienteRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve la ficha INCI de un producto contra el catálogo cerrado de ingredientes. Un INCI que no
 * esté en el catálogo se rechaza: el motor de compatibilidad solo puede cruzar lo que conoce.
 */
final class FichaInci {

    private FichaInci() {}

    static List<ComposicionProducto> resolver(List<RegistrarProductoCommand.IngredienteComando> ingredientes,
                                              IngredienteRepositoryPort catalogo) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new ReglaNegocioException("La ficha técnica debe contener al menos un ingrediente (INCI)");
        }
        List<ComposicionProducto> composicion = new ArrayList<>();
        List<String> desconocidos = new ArrayList<>();
        for (RegistrarProductoCommand.IngredienteComando ic : ingredientes) {
            Optional<Ingrediente> ingrediente = catalogo.buscarPorInci(new Inci(ic.inci()));
            if (ingrediente.isPresent()) {
                composicion.add(new ComposicionProducto(ingrediente.get(), ic.concentracion()));
            } else {
                desconocidos.add(ic.inci().trim());
            }
        }
        if (!desconocidos.isEmpty()) {
            throw new ReglaNegocioException("Ingredientes que no están en el catálogo INCI: "
                    + String.join(", ", desconocidos) + ". Elígelos de la lista del catálogo.");
        }
        return composicion;
    }
}
