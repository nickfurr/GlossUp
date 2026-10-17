package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.port.in.ExtraerIngredientesImagenUseCase;
import com.glossup.catalogo.domain.port.out.GeminiVisionPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExtraerIngredientesImagenService implements ExtraerIngredientesImagenUseCase {

    private final GeminiVisionPort geminiVisionPort;

    public ExtraerIngredientesImagenService(GeminiVisionPort geminiVisionPort) {
        this.geminiVisionPort = geminiVisionPort;
    }

    @Override
    public List<String> extraer(byte[] imagenBytes, String mimeType) {
        if (imagenBytes == null || imagenBytes.length == 0) {
            throw new ReglaNegocioException("La imagen no puede estar vacía");
        }
        List<String> ingredientes = geminiVisionPort.extraerIngredientes(imagenBytes, mimeType);
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new ReglaNegocioException(
                "No se detectaron ingredientes INCI en la imagen. " +
                "Asegúrate de que la etiqueta sea legible.");
        }
        return ingredientes;
    }
}
