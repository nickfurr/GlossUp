package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.port.in.ComprobanteUseCase;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.ComprobanteRepositoryPort;
import com.glossup.recomendacion.domain.port.out.DocumentoComprobantePort;
import com.glossup.recomendacion.domain.port.out.NotificacionPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Comprobante de compra (HU11). Se emite al confirmarse el pago y se guarda; si esa emisión falló,
 * se emite cuando el cliente lo descarga. Nunca se genera dos veces: el primero que lo guarda es el
 * que vale, y solo ese se envía por correo.
 */
@Service
public class ComprobanteService implements ComprobanteUseCase {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);

    private final PedidoRepositoryPort pedidos;
    private final ComprobanteRepositoryPort comprobantes;
    private final ClientePort clientes;
    private final DocumentoComprobantePort documento;
    private final NotificacionPort notificacion;

    public ComprobanteService(PedidoRepositoryPort pedidos, ComprobanteRepositoryPort comprobantes, ClientePort clientes,
                              DocumentoComprobantePort documento, NotificacionPort notificacion) {
        this.pedidos = pedidos;
        this.comprobantes = comprobantes;
        this.clientes = clientes;
        this.documento = documento;
        this.notificacion = notificacion;
    }

    @Override
    @Transactional
    public Comprobante emitir(Long pedidoId) {
        return emitir(pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + pedidoId)));
    }

    @Override
    @Transactional
    public Comprobante descargar(Long usuarioId, Long pedidoId) {
        return emitir(pedidos.buscarPorId(pedidoId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + pedidoId)));
    }

    private Comprobante emitir(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.PAGADO && pedido.getEstado() != EstadoPedido.ENVIADO) {
            throw new ReglaNegocioException("El pedido #" + pedido.getId() + " no está pagado: el comprobante se emite cuando se confirma el pago");
        }
        Optional<Comprobante> existente = comprobantes.buscar(pedido.getId());
        if (existente.isPresent()) {
            return existente.get();
        }
        ClientePort.Cliente cliente = clientes.buscar(pedido.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado con ID: " + pedido.getUsuarioId()));
        String numero = Comprobante.numeroDe(pedido.getId());
        OffsetDateTime ahora = OffsetDateTime.now();
        Comprobante nuevo = new Comprobante(pedido.getId(), numero, documento.generar(pedido, cliente, numero, ahora), ahora);
        if (!comprobantes.guardarSiNoExiste(nuevo)) {
            // El evento de pago y una descarga pueden emitirlo a la vez: vale el que se guardó primero.
            return comprobantes.buscar(pedido.getId()).orElseThrow();
        }
        try {
            notificacion.enviarComprobante(cliente, pedido, nuevo);
        } catch (RuntimeException e) {
            // Sin correo el comprobante igual queda guardado y se descarga desde "Mis pedidos".
            log.warn("No se pudo enviar el comprobante {} a {}: {}", numero, cliente.email(), e.getMessage());
        }
        return nuevo;
    }
}
