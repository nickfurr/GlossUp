package com.glossup.compatibilidad.infrastructure.out.persistence.adapter;

import com.glossup.compatibilidad.domain.model.Beneficio;
import com.glossup.compatibilidad.domain.model.ReglaIncompatibilidad;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
import com.glossup.compatibilidad.domain.model.ReglaRango;
import com.glossup.compatibilidad.domain.model.Severidad;
import com.glossup.compatibilidad.domain.port.out.ReglaRepositoryPort;
import com.glossup.compatibilidad.infrastructure.out.persistence.entity.ReglaPerfilEntity;
import com.glossup.compatibilidad.infrastructure.out.persistence.repository.ReglaPerfilJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReglaPerfilPersistenceAdapter implements ReglaRepositoryPort {

    private final ReglaPerfilJpaRepository jpa;
    private final JdbcTemplate jdbc;

    public ReglaPerfilPersistenceAdapter(ReglaPerfilJpaRepository jpa, JdbcTemplate jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public List<ReglaPerfil> reglasPerfil() {
        return jpa.findAll().stream().map(this::aDominio).toList();
    }

    @Override
    public Map<String, Set<String>> ingredientesPorAlergeno() {
        Map<String, Set<String>> mapa = new HashMap<>();
        jdbc.query("SELECT alergeno, ingrediente_inci FROM alergeno_ingrediente", rs -> {
            mapa.computeIfAbsent(rs.getString(1).toLowerCase(), k -> new HashSet<>())
                    .add(rs.getString(2).toLowerCase());
        });
        return mapa;
    }

    @Override
    public List<Beneficio> beneficios() {
        return jdbc.query("SELECT ingrediente_inci, condicion, motivo FROM beneficio",
                (rs, i) -> new Beneficio(rs.getString(1).toLowerCase(), rs.getString(2).toLowerCase(), rs.getString(3)));
    }

    @Override
    public List<ReglaRango> reglasRango() {
        return jdbc.query("SELECT atributo, condicion, desde, hasta, severidad, motivo FROM regla_rango",
                (rs, i) -> new ReglaRango(ReglaRango.Atributo.valueOf(rs.getString(1)), rs.getString(2).toLowerCase(),
                        rs.getDouble(3), rs.getDouble(4), Severidad.valueOf(rs.getString(5)), rs.getString(6)));
    }

    @Override
    public Map<String, Integer> comedogenicidad() {
        Map<String, Integer> mapa = new HashMap<>();
        jdbc.query("SELECT nombre_inci, comedogenicidad FROM ingrediente WHERE comedogenicidad IS NOT NULL",
                rs -> { mapa.put(rs.getString(1).toLowerCase(), rs.getInt(2)); });
        return mapa;
    }

    @Override
    public List<ReglaIncompatibilidad> reglasIncompatibilidad() {
        return jdbc.query("""
                SELECT r.id, a.nombre_inci, b.nombre_inci, r.severidad, r.motivo FROM regla_incompatibilidad r
                JOIN ingrediente a ON a.id = r.ingrediente_a JOIN ingrediente b ON b.id = r.ingrediente_b""",
                (rs, i) -> new ReglaIncompatibilidad(rs.getLong(1), rs.getString(2), rs.getString(3),
                        Severidad.valueOf(rs.getString(4)), rs.getString(5)));
    }

    private ReglaPerfil aDominio(ReglaPerfilEntity e) {
        return new ReglaPerfil(e.getId(), e.getIngredienteInci(), e.getCondicion(),
                Severidad.valueOf(e.getSeveridad()), e.getMotivo());
    }
}
