package com.glossup.compatibilidad.infrastructure.out.persistence.adapter;

import com.glossup.compatibilidad.domain.model.Beneficio;
import com.glossup.compatibilidad.domain.model.ReglaPerfil;
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

    private ReglaPerfil aDominio(ReglaPerfilEntity e) {
        return new ReglaPerfil(e.getId(), e.getIngredienteInci(), e.getCondicion(),
                Severidad.valueOf(e.getSeveridad()), e.getMotivo());
    }
}
