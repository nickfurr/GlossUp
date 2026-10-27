package com.glossup.recomendacion.application.service;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.model.TransaccionPago;
import com.glossup.recomendacion.domain.port.in.PagarPedidoUseCase;
import com.glossup.recomendacion.domain.port.out.InventarioPort;
import com.glossup.recomendacion.domain.port.out.PasarelaPagosPort;
import com.glossup.recomendacion.domain.port.out.PedidoRepositoryPort;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pago de pedidos con la pasarela externa (HU10).
 *
 * <p>Reglas: solo una transacción APROBADA, consultada a la pasarela o recibida en un evento con
 * firma válida, cuyo monto y moneda coinciden con el pedido, lo pasa a PAGADO. Aplicar la misma
 * transacción dos veces no cambia nada (webhook y regreso del cliente pueden llegar ambos). Un pago
 * rechazado deja el pedido CREADO para reintentar; si vence sin pagar, se cancela y su inventario
 * vuelve al catálogo.</p>
 */
@Service
public class PagarPedidoService implements PagarPedidoUseCase {

    private static final Logger log = LoggerFactory.getLogger(PagarPedidoService.class);
    private static final String PREFIJO_REFERENCIA = "GLOSSUP-";
    /** Margen tras el vencimiento del enlace de pago antes de liberar el stock: cubre eventos tardíos. */
    private static final int MINUTOS_DE_GRACIA = 5;

    private final PedidoRepositoryPort pedidos;
    private final PasarelaPagosPort pasarela;
    private final InventarioPort inventario;
    private final String urlPublica;
    private final Clock reloj;

    @Autowired
    public PagarPedidoService(PedidoRepositoryPort pedidos, PasarelaPagosPort pasarela, InventarioPort inventario,
                              @Value("${glossup.url-publica}") String urlPublica) {
        this(pedidos, pasarela, inventario, urlPublica, Clock.systemUTC());
    }

    PagarPedidoService(PedidoRepositoryPort pedidos, PasarelaPagosPort pasarela, InventarioPort inventario,
                       String urlPublica, Clock reloj) {
        this.pedidos = pedidos;
        this.pasarela = pasarela;
        this.inventario = inventario;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public String iniciarPago(Long usuarioId, Long pedidoId) {
        Pedido pedido = propio(usuarioId, pedidoId);
        if (pedido.getEstado() != EstadoPedido.CREADO) {
            throw new ReglaNegocioException("El pedido #" + pedidoId + " ya está " + pedido.getEstado().name().toLowerCase());
        }
        if (pedido.vencido(ahora())) {
            throw new ReglaNegocioException("El plazo para pagar el pedido #" + pedidoId + " venció. Vuelve a armar tu carrito.");
        }
        // Una referencia por intento: si un pago se rechaza, el siguiente intento es otra transacción.
        String referencia = PREFIJO_REFERENCIA + pedido.getId() + "-" + reloj.millis();
        return pasarela.urlPago(referencia, pedido.totalEnCentavos(),
                urlPublica + "/pago/resultado/" + pedido.getId(), pedido.expiraEn());
    }

    @Override
    @Transactional
    public ResultadoPago confirmarPago(Long usuarioId, Long pedidoId, String transaccionId) {
        Pedido pedido = propio(usuarioId, pedidoId);
        TransaccionPago transaccion = pasarela.consultar(transaccionId)
                .orElseThrow(() -> new NotFoundException("La pasarela no reconoce la transacción " + transaccionId));
        if (!pedidoId.equals(pedidoDeReferencia(transaccion.referencia()).orElse(null))) {
            throw new ReglaNegocioException("La transacción " + transaccionId + " no corresponde al pedido #" + pedidoId);
        }
        return new ResultadoPago(aplicar(pedido, transaccion), transaccion.estado());
    }

    @Override
    @Transactional
    public boolean procesarEvento(String cuerpo) {
        Optional<TransaccionPago> transaccion = pasarela.leerEvento(cuerpo);
        if (transaccion.isEmpty()) {
            return false;
        }
        TransaccionPago t = transaccion.get();
        pedidoDeReferencia(t.referencia())
                .flatMap(pedidos::buscarPorId)
                .ifPresentOrElse(p -> aplicar(p, t),
                        () -> log.warn("Evento de pago con referencia desconocida: {}", t.referencia()));
        return true;
    }

    @Override
    @Transactional
    public Pedido cancelar(Long usuarioId, Long pedidoId) {
        Pedido pedido = propio(usuarioId, pedidoId);
        if (pedido.getEstado() != EstadoPedido.CREADO) {
            throw new ReglaNegocioException("Solo se puede cancelar un pedido sin pagar");
        }
        return cancelarYLiberar(pedido);
    }

    @Override
    @Transactional
    public int cancelarVencidos() {
        List<Pedido> vencidos = pedidos.buscarSinPagarCreadosAntesDe(
                ahora().minusMinutes(Pedido.MINUTOS_PARA_PAGAR + MINUTOS_DE_GRACIA));
        vencidos.forEach(this::cancelarYLiberar);
        if (!vencidos.isEmpty()) {
            log.info("Pedidos vencidos sin pagar cancelados: {}", vencidos.size());
        }
        return vencidos.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> misPedidos(Long usuarioId) {
        return pedidos.buscarPorUsuario(usuarioId);
    }

    /** Aplica una transacción verificada al pedido. Idempotente. */
    private Pedido aplicar(Pedido pedido, TransaccionPago t) {
        if (t.estado() != TransaccionPago.Estado.APROBADA) {
            return pedido; // rechazada, en proceso o con error: el pedido sigue CREADO y se puede reintentar
        }
        if (t.montoCentavos() != pedido.totalEnCentavos() || !"COP".equals(t.moneda())) {
            log.error("Pago {} con monto {} {} no coincide con el pedido #{} ({} COP): no se aplica",
                    t.id(), t.montoCentavos(), t.moneda(), pedido.getId(), pedido.totalEnCentavos());
            return pedido;
        }
        switch (pedido.getEstado()) {
            case CREADO -> {
                pedido.registrarPago(t.id(), t.metodo(), ahora());
                return pedidos.guardar(pedido);
            }
            case CANCELADO -> log.error("Pago aprobado {} para el pedido #{} ya cancelado: requiere reembolso manual",
                    t.id(), pedido.getId());
            default -> { } // ya pagado (evento repetido): nada que hacer
        }
        return pedido;
    }

    private Pedido cancelarYLiberar(Pedido pedido) {
        pedido.cancelar();
        for (PedidoItem item : pedido.getItems()) {
            inventario.liberar(item.productoId(), item.cantidad());
        }
        return pedidos.guardar(pedido);
    }

    private Pedido propio(Long usuarioId, Long pedidoId) {
        return pedidos.buscarPorId(pedidoId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado con ID: " + pedidoId));
    }

    /** "GLOSSUP-{pedido}-{intento}" → id del pedido; vacío si la referencia no es nuestra. */
    static Optional<Long> pedidoDeReferencia(String referencia) {
        if (referencia == null || !referencia.startsWith(PREFIJO_REFERENCIA)) {
            return Optional.empty();
        }
        String[] partes = referencia.substring(PREFIJO_REFERENCIA.length()).split("-");
        try {
            return Optional.of(Long.parseLong(partes[0]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(reloj);
    }
}
