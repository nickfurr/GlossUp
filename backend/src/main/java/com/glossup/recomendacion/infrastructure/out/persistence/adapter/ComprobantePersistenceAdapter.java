package com.glossup.recomendacion.infrastructure.out.persistence.adapter;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.port.out.ComprobanteRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

/** Comprobantes en la tabla {@code comprobante} (V13): el PDF se guarda tal como se emitió. */
@Component
public class ComprobantePersistenceAdapter implements ComprobanteRepositoryPort {

    private final JdbcTemplate jdbc;

    public ComprobantePersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Comprobante> buscar(Long pedidoId) {
        return jdbc.query("SELECT pedido_id, numero, pdf, emitido_en FROM comprobante WHERE pedido_id = ?",
                (rs, i) -> new Comprobante(rs.getLong("pedido_id"), rs.getString("numero"), rs.getBytes("pdf"),
                        rs.getObject("emitido_en", OffsetDateTime.class)),
                pedidoId).stream().findFirst();
    }

    @Override
    public boolean guardarSiNoExiste(Comprobante c) {
        return jdbc.update("INSERT INTO comprobante (pedido_id, numero, pdf, emitido_en) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (pedido_id) DO NOTHING",
                c.pedidoId(), c.numero(), c.pdf(), c.emitidoEn()) == 1;
    }
}
