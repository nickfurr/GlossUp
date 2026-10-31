package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Direccion;
import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.model.TarifaEnvio;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoCommand;
import com.glossup.recomendacion.domain.port.in.ConsolidarCarritoUseCase;
import com.glossup.recomendacion.domain.port.out.DireccionRepositoryPort;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private final DireccionRepositoryPort direcciones;

    public ConsolidarCarritoService(InventarioPort inventario, PedidoRepositoryPort pedidoRepository,
                                    DireccionRepositoryPort direcciones) {
        this.inventario = inventario;
        this.pedidoRepository = pedidoRepository;
        this.direcciones = direcciones;
    }

    @Override
    public Pedido consolidar(ConsolidarCarritoCommand command) {
        if (command.lineas() == null || command.lineas().isEmpty()) {
            throw new ReglaNegocioException("El carrito no puede estar vacío");
        }

        Direccion destino = direcciones.buscar(command.direccionId())
                .filter(d -> d.usuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new NotFoundException("Dirección no encontrada con ID: " + command.direccionId()));

        // Consolidación: una sola línea por producto, sumando cantidades repetidas (orden estable).
        Map<Long, Integer> consolidado = new LinkedHashMap<>();
        for (ConsolidarCarritoCommand.Linea linea : command.lineas()) {
            consolidado.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }

        List<PedidoItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entrada : consolidado.entrySet()) {
            InventarioPort.Reserva reserva = inventario.reservar(entrada.getKey(), entrada.getValue());
            items.add(new PedidoItem(entrada.getKey(), reserva.nombre(), entrada.getValue(), reserva.precioUnitario()));
        }

        Pedido pedido = Pedido.crear(command.usuarioId(), items, envio(command, destino, items));
        return pedidoRepository.guardar(pedido);
    }

    /** El costo sale de los precios ya reservados; lanzar aquí revierte también la reserva. */
    private Envio envio(ConsolidarCarritoCommand command, Direccion destino, List<PedidoItem> items) {
        TarifaEnvio.Opcion opcion = TarifaEnvio.opciones(destino.departamento(), Pedido.subtotalDe(items)).stream()
                .filter(o -> o.metodo() == command.metodoEnvio())
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El envío " + command.metodoEnvio().etiqueta() + " no está disponible para " + destino.departamento()));
        return new Envio(destino, opcion.metodo(), opcion.costo(),
                TarifaEnvio.fechaEstimada(LocalDate.now(ZoneId.of("America/Bogota")), opcion.diasHabiles()));
    }
}
