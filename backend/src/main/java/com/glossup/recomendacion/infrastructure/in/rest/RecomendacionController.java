package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.port.in.RecomendarProductosUseCase;
import com.glossup.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Recomendaciones personalizadas del cliente (auditoría NC-T10). Los datos del producto se piden al catálogo por id. */
@RestController
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Recomendaciones", description = "Productos sugeridos según el perfil y las compras")
@SecurityRequirement(name = "bearerAuth")
public class RecomendacionController {

    public record RecomendacionResponse(Long productoId, String razon, double afinidad) {}

    public record RecomendacionesResponse(boolean perfilVacio, List<RecomendacionResponse> recomendaciones) {}

    private final RecomendarProductosUseCase recomendador;

    public RecomendacionController(RecomendarProductosUseCase recomendador) {
        this.recomendador = recomendador;
    }

    @GetMapping("/api/recomendaciones")
    @Operation(summary = "Productos recomendados para mí",
               description = "Solo productos sin alertas para mi perfil dermatológico, ordenados por los beneficios para mis "
                       + "condiciones, el parecido con lo que ya compré, lo que otros compraron junto con eso y la popularidad. "
                       + "Cada uno trae la razón principal. afinidad va de 0 a 1.")
    public RecomendacionesResponse recomendar(@RequestParam(defaultValue = "6") int limite, Authentication auth) {
        var resultado = recomendador.recomendar(UsuarioAutenticado.id(auth), limite);
        return new RecomendacionesResponse(resultado.perfilVacio(), resultado.recomendaciones().stream()
                .map(r -> new RecomendacionResponse(r.productoId(), r.razon(), Math.round(r.score() * 100) / 100.0))
                .toList());
    }
}
