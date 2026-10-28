package com.glossup.recomendacion.infrastructure.out.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_item")
public class PedidoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre", length = 200)
    private String nombre;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "precio_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnit;

    public PedidoItemEntity() {}

    public PedidoItemEntity(PedidoEntity pedido, Long productoId, String nombre, int cantidad, BigDecimal precioUnit) {
        this.pedido = pedido;
        this.productoId = productoId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnit = precioUnit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PedidoEntity getPedido() { return pedido; }
    public void setPedido(PedidoEntity pedido) { this.pedido = pedido; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnit() { return precioUnit; }
    public void setPrecioUnit(BigDecimal precioUnit) { this.precioUnit = precioUnit; }
}
