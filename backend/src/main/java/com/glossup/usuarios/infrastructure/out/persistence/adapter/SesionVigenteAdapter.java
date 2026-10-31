package com.glossup.usuarios.infrastructure.out.persistence.adapter;

import com.glossup.shared.security.SesionVigentePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class SesionVigenteAdapter implements SesionVigentePort {

    private final JdbcTemplate jdbc;

    public SesionVigenteAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** shortcut: una consulta por clave primaria en cada petición autenticada; poner una caché corta si llega a pesar. */
    @Override
    public boolean vigente(String subject, Instant emitidoEn) {
        long id;
        try {
            id = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            return false;
        }
        return jdbc.query("SELECT activo, sesiones_desde FROM usuario WHERE id = ?", (rs, n) -> {
            OffsetDateTime desde = rs.getObject("sesiones_desde", OffsetDateTime.class);
            // El token guarda segundos enteros: sin truncar, iniciar sesión justo después se rechazaría.
            return rs.getBoolean("activo")
                    && (desde == null || !emitidoEn.isBefore(desde.toInstant().truncatedTo(ChronoUnit.SECONDS)));
        }, id).stream().findFirst().orElse(false);
    }
}
