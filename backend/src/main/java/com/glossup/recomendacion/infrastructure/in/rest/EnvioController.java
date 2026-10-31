package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.TarifaEnvio;
import com.glossup.shared.domain.Dinero;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Cotización de envío para el checkout (HU14). Solo informa: el costo real se calcula al crear el pedido. */
@RestController
@RequestMapping("/api/envios")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Envíos", description = "Cotización y seguimiento de envíos (HU14)")
@SecurityRequirement(name = "bearerAuth")
public class EnvioController {

    public record OpcionResponse(String metodo, String etiqueta, BigDecimal costo, LocalDate fechaEstimada) {}

    @GetMapping("/departamentos")
    @Operation(summary = "Departamentos de Colombia a los que se envía")
    public List<String> departamentos() {
        return TarifaEnvio.DEPARTAMENTOS;
    }

    @GetMapping("/opciones")
    @Operation(summary = "Formas de envío disponibles, con costo y fecha estimada")
    public List<OpcionResponse> opciones(@RequestParam String departamento, @RequestParam BigDecimal subtotal) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        return TarifaEnvio.opciones(departamento, new Dinero(subtotal)).stream()
                .map(o -> new OpcionResponse(o.metodo().name(), o.metodo().etiqueta(), o.costo().monto(),
                        TarifaEnvio.fechaEstimada(hoy, o.diasHabiles())))
                .toList();
    }
}
