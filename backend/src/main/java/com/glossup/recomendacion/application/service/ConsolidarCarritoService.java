package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoCommand;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoUseCase;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicación que implementa {@link ConsolidarCarritoUseCase} (HU9 - SCRUM-36).
 *
 * <p>Consolida las líneas del carrito (suma cantidades del mismo producto), reserva el stock de
 * cada producto contra el inventario y crea el pedido. {@code @Transactional} garantiza que la
 * reserva sea todo-o-nada (RNF-25): si un producto no tiene stock, se revierten las reservas
 * previas.</p>
 */
@Service
@Transactional
public class ConsolidarCarritoService implements ConsolidarCarritoUseCase {

    private final InventarioPort inventario;
    private final PedidoRepositoryPort pedidoRepository;

    public ConsolidarCarritoService(InventarioPort inventario, PedidoRepositoryPort pedidoRepository) {
        this.inventario = inventario;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public Pedido consolidar(ConsolidarCarritoCommand command) {
        if (command.lineas() == null || command.lineas().isEmpty()) {
            throw new ReglaNegocioException("El carrito no puede estar vacío");
        }

        // Consolidación: una sola línea por producto, sumando cantidades repetidas (orden estable).
        Map<Long, Integer> consolidado = new LinkedHashMap<>();
        for (ConsolidarCarritoCommand.Linea linea : command.lineas()) {
            consolidado.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }

        List<PedidoItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entrada : consolidado.entrySet()) {
            Dinero precioUnitario = inventario.reservar(entrada.getKey(), entrada.getValue());
            items.add(new PedidoItem(entrada.getKey(), entrada.getValue(), precioUnitario));
        }

        Pedido pedido = Pedido.crear(command.usuarioId(), items);
        return pedidoRepository.guardar(pedido);
    }
}
