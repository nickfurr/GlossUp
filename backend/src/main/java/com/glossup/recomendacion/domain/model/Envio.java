package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Envío de un pedido (HU14): a dónde va, cómo, cuánto cuesta y en qué etapa está. */
public class Envio {

    private final Direccion destino;
    private final MetodoEnvio metodo;
    private final Dinero costo;
    private final LocalDate fechaEstimada;
    private EstadoEnvio estado;
    private String transportadora;
    private String guia;
    private int intentos;
    private OffsetDateTime notificadoEn;
    private OffsetDateTime enviadoEn;
    private OffsetDateTime entregadoEn;
    private OffsetDateTime noEntregadoEn;

    public Envio(Direccion destino, MetodoEnvio metodo, Dinero costo, LocalDate fechaEstimada) {
        this.destino = destino;
        this.metodo = metodo;
        this.costo = costo;
        this.fechaEstimada = fechaEstimada;
        this.estado = EstadoEnvio.PREPARANDO;
    }

    /** Reconstrucción desde persistencia. */
    public Envio conSeguimiento(EstadoEnvio estado, String transportadora, String guia, int intentos,
                                OffsetDateTime notificadoEn, OffsetDateTime enviadoEn,
                                OffsetDateTime entregadoEn, OffsetDateTime noEntregadoEn) {
        this.estado = estado;
        this.transportadora = transportadora;
        this.guia = guia;
        this.intentos = intentos;
        this.notificadoEn = notificadoEn;
        this.enviadoEn = enviadoEn;
        this.entregadoEn = entregadoEn;
        this.noEntregadoEn = noEntregadoEn;
        return this;
    }

    /** La transportadora recibió la solicitud y asignó la guía. */
    public void aceptado(String transportadora, String guia, OffsetDateTime ahora) {
        this.transportadora = transportadora;
        this.guia = guia;
        this.notificadoEn = ahora;
        this.estado = EstadoEnvio.LISTO_PARA_ENVIO;
    }

    public void intentoFallido() {
        intentos++;
    }

    /**
     * Aplica una etapa informada por la transportadora. Solo avanza: un evento repetido o atrasado no
     * cambia nada y devuelve false.
     */
    public boolean avanzarA(EstadoEnvio destinoEstado, OffsetDateTime ahora) {
        if (!destinoEstado.laInformaLaTransportadora() || estado.esFinal() || estado.compareTo(destinoEstado) >= 0) {
            return false;
        }
        if (enviadoEn == null) {
            enviadoEn = ahora; // la transportadora puede saltarse "en camino" y reportar directo la entrega
        }
        if (destinoEstado == EstadoEnvio.ENTREGADO) {
            entregadoEn = ahora;
        } else if (destinoEstado == EstadoEnvio.NO_ENTREGADO) {
            noEntregadoEn = ahora;
        }
        estado = destinoEstado;
        return true;
    }

    public Direccion getDestino() { return destino; }
    public MetodoEnvio getMetodo() { return metodo; }
    public Dinero getCosto() { return costo; }
    public LocalDate getFechaEstimada() { return fechaEstimada; }
    public EstadoEnvio getEstado() { return estado; }
    public String getTransportadora() { return transportadora; }
    public String getGuia() { return guia; }
    public int getIntentos() { return intentos; }
    public OffsetDateTime getNotificadoEn() { return notificadoEn; }
    public OffsetDateTime getEnviadoEn() { return enviadoEn; }
    public OffsetDateTime getEntregadoEn() { return entregadoEn; }
    public OffsetDateTime getNoEntregadoEn() { return noEntregadoEn; }
}
