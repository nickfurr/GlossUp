package com.glossup.catalogo.application.service;

import com.glossup.catalogo.domain.port.out.GeminiVisionPort;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtraerIngredientesImagenServiceTest {

    @Mock
    private GeminiVisionPort geminiVisionPort;

    private ExtraerIngredientesImagenService service;

    @BeforeEach
    void setUp() {
        service = new ExtraerIngredientesImagenService(geminiVisionPort);
    }

    @Test
    void extraer_conImagenNula_lanzaReglaNegocioException() {
        assertThatThrownBy(() -> service.extraer(null, "image/jpeg"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La imagen no puede estar vacía");

        verifyNoInteractions(geminiVisionPort);
    }

    @Test
    void extraer_conImagenVacia_lanzaReglaNegocioException() {
        assertThatThrownBy(() -> service.extraer(new byte[0], "image/jpeg"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La imagen no puede estar vacía");

        verifyNoInteractions(geminiVisionPort);
    }

    @Test
    void extraer_cuandoGeminiNoDetectaIngredientes_lanzaReglaNegocioException() {
        byte[] bytes = new byte[]{1, 2, 3};
        when(geminiVisionPort.extraerIngredientes(bytes, "image/jpeg"))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.extraer(bytes, "image/jpeg"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se detectaron ingredientes INCI en la imagen");
    }

    @Test
    void extraer_cuandoGeminiRetornaNull_lanzaReglaNegocioException() {
        byte[] bytes = new byte[]{1, 2, 3};
        when(geminiVisionPort.extraerIngredientes(bytes, "image/jpeg"))
                .thenReturn(null);

        assertThatThrownBy(() -> service.extraer(bytes, "image/jpeg"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No se detectaron ingredientes INCI en la imagen");
    }

    @Test
    void extraer_conImagenValida_retornaListaIngredientes() {
        byte[] bytes = new byte[]{1, 2, 3};
        List<String> esperados = List.of("aqua", "glycerin", "niacinamide");
        when(geminiVisionPort.extraerIngredientes(bytes, "image/jpeg"))
                .thenReturn(esperados);

        List<String> resultado = service.extraer(bytes, "image/jpeg");

        assertThat(resultado).containsExactly("aqua", "glycerin", "niacinamide");
        verify(geminiVisionPort).extraerIngredientes(bytes, "image/jpeg");
    }
}

