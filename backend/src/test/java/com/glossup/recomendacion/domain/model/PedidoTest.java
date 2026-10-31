package com.glossup.recomendacion.domain.model;

import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    private Pedido pedidoBase() {
        return new Pedido(null, 1L, EstadoPedido.CREADO, List.of(new PedidoItem(100L, 2, Dinero.de(30000))), null);
    }

    @Test
    void el_total_se_calcula_de_las_lineas() {
        Pedido p = pedidoBase();
        assertEquals(Dinero.de(60000), p.getTotal());
        assertEquals(EstadoPedido.CREADO, p.getEstado());
    }

    @Test
    void pedido_sin_items_no_es_valido() {
        assertThrows(ReglaNegocioException.class, () -> new Pedido(null, 1L, EstadoPedido.CREADO, List.of(), null));
    }

    @Test
    void transicion_valida_creado_pagado_enviado() {
        Pedido p = pedidoBase();
        p.marcarPagado();
        p.marcarEnviado();
        assertEquals(EstadoPedido.ENVIADO, p.getEstado());
    }

    @Test
    void no_se_puede_enviar_sin_pagar() {
        Pedido p = pedidoBase();
        assertThrows(ReglaNegocioException.class, p::marcarEnviado);
    }
}
