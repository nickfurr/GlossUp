package com.glossup.compatibilidad.infrastructure.in.rest;

import com.glossup.compatibilidad.domain.model.ResultadoCompatibilidad;
import com.glossup.compatibilidad.domain.port.in.EvaluarCompatibilidadUseCase;
import com.glossup.compatibilidad.infrastructure.in.rest.dto.ResultadoCompatibilidadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compatibilidad")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Compatibilidad", description = "Motor de compatibilidad producto–perfil: alergias e irritantes (HU6)")
@SecurityRequirement(name = "bearerAuth")
public class CompatibilidadController {

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
        return ResponseEntity.ok(ResultadoCompatibilidadResponse.fromDomain(resultado));
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
