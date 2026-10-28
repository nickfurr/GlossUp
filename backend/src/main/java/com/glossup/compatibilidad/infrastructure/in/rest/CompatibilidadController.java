package com.glossup.compatibilidad.infrastructure.in.rest;

import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.compatibilidad.infrastructure.in.rest.dto.CruceResponse;
import com.glossup.compatibilidad.infrastructure.in.rest.dto.ResultadoCompatibilidadResponse;
import com.glossup.shared.domain.ReglaNegocioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/compatibilidad")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Compatibilidad", description = "Motor de compatibilidad: producto–perfil (HU6, HU7) y producto–producto (HU8)")
@SecurityRequirement(name = "bearerAuth")
public class CompatibilidadController {

    private static final int MAX_LOTE = 200;
    /** Tipos de piel del perfil (glosario); el filtro HU13 no acepta otros valores. */
    private static final Set<String> TIPOS_PIEL = Set.of("NORMAL", "SECA", "GRASA", "MIXTA", "SENSIBLE");

    private final EvaluarCompatibilidadUseCase evaluarCompatibilidad;

    public CompatibilidadController(EvaluarCompatibilidadUseCase evaluarCompatibilidad) {
        this.evaluarCompatibilidad = evaluarCompatibilidad;
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Evaluar un producto contra mi perfil (HU6)",
               description = "Cruza la ficha INCI del producto con las alergias e irritantes de mi perfil. "
                       + "Devuelve si es compatible y las alertas con su causa (RNF-05).")
    public ResponseEntity<ResultadoCompatibilidadResponse> evaluar(
            @PathVariable Long productoId, Authentication authentication) {
        Long usuarioId = extraerUsuarioId(authentication);
        ResultadoCompatibilidad resultado = evaluarCompatibilidad.evaluarProductoPerfil(usuarioId, productoId);
        return ResponseEntity.ok(ResultadoCompatibilidadResponse.fromDomain(productoId, resultado));
    }

    @GetMapping("/productos")
    @Operation(summary = "Evaluar varios productos contra mi perfil (HU6)",
               description = "Evaluación por lote para el catálogo y el carrito: carga el perfil y las reglas una sola vez. "
                       + "Los ids inexistentes se omiten. Máximo " + MAX_LOTE + " productos por petición.")
    public ResponseEntity<List<ResultadoCompatibilidadResponse>> evaluarLote(
            @RequestParam("ids") List<Long> ids, Authentication authentication) {
        if (ids.size() > MAX_LOTE) {
            throw new ReglaNegocioException("Máximo " + MAX_LOTE + " productos por evaluación");
        }
        return ResponseEntity.ok(evaluarCompatibilidad.evaluarProductosPerfil(extraerUsuarioId(authentication), ids)
                .entrySet().stream()
                .map(e -> ResultadoCompatibilidadResponse.fromDomain(e.getKey(), e.getValue()))
                .toList());
    }

    @GetMapping("/cruces")
    @Operation(summary = "Cruces entre productos del carrito (HU8)",
               description = "Devuelve cada par de productos con ingredientes que no conviene combinar, con la causa. "
                       + "No depende del perfil. Máximo " + MAX_LOTE + " productos por petición.")
    public ResponseEntity<List<CruceResponse>> cruces(@RequestParam("ids") List<Long> ids) {
        if (ids.size() > MAX_LOTE) {
            throw new ReglaNegocioException("Máximo " + MAX_LOTE + " productos por evaluación");
        }
        return ResponseEntity.ok(evaluarCompatibilidad.evaluarCruces(ids).stream().map(CruceResponse::fromDomain).toList());
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Filtros dermatológicos del catálogo (HU13)",
               description = "Ids de los productos del catálogo aptos para el tipo de piel elegido (oculta los comedogénicos, "
                       + "irritantes o con pH inadecuado para esa piel) y, con excluirAlergenos=true, sin ingredientes de "
                       + "los alérgenos guardados en mi perfil. Sin filtros devuelve todo el catálogo.")
    public ResponseEntity<List<Long>> filtrarCatalogo(
            @RequestParam(required = false) String tipoPiel,
            @RequestParam(defaultValue = "false") boolean excluirAlergenos,
            Authentication authentication) {
        String piel = tipoPiel == null || tipoPiel.isBlank() ? null : tipoPiel.trim().toUpperCase();
        if (piel != null && !TIPOS_PIEL.contains(piel)) {
            throw new ReglaNegocioException("Tipo de piel no válido: " + tipoPiel + ". Usa uno de " + TIPOS_PIEL);
        }
        return ResponseEntity.ok(evaluarCompatibilidad.filtrarCatalogo(extraerUsuarioId(authentication), piel, excluirAlergenos));
    }

    private Long extraerUsuarioId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return 1L;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return (long) Math.abs(authentication.getName().hashCode());
        }
    }
}
