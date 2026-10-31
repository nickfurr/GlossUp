package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.infrastructure.in.rest.dto.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Vista de todos los pedidos para el administrador. Solo lectura. */
@RestController
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Usuarios, vendedores y moderación del catálogo")
@SecurityRequirement(name = "bearerAuth")
public class AdminPedidosController {

    private final PedidoRepositoryPort pedidos;

    public AdminPedidosController(PedidoRepositoryPort pedidos) {
        this.pedidos = pedidos;
    }

    /** shortcut: trae todo el historial y el front pagina; paginar en SQL cuando pase de miles de pedidos. */
    @GetMapping("/api/admin/pedidos")
    @Transactional(readOnly = true)
    @Operation(summary = "Todos los pedidos, del más reciente al más antiguo")
    public List<PedidoResponse> listar() {
        return pedidos.listarTodos().stream().map(PedidoResponse::fromDomain).toList();
    }
}
