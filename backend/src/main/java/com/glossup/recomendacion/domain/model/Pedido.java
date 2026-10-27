package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Raíz de agregado: pedido confirmado (proceso "Gestión de ventas y checkout").
 *
 * <p>Reglas de negocio:</p>
 * <ul>
 *   <li>Un pedido nace en estado {@link EstadoPedido#CREADO} con al menos una línea.</li>
 *   <li>El total se calcula a partir de sus líneas (no se acepta de fuera).</li>
 *   <li>Los cambios de estado respetan las transiciones válidas de {@link EstadoPedido}.</li>
 *   <li>Un pedido CREADO reserva inventario durante {@link #MINUTOS_PARA_PAGAR} minutos; después vence (HU10).</li>
 * </ul>
 */
public class Pedido {

    /** Tiempo que el inventario queda reservado esperando el pago. */
    public static final int MINUTOS_PARA_PAGAR = 30;

    private final Long id;
    private final Long usuarioId;
    private EstadoPedido estado;
    private final List<PedidoItem> items;
    private final Dinero total;
    private final OffsetDateTime creadoEn;
    private String transaccionId;
    private String metodoPago;
    private OffsetDateTime pagadoEn;

    public Pedido(Long id, Long usuarioId, EstadoPedido estado, List<PedidoItem> items,
                  OffsetDateTime creadoEn) {
        this.id = id;
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId requerido");
        if (items == null || items.isEmpty()) {
            throw new ReglaNegocioException("Un pedido debe tener al menos un producto");
        }
        this.items = List.copyOf(items);
        this.estado = estado != null ? estado : EstadoPedido.CREADO;
        this.total = items.stream().map(PedidoItem::subtotal).reduce(Dinero.cero(), Dinero::mas);
        this.creadoEn = creadoEn != null ? creadoEn : OffsetDateTime.now();
    }

    /** Crea un pedido nuevo (CREADO) a partir de las líneas de un carrito validado. */
    public static Pedido crear(Long usuarioId, List<PedidoItem> items) {
        return new Pedido(null, usuarioId, EstadoPedido.CREADO, items, OffsetDateTime.now());
    }

    // ---- Transiciones de estado ----

    public void marcarPagado() { transicionarA(EstadoPedido.PAGADO); }

    /** Pago aprobado por la pasarela: pasa a PAGADO y guarda la transacción (para el comprobante, HU11). */
    public void registrarPago(String transaccionId, String metodoPago, OffsetDateTime pagadoEn) {
        transicionarA(EstadoPedido.PAGADO);
        conPago(transaccionId, metodoPago, pagadoEn);
    }

    /** Reconstrucción desde persistencia: datos del pago ya registrado, sin transición de estado. */
    public Pedido conPago(String transaccionId, String metodoPago, OffsetDateTime pagadoEn) {
        this.transaccionId = transaccionId;
        this.metodoPago = metodoPago;
        this.pagadoEn = pagadoEn;
        return this;
    }

    public OffsetDateTime expiraEn() { return creadoEn.plusMinutes(MINUTOS_PARA_PAGAR); }

    /** CREADO y pasado su plazo de pago. */
    public boolean vencido(OffsetDateTime ahora) {
        return estado == EstadoPedido.CREADO && ahora.isAfter(expiraEn());
    }

    /** Total en centavos, la unidad en que la pasarela cobra. */
    public long totalEnCentavos() {
        return total.monto().movePointRight(2).longValueExact();
    }

    public void marcarEnviado() { transicionarA(EstadoPedido.ENVIADO); }

    public void cancelar() { transicionarA(EstadoPedido.CANCELADO); }

    private void transicionarA(EstadoPedido destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new ReglaNegocioException(
                    "Transición de estado inválida: " + estado + " -> " + destino);
        }
        this.estado = destino;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public EstadoPedido getEstado() { return estado; }
    public List<PedidoItem> getItems() { return items; }
    public Dinero getTotal() { return total; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public String getTransaccionId() { return transaccionId; }
    public String getMetodoPago() { return metodoPago; }
    public OffsetDateTime getPagadoEn() { return pagadoEn; }
}
