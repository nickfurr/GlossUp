package com.glossup.usuarios.infrastructure.out.persistence.adapter;

import com.glossup.usuarios.domain.port.out.RecuperacionClaveRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class RecuperacionClavePersistenceAdapter implements RecuperacionClaveRepositoryPort {

    private final JdbcTemplate jdbc;

    public RecuperacionClavePersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(String tokenHash, Long usuarioId, OffsetDateTime expiraEn) {
        jdbc.update("DELETE FROM recuperacion_clave WHERE usuario_id = ?", usuarioId);
        jdbc.update("INSERT INTO recuperacion_clave (token_hash, usuario_id, expira_en) VALUES (?, ?, ?)", tokenHash, usuarioId, expiraEn);
    }

    @Override
    public boolean hayRecienteDe(Long usuarioId, OffsetDateTime desde) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM recuperacion_clave WHERE usuario_id = ? AND creado_en > ?)", Boolean.class, usuarioId, desde));
    }

    @Override
    public Optional<Long> consumir(String tokenHash) {
        return jdbc.queryForList("DELETE FROM recuperacion_clave WHERE token_hash = ? AND expira_en > now() RETURNING usuario_id",
                Long.class, tokenHash).stream().findFirst();
    }
}
