package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import com.glossup.shared.domain.ReglaNegocioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Webhook de la transportadora (HU14). Es público porque lo llama ella, no un usuario: la
 * autenticidad la da el secreto compartido, y sin secreto configurado se rechaza todo.
 */
@RestController
@Tag(name = "Envíos", description = "Cotización y seguimiento de envíos (HU14)")
public class EventosEnvioController {

    public record EventoEnvio(@NotBlank String guia, @NotBlank String estado) {}

    private final EnvioUseCase envios;
    private final byte[] secreto;

    public EventosEnvioController(EnvioUseCase envios, @Value("${glossup.envios.secreto-eventos:}") String secreto) {
        this.envios = envios;
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/envios/eventos")
    @Operation(summary = "Etapas del envío reportadas por la transportadora",
               description = "estado: EN_CAMINO, ENTREGADO o NO_ENTREGADO. Un evento repetido no cambia nada. Secreto inválido → 401.")
    public ResponseEntity<Void> recibir(@RequestHeader(value = "X-Envios-Secreto", required = false) String recibido,
                                        @Valid @RequestBody EventoEnvio evento) {
        if (secreto.length == 0 || recibido == null
                || !MessageDigest.isEqual(secreto, recibido.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }
        envios.actualizar(evento.guia(), etapa(evento.estado()));
        return ResponseEntity.ok().build();
    }

    private static EstadoEnvio etapa(String estado) {
        try {
            EstadoEnvio etapa = EstadoEnvio.valueOf(estado);
            if (etapa.laInformaLaTransportadora()) {
                return etapa;
            }
        } catch (IllegalArgumentException e) {
            // cae al rechazo de abajo
        }
        throw new ReglaNegocioException("Estado de envío no válido: " + estado);
    }
}
