package com.glossup.catalogo.domain.port.out;

import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: consulta del catálogo cerrado de ingredientes (ontología). Los ingredientes
 * se cargan por migración; los productos solo pueden referenciar ingredientes existentes.
 */
public interface IngredienteRepositoryPort {

    Optional<Ingrediente> buscarPorInci(Inci inci);

    /** Hasta 20 ingredientes del catálogo cuyo INCI o descripción (nombre en español) contenga el texto. */
    List<Ingrediente> buscar(String texto);
}
