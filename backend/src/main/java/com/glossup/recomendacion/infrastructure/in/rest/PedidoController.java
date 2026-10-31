package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.MetodoEnvio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoCommand;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoUseCase;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.infrastructure.in.rest.dto.CarritoRequest;
import com.glossup.recomendacion.infrastructure.in.rest.dto.PedidoResponse;
import com.glossup.shared.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de pedidos del cliente (HU9 - SCRUM-36): consolidación del carrito y reserva de
 * inventario. El usuario se toma del token JWT (no del cuerpo) por seguridad.
 */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "Pedidos", description = "Consolidación de carrito, reserva de inventario y consulta de pedidos (HU9)")
@SecurityRequirement(name = "bearerAuth")
public class PedidoController {

    private final ConsolidarCarritoUseCase consolidarCarritoUseCase;
    private final PedidoRepositoryPort pedidoRepository;

    public PedidoController(ConsolidarCarritoUseCase consolidarCarritoUseCase,
                           PedidoRepositoryPort pedidoRepository) {
        this.consolidarCarritoUseCase = consolidarCarritoUseCase;
        this.pedidoRepository = pedidoRepository;
    }

    @PostMapping
    @Operation(summary = "Consolidar el carrito y reservar inventario (HU9)",
               description = "Suma las cantidades por producto, descuenta el stock de forma atómica y crea el pedido "
                       + "en estado CREADO, con el envío elegido (HU14). Falla sin reservar nada si algún producto no tiene stock.")
    public ResponseEntity<PedidoResponse> consolidar(
            @Valid @RequestBody CarritoRequest request,
            Authentication authentication) {

        Long usuarioId = extraerUsuarioId(authentication);
        List<ConsolidarCarritoCommand.Linea> lineas = request.items().stream()
                .map(i -> new ConsolidarCarritoCommand.Linea(i.productoId(), i.cantidad()))
                .toList();

        Pedido pedido = consolidarCarritoUseCase.consolidar(
                new ConsolidarCarritoCommand(usuarioId, lineas, request.direccionId(), MetodoEnvio.valueOf(request.metodoEnvio())));
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.fromDomain(pedido));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un pedido propio por ID")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Long id, Authentication authentication) {
        Long usuarioId = extraerUsuarioId(authentication);
        Pedido pedido = pedidoRepository.buscarPorId(id)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + id));
        return ResponseEntity.ok(PedidoResponse.fromDomain(pedido));
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
