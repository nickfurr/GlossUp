package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.port.in.PagarPedidoUseCase;
import com.glossup.recomendacion.infrastructure.in.rest.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Pago de pedidos con la pasarela Wompi (HU10). */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Pagos", description = "Pago de pedidos con pasarela externa (HU10)")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagarPedidoUseCase pagos;

    public PagoController(PagarPedidoUseCase pagos) {
        this.pagos = pagos;
    }

    public record UrlPagoResponse(String url) {}

    public record ResultadoPagoResponse(PedidoResponse pedido, String estadoTransaccion) {}

    @GetMapping
    @Operation(summary = "Mis pedidos, del más reciente al más antiguo")
    public List<PedidoResponse> misPedidos(Authentication authentication) {
        return pagos.misPedidos(usuarioId(authentication)).stream().map(PedidoResponse::fromDomain).toList();
    }

    @PostMapping("/{id}/pago")
    @Operation(summary = "Iniciar el pago de un pedido (HU10)",
               description = "Devuelve la URL del checkout de Wompi. El pedido debe estar CREADO y dentro de su plazo de pago.")
    public UrlPagoResponse iniciarPago(@PathVariable Long id, Authentication authentication) {
        return new UrlPagoResponse(pagos.iniciarPago(usuarioId(authentication), id));
    }

    @GetMapping("/{id}/pago/{transaccionId}")
    @Operation(summary = "Confirmar el pago al volver de Wompi (HU10)",
               description = "Consulta la transacción directamente a Wompi (no se confía en la redirección) y la aplica al pedido.")
    public ResultadoPagoResponse confirmarPago(@PathVariable Long id, @PathVariable String transaccionId,
                                               Authentication authentication) {
        var r = pagos.confirmarPago(usuarioId(authentication), id, transaccionId);
        return new ResultadoPagoResponse(PedidoResponse.fromDomain(r.pedido()), r.estadoTransaccion().name());
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un pedido sin pagar y liberar su inventario")
    public PedidoResponse cancelar(@PathVariable Long id, Authentication authentication) {
        return PedidoResponse.fromDomain(pagos.cancelar(usuarioId(authentication), id));
    }

    private static Long usuarioId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
