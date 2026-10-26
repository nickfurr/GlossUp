package com.glossup.catalogo.infrastructure.in.rest;

import com.glossup.catalogo.domain.port.in.GestionarProductoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/ingredientes")
@Tag(name = "Catálogo", description = "Catálogo cerrado de ingredientes INCI")
@SecurityRequirement(name = "bearerAuth")
public class IngredienteController {

    private final GestionarProductoUseCase gestionarProductoUseCase;

    public IngredienteController(GestionarProductoUseCase gestionarProductoUseCase) {
        this.gestionarProductoUseCase = gestionarProductoUseCase;
    }

    /** @param descripcion nombre en español y función del ingrediente. */
    public record IngredienteCatalogoResponse(Long id, String inci, String descripcion) {}

    @GetMapping
    @Operation(summary = "Buscar en el catálogo de ingredientes",
               description = "Busca por nombre INCI o por nombre en español (ej. 'glicerina'). Máximo 20 resultados.")
    public ResponseEntity<List<IngredienteCatalogoResponse>> buscar(@RequestParam("q") String q) {
        return ResponseEntity.ok(gestionarProductoUseCase.buscarIngredientes(q).stream()
                .map(i -> new IngredienteCatalogoResponse(i.getId(), i.getInci().nombre(), i.getDescripcion()))
                .toList());
    }
}
