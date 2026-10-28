package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.port.in.ComprobanteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Descarga del comprobante de compra de un pedido pagado (HU11). */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Comprobantes", description = "Comprobante de compra en PDF (HU11)")
@SecurityRequirement(name = "bearerAuth")
public class ComprobanteController {

    private final ComprobanteUseCase comprobantes;

    public ComprobanteController(ComprobanteUseCase comprobantes) {
        this.comprobantes = comprobantes;
    }

    @GetMapping(value = "/{id}/comprobante", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descargar el comprobante en PDF de un pedido propio pagado (HU11)")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, Authentication authentication) {
        Comprobante c = comprobantes.descargar(Long.parseLong(authentication.getName()), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("comprobante-" + c.numero() + ".pdf").build().toString())
                .body(c.pdf());
    }
}
