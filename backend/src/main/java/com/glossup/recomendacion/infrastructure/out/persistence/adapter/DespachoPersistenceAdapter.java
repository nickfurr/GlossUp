package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.port.out.DespachoRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DespachoPersistenceAdapter implements DespachoRepositoryPort {

    private final JdbcTemplate jdbc;

    public DespachoPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(Long pedidoId, Long vendedorId) {
        // Sin el bloqueo, dos vendedores a la vez no verían la confirmación del otro y el pedido nunca avanzaría.
        jdbc.queryForObject("SELECT id FROM pedido WHERE id = ? FOR UPDATE", Long.class, pedidoId);
        jdbc.update("INSERT INTO despacho_vendedor (pedido_id, vendedor_id) VALUES (?, ?) ON CONFLICT DO NOTHING", pedidoId, vendedorId);
    }

    @Override
    public Set<Long> vendedoresQueDespacharon(Long pedidoId) {
        return new HashSet<>(jdbc.queryForList("SELECT vendedor_id FROM despacho_vendedor WHERE pedido_id = ?", Long.class, pedidoId));
    }

    @Override
    public Set<Long> pedidosDespachadosPor(Long vendedorId) {
        return new HashSet<>(jdbc.queryForList("SELECT pedido_id FROM despacho_vendedor WHERE vendedor_id = ?", Long.class, vendedorId));
    }
}
