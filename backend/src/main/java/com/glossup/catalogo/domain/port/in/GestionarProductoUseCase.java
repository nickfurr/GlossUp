package com.glossup.catalogo.domain.port.in;

import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;

import java.util.List;

/**
 * Puerto de entrada para la modificación y eliminación lógica de productos (HU4 - SCRUM-31).
 */
public interface GestionarProductoUseCase {

    /**
     * Actualiza un producto existente verificando que pertenezca al vendedor autenticado
     * y que conserve una ficha INCI válida (RNF-04).
     */
    Producto actualizar(ActualizarProductoCommand command);

    /**
     * Elimina lógicamente (despublica: activo = false) un producto existente
     * verificando que pertenezca al vendedor autenticado.
     */
    void eliminar(Long productoId, Long vendedorId);

    /** Busca en el catálogo cerrado de ingredientes (para armar la ficha INCI desde una lista). */
    List<Ingrediente> buscarIngredientes(String texto);
}
