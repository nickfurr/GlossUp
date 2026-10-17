package com.glossup.catalogo.domain.port.out;

import java.util.List;

/**
 * Puerto de salida (HU5): contrato para analizar imágenes con Gemini Vision.
 */
public interface GeminiVisionPort {
    List<String> extraerIngredientes(byte[] imagenBytes, String mimeType);
}

