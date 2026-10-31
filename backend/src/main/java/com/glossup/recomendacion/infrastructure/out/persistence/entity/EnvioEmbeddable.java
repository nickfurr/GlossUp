package com.glossup.recomendacion.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Columnas envio_* del pedido (HU14). Es 1 a 1 y se lee siempre con el pedido, por eso no va en otra tabla. */
@Embeddable
public class EnvioEmbeddable {

    @Column(name = "envio_nombre_recibe", length = 120)
    public String nombreRecibe;

    @Column(name = "envio_telefono", length = 10)
    public String telefono;

    @Column(name = "envio_departamento", length = 40)
    public String departamento;

    @Column(name = "envio_ciudad", length = 80)
    public String ciudad;

    @Column(name = "envio_barrio", length = 80)
    public String barrio;

    @Column(name = "envio_direccion", length = 160)
    public String direccion;

    @Column(name = "envio_complemento", length = 120)
    public String complemento;

    @Column(name = "envio_indicaciones", length = 300)
    public String indicaciones;

    @Column(name = "envio_metodo", length = 20)
    public String metodo;

    @Column(name = "envio_costo", precision = 12, scale = 2)
    public BigDecimal costo;

    @Column(name = "envio_fecha_estimada")
    public LocalDate fechaEstimada;

    @Column(name = "envio_estado", length = 20)
    public String estado;

    @Column(name = "envio_transportadora", length = 80)
    public String transportadora;

    @Column(name = "envio_guia", length = 60)
    public String guia;

    @Column(name = "envio_intentos")
    public Integer intentos;

    @Column(name = "envio_notificado_en")
    public OffsetDateTime notificadoEn;

    @Column(name = "envio_enviado_en")
    public OffsetDateTime enviadoEn;

    @Column(name = "envio_entregado_en")
    public OffsetDateTime entregadoEn;

    @Column(name = "envio_no_entregado_en")
    public OffsetDateTime noEntregadoEn;
}
