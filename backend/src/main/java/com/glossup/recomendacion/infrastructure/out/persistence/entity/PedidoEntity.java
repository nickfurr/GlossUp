package com.glossup.recomendacion.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "pago_transaccion_id", length = 64)
    private String pagoTransaccionId;

    @Column(name = "pago_metodo", length = 40)
    private String pagoMetodo;

    @Column(name = "pagado_en")
    private OffsetDateTime pagadoEn;

    @Embedded
    private EnvioEmbeddable envio;

    /** Bloqueo optimista: dos escrituras simultáneas del mismo pedido no pueden ganar ambas. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItemEntity> items = new ArrayList<>();

    public PedidoEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getPagoTransaccionId() { return pagoTransaccionId; }
    public void setPagoTransaccionId(String pagoTransaccionId) { this.pagoTransaccionId = pagoTransaccionId; }

    public String getPagoMetodo() { return pagoMetodo; }
    public void setPagoMetodo(String pagoMetodo) { this.pagoMetodo = pagoMetodo; }

    public OffsetDateTime getPagadoEn() { return pagadoEn; }
    public void setPagadoEn(OffsetDateTime pagadoEn) { this.pagadoEn = pagadoEn; }

    public EnvioEmbeddable getEnvio() { return envio; }
    public void setEnvio(EnvioEmbeddable envio) { this.envio = envio; }

    public List<PedidoItemEntity> getItems() { return items; }
    public void setItems(List<PedidoItemEntity> items) { this.items = items; }

    public void agregarItem(PedidoItemEntity item) {
        items.add(item);
        item.setPedido(this);
    }
}
