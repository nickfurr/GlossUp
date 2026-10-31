package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EnvioActualizado;
import com.glossup.recomendacion.domain.model.EstadoEnvio;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.EnvioUseCase;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.DespachoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.NotificacionPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.recomendacion.domain.port.out.ProductosVendedorPort;
import com.glossup.recomendacion.domain.port.out.TransportadoraPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Notificación a la API de envíos (HU14 - SCRUM-39). Fire and forget: el pago nunca espera a la
 * transportadora ni falla por ella (RNF-22). Lo que no se pudo avisar se reintenta cada minuto.
 */
@Service
public class EnvioService implements EnvioUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnvioService.class);
    static final int MAX_INTENTOS = 30;
    /** No reintentar un pago recién aprobado: el aviso inicial todavía puede estar en curso. */
    private static final int MINUTOS_ANTES_DE_REINTENTAR = 2;

    private final PedidoRepositoryPort pedidos;
    private final TransportadoraPort transportadora;
    private final NotificacionPort notificacion;
    private final ClientePort clientes;
    private final ApplicationEventPublisher eventos;
    private final ProductosVendedorPort productos;
    private final DespachoRepositoryPort despachos;

    public EnvioService(PedidoRepositoryPort pedidos, TransportadoraPort transportadora, NotificacionPort notificacion,
                        ClientePort clientes, ApplicationEventPublisher eventos, ProductosVendedorPort productos,
                        DespachoRepositoryPort despachos) {
        this.pedidos = pedidos;
        this.transportadora = transportadora;
        this.notificacion = notificacion;
        this.clientes = clientes;
        this.eventos = eventos;
        this.productos = productos;
        this.despachos = despachos;
    }

    // Sin @Transactional: no se retiene una conexión de BD mientras se espera a la transportadora (hasta 5 s).
    @Override
    public void notificar(Long pedidoId) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + pedidoId));
        if (pedido.getEnvio() == null || pedido.getEstado() != EstadoPedido.PAGADO
                || pedido.getEnvio().getEstado() != EstadoEnvio.PREPARANDO) {
            return; // pedido sin envío (anterior a HU14) o ya avisado
        }
        try {
            TransportadoraPort.Aceptacion aceptacion = transportadora.solicitarEnvio(pedido);
            pedido.getEnvio().aceptado(aceptacion.transportadora(), aceptacion.guia(), OffsetDateTime.now());
            log.info("Pedido #{} entregado a {} con guía {}", pedidoId, aceptacion.transportadora(), aceptacion.guia());
        } catch (RuntimeException e) {
            pedido.getEnvio().intentoFallido();
            int intentos = pedido.getEnvio().getIntentos();
            if (intentos >= MAX_INTENTOS) {
                log.error("La transportadora rechazó el pedido #{} {} veces: requiere gestión manual", pedidoId, intentos, e);
            } else {
                log.warn("No se pudo avisar a la transportadora del pedido #{} (intento {}): {}", pedidoId, intentos, e.getMessage());
            }
        }
        pedidos.guardar(pedido);
    }

    @Override
    public void reintentarPendientes() {
        pedidos.buscarEnviosSinNotificar(OffsetDateTime.now().minusMinutes(MINUTOS_ANTES_DE_REINTENTAR), MAX_INTENTOS)
                .forEach(p -> notificar(p.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> enCurso() {
        return pedidos.buscarEnviosEnCurso();
    }

    @Override
    @Transactional
    public void actualizar(String guia, EstadoEnvio etapa) {
        Pedido pedido = pedidos.buscarPorGuia(guia)
                .orElseThrow(() -> new NotFoundException("No hay ningún envío con la guía " + guia));
        if (pedido.actualizarEnvio(etapa, OffsetDateTime.now())) {
            pedidos.guardar(pedido);
            eventos.publishEvent(new EnvioActualizado(pedido.getId()));
        }
    }

    @Override
    @Transactional
    public void marcarEnviado(Long vendedorId, Long pedidoId) {
        Pedido pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + pedidoId));
        Set<Long> vendedores = productos.vendedoresDe(
                pedido.getItems().stream().map(i -> i.productoId()).collect(Collectors.toSet()));
        if (!vendedores.contains(vendedorId)) {
            throw new NotFoundException("Pedido no encontrado con ID: " + pedidoId); // no revela pedidos ajenos
        }
        if (pedido.getEstado() != EstadoPedido.PAGADO && pedido.getEstado() != EstadoPedido.ENVIADO) {
            throw new ReglaNegocioException("El pedido #" + pedidoId + " no está pagado");
        }
        if (pedido.getEnvio() != null && pedido.getEnvio().getEstado() == EstadoEnvio.PREPARANDO) {
            throw new ReglaNegocioException("La transportadora todavía no asigna la guía de este pedido. Inténtalo en unos minutos.");
        }
        despachos.registrar(pedidoId, vendedorId);
        if (despachos.vendedoresQueDespacharon(pedidoId).containsAll(vendedores) && pedido.despachar(OffsetDateTime.now())) {
            pedidos.guardar(pedido);
            eventos.publishEvent(new EnvioActualizado(pedidoId));
        }
    }

    @Override
    public void avisarCliente(Long pedidoId) {
        pedidos.buscarPorId(pedidoId).ifPresent(pedido -> clientes.buscar(pedido.getUsuarioId())
                .ifPresent(cliente -> notificacion.avisarEnvio(cliente, pedido)));
    }
}
