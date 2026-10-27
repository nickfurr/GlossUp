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

@RestController
@RequestMapping("/api/compatibilidad")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Compatibilidad", description = "Motor de compatibilidad: producto–perfil (HU6, HU7) y producto–producto (HU8)")
@SecurityRequirement(name = "bearerAuth")
public class CompatibilidadController {

    private static final int MAX_LOTE = 200;

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
