package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.ResumenVentas;
import com.glossup.recomendacion.domain.model.VentaVendedor;
import com.glossup.recomendacion.domain.port.in.VentasVendedorUseCase;
import com.glossup.recomendacion.infrastructure.in.rest.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Pedidos y cifras del panel del vendedor. El vendedor siempre sale del token. */
@RestController
@PreAuthorize("hasRole('VENDEDOR')")
@Tag(name = "Ventas del vendedor", description = "Pedidos pagados con productos del vendedor y sus cifras")
@SecurityRequirement(name = "bearerAuth")
public class VentasVendedorController {

    private final VentasVendedorUseCase ventas;

    public VentasVendedorController(VentasVendedorUseCase ventas) {
        this.ventas = ventas;
    }

    public record SemanaResponse(LocalDate inicio, BigDecimal ingresos) {}

    public record MasVendidoResponse(Long productoId, String nombre, int unidades, BigDecimal ingresos) {}

    public record ResumenResponse(BigDecimal ingresosMes, int pedidosMes, int unidadesMes,
                                  List<SemanaResponse> semanas, List<MasVendidoResponse> masVendidos) {}

    public record VentaResponse(Long id, String estado, String cliente, OffsetDateTime pagadoEn,
                                List<PedidoResponse.Item> items, BigDecimal ingresos, PedidoResponse.EnvioResponse envio) {}

    public record VentasResponse(ResumenResponse resumen, List<VentaResponse> ventas) {}

    @GetMapping("/api/vendedor/ventas")
    @Operation(summary = "Mis ventas: pedidos pagados con mis productos y cifras del mes",
               description = "Cada pedido trae solo las líneas de este vendedor; sus ingresos no incluyen el envío.")
    public VentasResponse ventas(Authentication authentication) {
        VentasVendedorUseCase.Ventas v = ventas.ventas(Long.parseLong(authentication.getName()));
        return new VentasResponse(resumen(v.resumen()), v.ventas().stream().map(VentasVendedorController::venta).toList());
    }

    private static ResumenResponse resumen(ResumenVentas r) {
        return new ResumenResponse(r.ingresosMes().monto(), r.pedidosMes(), r.unidadesMes(),
                r.semanas().stream().map(s -> new SemanaResponse(s.inicio(), s.ingresos().monto())).toList(),
                r.masVendidos().stream().map(m -> new MasVendidoResponse(m.productoId(), m.nombre(), m.unidades(), m.ingresos().monto())).toList());
    }

    private static VentaResponse venta(VentaVendedor v) {
        return new VentaResponse(v.pedido().getId(), v.pedido().getEstado().name(), v.cliente(), v.pedido().getPagadoEn(),
                v.lineas().stream().map(PedidoResponse::itemDe).toList(), v.ingresos().monto(), PedidoResponse.envioDe(v.pedido()));
    }
}
