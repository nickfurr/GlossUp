package com.glossup.catalogo.domain.port.in;

import java.util.List;

/**
 * Puerto de entrada (HU5): extrae ingredientes INCI desde una imagen de etiqueta.
 */
public interface ExtraerIngredientesImagenUseCase {
    List<String> extraer(byte[] imagenBytes, String mimeType);
}

